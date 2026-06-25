import requests
import os
import time
import json
from flask import Flask, request

BOT_TOKEN = os.environ.get('TELEGRAM_BOT_TOKEN')
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
REPO_OWNER = 'antvigit'
REPO_NAME = 'beshenstvo-test'
WORKFLOW_ID = 'run-tests.yml'

app = Flask(__name__)

# ---- Отправка сообщений в Telegram ----
def send_message(chat_id, text):
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
    payload = {"chat_id": chat_id, "text": text}
    requests.post(url, json=payload)

# ---- Обработчики команд ----
def handle_start(chat_id, first_name):
    send_message(chat_id, f"Привет, {first_name}! 👋\nЯ бот для запуска автотестов.\nНапиши /run, чтобы запустить тесты.")

def handle_run(chat_id, first_name):
    send_message(chat_id, f"🔄 {first_name}, запускаю тесты...")

    gh_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/workflows/{WORKFLOW_ID}/dispatches"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    gh_payload = {
        "ref": "master",
        "inputs": {
            "chat_id": str(chat_id)
        }
    }
    response = requests.post(gh_url, json=gh_payload, headers=headers)

    if response.status_code != 204:
        send_message(chat_id, f"❌ Ошибка при запуске: {response.status_code}")
        return

    send_message(chat_id, f"⏳ {first_name}, тесты запущены. Жду завершения...\nЭто может занять 2–3 минуты.")
    wait_for_result(chat_id, first_name)

def wait_for_result(chat_id, first_name):
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/runs?branch=master&status=in_progress"
    headers = {"Authorization": f"token {GITHUB_TOKEN}"}
    for _ in range(30):
        time.sleep(10)
        response = requests.get(url, headers=headers)
        runs = response.json()
        if runs.get('total_count', 0) == 0:
            return
    send_message(chat_id, f"⏰ {first_name}, тесты всё ещё выполняются. Проверь результат вручную:\nhttps://github.com/{REPO_OWNER}/{REPO_NAME}/actions")

# ---- Установка вебхука ----
def set_webhook():
    webhook_url = os.environ.get('RENDER_EXTERNAL_URL', 'https://beshenstvo-test-bot.onrender.com') + '/webhook'
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/setWebhook?url={webhook_url}&drop_pending_updates=true"
    response = requests.get(url)
    print(f"✅ Webhook set: {response.json()}")

set_webhook()

# ---- Эндпоинты Flask ----
@app.route('/health', methods=['GET'])
def health():
    return 'OK', 200

@app.route('/webhook', methods=['POST'])
def webhook():
    if request.headers.get('content-type') == 'application/json':
        data = request.get_json()
        print("📩 Webhook received:", json.dumps(data, indent=2))
        if 'message' in data:
            message = data['message']
            chat_id = message['chat']['id']
            first_name = message['from'].get('first_name', 'User')
            if 'text' in message:
                text = message['text']
                if text == '/start':
                    handle_start(chat_id, first_name)
                elif text == '/run':
                    handle_run(chat_id, first_name)
                else:
                    send_message(chat_id, "Неизвестная команда. Доступны: /start, /run")
        return 'OK', 200
    return 'Bad Request', 400

if __name__ == '__main__':
    # Gunicorn запускает app
    pass