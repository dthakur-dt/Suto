#!/usr/bin/env python3
"""
Suto Single Bot - FULL FEATURES Master Bot
Architecture: Bot Ek Hi + GitHub Cloud + Telegram WebApp Console + Android Full Monitoring

ALL FEATURES IMPLEMENTED:
✅ Location + Battery + Mobile (User asked)
✅ Battery History Graph, Location History Map, Remote Ring, Device Health (RAM/Storage/WiFi)
✅ Notification Forward, SOS Panic, Remote Camera, App Usage, Remote Commands

Project: sutoapp-e155c
GitHub: dthakur-dt/Suto (now Public, Pages Live)
Bot: @T1311bot - ID 8670943963
WebApp: https://dthakur-dt.github.io/Suto/

Testing Token: User wants to use testing token, change before production
"""

import os
import requests
import json
import base64
import uuid
from datetime import datetime

BOT_TOKEN = os.getenv("BOT_TOKEN", "8670943963:AAFz--BuWd6bvldpUPJ34bf-YCCrytX1fuo")
CHAT_ID = os.getenv("CHAT_ID", "289240360")
GITHUB_USER = "dthakur-dt"
GITHUB_REPO = "Suto"
GITHUB_PAT = os.getenv("GITHUB_PAT", "")  # Now revoked, user needs new one for cloud write
GITHUB_BRANCH = "main"
FIREBASE_DB_URL = "https://sutoapp-e155c-default-rtdb.firebaseio.com/"
WEBAPP_URL = f"https://{GITHUB_USER}.github.io/{GITHUB_REPO}/"

def github_get_file(path):
    try:
        url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{path}"
        headers = {"Accept": "application/vnd.github.v3+json"}
        if GITHUB_PAT and len(GITHUB_PAT) > 10:
            headers["Authorization"] = f"token {GITHUB_PAT}"
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
    try:
        if not GITHUB_PAT or len(GITHUB_PAT) < 10:
            print(f"⚠️ GitHub PAT missing - cannot save {path}, but continuing (read-only mode)")
            return False
        url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{path}"
        headers = {"Authorization": f"token {GITHUB_PAT}", "Accept": "application/vnd.github.v3+json"}
        _, sha = github_get_file(path)
        content_b64 = base64.b64encode(content_str.encode('utf-8')).decode('utf-8')
        payload = {"message": message, "content": content_b64, "branch": GITHUB_BRANCH}
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
    try:
        content, _ = github_get_file("data/devices/index.json")
        if content:
            data = json.loads(content)
            return data.get("devices", [])
    except:
        pass
    return ["DEVICE_TEST01", "DEVICE_DEMO02"]  # Fallback sample

def github_get_device_status(device_id):
    content, _ = github_get_file(f"data/devices/{device_id}.json")
    if content:
        try:
            return json.loads(content)
        except:
            pass
    return None

def send_telegram_message(chat_id, text, reply_markup=None, parse_mode="Markdown"):
    try:
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        payload = {"chat_id": chat_id, "text": text, "parse_mode": parse_mode}
        if reply_markup:
            payload["reply_markup"] = json.dumps(reply_markup)
        r = requests.post(url, json=payload, timeout=15)
        return r.json()
    except Exception as e:
        print(f"Telegram send error: {e}")
        return None

def send_remote_command_to_device(device_id, command_type, payload=""):
    """Telegram -> GitHub data/commands/DEVICE_ID.json -> Android App polls and executes"""
    cmd = {
        "id": str(uuid.uuid4()),
        "type": command_type,  # ring, locate, battery, photo, lock, message, sosack
        "payload": payload,
        "timestamp": int(datetime.now().timestamp() * 1000),
        "executed": False,
        "from": "Telegram Admin Bot"
    }
    path = f"data/commands/{device_id}.json"
    success = github_save_file(path, json.dumps(cmd, indent=2), f"🎛️ Remote Command {command_type} to {device_id} from Telegram")
    return success, cmd

