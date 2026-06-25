import telebot
import requests
import os
import time
from flask import Flask, request

BOT_TOKEN = os.environ.get('TELEGRAM_BOT_TOKEN')
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
REPO_OWNER = 'antvigit'
REPO_NAME = 'beshenstvo-test'
WORKFLOW_ID = 'run-tests.yml'

bot = telebot.TeleBot(BOT_TOKEN)
app = Flask(__name__)

# ---- Установка вебхука при старте ----
def set_webhook():
    webhook_url = os.environ.get('RENDER_EXTERNAL_URL', 'https://beshenstvo-test-bot.onrender.com') + '/webhook'
    try:
        bot.remove_webhook()
        bot.set_webhook(url=webhook_url)
        print(f'✅ Webhook set to {webhook_url}')
    except Exception as e:
        print(f'❌ Failed to set webhook: {e}')

set_webhook()  # выполняется при импорте

@app.route('/health', methods=['GET'])
def health():
    return 'OK', 200

@bot.message_handler(commands=['start'])
def send_welcome(message):
    print(f"🔥 /start from {message.from_user.first_name} (id: {message.chat.id})")
    try:
        name = message.from_user.first_name
        bot.reply_to(message, f"Привет, {name}! 👋\nЯ бот для запуска автотестов.\nНапиши /run, чтобы запустить тесты.")
    except Exception as e:
        print(f"Error in start: {e}")

@bot.message_handler(commands=['run'])
def run_tests(message):
    print(f"🔥 /run from {message.from_user.first_name} (id: {message.chat.id})")
    try:
        name = message.from_user.first_name
        bot.reply_to(message, f"🔄 {name}, запускаю тесты...")

        chat_id = str(message.chat.id)

        url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/workflows/{WORKFLOW_ID}/dispatches"
        headers = {
            "Authorization": f"token {GITHUB_TOKEN}",
            "Accept": "application/vnd.github.v3+json"
        }
        payload = {
            "ref": "master",
            "inputs": {
                "chat_id": chat_id
            }
        }
        response = requests.post(url, json=payload, headers=headers)

        if response.status_code != 204:
            bot.send_message(message.chat.id, f"❌ Ошибка при запуске: {response.status_code}")
            return

        bot.send_message(message.chat.id, f"⏳ {name}, тесты запущены. Жду завершения...\nЭто может занять 2–3 минуты.")

        wait_for_result(message, name)
    except Exception as e:
        print(f"Error in run: {e}")
        bot.send_message(message.chat.id, f"❌ Ошибка: {e}")

def wait_for_result(message, name):
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/runs?branch=master&status=in_progress"
    headers = {"Authorization": f"token {GITHUB_TOKEN}"}

    for _ in range(30):
        time.sleep(10)
        try:
            response = requests.get(url, headers=headers)
            runs = response.json()
            if runs.get('total_count', 0) == 0:
                return
        except Exception as e:
            print(f"Error waiting: {e}")
            break

    bot.send_message(message.chat.id, f"⏰ {name}, тесты всё ещё выполняются. Проверь результат вручную:\nhttps://github.com/{REPO_OWNER}/{REPO_NAME}/actions")

@app.route('/webhook', methods=['POST'])
def webhook():
    try:
        if request.headers.get('content-type') == 'application/json':
            json_string = request.get_data().decode('utf-8')
            print("📩 Webhook data received")
            update = telebot.types.Update.de_json(json_string)
            if update.message:
                print(f"📩 Message from {update.message.from_user.first_name}: {update.message.text}")
                bot.process_new_messages([update.message])
            else:
                print("📩 No message in update")
                bot.process_new_updates([update])
            return 'OK', 200
        return 'Bad Request', 400
    except Exception as e:
        print(f"❌ Webhook error: {e}")
        return 'Error', 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)))