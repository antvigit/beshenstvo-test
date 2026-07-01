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

    // ===== УНИВЕРСАЛЬНЫЙ МЕТОД ДЛЯ ВВОДА ТЕКСТА =====
    private void setFieldValue(WebElement field, String value) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", field);
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        wait.until(ExpectedConditions.elementToBeClickable(field));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].focus();" +
                        "arguments[0].click();",
                field
        );

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                field, value
        );

        // Проверка и повторная установка
        String currentValue = field.getAttribute("value");
        if (!value.equals(currentValue)) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                    field, value
            );
        }
    }

    // ===== НОВЫЕ МЕТОДЫ (используют универсальный) =====
    @Step("Ввести ФИО: {fio}")
    public void enterFio(String fio) {
        setFieldValue(fioField, fio);
    }

    @Step("Ввести серию: {series}")
    public void enterSeries(String series) {
        setFieldValue(seriesField, series);
    }

    @Step("Ввести дозу: {dose}")
    public void enterDose(String dose) {
        setFieldValue(doseField, dose);
    }

    @Step("Ввести дату в поле по номеру {index}")
    public void enterDateByIndex(int index, String date) {
        By fieldLocator = By.xpath("(//input[@placeholder='ДД.ММ.ГГГГ'])[" + index + "]");
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(fieldLocator));
        setFieldValue(field, date);

        // Закрываем календарь (если он открылся)
        field.sendKeys(Keys.ESCAPE);
        try { Thread.sleep(200); } catch (InterruptedException e) {}
    }

    @Step("Нажать кнопку 'Сформировать план вакцинации'")
    public void submitForm() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));

        // Прокрутка к кнопке
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Пытаемся кликнуть через JavaScript (обходит перекрытие)
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
        } catch (Exception e) {
            // Если JS-клик не сработал, пробуем обычный клик
            submitButton.click();
        }
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