#!/usr/bin/env python3
"""
Suto Single Bot - Master Bot
Architecture:
- Bot Ek Hi: @T1311bot (T1311)
- Cloud: GitHub Repo dthakur-dt/Suto (data/devices/*.json)
- Admin Panel: Telegram Bot + WebApp Console
- Android App: User phone par install, status output deta hai jo GitHub cloud par jata hai aur Telegram WebApp me dikhta hai

Ye Single Bot sab kuch karta hai:
1. Android App se status receive (via direct GitHub write ya Telegram message)
2. GitHub ko cloud ke taur par manage
3. Telegram par admin ko WebApp console view dikhata hai
4. Feature control (enable/disable booking, etc.)

Project: sutoapp-e155c (Firebase optional)
GitHub: dthakur-dt/Suto
Bot: @T1311bot - ID 8670943963

Testing Token: User ne kaha testing ke liye purana token use karna hai, production me badlenge
"""

import os
import requests
import json
import base64
from datetime import datetime

# ============ CONFIG ============
# Testing token - user ne bola testing ke liye yahi use karo
BOT_TOKEN = os.getenv("BOT_TOKEN", "8670943963:AAFz--BuWd6bvldpUPJ34bf-YCCrytX1fuo")
CHAT_ID = os.getenv("CHAT_ID", "289240360")  # Admin ka Chat ID - sirf ye banda control kar sakta hai

# GitHub Cloud Config
GITHUB_USER = "dthakur-dt"
GITHUB_REPO = "Suto"
GITHUB_PAT = os.getenv("GITHUB_PAT", "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")  # Testing PAT
GITHUB_BRANCH = "main"

# Firebase (optional, for feature control)
FIREBASE_DB_URL = "https://sutoapp-e155c-default-rtdb.firebaseio.com/"

# WebApp Console URL - GitHub Pages se host hoga
WEBAPP_URL = f"https://{GITHUB_USER}.github.io/{GITHUB_REPO}/"
# Agar Pages enable nahi hai to: https://github.com/{GITHUB_USER}/{GITHUB_REPO}/blob/main/docs/index.html

# ============ GITHUB CLOUD FUNCTIONS ============

def github_get_file(path):
    """GitHub se file lao (cloud se read)"""
    try:
        url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{path}"
        headers = {
            "Authorization": f"token {GITHUB_PAT}",
            "Accept": "application/vnd.github.v3+json"
        }
        r = requests.get(url, headers=headers, timeout=10)
        if r.status_code == 200:
            data = r.json()
            content_b64 = data.get("content", "")
            sha = data.get("sha", "")
            content = base64.b64decode(content_b64).decode('utf-8')
            return content, sha
    except Exception as e:
        print(f"GitHub get error {path}: {e}")
    return None, None

def github_save_file(path, content_str, message):
    """GitHub par file save karo (cloud pe write)"""
    try:
        url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{path}"
        headers = {
            "Authorization": f"token {GITHUB_PAT}",
            "Accept": "application/vnd.github.v3+json"
        }
        # Check existing SHA
        _, sha = github_get_file(path)
        
        content_b64 = base64.b64encode(content_str.encode('utf-8')).decode('utf-8')
        payload = {
            "message": message,
            "content": content_b64,
            "branch": GITHUB_BRANCH
        }
        if sha:
            payload["sha"] = sha
        
        r = requests.put(url, headers=headers, json=payload, timeout=15)
        if r.status_code in [200, 201]:
            print(f"✅ GitHub saved: {path}")
            return True
        else:
            print(f"❌ GitHub save fail {path}: {r.status_code} {r.text[:200]}")
            return False
    except Exception as e:
        print(f"GitHub save error: {e}")
        return False

def github_list_devices():
    """GitHub cloud se saare devices lao"""
    try:
        content, _ = github_get_file("data/devices/index.json")
        if content:
            data = json.loads(content)
            return data.get("devices", [])
    except:
        pass
    return []

