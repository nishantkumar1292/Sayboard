import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * RevenueCat webhook handler.
 * RevenueCat sends events like INITIAL_PURCHASE, RENEWAL, EXPIRATION, CANCELLATION.
 * The webhook URL should be configured in RevenueCat dashboard to point here.
 * Set REVENUECAT_WEBHOOK_AUTH in Firebase secrets for auth header verification.
 */
export const revenuecatWebhook = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  try {
    // Verify webhook auth header
    const expectedAuth = process.env.REVENUECAT_WEBHOOK_AUTH;
    if (expectedAuth) {
      const authHeader = req.headers.authorization;
      if (authHeader !== `Bearer ${expectedAuth}`) {
        res.status(401).json({ error: "Unauthorized" });
        return;
      }
    }

    const event = req.body;
    const eventType = event?.event?.type;
    const appUserId = event?.event?.app_user_id;

    if (!eventType || !appUserId) {
      res.status(400).json({ error: "Invalid webhook payload" });
      return;
    }

    functions.logger.info(`RevenueCat webhook: ${eventType} for ${appUserId}`);

    const db = admin.firestore();

    // appUserId from RevenueCat is the Firebase UID (set during SDK init)
    const userRef = db.collection("users").doc(appUserId);

    switch (eventType) {
      case "INITIAL_PURCHASE":
      case "RENEWAL":
      case "UNCANCELLATION":
        await userRef.update({
          subscriptionStatus: "active",
          revenuecatAppUserId: appUserId,
        });
        break;

      case "EXPIRATION":
      case "CANCELLATION":
        await userRef.update({
          subscriptionStatus: "expired",
        });
        break;

      case "BILLING_ISSUE":
        // Keep active but log warning
        functions.logger.warn(`Billing issue for user ${appUserId}`);
        break;

      default:
        functions.logger.info(`Unhandled RevenueCat event: ${eventType}`);
    }

    res.status(200).json({ success: true });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Internal error";
    functions.logger.error("Webhook error:", error);
    res.status(500).json({ error: message });
  }
});
