const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Sends notification helper
async function sendNotificationToAllUsers(title, body, targetUrl) {
  const payload = {
    notification: {
      title: title,
      body: body,
    },
    data: {
      url: targetUrl || "https://marudharaexam.in",
      target_url: targetUrl || "https://marudharaexam.in",
    },
    topic: "all_users",
  };

  try {
    const response = await admin.messaging().send(payload);
    console.log("Successfully sent push notification:", response);
    return response;
  } catch (error) {
    console.error("Error sending push notification:", error);
    throw error;
  }
}

// 1. Trigger on New Banner
exports.onNewBanner = functions.firestore
  .document("offerBanners/{docId}")
  .onCreate((snapshot, context) => {
    const data = snapshot.data();
    if (!data || !data.active) return null;

    const title = "New Banner Alert 🌟";
    const body = "New special offers or updates available on Marudhara Exam!";
    const targetUrl = data.link || "https://marudharaexam.in";

    return sendNotificationToAllUsers(title, body, targetUrl);
  });

// 2. Trigger on New Mock Test
exports.onNewMockTest = functions.firestore
  .document("mockTests/{docId}")
  .onCreate((snapshot, context) => {
    const data = snapshot.data();
    if (!data) return null;

    const testName = data.title || data.name || "Mock Test";
    const title = "New Mock Test Published 📝";
    const body = `Attempt the new mock test: "${testName}" now and boost your preparation!`;
    const targetUrl = "https://marudharaexam.in/mock-tests/index.html";

    return sendNotificationToAllUsers(title, body, targetUrl);
  });

// 3. Trigger on New Vacancy Alert
exports.onNewVacancy = functions.firestore
  .document("vacancies/{docId}")
  .onCreate((snapshot, context) => {
    const data = snapshot.data();
    if (!data) return null;

    const postName = data.title || data.name || "Vacancy Alert";
    const title = "New Job Alert / Vacancy 📢";
    const body = `New recruitment notice posted: "${postName}". Check details!`;
    const targetUrl = "https://marudharaexam.in/vacancy.html";

    return sendNotificationToAllUsers(title, body, targetUrl);
  });

// 4. Trigger on New Important Notice or Announcement
exports.onNewNotice = functions.firestore
  .document("notices/{docId}")
  .onCreate((snapshot, context) => {
    const data = snapshot.data();
    if (!data) return null;

    const noticeTitle = data.title || "Important Notice";
    const title = "Important Notice / Announcement 🔔";
    const body = noticeTitle;
    const targetUrl = "https://marudharaexam.in/student-corner.html";

    return sendNotificationToAllUsers(title, body, targetUrl);
  });

// 5. Trigger on New Documents/Downloads
exports.onNewDocument = functions.firestore
  .document("documents/{docId}")
  .onCreate((snapshot, context) => {
    const data = snapshot.data();
    if (!data) return null;

    const docName = data.title || data.name || "Important Document";
    const title = "New Study Material / PDF Download 📚";
    const body = `"${docName}" has been uploaded in your portal. Download now!`;
    const targetUrl = "https://marudharaexam.in/student-corner.html";

    return sendNotificationToAllUsers(title, body, targetUrl);
  });
