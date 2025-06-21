package System.trinexon;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FinanceExportService {
    private static final Logger LOGGER = Logger.getLogger(FinanceExportService.class.getName());

    public void exportToPdf(FinanceExportContext context) {
        exportToPdf(
                context.getFile(),
                context.getFromDate(),
                context.getToDate(),
                context.getSelectedProject(),
                context.getSelectedCategory(),
                context.getNetProfitText(),
                context.getSummaryText(),
                context.getActiveTable(),
                context.getChart(),
                context.getTabPane()
        );
    }

    public void exportToExcel(FinanceExportContext context) {
        exportToExcel(
                context.getFile(),
                context.getFromDate(),
                context.getToDate(),
                context.getSelectedProject(),
                context.getSelectedCategory(),
                context.getNetProfitText(),
                context.getSummaryText(),
                context.getTableMap()
        );
    }

    public void exportToPdf(File file,
                            LocalDate from,
                            LocalDate to,
                            String project,
                            String category,
                            String netProfit,
                            String summary,
                            TableView<?> table,
                            LineChart<String, Number> chart,
                            TabPane tabPane) {
        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.DARK_GRAY);

            document.add(new Paragraph("Pénzügyi kimutatás", titleFont));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Dátumtartomány: " + from + " - " + to, normalFont));
            document.add(new Paragraph("Projekt: " + project, normalFont));
            document.add(new Paragraph("Kategória: " + category, normalFont));
            document.add(new Paragraph("Nettó eredmény: " + netProfit, normalFont));
            document.add(new Paragraph("Összegzés: " + summary, normalFont));
            document.add(Chunk.NEWLINE);

            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            String tabName = selectedTab != null ? selectedTab.getText() : "";

            if ("Diagramok".equals(tabName)) {
                WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
                File tempImage = new File("chart_snapshot.png");
                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", tempImage);
                Image chartImage = Image.getInstance(tempImage.getAbsolutePath());
                chartImage.scaleToFit(500, 300);
                chartImage.setAlignment(Element.ALIGN_CENTER);
                document.add(chartImage);
                tempImage.delete();
            } else {
                if (table == null || table.getItems().isEmpty()) {
                    document.add(new Paragraph("A táblázat nem tartalmaz adatot.", normalFont));
                } else {
                    List<String> headers = FinanceTableUtil.getColumnHeaders(table);
                    List<List<String>> rows = FinanceTableUtil.getTableData(table);

                    PdfPTable pdfTable = new PdfPTable(headers.size());
                    pdfTable.setWidthPercentage(100);

                    for (String headerText : headers) {
                        PdfPCell header = new PdfPCell(new Phrase(headerText, titleFont));
                        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        header.setHorizontalAlignment(Element.ALIGN_CENTER);
                        pdfTable.addCell(header);
                    }

                    for (List<String> row : rows) {
                        for (String cellText : row) {
                            pdfTable.addCell(new PdfPCell(new Phrase(cellText, normalFont)));
                        }
                    }

                    document.add(pdfTable);
                }
            }

            document.close();
            showInfo("PDF exportálás sikeres", "Fájl elmentve: " + file.getAbsolutePath());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PDF exportálási hiba", e);
            showError("PDF exportálás sikertelen", e);
        }
    }

    public void exportToExcel(File file,
                               LocalDate from,
                               LocalDate to,
                               String project,
                               String category,
                               String netProfit,
                               String summary,
                               Map<String, TableView<?>> tableMap) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            XSSFSheet infoSheet = workbook.createSheet("Összegzés");
            int rowIndex = 0;
            infoSheet.createRow(rowIndex++).createCell(0).setCellValue("Pénzügyi kimutatás");
            infoSheet.createRow(rowIndex++).createCell(0).setCellValue("Dátumtartomány: " + from + " - " + to);
            infoSheet.createRow(rowIndex++).createCell(0).setCellValue("Projekt: " + project);
            infoSheet.createRow(rowIndex++).createCell(0).setCellValue("Kategória: " + category);
            infoSheet.createRow(rowIndex++).createCell(0).setCellValue("Nettó eredmény: " + netProfit);
            infoSheet.createRow(rowIndex++).createCell(0).setCellValue("Összegzés: " + summary);

            for (Map.Entry<String, TableView<?>> entry : tableMap.entrySet()) {
                String sheetName = entry.getKey();
                TableView<?> table = entry.getValue();

                if (table == null || table.getItems().isEmpty()) continue;

                List<String> headers = FinanceTableUtil.getColumnHeaders(table);
                List<List<String>> rows = FinanceTableUtil.getTableData(table);

                XSSFSheet sheet = workbook.createSheet(sheetName);
                int rowNum = 0;

                Row headerRow = sheet.createRow(rowNum++);
                int colIndex = 0;
                for (String header : headers) {
                    Cell cell = headerRow.createCell(colIndex++);
                    cell.setCellValue(header);
                    cell.setCellStyle(headerStyle);
                }

                for (List<String> row : rows) {
                    Row excelRow = sheet.createRow(rowNum++);
                    int cellIndex = 0;
                    for (String cellText : row) {
                        excelRow.createCell(cellIndex++).setCellValue(cellText);
                    }
                }

                for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }

            showInfo("Excel exportálás sikeres", "Fájl elmentve: " + file.getAbsolutePath());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Excel exportálási hiba", e);
            showError("Excel exportálás sikertelen", e);
        }
    }

    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String header, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}