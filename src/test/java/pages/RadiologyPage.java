package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class RadiologyPage extends BasePage {

    private static final String URL = "https://rad.beshenstvo.pro/";

    public RadiologyPage(WebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Открыть страницу «Рентгенология»")
    public void open() {
        driver.get(URL);
    }

    @Override
    @Step("Дождаться загрузки страницы")
    public void waitForPageLoaded() {
        wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe("about:blank")));
        wait.until(d -> "complete".equals(
                ((org.openqa.selenium.JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    @Step("Получить заголовок страницы")
    public String getTitle() {
        return driver.getTitle();
    }
}
