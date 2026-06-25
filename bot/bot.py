import telebot
import requests
import os
import time
from flask import Flask

BOT_TOKEN = os.environ.get('TELEGRAM_BOT_TOKEN')
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
REPO_OWNER = 'antvigit'
REPO_NAME = 'beshenstvo-test'
WORKFLOW_ID = 'run-tests.yml'

bot = telebot.TeleBot(BOT_TOKEN)
app = Flask(__name__)

@app.route('/health', methods=['GET'])
def health():
    return 'OK', 200

def update_progress(chat_id, message_id, progress, text):
    """Обновляет сообщение с прогресс-баром."""
    bar_length = 10
    filled = int(progress / 100 * bar_length)
    bar = '█' * filled + '░' * (bar_length - filled)
    new_text = f"{text}\n[{bar}] {progress}%"
    try:
        bot.edit_message_text(new_text, chat_id, message_id)
    except Exception as e:
        print(f"Error updating progress: {e}")

@bot.message_handler(commands=['start'])
def send_welcome(message):
    name = message.from_user.first_name
    bot.reply_to(message, f"Привет, {name}! 👋\nЯ бот для запуска автотестов.\nНапиши /run, чтобы запустить тесты.")

@bot.message_handler(commands=['run'])
def run_tests(message):
    name = message.from_user.first_name
    chat_id = message.chat.id

    # Отправляем начальное сообщение с прогресс-баром
    progress_msg = bot.reply_to(message, "🔄 Запускаю тесты...")
    update_progress(chat_id, progress_msg.message_id, 0, "🔄 Подготовка к запуску...")

    # Запускаем GitHub Actions
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/workflows/{WORKFLOW_ID}/dispatches"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    payload = {
        "ref": "master",
        "inputs": {
            "chat_id": str(chat_id)
        }
    }
    response = requests.post(url, json=payload, headers=headers)

    if response.status_code != 204:
        update_progress(chat_id, progress_msg.message_id, 100, f"❌ Ошибка при запуске: {response.status_code}")
        return

    # Обновляем прогресс после успешного запуска
    update_progress(chat_id, progress_msg.message_id, 10, "⏳ Тесты запущены, ожидание завершения...")

    # Ждём завершения
    wait_for_result(chat_id, progress_msg.message_id, name)

def wait_for_result(chat_id, message_id, name):
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/runs?branch=master&status=in_progress"
    headers = {"Authorization": f"token {GITHUB_TOKEN}"}

    progress = 10
    for _ in range(30):
        time.sleep(10)
        try:
            response = requests.get(url, headers=headers)
            runs = response.json()
            if runs.get('total_count', 0) == 0:
                # Тесты завершены
                update_progress(chat_id, message_id, 100, "✅ Тесты завершены!")
                return
        except Exception as e:
            print(f"Error checking status: {e}")

        # Имитация прогресса (не более 95%)
        progress += 5
        if progress > 95:
            progress = 95
        update_progress(chat_id, message_id, progress, f"⏳ Выполнение тестов... {progress}%")

    # Если цикл закончился (таймаут)
    update_progress(chat_id, message_id, 100, "⏰ Тесты всё ещё выполняются. Проверь вручную.")
    bot.send_message(chat_id, f"⏰ {name}, проверь результат вручную:\nhttps://github.com/{REPO_OWNER}/{REPO_NAME}/actions")

if __name__ == '__main__':
    bot.remove_webhook()
    print("🤖 Бот запущен в режиме polling")
    import threading
    threading.Thread(target=app.run, kwargs={'host': '0.0.0.0', 'port': int(os.environ.get('PORT', 5000))}, daemon=True).start()
    bot.polling()