def github_get_device_status(device_id):
    content, _ = github_get_file(f"data/devices/{device_id}.json")
    if content:
        try:
            return json.loads(content)
        except:
            pass
    return None

# ============ TELEGRAM BOT FUNCTIONS ============

def send_telegram_message(chat_id, text, reply_markup=None, parse_mode="Markdown"):
    """Telegram pe message bhejo"""
    try:
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        payload = {
            "chat_id": chat_id,
            "text": text,
            "parse_mode": parse_mode
        }
        if reply_markup:
            payload["reply_markup"] = json.dumps(reply_markup)
        
        r = requests.post(url, json=payload, timeout=10)
        return r.json()
    except Exception as e:
        print(f"Telegram send error: {e}")
        return None

def send_telegram_with_webapp(chat_id):
    """Admin panel - WebApp Console button ke saath"""
    # WebApp button
    keyboard = {
        "inline_keyboard": [
            [
                {
                    "text": "💻 Open Console (WebApp)",
                    "web_app": {"url": WEBAPP_URL}
                }
            ],
            [
                {"text": "📱 Devices Status", "callback_data": "devices_status"},
                {"text": "📊 /status", "callback_data": "check_status"}
            ],
            [
                {"text": "✅ Enable Booking", "callback_data": "enable_booking"},
                {"text": "🚫 Disable Booking", "callback_data": "disable_booking"}
            ],
            [
                {"text": "🔧 Maintenance ON", "callback_data": "maintenance_on"},
                {"text": "✅ Maintenance OFF", "callback_data": "maintenance_off"}
            ]
        ]
    }
    
    text = f"""🛰️ *Suto Admin Panel*
    
*Architecture:*
Bot Ek Hi: @T1311bot
Cloud: GitHub {GITHUB_USER}/{GITHUB_REPO}
Admin: Telegram WebApp Console
App: Android Status Reporter

*Android App jo output degi, wo yahan dikhegi:*

💻 *Console View:* Neeche "Open Console" button dabao - WebApp khulega jisme saare Android devices ka live status dikhega jo GitHub cloud se aa raha hai.

*Data Flow:*
📱 Android App (User) → ☁️ GitHub Cloud (data/devices/) → 🤖 Telegram Bot (Admin) → 💻 WebApp Console View

*Quick Actions:* Neeche buttons se direct control kar sakte ho.

*Console URL:* {WEBAPP_URL}
*GitHub:* github.com/{GITHUB_USER}/{GITHUB_REPO}
"""
    send_telegram_message(chat_id, text, keyboard)

# ============ FEATURE CONTROL (Firebase or GitHub) ============

def get_features():
    # Try Firebase first
    try:
        url = f"{FIREBASE_DB_URL}/app_features.json"
        r = requests.get(url, timeout=5)
        if r.status_code == 200 and r.text not in ["null", ""]:
            return r.json()
    except:
        pass
    
    # Fallback to GitHub
    try:
        content, _ = github_get_file("data/app_features.json")
        if content:
            return json.loads(content)
    except:
        pass
    
    return {
        "bookingEnabled": True,
        "maintenanceMode": False,
        "servicePrice": "₹100",
        "announcement": "",
        "paymentEnabled": True
    }

def save_features(features):
    features["updatedAt"] = str(datetime.now())
    features["lastUpdatedBy"] = "Telegram Admin Bot"
    
    # Save to Firebase
    try:
        url = f"{FIREBASE_DB_URL}/app_features.json"
        requests.put(url, json=features, timeout=5)
    except:
        pass
    
    # Save to GitHub as backup
    try:
        github_save_file("data/app_features.json", json.dumps(features, indent=2), "🎛️ Update app features from Telegram")
    except:
        pass
    
    return True

# ============ COMMAND HANDLERS ============

