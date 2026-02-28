import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

async function verifyAuth(req: functions.https.Request): Promise<string> {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith("Bearer ")) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth token");
  }
  const token = authHeader.split("Bearer ")[1];
  const decoded = await admin.auth().verifyIdToken(token);
  return decoded.uid;
}

const REGION = "asia-south1";

export const userStatus = functions.region(REGION).https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "GET");
    res.set("Access-Control-Allow-Headers", "Authorization");
    res.status(204).send("");
    return;
  }

  if (req.method !== "GET") {
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  try {
    const uid = await verifyAuth(req);
    const db = admin.firestore();
    const userDoc = await db.collection("users").doc(uid).get();

    if (!userDoc.exists) {
      // User hasn't made any request yet — trial will start on first transcription
      res.json({
        trialStartedAt: null,
        trialExpiresAt: null,
        subscriptionStatus: "none",
        credits: 0,
        hasAccess: true,  // New users always have access (trial starts on first use)
      });
      return;
    }

    const data = userDoc.data()!;
    const trialStart = data.trialStartedAt?.toDate();
    const trialEnd = trialStart
      ? new Date(trialStart.getTime() + 7 * 24 * 60 * 60 * 1000)
      : null;

    const isTrialActive = trialEnd ? new Date() < trialEnd : false;
    const isSubscribed = data.subscriptionStatus === "active";
    const hasCredits = (data.credits || 0) > 0;

    res.json({
      trialStartedAt: trialStart?.toISOString() || null,
      trialExpiresAt: trialEnd?.toISOString() || null,
      subscriptionStatus: data.subscriptionStatus || "none",
      credits: data.credits || 0,
      hasAccess: isSubscribed || isTrialActive || hasCredits,
    });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Internal error";
    functions.logger.error("UserStatus error:", error);
    res.status(500).json({ error: message });
  }
});
