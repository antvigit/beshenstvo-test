package api;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API-тесты бэкенда калькулятора вакцинации — POST /api/submit.
 *
 * Отдельно от Selenium/UI-тестов: бьём напрямую в эндпоинт (multipart/form-data,
 * как это делает форма на сайте — см. fetch("/api/submit", {method:"POST", body:
 * FormData}) в клиентском бандле), без браузера. Ответ при успехе — готовый PDF
 * (генерируется на сервере через ReportLab, судя по сигнатуре в теле ответа), а
 * не blob-ссылка, которую создаёт браузер — сам PDF прилетает с сервера.
 *
 * Маппинг полей формы -> имена полей в API (взято из клиентского JS-бандла):
 * fio -> fio, дата обращения -> visit_date, дата начала вакцинации ->
 * start_vaccinate_date, серия -> series, доза -> dose, дата рождения пациента ->
 * patient_dob (необязательно), законный представитель -> representative /
 * representative_dob (оба необязательны). Даты — в формате YYYY-MM-DD.
 */
@Feature("API калькулятора вакцинации (/api/submit)")
public class VaccinationApiTest {

    private static final String BASE_URL = "https://beshenstvo.pro";
    private static final String ENDPOINT = "/api/submit";
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};

    @BeforeAll
    static void setUpBaseUri() {
        RestAssured.baseURI = BASE_URL;
    }

    private static void assertIsPdf(Response response) {
        assertEquals(200, response.statusCode(), "Ожидался успешный ответ с PDF");
        assertEquals("application/pdf", response.contentType(), "Content-Type должен быть application/pdf");
        byte[] body = response.asByteArray();
        assertTrue(body.length > 0, "Тело ответа не должно быть пустым");
        byte[] header = new byte[4];
        System.arraycopy(body, 0, header, 0, Math.min(4, body.length));
        assertTrue(java.util.Arrays.equals(PDF_MAGIC, header),
                "Тело ответа должно начинаться с сигнатуры PDF (%PDF), получено: "
                        + new String(header, java.nio.charset.StandardCharsets.ISO_8859_1));
    }

    @Test
    @Story("Успешная генерация PDF с минимальным набором полей")
    @Severity(SeverityLevel.CRITICAL)
    void shouldGeneratePdfWithRequiredFieldsOnly() {
        // ФИО/серия/доза на UI не помечены звёздочкой (необязательны), но должны
        // присутствовать в форме хотя бы как пустая строка — этого через реальную
        // форму сайта добиться нельзя (react-hook-form всегда шлёт все поля), но
        // именно это и происходит при обычном заполнении только дат на сайте.
        Response response = given()
                .multiPart("fio", "")
                .multiPart("visit_date", "2026-09-17")
                .multiPart("start_vaccinate_date", "2026-09-17")
                .multiPart("series", "")
                .multiPart("dose", "")
                .when()
                .post(ENDPOINT);

        assertIsPdf(response);
    }

    @Test
    @Story("Успешная генерация PDF со всеми полями пациента")
    @Severity(SeverityLevel.CRITICAL)
    void shouldGeneratePdfWithAllPatientFields() {
        Response response = given()
                .multiPart("fio", "Петров Пётр Петрович")
                .multiPart("visit_date", "2026-09-17")
                .multiPart("start_vaccinate_date", "2026-09-17")
                .multiPart("patient_dob", "1990-01-01")
                .multiPart("series", "123123")
                .multiPart("dose", "1")
                .when()
                .post(ENDPOINT);

        assertIsPdf(response);
    }

    @Test
    @Story("Успешная генерация PDF с данными законного представителя")
    @Severity(SeverityLevel.NORMAL)
    void shouldGeneratePdfWithRepresentativeFields() {
        Response response = given()
                .multiPart("fio", "Иванов Иван Иванович")
                .multiPart("visit_date", "2026-09-17")
                .multiPart("start_vaccinate_date", "2026-09-17")
                .multiPart("patient_dob", "2020-05-15")
                .multiPart("series", "")
                .multiPart("dose", "")
                .multiPart("representative", "Иванова Мария Сергеевна")
                .multiPart("representative_dob", "1985-03-10")
                .when()
                .post(ENDPOINT);

        assertIsPdf(response);
    }

    @Test
    @Story("Отсутствие даты обращения приводит к ошибке 422")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn422WhenVisitDateMissing() {
        Response response = given()
                .multiPart("fio", "")
                .multiPart("start_vaccinate_date", "2026-09-17")
                .multiPart("series", "")
                .multiPart("dose", "")
                .when()
                .post(ENDPOINT);

        response.then()
                .statusCode(422)
                .contentType("application/json")
                .body("error", equalTo("Ошибка при создании PDF"));
    }

    @Test
    @Story("Отсутствие даты начала вакцинации приводит к ошибке 422")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn422WhenStartVaccinateDateMissing() {
        Response response = given()
                .multiPart("fio", "")
                .multiPart("visit_date", "2026-09-17")
                .multiPart("series", "")
                .multiPart("dose", "")
                .when()
                .post(ENDPOINT);

        response.then()
                .statusCode(422)
                .contentType("application/json")
                .body("error", equalTo("Ошибка при создании PDF"));
    }

    @Test
    @Story("Полностью пустой запрос без обязательных дат приводит к ошибке 422")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn422ForCompletelyEmptyRequest() {
        given()
                .when()
                .post(ENDPOINT)
                .then()
                .statusCode(422);
    }

    @Test
    @Story("Сервер не валидирует формат даты обращения")
    @Severity(SeverityLevel.MINOR)
    void shouldAcceptUnparsableDateWithoutValidation() {
        // Задокументированное текущее поведение: сервер не проверяет, что
        // visit_date вообще похож на дату, и всё равно генерирует PDF. Это не
        // "правильно", но это то, что реально происходит сейчас - если валидацию
        // добавят, тест упадёт и его нужно будет обновить, а не то, что он должен
        // проходить всегда.
        Response response = given()
                .multiPart("fio", "")
                .multiPart("visit_date", "not-a-date")
                .multiPart("start_vaccinate_date", "2026-09-17")
                .multiPart("series", "")
                .multiPart("dose", "")
                .when()
                .post(ENDPOINT);

        assertIsPdf(response);
    }

    @Test
    @Story("GET на /api/submit не поддерживается")
    @Severity(SeverityLevel.MINOR)
    void shouldReturn405ForGetMethod() {
        given()
                .when()
                .get(ENDPOINT)
                .then()
                .statusCode(405);
    }

    @Test
    @Story("DELETE на /api/submit не поддерживается")
    @Severity(SeverityLevel.MINOR)
    void shouldReturn405ForDeleteMethod() {
        given()
                .when()
                .delete(ENDPOINT)
                .then()
                .statusCode(405);
    }
}