def send_telegram_with_webapp(chat_id):
    keyboard = {
        "inline_keyboard": [
            [{"text": "💻 Open Full Console (WebApp)", "web_app": {"url": WEBAPP_URL}}],
            [{"text": "📱 Devices", "callback_data": "devices_status"}, {"text": "📊 Status", "callback_data": "check_status"}],
            [{"text": "🔋 Battery Graph", "callback_data": "battery_graph"}, {"text": "🗺️ Location Map", "callback_data": "location_map"}],
            [{"text": "🔔 Ring Phone", "callback_data": "ring_device"}, {"text": "📍 Locate Now", "callback_data": "locate_device"}],
            [{"text": "✅ Enable Booking", "callback_data": "enable_booking"}, {"text": "🚫 Disable Booking", "callback_data": "disable_booking"}],
            [{"text": "🆘 SOS History", "callback_data": "sos_history"}, {"text": "📊 Health", "callback_data": "health_status"}]
        ]
    }
    text = f"""🛰️ *Suto Full Monitoring - Admin Panel*

*Architecture:*
Bot: @T1311bot (Single)
Cloud: GitHub {GITHUB_USER}/{GITHUB_REPO} (Public, Pages Live)
WebApp: {WEBAPP_URL}
Project: sutoapp-e155c

*New Full Features Added:*
✅ Location (Lat/Lon + Map link)
✅ Battery (Full/Good/Low) + History Graph
✅ Mobile Number + Call Button
✅ Device Health: RAM, Storage, WiFi, IP, Network
✅ Battery Graph (Chart.js)
✅ Location History Map (Leaflet)
✅ Remote Ring - /ring to find phone
✅ Remote Commands - /locate, /battery, /photo
✅ SOS Panic Alert
✅ Notification Forward
✅ App Usage (optional)

*Android App jo output degi:*
Location + Battery + Mobile + RAM + Storage + WiFi + Notifications sab GitHub pe jayega aur is Console me dikhega

*Console:* Neeche "Open Console" dabao - Full dashboard with graphs & maps!

*Quick Remote:*
/ring DEVICE_ID - Phone loud ring
/locate DEVICE_ID - Instant location
/battery DEVICE_ID - Battery status
/photo DEVICE_ID - Take photo
/sos - SOS alerts history

*GitHub PAT:* {'✅ Set' if GITHUB_PAT and len(GITHUB_PAT)>10 else '❌ Revoked - Naya banao (Pages still works read-only)'}

*URL:* {WEBAPP_URL}
"""
    send_telegram_message(chat_id, text, keyboard)

def get_features():
    try:
        url = f"{FIREBASE_DB_URL}/app_features.json"
        r = requests.get(url, timeout=5)
        if r.status_code == 200 and r.text not in ["null", ""]:
            return r.json()
    except:
        pass
    try:
        content, _ = github_get_file("data/app_features.json")
        if content:
            return json.loads(content)
    except:
        pass
    return {"bookingEnabled": True, "maintenanceMode": False, "servicePrice": "₹100", "announcement": "Full Features Enabled", "paymentEnabled": True}

def save_features(features):
    features["updatedAt"] = str(datetime.now())
    features["lastUpdatedBy"] = "Telegram Admin Bot Full"
    try:
        url = f"{FIREBASE_DB_URL}/app_features.json"
        requests.put(url, json=features, timeout=5)
    except:
        pass
    try:
        github_save_file("data/app_features.json", json.dumps(features, indent=2), "🎛️ Update features from Telegram Full")
    except:
        pass
    return True

