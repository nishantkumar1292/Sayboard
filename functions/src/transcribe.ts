import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import Busboy from "busboy";
import fetch from "node-fetch";
import FormData from "form-data";

const REGION = "asia-south1";

const OPENAI_API_URL = "https://api.openai.com/v1/audio/transcriptions";
const SARVAM_API_URL = "https://api.sarvam.ai/speech-to-text";

interface ParsedForm {
  fields: Record<string, string>;
  file?: { buffer: Buffer; filename: string; mimetype: string };
}

function parseMultipart(req: functions.https.Request): Promise<ParsedForm> {
  return new Promise((resolve, reject) => {
    const busboy = Busboy({ headers: req.headers });
    const fields: Record<string, string> = {};
    let fileBuffer: Buffer | undefined;
    let fileName = "";
    let fileMime = "";

    busboy.on("field", (name: string, val: string) => {
      fields[name] = val;
    });

    busboy.on("file", (_name: string, file: NodeJS.ReadableStream, info: { filename: string; mimeType: string }) => {
      const chunks: Buffer[] = [];
      fileName = info.filename;
      fileMime = info.mimeType;
      file.on("data", (chunk: Buffer) => chunks.push(chunk));
      file.on("end", () => {
        fileBuffer = Buffer.concat(chunks);
      });
    });

    busboy.on("finish", () => {
      resolve({
        fields,
        file: fileBuffer
          ? { buffer: fileBuffer, filename: fileName, mimetype: fileMime }
          : undefined,
      });
    });

    busboy.on("error", reject);

    // Cloud Functions pre-parses the body as a raw buffer
    if (req.rawBody) {
      busboy.end(req.rawBody);
    } else {
      req.pipe(busboy);
    }
  });
}

async function verifyAuth(req: functions.https.Request): Promise<string> {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith("Bearer ")) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth token");
  }
  const token = authHeader.split("Bearer ")[1];
  const decoded = await admin.auth().verifyIdToken(token);
  return decoded.uid;
}

interface UserDoc {
  createdAt: admin.firestore.Timestamp;
  trialStartedAt: admin.firestore.Timestamp;
  subscriptionStatus: "none" | "active" | "expired";
  credits: number;
  revenuecatAppUserId?: string;
}

// In-memory cache to avoid a Firestore round-trip on every request.
// Keyed by uid. Entries expire after 5 minutes.
const accessCache = new Map<string, { allowed: boolean; expiresAt: number }>();
const CACHE_TTL_MS = 5 * 60 * 1000;

async function checkAccess(uid: string): Promise<{ allowed: boolean; reason?: string }> {
  const cached = accessCache.get(uid);
  if (cached && Date.now() < cached.expiresAt) {
    return { allowed: cached.allowed };
  }

  const db = admin.firestore();
  const userRef = db.collection("users").doc(uid);
  const userDoc = await userRef.get();

  if (!userDoc.exists) {
    // First request — create user with trial
    const now = admin.firestore.Timestamp.now();
    const newUser: UserDoc = {
      createdAt: now,
      trialStartedAt: now,
      subscriptionStatus: "none",
      credits: 0,
    };
    await userRef.set(newUser);
    accessCache.set(uid, { allowed: true, expiresAt: Date.now() + CACHE_TTL_MS });
    return { allowed: true };
  }

  const data = userDoc.data() as UserDoc;

  let allowed = false;
  let reason: string | undefined;

  if (data.subscriptionStatus === "active") {
    allowed = true;
  } else if (data.credits > 0) {
    allowed = true;
  } else {
    const trialStart = data.trialStartedAt.toDate();
    const trialEnd = new Date(trialStart.getTime() + 7 * 24 * 60 * 60 * 1000);
    if (new Date() < trialEnd) {
      allowed = true;
    } else {
      reason = "trial_expired";
    }
  }

  // Cache for 5 min (shorter for denied so trial expiry is noticed quickly)
  accessCache.set(uid, {
    allowed,
    expiresAt: Date.now() + (allowed ? CACHE_TTL_MS : 60_000),
  });

  return { allowed, reason };
}

