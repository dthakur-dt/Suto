#!/usr/bin/env python3
"""
Telegram Control Bot - Admin SDK Version (Secure)
Project: sutoapp-e155c
Page: https://console.firebase.google.com/project/sutoapp-e155c/settings/serviceaccounts/adminsdk

Ye version Admin SDK use karta hai - zyada secure hai!
Simple wale me Realtime DB test mode me open hota hai, isme secure rules laga sakte ho.

Setup:
1. Us Admin SDK page se "Generate new private key" se JSON download karo
2. File ka naam: sutoapp-e155c-firebase-adminsdk-xxxxx.json
3. Isko project folder me rakho (gitignore me hai, push nahi hogi)
4. export GOOGLE_APPLICATION_CREDENTIALS="./your-file.json"
5. pip install firebase-admin
6. Bot chalao

Security: JSON file kabhi share mat karo!
"""

import os
import requests
from datetime import datetime

# Config
BOT_TOKEN = os.getenv("BOT_TOKEN", "PUT_YOUR_NEW_BOT_TOKEN_HERE")
CHAT_ID = os.getenv("CHAT_ID", "289240360")
FIREBASE_DB_URL = "https://sutoapp-e155c-default-rtdb.firebaseio.com/"  # Aapka project

# Try to use Admin SDK if available
USE_ADMIN_SDK = False
db = None

try:
    import firebase_admin
    from firebase_admin import credentials, db as admin_db
    
    cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", "")
    if cred_path and os.path.exists(cred_path) and not firebase_admin._apps:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred, {
            'databaseURL': FIREBASE_DB_URL
        })
        USE_ADMIN_SDK = True
        print(f"✅ Admin SDK loaded from {cred_path}")
    elif not cred_path:
        print("ℹ️ GOOGLE_APPLICATION_CREDENTIALS set nahi hai - REST mode use hoga")
        print("   Secure mode ke liye: export GOOGLE_APPLICATION_CREDENTIALS='./serviceAccountKey.json'")
except ImportError:
    print("ℹ️ firebase-admin install nahi hai - REST mode chalega")
    print("   Install: pip install firebase-admin")
except Exception as e:
    print(f"⚠️ Admin SDK error: {e} - REST mode me chalega")

def get_features_admin():
    """Admin SDK se features lao"""
    try:
        ref = admin_db.reference('app_features')
        data = ref.get()
        if data:
            return data
    except Exception as e:
        print(f"Admin get error: {e}")
    return None

def save_features_admin(features):
    """Admin SDK se save karo"""
    try:
        ref = admin_db.reference('app_features')
        features["updatedAt"] = str(datetime.now())
        features["lastUpdatedBy"] = "Telegram Admin SDK Bot"
        ref.set(features)
        return True
    except Exception as e:
        print(f"Admin save error: {e}")
        return False

def get_features_rest():
    """REST se lao (fallback)"""
    try:
        url = f"{FIREBASE_DB_URL}/app_features.json"
        r = requests.get(url, timeout=10)
        if r.status_code == 200 and r.text not in ["null", ""]:
            return r.json()
    except:
        pass
    return {
        "bookingEnabled": True,
        "maintenanceMode": False,
        "servicePrice": "₹100",
        "announcement": "",
        "paymentEnabled": True
    }

def save_features_rest(features):
    try:
        url = f"{FIREBASE_DB_URL}/app_features.json"
        features["updatedAt"] = str(datetime.now())
        features["lastUpdatedBy"] = "Telegram REST Bot"
        r = requests.put(url, json=features, timeout=10)
        return r.status_code == 200
    except:
        return False

def get_features():
    if USE_ADMIN_SDK:
        data = get_features_admin()
        if data:
            return data
    return get_features_rest()

def save_features(features):
    if USE_ADMIN_SDK:
        return save_features_admin(features)
    return save_features_rest(features)

def send_telegram(text):
    try:
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        requests.post(url, data={
            "chat_id": CHAT_ID,
            "text": text,
            "parse_mode": "Markdown"
        }, timeout=10)
    except:
        pass

