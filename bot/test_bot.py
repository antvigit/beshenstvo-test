"""
Юнит-тесты логики bot.py.

Реальный Telegram/GitHub API не используется: сетевые методы TeleBot и
requests.post замоканы, но сам модуль bot.py импортируется как есть —
проверяются настоящие обработчики команд и callback-кнопок.
"""
import os
import sys
import types
from unittest.mock import MagicMock

import pytest

os.environ.setdefault("TELEGRAM_BOT_TOKEN", "123456:TEST-FAKE-TOKEN-FOR-CI")
os.environ.setdefault("GITHUB_TOKEN", "fake-github-token-for-ci")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import telebot  # noqa: E402

# remove_webhook() дёргается на уровне модуля при импорте bot.py — подменяем
# его ДО импорта, чтобы не улетать в реальный Telegram.
telebot.TeleBot.remove_webhook = MagicMock(return_value=True)

import bot as bot_module  # noqa: E402  (реальный код проекта)


@pytest.fixture(autouse=True)
def mocked_telegram_calls(monkeypatch):
    """Подменяет исходящие методы TeleBot на моки перед каждым тестом."""
    monkeypatch.setattr(
        bot_module.bot, "send_message",
        MagicMock(side_effect=lambda chat_id, text, **kw: types.SimpleNamespace(
            message_id=999, chat=types.SimpleNamespace(id=chat_id))),
    )
    monkeypatch.setattr(
        bot_module.bot, "reply_to",
        MagicMock(side_effect=lambda message, text, **kw: types.SimpleNamespace(message_id=998)),
    )
    monkeypatch.setattr(bot_module.bot, "edit_message_text", MagicMock())
    monkeypatch.setattr(bot_module.bot, "edit_message_reply_markup", MagicMock())
    monkeypatch.setattr(bot_module.bot, "delete_message", MagicMock())
    monkeypatch.setattr(bot_module.bot, "answer_callback_query", MagicMock())


def fake_call(callback_data, chat_id=555555, message_id=777):
    return types.SimpleNamespace(
        data=callback_data,
        id="cbq-1",
        from_user=types.SimpleNamespace(first_name="ТестПользователь"),
        message=types.SimpleNamespace(
            chat=types.SimpleNamespace(id=chat_id),
            message_id=message_id,
        ),
    )


def test_run_command_builds_keyboard_with_all_sections():
    fake_message = types.SimpleNamespace(chat=types.SimpleNamespace(id=555555))
    bot_module.run_tests(fake_message)

    reply_call = bot_module.bot.reply_to.call_args
    markup = reply_call.kwargs.get("reply_markup") or reply_call.args[-1]
    buttons = [btn for row in markup.keyboard for btn in row]

    assert len(buttons) == len(bot_module.TEST_SECTIONS)
    assert {b.callback_data for b in buttons} == {f"run:{k}" for k in bot_module.TEST_SECTIONS}


@pytest.mark.parametrize("section_key", list(bot_module.TEST_SECTIONS))
def test_section_choice_shows_browser_menu(section_key):
    call = fake_call(f"run:{section_key}", chat_id=333444)
    bot_module.handle_run_section(call)

    edit_call = bot_module.bot.edit_message_text.call_args
    markup = edit_call.kwargs.get("reply_markup")
    buttons = [btn for row in markup.keyboard for btn in row]

    assert len(buttons) == len(bot_module.TEST_BROWSERS)
    assert {b.callback_data for b in buttons} == {
        f"browser:{section_key}:{bkey}" for bkey in bot_module.TEST_BROWSERS
    }
    # requests.post не вызывается на этом шаге - только показывается меню браузера
    assert bot_module.bot.send_message.call_count == 0


def test_unknown_section_does_not_crash_or_show_browser_menu():
    call = fake_call("run:does-not-exist")
    bot_module.handle_run_section(call)  # не должно кинуть исключение

    assert bot_module.bot.edit_message_text.call_count == 0
    assert bot_module.bot.answer_callback_query.call_count >= 1


@pytest.mark.parametrize("callback_data,expected_class,expected_browser", [
    ("browser:all:both", "all", "all"),
    ("browser:vaccination:chrome", "VaccinationCalculatorTest", "chrome"),
    ("browser:rheumatology:firefox", "RheumatologyCalculatorTest", "firefox"),
    ("browser:radiology:both", "RadiologySiteTest", "all"),
    ("browser:api:chrome", "VaccinationApiTest", "chrome"),
])
def test_browser_choice_dispatches_correct_test_class_and_browser(
    monkeypatch, callback_data, expected_class, expected_browser
):
    mock_post = MagicMock(return_value=types.SimpleNamespace(status_code=204))
    mock_find_run = MagicMock(return_value=999888)
    mock_wait = MagicMock()
    monkeypatch.setattr(bot_module.requests, "post", mock_post)
    monkeypatch.setattr(bot_module, "find_dispatched_run_id", mock_find_run)
    monkeypatch.setattr(bot_module, "wait_for_result", mock_wait)

    call = fake_call(callback_data, chat_id=111222)
    bot_module.handle_browser_choice(call)

    assert mock_post.call_count == 1
    args, kwargs = mock_post.call_args
    url = args[0] if args else kwargs.get("url")
    payload = kwargs.get("json", {})

    assert url == (
        "https://api.github.com/repos/antvigit/beshenstvo-test"
        "/actions/workflows/run-tests.yml/dispatches"
    )
    assert payload["ref"] == "master"
    assert payload["inputs"]["test_class"] == expected_class
    assert payload["inputs"]["browser"] == expected_browser
    assert payload["inputs"]["chat_id"] == str(call.message.chat.id)

    assert mock_find_run.call_count == 1
    assert mock_wait.call_count == 1
    # wait_for_result должен получить id именно найденного запуска, а не
    # просто "что-нибудь" - это и есть весь смысл фикса точного трекинга.
    assert mock_wait.call_args.args[-1] == 999888


def test_browser_choice_reports_when_run_not_found(monkeypatch):
    mock_post = MagicMock(return_value=types.SimpleNamespace(status_code=204))
    mock_find_run = MagicMock(return_value=None)
    mock_wait = MagicMock()
    monkeypatch.setattr(bot_module.requests, "post", mock_post)
    monkeypatch.setattr(bot_module, "find_dispatched_run_id", mock_find_run)
    monkeypatch.setattr(bot_module, "wait_for_result", mock_wait)

    call = fake_call("browser:vaccination:chrome", chat_id=111222)
    bot_module.handle_browser_choice(call)

    assert mock_wait.call_count == 0


def test_unknown_browser_choice_does_not_crash_or_trigger_workflow(monkeypatch):
    mock_post = MagicMock()
    monkeypatch.setattr(bot_module.requests, "post", mock_post)

    call = fake_call("browser:vaccination:does-not-exist")
    bot_module.handle_browser_choice(call)  # не должно кинуть исключение

    assert mock_post.call_count == 0
    assert bot_module.bot.answer_callback_query.call_count >= 1


def test_github_api_error_does_not_start_waiting(monkeypatch):
    mock_post = MagicMock(return_value=types.SimpleNamespace(status_code=422))
    mock_wait = MagicMock()
    monkeypatch.setattr(bot_module.requests, "post", mock_post)
    monkeypatch.setattr(bot_module, "wait_for_result", mock_wait)

    call = fake_call("browser:vaccination:chrome")
    bot_module.handle_browser_choice(call)

    assert mock_wait.call_count == 0


def test_health_endpoint_returns_ok():
    client = bot_module.app.test_client()
    resp = client.get("/health")

    assert resp.status_code == 200
    assert resp.data == b"OK"
