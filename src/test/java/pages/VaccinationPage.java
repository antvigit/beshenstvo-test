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

    @FindBy(xpath = "//label[contains(., 'Указать законного представителя')]//input[@type='checkbox']")
    private WebElement legalRepresentativeCheckbox;

    @FindBy(name = "parent")
    private WebElement representativeFioField;

    @FindBy(xpath = "//button[contains(@aria-label, 'тёмную тему') or contains(@aria-label, 'светлую тему')]")
    private WebElement themeToggleButton;

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
        // Поиск дня по клику в календаре был хрупким (зависел от текущего месяца в
        // пикере и падал с ElementNotInteractableException на CI из-за анимации
        // открытия/закрытия попапа). Поле — маскированный инпут ДД.ММ.ГГГГ: после
        // клика курсор автоматически встаёт на первый сегмент, и мышь-независимый
        // sendKeys одними цифрами (без точек — маска расставляет их сама) заполняет
        // его посегментно. Работает как для пустого поля, так и для уже заполненного.
        By fieldLocator = By.xpath("(//input[@placeholder='ДД.ММ.ГГГГ'])[" + index + "]");
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(fieldLocator));

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", field);
        wait.until(ExpectedConditions.elementToBeClickable(field));
        field.click();
        field.sendKeys(Keys.HOME);
        field.sendKeys(date.replace(".", ""));
        field.sendKeys(Keys.ESCAPE);

        String currentValue = field.getAttribute("value");
        if (!date.equals(currentValue)) {
            ((JavascriptExecutor) driver).executeScript(
                    "const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "setter.call(arguments[0], arguments[1]);" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                    field, date
            );
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

    @Step("Переключить чекбокс 'Указать законного представителя'")
    public void toggleLegalRepresentative() {
        // Нативный input чекбокса в MUI визуально скрыт (opacity: 0) под SVG-иконкой,
        // поэтому isDisplayed()/elementToBeClickable() для него никогда не станут true —
        // кликаем через JS, дождавшись только присутствия элемента в DOM.
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//label[contains(., 'Указать законного представителя')]//input[@type='checkbox']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", legalRepresentativeCheckbox);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", legalRepresentativeCheckbox);
    }

    @Step("Проверить, что поля законного представителя отображаются")
    public boolean isRepresentativeFioFieldVisible() {
        List<WebElement> fields = driver.findElements(By.name("parent"));
        return !fields.isEmpty() && fields.get(0).isDisplayed();
    }

    @Step("Ввести ФИО законного представителя: {fio}")
    public void enterRepresentativeFio(String fio) {
        enterText(representativeFioField, fio);
    }

    @Step("Переключить тему оформления")
    public void toggleTheme() {
        wait.until(ExpectedConditions.elementToBeClickable(themeToggleButton));
        String previousLabel = themeToggleButton.getAttribute("aria-label");
        String previousBackground = getBodyBackgroundColor();
        themeToggleButton.click();
        // Смена темы происходит через React state и применяется не синхронно с click(),
        // поэтому дожидаемся фактической смены подписи кнопки, прежде чем читать стили.
        wait.until(d -> !previousLabel.equals(themeToggleButton.getAttribute("aria-label")));
        waitForColorChangeAndStabilize(previousBackground);
    }

    @Step("Дождаться стабилизации цвета фона страницы (после загрузки, до любых переключений)")
    public void waitForStableBackgroundColor() {
        waitForColorChangeAndStabilize(null);
    }

    private void waitForColorChangeAndStabilize(String previousColor) {
        // Цвет фона анимируется CSS-transition (150ms, см. CSS body { transition:
        // background-color 0.15s }), поэтому чтение сразу после клика может поймать
        // промежуточное значение. Требуем ОБА условия: цвет уже отличается от того,
        // что было до клика (страхует от ложного «стабильно», пойманного ДО начала
        // перехода — под нагрузкой JS может выполниться с задержкой больше паузы между
        // чтениями), и два чтения подряд совпадают (сам переход завершился).
        wait.until(d -> {
            String first = getBodyBackgroundColor();
            if (previousColor != null && previousColor.equals(first)) {
                return false;
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return first.equals(getBodyBackgroundColor());
        });
    }

    @Step("Получить подпись кнопки переключения темы")
    public String getThemeToggleLabel() {
        return themeToggleButton.getAttribute("aria-label");
    }

    @Step("Получить цвет фона страницы")
    public String getBodyBackgroundColor() {
        // getComputedStyle().backgroundColor сериализуется по-разному в зависимости
        // от сборки браузера/ОС: "rgb(240, 253, 250)" на одних, "rgba(240, 253, 250, 1)"
        // на других (например, Chrome на Linux в CI). Нормализуем до "r,g,b", отбросив
        // альфа-канал, чтобы сравнение цветов не зависело от формата сериализации.
        return (String) ((JavascriptExecutor) driver).executeScript(
                "const c = getComputedStyle(document.body).backgroundColor;" +
                        "const m = c.match(/\\d+/g);" +
                        "return m ? m.slice(0, 3).join(',') : c;");
    }
}