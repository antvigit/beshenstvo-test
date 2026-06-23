package locators;

import org.openqa.selenium.By;

public class PageLocators {
    public static final By NAME_FIELD = By.name("fio");
    public static final By DATE_FIELD = By.id("vaccinationDate");
    public static final By ERROR_MESSAGE = By.cssSelector(".error");
}