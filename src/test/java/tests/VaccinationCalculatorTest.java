package tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import pages.VaccinationPage;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Feature("Календарь прививок")
public class VaccinationCalculatorTest {

    private WebDriver driver;
    private VaccinationPage page;

    @BeforeEach
    void setUp() throws MalformedURLException {
        String browser = System.getProperty("browser", "chrome");
        String gridUrl = System.getProperty("grid.url");

        if (gridUrl != null && !gridUrl.isEmpty()) {
            DesiredCapabilities caps = new DesiredCapabilities();
            caps.setBrowserName(browser);
            driver = new RemoteWebDriver(new URL(gridUrl), caps);
        } else {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            driver = new ChromeDriver(options);
        }

        driver.manage().window().maximize();
        page = new VaccinationPage(driver);
    }

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
                System.out.println("✅ Valid date: " + datePart);
                atLeastOneDateFound = true;
            } catch (DateTimeParseException e) {
                System.out.println("⏭️ Skipped (not a date): " + datePart);
            }
        }

        assertTrue(atLeastOneDateFound, "No valid dates found on page");
    }

    @Test
    @Story("Валидация поля ФИО")
    @Severity(SeverityLevel.NORMAL)
    void testValidNameCyrillic() {
        page.open();
        page.waitForPageLoaded();
        page.enterName("Иванов Иван");
        page.enterDate("23.06.2026");
        assertTrue(page.getErrorMessage().isEmpty(), "No error expected");
    }

    @Test
    @Story("Валидация поля ФИО")
    @Severity(SeverityLevel.CRITICAL)
    void testEmptyName() {
        page.open();
        page.waitForPageLoaded();
        page.enterName("");
        page.enterDate("23.06.2026");
        assertFalse(page.getErrorMessage().isEmpty(), "Error expected for empty name");
    }

    @Test
    @Story("Валидация поля ФИО")
    @Severity(SeverityLevel.NORMAL)
    void testNameWithSpecialChars() {
        page.open();
        page.waitForPageLoaded();
        page.enterName("@#$%^&*()");
        page.enterDate("23.06.2026");
        assertFalse(page.getErrorMessage().isEmpty(), "Error expected for special chars");
    }

    @Test
    @Story("Валидация поля ФИО")
    @Severity(SeverityLevel.MINOR)
    void testVeryLongName() {
        page.open();
        page.waitForPageLoaded();
        String longName = "A".repeat(201);
        page.enterName(longName);
        page.enterDate("23.06.2026");
        assertFalse(page.getErrorMessage().isEmpty(), "Error expected for too long name");
    }

    @Test
    @Story("Валидация даты")
    @Severity(SeverityLevel.CRITICAL)
    void testInvalidDate() {
        page.open();
        page.waitForPageLoaded();
        page.enterName("Иванов Иван");
        page.enterDate("31.13.2026");
        assertFalse(page.getErrorMessage().isEmpty(), "Error expected for invalid date");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }
}