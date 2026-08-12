# Telegram se App Control - Complete Guide (Hindi)

Ab aap **Telegram se apna Android App control** kar sakte ho! 🔥

## Ye Kaise Kaam Karta Hai?

```
Telegram (Aap)  -->  Firebase (Free Server)  -->  Android App (Customer)
   /disable_booking      features update          App me booking band
```

1. Aap Telegram pe `/disable_booking` likhoge
2. Bot Firebase me `bookingEnabled = false` kar dega
3. Customer ka Android App automatically check karega aur booking band kar dega!

---

## Step 1: Firebase Free Database Banao (5 minute)

1. https://console.firebase.google.com jao
2. **Add Project** -> Naam do `SutoApp` -> Continue
3. Google Analytics -> Disable kar do -> Create Project
4. Left side -> **Build -> Realtime Database** -> **Create Database**
5. Location: **us-central1** -> Next
6. Rules: **Start in test mode** select karo -> Enable
7. Database ban gaya! Upar URL copy karo:
   ```
   https://sutoapp-default-rtdb.firebaseio.com/
   ```
8. Iske end me `app_features.json` jod do:
   ```
   https://sutoapp-default-rtdb.firebaseio.com/app_features.json
   ```
   Ye pura URL aapka `FIREBASE_URL` hai.

---

## Step 2: Android App me Firebase URL Set Karo

`local.properties` file me ek line aur add karo:

```properties
telegram.bot.token=NAYA_TOKEN_HERE (BotFather wala)
telegram.chat.id=289240360
firebase.url=https://sutoapp-default-rtdb.firebaseio.com/app_features.json
```

Ab Android Studio me Rebuild karo. App ab Telegram se control hoga!

---

## Step 3: Telegram Control Bot Chalao

Apne PC/Laptop par:

```bash
# Naye tokens set karo (purane revoke kar diye na?)
export BOT_TOKEN='8670... naya wala'
export CHAT_ID='289240360'
export FIREBASE_URL='https://sutoapp-default-rtdb.firebaseio.com/app_features.json'

python3 telegram_feature_control_bot.py
```

Bot chalega aur bolega: `Bot polling...`

Ab Telegram pe apne `T1311bot` ko message bhejo:

- `/status` -> App ka current status dekho
- `/disable_booking` -> App me booking band!
- `/enable_booking` -> Booking chalu
- `/maintenance_on` -> App maintenance mode me
- `/maintenance_off` -> Normal
- `/set_price ₹250` -> Price change
- `/announce Kal dukaan band hai` -> App me announcement dikhega
- `/clear_announce` -> Announcement hatao

**Example Test:**
1. Telegram pe `/disable_booking` bhejo
2. Android App open karo
3. Dekhoge: `🚫 Booking abhi band hai` aur Book button disable!

---

## Features Jo Control Kar Sakte Ho

| Telegram Command | App me Kya Hoga |
|-----------------|----------------|
| `/enable_booking` | Booking ON |
| `/disable_booking` | Booking OFF, customer book nahi kar payega |
| `/maintenance_on` | App pura maintenance mode me |
| `/maintenance_off` | Normal |
| `/set_price 200` | Price change ho jayega |
| `/announce ...` | App me banner dikhega |

Aap `FeatureManager.kt` me aur features add kar sakte ho jaise `deliveryCharge`, `offerEnabled`, etc.

---

## Bina Firebase ke Chalega?

Haan, agar Firebase nahi banana to App default features se chalega (sab ON). Lekin Telegram se control ke liye Firebase zaruri hai (free hai).

---

## Security

- Sirf aapka `CHAT_ID = 289240360` hi control kar sakta hai, koi aur command bhejega to ignore hoga
- Firebase rules test mode me hai, production me secure rules lagao
- Token ko kabhi GitHub par mat dalo!

---

## Kya Chahiye Aapko?

1. Firebase banane me help chahiye?
2. Aur kaun se features Telegram se control karne hain? (jaise delivery, payment, etc.)
3. Bot ko 24x7 server par chalana hai?

Batao, main aur detail guide de dunga!
