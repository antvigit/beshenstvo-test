package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class RheumatologyPage extends BasePage {

    private static final String URL = "https://beshenstvo.pro/rheumatology";

    public RheumatologyPage(WebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Открыть страницу «Ревматология»")
    public void open() {
        driver.get(URL);
    }

    @Override
    @Step("Дождаться загрузки страницы")
    public void waitForPageLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@role='tab']")));
    }

    @Step("Переключиться на вкладку «{tabName}»")
    public void switchTab(String tabName) {
        By tabLocator = By.xpath("//button[@role='tab'][contains(., '" + tabName + "')]");
        WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(tabLocator));
        tab.click();
    }

    @Step("Проверить, что вкладка «{tabName}» выбрана")
    public boolean isTabSelected(String tabName) {
        By tabLocator = By.xpath("//button[@role='tab'][contains(., '" + tabName + "')]");
        WebElement tab = wait.until(ExpectedConditions.visibilityOfElementLocated(tabLocator));
        return "true".equals(tab.getAttribute("aria-selected"));
    }

    @Step("Выбрать шкалу «{scaleName}»")
    public void selectScale(String scaleName) {
        By comboLocator = By.cssSelector("div[role='combobox']");
        WebElement combo = wait.until(ExpectedConditions.elementToBeClickable(comboLocator));
        combo.click();
        By optionLocator = By.xpath("//li[@role='option'][contains(., '" + scaleName + "')]");
        WebElement option = wait.until(ExpectedConditions.elementToBeClickable(optionLocator));
        option.click();
        // Дожидаемся, пока попап меню полностью закроется (анимация закрытия в Firefox
        // заметно дольше, чем в Chrome) — иначе следующий вызов selectScale в цикле может
        // кликнуть по комбобоксу, пока ещё видим уходящий MuiPopover поверх него
        // (ElementClickIntercepted).
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".MuiPopover-root")));
    }

    @Step("Получить название текущей выбранной шкалы")
    public String getSelectedScale() {
        WebElement combo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[role='combobox']")));
        return combo.getText().trim();
    }

    private By numberFieldLocator(String labelText) {
        return By.xpath("//label[contains(., '" + labelText + "')]/following-sibling::div[contains(@class,'MuiInputBase-root')]//input");
    }

    @Step("Ввести значение поля «{labelText}»: {value}")
    public void fillNumberField(String labelText, String value) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(numberFieldLocator(labelText)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", field);
        wait.until(ExpectedConditions.elementToBeClickable(field));
        // Обычный клик иногда попадает в плавающий MUI-лейбл поля (ещё не успел
        // анимированно сместиться после появления формы) — ElementClickIntercepted.
        // JS-клик и фокус берут элемент напрямую, минуя геометрический hit-test.
        ((JavascriptExecutor) driver).executeScript("arguments[0].focus(); arguments[0].click();", field);
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        field.sendKeys(Keys.DELETE);
        field.sendKeys(value);
        field.sendKeys(Keys.TAB);

        // На некоторых числовых полях React-приложения выделение/очистка через
        // клавиатуру срабатывает не всегда (значение дописывается, а не заменяется).
        // Если это произошло — принудительно выставляем значение через нативный сеттер.
        String currentValue = field.getAttribute("value");
        if (!value.equals(currentValue)) {
            ((JavascriptExecutor) driver).executeScript(
                    "const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "setter.call(arguments[0], arguments[1]);" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                    field, value
            );
        }
    }

    @Step("Получить значение поля «{labelText}»")
    public String getFieldValue(String labelText) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(numberFieldLocator(labelText)));
        return field.getAttribute("value");
    }

    @Step("Проверить наличие поля «{labelText}» на форме")
    public boolean isFieldPresent(String labelText) {
        return !driver.findElements(numberFieldLocator(labelText)).isEmpty();
    }

    private By sliderLocator(String labelPrefix) {
        return By.xpath("//p[contains(., '" + labelPrefix + "')]/following-sibling::span[contains(@class,'MuiSlider-root')]//input[@type='range']");
    }

    @Step("Установить слайдер «{labelPrefix}» в значение {value}")
    public void setSlider(String labelPrefix, int value) {
        WebElement slider = wait.until(ExpectedConditions.visibilityOfElementLocated(sliderLocator(labelPrefix)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", slider);
        // Шаг некоторых слайдеров (например, ВАШ 0-10 см в CDAI/SDAI) равен 0.1,
        // поэтому пошаговые ArrowRight сдвигают значение на доли единицы.
        // Выставляем значение напрямую через нативный сеттер, как это делает браузер при drag.
        ((JavascriptExecutor) driver).executeScript(
                "const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(arguments[0], arguments[1]);" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                slider, String.valueOf(value)
        );
    }

    @Step("Получить текущее значение слайдера «{labelPrefix}»")
    public int getSliderValue(String labelPrefix) {
        WebElement slider = wait.until(ExpectedConditions.visibilityOfElementLocated(sliderLocator(labelPrefix)));
        return Integer.parseInt(slider.getAttribute("aria-valuenow"));
    }

    @Step("Развернуть категорию признаков «{categoryLabel}»")
    public void expandAccordion(String categoryLabel) {
        By headerLocator = By.xpath("//button[contains(@class,'MuiAccordionSummary-root')][contains(., '" + categoryLabel + "')]");
        WebElement header = wait.until(ExpectedConditions.elementToBeClickable(headerLocator));
        if ("false".equals(header.getAttribute("aria-expanded"))) {
            header.click();
        }
    }

    @Step("Отметить признак «{symptomLabel}»")
    public void toggleSymptom(String symptomLabel) {
        By checkboxLocator = By.xpath("//label[contains(., '" + symptomLabel + "')]//input[@type='checkbox']");
        WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(checkboxLocator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", checkbox);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkbox);
    }

    @Step("Получить текущий счёт (СКВ)")
    public int getCurrentSleScore() {
        By scoreLocator = By.xpath("//p[contains(., 'Текущий счёт')]/strong");
        WebElement scoreEl = wait.until(ExpectedConditions.visibilityOfElementLocated(scoreLocator));
        return Integer.parseInt(scoreEl.getText().trim());
    }

    @Step("Нажать кнопку расчёта")
    public void clickCalculate() {
        By buttonLocator = By.xpath("//button[contains(., 'Рассчитать') or contains(., 'Интерпретировать')]");
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(buttonLocator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
        button.click();
    }

    @Step("Дождаться и получить числовое значение результата")
    public String getResultValue() {
        By resultLocator = By.cssSelector("h3.MuiTypography-h3");
        WebElement result = wait.until(ExpectedConditions.visibilityOfElementLocated(resultLocator));
        return result.getText().trim();
    }

    @Step("Дождаться и получить текстовую интерпретацию результата")
    public String getResultInterpretation() {
        By chipLocator = By.cssSelector(".MuiChip-label");
        WebElement chip = wait.until(ExpectedConditions.visibilityOfElementLocated(chipLocator));
        return chip.getText().trim();
    }

    @Step("Проверить, что результат расчёта отображается")
    public boolean isResultDisplayed() {
        List<WebElement> results = driver.findElements(By.cssSelector("h3.MuiTypography-h3"));
        return !results.isEmpty() && results.get(0).isDisplayed();
    }
}
