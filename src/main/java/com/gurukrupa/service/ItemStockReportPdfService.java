package com.gurukrupa.service;

import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.utility.WeightFormatter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class ItemStockReportPdfService {

    private static final Logger logger = LoggerFactory.getLogger(ItemStockReportPdfService.class);

    // Colors
    private static final BaseColor HEADER_COLOR = new BaseColor(255, 152, 0); // Orange
    private static final BaseColor TABLE_HEADER_COLOR = new BaseColor(255, 152, 0);
    private static final BaseColor ALT_ROW_COLOR = new BaseColor(250, 250, 250);

    public void generateItemStockReportPdf(List<JewelryItem> items, String metalType, String category,
                                           String stockStatus, String filePath) {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Add Shop Header
            addShopHeader(document);

            // Add Report Title
            addReportTitle(document);

            // Add Filter Information
            addFilterInfo(document, metalType, category, stockStatus);

            // Add Statistics Summary
            addStatisticsSummary(document, items);

            // Add Items Table
            addItemsTable(document, items);

            // Add Footer
            addFooter(document);

            document.close();
            logger.info("Item Stock Report PDF generated successfully: {}", filePath);

            // Open PDF in default application
            openPdfInDefaultApp(filePath);

        } catch (Exception e) {
            logger.error("Error generating Item Stock Report PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void addShopHeader(Document document) throws DocumentException {
        Font shopNameFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
        Font detailsFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);

        Paragraph shopName = new Paragraph("GURUKRUPA JEWELLERS", shopNameFont);
        shopName.setAlignment(Element.ALIGN_CENTER);
        document.add(shopName);

        Paragraph address = new Paragraph("Shop Address | Contact: +91-XXXXXXXXXX | Email: info@gurukrupa.com", detailsFont);
        address.setAlignment(Element.ALIGN_CENTER);
        address.setSpacingAfter(10);
        document.add(address);

        // Add horizontal line
        addSeparatorLine(document);
    }

    private void addReportTitle(Document document) throws DocumentException {
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, HEADER_COLOR);
        Paragraph title = new Paragraph("ITEM STOCK REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(10);
        title.setSpacingAfter(5);
        document.add(title);

        // Add generated date
        Font dateFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);
        String dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new Date());
        Paragraph date = new Paragraph("Generated on: " + dateStr, dateFont);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(10);
        document.add(date);
    }

    private void addFilterInfo(Document document, String metalType, String category, String stockStatus) throws DocumentException {
        Font filterFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

        PdfPTable filterTable = new PdfPTable(3);
        filterTable.setWidthPercentage(100);
        filterTable.setSpacingBefore(5);
        filterTable.setSpacingAfter(10);

        addFilterCell(filterTable, "Metal Type: " + (metalType != null ? metalType : "All"), filterFont);
        addFilterCell(filterTable, "Category: " + (category != null ? category : "All"), filterFont);
        addFilterCell(filterTable, "Stock Status: " + (stockStatus != null ? stockStatus : "All"), filterFont);

        document.add(filterTable);
    }

    private void addFilterCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        cell.setBackgroundColor(new BaseColor(245, 245, 245));
        table.addCell(cell);
    }

    private void addStatisticsSummary(Document document, List<JewelryItem> items) throws DocumentException {
        int totalItems = items.size();
        int totalQuantity = items.stream().mapToInt(JewelryItem::getQuantity).sum();
        BigDecimal totalWeight = items.stream()
                .map(item -> item.getNetWeight().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowStockCount = items.stream()
                .filter(item -> item.getQuantity() > 0 && item.getQuantity() <= 5)
                .count();

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingBefore(5);
        statsTable.setSpacingAfter(15);

        addStatCell(statsTable, "Total Items", String.valueOf(totalItems), labelFont, valueFont, new BaseColor(255, 152, 0));
        addStatCell(statsTable, "Total Quantity", String.valueOf(totalQuantity), labelFont, valueFont, new BaseColor(76, 175, 80));
        addStatCell(statsTable, "Total Weight", WeightFormatter.format(totalWeight), labelFont, valueFont, new BaseColor(33, 150, 243));
        addStatCell(statsTable, "Low Stock", String.valueOf(lowStockCount), labelFont, valueFont, new BaseColor(244, 67, 54));

        document.add(statsTable);
    }

    private void addStatCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(10);
        cell.setBackgroundColor(color);

        Paragraph labelPara = new Paragraph(label, new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.WHITE));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        Paragraph valuePara = new Paragraph(value, new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.WHITE));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        table.addCell(cell);
    }

    private void addItemsTable(Document document, List<JewelryItem> items) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.BLACK);

        PdfPTable table = new PdfPTable(new float[]{0.5f, 1.2f, 1.5f, 1f, 1f, 1f, 1f, 1f, 0.7f, 1f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        // Add headers
        addTableHeader(table, "#", headerFont);
        addTableHeader(table, "Item Code", headerFont);
        addTableHeader(table, "Item Name", headerFont);
        addTableHeader(table, "Category", headerFont);
        addTableHeader(table, "Metal", headerFont);
        addTableHeader(table, "Purity", headerFont);
        addTableHeader(table, "Gross Wt (g)", headerFont);
        addTableHeader(table, "Net Wt (g)", headerFont);
        addTableHeader(table, "Qty", headerFont);
        addTableHeader(table, "Total Wt (g)", headerFont);
        addTableHeader(table, "Status", headerFont);

        // Add data rows
        int index = 1;
        for (JewelryItem item : items) {
            boolean isAlternate = index % 2 == 0;
            BigDecimal totalWeight = item.getNetWeight().multiply(BigDecimal.valueOf(item.getQuantity()));
            String status = getStockStatus(item.getQuantity());

            addTableCell(table, String.valueOf(index++), cellFont, isAlternate);
            addTableCell(table, item.getItemCode(), cellFont, isAlternate);
            addTableCell(table, item.getItemName(), cellFont, isAlternate);
            addTableCell(table, item.getCategory(), cellFont, isAlternate);
            addTableCell(table, item.getMetalType(), cellFont, isAlternate);
            addTableCell(table, item.getPurity() != null ? item.getPurity().toString() : "", cellFont, isAlternate);
            addTableCell(table, WeightFormatter.format(item.getGrossWeight()), cellFont, isAlternate);
            addTableCell(table, WeightFormatter.format(item.getNetWeight()), cellFont, isAlternate);
            addTableCell(table, String.valueOf(item.getQuantity()), cellFont, isAlternate);
            addTableCell(table, WeightFormatter.format(totalWeight), cellFont, isAlternate);
            addTableCell(table, status, cellFont, isAlternate);
        }

        document.add(table);
    }

    private String getStockStatus(int quantity) {
        if (quantity == 0) {
            return "Out of Stock";
        } else if (quantity <= 5) {
            return "Low Stock";
        } else {
            return "In Stock";
        }
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(TABLE_HEADER_COLOR);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, boolean isAlternate) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (isAlternate) {
            cell.setBackgroundColor(ALT_ROW_COLOR);
        }
        table.addCell(cell);
    }

    private void addFooter(Document document) throws DocumentException {
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);
        Paragraph footer = new Paragraph("Generated by Gurukrupa Jewellers Management System", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
    }

    private void addSeparatorLine(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingBefore(5);
        line.setSpacingAfter(10);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(1);
        cell.setBorderColorBottom(BaseColor.LIGHT_GRAY);
        line.addCell(cell);

        document.add(line);
    }

    private void openPdfInDefaultApp(String filePath) {
        try {
            File pdfFile = new File(filePath);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
                logger.info("Opened PDF in default application");
            }
        } catch (IOException e) {
            logger.error("Could not open PDF in default application", e);
        }
    }
}
