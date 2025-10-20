package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import com.gurukrupa.data.entities.ShopInfo;
import com.gurukrupa.data.repository.ShopInfoRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesReportPdfService {

    @Autowired
    private ShopInfoRepository shopInfoRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void generateSalesReportPdf(List<Bill> bills, LocalDate fromDate, LocalDate toDate, String filePath) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        addSalesReportContent(document, bills, fromDate, toDate);

        document.close();
    }

    private void addSalesReportContent(Document document, List<Bill> bills, LocalDate fromDate, LocalDate toDate) throws Exception {
        // Fonts
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font subHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

        // Get shop info
        ShopInfo shopInfo = shopInfoRepository.findAll().stream().findFirst().orElse(null);

        // Shop Header
        if (shopInfo != null) {
            Paragraph shopName = new Paragraph(shopInfo.getShopName(), titleFont);
            shopName.setAlignment(Element.ALIGN_CENTER);
            document.add(shopName);

            Paragraph shopAddress = new Paragraph(shopInfo.getShopAddress(), normalFont);
            shopAddress.setAlignment(Element.ALIGN_CENTER);
            document.add(shopAddress);

            if (shopInfo.getShopMobile() != null || shopInfo.getShopEmail() != null) {
                String contactInfo = "";
                if (shopInfo.getShopMobile() != null) contactInfo += "Ph: " + shopInfo.getShopMobile();
                if (shopInfo.getShopEmail() != null) {
                    if (!contactInfo.isEmpty()) contactInfo += " | ";
                    contactInfo += "Email: " + shopInfo.getShopEmail();
                }
                Paragraph contact = new Paragraph(contactInfo, normalFont);
                contact.setAlignment(Element.ALIGN_CENTER);
                document.add(contact);
            }
        }

        document.add(new Paragraph(" "));

        // Report Title
        Paragraph reportTitle = new Paragraph("SALES REPORT", headerFont);
        reportTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(reportTitle);

        // Date Range
        String dateRangeText = "Period: " + fromDate.format(DATE_FORMATTER) + " to " + toDate.format(DATE_FORMATTER);
        Paragraph dateRange = new Paragraph(dateRangeText, normalFont);
        dateRange.setAlignment(Element.ALIGN_CENTER);
        document.add(dateRange);

        document.add(new Paragraph(" "));

        // Calculate Statistics
        SalesStatistics stats = calculateStatistics(bills);

        // Statistics Section
        addStatisticsSection(document, stats, boldFont, normalFont);

        document.add(new Paragraph(" "));

        // Metal Type Breakdown
        addMetalBreakdownSection(document, bills, subHeaderFont, smallFont);

        document.add(new Paragraph(" "));

        // Payment Method Breakdown
        addPaymentBreakdownSection(document, bills, subHeaderFont, smallFont);

        // New Page for Transactions
        document.newPage();

        // Detailed Transactions Table
        addTransactionsTable(document, bills, subHeaderFont, smallFont);
    }

    private void addStatisticsSection(Document document, SalesStatistics stats, Font boldFont, Font normalFont) throws DocumentException {
        Paragraph statsHeader = new Paragraph("Sales Summary", boldFont);
        statsHeader.setSpacingAfter(10);
        document.add(statsHeader);

        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(10);

        // Header row with background color
        BaseColor headerColor = new BaseColor(33, 150, 243);
        PdfPCell[] headers = {
            createCell("Total Sales", boldFont, headerColor, BaseColor.WHITE),
            createCell("Total Bills", boldFont, headerColor, BaseColor.WHITE),
            createCell("Avg Bill Value", boldFont, headerColor, BaseColor.WHITE),
            createCell("Total Items", boldFont, headerColor, BaseColor.WHITE)
        };
        for (PdfPCell header : headers) {
            statsTable.addCell(header);
        }

        // Data row
        statsTable.addCell(createCell("₹" + formatCurrency(stats.totalSales), normalFont, BaseColor.WHITE, BaseColor.BLACK));
        statsTable.addCell(createCell(String.valueOf(stats.totalBills), normalFont, BaseColor.WHITE, BaseColor.BLACK));
        statsTable.addCell(createCell("₹" + formatCurrency(stats.avgBillValue), normalFont, BaseColor.WHITE, BaseColor.BLACK));
        statsTable.addCell(createCell(String.valueOf(stats.totalItems), normalFont, BaseColor.WHITE, BaseColor.BLACK));

        document.add(statsTable);

        // Second row of statistics
        PdfPTable statsTable2 = new PdfPTable(4);
        statsTable2.setWidthPercentage(100);
        statsTable2.setSpacingAfter(10);

        PdfPCell[] headers2 = {
            createCell("Collected Amount", boldFont, headerColor, BaseColor.WHITE),
            createCell("Pending Amount", boldFont, headerColor, BaseColor.WHITE),
            createCell("Total Weight", boldFont, headerColor, BaseColor.WHITE),
            createCell("Total GST", boldFont, headerColor, BaseColor.WHITE)
        };
        for (PdfPCell header : headers2) {
            statsTable2.addCell(header);
        }

        statsTable2.addCell(createCell("₹" + formatCurrency(stats.collectedAmount), normalFont, BaseColor.WHITE, BaseColor.BLACK));
        statsTable2.addCell(createCell("₹" + formatCurrency(stats.pendingAmount), normalFont, BaseColor.WHITE, BaseColor.BLACK));
        statsTable2.addCell(createCell(String.format("%.3fg", stats.totalWeight.doubleValue()), normalFont, BaseColor.WHITE, BaseColor.BLACK));
        statsTable2.addCell(createCell("₹" + formatCurrency(stats.totalGST), normalFont, BaseColor.WHITE, BaseColor.BLACK));

        document.add(statsTable2);
    }

    private void addMetalBreakdownSection(Document document, List<Bill> bills, Font headerFont, Font normalFont) throws DocumentException {
        Paragraph metalHeader = new Paragraph("Metal Type Breakdown", headerFont);
        metalHeader.setSpacingAfter(10);
        document.add(metalHeader);

        Map<String, MetalBreakdown> metalBreakdowns = new HashMap<>();

        for (Bill bill : bills) {
            for (BillTransaction transaction : bill.getBillTransactions()) {
                String metalType = transaction.getMetalType();
                MetalBreakdown breakdown = metalBreakdowns.getOrDefault(metalType,
                    new MetalBreakdown(metalType, 0, BigDecimal.ZERO, BigDecimal.ZERO));

                breakdown.quantity += transaction.getQuantity();
                breakdown.weight = breakdown.weight.add(transaction.getWeight());
                breakdown.amount = breakdown.amount.add(transaction.getTotalAmount());

                metalBreakdowns.put(metalType, breakdown);
            }
        }

        PdfPTable metalTable = new PdfPTable(4);
        metalTable.setWidthPercentage(100);
        metalTable.setWidths(new float[]{3, 2, 2, 3});

        BaseColor headerColor = new BaseColor(255, 152, 0);
        metalTable.addCell(createCell("Metal Type", headerFont, headerColor, BaseColor.WHITE));
        metalTable.addCell(createCell("Quantity", headerFont, headerColor, BaseColor.WHITE));
        metalTable.addCell(createCell("Weight (g)", headerFont, headerColor, BaseColor.WHITE));
        metalTable.addCell(createCell("Amount", headerFont, headerColor, BaseColor.WHITE));

        for (MetalBreakdown breakdown : metalBreakdowns.values()) {
            metalTable.addCell(createCell(breakdown.metalType, normalFont, BaseColor.WHITE, BaseColor.BLACK));
            metalTable.addCell(createCell(String.valueOf(breakdown.quantity), normalFont, BaseColor.WHITE, BaseColor.BLACK));
            metalTable.addCell(createCell(String.format("%.3f", breakdown.weight.doubleValue()), normalFont, BaseColor.WHITE, BaseColor.BLACK));
            metalTable.addCell(createCell("₹" + formatCurrency(breakdown.amount), normalFont, BaseColor.WHITE, BaseColor.BLACK));
        }

        document.add(metalTable);
    }

    private void addPaymentBreakdownSection(Document document, List<Bill> bills, Font headerFont, Font normalFont) throws DocumentException {
        Paragraph paymentHeader = new Paragraph("Payment Method Breakdown", headerFont);
        paymentHeader.setSpacingAfter(10);
        document.add(paymentHeader);

        Map<String, PaymentBreakdown> paymentBreakdowns = new HashMap<>();

        for (Bill bill : bills) {
            String paymentMethod = formatPaymentMethod(bill.getPaymentMethod());
            PaymentBreakdown breakdown = paymentBreakdowns.getOrDefault(paymentMethod,
                new PaymentBreakdown(paymentMethod, 0, BigDecimal.ZERO));

            breakdown.count++;
            breakdown.amount = breakdown.amount.add(bill.getGrandTotal());

            paymentBreakdowns.put(paymentMethod, breakdown);
        }

        PdfPTable paymentTable = new PdfPTable(3);
        paymentTable.setWidthPercentage(100);
        paymentTable.setWidths(new float[]{4, 2, 4});

        BaseColor headerColor = new BaseColor(76, 175, 80);
        paymentTable.addCell(createCell("Payment Method", headerFont, headerColor, BaseColor.WHITE));
        paymentTable.addCell(createCell("Count", headerFont, headerColor, BaseColor.WHITE));
        paymentTable.addCell(createCell("Amount", headerFont, headerColor, BaseColor.WHITE));

        for (PaymentBreakdown breakdown : paymentBreakdowns.values()) {
            paymentTable.addCell(createCell(breakdown.paymentMethod, normalFont, BaseColor.WHITE, BaseColor.BLACK));
            paymentTable.addCell(createCell(String.valueOf(breakdown.count), normalFont, BaseColor.WHITE, BaseColor.BLACK));
            paymentTable.addCell(createCell("₹" + formatCurrency(breakdown.amount), normalFont, BaseColor.WHITE, BaseColor.BLACK));
        }

        document.add(paymentTable);
    }

    private void addTransactionsTable(Document document, List<Bill> bills, Font headerFont, Font smallFont) throws DocumentException {
        Paragraph transHeader = new Paragraph("Sales Transactions", headerFont);
        transHeader.setSpacingAfter(10);
        document.add(transHeader);

        PdfPTable transTable = new PdfPTable(7);
        transTable.setWidthPercentage(100);
        transTable.setWidths(new float[]{2, 2.5f, 3, 1.5f, 2, 2, 2});

        BaseColor headerColor = new BaseColor(33, 150, 243);
        transTable.addCell(createCell("Date", headerFont, headerColor, BaseColor.WHITE));
        transTable.addCell(createCell("Bill No", headerFont, headerColor, BaseColor.WHITE));
        transTable.addCell(createCell("Customer", headerFont, headerColor, BaseColor.WHITE));
        transTable.addCell(createCell("Items", headerFont, headerColor, BaseColor.WHITE));
        transTable.addCell(createCell("Total", headerFont, headerColor, BaseColor.WHITE));
        transTable.addCell(createCell("Paid", headerFont, headerColor, BaseColor.WHITE));
        transTable.addCell(createCell("Pending", headerFont, headerColor, BaseColor.WHITE));

        for (Bill bill : bills) {
            transTable.addCell(createCell(bill.getBillDate().format(DATETIME_FORMATTER), smallFont, BaseColor.WHITE, BaseColor.BLACK));
            transTable.addCell(createCell(bill.getBillNumber(), smallFont, BaseColor.WHITE, BaseColor.BLACK));
            transTable.addCell(createCell(bill.getCustomer().getCustomerFullName(), smallFont, BaseColor.WHITE, BaseColor.BLACK));
            transTable.addCell(createCell(String.valueOf(bill.getBillTransactions().size()), smallFont, BaseColor.WHITE, BaseColor.BLACK));
            transTable.addCell(createCell("₹" + formatCurrency(bill.getGrandTotal()), smallFont, BaseColor.WHITE, BaseColor.BLACK));
            transTable.addCell(createCell("₹" + formatCurrency(bill.getPaidAmount()), smallFont, BaseColor.WHITE, BaseColor.BLACK));
            transTable.addCell(createCell("₹" + formatCurrency(bill.getPendingAmount()), smallFont, BaseColor.WHITE, BaseColor.BLACK));
        }

        document.add(transTable);

        // Add footer with generation timestamp
        document.add(new Paragraph(" "));
        Paragraph footer = new Paragraph("Generated on: " + LocalDate.now().format(DATE_FORMATTER), smallFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);
    }

    private PdfPCell createCell(String text, Font font, BaseColor bgColor, BaseColor textColor) {
        Phrase phrase = new Phrase(text, font);
        phrase.getFont().setColor(textColor);
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(8);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount.doubleValue());
    }

    private String formatPaymentMethod(Bill.PaymentMethod method) {
        if (method == null) return "N/A";
        switch (method) {
            case CASH: return "Cash";
            case UPI: return "UPI";
            case CARD: return "Card";
            case CHEQUE: return "Cheque";
            case BANK_TRANSFER: return "Bank Transfer";
            case PARTIAL: return "Partial";
            case CREDIT: return "Credit";
            default: return method.name();
        }
    }

    private SalesStatistics calculateStatistics(List<Bill> bills) {
        SalesStatistics stats = new SalesStatistics();

        stats.totalBills = bills.size();

        stats.totalSales = bills.stream()
            .map(Bill::getGrandTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.avgBillValue = stats.totalBills > 0 ?
            stats.totalSales.divide(BigDecimal.valueOf(stats.totalBills), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;

        stats.collectedAmount = bills.stream()
            .map(Bill::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.pendingAmount = bills.stream()
            .map(Bill::getPendingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.totalItems = bills.stream()
            .mapToInt(bill -> bill.getBillTransactions() != null ?
                bill.getBillTransactions().size() : 0)
            .sum();

        stats.totalWeight = bills.stream()
            .flatMap(bill -> bill.getBillTransactions().stream())
            .map(BillTransaction::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.totalGST = bills.stream()
            .map(Bill::getTotalTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return stats;
    }

    // Inner classes for data structures
    private static class SalesStatistics {
        int totalBills;
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal avgBillValue = BigDecimal.ZERO;
        BigDecimal collectedAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        int totalItems;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalGST = BigDecimal.ZERO;
    }

    private static class MetalBreakdown {
        String metalType;
        int quantity;
        BigDecimal weight;
        BigDecimal amount;

        MetalBreakdown(String metalType, int quantity, BigDecimal weight, BigDecimal amount) {
            this.metalType = metalType;
            this.quantity = quantity;
            this.weight = weight;
            this.amount = amount;
        }
    }

    private static class PaymentBreakdown {
        String paymentMethod;
        int count;
        BigDecimal amount;

        PaymentBreakdown(String paymentMethod, int count, BigDecimal amount) {
            this.paymentMethod = paymentMethod;
            this.count = count;
            this.amount = amount;
        }
    }
}
