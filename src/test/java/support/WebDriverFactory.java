package support;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Единая точка создания WebDriver для всех тестовых классов: читает браузер,
 * grid.url и headless из системных свойств (-Dbrowser=, -Dgrid.url=,
 * -Dheadless=), которые задаёт CI (см. run-tests.yml). Раньше эта логика была
 * продублирована в каждом тестовом классе и читала значения через
 * System.getenv(...), хотя CI передаёт их как JVM system properties (-D) —
 * getenv их никогда не видел, поэтому матрица браузеров в CI молча всегда
 * запускала Chrome локально, а Selenium Grid из docker-compose не
 * использовался вовсе.
 */
public final class WebDriverFactory {

    private WebDriverFactory() {
    }

    public static WebDriver createDriver() throws MalformedURLException {
        String browser = System.getProperty("browser", "chrome");
        String gridUrl = System.getProperty("grid.url");
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        if (gridUrl != null && !gridUrl.isEmpty()) {
            return createRemoteDriver(browser, gridUrl, headless);
        }
        return createLocalDriver(browser, headless);
    }

    private static WebDriver createRemoteDriver(String browser, String gridUrl, boolean headless)
            throws MalformedURLException {
        if ("chrome".equals(browser)) {
            ChromeOptions options = new ChromeOptions();
            if (headless) {
                options.addArguments("--headless=new");
            }
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--remote-debugging-port=9222");
            return new RemoteWebDriver(new URL(gridUrl), options);
        }
        if ("firefox".equals(browser)) {
            FirefoxOptions options = new FirefoxOptions();
            if (headless) {
                options.addArguments("--headless");
            }
            return new RemoteWebDriver(new URL(gridUrl), options);
        }
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setBrowserName(browser);
        return new RemoteWebDriver(new URL(gridUrl), caps);
    }

    private static WebDriver createLocalDriver(String browser, boolean headless) {
        if ("firefox".equals(browser)) {
            WebDriverManager.firefoxdriver().setup();
            FirefoxOptions options = new FirefoxOptions();
            if (headless) {
                options.addArguments("--headless");
            }
            options.addArguments("--width=1920", "--height=1080");
            return new FirefoxDriver(options);
        }
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1920,1080");
        return new ChromeDriver(options);
    }
}
