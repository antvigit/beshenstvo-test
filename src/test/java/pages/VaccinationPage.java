package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VaccinationPage extends BasePage {

    // ===== СТАРЫЕ ЛОКАТОРЫ (для календаря) =====
    @FindBy(xpath = "//*[contains(text(), 'План антирабической вакцинации')]")
    private WebElement tableTitle;

    @FindBy(xpath = "//*[contains(text(), '.') and contains(text(), ',') and string-length(text()) > 10]")
    private List<WebElement> dateRows;

    // ===== НОВЫЕ ЛОКАТОРЫ (для формы) =====
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

    // ===== СТАРЫЕ МЕТОДЫ (для календаря) =====
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

    // ===== НОВЫЕ МЕТОДЫ (для формы) =====
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

        // Ставим фокус
        field.click();

        // Выделяем весь текст (Ctrl+A)
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"));

        // Вводим новую дату
        field.sendKeys(date);

        // Подтверждаем ввод
        field.sendKeys(Keys.TAB);
    }

    @Step("Нажать кнопку 'Сформировать план вакцинации'")
    public void submitForm() {
        waitForElementClickable(submitButton);
        submitButton.click();
    }

    @Step("Проверить, что PDF открылся в новой вкладке")
    public boolean isPdfOpenedInNewTab() {
        String mainWindowHandle = driver.getWindowHandle();

        // Ждём открытия новой вкладки
        try {
            wait.until(ExpectedConditions.numberOfWindowsToBe(2));
        } catch (Exception e) {
            return false;
        }

        // Переключаемся на новую вкладку
        for (String windowHandle : driver.getWindowHandles()) {
            if (!windowHandle.equals(mainWindowHandle)) {
                driver.switchTo().window(windowHandle);
                String currentUrl = driver.getCurrentUrl();
                driver.close();
                driver.switchTo().window(mainWindowHandle);
                return currentUrl != null && currentUrl.startsWith("blob:");
            }
        }
        return false;
    }

    @Step("Получить сегодняшнюю дату в формате ДД.ММ.ГГГГ")
    public String getTodayDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}