package com.gurukrupa.service;

import com.gurukrupa.data.entities.PurchaseMetalStock;
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
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class MetalStockReportPdfService {

    private static final Logger logger = LoggerFactory.getLogger(MetalStockReportPdfService.class);

    // Colors
    private static final BaseColor HEADER_COLOR = new BaseColor(0, 150, 136); // Teal
    private static final BaseColor TABLE_HEADER_COLOR = new BaseColor(0, 150, 136);
    private static final BaseColor ALT_ROW_COLOR = new BaseColor(250, 250, 250);

    public void generateMetalStockReportPdf(List<PurchaseMetalStock> stocks, String metalType,
                                            String stockStatus, String filePath) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Add Shop Header
            addShopHeader(document);

            // Add Report Title
            addReportTitle(document);

            // Add Filter Information
            addFilterInfo(document, metalType, stockStatus);

            // Add Statistics Summary
            addStatisticsSummary(document, stocks);

            // Add Metal Stock Table
            addStockTable(document, stocks);

            // Add Footer
            addFooter(document);

            document.close();
            logger.info("Metal Stock Report PDF generated successfully: {}", filePath);

            // Open PDF in default application
            openPdfInDefaultApp(filePath);

        } catch (Exception e) {
            logger.error("Error generating Metal Stock Report PDF", e);
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
        Paragraph title = new Paragraph("METAL STOCK REPORT", titleFont);
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

    private void addFilterInfo(Document document, String metalType, String stockStatus) throws DocumentException {
        Font filterFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

        PdfPTable filterTable = new PdfPTable(2);
        filterTable.setWidthPercentage(100);
        filterTable.setSpacingBefore(5);
        filterTable.setSpacingAfter(10);

        addFilterCell(filterTable, "Metal Type: " + (metalType != null ? metalType : "All"), filterFont);
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

    private void addStatisticsSummary(Document document, List<PurchaseMetalStock> stocks) throws DocumentException {
        int metalTypes = stocks.size();
        BigDecimal totalGross = stocks.stream()
                .map(PurchaseMetalStock::getTotalGrossWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableWeight = stocks.stream()
                .map(PurchaseMetalStock::getAvailableWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal usedWeight = stocks.stream()
                .map(PurchaseMetalStock::getUsedWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingBefore(5);
        statsTable.setSpacingAfter(15);

        addStatCell(statsTable, "Metal Types", String.valueOf(metalTypes), labelFont, valueFont, new BaseColor(0, 150, 136));
        addStatCell(statsTable, "Total Gross", WeightFormatter.format(totalGross), labelFont, valueFont, new BaseColor(255, 152, 0));
        addStatCell(statsTable, "Available Wt", WeightFormatter.format(availableWeight), labelFont, valueFont, new BaseColor(76, 175, 80));
        addStatCell(statsTable, "Used Weight", WeightFormatter.format(usedWeight), labelFont, valueFont, new BaseColor(33, 150, 243));

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

    private void addStockTable(Document document, List<PurchaseMetalStock> stocks) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.BLACK);

        PdfPTable table = new PdfPTable(new float[]{0.5f, 2f, 1f, 1f, 1.3f, 1.3f, 1.3f, 1.3f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        // Add headers
        addTableHeader(table, "#", headerFont);
        addTableHeader(table, "Metal Name", headerFont);
        addTableHeader(table, "Metal Type", headerFont);
        addTableHeader(table, "Purity", headerFont);
        addTableHeader(table, "Total Gross (g)", headerFont);
        addTableHeader(table, "Total Net (g)", headerFont);
        addTableHeader(table, "Available (g)", headerFont);
        addTableHeader(table, "Used (g)", headerFont);
        addTableHeader(table, "Utilization %", headerFont);

        // Add data rows
        int index = 1;
        for (PurchaseMetalStock stock : stocks) {
            boolean isAlternate = index % 2 == 0;

            String metalName = stock.getMetal() != null ?
                    stock.getMetal().getMetalName() :
                    stock.getMetalType() + " " + stock.getPurity();

            BigDecimal totalNet = stock.getTotalNetWeight();
            BigDecimal used = stock.getUsedWeight();
            String utilization = "0.00%";
            if (totalNet.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal util = used.multiply(BigDecimal.valueOf(100))
                        .divide(totalNet, 2, RoundingMode.HALF_UP);
                utilization = String.format("%.2f%%", util);
            }

            addTableCell(table, String.valueOf(index++), cellFont, isAlternate);
            addTableCell(table, metalName, cellFont, isAlternate);
            addTableCell(table, stock.getMetalType(), cellFont, isAlternate);
            addTableCell(table, stock.getPurity() != null ? stock.getPurity().toString() : "", cellFont, isAlternate);
            addTableCell(table, WeightFormatter.format(stock.getTotalGrossWeight()), cellFont, isAlternate);
            addTableCell(table, WeightFormatter.format(stock.getTotalNetWeight()), cellFont, isAlternate);
            addTableCell(table, WeightFormatter.format(stock.getAvailableWeight()), cellFont, isAlternate);
            addTableCell(table, WeightFormatter.format(stock.getUsedWeight()), cellFont, isAlternate);
            addTableCell(table, utilization, cellFont, isAlternate);
        }

        document.add(table);
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
