#!/usr/bin/env python3
"""
Telegram Bot Test + Secure Token Helper
Aapka purana token leak ho gaya, is script se naya token test karo
"""
import requests
import os

# Yahan apna NAYA token (revoke ke baad wala) dalo test ke liye
BOT_TOKEN = os.getenv("BOT_TOKEN", "PUT_YOUR_NEW_TOKEN_HERE")
CHAT_ID = os.getenv("CHAT_ID", "289240360")

def test_bot():
    if "YOUR_NEW" in BOT_TOKEN or len(BOT_TOKEN) < 20:
        print("❌ Pehle BOT_TOKEN set karo!")
        print("   export BOT_TOKEN='your_new_token'")
        print("   export CHAT_ID='289240360'")
        return
    
    # getMe test
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/getMe"
    print(f"🔍 Bot check kar raha hu: {url.split('/bot')[0]}/bot***.../getMe")
    try:
        r = requests.get(url, timeout=10)
        data = r.json()
        if data.get("ok"):
            bot = data["result"]
            print(f"✅ Bot Connected: @{bot['username']} ({bot['first_name']})")
        else:
            print(f"❌ Bot Error: {data}")
            return
    except Exception as e:
        print(f"❌ Connection fail: {e}")
        return

    # sendMessage test
    msg = """🔔 Test Booking - Fixed App se

👤 Name: Test User
📞 Phone: 9999999999
🛠 Service: Bike Repair
✅ Ye message naye fixed app se aaya hai!
"""
    try:
        url2 = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        r2 = requests.post(url2, data={
            "chat_id": CHAT_ID,
            "text": msg,
            "parse_mode": "Markdown"
        }, timeout=10)
        print(f"📨 Send result: {r2.json()}")
        if r2.json().get("ok"):
            print("✅ Telegram pe message chala gaya! Check karo.")
    except Exception as e:
        print(f"❌ Send fail: {e}")

if __name__ == "__main__":
    print("=== Telegram Bot Security Check ===")
    print(f"Chat ID: {CHAT_ID}")
    test_bot()
    print("\n⚠️ Yaad rahe: Token ko kabhi GitHub ya public me share mat karo!")
