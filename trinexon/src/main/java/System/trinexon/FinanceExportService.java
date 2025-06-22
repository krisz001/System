package System.trinexon;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Font;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Alert;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.image.WritableImage;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

    public void exportToPdf(FinanceExportContext ctx) {
        File file = ctx.getFile();
        LocalDate from = ctx.getFromDate();
        LocalDate to = ctx.getToDate();
        String project = ctx.getSelectedProject();
        String category = ctx.getSelectedCategory();
        String netProfit = ctx.getNetProfitText();
        String summary = ctx.getSummaryText();
        TableView<?> table = ctx.getActiveTable();
        LineChart<String, Number> chart = ctx.getChart();
        TabPane tabs = ctx.getTabPane();

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.DARK_GRAY);

            document.add(new Paragraph("Pénzügyi kimutatás", titleFont));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Dátumtartomány: " + from + " - " + to, normalFont));
            document.add(new Paragraph("Projekt: " + project, normalFont));
            document.add(new Paragraph("Kategória: " + category, normalFont));
            document.add(new Paragraph("Nettó eredmény: " + netProfit, normalFont));
            document.add(new Paragraph("Összegzés: " + summary, normalFont));
            document.add(Chunk.NEWLINE);

            String tabName = tabs.getSelectionModel().getSelectedItem().getText();
            if ("Diagramok".equals(tabName) && chart != null) {
                WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
                File tmp = File.createTempFile("chart", ".png");
                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", tmp);
                Image pdfImg = Image.getInstance(tmp.getAbsolutePath());
                pdfImg.scaleToFit(500, 300);
                pdfImg.setAlignment(Image.ALIGN_CENTER);
                document.add(pdfImg);
                tmp.delete();
            } else {
                if (table == null || table.getItems().isEmpty()) {
                    document.add(new Paragraph("A táblázat nem tartalmaz adatot.", normalFont));
                } else {
                    List<String> headers = FinanceTableUtil.getColumnHeaders(table);
                    List<List<String>> rows = FinanceTableUtil.getTableData(table);
                    PdfPTable pdfTable = new PdfPTable(headers.size());
                    pdfTable.setWidthPercentage(100);

                    for (String h : headers) {
                        PdfPCell header = new PdfPCell(new Phrase(h, titleFont));
                        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        header.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                        pdfTable.addCell(header);
                    }
                    for (List<String> rowData : rows) {
                        for (String cellText : rowData) {
                            pdfTable.addCell(new PdfPCell(new Phrase(cellText, normalFont)));
                        }
                    }
                    document.add(pdfTable);
                }
            }

            showInfo("PDF exportálás sikeres", "Fájl elmentve: " + file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PDF exportálási hiba", e);
            showError("PDF exportálás sikertelen", e.getMessage());
        } finally {
            document.close();
        }
    }

    public void exportToExcel(FinanceExportContext ctx) {
        File file = ctx.getFile();
        LocalDate from = ctx.getFromDate();
        LocalDate to = ctx.getToDate();
        String project = ctx.getSelectedProject();
        String category = ctx.getSelectedCategory();
        String netProfit = ctx.getNetProfitText();
        String summary = ctx.getSummaryText();
        Map<String, TableView<?>> tableMap = ctx.getTableMap();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet info = workbook.createSheet("Összegzés");
            int r = 0;
            info.createRow(r++).createCell(0).setCellValue("Pénzügyi kimutatás");
            info.createRow(r++).createCell(0).setCellValue("Dátumtartomány: " + from + " - " + to);
            info.createRow(r++).createCell(0).setCellValue("Projekt: " + project);
            info.createRow(r++).createCell(0).setCellValue("Kategória: " + category);
            info.createRow(r++).createCell(0).setCellValue("Nettó eredmény: " + netProfit);
            info.createRow(r++).createCell(0).setCellValue("Összegzés: " + summary);

            for (var entry : tableMap.entrySet()) {
                String sheetName = entry.getKey();
                TableView<?> table = entry.getValue();
                if (table == null || table.getItems().isEmpty()) continue;

                Sheet sheet = workbook.createSheet(sheetName);
                var headers = FinanceTableUtil.getColumnHeaders(table);
                var rows = FinanceTableUtil.getTableData(table);

                CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                int rowNum = 0;
                Row headerRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    cell.setCellStyle(headerStyle);
                }
                for (var rowData : rows) {
                    Row excelRow = sheet.createRow(rowNum++);
                    for (int i = 0; i < rowData.size(); i++) {
                        excelRow.createCell(i).setCellValue(rowData.get(i));
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
            showError("Excel exportálás sikertelen", e.getMessage());
        }
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}