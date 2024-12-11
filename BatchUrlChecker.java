package test;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import utility.ExcelUtils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchUrlChecker {
    private static final String API_ENDPOINT = "https://data.reversinglabs.com/api/networking/reputation/v1/query/json";
    private static final String USERNAME = "u/virsec/rlapi";
    private static final String PASSWORD = "vdkmxPA0";

    public static void main(String[] args) {
        String inputFilePath = "C:\\Users\\deben\\eclipse-workspace\\UrlChecker\\redhatmysql_input.xlsx";
        String outputFilePath = "C:\\Users\\deben\\eclipse-workspace\\UrlChecker\\redhatmysql_Output1.xlsx";

        try {
            List<String> urls = ExcelUtils.readUrls(inputFilePath);
            List<List<String>> batches = createBatches(urls, 100);

            Workbook workbook;
            Sheet sheet;
            int rowNum = 1;

            try (FileInputStream fis = new FileInputStream(outputFilePath)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
            } catch (IOException e) {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("URL Check Report");
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("URL");
//                headerRow.createCell(1).setCellValue("Status");
                headerRow.createCell(1).setCellValue("Classification");
            }

            for (List<String> batch : batches) {
                List<Map<String, String>> results = checkUrlsBatch(batch);

                for (Map<String, String> result : results) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(result.get("url"));
//                    row.createCell(1).setCellValue(result.get("status"));
                    row.createCell(1).setCellValue(result.get("classification"));
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
                workbook.write(fos);
            }
            workbook.close();
            System.out.println("Batch URL check report generated at: " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<List<String>> createBatches(List<String> urls, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < urls.size(); i += batchSize) {
            int end = Math.min(urls.size(), i + batchSize);
            batches.add(urls.subList(i, end));
        }
        return batches;
    }

    private static List<Map<String, String>> checkUrlsBatch(List<String> urls) {
        List<Map<String, String>> results = new ArrayList<>();
        List<Map<String, Object>> networkLocations = new ArrayList<>();

        for (String url : urls) {
            Map<String, Object> location = new HashMap<>();
            location.put("network_location", url);
            location.put("type", "url");
            networkLocations.add(location);
        }

        Map<String, Object> query = new HashMap<>();
        query.put("network_locations", networkLocations);
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

            System.out.println("API Response Status: " + response.getStatusCode());
            String responseBody = response.getBody().asString();
            System.out.println("Response Body: " + responseBody); // Debugging line to check response structure

            if (response.getStatusCode() == 200) {
                JSONObject jsonObject = new JSONObject(responseBody);
                
                if (jsonObject.has("rl") && jsonObject.getJSONObject("rl").has("entries")) {
                    JSONArray entries = jsonObject.getJSONObject("rl").getJSONArray("entries");

                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject entry = entries.getJSONObject(i);

                        Map<String, String> result = new HashMap<>();

                        // Retrieve "network_location" or fall back to "requested_network_location"
                        String url = entry.optString("network_location", entry.optString("requested_network_location", "N/A"));
                        result.put("url", url);

//                        boolean isValid = entry.optBoolean("url_is_valid", false);
//                        result.put("status", isValid ? "Valid" : "Invalid");

                        String classification = entry.optString("classification", "Unknown");
                        result.put("classification", classification);

                        results.add(result);
                    }
                } else {
                    System.out.println("Error: 'entries' array not found in JSON response.");
                }
            } else {
                System.err.println("Unexpected response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error sending batch request: " + e.getMessage());
        }

        return results;
    }
}
