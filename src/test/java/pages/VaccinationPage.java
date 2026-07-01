package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VaccinationPage extends BasePage {

    @FindBy(xpath = "//*[contains(text(), 'План антирабической вакцинации')]")
    private WebElement tableTitle;

    @FindBy(xpath = "//*[contains(text(), '.') and contains(text(), ',') and string-length(text()) > 10]")
    private List<WebElement> dateRows;

    @FindBy(name = "fio")
    private WebElement fioField;

    @FindBy(name = "series")
    private WebElement seriesField;

    @FindBy(name = "dose")
    private WebElement doseField;

    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    public VaccinationPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    @Override
    @Step("Открыть страницу")
    public void open() {
        driver.get("https://beshenstvo.pro/");
    }

    @Override
    @Step("Дождаться загрузки страницы")
    public void waitForPageLoaded() {
        waitForElementVisible(tableTitle);
    }

    @Step("Получить элементы с датами")
    public List<WebElement> getDateElements() {
        return dateRows;
    }

    private void enterText(WebElement field, String value) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", field);
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        wait.until(ExpectedConditions.elementToBeClickable(field));
        field.click();
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        field.sendKeys(Keys.DELETE);
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        Actions actions = new Actions(driver);
        actions.moveToElement(field).click().build().perform();

        for (char ch : value.toCharArray()) {
            actions.sendKeys(String.valueOf(ch)).pause(50).build().perform();
        }

        actions.sendKeys(Keys.TAB).build().perform();
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        String currentValue = field.getAttribute("value");
        if (!value.equals(currentValue)) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                    field, value
            );
            field.sendKeys(Keys.TAB);
        }
    }

    @Step("Ввести ФИО: {fio}")
    public void enterFio(String fio) {
        enterText(fioField, fio);
    }

    @Step("Ввести серию: {series}")
    public void enterSeries(String series) {
        enterText(seriesField, series);
    }

    @Step("Ввести дозу: {dose}")
    public void enterDose(String dose) {
        enterText(doseField, dose);
    }

    @Step("Ввести дату в поле по номеру {index}")
    public void enterDateByIndex(int index, String date) {
        By fieldLocator = By.xpath("(//input[@placeholder='ДД.ММ.ГГГГ'])[" + index + "]");
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(fieldLocator));

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", field);
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        field.click();
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        String day = date.split("\\.")[0];
        By dayLocator = By.xpath("//button[contains(@class, 'MuiPickersDay-root') and text()='" + day + "']");
        try {
            WebElement dayButton = wait.until(ExpectedConditions.elementToBeClickable(dayLocator));
            dayButton.click();
            try { Thread.sleep(300); } catch (InterruptedException ex) {}
        } catch (Exception e) {
            By altLocator = By.xpath("//button[contains(@aria-label, '" + day + ".')]");
            try {
                WebElement dayButton = wait.until(ExpectedConditions.elementToBeClickable(altLocator));
                dayButton.click();
                try { Thread.sleep(300); } catch (InterruptedException ex) {}
            } catch (Exception ex) {
                // fallback – пробуем ввести через sendKeys
                field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                field.sendKeys(Keys.DELETE);
                for (char ch : date.toCharArray()) {
                    field.sendKeys(String.valueOf(ch));
                    try { Thread.sleep(50); } catch (InterruptedException ex2) {}
                }
                field.sendKeys(Keys.TAB);
            }
        }
        field.sendKeys(Keys.ESCAPE);
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Проверка и скриншот при ошибке (только для даты рождения)
        if (index == 1) {
            String currentValue = field.getAttribute("value");
            if (!date.equals(currentValue)) {
                System.out.println("⚠️ Дата рождения не установилась: " + currentValue);
                try {
                    File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                    Files.copy(screenshot.toPath(), Paths.get("birth_date_failed.png"), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("📸 Скриншот сохранён: birth_date_failed.png");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Step("Получить значение даты по индексу {index}")
    public String getDateValueByIndex(int index) {
        By fieldLocator = By.xpath("(//input[@placeholder='ДД.ММ.ГГГГ'])[" + index + "]");
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(fieldLocator));
        return field.getAttribute("value");
    }

    @Step("Нажать кнопку 'Сформировать план вакцинации'")
    public void submitForm() {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
    }

    @Step("Проверить, что PDF открылся в новой вкладке")
    public boolean isPdfOpenedInNewTab() {
        String mainWindowHandle = driver.getWindowHandle();
        try {
            wait.until(ExpectedConditions.numberOfWindowsToBe(2));
        } catch (Exception e) {
            return false;
        }
        for (String windowHandle : driver.getWindowHandles()) {
            if (!windowHandle.equals(mainWindowHandle)) {
                driver.switchTo().window(windowHandle);
                String url = driver.getCurrentUrl();
                driver.close();
                driver.switchTo().window(mainWindowHandle);
                return url != null && url.startsWith("blob:");
            }
        }
        return false;
    }

    @Step("Получить сегодняшнюю дату в формате ДД.ММ.ГГГГ")
    public String getTodayDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}