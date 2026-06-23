package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class VaccinationPage extends BasePage {

    @FindBy(xpath = "//*[contains(text(), 'План антирабической вакцинации')]")
    private WebElement tableTitle;

    @FindBy(xpath = "//*[contains(text(), '.') and contains(text(), ',') and string-length(text()) > 10]")
    private List<WebElement> dateRows;

    @FindBy(name = "fio")
    private WebElement nameField;

    @FindBy(id = "vaccinationDate")
    private WebElement dateField;

    @FindBy(css = ".error")
    private WebElement errorMessage;

    public VaccinationPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    @Override
    @Step("Открыть страницу")
    public void open() {
        driver.get(BASE_URL);
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

    @Step("Ввести имя: {name}")
    public void enterName(String name) {
        nameField.clear();
        nameField.sendKeys(name);
    }

    @Step("Ввести дату: {date}")
    public void enterDate(String date) {
        dateField.clear();
        dateField.sendKeys(date);
    }

    @Step("Получить сообщение об ошибке")
    public String getErrorMessage() {
        return errorMessage.getText();
    }
}