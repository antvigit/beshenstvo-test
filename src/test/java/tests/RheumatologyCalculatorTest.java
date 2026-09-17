package tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pages.RheumatologyPage;
import support.WebDriverFactory;

import java.net.MalformedURLException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Feature("Калькуляторы ревматолога")
public class RheumatologyCalculatorTest {

    private WebDriver driver;
    private RheumatologyPage page;

    @BeforeEach
    void setUp() throws MalformedURLException {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        driver = WebDriverFactory.createDriver();
        // window().maximize() in headless mode shrinks the viewport to a small
        // screen size on this host, which flips the site into its mobile layout
        // (the MuiTabs bar is replaced entirely) — WebDriverFactory already sets
        // the window size we want, so only maximize for a real, visible browser window.
        if (!headless) {
            driver.manage().window().maximize();
        }
        page = new RheumatologyPage(driver);
        page.open();
        page.waitForPageLoaded();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    private double parseResultValue() {
        return Double.parseDouble(page.getResultValue().replace(",", ".").replace("%", "").trim());
    }

    // ===== РЕВМАТОИДНЫЙ АРТРИТ =====

    @Test
    @Story("DAS28-ESR: расчёт по формуле 0.56*sqrt(ЧБС)+0.28*sqrt(ЧПС)+0.70*ln(СОЭ)+0.014*ВАШ")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCalculateDas28Esr() {
        int tjc = 5, sjc = 3, esr = 20;
        page.fillNumberField("Число болезненных суставов", String.valueOf(tjc));
        page.fillNumberField("Число припухших суставов", String.valueOf(sjc));
        page.fillNumberField("СОЭ", String.valueOf(esr));
        page.clickCalculate();

        double expected = 0.56 * Math.sqrt(tjc) + 0.28 * Math.sqrt(sjc) + 0.70 * Math.log(esr) + 0.014 * 50;
        assertEquals(expected, parseResultValue(), 0.02, "DAS28-ESR рассчитан неверно");
        assertFalse(page.getResultInterpretation().isBlank(), "Интерпретация результата не отображается");
    }

    @Test
    @Story("DAS28-CRP: расчёт по формуле 0.56*sqrt(ЧБС)+0.28*sqrt(ЧПС)+0.36*ln(СРБ+1)+0.014*ВАШ+0.96")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCalculateDas28Crp() {
        page.selectScale("DAS28-CRP");
        int tjc = 4, sjc = 2, crp = 10;
        page.fillNumberField("Число болезненных суставов", String.valueOf(tjc));
        page.fillNumberField("Число припухших суставов", String.valueOf(sjc));
        page.fillNumberField("СРБ", String.valueOf(crp));
        page.clickCalculate();

        double expected = 0.56 * Math.sqrt(tjc) + 0.28 * Math.sqrt(sjc) + 0.36 * Math.log(crp + 1) + 0.014 * 50 + 0.96;
        assertEquals(expected, parseResultValue(), 0.02, "DAS28-CRP рассчитан неверно");
    }

    @Test
    @Story("CDAI: расчёт по формуле ЧБС+ЧПС+ВАШ врача+ВАШ пациента")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCalculateCdai() {
        page.selectScale("CDAI");
        int tjc = 6, sjc = 4;
        page.fillNumberField("Число болезненных суставов", String.valueOf(tjc));
        page.fillNumberField("Число припухших суставов", String.valueOf(sjc));
        page.setSlider("Оценка активности врачом", 7);
        page.setSlider("Оценка заболевания больным", 6);
        page.clickCalculate();

        double expected = tjc + sjc + 7 + 6;
        assertEquals(expected, parseResultValue(), 0.01, "CDAI рассчитан неверно");
    }

    @Test
    @Story("SDAI: расчёт по формуле ЧБС+ЧПС+ВАШ врача+ВАШ пациента+СРБ")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCalculateSdai() {
        page.selectScale("SDAI");
        int tjc = 4, sjc = 2, crp = 1;
        page.fillNumberField("Число болезненных суставов", String.valueOf(tjc));
        page.fillNumberField("Число припухших суставов", String.valueOf(sjc));
        page.fillNumberField("СРБ", String.valueOf(crp));
        page.clickCalculate();

        double expected = tjc + sjc + 5 + 5 + crp; // ВАШ врача и пациента по умолчанию = 5
        assertEquals(expected, parseResultValue(), 0.01, "SDAI рассчитан неверно");
    }

    @Test
    @Story("Переключение шкал ревматоидного артрита меняет набор полей")
    @Severity(SeverityLevel.NORMAL)
    void shouldSwitchBetweenRaScales() {
        assertTrue(page.getSelectedScale().contains("DAS28-ESR"), "По умолчанию должна быть выбрана шкала DAS28-ESR");

        page.selectScale("CDAI");
        assertTrue(page.getSelectedScale().contains("CDAI"));
        assertFalse(page.isFieldPresent("СОЭ"), "Поле СОЭ не должно отображаться для шкалы CDAI");

        page.selectScale("SDAI");
        assertTrue(page.getSelectedScale().contains("SDAI"));
        assertTrue(page.isFieldPresent("СРБ"), "Поле СРБ должно отображаться для шкалы SDAI");
    }

    @Test
    @Story("Расчёт без ввода данных не приводит к ошибке приложения")
    @Severity(SeverityLevel.NORMAL)
    void shouldHandleCalculationWithEmptyFields() {
        page.clickCalculate();
        assertTrue(page.isResultDisplayed(), "Результат должен отображаться даже при пустых числовых полях");
    }

    // ===== БОЛЕЗНЬ БЕХТЕРЕВА =====

    @Test
    @Story("BASDAI: расчёт по формуле (Q1+Q2+Q3+Q4+(Q5+Q6)/2)/5")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCalculateBasdaiWithDefaultValues() {
        page.switchTab("Бол. Бехтерева");
        assertTrue(page.getSelectedScale().contains("BASDAI"), "По умолчанию должна быть выбрана шкала BASDAI");

        page.clickCalculate();

        assertEquals(5.0, parseResultValue(), 0.01,
                "BASDAI при всех слайдерах по умолчанию (5) должен быть равен 5.0");
        assertFalse(page.getResultInterpretation().isBlank());
    }

    @Test
    @Story("Переключение шкал анкилозирующего спондилита")
    @Severity(SeverityLevel.NORMAL)
    void shouldSwitchBekhterevScalesAndCalculate() {
        page.switchTab("Бол. Бехтерева");

        for (String scale : new String[]{"ASDAS-ESR", "ASDAS-CRP", "BASFI", "BASMI"}) {
            page.selectScale(scale);
            assertTrue(page.getSelectedScale().contains(scale), "Не удалось выбрать шкалу " + scale);
        }
    }

    // ===== СКВ =====

    @Test
    @Story("SLEDAI-2K: накопление баллов при отметке признаков")
    @Severity(SeverityLevel.CRITICAL)
    void shouldAccumulateSledaiScoreOnSymptomToggle() {
        page.switchTab("СКВ");
        assertEquals(0, page.getCurrentSleScore(), "Начальный счёт СКВ должен быть равен 0");

        page.expandAccordion("Неврологические");
        page.toggleSymptom("Головная боль");
        int scoreAfterCheck = page.getCurrentSleScore();
        assertTrue(scoreAfterCheck > 0, "Счёт должен увеличиться после отметки признака «Головная боль»");

        page.toggleSymptom("Головная боль");
        assertEquals(0, page.getCurrentSleScore(), "Счёт должен вернуться к 0 после снятия отметки признака");
    }

    @Test
    @Story("SLEDAI-2K: комбинация признаков из разных категорий суммируется")
    @Severity(SeverityLevel.NORMAL)
    void shouldSumSledaiScoreAcrossCategories() {
        page.switchTab("СКВ");

        page.expandAccordion("Неврологические");
        page.toggleSymptom("Головная боль");
        int scoreAfterFirst = page.getCurrentSleScore();

        page.expandAccordion("Мышечно-скелетные");
        page.toggleSymptom("Артрит");
        int scoreAfterSecond = page.getCurrentSleScore();

        assertTrue(scoreAfterSecond > scoreAfterFirst,
                "Отметка признака из второй категории должна увеличить общий счёт");

        page.clickCalculate();
        assertTrue(page.isResultDisplayed(), "Результат SLEDAI-2K должен отображаться после расчёта");
        assertFalse(page.getResultInterpretation().isBlank());
    }

    @Test
    @Story("Переключение на шкалу SELENA-SLEDAI")
    @Severity(SeverityLevel.MINOR)
    void shouldSwitchToSelenaSledaiScale() {
        page.switchTab("СКВ");
        page.selectScale("SELENA-SLEDAI");
        assertTrue(page.getSelectedScale().contains("SELENA-SLEDAI"));
    }

    // ===== ОСТЕОПОРОЗ =====

    @Test
    @Story("FRAX: интерпретация риска переломов зависит от результата FRAX%")
    @Severity(SeverityLevel.CRITICAL)
    void shouldInterpretFraxRiskMonotonically() {
        page.switchTab("Остеопороз");

        page.fillNumberField("Возраст", "65");

        page.fillNumberField("Результат FRAX", "1");
        page.clickCalculate();
        String lowRiskLabel = page.getResultInterpretation();

        page.fillNumberField("Результат FRAX", "40");
        page.clickCalculate();
        String highRiskLabel = page.getResultInterpretation();

        assertNotEquals(lowRiskLabel, highRiskLabel,
                "Интерпретация риска должна отличаться для низкого (1%) и высокого (40%) результата FRAX");
    }

    @Test
    @Story("FRAX: граничные значения возраста пациента (40-90 лет)")
    @Severity(SeverityLevel.NORMAL)
    void shouldAcceptAgeBoundaryValues() {
        page.switchTab("Остеопороз");

        page.fillNumberField("Возраст", "40");
        assertEquals("40", page.getFieldValue("Возраст"), "Нижняя граница возраста (40) должна приниматься");

        page.fillNumberField("Возраст", "90");
        assertEquals("90", page.getFieldValue("Возраст"), "Верхняя граница возраста (90) должна приниматься");
    }

    // ===== НАВИГАЦИЯ =====

    @Test
    @Story("Переход из раздела «Ревматология» в раздел «Вакцинация»")
    @Severity(SeverityLevel.NORMAL)
    void shouldNavigateBackToVaccinationSection() {
        WebDriverWait navWait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.findElement(By.xpath("//a[contains(., 'Вакцинация')]")).click();

        navWait.until(ExpectedConditions.urlToBe("https://beshenstvo.pro/"));
        assertEquals("https://beshenstvo.pro/", driver.getCurrentUrl(),
                "Переход по ссылке «Вакцинация» должен вести на главную страницу калькулятора прививок");
    }
}
