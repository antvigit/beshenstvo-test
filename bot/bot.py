import telebot
from telebot import types
import requests
import os
import time
from datetime import datetime, timezone
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
    'api': ('🔌 API вакцинации', 'VaccinationApiTest'),
}

# Браузеры, доступные для выбора на втором шаге /run: ключ callback-данных ->
# (подпись кнопки, значение input'а browser воркфлоу run-tests.yml).
TEST_BROWSERS = {
    'both': ('🌐 Chrome + Firefox', 'all'),
    'chrome': ('🖥 Только Chrome', 'chrome'),
    'firefox': ('🦊 Только Firefox', 'firefox'),
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

    label, _ = section
    chat_id = call.message.chat.id

    bot.answer_callback_query(call.id, f"Раздел: {label}")

    markup = types.InlineKeyboardMarkup(row_width=1)
    for bkey, (blabel, _) in TEST_BROWSERS.items():
        markup.add(types.InlineKeyboardButton(blabel, callback_data=f"browser:{key}:{bkey}"))
    try:
        bot.edit_message_text(
            f"Раздел: {label}\nВ каком браузере запустить?",
            chat_id, call.message.message_id,
            reply_markup=markup
        )
    except Exception as e:
        print(f"⚠️ Could not show browser choice: {e}")
        bot.send_message(chat_id, f"Раздел: {label}\nВ каком браузере запустить?", reply_markup=markup)

@bot.callback_query_handler(func=lambda call: call.data.startswith('browser:'))
def handle_browser_choice(call):
    _, section_key, browser_key = call.data.split(':', 2)
    section = TEST_SECTIONS.get(section_key)
    browser_choice = TEST_BROWSERS.get(browser_key)
    if section is None or browser_choice is None:
        bot.answer_callback_query(call.id, "Что-то пошло не так, попробуйте /run ещё раз")
        return

    label, test_class = section
    browser_label, browser_value = browser_choice
    full_label = f"{label}, {browser_label}"
    name = call.from_user.first_name
    chat_id = call.message.chat.id

    bot.answer_callback_query(call.id, f"Запускаю: {full_label}")
    try:
        bot.edit_message_reply_markup(chat_id, call.message.message_id, reply_markup=None)
    except Exception as e:
        print(f"⚠️ Could not clear keyboard: {e}")

    progress_msg = bot.send_message(chat_id, f"⏳ Подготовка к запуску ({full_label})... (примерное время ожидания: до 5 минут)")
    update_progress(chat_id, progress_msg.message_id, 0, f"⏳ Подготовка к запуску ({full_label})...")

    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/workflows/{WORKFLOW_ID}/dispatches"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    payload = {
        "ref": "master",
        "inputs": {
            "chat_id": str(chat_id),
            "test_class": test_class,
            "browser": browser_value
        }
    }
    # workflow_dispatch не возвращает id созданного запуска напрямую (ограничение
    # GitHub API) - запоминаем момент отправки, чтобы затем найти именно свой run,
    # а не полагаться на "нет вообще никаких активных запусков в репозитории",
    # что путается, если кто-то ещё запустит /run параллельно.
    dispatch_time = datetime.now(timezone.utc)
    response = requests.post(url, json=payload, headers=headers)

    if response.status_code != 204:
        update_progress(chat_id, progress_msg.message_id, 100, f"❌ Ошибка при запуске: {response.status_code}")
        return

    update_progress(chat_id, progress_msg.message_id, 5, f"🚀 Тесты запущены ({full_label}), ищу запуск...")
    run_id = find_dispatched_run_id(dispatch_time)
    if run_id is None:
        update_progress(
            chat_id, progress_msg.message_id, 100,
            "⚠️ Не удалось найти запущенный workflow. Проверьте вручную:"
        )
        delete_progress(chat_id, progress_msg.message_id)
        bot.send_message(chat_id, f"📊 https://github.com/{REPO_OWNER}/{REPO_NAME}/actions")
        return

    update_progress(chat_id, progress_msg.message_id, 10, f"🚀 Тесты запущены ({full_label}), ожидание завершения... (примерное время: до 5 минут)")
    wait_for_result(chat_id, progress_msg.message_id, name, full_label, run_id)

def find_dispatched_run_id(dispatch_time, attempts=10, delay=2):
    """Находит id только что созданного запуска workflow_dispatch, а не любого
    активного запуска в репозитории - иначе бот путает статус своего запуска
    с чужим, если кто-то ещё нажмёт /run примерно в то же время."""
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/workflows/{WORKFLOW_ID}/runs"
    headers = {"Authorization": f"token {GITHUB_TOKEN}"}
    params = {"event": "workflow_dispatch", "branch": "master", "per_page": 5}

    for _ in range(attempts):
        try:
            resp = requests.get(url, headers=headers, params=params)
            for run in resp.json().get("workflow_runs", []):
                created_at = datetime.fromisoformat(run["created_at"].replace("Z", "+00:00"))
                if created_at >= dispatch_time:
                    return run["id"]
        except Exception as e:
            print(f"Error finding dispatched run: {e}")
        time.sleep(delay)
    return None

def wait_for_result(chat_id, message_id, name, label, run_id):
    total_time = 300  # 5 минут
    start_time = time.time()
    progress = 10
    step = 5

    run_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/runs/{run_id}"
    headers = {"Authorization": f"token {GITHUB_TOKEN}"}

    while True:
        elapsed = time.time() - start_time

        try:
            run = requests.get(run_url, headers=headers).json()
            status = run.get("status")  # queued | in_progress | completed

            if status == "completed":
                conclusion = run.get("conclusion")  # success | failure | cancelled | ...
                icon = "✅" if conclusion == "success" else "❌"
                conclusion_text = {
                    "success": "все тесты прошли",
                    "failure": "есть упавшие тесты или ошибка сборки",
                    "cancelled": "запуск отменён",
                }.get(conclusion, f"статус: {conclusion}")

                update_progress(chat_id, message_id, 95, "📊 Тесты завершены, ожидаю скриншоты...")
                time.sleep(10)
                update_progress(chat_id, message_id, 100, f"{icon} Готово!")
                delete_progress(chat_id, message_id)
                bot.send_message(
                    chat_id,
                    f"{icon} Тесты завершены ({label}), {name}!\n"
                    f"{conclusion_text}\n"
                    f"📊 Отчёт: {run.get('html_url', f'https://github.com/{REPO_OWNER}/{REPO_NAME}/actions')}\n"
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
                f"📊 https://github.com/{REPO_OWNER}/{REPO_NAME}/actions/runs/{run_id}"
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