# Beshenstvo Test — автотесты для калькулятора прививок

Автотесты для сайта [beshenstvo.pro](https://beshenstvo.pro/) — проверка корректности отображения дат вакцинации.

## 🚀 Стек

- Java 11
- Maven
- Selenium 4
- JUnit 5
- Allure (отчёты)
- Page Object Model + Page Factory
- SOLID

## 📁 Структура проекта
src/test/java/
├── interfaces/
│ └── IPage.java # контракт для страниц
├── locators/
│ └── PageLocators.java # локаторы элементов
├── pages/
│ ├── BasePage.java # общая логика (драйвер, ожидания)
│ └── VaccinationPage.java # страница календаря
└── tests/
└── VaccinationCalculatorTest.java # тесты
src/test/resources/
└── config.properties # URL, таймаут


## 🧪 Тесты

| Тест | Что проверяет |
|------|---------------|
| `shouldDisplayDates` | Наличие дат на странице |
| `shouldHaveCorrectDateFormat` | Формат `ДД.ММ.ГГГГ, День` |
| `shouldBeInChronologicalOrder` | Хронологический порядок дат |
| `shouldHaveCorrectIntervals` | Интервалы между датами |
| `testValidNameCyrillic` | Ввод имени на кириллице |
| `testEmptyName` | Ошибка при пустом поле |
| `testNameWithSpecialChars` | Ошибка при спецсимволах |
| `testVeryLongName` | Ошибка при превышении лимита |
| `testInvalidDate` | Ошибка при невалидной дате |

## ▶️ Запуск

```bash
mvn clean test
mvn allure:serve

📊 Отчёт
Allure-отчёт откроется в браузере после выполнения mvn allure:serve.

📜 История изменений
Версия	Что сделано
v1.0	Создан базовый проект с Selenium и JUnit. Написан первый тест на проверку дат.
v1.1	Добавлен Page Object Model, локаторы вынесены в отдельный класс.
v1.2	Внедрён SOLID: добавлен BasePage, интерфейс IPage, кастомные ожидания.
v1.3	Добавлен Page Factory (@FindBy), переписан VaccinationPage.
v1.4	Добавлен Allure: шаги (@Step), аннотации (@Feature, @Story, @Severity).
v1.5	Добавлены скриншоты при падении в Allure-отчёт.
v1.6	Добавлен config.properties для URL и таймаута.
v1.7	Переписаны тесты по реальным сценариям: формат, порядок, интервалы.
v1.8	Добавлены тесты на поле ФИО: позитивные и негативные сценарии.
v1.9	Добавлен README.md, исправлена конфигурация Allure в pom.xml.

👤 Автор
Антон Вихарев
GitHub