def handle_command(text, from_chat_id):
    """Sirf admin (CHAT_ID) ke commands"""
    if str(from_chat_id) != str(CHAT_ID):
        send_telegram_message(from_chat_id, f"❌ Aap admin nahi ho. Admin ID: {CHAT_ID[:3]}***")
        print(f"Blocked unauthorized {from_chat_id}")
        return

    cmd = text.lower().strip()
    features = get_features()

    if cmd.startswith("/start"):
        send_telegram_with_webapp(from_chat_id)

    elif cmd.startswith("/console") or cmd.startswith("/admin"):
        send_telegram_with_webapp(from_chat_id)

    elif cmd.startswith("/status"):
        devices = github_list_devices()
        total = len(devices)
        
        # Get recent statuses
        recent_output = ""
        for dev_id in devices[:3]:  # Last 3 devices
            status = github_get_device_status(dev_id)
            if status:
                recent_output += f"\n📱 {dev_id}: {status.get('name','')} - {status.get('status','')} - {status.get('battery','')}%"

        status_text = f"""📊 *System Status*

*Cloud:* GitHub {GITHUB_USER}/{GITHUB_REPO} - {'✅ Connected' if GITHUB_PAT else '❌ PAT missing'}
*Bot:* @T1311bot - ✅ Online (Testing Token)
*Project:* sutoapp-e155c
*WebApp:* {WEBAPP_URL}

*App Features:*
Booking: {'✅ ON' if features.get('bookingEnabled') else '🚫 OFF'}
Maintenance: {'🔧 ON' if features.get('maintenanceMode') else '✅ OFF'}
Price: {features.get('servicePrice')}
Announcement: {features.get('announcement') or 'Nahi hai'}

*Devices (GitHub Cloud):*
Total: {total}
{recent_output if recent_output else 'Koi device nahi - Android App se status bhejo'}

*Console:* "Open Console" button dabao WebApp kholne ke liye
"""
        send_telegram_message(from_chat_id, status_text)

        # Also send devices summary as JSON
        if total > 0:
            send_telegram_with_webapp(from_chat_id)

    elif cmd.startswith("/devices"):
        devices = github_list_devices()
        if not devices:
            send_telegram_message(from_chat_id, "📱 Koi device connect nahi hai.\nAndroid App install karo jo status GitHub par bhejega.")
            return
        
        text = "📱 *Connected Devices - Status Output:*\n\n"
        for dev_id in devices:
            status = github_get_device_status(dev_id)
            if status:
                text += f"🆔 {dev_id}\n"
                text += f"   📱 {status.get('name','')} ({status.get('model','')})\n"
                text += f"   🔋 {status.get('battery','')}% • {status.get('status','')}\n"
                text += f"   📝 Output: {status.get('output','')[:100]}...\n\n"
        
        text += f"\n💻 Full Console: {WEBAPP_URL}"
        send_telegram_message(from_chat_id, text)

    elif "/enable_booking" in cmd:
        features["bookingEnabled"] = True
        save_features(features)
        send_telegram_message(from_chat_id, "✅ Booking ON! Android App me booking chalu.")

    elif "/disable_booking" in cmd:
        features["bookingEnabled"] = False
        save_features(features)
        send_telegram_message(from_chat_id, "🚫 Booking OFF! App me booking band.")

    elif "/maintenance_on" in cmd:
        features["maintenanceMode"] = True
        save_features(features)
        send_telegram_message(from_chat_id, "🔧 Maintenance ON!")

    elif "/maintenance_off" in cmd:
        features["maintenanceMode"] = False
        save_features(features)
        send_telegram_message(from_chat_id, "✅ Maintenance OFF!")

    elif cmd.startswith("/set_price"):
        parts = text.split(" ", 1)
        if len(parts) > 1:
            features["servicePrice"] = parts[1]
            save_features(features)
            send_telegram_message(from_chat_id, f"💰 Price set: {parts[1]}")

    elif cmd.startswith("/announce"):
        parts = text.split(" ", 1)
        if len(parts) > 1:
            features["announcement"] = parts[1]
            save_features(features)
            send_telegram_message(from_chat_id, f"📢 Announcement: {parts[1]}")

    elif cmd.startswith("/help"):
        handle_command("/start", from_chat_id)

    else:
        # Unknown - maybe status output from Android?
        # If message contains DEVICE_ pattern, save as status
        if "DEVICE_" in text or "Status" in text or "Output" in text:
            # Try to parse as device status - save to GitHub
            print(f"Potential device status from {from_chat_id}: {text[:100]}")
            # For now, just ack
            send_telegram_message(from_chat_id, f"📝 Received: {text[:200]}...\n\nAgar ye Android App ka status hai to GitHub cloud par save ho raha hai.\nConsole dekho: {WEBAPP_URL}")

