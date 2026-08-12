#!/usr/bin/env python3
"""
Telegram se App Feature Control Bot
Ye bot aapko Telegram se App ko control karne deta hai!

Features:
- /start - Bot start
- /status - App ke features ka status dekho
- /enable_booking - Booking ON
- /disable_booking - Booking OFF
- /maintenance_on - Maintenance mode ON
- /maintenance_off - Maintenance mode OFF
- /set_price 200 - Price set karo
- /announce Ham kal band hai - Announcement set karo
- /clear_announce - Announcement hatao

Setup:
1. Firebase Realtime Database banao (free) - https://console.firebase.google.com
2. Project banao -> Realtime Database -> Create (test mode)
3. Database URL copy karo: https://your-project-default-rtdb.firebaseio.com
4. Neeche FIREBASE_URL me daalo
5. Bot token daalo (NAYA wala, revoke ke baad)

Aap Telegram se control karoge, App automatically update ho jayega!
"""

import requests
import os
from datetime import datetime

# ============ CONFIG ============
# NAYA token (purana revoke kar diya hai na?)
BOT_TOKEN = os.getenv("BOT_TOKEN", "PUT_YOUR_NEW_BOT_TOKEN_HERE")
CHAT_ID = os.getenv("CHAT_ID", "289240360")  # Aapki ID - sirf aap control kar sakte ho

# Firebase URL - Aapko banani hai (free)
# Example: https://suto-app-default-rtdb.firebaseio.com/app_features.json
FIREBASE_URL = os.getenv("FIREBASE_URL", "PUT_YOUR_FIREBASE_URL_HERE/app_features.json")

# ============ FEATURE FUNCTIONS ============

def get_features():
    """Firebase se current features lao"""
    if "YOUR_" in FIREBASE_URL or "PUT_" in FIREBASE_URL:
        return {
            "bookingEnabled": True,
            "maintenanceMode": False,
            "servicePrice": "₹100",
            "announcement": "",
            "paymentEnabled": True,
            "lastUpdatedBy": "Not set",
            "updatedAt": ""
        }
    try:
        r = requests.get(FIREBASE_URL, timeout=10)
        if r.status_code == 200 and r.text != "null":
            return r.json()
    except:
        pass
    return {
        "bookingEnabled": True,
        "maintenanceMode": False,
        "servicePrice": "₹100",
        "announcement": "",
        "paymentEnabled": True,
        "lastUpdatedBy": "System",
        "updatedAt": str(datetime.now())
    }

def save_features(features):
    """Firebase me features save karo - App automatically update ho jayega"""
    if "YOUR_" in FIREBASE_URL or "PUT_" in FIREBASE_URL:
        print("❌ Firebase URL set nahi hai! README me guide dekho.")
        return False
    
    features["updatedAt"] = str(datetime.now())
    features["lastUpdatedBy"] = "Telegram Bot"
    
    try:
        # Firebase PUT request
        r = requests.put(FIREBASE_URL, json=features, timeout=10)
        return r.status_code == 200
    except Exception as e:
        print(f"Save error: {e}")
        return False

def send_telegram_message(text):
    """Owner ko Telegram pe reply bhejo"""
    try:
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        requests.post(url, data={
            "chat_id": CHAT_ID,
            "text": text,
            "parse_mode": "Markdown"
        }, timeout=10)
    except Exception as e:
        print(f"Telegram send error: {e}")

