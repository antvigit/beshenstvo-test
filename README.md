# Beshenstvo Test — автотесты для калькулятора прививок

Автотесты для сайта [beshenstvo.pro](https://beshenstvo.pro/) — проверка корректности отображения дат вакцинации.

---

## 🚀 Стек технологий

| Компонент | Технологии |
|-----------|------------|
| **Языки** | Java 11, Python 3.9+ |
| **Сборка** | Maven |
| **Тесты** | Selenium 4, JUnit 5 |
| **Отчёты** | Allure |
| **Архитектура** | Page Object Model + Page Factory, SOLID |
| **Логирование** | Log4j |
| **Контейнеризация окружения** | Docker Compose + Selenium Grid (Chrome + Firefox) |
| **Бот** | TeleBot, Flask, Requests (polling + health check) |
| **CI/CD** | GitHub Actions (матрица браузеров) |
| **Хостинг** | Render |

---

## 🧱 Архитектура проекта: ООП, POM, SOLID

Проект построен на принципах ООП, Page Object Model и SOLID:

- **ООП:** наследование (`VaccinationPage` расширяет `BasePage`), инкапсуляция, полиморфизм.
- **POM + Page Factory:** каждая страница — отдельный класс, элементы инициализируются через `@FindBy`.
- **SOLID:** Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion.

---

## 🐳 Docker и Selenium Grid

Для запуска тестов в контейнерах используется **Docker Compose**, который поднимает Selenium Hub и узлы для Chrome и Firefox.

```bash
docker compose up -d
```
Тесты подключаются к Grid через переменные browser и grid.url.
📊 Тесты

Основной тест — shouldValidateVaccinationDates:

    Проверяет наличие дат на странице.

    Проверяет формат ДД.ММ.ГГГГ.

    Проверяет хронологический порядок и интервалы (3, 4, 7, 16, 60 дней).

Тест	Статус
shouldValidateVaccinationDates	✅ Проходит
testValidNameCyrillic	❌ Падает (на сайте нет формы)
testEmptyName	❌ Падает
testNameWithSpecialChars	❌ Падает
testVeryLongName	❌ Падает
testInvalidDate	❌ Падает

##🤖 Telegram-бот

Бот работает в режиме polling с параллельным Flask-сервером для health check.

Команды:

    /start — приветствие.

    /run — запустить тесты в Chrome и Firefox.

Что приходит:

    Статус запуска (🔄)

    Ожидание завершения (⏳)

    Скриншот Allure-отчёта для каждого браузера + ссылка на полный отчёт.

    Финальное сообщение «✅ Все тесты завершены!» со ссылкой на отчёт.

▶️ Запуск

Локально:
```bash

mvn clean test
mvn allure:serve
```

Локально с Docker (Selenium Grid):
```bash

docker compose up -d
mvn clean test -Dbrowser=chrome -Dgrid.url=http://localhost:4444/wd/hub
mvn allure:serve
```

Через бота:

    Напиши боту /run.

    Дождись сообщения о завершении.

    Открой скриншот или ссылку на отчёт.

🚧 План по улучшению проекта
№	Задача	Приоритет	Статус
1	Закомментировать падающие тесты с пояснением (или удалить)	🔥 Высокий	⬜ Не сделано
2	Параллельный запуск тестов внутри одного браузера (JUnit 5)	🔥 Высокий	⬜ Не сделано
3	Контейнеризация самих тестов (Dockerfile + запуск в Docker)	🔥 Высокий	⬜ Не сделано
4	API-тесты (REST Assured) — отдельный модуль	📌 Средний	⬜ Не сделано
5	Выбор браузера через бота (/run chrome или /run firefox)	📌 Средний	⬜ Не сделано
6	Запуск по расписанию (cron в GitHub Actions)	📌 Средний	⬜ Не сделано
7	Видео запись тестов (Selenoid)	💡 Низкий	⬜ Не сделано
8	Интерактивные кнопки в боте (выбор браузера через кнопки)	💡 Низкий	⬜ Не сделано

##📜 История изменений

Полная история изменений проекта описана в файле CHANGELOG.md.