async function deductCredit(uid: string): Promise<void> {
  const db = admin.firestore();
  const userRef = db.collection("users").doc(uid);
  const userDoc = await userRef.get();
  const data = userDoc.data() as UserDoc;

  // Only deduct credits if not on active subscription and not in trial
  if (data.subscriptionStatus === "active") return;

  const trialStart = data.trialStartedAt.toDate();
  const trialEnd = new Date(trialStart.getTime() + 7 * 24 * 60 * 60 * 1000);
  if (new Date() < trialEnd) return;

  // Deduct 1 credit per transcription
  if (data.credits > 0) {
    await userRef.update({
      credits: admin.firestore.FieldValue.increment(-1),
    });
  }
}

async function forwardToWhisper(
  audioBuffer: Buffer,
  filename: string,
  fields: Record<string, string>
): Promise<{ status: number; body: string }> {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    throw new Error("OPENAI_API_KEY not configured");
  }

  const form = new FormData();
  form.append("file", audioBuffer, { filename, contentType: "audio/wav" });
  form.append("model", fields.model || "whisper-1");

  if (fields.language) form.append("language", fields.language);
  if (fields.prompt) form.append("prompt", fields.prompt);

  const response = await fetch(OPENAI_API_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      ...form.getHeaders(),
    },
    body: form,
  });

  const body = await response.text();
  return { status: response.status, body };
}

async function forwardToSarvam(
  audioBuffer: Buffer,
  filename: string,
  fields: Record<string, string>
): Promise<{ status: number; body: string }> {
  const apiKey = process.env.SARVAM_API_KEY;
  if (!apiKey) {
    throw new Error("SARVAM_API_KEY not configured");
  }

  const form = new FormData();
  form.append("file", audioBuffer, { filename, contentType: "audio/wav" });
  form.append("model", fields.model || "saaras:v3");
  form.append("mode", fields.mode || "translit");
  form.append("language_code", fields.language_code || "unknown");
  form.append("with_timestamps", "false");

  const response = await fetch(SARVAM_API_URL, {
    method: "POST",
    headers: {
      "api-subscription-key": apiKey,
      ...form.getHeaders(),
    },
    body: form,
  });

  const body = await response.text();
  return { status: response.status, body };
}

export const transcribe = functions
  .region(REGION)
  .runWith({
    secrets: ["SARVAM_API_KEY"],
    timeoutSeconds: 120,
    memory: "256MB",
    minInstances: 1,
  })
  .https.onRequest(async (req, res) => {
    // CORS
    res.set("Access-Control-Allow-Origin", "*");
    if (req.method === "OPTIONS") {
      res.set("Access-Control-Allow-Methods", "POST");
      res.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
      res.status(204).send("");
      return;
    }

    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }

    try {
      // 1. Authenticate
      const uid = await verifyAuth(req);

      // 2. Check access + parse multipart in parallel
      const [access, parsed] = await Promise.all([
        checkAccess(uid),
        parseMultipart(req),
      ]);

      if (!access.allowed) {
        res.status(402).json({ error: access.reason || "no_access" });
        return;
      }

      if (!parsed.file) {
        res.status(400).json({ error: "No audio file provided" });
        return;
      }

      const provider = parsed.fields.provider || "whisper";

      // 4. Forward to real API
      let result: { status: number; body: string };
      if (provider === "sarvam") {
        result = await forwardToSarvam(
          parsed.file.buffer,
          parsed.file.filename,
          parsed.fields
        );
      } else {
        result = await forwardToWhisper(
          parsed.file.buffer,
          parsed.file.filename,
          parsed.fields
        );
      }

      // 5. Deduct credit if applicable
      await deductCredit(uid);

      // 6. Return response
      res.status(result.status).send(result.body);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Internal error";
      functions.logger.error("Transcribe error:", error);
      res.status(500).json({ error: message });
    }
  });
