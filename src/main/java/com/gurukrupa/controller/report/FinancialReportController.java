package com.gurukrupa.controller.report;

import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.data.service.ExchangeService;
import com.gurukrupa.data.service.PurchaseInvoiceService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FinancialReportController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FinancialReportController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    @Lazy
    private StageManager stageManager;

    @Autowired
    private AlertNotification alert;

    @Autowired
    private BillService billService;

    @Autowired
    private PurchaseInvoiceService purchaseInvoiceService;

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
    @FXML private Button btnThisYear;
    @FXML private Button btnGenerateReport;

    // Revenue Statistics
    @FXML private Label lblTotalSales; // Items Sales Value (subtotal)
    @FXML private Label lblExchangeValue; // Exchange Value Received
    @FXML private Label lblGrossRevenue; // Gross Revenue (sales + exchange)
    @FXML private Label lblCollectedAmount;
    @FXML private Label lblPendingReceivables;
    @FXML private Label lblSalesGST;

    // Expense Statistics
    @FXML private Label lblTotalPurchases;
    @FXML private Label lblPaidToPurchases;
    @FXML private Label lblPendingPayables;
    @FXML private Label lblPurchaseGST;

    // Profit/Loss Statistics
    @FXML private Label lblGrossProfit;
    @FXML private Label lblNetProfit;
    @FXML private Label lblNetGST;
    @FXML private Label lblProfitMargin;

    // Cash Flow Statistics
    @FXML private Label lblCashInflow;
    @FXML private Label lblCashOutflow;
    @FXML private Label lblNetCashFlow;
    @FXML private Label lblTotalBills;
    @FXML private Label lblTotalPurchaseInvoices;

    // Metal Inventory P/L Table
    @FXML private TableView<MetalInventory> tableMetalInventory;
    @FXML private TableColumn<MetalInventory, String> colInvMetalType;
    @FXML private TableColumn<MetalInventory, String> colMetalPurchased;
    @FXML private TableColumn<MetalInventory, String> colMetalFromCustomers;
    @FXML private TableColumn<MetalInventory, String> colMetalSold;
    @FXML private TableColumn<MetalInventory, String> colMetalToSuppliers;
    @FXML private TableColumn<MetalInventory, String> colMetalBalance;
    @FXML private TableColumn<MetalInventory, String> colItemsWeight;

    // Breakdown Tables
    @FXML private TableView<MetalMovement> tableMetalMovement;
    @FXML private TableColumn<MetalMovement, String> colMetalType;
    @FXML private TableColumn<MetalMovement, String> colPurchasedWeight;
    @FXML private TableColumn<MetalMovement, String> colExchangedFromCustomers;
    @FXML private TableColumn<MetalMovement, String> colSoldWeight;
    @FXML private TableColumn<MetalMovement, String> colExchangedToSuppliers;
    @FXML private TableColumn<MetalMovement, String> colNetMovement;
    @FXML private TableColumn<MetalMovement, String> colPurchaseValue;
    @FXML private TableColumn<MetalMovement, String> colSalesValue;

    @FXML private TableView<PaymentMethodSummary> tablePaymentSummary;
    @FXML private TableColumn<PaymentMethodSummary, String> colPaymentMethod;
    @FXML private TableColumn<PaymentMethodSummary, String> colSalesAmount;
    @FXML private TableColumn<PaymentMethodSummary, String> colPurchaseAmount;
    @FXML private TableColumn<PaymentMethodSummary, String> colNetAmount;

    // Data
    private ObservableList<MetalInventory> metalInventories = FXCollections.observableArrayList();
    private ObservableList<MetalMovement> metalMovements = FXCollections.observableArrayList();
    private ObservableList<PaymentMethodSummary> paymentSummaries = FXCollections.observableArrayList();

    private List<Bill> bills = new ArrayList<>();
    private List<PurchaseInvoice> purchaseInvoices = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing FinancialReportController");

        setupTableColumns();
        setupFilters();

        // Set default date range (This Month) with button styling
        handleThisMonthFilter();

        // Auto-generate report on load
        handleGenerateReport();
    }

    private void setupTableColumns() {
        // Apply constrained resize policy to all tables
        tableMetalInventory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableMetalMovement.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablePaymentSummary.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Apply bold styling to important columns
        colInvMetalType.setStyle("-fx-font-weight: bold;");
        colMetalBalance.setStyle("-fx-font-weight: bold;");
        colMetalType.setStyle("-fx-font-weight: bold;");
        colNetMovement.setStyle("-fx-font-weight: bold;");
        colPaymentMethod.setStyle("-fx-font-weight: bold;");
        colNetAmount.setStyle("-fx-font-weight: bold;");

        // Metal Inventory P/L table columns
        colInvMetalType.setCellValueFactory(new PropertyValueFactory<>("metalType"));
        colMetalPurchased.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getMetalPurchased())));
        colMetalFromCustomers.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getMetalFromCustomers())));
        colMetalSold.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getMetalSold())));
        colMetalToSuppliers.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getMetalToSuppliers())));
        colMetalBalance.setCellValueFactory(cellData -> {
            BigDecimal balance = cellData.getValue().getMetalBalance();
            String sign = balance.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return new SimpleStringProperty(String.format("%s%.3f", sign, balance));
        });
        colItemsWeight.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getItemsWeight())));

        // Metal Movement table columns
        colMetalType.setCellValueFactory(new PropertyValueFactory<>("metalType"));
        colPurchasedWeight.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getPurchasedWeight())));
        colExchangedFromCustomers.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getExchangedFromCustomers())));
        colSoldWeight.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getSoldWeight())));
        colExchangedToSuppliers.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.3f", cellData.getValue().getExchangedToSuppliers())));
        colNetMovement.setCellValueFactory(cellData -> {
            BigDecimal net = cellData.getValue().getNetMovement();
            String sign = net.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return new SimpleStringProperty(String.format("%s%.3f g", sign, net));
        });
        colPurchaseValue.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getPurchaseValue())));
        colSalesValue.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getSalesValue())));

        // Payment Summary table columns
        colPaymentMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colSalesAmount.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getSalesAmount())));
        colPurchaseAmount.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getPurchaseAmount())));
        colNetAmount.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getNetAmount())));

        // Bind data to tables
        tableMetalInventory.setItems(metalInventories);
        tableMetalMovement.setItems(metalMovements);
        tablePaymentSummary.setItems(paymentSummaries);

        // Make tables dynamically sized based on content
        setupDynamicTableHeight(tableMetalInventory, metalInventories);
        setupDynamicTableHeight(tableMetalMovement, metalMovements);
        setupDynamicTableHeight(tablePaymentSummary, paymentSummaries);
    }

    private <T> void setupDynamicTableHeight(TableView<T> table, ObservableList<T> items) {
        // Listen for changes in the items list and adjust height accordingly
        items.addListener((javafx.collections.ListChangeListener.Change<? extends T> change) -> {
            updateTableHeight(table, items.size());
        });
        // Set initial height
        updateTableHeight(table, items.size());
    }

    private void updateTableHeight(TableView<?> table, int itemCount) {
        // Header height + (number of rows * fixed cell size) + small padding
        // If no items, show minimal height (just header + placeholder)
        double headerHeight = 40; // Approximate header height
        double fixedCellSize = 35; // Must match the fixedCellSize in FXML
        double minHeight = headerHeight + 60; // Header + placeholder message
        double padding = 5;

        if (itemCount == 0) {
            table.setPrefHeight(minHeight);
            table.setMinHeight(minHeight);
        } else {
            // Show maximum 10 rows at a time, then add scrollbar
            int visibleRows = Math.min(itemCount, 10);
            double calculatedHeight = headerHeight + (visibleRows * fixedCellSize) + padding;
            table.setPrefHeight(calculatedHeight);
            table.setMinHeight(calculatedHeight);
        }
    }

    private void setupFilters() {
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
        updateQuickFilterButtonStyles(btnToday);
    }

    @FXML
    private void handleThisWeekFilter() {
        LocalDate now = LocalDate.now();
        dpFromDate.setValue(now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)));
        dpToDate.setValue(now.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)));
        updateQuickFilterButtonStyles(btnThisWeek);
    }

    @FXML
    private void handleThisMonthFilter() {
        LocalDate now = LocalDate.now();
        dpFromDate.setValue(now.with(TemporalAdjusters.firstDayOfMonth()));
        dpToDate.setValue(now.with(TemporalAdjusters.lastDayOfMonth()));
        updateQuickFilterButtonStyles(btnThisMonth);
    }

    @FXML
    private void handleThisYearFilter() {
        LocalDate now = LocalDate.now();
        dpFromDate.setValue(now.with(TemporalAdjusters.firstDayOfYear()));
        dpToDate.setValue(now.with(TemporalAdjusters.lastDayOfYear()));
        updateQuickFilterButtonStyles(btnThisYear);
    }

    @FXML
    private void handleGenerateReport() {
        try {
            logger.info("Generating financial report");

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

            // Load data
            loadData();

            // Calculate all statistics
            calculateRevenueStatistics();
            calculateExpenseStatistics();
            calculateProfitLossStatistics();
            calculateCashFlowStatistics();

            // Generate breakdowns
            generateMetalInventoryBreakdown();
            generateMetalMovementBreakdown();
            generatePaymentMethodSummary();

            alert.showSuccess("Financial report generated successfully!");

        } catch (Exception e) {
            logger.error("Error generating financial report", e);
            alert.showError("Error generating report: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            LocalDateTime fromDateTime = dpFromDate.getValue().atStartOfDay();
            LocalDateTime toDateTime = dpToDate.getValue().atTime(23, 59, 59);

            // Load bills (sales)
            bills = billService.findByDateRange(fromDateTime, toDateTime);
            // Filter out cancelled bills
            bills = bills.stream()
                .filter(bill -> bill.getStatus() != Bill.BillStatus.CANCELLED)
                .collect(Collectors.toList());

            // Load purchase invoices
            purchaseInvoices = purchaseInvoiceService.findByDateRange(fromDateTime, toDateTime);
            // Filter out cancelled invoices
            purchaseInvoices = purchaseInvoices.stream()
                .filter(invoice -> invoice.getStatus() != PurchaseInvoice.InvoiceStatus.CANCELLED)
                .collect(Collectors.toList());

            logger.info("Loaded {} bills and {} purchase invoices", bills.size(), purchaseInvoices.size());

        } catch (Exception e) {
            logger.error("Error loading data", e);
            throw new RuntimeException("Failed to load data", e);
        }
    }

    private void calculateRevenueStatistics() {
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

        // Pending Receivables
        BigDecimal pendingReceivables = bills.stream()
            .map(Bill::getPendingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblPendingReceivables.setText(CurrencyFormatter.format(pendingReceivables));

        // Sales GST
        BigDecimal salesGST = bills.stream()
            .map(Bill::getTotalTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblSalesGST.setText(CurrencyFormatter.format(salesGST));
    }

    private void calculateExpenseStatistics() {
        // Total Purchases
        BigDecimal totalPurchases = purchaseInvoices.stream()
            .map(PurchaseInvoice::getGrandTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalPurchases.setText(CurrencyFormatter.format(totalPurchases));

        // Paid to Purchases
        BigDecimal paidAmount = purchaseInvoices.stream()
            .map(PurchaseInvoice::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblPaidToPurchases.setText(CurrencyFormatter.format(paidAmount));

        // Pending Payables
        BigDecimal pendingPayables = purchaseInvoices.stream()
            .map(PurchaseInvoice::getPendingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblPendingPayables.setText(CurrencyFormatter.format(pendingPayables));

        // Purchase GST
        BigDecimal purchaseGST = purchaseInvoices.stream()
            .map(PurchaseInvoice::getGstAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblPurchaseGST.setText(CurrencyFormatter.format(purchaseGST));
    }

    private void calculateProfitLossStatistics() {
        // Get values - use gross revenue (items + exchange)
        BigDecimal itemsSalesValue = bills.stream()
            .map(Bill::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal exchangeValueReceived = bills.stream()
            .map(Bill::getExchangeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossRevenue = itemsSalesValue.add(exchangeValueReceived);

        BigDecimal totalPurchases = purchaseInvoices.stream()
            .map(PurchaseInvoice::getGrandTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal collectedAmount = bills.stream()
            .map(Bill::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidAmount = purchaseInvoices.stream()
            .map(PurchaseInvoice::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal salesGST = bills.stream()
            .map(Bill::getTotalTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal purchaseGST = purchaseInvoices.stream()
            .map(PurchaseInvoice::getGstAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Gross Profit (Gross Revenue - Purchases)
        BigDecimal grossProfit = grossRevenue.subtract(totalPurchases);
        lblGrossProfit.setText(CurrencyFormatter.format(grossProfit));
        setLabelStyle(lblGrossProfit, grossProfit);

        // Net Profit (Collected - Paid)
        BigDecimal netProfit = collectedAmount.subtract(paidAmount);
        lblNetProfit.setText(CurrencyFormatter.format(netProfit));
        setLabelStyle(lblNetProfit, netProfit);

        // Net GST (GST Collected - GST Paid)
        BigDecimal netGST = salesGST.subtract(purchaseGST);
        lblNetGST.setText(CurrencyFormatter.format(netGST));
        setLabelStyle(lblNetGST, netGST);

        // Profit Margin (based on gross revenue)
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (grossRevenue.compareTo(BigDecimal.ZERO) > 0) {
            profitMargin = grossProfit.multiply(BigDecimal.valueOf(100))
                .divide(grossRevenue, 2, RoundingMode.HALF_UP);
        }
        lblProfitMargin.setText(String.format("%.2f%%", profitMargin));
        setLabelStyle(lblProfitMargin, grossProfit);
    }

    private void calculateCashFlowStatistics() {
        // Cash Inflow (from sales)
        BigDecimal cashInflow = bills.stream()
            .map(Bill::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblCashInflow.setText(CurrencyFormatter.format(cashInflow));

        // Cash Outflow (from purchases)
        BigDecimal cashOutflow = purchaseInvoices.stream()
            .map(PurchaseInvoice::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblCashOutflow.setText(CurrencyFormatter.format(cashOutflow));

        // Net Cash Flow
        BigDecimal netCashFlow = cashInflow.subtract(cashOutflow);
        lblNetCashFlow.setText(CurrencyFormatter.format(netCashFlow));
        setLabelStyle(lblNetCashFlow, netCashFlow);

        // Total Bills Count
        lblTotalBills.setText(String.valueOf(bills.size()));

        // Total Purchase Invoices Count
        lblTotalPurchaseInvoices.setText(String.valueOf(purchaseInvoices.size()));
    }

    private void generateMetalInventoryBreakdown() {
        Map<String, MetalInventory> inventoryMap = new HashMap<>();

        // Process purchase metal transactions
        for (PurchaseInvoice invoice : purchaseInvoices) {
            if (invoice.getPurchaseMetalTransactions() != null) {
                for (PurchaseMetalTransaction transaction : invoice.getPurchaseMetalTransactions()) {
                    String metalType = transaction.getMetalType();
                    MetalInventory inventory = inventoryMap.getOrDefault(metalType,
                        new MetalInventory(metalType, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                    inventory.setMetalPurchased(inventory.getMetalPurchased()
                        .add(transaction.getNetWeightCharged()));

                    inventoryMap.put(metalType, inventory);
                }
            }

            // Process purchase exchange transactions (metal given to suppliers)
            if (invoice.getPurchaseExchangeTransactions() != null) {
                for (PurchaseExchangeTransaction exchangeTransaction : invoice.getPurchaseExchangeTransactions()) {
                    String metalType = exchangeTransaction.getMetalType();
                    MetalInventory inventory = inventoryMap.getOrDefault(metalType,
                        new MetalInventory(metalType, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                    inventory.setMetalToSuppliers(inventory.getMetalToSuppliers()
                        .add(exchangeTransaction.getNetWeight()));

                    inventoryMap.put(metalType, inventory);
                }
            }
        }

        // Process bill transactions (sales)
        for (Bill bill : bills) {
            if (bill.getBillTransactions() != null) {
                for (BillTransaction transaction : bill.getBillTransactions()) {
                    String metalType = transaction.getMetalType();
                    MetalInventory inventory = inventoryMap.getOrDefault(metalType,
                        new MetalInventory(metalType, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                    inventory.setMetalSold(inventory.getMetalSold().add(transaction.getWeight()));
                    inventory.setItemsWeight(inventory.getItemsWeight().add(transaction.getWeight()));

                    inventoryMap.put(metalType, inventory);
                }
            }

            // Process exchanges from customers
            Optional<Exchange> exchangeOpt = exchangeService.findByBillId(bill.getId());
            if (exchangeOpt.isPresent()) {
                Exchange exchange = exchangeOpt.get();
                if (exchange.getExchangeTransactions() != null) {
                    for (ExchangeTransaction exchangeTransaction : exchange.getExchangeTransactions()) {
                        String metalType = exchangeTransaction.getMetalType();

                        MetalInventory inventory = inventoryMap.getOrDefault(metalType,
                            new MetalInventory(metalType, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                        inventory.setMetalFromCustomers(inventory.getMetalFromCustomers()
                            .add(exchangeTransaction.getNetWeight()));

                        inventoryMap.put(metalType, inventory);
                    }
                }
            }
        }

        // Calculate metal balance for each type
        for (MetalInventory inventory : inventoryMap.values()) {
            BigDecimal balance = inventory.getMetalPurchased()
                .add(inventory.getMetalFromCustomers())
                .subtract(inventory.getMetalSold())
                .subtract(inventory.getMetalToSuppliers());
            inventory.setMetalBalance(balance);
        }

        metalInventories.setAll(inventoryMap.values());
        updateTableHeight(tableMetalInventory, metalInventories.size());
    }

    private void generateMetalMovementBreakdown() {
        Map<String, MetalMovement> movementMap = new HashMap<>();

        // Process purchase metal transactions
        for (PurchaseInvoice invoice : purchaseInvoices) {
            if (invoice.getPurchaseMetalTransactions() != null) {
                for (PurchaseMetalTransaction transaction : invoice.getPurchaseMetalTransactions()) {
                    String metalType = transaction.getMetalType();
                    MetalMovement movement = movementMap.getOrDefault(metalType,
                        new MetalMovement(metalType, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                    movement.setPurchasedWeight(movement.getPurchasedWeight()
                        .add(transaction.getNetWeightCharged()));
                    movement.setPurchaseValue(movement.getPurchaseValue()
                        .add(transaction.getTotalAmount()));

                    movementMap.put(metalType, movement);
                }
            }

            // Process purchase exchange transactions (metal given to suppliers)
            if (invoice.getPurchaseExchangeTransactions() != null) {
                for (PurchaseExchangeTransaction exchangeTransaction : invoice.getPurchaseExchangeTransactions()) {
                    String metalType = exchangeTransaction.getMetalType();
                    MetalMovement movement = movementMap.getOrDefault(metalType,
                        new MetalMovement(metalType, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                    movement.setExchangedToSuppliers(movement.getExchangedToSuppliers()
                        .add(exchangeTransaction.getNetWeight()));

                    movementMap.put(metalType, movement);
                }
            }
        }

        // Process bill transactions (sales)
        for (Bill bill : bills) {
            if (bill.getBillTransactions() != null) {
                for (BillTransaction transaction : bill.getBillTransactions()) {
                    String metalType = transaction.getMetalType();
                    MetalMovement movement = movementMap.getOrDefault(metalType,
                        new MetalMovement(metalType, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                    movement.setSoldWeight(movement.getSoldWeight().add(transaction.getWeight()));
                    movement.setSalesValue(movement.getSalesValue().add(transaction.getTotalAmount()));

                    movementMap.put(metalType, movement);
                }
            }

            // Process exchanges from customers
            Optional<Exchange> exchangeOpt = exchangeService.findByBillId(bill.getId());
            if (exchangeOpt.isPresent()) {
                Exchange exchange = exchangeOpt.get();
                if (exchange.getExchangeTransactions() != null) {
                    for (ExchangeTransaction exchangeTransaction : exchange.getExchangeTransactions()) {
                        String metalType = exchangeTransaction.getMetalType();

                        MetalMovement movement = movementMap.getOrDefault(metalType,
                            new MetalMovement(metalType, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                        movement.setExchangedFromCustomers(movement.getExchangedFromCustomers()
                            .add(exchangeTransaction.getNetWeight()));

                        movementMap.put(metalType, movement);
                    }
                }
            }
        }

        // Calculate net movements (Purchased + Exchanged from Customers - Sold - Exchanged to Suppliers)
        for (MetalMovement movement : movementMap.values()) {
            BigDecimal netMovement = movement.getPurchasedWeight()
                .add(movement.getExchangedFromCustomers())
                .subtract(movement.getSoldWeight())
                .subtract(movement.getExchangedToSuppliers());
            movement.setNetMovement(netMovement);
        }

        metalMovements.setAll(movementMap.values());
        updateTableHeight(tableMetalMovement, metalMovements.size());
    }

    private void generatePaymentMethodSummary() {
        Map<String, PaymentMethodSummary> summaryMap = new HashMap<>();

        // Process bills (sales) - use gross revenue (subtotal + exchange)
        for (Bill bill : bills) {
            String paymentMethod = formatPaymentMethod(bill.getPaymentMethod().name());
            PaymentMethodSummary summary = summaryMap.getOrDefault(paymentMethod,
                new PaymentMethodSummary(paymentMethod, BigDecimal.ZERO, BigDecimal.ZERO));

            BigDecimal grossRevenue = bill.getSubtotal().add(bill.getExchangeAmount());
            summary.setSalesAmount(summary.getSalesAmount().add(grossRevenue));
            summaryMap.put(paymentMethod, summary);
        }

        // Process purchases
        for (PurchaseInvoice invoice : purchaseInvoices) {
            String paymentMethod = formatPaymentMethod(invoice.getPaymentMethod().name());
            PaymentMethodSummary summary = summaryMap.getOrDefault(paymentMethod,
                new PaymentMethodSummary(paymentMethod, BigDecimal.ZERO, BigDecimal.ZERO));

            summary.setPurchaseAmount(summary.getPurchaseAmount().add(invoice.getGrandTotal()));
            summaryMap.put(paymentMethod, summary);
        }

        // Calculate net amounts
        for (PaymentMethodSummary summary : summaryMap.values()) {
            BigDecimal netAmount = summary.getSalesAmount().subtract(summary.getPurchaseAmount());
            summary.setNetAmount(netAmount);
        }

        paymentSummaries.setAll(summaryMap.values());
        updateTableHeight(tablePaymentSummary, paymentSummaries.size());
    }

    private String formatPaymentMethod(String method) {
        if (method == null) return "N/A";

        switch (method) {
            case "CASH": return "Cash";
            case "UPI": return "UPI";
            case "CARD": return "Card";
            case "CHEQUE": return "Cheque";
            case "BANK_TRANSFER": return "Bank Transfer";
            case "PARTIAL": return "Partial";
            case "CREDIT": return "Credit";
            default: return method;
        }
    }

    private void setLabelStyle(Label label, BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            label.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: 600;");
        } else if (value.compareTo(BigDecimal.ZERO) < 0) {
            label.setStyle("-fx-text-fill: #F44336; -fx-font-weight: 600;");
        } else {
            label.setStyle("-fx-text-fill: #757575; -fx-font-weight: 600;");
        }
    }

    private void updateQuickFilterButtonStyles(Button selectedButton) {
        // Inactive style - light purple background
        String inactiveStyle = "-fx-background-color: #F3E5F5; -fx-text-fill: #9C27B0; " +
                              "-fx-font-family: 'Segoe UI'; -fx-font-weight: 600; " +
                              "-fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;";

        // Active style - dark purple background
        String activeStyle = "-fx-background-color: #9C27B0; -fx-text-fill: white; " +
                            "-fx-font-family: 'Segoe UI'; -fx-font-weight: 600; " +
                            "-fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;";

        // Reset all buttons to inactive style
        btnToday.setStyle(inactiveStyle);
        btnThisWeek.setStyle(inactiveStyle);
        btnThisMonth.setStyle(inactiveStyle);
        btnThisYear.setStyle(inactiveStyle);

        // Set selected button to active style
        if (selectedButton != null) {
            selectedButton.setStyle(activeStyle);
        }
    }

    // Inner classes for breakdown data
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetalInventory {
        private String metalType;
        private BigDecimal metalPurchased;      // Weight purchased from suppliers
        private BigDecimal metalFromCustomers;  // Weight received from customers (exchange)
        private BigDecimal metalSold;           // Weight sold to customers
        private BigDecimal metalToSuppliers;    // Weight given to suppliers (exchange)
        @Setter(AccessLevel.NONE)
        private BigDecimal metalBalance = BigDecimal.ZERO; // Net inventory balance
        private BigDecimal itemsWeight;         // Total weight of items sold

        public MetalInventory(String metalType, BigDecimal metalPurchased, BigDecimal metalFromCustomers,
                            BigDecimal metalSold, BigDecimal metalToSuppliers, BigDecimal itemsWeight) {
            this.metalType = metalType;
            this.metalPurchased = metalPurchased;
            this.metalFromCustomers = metalFromCustomers;
            this.metalSold = metalSold;
            this.metalToSuppliers = metalToSuppliers;
            this.itemsWeight = itemsWeight;
            // Calculate balance: (Purchased + From Customers) - (Sold + To Suppliers)
            this.metalBalance = metalPurchased.add(metalFromCustomers)
                .subtract(metalSold).subtract(metalToSuppliers);
        }

        public void setMetalBalance(BigDecimal metalBalance) {
            this.metalBalance = metalBalance;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetalMovement {
        private String metalType;
        private BigDecimal purchasedWeight;
        private BigDecimal exchangedFromCustomers; // Metal received from customers
        private BigDecimal soldWeight;
        private BigDecimal exchangedToSuppliers; // Metal given to suppliers
        private BigDecimal purchaseValue;
        private BigDecimal salesValue;
        @Setter(AccessLevel.NONE)
        private BigDecimal netMovement = BigDecimal.ZERO;

        public MetalMovement(String metalType, BigDecimal purchasedWeight, BigDecimal exchangedFromCustomers,
                           BigDecimal soldWeight, BigDecimal exchangedToSuppliers,
                           BigDecimal purchaseValue, BigDecimal salesValue) {
            this.metalType = metalType;
            this.purchasedWeight = purchasedWeight;
            this.exchangedFromCustomers = exchangedFromCustomers;
            this.soldWeight = soldWeight;
            this.exchangedToSuppliers = exchangedToSuppliers;
            this.purchaseValue = purchaseValue;
            this.salesValue = salesValue;
            // Net movement = what we gained - what we lost
            this.netMovement = purchasedWeight.add(exchangedFromCustomers)
                .subtract(soldWeight).subtract(exchangedToSuppliers);
        }

        public void setNetMovement(BigDecimal netMovement) {
            this.netMovement = netMovement;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentMethodSummary {
        private String paymentMethod;
        private BigDecimal salesAmount;
        private BigDecimal purchaseAmount;
        @Setter(AccessLevel.NONE)
        private BigDecimal netAmount = BigDecimal.ZERO;

        public PaymentMethodSummary(String paymentMethod, BigDecimal salesAmount, BigDecimal purchaseAmount) {
            this.paymentMethod = paymentMethod;
            this.salesAmount = salesAmount;
            this.purchaseAmount = purchaseAmount;
            this.netAmount = salesAmount.subtract(purchaseAmount);
        }

        public void setNetAmount(BigDecimal netAmount) {
            this.netAmount = netAmount;
        }
    }
}
