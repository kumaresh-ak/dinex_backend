package com.example.dineX.billing;

import com.example.dineX.hotel.Hotel;
import com.example.dineX.order.DiningSession;
import com.example.dineX.order.OrderDtos.OrderItemView;
import com.example.dineX.order.OrderDtos.OrderView;
import com.example.dineX.settings.PlatformSettingsService;
import com.example.dineX.settings.PlatformSettingsService.Key;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class BillPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final PlatformSettingsService settings;

    public BillPdfService(PlatformSettingsService settings) {
        this.settings = settings;
    }

    public byte[] render(Hotel hotel, Bill bill, DiningSession session, String tableNumber, List<OrderView> orders) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Font hotelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font mono = FontFactory.getFont(FontFactory.COURIER, 9);
        Document document = new Document(PageSize.A4, 40, 40, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(centered(hotel.getHotelName(), hotelFont));
            String address = join(", ", hotel.getAddress(), hotel.getCity(), hotel.getState(), hotel.getPincode());
            if (!address.isBlank()) {
                document.add(centered(address, small));
            }
            String contact = join("  |  ", hotel.getMobileNumber(), hotel.getEmail());
            if (!contact.isBlank()) {
                document.add(centered(contact, small));
            }
            if (hotel.getGstNumber() != null && !hotel.getGstNumber().isBlank()) {
                document.add(centered("GSTIN: " + hotel.getGstNumber(), bold));
            }
            document.add(centered("TAX INVOICE", heading));
            document.add(rule(mono));

            PdfPTable info = new PdfPTable(new float[]{1f, 1f});
            info.setWidthPercentage(100);
            String[][] fields = {
                    {"Bill No", bill.getBillNumber(), "Table", tableNumber},
                    {"Date", DATE.format(bill.getGeneratedAt()), "Guest", session.getCustomerName()},
                    {"Session", session.getSessionNumber(), "Mobile", session.getCustomerMobile()},
            };
            for (String[] row : fields) {
                infoCell(info, row[0] + ": " + nz(row[1]), normal);
                infoCell(info, row[2] + ": " + nz(row[3]), normal);
            }
            document.add(info);
            document.add(rule(mono));

            PdfPTable items = new PdfPTable(new float[]{0.6f, 4.4f, 1f, 1.8f, 2f});
            items.setWidthPercentage(100);
            items.setSpacingBefore(6f);
            header(items, "#", bold, Element.ALIGN_CENTER);
            header(items, "Item", bold, Element.ALIGN_LEFT);
            header(items, "Qty", bold, Element.ALIGN_CENTER);
            header(items, "Rate", bold, Element.ALIGN_RIGHT);
            header(items, "Amount", bold, Element.ALIGN_RIGHT);
            int serial = 1;
            for (OrderView order : orders) {
                for (OrderItemView line : order.items()) {
                    cell(items, String.valueOf(serial++), normal, Element.ALIGN_CENTER);
                    cell(items, line.itemName(), normal, Element.ALIGN_LEFT);
                    cell(items, String.valueOf(line.quantity()), normal, Element.ALIGN_CENTER);
                    cell(items, line.unitPrice().toPlainString(), normal, Element.ALIGN_RIGHT);
                    cell(items, line.totalPrice().toPlainString(), normal, Element.ALIGN_RIGHT);
                }
            }
            document.add(items);

            PdfPTable totals = new PdfPTable(new float[]{3f, 2f});
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setSpacingBefore(8f);
            totalRow(totals, "Subtotal", bill.getSubtotal(), normal);
            totalRow(totals, "SGST", bill.getSgst(), normal);
            totalRow(totals, "CGST", bill.getCgst(), normal);
            totalRow(totals, "Tip", bill.getTipAmount(), normal);
            totalRow(totals, "Grand Total", bill.getGrandTotal(), heading);
            document.add(totals);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Payment status: " + bill.getStatus().name(), normal));
            if (bill.getPaidAt() != null) {
                document.add(new Paragraph("Paid at: " + DATE.format(bill.getPaidAt()), normal));
            }
            document.add(rule(mono));
            document.add(centered("Thank you for dining with us!", bold));
            String footer = settings.value(Key.INVOICE_FOOTER);
            if (!footer.isBlank()) {
                document.add(centered(footer, small));
            }
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate the bill PDF", e);
        }
        return out.toByteArray();
    }

    private static void cell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static void totalRow(PdfPTable table, String label, BigDecimal value, Font font) {
        cell(table, label, font, Element.ALIGN_LEFT);
        cell(table, value == null ? "0.00" : value.toPlainString(), font, Element.ALIGN_RIGHT);
    }

    private static void header(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private static void infoCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2);
        table.addCell(cell);
    }

    private static Paragraph centered(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private static Paragraph rule(Font font) {
        Paragraph paragraph = new Paragraph("-".repeat(90), font);
        paragraph.setSpacingBefore(4f);
        paragraph.setSpacingAfter(4f);
        return paragraph;
    }

    private static String join(String separator, String... parts) {
        return Stream.of(parts)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(separator));
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
