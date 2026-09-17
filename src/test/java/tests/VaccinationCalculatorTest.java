package tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pages.VaccinationPage;
import support.WebDriverFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@Feature("Календарь прививок")
public class VaccinationCalculatorTest {

    private WebDriver driver;
    private VaccinationPage page;
    private static final Logger log = LogManager.getLogger(VaccinationCalculatorTest.class);

    private static final Properties testProps = new Properties();

    static {
        try (InputStream input = VaccinationCalculatorTest.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found in classpath");
            }
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                testProps.load(reader);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    @BeforeEach
    void setUp() throws MalformedURLException {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        driver = WebDriverFactory.createDriver();
        // window().maximize() в headless-режиме на некоторых страницах сайта схлопывает
        // viewport до мобильного брейкпоинта (см. RheumatologyPage) — WebDriverFactory
        // уже задаёт нужный --window-size, поэтому maximize нужен только для видимого окна.
        if (!headless) {
            driver.manage().window().maximize();
        }
        page = new VaccinationPage(driver);
    }

    // ===== ТЕСТ 1: ПРОВЕРКА ДАТ ВАКЦИНАЦИИ =====
    @Test
    @Story("Проверка дат вакцинации")
    @Severity(SeverityLevel.CRITICAL)
    void shouldValidateVaccinationDates() {
        page.open();
        page.waitForPageLoaded();

        List<WebElement> dateElements = page.getDateElements();
        assertFalse(dateElements.isEmpty(), "No dates found on page");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        boolean atLeastOneDateFound = false;

        for (WebElement element : dateElements) {
            String fullText = element.getText().trim();
            if (fullText.isEmpty()) continue;

            String datePart = fullText.split(",")[0].trim();
            if (datePart.isEmpty()) continue;

            try {
                LocalDate parsedDate = LocalDate.parse(datePart, formatter);
                assertTrue(parsedDate.getYear() >= 2025 && parsedDate.getYear() <= 2030,
                        "Date out of range: " + datePart);
                log.info("✅ Valid date: " + datePart);
                atLeastOneDateFound = true;
            } catch (DateTimeParseException e) {
                log.warn("⏭️ Skipped (not a date): " + datePart);
            }
        }

        assertTrue(atLeastOneDateFound, "No valid dates found on page");
    }

    // ===== ТЕСТ 2: ЗАПОЛНЕНИЕ ФОРМЫ И ГЕНЕРАЦИЯ PDF =====
    @Test
    @Story("Заполнение формы и генерация PDF")
    @Severity(SeverityLevel.CRITICAL)
    void testFullFormSubmission() {
        String fio = testProps.getProperty("test.fio", "Петров Петр Петрович");
        String birthDate = testProps.getProperty("test.birthDate", "01.01.1990");
        String series = testProps.getProperty("test.series", "123123");
        String dose = testProps.getProperty("test.dose", "1");

        page.open();

        page.enterFio(fio);
        page.enterDateByIndex(1, birthDate);

        // ПРОВЕРКА: убеждаемся, что дата рождения действительно введена
        String actualBirthDate = page.getDateValueByIndex(1);
        assertEquals(birthDate, actualBirthDate, "Дата рождения не отображается корректно");

        String today = page.getTodayDate();
        page.enterDateByIndex(2, today);
        page.enterDateByIndex(3, today);

        page.enterSeries(series);
        page.enterDose(dose);

        page.submitForm();

        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Скриншот для отладки (перезаписываем, если существует)
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            Files.copy(screenshot.toPath(), Paths.get("after_submit.png"), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("📸 Скриншот сохранён: after_submit.png");
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(page.isPdfOpenedInNewTab(), "PDF не открылся в новой вкладке");
    }

    // ===== ТЕСТ 3: ЗАКОННЫЙ ПРЕДСТАВИТЕЛЬ =====
    @Test
    @Story("Отображение полей законного представителя")
    @Severity(SeverityLevel.NORMAL)
    void shouldShowRepresentativeFieldsWhenCheckboxEnabled() {
        page.open();
        page.waitForPageLoaded();

        assertFalse(page.isRepresentativeFioFieldVisible(), "Поле представителя не должно отображаться до включения чекбокса");

        page.toggleLegalRepresentative();

        assertTrue(page.isRepresentativeFioFieldVisible(), "Поле ФИО представителя должно появиться после включения чекбокса");
        page.enterRepresentativeFio("Иванова Мария Сергеевна");
    }

    // ===== ТЕСТ 4: ОБЯЗАТЕЛЬНЫ ТОЛЬКО ПОЛЯ, ПОМЕЧЕННЫЕ * (ДАТЫ) =====
    @Test
    @Story("Единственно обязательные поля формы — даты, помеченные *")
    @Severity(SeverityLevel.CRITICAL)
    void shouldGeneratePdfWithOnlyRequiredDateFields() {
        page.open();
        page.waitForPageLoaded();

        // На форме звёздочкой (*) помечены только «Дата обращения» и «Дата начала
        // вакцинации» — они подставляются по умолчанию текущей датой. ФИО и дата
        // рождения пациента звёздочкой не отмечены, то есть формально не обязательны:
        // план должен сформироваться и без них.
        page.submitForm();

        assertTrue(page.isPdfOpenedInNewTab(),
                "PDF должен формироваться при заполненных обязательных полях (датах), даже если ФИО и дата рождения пусты");
    }

    // ===== ТЕСТ 5: ПЕРЕКЛЮЧЕНИЕ ТЁМНОЙ ТЕМЫ =====
    @Test
    @Story("Переключение тёмной темы")
    @Severity(SeverityLevel.MINOR)
    void shouldToggleDarkTheme() {
        page.open();
        page.waitForPageLoaded();
        page.waitForStableBackgroundColor();

        String lightBackground = page.getBodyBackgroundColor();
        String lightLabel = page.getThemeToggleLabel();

        page.toggleTheme();

        String darkBackground = page.getBodyBackgroundColor();
        String darkLabel = page.getThemeToggleLabel();

        assertNotEquals(lightBackground, darkBackground, "Цвет фона должен измениться при включении тёмной темы");
        assertNotEquals(lightLabel, darkLabel, "Подпись кнопки переключения темы должна измениться");

        page.toggleTheme();
        assertEquals(lightBackground, page.getBodyBackgroundColor(), "Цвет фона должен вернуться к исходному");
    }

    // ===== ТЕСТ 6: ХРОНОЛОГИЧЕСКИЙ ПОРЯДОК И ИНТЕРВАЛЫ ДАТ ДЛЯ РАЗНЫХ ДАТ НАЧАЛА =====
    @Test
    @Story("Интервалы графика вакцинации при смещении даты начала")
    @Severity(SeverityLevel.CRITICAL)
    void shouldKeepVaccinationIntervalsForCustomStartDate() {
        page.open();
        page.waitForPageLoaded();

        String today = page.getTodayDate();
        page.enterDateByIndex(2, today);
        page.enterDateByIndex(3, today);

        List<WebElement> dateElements = page.getDateElements();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        LocalDate startDate = LocalDate.parse(today, formatter);
        int[] expectedOffsets = {0, 3, 7, 14, 30, 90};
        int found = 0;

        for (int i = 0; i < dateElements.size() && found < expectedOffsets.length; i++) {
            String fullText = dateElements.get(i).getText().trim();
            if (fullText.isEmpty()) continue;
            String datePart = fullText.split(",")[0].trim();
            try {
                LocalDate parsedDate = LocalDate.parse(datePart, formatter);
                LocalDate expectedDate = startDate.plusDays(expectedOffsets[found]);
                assertEquals(expectedDate, parsedDate,
                        "Дата дозы №" + (found + 1) + " не соответствует ожидаемому интервалу от даты начала вакцинации");
                found++;
            } catch (DateTimeParseException ignored) {
                // Пропускаем элементы, не являющиеся датами
            }
        }

        assertEquals(expectedOffsets.length, found, "Не все 6 доз графика вакцинации были найдены на странице");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }
}