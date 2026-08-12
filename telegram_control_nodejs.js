/**
 * Telegram se App Control - Node.js Admin SDK Version
 * Project: sutoapp-e155c
 * Ye wahi code hai jo aapne Firebase console par dekha:
 * 
 * var admin = require("firebase-admin");
 * var serviceAccount = require("path/to/serviceAccountKey.json");
 * admin.initializeApp({
 *   credential: admin.credential.cert(serviceAccount)
 * });
 * 
 * Iska matlab: Firebase Admin SDK ko serviceAccountKey.json se initialize karo
 * 
 * Setup:
 * 1. Firebase Console -> Project Settings -> Service Accounts -> Generate new private key
 * 2. JSON file download hogi, uska naam kuch aisa hoga:
 *    sutoapp-e155c-firebase-adminsdk-xxxxx.json
 * 3. Isko apne project folder me rakho (gitignore me hai, GitHub par nahi jayegi)
 * 4. npm install firebase-admin node-telegram-bot-api
 * 5. BOT_TOKEN aur CHAT_ID set karo
 * 6. node telegram_control_nodejs.js
 * 
 * ⚠️ serviceAccountKey.json kabhi share mat karo! Ye master key hai!
 */

const admin = require("firebase-admin");
const TelegramBot = require('node-telegram-bot-api');
const fs = require('fs');
const path = require('path');

// ============ CONFIG - Yahan apna data dalo ============
const BOT_TOKEN = process.env.BOT_TOKEN || "PUT_YOUR_NEW_BOT_TOKEN_HERE"; // Naya wala, purana revoke karke
const CHAT_ID = process.env.CHAT_ID || "289240360"; // Sirf aap control kar sakte ho
const SERVICE_ACCOUNT_PATH = process.env.GOOGLE_APPLICATION_CREDENTIALS || "./serviceAccountKey.json";

// ============ FIREBASE ADMIN SDK INITIALIZE (Wahi code jo aapne bheja) ============

// Check if service account file exists
let serviceAccount;
try {
    // Agar path me file hai to use karo
    if (fs.existsSync(SERVICE_ACCOUNT_PATH)) {
        serviceAccount = require(path.resolve(SERVICE_ACCOUNT_PATH));
        console.log(`✅ Service Account loaded from: ${SERVICE_ACCOUNT_PATH}`);
    } else {
        // Try common names
        const possibleNames = [
            "./sutoapp-e155c-firebase-adminsdk.json",
            "./serviceAccountKey.json",
            "./admin-sdk.json"
        ];
        for (const fileName of possibleNames) {
            if (fs.existsSync(fileName)) {
                serviceAccount = require(path.resolve(fileName));
                console.log(`✅ Service Account found: ${fileName}`);
                break;
            }
        }
    }

    if (!serviceAccount) {
        console.log("⚠️ Service Account JSON nahi mila - REST mode fallback nahi hai Node me");
        console.log("   Firebase Console -> Service Accounts -> Generate new private key se download karo");
        console.log(`   File ko rakho: ${SERVICE_ACCOUNT_PATH}`);
        process.exit(1);
    }

    // YE WAHI CODE HAI JO AAPNE BHEJA - Firebase Admin Initialize
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: "https://sutoapp-e155c-default-rtdb.firebaseio.com/"
    });

    console.log("✅ Firebase Admin SDK Initialized - Project: sutoapp-e155c");

} catch (error) {
    console.error("❌ Firebase Init Error:", error.message);
    console.log("\nGuide:");
    console.log("1. https://console.firebase.google.com/project/sutoapp-e155c/settings/serviceaccounts/adminsdk jao");
    console.log("2. Generate new private key -> JSON download karo");
    console.log("3. Us file ko ./serviceAccountKey.json naam se save karo");
    process.exit(1);
}

const db = admin.database();
const featuresRef = db.ref("app_features");

// Default features
const defaultFeatures = {
    bookingEnabled: true,
    maintenanceMode: false,
    servicePrice: "₹100",
    announcement: "",
    paymentEnabled: true,
    lastUpdatedBy: "System",
    updatedAt: new Date().toISOString()
};

// ============ TELEGRAM BOT ============
if (BOT_TOKEN.includes("PUT_") || BOT_TOKEN.includes("YOUR_")) {
    console.log("❌ BOT_TOKEN set karo!");
    console.log("   export BOT_TOKEN='naya_token'");
    console.log("   export CHAT_ID='289240360'");
    process.exit(1);
}

const bot = new TelegramBot(BOT_TOKEN, { polling: true });
console.log(`🤖 Suto Control Bot Started - Chat ID ${CHAT_ID} only allowed`);
console.log("   Telegram pe /start bhejo...");