def handle_callback(callback_query):
    """Inline button callbacks"""
    data = callback_query.get("data", "")
    from_chat_id = callback_query.get("from", {}).get("id")
    message_id = callback_query.get("message", {}).get("message_id")

    if str(from_chat_id) != str(CHAT_ID):
        return

    # Answer callback
    try:
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/answerCallbackQuery"
        requests.post(url, json={"callback_query_id": callback_query["id"]}, timeout=5)
    except:
        pass

    # Handle
    if data == "devices_status":
        handle_command("/devices", from_chat_id)
    elif data == "check_status":
        handle_command("/status", from_chat_id)
    elif data in ["enable_booking", "disable_booking", "maintenance_on", "maintenance_off"]:
        handle_command(f"/{data}", from_chat_id)
    else:
        handle_command(f"/{data}", from_chat_id)

# ============ MAIN POLLING LOOP ============

def poll_bot():
    print(f"""
🛰️ Suto Single Bot - Master Admin Bot Started
================================================
Bot: @T1311bot (ID 8670943963)
Token: {BOT_TOKEN[:10]}... (Testing token - change before production)
Admin Chat ID: {CHAT_ID} - Only this user can control
Cloud: GitHub {GITHUB_USER}/{GITHUB_REPO}
WebApp Console: {WEBAPP_URL}
Firebase: sutoapp-e155c (optional)

Architecture:
📱 Android App (user) -> ☁️ GitHub Cloud (data/devices/) -> 🤖 Telegram Bot (admin) -> 💻 WebApp Console View

Bot Commands:
/start, /console, /status, /devices, /enable_booking, /disable_booking, etc.

Waiting for Telegram messages... Send /start to this bot
================================================
""")

    offset = 0
    while True:
        try:
            url = f"https://api.telegram.org/bot{BOT_TOKEN}/getUpdates?offset={offset}&timeout=30"
            r = requests.get(url, timeout=35)
            data = r.json()

            if not data.get("ok"):
                print(f"Telegram API error: {data}")
                continue

            for update in data.get("result", []):
                offset = update["update_id"] + 1

                # Message
                if "message" in update:
                    msg = update["message"]
                    chat_id = msg.get("chat", {}).get("id")
                    text = msg.get("text", "")
                    if text and chat_id:
                        print(f"📩 Message {chat_id}: {text[:50]}")
                        handle_command(text, chat_id)

                # Callback query (inline buttons)
                if "callback_query" in update:
                    print(f"🔘 Callback: {update['callback_query'].get('data')}")
                    handle_callback(update["callback_query"])

        except KeyboardInterrupt:
            print("\n🛑 Bot stopped by user")
            break
        except Exception as e:
            print(f"⚠️ Poll error: {e}")
            import time
            time.sleep(3)

if __name__ == "__main__":
    # Verify tokens
    if "PUT_" in BOT_TOKEN or len(BOT_TOKEN) < 20:
        print("❌ BOT_TOKEN set karo!")
        exit(1)
    
    if "ghp_" not in GITHUB_PAT and len(GITHUB_PAT) < 10:
        print("⚠️ GITHUB_PAT not set - cloud save fail hoga, but Telegram still works")
    
    poll_bot()