def handle_command(command_text, from_chat_id):
    """Sirf owner ke commands handle karo"""
    # Security: Sirf aapka CHAT_ID allowed hai
    if str(from_chat_id) != str(CHAT_ID):
        print(f"Unauthorized access from {from_chat_id}")
        return

    features = get_features()
    command = command_text.lower().strip()

    if command.startswith("/start"):
        msg = """🤖 *App Control Bot Ready!*

*Commands:*
/status - Features status
/enable_booking - Booking ON ✅
/disable_booking - Booking OFF 🚫
/maintenance_on - Maintenance ON 🔧
/maintenance_off - Maintenance OFF
/set_price 250 - Price set karo
/announce Kal chhuti hai - Announcement
/clear_announce - Announcement clear
/help - Ye help

*Example:*
Aap `/disable_booking` bhejoge to App me booking band ho jayegi!
"""
        send_telegram_message(msg)

    elif command.startswith("/status"):
        status = f"""📱 *App Features Status:*

Booking: {'✅ ON' if features.get('bookingEnabled') else '🚫 OFF'}
Maintenance: {'🔧 ON' if features.get('maintenanceMode') else '✅ OFF'}
Price: {features.get('servicePrice', '₹100')}
Payment: {'✅ ON' if features.get('paymentEnabled') else '🚫 OFF'}
Announcement: {features.get('announcement') or 'Koi nahi'}

Last Update: {features.get('lastUpdatedBy')} at {features.get('updatedAt')}
"""
        send_telegram_message(status)

    elif command.startswith("/enable_booking"):
        features["bookingEnabled"] = True
        if save_features(features):
            send_telegram_message("✅ Booking *ON* kar diya! App me booking chalu ho gayi.")
        else:
            send_telegram_message("❌ Firebase URL set nahi hai, README dekho.")

    elif command.startswith("/disable_booking"):
        features["bookingEnabled"] = False
        if save_features(features):
            send_telegram_message("🚫 Booking *OFF* kar diya! App me booking band ho gayi.")
        else:
            send_telegram_message("❌ Firebase error")

    elif command.startswith("/maintenance_on"):
        features["maintenanceMode"] = True
        if save_features(features):
            send_telegram_message("🔧 Maintenance *ON*! App maintenance mode me chala gaya.")
        else:
            send_telegram_message("❌ Error")

    elif command.startswith("/maintenance_off"):
        features["maintenanceMode"] = False
        if save_features(features):
            send_telegram_message("✅ Maintenance *OFF*! App wapas normal ho gaya.")
        else:
            send_telegram_message("❌ Error")

    elif command.startswith("/set_price"):
        parts = command_text.split(" ", 1)
        if len(parts) > 1:
            price = parts[1].strip()
            features["servicePrice"] = price
            if save_features(features):
                send_telegram_message(f"💰 Price set kar diya: *{price}*")
            else:
                send_telegram_message("❌ Error")
        else:
            send_telegram_message("Usage: /set_price 250 ya /set_price ₹200")

    elif command.startswith("/announce"):
        parts = command_text.split(" ", 1)
        if len(parts) > 1:
            ann = parts[1].strip()
            features["announcement"] = ann
            if save_features(features):
                send_telegram_message(f"📢 Announcement set: *{ann}*")
            else:
                send_telegram_message("❌ Error")
        else:
            send_telegram_message("Usage: /announce Kal ham band hai")

    elif command.startswith("/clear_announce"):
        features["announcement"] = ""
        if save_features(features):
            send_telegram_message("✅ Announcement clear kar diya!")
        else:
            send_telegram_message("❌ Error")

    elif command.startswith("/help"):
        handle_command("/start", from_chat_id)

    else:
        send_telegram_message("❓ Command samajh nahi aaya. /help likho.")

def poll_telegram():
    """Telegram se commands check karo (simple polling)"""
    print("🤖 Telegram Control Bot Started...")
    print(f"Your Chat ID: {CHAT_ID} (sirf aap control kar sakte ho)")
    if "YOUR_" in BOT_TOKEN or "PUT_" in BOT_TOKEN:
        print("❌ BOT_TOKEN set karo pehle!")
        print("export BOT_TOKEN='naya_token'")
        print("export FIREBASE_URL='https://...firebaseio.com/app_features.json'")
        return

    offset = 0
    print("✅ Bot polling... Telegram pe /start bhejo")
    
    while True:
        try:
            url = f"https://api.telegram.org/bot{BOT_TOKEN}/getUpdates?offset={offset}&timeout=30"
            r = requests.get(url, timeout=35)
            data = r.json()
            
            if not data.get("ok"):
                print(f"Error: {data}")
                continue

            for update in data.get("result", []):
                offset = update["update_id"] + 1
                message = update.get("message", {})
                chat_id = message.get("chat", {}).get("id")
                text = message.get("text", "")
                
                if text and chat_id:
                    print(f"Command from {chat_id}: {text}")
                    handle_command(text, chat_id)

        except KeyboardInterrupt:
            print("\nBot stopped")
            break
        except Exception as e:
            print(f"Poll error: {e}")
            import time
            time.sleep(5)

if __name__ == "__main__":
    poll_telegram()
