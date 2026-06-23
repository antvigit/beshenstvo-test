package pages;

import interfaces.IPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public abstract class BasePage implements IPage {

    protected WebDriver driver;
    protected WebDriverWait wait;

    protected static final String BASE_URL;
    protected static final int DEFAULT_TIMEOUT;

    static {
        try (InputStream input = BasePage.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found");
            }
            Properties props = new Properties();
            props.load(input);
            BASE_URL = props.getProperty("base.url", "https://beshenstvo.pro/");
            DEFAULT_TIMEOUT = Integer.parseInt(props.getProperty("default.timeout", "10"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
    }

    protected void waitForElementVisible(WebElement element) {
        wait.until(ExpectedConditions.visibilityOf(element));
    }

    protected void waitForElementClickable(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    protected void waitForElementDisappear(WebElement element) {
        wait.until(ExpectedConditions.invisibilityOf(element));
    }

    @Override
    public abstract void open();

    @Override
    public abstract void waitForPageLoaded();
}