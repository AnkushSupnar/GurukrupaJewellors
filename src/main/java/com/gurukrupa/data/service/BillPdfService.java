package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import com.gurukrupa.data.entities.Exchange;
import com.gurukrupa.data.entities.ExchangeTransaction;
import com.gurukrupa.data.entities.ShopInfo;
import com.gurukrupa.data.repository.ShopInfoRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BillPdfService {

    @Autowired
    private ShopInfoRepository shopInfoRepository;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

    public void generateBillPdf(Bill bill, String filePath) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        addBillContent(document, bill);

        document.close();
    }

    public byte[] generateBillPdfBytes(Bill bill) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        addBillContent(document, bill);

        document.close();
        return baos.toByteArray();
    }

    private void addBillContent(Document document, Bill bill) throws Exception {
        // Fonts
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font subHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

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

            if (shopInfo.getGstinNumber() != null) {
                Paragraph gstin = new Paragraph("GSTIN: " + shopInfo.getGstinNumber(), normalFont);
                gstin.setAlignment(Element.ALIGN_CENTER);
                document.add(gstin);
            }
        }

        document.add(new Paragraph(" "));

        // Invoice Title
        Paragraph invoiceTitle = new Paragraph("TAX INVOICE", headerFont);
        invoiceTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(invoiceTitle);

        document.add(new Paragraph(" "));

        // Invoice Details Table
        PdfPTable invoiceDetailsTable = new PdfPTable(2);
        invoiceDetailsTable.setWidthPercentage(100);
        invoiceDetailsTable.setSpacingBefore(10f);

        // Left side - Customer details
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("Bill To:", subHeaderFont));
        leftCell.addElement(new Paragraph(bill.getCustomer().getCustomerFullName(), normalFont));
        leftCell.addElement(new Paragraph(bill.getCustomer().getCustomerAddress(), normalFont));
        leftCell.addElement(new Paragraph("Mobile: " + bill.getCustomer().getMobile(), normalFont));

        // Right side - Invoice details
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Paragraph("Invoice No: " + bill.getBillNumber(), normalFont));
        rightCell.addElement(new Paragraph("Date: " + bill.getBillDate().format(dateFormatter), normalFont));
        rightCell.addElement(new Paragraph("Time: " + bill.getBillDate().format(timeFormatter), normalFont));

        invoiceDetailsTable.addCell(leftCell);
        invoiceDetailsTable.addCell(rightCell);
        document.add(invoiceDetailsTable);

        document.add(new Paragraph(" "));

        // Get bill transactions (sales) and exchange transactions
        List<BillTransaction> billTransactions = bill.getBillTransactions();
        List<ExchangeTransaction> exchangeTransactions = (bill.getExchange() != null && bill.getExchange().getExchangeTransactions() != null) 
            ? bill.getExchange().getExchangeTransactions() 
            : new ArrayList<>();

        // Sale Items Table
        if (!billTransactions.isEmpty()) {
            Paragraph saleHeader = new Paragraph("Sale Items", subHeaderFont);
            document.add(saleHeader);

            PdfPTable saleTable = new PdfPTable(9);
            saleTable.setWidthPercentage(100);
            saleTable.setSpacingBefore(5f);
            saleTable.setWidths(new float[]{0.5f, 0.8f, 2f, 0.8f, 0.5f, 0.8f, 0.8f, 0.8f, 1.2f});

            // Headers
            addTableHeader(saleTable, new String[]{"S.No", "Code", "Item Name", "Metal", "Qty", "Weight", "Rate/10g", "Labour %", "Amount"}, smallFont);

            // Data rows
            int sno = 1;
            for (BillTransaction transaction : billTransactions) {
                saleTable.addCell(createCell(String.valueOf(sno++), smallFont, Element.ALIGN_CENTER));
                saleTable.addCell(createCell(transaction.getItemCode(), smallFont, Element.ALIGN_LEFT));
                saleTable.addCell(createCell(transaction.getItemName(), smallFont, Element.ALIGN_LEFT));
                saleTable.addCell(createCell(transaction.getMetalType(), smallFont, Element.ALIGN_CENTER));
                saleTable.addCell(createCell(String.valueOf(transaction.getQuantity()), smallFont, Element.ALIGN_CENTER));
                saleTable.addCell(createCell(formatDecimal(transaction.getWeight()) + "g", smallFont, Element.ALIGN_RIGHT));
                saleTable.addCell(createCell(formatDecimal(transaction.getRatePerTenGrams()), smallFont, Element.ALIGN_RIGHT));
                // Calculate labour percentage
                BigDecimal goldValue = transaction.getWeight().multiply(transaction.getRatePerTenGrams()).divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
                BigDecimal labourPercentage = BigDecimal.ZERO;
                if (goldValue.compareTo(BigDecimal.ZERO) > 0 && transaction.getLabourCharges() != null) {
                    labourPercentage = transaction.getLabourCharges().multiply(BigDecimal.valueOf(100)).divide(goldValue, 1, RoundingMode.HALF_UP);
                }
                saleTable.addCell(createCell(labourPercentage + "%", smallFont, Element.ALIGN_RIGHT));
                saleTable.addCell(createCell("₹ " + formatDecimal(transaction.getTotalAmount()), smallFont, Element.ALIGN_RIGHT));
            }

            document.add(saleTable);
            document.add(new Paragraph(" "));
        }

        // Exchange Items Table
        if (!exchangeTransactions.isEmpty()) {
            Paragraph exchangeHeader = new Paragraph("Exchange Items", subHeaderFont);
            document.add(exchangeHeader);

            PdfPTable exchangeTable = new PdfPTable(7);
            exchangeTable.setWidthPercentage(100);
            exchangeTable.setSpacingBefore(5f);
            exchangeTable.setWidths(new float[]{0.5f, 2.5f, 1f, 1f, 1f, 1f, 1.5f});

            // Headers
            addTableHeader(exchangeTable, new String[]{"S.No", "Item Name", "Metal", "Gross Wt", "Deduction", "Net Wt", "Amount"}, smallFont);

            // Data rows
            int sno = 1;
            for (ExchangeTransaction transaction : exchangeTransactions) {
                exchangeTable.addCell(createCell(String.valueOf(sno++), smallFont, Element.ALIGN_CENTER));
                exchangeTable.addCell(createCell(transaction.getItemName(), smallFont, Element.ALIGN_LEFT));
                exchangeTable.addCell(createCell(transaction.getMetalType(), smallFont, Element.ALIGN_CENTER));
                exchangeTable.addCell(createCell(formatDecimal(transaction.getGrossWeight()) + "g", smallFont, Element.ALIGN_RIGHT));
                exchangeTable.addCell(createCell(formatDecimal(transaction.getDeduction()) + "g", smallFont, Element.ALIGN_RIGHT));
                exchangeTable.addCell(createCell(formatDecimal(transaction.getNetWeight()) + "g", smallFont, Element.ALIGN_RIGHT));
                exchangeTable.addCell(createCell("₹ " + formatDecimal(transaction.getTotalAmount()), smallFont, Element.ALIGN_RIGHT));
            }

            document.add(exchangeTable);
            document.add(new Paragraph(" "));
        }

        // Summary Table
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setSpacingBefore(20f);

        addSummaryRow(summaryTable, "Subtotal:", "₹ " + formatDecimal(bill.getSubtotal()), normalFont);
        if (bill.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(summaryTable, "Discount:", "-₹ " + formatDecimal(bill.getDiscount()), normalFont);
        }
        addSummaryRow(summaryTable, "Net Total:", "₹ " + formatDecimal(bill.getNetTotal()), normalFont);
        addSummaryRow(summaryTable, "CGST (" + bill.getGstRate().divide(BigDecimal.valueOf(2)) + "%):", "₹ " + formatDecimal(bill.getCgstAmount()), normalFont);
        addSummaryRow(summaryTable, "SGST (" + bill.getGstRate().divide(BigDecimal.valueOf(2)) + "%):", "₹ " + formatDecimal(bill.getSgstAmount()), normalFont);
        
        // Add total after GST
        BigDecimal totalAfterGST = bill.getNetTotal().add(bill.getTotalTaxAmount());
        addSummaryRow(summaryTable, "Total after GST:", "₹ " + formatDecimal(totalAfterGST), normalFont);
        
        if (bill.getExchangeAmount() != null && bill.getExchangeAmount().compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(summaryTable, "Less: Exchange Amount:", "-₹ " + formatDecimal(bill.getExchangeAmount()), normalFont);
        }
        
        // Grand Total
        PdfPCell grandTotalLabel = new PdfPCell(new Paragraph("Grand Total:", headerFont));
        grandTotalLabel.setBorder(Rectangle.TOP);
        grandTotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        grandTotalLabel.setPadding(5);

        PdfPCell grandTotalValue = new PdfPCell(new Paragraph("₹ " + formatDecimal(bill.getGrandTotal()), headerFont));
        grandTotalValue.setBorder(Rectangle.TOP);
        grandTotalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        grandTotalValue.setPadding(5);

        summaryTable.addCell(grandTotalLabel);
        summaryTable.addCell(grandTotalValue);

        document.add(summaryTable);

        // Payment Details Section
        document.add(new Paragraph(" "));
        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(50);
        paymentTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        paymentTable.setSpacingBefore(10f);

        // Payment Method
        Font paymentFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
        String paymentMethodDisplay = formatPaymentMethod(bill.getPaymentMethod());
        addSummaryRow(paymentTable, "Payment Method:", paymentMethodDisplay, paymentFont);
        
        // Paid Amount
        addSummaryRow(paymentTable, "Paid Amount:", "₹ " + formatDecimal(bill.getPaidAmount()), paymentFont);
        
        // Pending Amount (if any)
        if (bill.getPendingAmount().compareTo(BigDecimal.ZERO) > 0) {
            Font pendingFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.RED);
            PdfPCell pendingLabel = new PdfPCell(new Paragraph("Pending Amount:", pendingFont));
            pendingLabel.setBorder(Rectangle.NO_BORDER);
            pendingLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pendingLabel.setPadding(5);

            PdfPCell pendingValue = new PdfPCell(new Paragraph("₹ " + formatDecimal(bill.getPendingAmount()), pendingFont));
            pendingValue.setBorder(Rectangle.NO_BORDER);
            pendingValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pendingValue.setPadding(5);

            paymentTable.addCell(pendingLabel);
            paymentTable.addCell(pendingValue);
        } else {
            // Show payment status as PAID
            Font paidFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.GREEN.darker());
            addSummaryRow(paymentTable, "Payment Status:", "PAID", paidFont);
        }

        document.add(paymentTable);

        // Footer
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        Paragraph footer = new Paragraph("Thank you for your business!", normalFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        // Terms and conditions
        if (shopInfo != null && shopInfo.getTermsAndConditions() != null) {
            document.add(new Paragraph(" "));
            Paragraph termsHeader = new Paragraph("Terms & Conditions:", smallFont);
            document.add(termsHeader);
            Paragraph terms = new Paragraph(shopInfo.getTermsAndConditions(), smallFont);
            document.add(terms);
        }
    }

    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(3);
        return cell;
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(3);

        PdfPCell valueCell = new PdfPCell(new Paragraph(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(3);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) return "0.00";
        return String.format("%.2f", value);
    }
    
    private String formatPaymentMethod(Bill.PaymentMethod paymentMethod) {
        if (paymentMethod == null) return "N/A";
        switch (paymentMethod) {
            case CASH:
                return "Cash";
            case UPI:
                return "UPI";
            case CARD:
                return "Card";
            case CHEQUE:
                return "Cheque";
            case BANK_TRANSFER:
                return "Bank Transfer";
            case PARTIAL:
                return "Partial Payment";
            case CREDIT:
                return "Credit (Pending)";
            default:
                return paymentMethod.toString();
        }
    }
}