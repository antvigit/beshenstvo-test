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
import org.openqa.selenium.interactions.Actions;
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
        // Прокрутка к элементу
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", field);
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Ждём, пока элемент станет видимым и кликабельным
        wait.until(ExpectedConditions.visibilityOf(field));
        wait.until(ExpectedConditions.elementToBeClickable(field));

        // Устанавливаем фокус и кликаем через JavaScript
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].focus();" +
                        "arguments[0].click();",
                field
        );

        // Устанавливаем значение через JavaScript
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));" +
                        "arguments[0].dispatchEvent(new Event('blur', { bubbles: true, cancelable: true }));",
                field, value
        );

        // Проверка и повторная установка при необходимости
        String currentValue = field.getAttribute("value");
        if (!value.equals(currentValue)) {
            // Если не установилось, пробуем ещё раз с дополнительными событиями
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));" +
                            "arguments[0].dispatchEvent(new Event('blur', { bubbles: true, cancelable: true }));",
                    field, value
            );
        }
    }

    // ===== МЕТОДЫ ВВОДА =====
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

        // Прокрутка к элементу
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", field);
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Ждём кликабельности
        wait.until(ExpectedConditions.elementToBeClickable(field));

        // Клик по полю (фокус)
        field.click();
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Очистка через JavaScript
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", field);

        // Установка значения с принудительным обновлением маски
        ((JavascriptExecutor) driver).executeScript(
                "var el = arguments[0];" +
                        "el.value = arguments[1];" +
                        "el.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));" +
                        "el.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));" +
                        "el.dispatchEvent(new Event('blur', { bubbles: true, cancelable: true }));" +
                        "el.setSelectionRange(arguments[1].length, arguments[1].length);" +
                        "el.focus();" +
                        "el.blur();",
                field, date
        );

        // Потеря фокуса для применения
        field.sendKeys(Keys.TAB);
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Проверка: если значение не установилось — fallback через Actions (посимвольный ввод)
        String currentValue = field.getAttribute("value");
        if (!date.equals(currentValue)) {
            // Очищаем через Ctrl+A + Delete
            field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            field.sendKeys(Keys.DELETE);

            // Вводим посимвольно с задержкой
            for (char ch : date.toCharArray()) {
                field.sendKeys(String.valueOf(ch));
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }
            field.sendKeys(Keys.TAB);
        }

        // Если всё ещё не установилось — принудительно через календарь (только для даты рождения)
        String finalValue = field.getAttribute("value");
        if (!date.equals(finalValue) && index == 1) {
            // Открываем календарь
            field.click();
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            // Парсим день
            String day = date.split("\\.")[0];
            // Ищем кнопку с этим числом
            By dayLocator = By.xpath("//button[contains(@class, 'MuiPickersDay-root') and text()='" + day + "']");
            try {
                WebElement dayButton = wait.until(ExpectedConditions.elementToBeClickable(dayLocator));
                dayButton.click();
                try { Thread.sleep(300); } catch (InterruptedException e) {}
            } catch (Exception e) {
                // Если не нашли — игнорируем
            }
            // Закрываем календарь
            field.sendKeys(Keys.ESCAPE);
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
        // Прокрутка к кнопке
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Ждём кликабельности
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));

        // Клик через JavaScript (обходит перекрытие)
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