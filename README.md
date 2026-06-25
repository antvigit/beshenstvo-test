# Beshenstvo Test — автотесты для калькулятора прививок

Автотесты для сайта [beshenstvo.pro](https://beshenstvo.pro/) — проверка корректности отображения дат вакцинации.

---

## Стек технологий

- **Языки:** Java 11, Python 3.9+
- **Сборка:** Maven
- **Тесты:** Selenium 4, JUnit 5 (параллельный запуск)
- **Отчёты:** Allure
- **Архитектура:** Page Object Model + Page Factory, SOLID
- **Логирование:** Log4j
- **Контейнеризация:** Docker (Dockerfile для тестов)
- **Бот:** TeleBot, Flask, Requests (вебхук)
- **Контейнеризация окружения:** Docker Compose + Selenium Grid (Chrome + Firefox)
- **CI/CD:** GitHub Actions (матрица браузеров, контейнерный запуск)
- **Хостинг:** Render

---

## Архитектура проекта: ООП, POM, SOLID

### Объектно-ориентированное программирование

- **Наследование:** `VaccinationPage` расширяет `BasePage`, который содержит общую логику для всех страниц (драйвер, ожидания).
- **Инкапсуляция:** Все элементы страницы (`WebElement`) объявлены как `private`. Доступ к ним — только через публичные методы.
- **Полиморфизм:** Используются интерфейсы `IPage` и `WebDriver` для гибкой замены реализаций (например, `ChromeDriver` на `FirefoxDriver`).

### Page Object Model + Page Factory

- Каждая страница — отдельный класс (`VaccinationPage`), который хранит локаторы и методы взаимодействия.
- Для инициализации элементов используется паттерн **Page Factory** с аннотацией `@FindBy`.

### Принципы SOLID в проекте

| Принцип | Что означает | Как реализован в проекте |
| :--- | :--- | :--- |
| **S** (Single Responsibility) | У класса должна быть только одна причина для изменения. | `VaccinationPage` отвечает за логику страницы, `PageLocators` — за локаторы, `VaccinationCalculatorTest` — за сценарии тестов. |
| **O** (Open/Closed) | Классы должны быть открыты для расширения, но закрыты для модификации. | Можно добавить новую страницу (`ResultPage`), создав класс-наследник `BasePage`, не изменяя существующие классы. |
| **L** (Liskov Substitution) | Объекты в программе должны быть заменяемы на экземпляры их подтипов. | Используем интерфейс `WebDriver`. Можно заменить `ChromeDriver` на `FirefoxDriver` без изменения кода тестов. |
| **I** (Interface Segregation) | Не следует создавать «толстые» интерфейсы. | Интерфейс `IPage` содержит только два метода: `open()` и `waitForPageLoaded()`. |
| **D** (Dependency Inversion) | Зависимости должны строиться на абстракциях, а не на конкретиках. | Все страницы получают `WebDriver` через конструктор (`BasePage`), а не создают его сами. |

---
## Docker и Selenium Grid

Для запуска тестов в контейнерах используется **Docker Compose**, который поднимает Selenium Hub и узлы для Chrome и Firefox.

```bash
docker compose up -d
```
Тесты подключаются к Grid через переменные browser и grid.url.
Сами тесты также контейнеризированы — для их запуска используется Dockerfile:
docker build -t beshenstvo-tests .
docker run --network="host" -e browser=chrome -e grid.url=http://localhost:4444/wd/hub beshenstvo-tests
---
##Параллельный запуск
JUnit 5 настроен на параллельное выполнение тестов внутри одного браузера. Конфигурация лежит в src/test/resources/junit-platform.properties.

##Логирование

Вместо System.out.println используется Log4j. Настройки в src/main/resources/log4j2.xml.

junit.jupiter.execution.parallel.enabled = true
junit.jupiter.execution.parallel.mode.default = concurrent
## Тесты

Основной тест — `shouldValidateVaccinationDates`:
- Проверяет наличие дат на странице.
- Проверяет формат `ДД.ММ.ГГГГ`.
- Проверяет хронологический порядок и интервалы (3, 4, 7, 16, 60 дней).

| Тест | Статус |
| :--- | :--- |
| `shouldValidateVaccinationDates` | ✅ Проходит |
| `testValidNameCyrillic` | ❌ Падает |
| `testEmptyName` | ❌ Падает |
| `testNameWithSpecialChars` | ❌ Падает |
| `testVeryLongName` | ❌ Падает |
| `testInvalidDate` | ❌ Падает |
---

## Telegram-бот

Бот работает в режиме **polling** с параллельным Flask-сервером для health check.

**Команды:**
- `/start` — приветствие.
- `/run` — запустить тесты в Chrome и Firefox.

**Что приходит:**
1. Статус запуска (🔄)
2. Ожидание завершения (⏳)
3. Скриншот Allure-отчёта для каждого браузера + ссылка на полный отчёт.
4. Финальное сообщение «✅ Все тесты завершены!»

---

## Развёртывание на Render

Проект использует **Flask** для health check и **polling** для получения обновлений от Telegram.

- **Build Command:** `pip install -r bot/requirements.txt`
- **Start Command:** `python bot/bot.py`
- **Health Check Path:** `/health`

> ⚠️ Встроенный Flask-сервер не рекомендуется для продакшена с высокой нагрузкой, но для данного проекта (несколько запросов в день) этого более чем достаточно.

## Запуск

**Локально:**
```bash
1. mvn clean test
2. mvn allure:serve
```

**Локально с Docker (Selenium Grid)**
docker compose up -d
mvn clean test -Dbrowser=chrome -Dgrid.url=http://localhost:4444/wd/hub
mvn allure:serve


**Через бота**
1. Напиши боту /run.
2. Дождись сообщения о завершении.
3. Открой скриншот или ссылку на отчёт.

##История изменений
Полная история изменений проекта описана в файле CHANGELOG.md