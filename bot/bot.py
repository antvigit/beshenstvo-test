import telebot
from telebot import types
import requests
import os
import time
from flask import Flask

BOT_TOKEN = os.environ.get('TELEGRAM_BOT_TOKEN')
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
REPO_OWNER = 'antvigit'
REPO_NAME = 'beshenstvo-test'
WORKFLOW_ID = 'run-tests.yml'

# Разделы сайта, доступные для выбора в /run: ключ callback-данных -> (подпись
# кнопки, имя JUnit-класса для input'а test_class воркфлоу run-tests.yml).
TEST_SECTIONS = {
    'all': ('🌐 Все разделы', 'all'),
    'vaccination': ('💉 Вакцинация', 'VaccinationCalculatorTest'),
    'rheumatology': ('🦴 Ревматология', 'RheumatologyCalculatorTest'),
    'radiology': ('🩻 Рентгенология', 'RadiologySiteTest'),
}

bot = telebot.TeleBot(BOT_TOKEN)
app = Flask(__name__)

try:
    bot.remove_webhook()
    print("✅ Webhook removed on startup")
except Exception as e:
    print(f"⚠️ Failed to remove webhook: {e}")

@app.route('/health', methods=['GET'])
def health():
    return 'OK', 200

# === SELF-PING (чтобы Render не засыпал) ===
def self_ping():
    import time
    import requests
    while True:
        time.sleep(120)
        try:
            requests.get('http://localhost:10000/health')
            print("💓 Self-ping sent")
        except Exception as e:
            print(f"⚠️ Self-ping failed: {e}")

import threading
threading.Thread(target=self_ping, daemon=True).start()

def update_progress(chat_id, message_id, progress, text):
    bar_length = 10
    filled = int(progress / 100 * bar_length)
    bar = '█' * filled + '░' * (bar_length - filled)
    new_text = f"{text}\n[{bar}] {progress}%"
    try:
        bot.edit_message_text(new_text, chat_id, message_id)
    except Exception as e:
        print(f"Error updating progress: {e}")

def delete_progress(chat_id, message_id):
    try:
        bot.delete_message(chat_id, message_id)
        print(f"✅ Progress message deleted for {chat_id}")
    except Exception as e:
        print(f"⚠️ Could not delete progress message: {e}")

@bot.message_handler(commands=['start'])
def send_welcome(message):
    name = message.from_user.first_name
    bot.reply_to(message, f"Привет, {name}! 👋\nЯ бот для запуска автотестов.\nНапиши /run, чтобы выбрать раздел сайта и запустить тесты.")

@bot.message_handler(commands=['run'])
def run_tests(message):
    markup = types.InlineKeyboardMarkup(row_width=1)
    for key, (label, _) in TEST_SECTIONS.items():
        markup.add(types.InlineKeyboardButton(label, callback_data=f"run:{key}"))
    bot.reply_to(message, "Какой раздел сайта протестировать?", reply_markup=markup)

@bot.callback_query_handler(func=lambda call: call.data.startswith('run:'))
def handle_run_section(call):
    key = call.data.split(':', 1)[1]
    section = TEST_SECTIONS.get(key)
    if section is None:
        bot.answer_callback_query(call.id, "Неизвестный раздел, попробуйте /run ещё раз")
        return

    label, test_class = section
    name = call.from_user.first_name
    chat_id = call.message.chat.id

    bot.answer_callback_query(call.id, f"Запускаю: {label}")
    try:
        bot.edit_message_reply_markup(chat_id, call.message.message_id, reply_markup=None)
    except Exception as e:
        print(f"⚠️ Could not clear keyboard: {e}")

    progress_msg = bot.send_message(chat_id, f"⏳ Подготовка к запуску ({label})... (примерное время ожидания: до 5 минут)")
    update_progress(chat_id, progress_msg.message_id, 0, f"⏳ Подготовка к запуску ({label})...")

    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/workflows/{WORKFLOW_ID}/dispatches"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    payload = {
        "ref": "master",
        "inputs": {
            "chat_id": str(chat_id),
            "test_class": test_class
        }
    }
    response = requests.post(url, json=payload, headers=headers)

    if response.status_code != 204:
        update_progress(chat_id, progress_msg.message_id, 100, f"❌ Ошибка при запуске: {response.status_code}")
        return

    update_progress(chat_id, progress_msg.message_id, 10, f"🚀 Тесты запущены ({label}), ожидание завершения... (примерное время: до 5 минут)")
    wait_for_result(chat_id, progress_msg.message_id, name, label)

def wait_for_result(chat_id, message_id, name, label="все разделы"):
    total_time = 300  # 5 минут
    start_time = time.time()
    progress = 10
    step = 5

    # Сначала даём GitHub время на запуск workflow (20 секунд)
    time.sleep(20)

    status_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/runs?branch=master&status=in_progress"
    queued_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/runs?branch=master&status=queued"
    headers = {"Authorization": f"token {GITHUB_TOKEN}"}

    while True:
        elapsed = time.time() - start_time

        try:
            # Проверяем выполняющиеся и поставленные в очередь запуски
            response_in = requests.get(status_url, headers=headers)
            in_progress = response_in.json().get('total_count', 0)

            response_q = requests.get(queued_url, headers=headers)
            queued = response_q.json().get('total_count', 0)

            # Если нет ни выполняющихся, ни поставленных в очередь — значит всё завершено
            if in_progress == 0 and queued == 0:
                update_progress(chat_id, message_id, 95, "📊 Тесты завершены, ожидаю скриншоты...")
                time.sleep(10)
                update_progress(chat_id, message_id, 100, "✅ Все тесты завершены!")
                delete_progress(chat_id, message_id)
                bot.send_message(
                    chat_id,
                    f"✅ Тесты завершены ({label}), {name}!\n"
                    f"📊 Отчёт: https://github.com/{REPO_OWNER}/{REPO_NAME}/actions\n"
                    f"💡 Можете повторить запрос командой /run"
                )
                return
        except Exception as e:
            print(f"Error checking status: {e}")

        if elapsed >= total_time:
            update_progress(chat_id, message_id, 95, "⏰ Время ожидания истекло, проверьте результат вручную...")
            time.sleep(5)
            update_progress(chat_id, message_id, 100, "⏰ Проверьте результат вручную")
            delete_progress(chat_id, message_id)
            bot.send_message(
                chat_id,
                f"⏰ {name}, тесты ({label}) всё ещё выполняются. Проверь результат вручную:\n"
                f"📊 https://github.com/{REPO_OWNER}/{REPO_NAME}/actions"
            )
            return

        time.sleep(10)
        progress += step
        if progress > 95:
            progress = 95
        update_progress(chat_id, message_id, progress, f"⏳ Выполнение тестов... (примерное время: до 5 минут)")

if __name__ == '__main__':
    try:
        bot.remove_webhook()
        print("✅ Webhook removed before polling")
    except Exception as e:
        print(f"⚠️ Failed to remove webhook: {e}")

    print("🤖 Бот запущен в режиме polling")
    import threading
    threading.Thread(target=app.run, kwargs={'host': '0.0.0.0', 'port': int(os.environ.get('PORT', 5000))}, daemon=True).start()
    bot.polling()