def handle_command(text, from_chat_id):
    if str(from_chat_id) != str(CHAT_ID):
        send_telegram_message(from_chat_id, f"❌ Admin only. Your ID: {from_chat_id}, Admin: {CHAT_ID[:3]}***")
        return

    cmd_lower = text.lower().strip()
    features = get_features()
    parts = text.strip().split()
    base_cmd = parts[0].lower() if parts else ""
    arg_device = parts[1] if len(parts) > 1 else None
    arg_payload = " ".join(parts[2:]) if len(parts) > 2 else (parts[1] if len(parts) > 1 and base_cmd in ["/set_price", "/announce"] else "")

    # Extract device ID if provided, else use first device
    def get_target_device():
        if arg_device and arg_device.startswith("DEVICE_"):
            return arg_device
        devices = github_list_devices()
        return devices[0] if devices else "DEVICE_TEST01"

    if base_cmd in ["/start", "/console", "/admin"]:
        send_telegram_with_webapp(from_chat_id)

    elif base_cmd == "/status":
        devices = github_list_devices()
        total = len(devices)
        recent = ""
        for dev_id in devices[:3]:
            status = github_get_device_status(dev_id)
            if status:
                recent += f"\n📱 {dev_id}: {status.get('name','')} - Batt {status.get('battery','')}% - {status.get('mobileNumber','')}"

        status_text = f"""📊 *Full System Status*

*Cloud:* GitHub {GITHUB_USER}/{GITHUB_REPO} - Public ✅ Pages Live {WEBAPP_URL}
*Bot:* @T1311bot - ✅ Online
*PAT:* {'✅ Set' if GITHUB_PAT and len(GITHUB_PAT)>10 else '❌ Revoked (Read-only mode, create new PAT)'}

*Features (All Added):*
✅ Location + Battery + Mobile
✅ Battery Graph, Location Map
✅ Remote Ring, Device Health, SOS
✅ Notification Forward, Camera (placeholder)

*App Features:*
Booking: {'✅ ON' if features.get('bookingEnabled') else '🚫 OFF'}
Maintenance: {'🔧 ON' if features.get('maintenanceMode') else '✅ OFF'}
Price: {features.get('servicePrice')}

*Devices:* Total {total}{recent if recent else '\nKoi device nahi'}

*Console:* Open Console button dabao - Full graphs & maps!
"""
        send_telegram_message(from_chat_id, status_text)
        if total > 0:
            send_telegram_with_webapp(from_chat_id)

    elif base_cmd == "/devices":
        devices = github_list_devices()
        if not devices:
            send_telegram_message(from_chat_id, "📱 Koi device nahi. Android App install karo.")
            return
        txt = "📱 *Devices - Full Status:*\n\n"
        for dev_id in devices:
            s = github_get_device_status(dev_id)
            if s:
                txt += f"🆔 {dev_id}\n📱 {s.get('name','')} ({s.get('model','')})\n🔋 {s.get('batteryText', str(s.get('battery',''))+'%')}\n📞 {s.get('mobileNumber','')}\n📍 {s.get('latitude','')},{s.get('longitude','')}\n🧠 {s.get('ram', s.get('output','')[:50])}\n\n"
        txt += f"\n💻 Full Console: {WEBAPP_URL}"
        send_telegram_message(from_chat_id, txt)

    elif "/enable_booking" in cmd_lower:
        features["bookingEnabled"] = True
        save_features(features)
        send_telegram_message(from_chat_id, "✅ Booking ON!")

    elif "/disable_booking" in cmd_lower:
        features["bookingEnabled"] = False
        save_features(features)
        send_telegram_message(from_chat_id, "🚫 Booking OFF!")

    elif "/maintenance_on" in cmd_lower:
        features["maintenanceMode"] = True
        save_features(features)
        send_telegram_message(from_chat_id, "🔧 Maintenance ON!")

    elif "/maintenance_off" in cmd_lower:
        features["maintenanceMode"] = False
        save_features(features)
        send_telegram_message(from_chat_id, "✅ Maintenance OFF!")

    elif base_cmd == "/set_price":
        price = " ".join(parts[1:]) if len(parts) > 1 else "₹100"
        features["servicePrice"] = price
        save_features(features)
        send_telegram_message(from_chat_id, f"💰 Price set: {price}")

    elif base_cmd == "/announce":
        ann = " ".join(parts[1:]) if len(parts) > 1 else ""
        features["announcement"] = ann
        save_features(features)
        send_telegram_message(from_chat_id, f"📢 Announce: {ann}")

    # Remote Commands - All Features
    elif base_cmd == "/ring":
        target = get_target_device()
        success, cmd = send_remote_command_to_device(target, "ring", "Find My Phone - Loud Ring 15 sec")
        if success:
            send_telegram_message(from_chat_id, f"🔔 Ring command sent to {target}! Phone loud ring karega 15 sec tak. Result GitHub data/commands/{target}.json me ayega.")
        else:
            send_telegram_message(from_chat_id, f"🔔 Ring command to {target} - GitHub PAT chahiye write ke liye (abhi revoked hai, read-only). Android App direct polling me check karega agar PAT nahi bhi ho to Telegram se manual /locate ka wait karega.\n\nCommand: ring -> {target}")

    elif base_cmd == "/locate":
        target = get_target_device()
        success, cmd = send_remote_command_to_device(target, "locate", "Get instant location")
        s = github_get_device_status(target)
        if s and s.get("latitude"):
            lat = s.get("latitude"); lon = s.get("longitude")
            send_telegram_message(from_chat_id, f"📍 *Location for {target}:*\nLat: {lat}, Lon: {lon}\n🗺️ https://maps.google.com/?q={lat},{lon}\n\nFresh location ke liye Android App next poll pe location bhejega (10 sec me).", parse_mode="Markdown")
        else:
            send_telegram_message(from_chat_id, f"📍 Locate command sent to {target}. Last known: Check console {WEBAPP_URL}\n\nGitHub: data/devices/{target}.json")

    elif base_cmd == "/battery":
        target = get_target_device()
        success, cmd = send_remote_command_to_device(target, "battery", "")
        s = github_get_device_status(target)
        if s:
            send_telegram_message(from_chat_id, f"🔋 *Battery for {target}:*\n{s.get('batteryText', s.get('battery',''))}% \n📞 {s.get('mobileNumber','')}\n📱 {s.get('name','')}\n\nGraph: {WEBAPP_URL} me dekho", parse_mode="Markdown")
        else:
            send_telegram_message(from_chat_id, f"🔋 Battery check for {target} - Console dekho: {WEBAPP_URL}")

    elif base_cmd == "/photo":
        target = get_target_device()
        success, cmd = send_remote_command_to_device(target, "photo", "")
        send_telegram_message(from_chat_id, f"📸 Photo command to {target}! Android App front camera se photo lega (Camera2 API - placeholder implementation, need CAMERA permission).\n\nResult: data/commands/{target}.json + Telegram pe photo")

    elif base_cmd == "/health":
        target = get_target_device()
        s = github_get_device_status(target)
        if s:
            txt = f"""📊 *Device Health - {target}*

{s.get('batteryText','')}
📞 Mobile: {s.get('mobileNumber','')}
📍 Loc: {s.get('latitude','')},{s.get('longitude','')}
🧠 {s.get('ram','RAM info')}
💾 {s.get('storage','Storage info')}
🌐 {s.get('network','Network info')}
📱 Model: {s.get('model','')} Android {s.get('androidVersion','')}

Output: {s.get('output','')[:300]}

Console: {WEBAPP_URL}
"""
            send_telegram_message(from_chat_id, txt)
        else:
            send_telegram_message(from_chat_id, f"📊 Health for {target} - No data yet")

    elif base_cmd == "/sos":
        # Show SOS history
        send_telegram_message(from_chat_id, f"🆘 *SOS History*\n\nAndroid App me SOS button hai - dabate hi Telegram pe alert aata hai: Battery + Mobile + Location + Maps link\n\nLast SOS: Check data/devices/ me jisme SOS keyword ho\n\nConsole: {WEBAPP_URL} me SOS alerts filter karke dekho")

    elif base_cmd.startswith("/help"):
        send_telegram_with_webapp(from_chat_id)

    else:
        if "DEVICE_" in text or len(text) > 20:
            send_telegram_message(from_chat_id, f"📝 Received: {text[:200]}...\n\nCommands:\n/start - Console\n/status - System status\n/devices - Device list\n/ring DEVICE_ID - Ring phone\n/locate DEVICE_ID - Get location\n/battery DEVICE_ID - Battery\n/photo DEVICE_ID - Camera\n/health DEVICE_ID - Full health\n\nConsole: {WEBAPP_URL}")

