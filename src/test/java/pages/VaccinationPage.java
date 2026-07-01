package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VaccinationPage extends BasePage {

    // ===== ЛОКАТОРЫ =====
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

    // ===== КОНСТРУКТОР =====
    public VaccinationPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    // ===== СТАРЫЕ МЕТОДЫ =====
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

    // ===== НОВЫЕ МЕТОДЫ =====
    @Step("Ввести ФИО: {fio}")
    public void enterFio(String fio) {
        waitForElementVisible(fioField);
        fioField.clear();
        fioField.sendKeys(fio);
    }

    @Step("Ввести серию: {series}")
    public void enterSeries(String series) {
        waitForElementVisible(seriesField);
        seriesField.clear();
        seriesField.sendKeys(series);
    }

    @Step("Ввести дозу: {dose}")
    public void enterDose(String dose) {
        waitForElementVisible(doseField);
        doseField.clear();
        doseField.sendKeys(dose);
    }

    @Step("Ввести дату в поле по номеру {index}")
    public void enterDateByIndex(int index, String date) {
        By fieldLocator = By.xpath("(//input[@placeholder='ДД.ММ.ГГГГ'])[" + index + "]");
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(fieldLocator));

        // Прокрутка к элементу
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", field);
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // 1. Клик по полю (фокус)
        field.click();

        // 2. Очистка через Ctrl+A + Delete
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        field.sendKeys(Keys.DELETE);

        // 3. Ввод даты посимвольно (имитация пользователя)
        for (char ch : date.toCharArray()) {
            field.sendKeys(String.valueOf(ch));
            try { Thread.sleep(30); } catch (InterruptedException e) {}
        }

        // 4. Потеря фокуса (применяет изменения)
        field.sendKeys(Keys.TAB);

        // 5. Проверяем, установилось ли значение
        String currentValue = field.getAttribute("value");
        if (!date.equals(currentValue)) {
            // Если не установилось — принудительно через JavaScript с полным набором событий
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new FocusEvent('focus', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new FocusEvent('blur', { bubbles: true }));",
                    field, date
            );
        }
    }

    @Step("Нажать кнопку 'Сформировать план вакцинации'")
    public void submitForm() {
        waitForElementClickable(submitButton);
        submitButton.click();
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