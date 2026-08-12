# Aapne Jo Code Bheja - Uska Matlab (Hindi)

Aapne ye code bheja:
```javascript
var admin = require("firebase-admin");
var serviceAccount = require("path/to/serviceAccountKey.json");
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});
```

## Iska Matlab Simple Hai:

### Line 1: `var admin = require("firebase-admin");`
- Firebase Admin SDK library ko load karo
- Ye library aapko Firebase ko full control deti hai (backend se)

### Line 2: `var serviceAccount = require("path/to/serviceAccountKey.json");`
- `serviceAccountKey.json` wo **Master Key** hai jo aapne Firebase Console se download ki
- Is file me secret keys hoti hain jo batati hain "Main sutoapp-e155c project ka owner hu"
- **path/to/** ko apni file ke path se replace karna hai, jaise:
  ```javascript
  var serviceAccount = require("./serviceAccountKey.json");
  // ya
  var serviceAccount = require("./sutoapp-e155c-firebase-adminsdk-abc123.json");
  ```

### Line 3-5: `admin.initializeApp({...})`
- Firebase ko us Master Key se connect karo
- Ab aapka Node.js / Python code Firebase ko admin banke control kar sakta hai

## Aapke Project Me Iska Use:

### 1. File Download Karo (Aapke Link Se)
Link: https://console.firebase.google.com/project/sutoapp-e155c/settings/serviceaccounts/adminsdk

- Wahan `Generate new private key` button par click karo
- Ek JSON file download hogi, naam kuch aisa: `sutoapp-e155c-firebase-adminsdk-xxxxx.json`

### 2. Project Me Rakho

```
Suto/
  ├── telegram_control_nodejs.js  <- Naya Node.js bot (maine banaya)
  ├── telegram_control_admin_sdk.py <- Python wala
  └── serviceAccountKey.json  <- Download ki hui file yahan rakho (rename karke)
```

**Important:** Ye JSON file `.gitignore` me hai, to GitHub par nahi jayegi - safe hai!

### 3. Code Chalao

**Node.js Version (Aapne jo code bheja usi se):**
```bash
npm install firebase-admin node-telegram-bot-api
# serviceAccountKey.json ko folder me rakho
export BOT_TOKEN='NAYA_TOKEN'  # purana revoke ke baad
export CHAT_ID='289240360'
node telegram_control_nodejs.js
```

**Python Version (Same kaam, Python me):**
```bash
pip install firebase-admin
export GOOGLE_APPLICATION_CREDENTIALS="./serviceAccountKey.json"
export BOT_TOKEN='NAYA_TOKEN'
python3 telegram_control_admin_sdk.py
```

## Dono Ka Kaam Same Hai:

Aap Telegram pe `/disable_booking` bhejoge:
1. Bot (Node ya Python) Firebase Admin SDK se connect hoga (us JSON key se)
2. Firebase me `app_features/bookingEnabled = false` likh dega
3. Android App Firebase se check karke booking band kar dega!

## Kya Aapne JSON Download Kiya?

Agar haan, to:
- File ka naam kya hai? (Share mat karo, bas batao download hui ya nahi)
- Main aapko exact command de dunga us file ke naam se

Agar nahi kiya, to us Admin SDK page par jaake `Generate new private key` par click karo.

## Security Checklist:

- ✅ `serviceAccountKey.json` - Kabhi GitHub par mat dalo (humne gitignore me daal diya)
- ✅ Telegram Bot Token - Naya banao (purana leak ho gaya)
- ✅ GitHub PAT `ghp_93...` - Delete karo (aapne leak kiya tha)
- ✅ Ye 3 cheezen sirf apne PC par rakho
