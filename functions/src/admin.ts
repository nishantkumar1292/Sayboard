import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * Verify that the request comes from an admin user.
 * Admin users have the custom claim { admin: true } set via:
 *   firebase auth:claims:set <uid> --custom-claims '{"admin":true}'
 */
async function verifyAdmin(req: functions.https.Request): Promise<string> {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith("Bearer ")) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth token");
  }
  const token = authHeader.split("Bearer ")[1];
  const decoded = await admin.auth().verifyIdToken(token);

  if (!decoded.admin) {
    throw new functions.https.HttpsError("permission-denied", "Not an admin");
  }

  return decoded.uid;
}

/**
 * POST /adminAddCredits
 * Body: { uid: string, credits: number }
 * Adds credits to a user account.
 */
export const adminAddCredits = functions.https.onRequest(async (req, res) => {
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
    await verifyAdmin(req);

    const { uid, credits } = req.body;
    if (!uid || typeof credits !== "number" || credits <= 0) {
      res.status(400).json({ error: "Invalid uid or credits amount" });
      return;
    }

    const db = admin.firestore();
    const userRef = db.collection("users").doc(uid);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      res.status(404).json({ error: "User not found" });
      return;
    }

    await userRef.update({
      credits: admin.firestore.FieldValue.increment(credits),
    });

    const updated = await userRef.get();
    res.json({
      success: true,
      uid,
      creditsAdded: credits,
      totalCredits: updated.data()?.credits || 0,
    });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Internal error";
    functions.logger.error("AdminAddCredits error:", error);
    res.status(500).json({ error: message });
  }
});

/**
 * POST /adminSetSubscription
 * Body: { uid: string, status: "none" | "active" | "expired" }
 * Manually set a user's subscription status.
 */
export const adminSetSubscription = functions.https.onRequest(async (req, res) => {
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
    await verifyAdmin(req);

    const { uid, status } = req.body;
    if (!uid || !["none", "active", "expired"].includes(status)) {
      res.status(400).json({ error: "Invalid uid or status" });
      return;
    }

    const db = admin.firestore();
    const userRef = db.collection("users").doc(uid);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      res.status(404).json({ error: "User not found" });
      return;
    }

    await userRef.update({ subscriptionStatus: status });

    res.json({ success: true, uid, subscriptionStatus: status });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Internal error";
    functions.logger.error("AdminSetSubscription error:", error);
    res.status(500).json({ error: message });
  }
});

/**
 * POST /adminResetTrial
 * Body: { uid: string }
 * Resets a user's trial start date to now (gives them a fresh 7-day trial).
 */
export const adminResetTrial = functions.https.onRequest(async (req, res) => {
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
    await verifyAdmin(req);

    const { uid } = req.body;
    if (!uid) {
      res.status(400).json({ error: "Missing uid" });
      return;
    }

    const db = admin.firestore();
    const userRef = db.collection("users").doc(uid);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      res.status(404).json({ error: "User not found" });
      return;
    }

    const now = admin.firestore.Timestamp.now();
    await userRef.update({ trialStartedAt: now });

    const trialEnd = new Date(now.toDate().getTime() + 7 * 24 * 60 * 60 * 1000);

    res.json({
      success: true,
      uid,
      trialStartedAt: now.toDate().toISOString(),
      trialExpiresAt: trialEnd.toISOString(),
    });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Internal error";
    functions.logger.error("AdminResetTrial error:", error);
    res.status(500).json({ error: message });
  }
});

/**
 * GET /adminGetUser?uid=<uid>
 * Returns full user document for admin inspection.
 */
export const adminGetUser = functions.https.onRequest(async (req, res) => {
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
    await verifyAdmin(req);

    const uid = req.query.uid as string;
    if (!uid) {
      res.status(400).json({ error: "Missing uid query parameter" });
      return;
    }

    const db = admin.firestore();
    const userDoc = await db.collection("users").doc(uid).get();

    if (!userDoc.exists) {
      res.status(404).json({ error: "User not found" });
      return;
    }

    const data = userDoc.data()!;

    // Also fetch Firebase Auth user info
    let authUser;
    try {
      authUser = await admin.auth().getUser(uid);
    } catch {
      authUser = null;
    }

    res.json({
      uid,
      email: authUser?.email || null,
      displayName: authUser?.displayName || null,
      ...data,
      trialStartedAt: data.trialStartedAt?.toDate()?.toISOString() || null,
      createdAt: data.createdAt?.toDate()?.toISOString() || null,
    });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Internal error";
    functions.logger.error("AdminGetUser error:", error);
    res.status(500).json({ error: message });
  }
});

/**
 * GET /adminListUsers?limit=50&offset=0
 * Lists all users with their subscription status.
 */
export const adminListUsers = functions.https.onRequest(async (req, res) => {
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
    await verifyAdmin(req);

    const limit = Math.min(parseInt(req.query.limit as string) || 50, 100);
    const offset = parseInt(req.query.offset as string) || 0;

    const db = admin.firestore();
    const snapshot = await db
      .collection("users")
      .orderBy("createdAt", "desc")
      .offset(offset)
      .limit(limit)
      .get();

    const users = snapshot.docs.map((doc) => {
      const data = doc.data();
      const trialStart = data.trialStartedAt?.toDate();
      const trialEnd = trialStart
        ? new Date(trialStart.getTime() + 7 * 24 * 60 * 60 * 1000)
        : null;

      return {
        uid: doc.id,
        createdAt: data.createdAt?.toDate()?.toISOString() || null,
        trialStartedAt: trialStart?.toISOString() || null,
        trialExpiresAt: trialEnd?.toISOString() || null,
        subscriptionStatus: data.subscriptionStatus || "none",
        credits: data.credits || 0,
      };
    });

    res.json({ users, count: users.length, offset, limit });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Internal error";
    functions.logger.error("AdminListUsers error:", error);
    res.status(500).json({ error: message });
  }
});
