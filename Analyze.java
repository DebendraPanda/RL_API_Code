package test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import utility.ExcelUtils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Analyze {
    private static final String API_ENDPOINT = "https://data.reversinglabs.com/api/networking/url/v1/analyze/query/json";
    private static final String USERNAME = "u/virsec/rlapi";
    private static final String PASSWORD = "vdkmxPA0";

    public static void main(String[] args) {
        String inputFilePath = "C:\\Users\\deben\\eclipse-workspace\\UrlChecker\\MySql.xlsx";
        String outputFilePath = "C:\\Users\\deben\\eclipse-workspace\\UrlChecker\\Output.xlsx";

        try {
            // Read the list of URLs from the input Excel file
            List<String> urls = ExcelUtils.readUrls(inputFilePath);

            // Create the Excel output workbook and sheet
            Workbook workbook;
            Sheet sheet;
            int rowNum = 0;

            // If output file exists, open it, otherwise create a new one
            try (FileInputStream fis = new FileInputStream(outputFilePath)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
            } catch (IOException e) {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("URL Check Report");
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("URL");
                headerRow.createCell(1).setCellValue("Analysis ID");
            }

            // Process each URL one by one with a 1-minute delay between requests
            for (String url : urls) {
                Map<String, String> result = checkUrl(url);

                if (result != null) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(result.get("url"));
                    row.createCell(1).setCellValue(result.get("analysis_id"));
                }

                // Wait for 1 minute before sending the next request
                Thread.sleep(70000); // 60000 milliseconds = 1 minute
            }

            // Save the results into the output file
            try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
                workbook.write(fos);
            }

            workbook.close();
            System.out.println("Batch URL check report generated at: " + outputFilePath);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Function to check the status of a single URL
    private static Map<String, String> checkUrl(String url) {
        Map<String, String> result = new HashMap<>();

        // Prepare the request body with the URL
        Map<String, Object> query = new HashMap<>();
        query.put("url", url);
        query.put("response_format", "json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("rl", Map.of("query", query));

        try {
            // Send the request
            Response response = RestAssured
                    .given()
                    .auth().preemptive().basic(USERNAME, PASSWORD)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .post(API_ENDPOINT);

            System.out.println("API Response Status: " + response.getStatusCode());
            String responseBody = response.getBody().asString();
            System.out.println("Response Body: " + responseBody); // Debugging line to check response structure

            if (response.getStatusCode() == 200) {
                // Parse the response JSON
                JSONObject jsonObject = new JSONObject(responseBody);
                if (jsonObject.has("rl") && jsonObject.getJSONObject("rl").has("analysis_id")) {
                    String analysisId = jsonObject.getJSONObject("rl").getString("analysis_id");

                    // Add the URL and analysis_id to the result map
                    result.put("url", url);
                    result.put("analysis_id", analysisId);
                } else {
                    System.err.println("Error: 'analysis_id' not found in the JSON response.");
                }
            } else {
                System.err.println("Unexpected response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error sending request for URL " + url + ": " + e.getMessage());
        }

        return result;
    }
}
