import * as admin from "firebase-admin";

admin.initializeApp();

export { transcribe } from "./transcribe";
export { userStatus } from "./userStatus";
export { revenuecatWebhook } from "./webhooks";
export {
  adminAddCredits,
  adminSetSubscription,
  adminResetTrial,
  adminGetUser,
  adminListUsers,
} from "./admin";
