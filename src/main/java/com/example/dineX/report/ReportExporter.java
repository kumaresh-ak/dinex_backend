package com.example.dineX.report;

import com.example.dineX.report.ReportService.ReportTable;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ReportExporter {

    public byte[] excel(ReportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < table.headers().size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(table.headers().get(i));
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (List<Object> values : table.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < values.size(); i++) {
                    setValue(row.createCell(i), values.get(i));
                }
            }
            for (int i = 0; i < table.headers().size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not build the Excel report", e);
        }
    }

    public byte[] pdf(ReportTable table, String hotelName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(hotelName, title));
            document.add(new Paragraph(table.title(), normal));
            document.add(new Paragraph(" "));
            PdfPTable grid = new PdfPTable(table.headers().size());
            grid.setWidthPercentage(100);
            for (String header : table.headers()) {
                addCell(grid, header, bold, Element.ALIGN_LEFT);
            }
            for (List<Object> values : table.rows()) {
                for (Object value : values) {
                    addCell(grid, format(value), normal, value instanceof Number ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                }
            }
            document.add(grid);
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not build the PDF report", e);
        }
        return out.toByteArray();
    }

    private static void setValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private static String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        return value.toString();
    }

    private static void addCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
