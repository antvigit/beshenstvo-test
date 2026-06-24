import telebot
import requests
import os
import time

# Секреты из GitHub Secrets (на локальной машине можно заменить на переменные окружения)
BOT_TOKEN = os.environ.get('TELEGRAM_BOT_TOKEN')  # Токен бота
CHAT_ID = os.environ.get('TELEGRAM_CHAT_ID')      # Твой chat_id
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')     # GitHub Personal Access Token
REPO_OWNER = 'antvigit'
REPO_NAME = 'beshenstvo-test'
WORKFLOW_ID = 'run-tests.yml'  # Имя файла workflow в .github/workflows/

bot = telebot.TeleBot(BOT_TOKEN)

# При старте бота отправляем приветствие
@bot.message_handler(commands=['start'])
def send_welcome(message):
    bot.reply_to(message, "Привет! Я бот для запуска автотестов.\nНапиши /run, чтобы запустить тесты.")

# Команда для запуска тестов
@bot.message_handler(commands=['run'])
def run_tests(message):
    bot.reply_to(message, "🔄 Запускаю тесты...")

    # Запускаем GitHub Actions через API
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/workflows/{WORKFLOW_ID}/dispatches"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    payload = {"ref": "master"}

    response = requests.post(url, json=payload, headers=headers)

    if response.status_code == 204:
        bot.send_message(message.chat.id, "✅ Тесты успешно запущены! Подожди 2-3 минуты, я пришлю результат.")
        # Здесь можно добавить логику ожидания и отправки результатов, пока просто заглушка
        # В реальном проекте нужно сделать более сложную логику с отслеживанием статуса раннера
        bot.send_message(message.chat.id, "📊 Как только тесты закончатся, я пришлю отчёт. (Пока эта функция в разработке!)")
    else:
        bot.send_message(message.chat.id, f"❌ Ошибка при запуске: {response.status_code}\n{response.text}")

# Запуск бота
if __name__ == "__main__":
    # Устанавливаем вебхук (для Render) или запускаем polling (для локальной отладки)
    # Для Render используем webhook
    import sys
    if '--webhook' in sys.argv:
        bot.remove_webhook()
        bot.set_webhook(url="https://ваш-бот-на-render.com/webhook")
    else:
        bot.polling()