def handle_command(cmd_text, from_id):
    if str(from_id) != str(CHAT_ID):
        print(f"Blocked unauthorized: {from_id}")
        return

    features = get_features()
    cmd = cmd_text.lower().strip()

    if cmd.startswith("/start"):
        mode = "Admin SDK (Secure ✅)" if USE_ADMIN_SDK else "REST Mode (Test ⚠️)"
        send_telegram(f"""🤖 *Suto App Control - {mode}*

Project: `sutoapp-e155c`

*Commands:*
/status
/enable_booking ✅
/disable_booking 🚫
/maintenance_on 🔧
/maintenance_off
/set_price 250
/announce Kal band hai
/clear_announce
/help

Bot ab is Firebase ko control karega:
`{FIREBASE_DB_URL}`
""")

    elif cmd.startswith("/status"):
        status = f"""📱 *Suto App Features*

Booking: {'✅ ON' if features.get('bookingEnabled', True) else '🚫 OFF'}
Maintenance: {'🔧 ON' if features.get('maintenanceMode') else '✅ OFF'}
Price: {features.get('servicePrice', '₹100')}
Ann: {features.get('announcement') or 'Nahi hai'}

Mode: {'Admin SDK ✅' if USE_ADMIN_SDK else 'REST ⚠️'}
Last: {features.get('lastUpdatedBy', '')}
At: {features.get('updatedAt', '')}

Project: sutoapp-e155c
"""
        send_telegram(status)

    elif "/enable_booking" in cmd:
        features["bookingEnabled"] = True
        if save_features(features):
            send_telegram("✅ Booking ON! Customer app me booking chalu.")

    elif "/disable_booking" in cmd:
        features["bookingEnabled"] = False
        if save_features(features):
            send_telegram("🚫 Booking OFF! App me booking band.")

    elif "/maintenance_on" in cmd:
        features["maintenanceMode"] = True
        if save_features(features):
            send_telegram("🔧 Maintenance ON!")

    elif "/maintenance_off" in cmd:
        features["maintenanceMode"] = False
        if save_features(features):
            send_telegram("✅ Maintenance OFF!")

    elif cmd.startswith("/set_price"):
        parts = cmd_text.split(" ", 1)
        if len(parts) > 1:
            features["servicePrice"] = parts[1]
            if save_features(features):
                send_telegram(f"💰 Price set: {parts[1]}")

    elif cmd.startswith("/announce"):
        parts = cmd_text.split(" ", 1)
        if len(parts) > 1:
            features["announcement"] = parts[1]
            if save_features(features):
                send_telegram(f"📢 Announce set: {parts[1]}")

    elif "/clear_announce" in cmd:
        features["announcement"] = ""
        if save_features(features):
            send_telegram("✅ Announce clear!")

def poll():
    print(f"🤖 Suto Control Bot Started - Project sutoapp-e155c")
    print(f"Mode: {'Admin SDK Secure' if USE_ADMIN_SDK else 'REST Test'}")
    print(f"Chat ID: {CHAT_ID} only allowed")
    print("Telegram pe /start bhejo...")

    if "PUT_" in BOT_TOKEN or "YOUR_" in BOT_TOKEN:
        print("❌ BOT_TOKEN set karo!")
        return

    offset = 0
    while True:
        try:
            url = f"https://api.telegram.org/bot{BOT_TOKEN}/getUpdates?offset={offset}&timeout=30"
            r = requests.get(url, timeout=35)
            data = r.json()
            if not data.get("ok"):
                continue
            for upd in data.get("result", []):
                offset = upd["update_id"] + 1
                msg = upd.get("message", {})
                text = msg.get("text", "")
                cid = msg.get("chat", {}).get("id")
                if text and cid:
                    print(f"CMD {cid}: {text}")
                    handle_command(text, cid)
        except KeyboardInterrupt:
            break
        except Exception as e:
            print(f"Error: {e}")
            import time
            time.sleep(3)

if __name__ == "__main__":
    poll()