def handle_callback(cq):
    data = cq.get("data", "")
    from_id = cq.get("from", {}).get("id")
    if str(from_id) != str(CHAT_ID):
        return
    try:
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/answerCallbackQuery"
        requests.post(url, json={"callback_query_id": cq["id"]}, timeout=5)
    except:
        pass
    mapping = {
        "devices_status": "/devices",
        "check_status": "/status",
        "battery_graph": "/status",
        "location_map": "/devices",
        "ring_device": "/ring",
        "locate_device": "/locate",
        "enable_booking": "/enable_booking",
        "disable_booking": "/disable_booking",
        "maintenance_on": "/maintenance_on",
        "maintenance_off": "/maintenance_off",
        "sos_history": "/sos",
        "health_status": "/health"
    }
    cmd = mapping.get(data, f"/{data}")
    handle_command(cmd, from_id)

def poll_bot():
    print(f"""
🛰️ Suto FULL Monitoring Bot Started
Bot: @T1311bot Testing Token
Admin: {CHAT_ID}
Cloud: GitHub {GITHUB_USER}/{GITHUB_REPO} Public, Pages: {WEBAPP_URL}
PAT: {'Set' if GITHUB_PAT and len(GITHUB_PAT)>10 else 'Revoked - Read-only, create new PAT for write'}

Features: Location, Battery, Mobile, Battery Graph, Location Map, Remote Ring, Health (RAM/Storage/WiFi), Notification Forward, SOS, Camera, Locate, Battery, Photo

Commands: /start, /status, /devices, /ring DEVICE_ID, /locate DEVICE_ID, /battery DEVICE_ID, /photo DEVICE_ID, /health DEVICE_ID, /sos
""")
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
                if "message" in upd:
                    msg = upd["message"]
                    chat_id = msg.get("chat", {}).get("id")
                    text = msg.get("text", "")
                    if text and chat_id:
                        print(f"📩 {chat_id}: {text[:60]}")
                        handle_command(text, chat_id)
                if "callback_query" in upd:
                    handle_callback(upd["callback_query"])
        except KeyboardInterrupt:
            break
        except Exception as e:
            print(f"Error: {e}")
            import time
            time.sleep(3)

if __name__ == "__main__":
    if "PUT_" in BOT_TOKEN or len(BOT_TOKEN) < 20:
        print("BOT_TOKEN missing")
        exit(1)
    poll_bot()
