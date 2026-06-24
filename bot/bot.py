import telebot
import requests
import os
import time

BOT_TOKEN = os.environ.get('TELEGRAM_BOT_TOKEN')
CHAT_ID = os.environ.get('TELEGRAM_CHAT_ID')
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
REPO_OWNER = 'antvigit'
REPO_NAME = 'beshenstvo-test'
WORKFLOW_ID = 'run-tests.yml'

# ===== СБРОС ВЕБХУКА ПРИ СТАРТЕ =====
try:
    response = requests.post(f'https://api.telegram.org/bot{BOT_TOKEN}/deleteWebhook')
    print('Webhook deleted:', response.status_code, response.text)
except Exception as e:
    print('Failed to delete webhook:', e)

bot = telebot.TeleBot(BOT_TOKEN)

@bot.message_handler(commands=['start'])
def send_welcome(message):
    bot.reply_to(message, "Привет! Напиши /run, чтобы запустить тесты.")

@bot.message_handler(commands=['run'])
def run_tests(message):
    bot.reply_to(message, "🔄 Запускаю тесты...")

    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/workflows/{WORKFLOW_ID}/dispatches"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    payload = {"ref": "master"}
    response = requests.post(url, json=payload, headers=headers)

    if response.status_code != 204:
        bot.send_message(message.chat.id, f"❌ Ошибка при запуске: {response.status_code}")
        return

    bot.send_message(message.chat.id, "✅ Тесты запущены. Жду завершения...")
    wait_for_result(message)

def wait_for_result(message):
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/runs?branch=master&status=in_progress"
    headers = {"Authorization": f"token {GITHUB_TOKEN}"}

    for _ in range(30):
        time.sleep(10)
        response = requests.get(url, headers=headers)
        runs = response.json()

        if runs.get('total_count', 0) == 0:
            bot.send_message(message.chat.id, "✅ Тесты завершены!")
            bot.send_message(message.chat.id, f"📊 Отчёт: https://github.com/{REPO_OWNER}/{REPO_NAME}/actions")
            return

    bot.send_message(message.chat.id, "⏰ Тесты всё ещё выполняются. Проверь результат вручную.")

if __name__ == "__main__":
    bot.polling()