package test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;

public class ApiValidator {
    private static final String API_ENDPOINT = "https://data.reversinglabs.com/api/networking/url/v1/report/query/json";
    private static final String USERNAME = "u/virsec/rlapi";
    private static final String PASSWORD = "vdkmxPA0";

    public static UrlStatus checkUrlStatus(String url) {
        Map<String, Object> query = new HashMap<>();
        query.put("url", url);
        query.put("response_format", "json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("rl", Map.of("query", query));

        try {
            Response response = RestAssured
                    .given()
                    .auth().preemptive().basic(USERNAME, PASSWORD)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .post(API_ENDPOINT);

            System.out.println("Response: " + response.prettyPrint());

            if (response.getStatusCode() == 200) {
                boolean isValid = response.jsonPath().getBoolean("rl.query.url_is_valid");
                String classification = response.jsonPath().getString("rl.query.classification");
                return new UrlStatus(isValid, classification != null ? classification : "unknown");
            } else {
                System.err.println("Unexpected response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error checking URL status for " + url + ": " + e.getMessage());
        }

        // Return default if the API call fails
        return new UrlStatus(false, "unknown");
    }
}
