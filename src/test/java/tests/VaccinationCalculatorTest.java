package tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import pages.VaccinationPage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
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
        String browser = System.getenv("browser") != null ? System.getenv("browser")
                : testProps.getProperty("browser", "chrome");
        String gridUrl = System.getenv("grid.url");
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        if (gridUrl != null && !gridUrl.isEmpty()) {
            if (browser.equals("chrome")) {
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                options.addArguments("--remote-debugging-port=9222");
                driver = new RemoteWebDriver(new URL(gridUrl), options);
            } else if (browser.equals("firefox")) {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("--headless");
                }
                driver = new RemoteWebDriver(new URL(gridUrl), options);
            } else {
                DesiredCapabilities caps = new DesiredCapabilities();
                caps.setBrowserName(browser);
                driver = new RemoteWebDriver(new URL(gridUrl), caps);
            }
        } else {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            if (headless) {
                options.addArguments("--headless=new");
            }
            options.addArguments("--window-size=1920,1080");
            driver = new ChromeDriver(options);
        }

        driver.manage().window().maximize(); // это работает и в headless
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
                log.info("✅ Valid date: " + datePart);
                atLeastOneDateFound = true;
            } catch (DateTimeParseException e) {
                log.warn("⏭️ Skipped (not a date): " + datePart);
            }
        }

        assertTrue(atLeastOneDateFound, "No valid dates found on page");
    }

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

        String today = page.getTodayDate();
        page.enterDateByIndex(2, today);
        page.enterDateByIndex(3, today);

        page.enterSeries(series);
        page.enterDose(dose);

        page.submitForm();

        try {
            Thread.sleep(7000); // увеличил до 7 секунд
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(page.isPdfOpenedInNewTab(), "PDF не открылся в новой вкладке");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }
}