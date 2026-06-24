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

bot = telebot.TeleBot(BOT_TOKEN)

def is_authorized(message):
    return str(message.chat.id) == CHAT_ID

@bot.message_handler(commands=['start'])
def send_welcome(message):
    if not is_authorized(message):
        bot.reply_to(message, "❌ У вас нет доступа к этому боту.")
        return
    name = message.from_user.first_name
    bot.reply_to(message, f"Привет, {name}! 👋\nЯ бот для запуска автотестов.\nНапиши /run, чтобы запустить тесты.")

@bot.message_handler(commands=['run'])
def run_tests(message):
    if not is_authorized(message):
        bot.reply_to(message, "❌ У вас нет доступа к этому боту.")
        return

    name = message.from_user.first_name
    bot.reply_to(message, f"🔄 {name}, запускаю тесты...")

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

    bot.send_message(message.chat.id, f"⏳ {name}, тесты запущены. Жду завершения...\nЭто может занять 2–3 минуты.")

    wait_for_result(message, name)

def wait_for_result(message, name):
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/runs?branch=master&status=in_progress"
    headers = {"Authorization": f"token {GITHUB_TOKEN}"}

    for _ in range(30):
        time.sleep(10)
        response = requests.get(url, headers=headers)
        runs = response.json()

        if runs.get('total_count', 0) == 0:
            bot.send_message(message.chat.id, f"📸 {name}, скриншот отчёта уже в пути...")
            return

    bot.send_message(message.chat.id, f"⏰ {name}, тесты всё ещё выполняются. Проверь результат вручную:\nhttps://github.com/{REPO_OWNER}/{REPO_NAME}/actions")

if __name__ == "__main__":
    bot.polling()