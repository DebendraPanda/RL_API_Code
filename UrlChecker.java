package test;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import utility.ExcelUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class UrlChecker {
    public static void main(String[] args) {
        String inputFilePath = "C:\\Users\\deben\\eclipse-workspace\\UrlChecker\\MySql.xlsx";
        String outputFilePath = "C:\\Users\\deben\\eclipse-workspace\\UrlChecker\\Output.xlsx";

        try {
            List<String> urls = ExcelUtils.readUrls(inputFilePath);
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("URL Check Report");

            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("URL");
            headerRow.createCell(1).setCellValue("Status");
            headerRow.createCell(2).setCellValue("Classification");

            int rowNum = 1;
            for (String url : urls) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(url);

                UrlStatus urlStatus = ApiValidator.checkUrlStatus(url);
                row.createCell(1).setCellValue(urlStatus.isValid() ? "Valid" : "Invalid");
                row.createCell(2).setCellValue(urlStatus.getClassification());

                System.out.println("Checked URL: " + url + " - " + (urlStatus.isValid() ? "Valid" : "Invalid") + ", Classification: " + urlStatus.getClassification());
            }

            try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
                workbook.write(fos);
            }
            workbook.close();
            System.out.println("URL check report generated at: " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