// Helper to get features
async function getFeatures() {
    try {
        const snapshot = await featuresRef.once('value');
        return snapshot.val() || defaultFeatures;
    } catch (e) {
        console.error("Get features error:", e.message);
        return defaultFeatures;
    }
}

async function saveFeatures(features) {
    try {
        features.updatedAt = new Date().toISOString();
        features.lastUpdatedBy = "Telegram Node.js Bot";
        await featuresRef.set(features);
        return true;
    } catch (e) {
        console.error("Save error:", e.message);
        return false;
    }
}

// Only owner can control
function isOwner(chatId) {
    return String(chatId) === String(CHAT_ID);
}

bot.onText(/\/start/, async (msg) => {
    if (!isOwner(msg.chat.id)) {
        console.log(`Blocked: ${msg.chat.id}`);
        return;
    }
    bot.sendMessage(msg.chat.id, `🤖 *Suto App Control - Admin SDK Secure*
Project: \`sutoapp-e155c\`
Mode: Admin SDK ✅ (Aapne jo code bheja wahi use ho raha hai)

*Commands:*
/status - Status dekho
/enable_booking - Booking ON
/disable_booking - Booking OFF
/maintenance_on - Maintenance ON
/maintenance_off - Maintenance OFF
/set_price 250 - Price set
/announce Kal band hai - Announcement
/clear_announce - Clear

*Ye code chal raha hai:*
\`admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
})\`

Ab aap Telegram se App control kar sakte ho!`, { parse_mode: "Markdown" });
});

bot.onText(/\/status/, async (msg) => {
    if (!isOwner(msg.chat.id)) return;
    const f = await getFeatures();
    bot.sendMessage(msg.chat.id, `📱 *Suto App Status*

Booking: ${f.bookingEnabled ? '✅ ON' : '🚫 OFF'}
Maintenance: ${f.maintenanceMode ? '🔧 ON' : '✅ OFF'}
Price: ${f.servicePrice}
Announce: ${f.announcement || 'Nahi hai'}

Last: ${f.lastUpdatedBy}
At: ${f.updatedAt}

Project: sutoapp-e155c
Secure: Admin SDK via serviceAccountKey.json`, { parse_mode: "Markdown" });
});

bot.onText(/\/enable_booking/, async (msg) => {
    if (!isOwner(msg.chat.id)) return;
    const f = await getFeatures();
    f.bookingEnabled = true;
    if (await saveFeatures(f)) {
        bot.sendMessage(msg.chat.id, "✅ Booking ON! App me booking chalu ho gayi.");
    }
});

bot.onText(/\/disable_booking/, async (msg) => {
    if (!isOwner(msg.chat.id)) return;
    const f = await getFeatures();
    f.bookingEnabled = false;
    if (await saveFeatures(f)) {
        bot.sendMessage(msg.chat.id, "🚫 Booking OFF! App me booking band.");
    }
});

bot.onText(/\/maintenance_on/, async (msg) => {
    if (!isOwner(msg.chat.id)) return;
    const f = await getFeatures();
    f.maintenanceMode = true;
    if (await saveFeatures(f)) bot.sendMessage(msg.chat.id, "🔧 Maintenance ON!");
});

bot.onText(/\/maintenance_off/, async (msg) => {
    if (!isOwner(msg.chat.id)) return;
    const f = await getFeatures();
    f.maintenanceMode = false;
    if (await saveFeatures(f)) bot.sendMessage(msg.chat.id, "✅ Maintenance OFF!");
});

bot.onText(/\/set_price (.+)/, async (msg, match) => {
    if (!isOwner(msg.chat.id)) return;
    const price = match[1];
    const f = await getFeatures();
    f.servicePrice = price;
    if (await saveFeatures(f)) bot.sendMessage(msg.chat.id, `💰 Price set: *${price}*`, { parse_mode: "Markdown" });
});

bot.onText(/\/announce (.+)/, async (msg, match) => {
    if (!isOwner(msg.chat.id)) return;
    const ann = match[1];
    const f = await getFeatures();
    f.announcement = ann;
    if (await saveFeatures(f)) bot.sendMessage(msg.chat.id, `📢 Announce: *${ann}*`, { parse_mode: "Markdown" });
});

bot.onText(/\/clear_announce/, async (msg) => {
    if (!isOwner(msg.chat.id)) return;
    const f = await getFeatures();
    f.announcement = "";
    if (await saveFeatures(f)) bot.sendMessage(msg.chat.id, "✅ Announcement clear!");
});

console.log("✅ Bot ready - Aapne jo Firebase code bheja tha, wahi use ho raha hai!");
console.log("   var admin = require('firebase-admin'); se initialize ho gaya");
