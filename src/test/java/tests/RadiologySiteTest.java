package tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import pages.RadiologyPage;
import support.WebDriverFactory;

import java.net.MalformedURLException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Feature("Рентгенология (rad.beshenstvo.pro)")
public class RadiologySiteTest {

    private WebDriver driver;
    private RadiologyPage page;

    @BeforeEach
    void setUp() throws MalformedURLException {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        driver = WebDriverFactory.createDriver();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        if (!headless) {
            driver.manage().window().maximize();
        }
        page = new RadiologyPage(driver);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    // Поддомен рентгенологии — отдельное развёртывание от основного сайта,
    // поэтому его доступность проверяется отдельным smoke-тестом.
    @Test
    @Disabled("rad.beshenstvo.pro не отвечает на TLS-хендшейк (проверено curl и Selenium,"
            + " последний раз — 2026-09-17). Убрать аннотацию, когда поддомен восстановят.")
    @Story("Поддомен рентгенологии открывается и отдаёт непустую страницу")
    @Severity(SeverityLevel.BLOCKER)
    void shouldOpenRadiologySubdomain() {
        page.open();
        page.waitForPageLoaded();

        assertFalse(page.getTitle() == null || page.getTitle().isBlank(),
                "У страницы rad.beshenstvo.pro должен быть непустой заголовок");
        assertTrue(driver.getCurrentUrl().contains("rad.beshenstvo.pro"),
                "После открытия ссылки «Рентгенология» URL должен указывать на rad.beshenstvo.pro");
    }
}
