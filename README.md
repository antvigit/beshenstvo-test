# Beshenstvo Test — автотесты для калькулятора прививок

Автотесты для сайта [beshenstvo.pro](https://beshenstvo.pro/) — проверка корректности отображения дат вакцинации.

---

## Стек технологий

- **Языки:** Java 11, Python 3.9+
- **Сборка:** Maven
- **Тесты:** Selenium 4, JUnit 5
- **Отчёты:** Allure
- **Архитектура:** Page Object Model + Page Factory, SOLID
- **Бот:** TeleBot, Flask, Requests (вебхук)
- **Контейнеризация:** Docker, Selenium Grid (Chrome + Firefox)
- **CI/CD:** GitHub Actions (матрица браузеров)
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
Тесты подключаются к Grid через переменные browser и grid.url

---
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

Бот работает через вебхук (Flask) и позволяет запускать тесты удалённо.

Команды:

    /start — приветствие.

    /run — запустить тесты в Chrome и Firefox.

Что приходит:

    Статус запуска (🔄)

    Ожидание завершения (⏳)

    Скриншот Allure-отчёта для каждого браузера с ссылкой на полный отчёт.

    Финальное сообщение «✅ Все тесты завершены!»

---

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
Полная история изменений проекта описана в файле CHANGELOG.md.