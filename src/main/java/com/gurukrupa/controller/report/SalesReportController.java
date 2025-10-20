package com.gurukrupa.controller.report;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import com.gurukrupa.data.entities.Exchange;
import com.gurukrupa.data.entities.ExchangeTransaction;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.data.service.ExchangeService;
import com.gurukrupa.data.service.SalesReportPdfService;
import com.gurukrupa.utility.CurrencyFormatter;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.StageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SalesReportController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SalesReportController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Autowired
    @Lazy
    private StageManager stageManager;

    @Autowired
    private AlertNotification alert;

    @Autowired
    private BillService billService;

    @Autowired
    private SalesReportPdfService salesReportPdfService;

    @Autowired
    private ExchangeService exchangeService;

    // Header Controls
    @FXML private Button btnBack;

    // Filter Controls
    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;
    @FXML private Button btnToday;
    @FXML private Button btnThisWeek;
    @FXML private Button btnThisMonth;
    @FXML private ToggleButton btnAllStatus;
    @FXML private ToggleButton btnPaidStatus;
    @FXML private ToggleButton btnUnpaidStatus;
    @FXML private ToggleGroup statusToggleGroup;
    @FXML private ComboBox<String> cmbPaymentMethod;
    @FXML private Button btnGenerateReport;

    // Statistics Labels
    @FXML private Label lblTotalSales; // Now shows Items Sales Value (subtotal)
    @FXML private Label lblExchangeValue; // Value of metal received from customers
    @FXML private Label lblGrossRevenue; // Total business revenue (sales + exchange)
    @FXML private Label lblCollectedAmount;
    @FXML private Label lblPendingAmount;
    @FXML private Label lblTotalBills;
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalWeight; // Sold weight
    @FXML private Label lblExchangeWeight; // Exchange weight
    // @FXML private Label lblTotalGST; // Removed from UI

    // Breakdown Tables
    @FXML private TableView<MetalBreakdown> tableMetalBreakdown;
    @FXML private TableColumn<MetalBreakdown, String> colMetalType;
    @FXML private TableColumn<MetalBreakdown, String> colMetalQuantity;
    @FXML private TableColumn<MetalBreakdown, String> colMetalWeight;
    @FXML private TableColumn<MetalBreakdown, String> colExchangeWeight;
    @FXML private TableColumn<MetalBreakdown, String> colMetalAmount;

    @FXML private TableView<PaymentBreakdown> tablePaymentBreakdown;
    @FXML private TableColumn<PaymentBreakdown, String> colPaymentMethod;
    @FXML private TableColumn<PaymentBreakdown, String> colPaymentCount;
    @FXML private TableColumn<PaymentBreakdown, String> colPaymentAmount;

    // Transaction Table
    @FXML private TableView<Bill> tableTransactions;
    @FXML private TableColumn<Bill, String> colDate;
    @FXML private TableColumn<Bill, String> colBillNumber;
    @FXML private TableColumn<Bill, String> colCustomerName;
    @FXML private TableColumn<Bill, String> colItemsCount;
    @FXML private TableColumn<Bill, String> colSubtotal;
    @FXML private TableColumn<Bill, String> colGST;
    @FXML private TableColumn<Bill, String> colGrandTotal;
    @FXML private TableColumn<Bill, String> colPaidAmount;
    @FXML private TableColumn<Bill, String> colPendingAmount;
    @FXML private TableColumn<Bill, String> colPaymentMethodCol;
    @FXML private TableColumn<Bill, String> colStatus;

    @FXML private Button btnExportReport;

    // Data
    private ObservableList<Bill> allBills = FXCollections.observableArrayList();
    private ObservableList<MetalBreakdown> metalBreakdowns = FXCollections.observableArrayList();
    private ObservableList<PaymentBreakdown> paymentBreakdowns = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing SalesReportController");

        setupTableColumns();
        setupFilters();
        setupToggleButtons();

        // Set default date range (This Month)
        handleThisMonthFilter();

        // Set default filter to "All Status"
        btnAllStatus.setSelected(true);

        // Auto-generate report on load
        handleGenerateReport();
    }

    private void setupTableColumns() {
        // Transaction table columns
        colDate.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getBillDate().format(DATETIME_FORMATTER)));

        colBillNumber.setCellValueFactory(new PropertyValueFactory<>("billNumber"));

        colCustomerName.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getCustomer().getCustomerFullName()));

        colItemsCount.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getBillTransactions() != null ?
                cellData.getValue().getBillTransactions().size() : 0;
            return new SimpleStringProperty(String.valueOf(count));
        });

        colSubtotal.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getSubtotal())));

        colGST.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getTotalTaxAmount())));

        colGrandTotal.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getGrandTotal())));

        colPaidAmount.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getPaidAmount())));

        colPendingAmount.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getPendingAmount())));

        colPaymentMethodCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(formatPaymentMethod(cellData.getValue().getPaymentMethod())));

        colStatus.setCellValueFactory(cellData -> {
            Bill bill = cellData.getValue();
            if (bill.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return new SimpleStringProperty("Paid");
            } else if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return new SimpleStringProperty("Unpaid");
            } else {
                return new SimpleStringProperty("Partial");
            }
        });

        // Metal breakdown table columns
        colMetalType.setCellValueFactory(new PropertyValueFactory<>("metalType"));
        colMetalQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colMetalWeight.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getWeight())));
        colExchangeWeight.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getExchangeWeight())));
        colMetalAmount.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getAmount())));

        // Payment breakdown table columns
        colPaymentMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colPaymentCount.setCellValueFactory(new PropertyValueFactory<>("count"));
        colPaymentAmount.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getAmount())));

        // Bind data to tables
        tableTransactions.setItems(allBills);
        tableMetalBreakdown.setItems(metalBreakdowns);
        tablePaymentBreakdown.setItems(paymentBreakdowns);
    }

    private void setupFilters() {
        // Setup payment method combo box
        cmbPaymentMethod.setItems(FXCollections.observableArrayList(
            "All Methods", "CASH", "UPI", "CARD", "CHEQUE", "BANK_TRANSFER", "PARTIAL", "CREDIT"
        ));
        cmbPaymentMethod.getSelectionModel().selectFirst();

        // Date picker constraints
        dpFromDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && dpToDate.getValue() != null && newVal.isAfter(dpToDate.getValue())) {
                dpToDate.setValue(newVal);
            }
        });

        dpToDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && dpFromDate.getValue() != null && newVal.isBefore(dpFromDate.getValue())) {
                dpFromDate.setValue(newVal);
            }
        });
    }

    private void setupToggleButtons() {
        statusToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            styleToggleButtons();
        });
        styleToggleButtons();
    }

    private void styleToggleButtons() {
        String activeStyle = "-fx-background-color: %s; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
                           "-fx-font-weight: 600; -fx-padding: 8 16; -fx-background-radius: 6;";
        String inactiveStyle = "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: 'Segoe UI'; " +
                             "-fx-font-weight: 600; -fx-padding: 8 16; -fx-background-radius: 6;";

        if (btnAllStatus.isSelected()) {
            btnAllStatus.setStyle(String.format(activeStyle, "#1976D2"));
        } else {
            btnAllStatus.setStyle(String.format(inactiveStyle, "#E3F2FD", "#1976D2"));
        }

        if (btnPaidStatus.isSelected()) {
            btnPaidStatus.setStyle(String.format(activeStyle, "#388E3C"));
        } else {
            btnPaidStatus.setStyle(String.format(inactiveStyle, "#E8F5E9", "#388E3C"));
        }

        if (btnUnpaidStatus.isSelected()) {
            btnUnpaidStatus.setStyle(String.format(activeStyle, "#D32F2F"));
        } else {
            btnUnpaidStatus.setStyle(String.format(inactiveStyle, "#FFEBEE", "#D32F2F"));
        }
    }

    @FXML
    private void handleBack() {
        try {
            logger.info("Navigating back to Report Menu");
            BorderPane dashboard = (BorderPane) btnBack.getScene().getRoot();
            Parent reportMenu = stageManager.getSpringFXMLLoader().load("/fxml/report/ReportMenu.fxml");
            dashboard.setCenter(reportMenu);
            logger.info("Report Menu loaded successfully");
        } catch (Exception e) {
            logger.error("Error navigating back to Report Menu: {}", e.getMessage());
            e.printStackTrace();
            alert.showError("Error navigating back: " + e.getMessage());
        }
    }

    @FXML
    private void handleTodayFilter() {
        dpFromDate.setValue(LocalDate.now());
        dpToDate.setValue(LocalDate.now());
    }

    @FXML
    private void handleThisWeekFilter() {
        LocalDate now = LocalDate.now();
        dpFromDate.setValue(now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)));
        dpToDate.setValue(now.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)));
    }

    @FXML
    private void handleThisMonthFilter() {
        LocalDate now = LocalDate.now();
        dpFromDate.setValue(now.with(TemporalAdjusters.firstDayOfMonth()));
        dpToDate.setValue(now.with(TemporalAdjusters.lastDayOfMonth()));
    }

    @FXML
    private void handleGenerateReport() {
        try {
            logger.info("Generating sales report");

            // Validate date range
            LocalDate fromDate = dpFromDate.getValue();
            LocalDate toDate = dpToDate.getValue();

            if (fromDate == null || toDate == null) {
                alert.showError("Please select both from and to dates");
                return;
            }

            if (fromDate.isAfter(toDate)) {
                alert.showError("From date cannot be after to date");
                return;
            }

            // Load bills data
            loadBillsData();

            // Apply filters
            applyFilters();

            // Calculate statistics
            calculateStatistics();

            // Generate breakdowns
            generateMetalBreakdown();
            generatePaymentBreakdown();

            alert.showSuccess("Sales report generated successfully!");

        } catch (Exception e) {
            logger.error("Error generating sales report", e);
            alert.showError("Error generating report: " + e.getMessage());
        }
    }

    private void loadBillsData() {
        try {
            LocalDateTime fromDateTime = dpFromDate.getValue().atStartOfDay();
            LocalDateTime toDateTime = dpToDate.getValue().atTime(23, 59, 59);

            List<Bill> bills = billService.findByDateRange(fromDateTime, toDateTime);
            allBills.setAll(bills);

            logger.info("Loaded {} bills for report", bills.size());

        } catch (Exception e) {
            logger.error("Error loading bills data", e);
            throw new RuntimeException("Failed to load bills data", e);
        }
    }

    private void applyFilters() {
        List<Bill> filteredBills = new ArrayList<>(allBills);

        // Apply payment status filter
        if (btnPaidStatus.isSelected()) {
            filteredBills = filteredBills.stream()
                .filter(bill -> bill.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0)
                .collect(Collectors.toList());
        } else if (btnUnpaidStatus.isSelected()) {
            filteredBills = filteredBills.stream()
                .filter(bill -> bill.getPendingAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        }

        // Apply payment method filter
        String selectedPaymentMethod = cmbPaymentMethod.getSelectionModel().getSelectedItem();
        if (selectedPaymentMethod != null && !"All Methods".equals(selectedPaymentMethod)) {
            filteredBills = filteredBills.stream()
                .filter(bill -> bill.getPaymentMethod().name().equals(selectedPaymentMethod))
                .collect(Collectors.toList());
        }

        // Filter out cancelled bills
        filteredBills = filteredBills.stream()
            .filter(bill -> bill.getStatus() != Bill.BillStatus.CANCELLED)
            .collect(Collectors.toList());

        allBills.setAll(filteredBills);
    }

    private void calculateStatistics() {
        List<Bill> bills = new ArrayList<>(allBills);

        // Total Bills
        int totalBills = bills.size();
        lblTotalBills.setText(String.valueOf(totalBills));

        // Items Sales Value (subtotal - value of jewelry sold)
        BigDecimal itemsSalesValue = bills.stream()
            .map(Bill::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalSales.setText(CurrencyFormatter.format(itemsSalesValue));

        // Exchange Value Received (value of metal received from customers)
        BigDecimal exchangeValueReceived = bills.stream()
            .map(Bill::getExchangeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblExchangeValue.setText(CurrencyFormatter.format(exchangeValueReceived));

        // Gross Revenue (total business value = items sold + metal received)
        BigDecimal grossRevenue = itemsSalesValue.add(exchangeValueReceived);
        lblGrossRevenue.setText(CurrencyFormatter.format(grossRevenue));

        // Collected Amount
        BigDecimal collectedAmount = bills.stream()
            .map(Bill::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblCollectedAmount.setText(CurrencyFormatter.format(collectedAmount));

        // Pending Amount
        BigDecimal pendingAmount = bills.stream()
            .map(Bill::getPendingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblPendingAmount.setText(CurrencyFormatter.format(pendingAmount));

        // Total Items Sold
        int totalItems = bills.stream()
            .mapToInt(bill -> bill.getBillTransactions() != null ?
                bill.getBillTransactions().size() : 0)
            .sum();
        lblTotalItems.setText(String.valueOf(totalItems));

        // Total Sold Weight
        BigDecimal totalWeight = bills.stream()
            .flatMap(bill -> bill.getBillTransactions().stream())
            .map(BillTransaction::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalWeight.setText(String.format("%.3fg", totalWeight));

        // Total Exchange Weight
        BigDecimal totalExchangeWeight = BigDecimal.ZERO;
        for (Bill bill : bills) {
            Optional<Exchange> exchangeOpt = exchangeService.findByBillId(bill.getId());
            if (exchangeOpt.isPresent()) {
                Exchange exchange = exchangeOpt.get();
                if (exchange.getExchangeTransactions() != null) {
                    for (ExchangeTransaction transaction : exchange.getExchangeTransactions()) {
                        totalExchangeWeight = totalExchangeWeight.add(transaction.getNetWeight());
                    }
                }
            }
        }
        lblExchangeWeight.setText(String.format("%.3fg", totalExchangeWeight));

        // Total GST - Removed from UI
        // BigDecimal totalGST = bills.stream()
        //     .map(Bill::getTotalTaxAmount)
        //     .reduce(BigDecimal.ZERO, BigDecimal::add);
        // lblTotalGST.setText(CurrencyFormatter.format(totalGST));
    }

    private void generateMetalBreakdown() {
        Map<String, MetalBreakdown> breakdownMap = new HashMap<>();

        // Process sold items
        for (Bill bill : allBills) {
            for (BillTransaction transaction : bill.getBillTransactions()) {
                String metalType = transaction.getMetalType();

                MetalBreakdown breakdown = breakdownMap.getOrDefault(metalType,
                    new MetalBreakdown(metalType, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                breakdown.setQuantity(breakdown.getQuantity() + transaction.getQuantity());
                breakdown.setWeight(breakdown.getWeight().add(transaction.getWeight()));
                breakdown.setAmount(breakdown.getAmount().add(transaction.getTotalAmount()));

                breakdownMap.put(metalType, breakdown);
            }

            // Process exchanges
            Optional<Exchange> exchangeOpt = exchangeService.findByBillId(bill.getId());
            if (exchangeOpt.isPresent()) {
                Exchange exchange = exchangeOpt.get();
                if (exchange.getExchangeTransactions() != null) {
                    for (ExchangeTransaction exchangeTransaction : exchange.getExchangeTransactions()) {
                        String metalType = exchangeTransaction.getMetalType();

                        MetalBreakdown breakdown = breakdownMap.getOrDefault(metalType,
                            new MetalBreakdown(metalType, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                        breakdown.setExchangeWeight(breakdown.getExchangeWeight().add(exchangeTransaction.getNetWeight()));

                        breakdownMap.put(metalType, breakdown);
                    }
                }
            }
        }

        metalBreakdowns.setAll(breakdownMap.values());
    }

    private void generatePaymentBreakdown() {
        Map<String, PaymentBreakdown> breakdownMap = new HashMap<>();

        for (Bill bill : allBills) {
            String paymentMethod = formatPaymentMethod(bill.getPaymentMethod());

            PaymentBreakdown breakdown = breakdownMap.getOrDefault(paymentMethod,
                new PaymentBreakdown(paymentMethod, 0, BigDecimal.ZERO));

            breakdown.setCount(breakdown.getCount() + 1);
            breakdown.setAmount(breakdown.getAmount().add(bill.getGrandTotal()));

            breakdownMap.put(paymentMethod, breakdown);
        }

        paymentBreakdowns.setAll(breakdownMap.values());
    }

    @FXML
    private void handleExportReport() {
        try {
            logger.info("Exporting sales report to PDF");

            // Validate that report has been generated
            if (allBills.isEmpty()) {
                alert.showError("Please generate a report first before exporting");
                return;
            }

            // Validate date range
            LocalDate fromDate = dpFromDate.getValue();
            LocalDate toDate = dpToDate.getValue();

            if (fromDate == null || toDate == null) {
                alert.showError("Please select date range");
                return;
            }

            // Create D:/software directory if it doesn't exist
            String directoryPath = "D:/software";
            Path directory = Paths.get(directoryPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                logger.info("Created directory: {}", directoryPath);
            }

            // Generate filename based on date range
            String filename = String.format("SalesReport_%s_to_%s.pdf",
                fromDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                toDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            String filePath = directoryPath + File.separator + filename;

            // Generate PDF
            List<Bill> billsToExport = new ArrayList<>(allBills);
            salesReportPdfService.generateSalesReportPdf(billsToExport, fromDate, toDate, filePath);

            logger.info("Sales report PDF generated successfully at: {}", filePath);

            // Open PDF in default application
            File pdfFile = new File(filePath);
            if (pdfFile.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        desktop.open(pdfFile);
                        logger.info("Opened PDF in default application");
                    }
                }
            }

            alert.showSuccess("Sales report exported successfully!\nSaved to: " + filePath);

        } catch (Exception e) {
            logger.error("Error exporting sales report", e);
            e.printStackTrace();
            alert.showError("Error exporting report: " + e.getMessage());
        }
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

    // Inner classes for breakdown data
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetalBreakdown {
        private String metalType;
        private int quantity;
        private BigDecimal weight;
        private BigDecimal exchangeWeight;
        private BigDecimal amount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentBreakdown {
        private String paymentMethod;
        private int count;
        private BigDecimal amount;
    }
}
