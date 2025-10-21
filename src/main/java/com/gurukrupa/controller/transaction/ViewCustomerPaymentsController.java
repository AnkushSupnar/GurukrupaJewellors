package com.gurukrupa.controller.transaction;

import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.entities.CustomerPayment;
import com.gurukrupa.data.service.CustomerPaymentService;
import com.gurukrupa.data.service.CustomerService;
import com.gurukrupa.utility.CurrencyFormatter;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.FxmlView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Controller for viewing customer payment history with search filters
 */
@Component
public class ViewCustomerPaymentsController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ViewCustomerPaymentsController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    // ==================== FXML Components ====================

    // Header
    @FXML private Button btnBack;
    @FXML private Button btnRefresh;

    // Search Filters
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private HBox customerSearchContainer;
    @FXML private Button btnSearch;
    @FXML private Button btnClearFilters;

    // Summary Labels
    @FXML private Label lblTotalPayments;
    @FXML private Label lblPaymentCount;
    @FXML private Label lblCustomerCount;
    @FXML private Label lblRecordCount;

    // Table
    @FXML private TableView<CustomerPayment> paymentsTable;
    @FXML private TableColumn<CustomerPayment, String> colReceiptNumber;
    @FXML private TableColumn<CustomerPayment, String> colDate;
    @FXML private TableColumn<CustomerPayment, String> colCustomer;
    @FXML private TableColumn<CustomerPayment, String> colAmount;
    @FXML private TableColumn<CustomerPayment, String> colPaymentMode;
    @FXML private TableColumn<CustomerPayment, String> colBankAccount;
    @FXML private TableColumn<CustomerPayment, String> colTransactionRef;
    @FXML private TableColumn<CustomerPayment, String> colPreviousPending;
    @FXML private TableColumn<CustomerPayment, String> colRemainingPending;

    // Action Buttons
    @FXML private Button btnView;
    @FXML private Button btnPrint;
    @FXML private Button btnExport;

    // ==================== Spring Services ====================

    @Autowired private CustomerPaymentService customerPaymentService;
    @Autowired private CustomerService customerService;
    @Autowired private AlertNotification alertNotification;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.gurukrupa.view.StageManager stageManager;

    // ==================== Data Collections ====================

    private AutoCompleteTextField<Customer> customerSearch;
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<CustomerPayment> payments = FXCollections.observableArrayList();

    // ==================== Initialization ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("Initializing View Customer Payments Controller");

        setupCustomerSearch();
        setupTable();
        loadInitialData();

        LOG.info("View Customer Payments Controller initialized successfully");
    }

    /**
     * Setup customer search with AutoCompleteTextField
     */
    private void setupCustomerSearch() {
        customerSearch = new AutoCompleteTextField<>(
            customers,
            new StringConverter<>() {
                @Override
                public String toString(Customer customer) {
                    return customer == null ? "" : customer.getFullname();
                }

                @Override
                public Customer fromString(String string) {
                    return null;
                }
            },
            searchText -> customers.stream()
                .filter(customer -> customer.getFullname().toLowerCase().contains(searchText.toLowerCase()) ||
                                  (customer.getMobile() != null && customer.getMobile().contains(searchText)))
                .toList()
        );

        customerSearch.setPromptText("Search customer (optional)...");

        // Set custom cell factory
        customerSearch.setCellFactory(customer -> {
            Label label = new Label();
            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(2);
            vbox.setStyle("-fx-padding: 4;");

            Label nameLabel = new Label(customer.getFullname());
            nameLabel.setStyle("-fx-font-family: 'Segoe UI Semibold'; -fx-font-size: 13px;");

            String details = "";
            if (customer.getMobile() != null && !customer.getMobile().isEmpty()) {
                details = customer.getMobile();
            }
            if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                details += (details.isEmpty() ? "" : " • ") + customer.getAddress();
            }

            if (!details.isEmpty()) {
                Label detailsLabel = new Label(details);
                detailsLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #757575;");
                vbox.getChildren().addAll(nameLabel, detailsLabel);
            } else {
                vbox.getChildren().add(nameLabel);
            }

            label.setGraphic(vbox);
            label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return label;
        });

        customerSearchContainer.getChildren().add(customerSearch.getNode());
        HBox.setHgrow(customerSearch.getNode(), javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        colReceiptNumber.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReceiptNumber()));

        colDate.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPaymentDate().format(DATE_FORMATTER)));

        colCustomer.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCustomer().getFullname()));

        colAmount.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                CurrencyFormatter.format(cellData.getValue().getPaymentAmount())));

        colPaymentMode.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPaymentMode().toString()));

        colBankAccount.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getBankAccount().getBankName() + " - " +
                cellData.getValue().getBankAccount().getAccountNumber()));

        colTransactionRef.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTransactionReference() != null ?
                cellData.getValue().getTransactionReference() : "-"));

        colPreviousPending.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                CurrencyFormatter.format(cellData.getValue().getPreviousPendingAmount())));

        colRemainingPending.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                CurrencyFormatter.format(cellData.getValue().getRemainingPendingAmount())));

        paymentsTable.setItems(payments);

        // Make table height dynamic based on content
        paymentsTable.setFixedCellSize(35);
        paymentsTable.prefHeightProperty().bind(
            paymentsTable.fixedCellSizeProperty()
                .multiply(javafx.beans.binding.Bindings.size(paymentsTable.getItems()).add(1.1)));
    }

    /**
     * Load initial data
     */
    private void loadInitialData() {
        Platform.runLater(() -> {
            try {
                // Load customers
                List<Customer> customerList = customerService.getAllCustomers();
                customers.setAll(customerList);
                if (customerSearch != null) {
                    customerSearch.setSuggestions(customers);
                }

                // Set default date range (last 30 days)
                dateTo.setValue(LocalDate.now());
                dateFrom.setValue(LocalDate.now().minusDays(30));

                // Load payments for default date range
                loadPayments();

            } catch (Exception e) {
                LOG.error("Error loading initial data", e);
                alertNotification.showError("Failed to load initial data: " + e.getMessage());
            }
        });
    }

    // ==================== Event Handlers ====================

    /**
     * Handle back button
     */
    @FXML
    private void handleBack() {
        try {
            javafx.scene.layout.BorderPane dashboard = (javafx.scene.layout.BorderPane) btnBack.getScene().getRoot();
            javafx.scene.Parent transactionMenu = stageManager.getSpringFXMLLoader()
                    .load(FxmlView.TRANSACTION_MENU.getFxmlFile());
            dashboard.setCenter(transactionMenu);
            LOG.info("Returned to Transaction Menu");
        } catch (Exception e) {
            LOG.error("Error returning to transaction menu", e);
        }
    }

    /**
     * Handle refresh button
     */
    @FXML
    private void handleRefresh() {
        loadPayments();
    }

    /**
     * Handle search button
     */
    @FXML
    private void handleSearch() {
        if (dateFrom.getValue() == null || dateTo.getValue() == null) {
            alertNotification.showError("Please select both From Date and To Date");
            return;
        }

        if (dateFrom.getValue().isAfter(dateTo.getValue())) {
            alertNotification.showError("From Date cannot be after To Date");
            return;
        }

        loadPayments();
    }

    /**
     * Handle clear filters button
     */
    @FXML
    private void handleClearFilters() {
        dateTo.setValue(LocalDate.now());
        dateFrom.setValue(LocalDate.now().minusDays(30));
        customerSearch.clear();
        loadPayments();
    }

    /**
     * Handle view payment
     */
    @FXML
    private void handleViewPayment() {
        CustomerPayment selected = paymentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showError("Please select a payment to view");
            return;
        }

        StringBuilder details = new StringBuilder();
        details.append("═══════════════════════════════════════\n");
        details.append("        PAYMENT RECEIPT DETAILS\n");
        details.append("═══════════════════════════════════════\n\n");
        details.append("Receipt Number: ").append(selected.getReceiptNumber()).append("\n\n");
        details.append("---  Payment Information  ---\n");
        details.append("Customer: ").append(selected.getCustomer().getFullname()).append("\n");
        details.append("Date: ").append(selected.getPaymentDate().format(DATETIME_FORMATTER)).append("\n");
        details.append("Amount Received: ").append(CurrencyFormatter.format(selected.getPaymentAmount())).append("\n\n");
        details.append("---  Transaction Details  ---\n");
        details.append("Payment Mode: ").append(selected.getPaymentMode()).append("\n");
        details.append("Bank Account: ").append(selected.getBankAccount().getBankName())
               .append(" - ").append(selected.getBankAccount().getAccountNumber()).append("\n");

        if (selected.getTransactionReference() != null && !selected.getTransactionReference().isEmpty()) {
            details.append("Transaction Ref: ").append(selected.getTransactionReference()).append("\n");
        }

        details.append("\n---  Amount Summary  ---\n");
        details.append("Previous Pending: ").append(CurrencyFormatter.format(selected.getPreviousPendingAmount())).append("\n");
        details.append("Amount Received: ").append(CurrencyFormatter.format(selected.getPaymentAmount())).append("\n");
        details.append("Remaining Pending: ").append(CurrencyFormatter.format(selected.getRemainingPendingAmount())).append("\n");

        if (selected.getNotes() != null && !selected.getNotes().isEmpty()) {
            details.append("\n---  Notes  ---\n").append(selected.getNotes()).append("\n");
        }

        details.append("\n═══════════════════════════════════════\n");
        details.append("Created: ").append(selected.getCreatedDate().format(DATETIME_FORMATTER)).append("\n");

        alertNotification.showInfo(details.toString());
    }

    /**
     * Handle print payment
     */
    @FXML
    private void handlePrintPayment() {
        CustomerPayment selected = paymentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showError("Please select a payment to print");
            return;
        }

        try {
            StringBuilder receipt = new StringBuilder();
            receipt.append("\n\n");
            receipt.append("═════════════════════════════════════════════════════════════\n");
            receipt.append("                      GURUKRUPA JEWELRY\n");
            receipt.append("                   CUSTOMER PAYMENT RECEIPT\n");
            receipt.append("═════════════════════════════════════════════════════════════\n\n");
            receipt.append("Receipt Number: ").append(selected.getReceiptNumber()).append("\n");
            receipt.append("Date: ").append(selected.getPaymentDate().format(DATETIME_FORMATTER)).append("\n\n");
            receipt.append("─────────────────────────────────────────────────────────────\n");
            receipt.append("CUSTOMER DETAILS\n");
            receipt.append("─────────────────────────────────────────────────────────────\n");
            receipt.append("Name: ").append(selected.getCustomer().getFullname()).append("\n");
            if (selected.getCustomer().getMobile() != null) {
                receipt.append("Mobile: ").append(selected.getCustomer().getMobile()).append("\n");
            }
            if (selected.getCustomer().getAddress() != null) {
                receipt.append("Address: ").append(selected.getCustomer().getAddress()).append("\n");
            }
            receipt.append("\n");
            receipt.append("─────────────────────────────────────────────────────────────\n");
            receipt.append("PAYMENT DETAILS\n");
            receipt.append("─────────────────────────────────────────────────────────────\n");
            receipt.append("Payment Amount: ").append(CurrencyFormatter.format(selected.getPaymentAmount())).append("\n");
            receipt.append("Payment Mode: ").append(selected.getPaymentMode()).append("\n");
            receipt.append("Bank Account: ").append(selected.getBankAccount().getBankName())
                   .append(" - ").append(selected.getBankAccount().getAccountNumber()).append("\n");
            if (selected.getTransactionReference() != null && !selected.getTransactionReference().isEmpty()) {
                receipt.append("Transaction Ref: ").append(selected.getTransactionReference()).append("\n");
            }
            receipt.append("\n");
            receipt.append("─────────────────────────────────────────────────────────────\n");
            receipt.append("AMOUNT SUMMARY\n");
            receipt.append("─────────────────────────────────────────────────────────────\n");
            receipt.append("Previous Pending Amount: ").append(CurrencyFormatter.format(selected.getPreviousPendingAmount())).append("\n");
            receipt.append("Payment Received: ").append(CurrencyFormatter.format(selected.getPaymentAmount())).append("\n");
            receipt.append("Remaining Pending: ").append(CurrencyFormatter.format(selected.getRemainingPendingAmount())).append("\n");

            if (selected.getNotes() != null && !selected.getNotes().isEmpty()) {
                receipt.append("\n");
                receipt.append("─────────────────────────────────────────────────────────────\n");
                receipt.append("NOTES\n");
                receipt.append("─────────────────────────────────────────────────────────────\n");
                receipt.append(selected.getNotes()).append("\n");
            }

            receipt.append("\n");
            receipt.append("═════════════════════════════════════════════════════════════\n");
            receipt.append("             Thank you for your payment!\n");
            receipt.append("═════════════════════════════════════════════════════════════\n");

            TextArea printArea = new TextArea(receipt.toString());
            printArea.setEditable(false);
            printArea.setPrefSize(600, 500);
            printArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Print Payment Receipt");
            dialog.setHeaderText("Receipt #: " + selected.getReceiptNumber());
            dialog.getDialogPane().setContent(printArea);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResizable(true);

            dialog.showAndWait();

            LOG.info("Receipt displayed for printing: {}", selected.getReceiptNumber());

        } catch (Exception e) {
            LOG.error("Error preparing receipt for printing", e);
            alertNotification.showError("Failed to prepare receipt: " + e.getMessage());
        }
    }

    /**
     * Handle export
     */
    @FXML
    private void handleExport() {
        if (payments.isEmpty()) {
            alertNotification.showError("No payment records to export");
            return;
        }

        alertNotification.showInfo("Export functionality will be implemented soon");
    }

    // ==================== Helper Methods ====================

    /**
     * Load payments based on filters
     */
    private void loadPayments() {
        try {
            LocalDate fromDate = dateFrom.getValue();
            LocalDate toDate = dateTo.getValue();

            if (fromDate == null || toDate == null) {
                alertNotification.showError("Please select date range");
                return;
            }

            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            LocalDateTime toDateTime = toDate.atTime(23, 59, 59);

            List<CustomerPayment> paymentList;

            // Filter by customer if selected
            Customer selectedCustomer = customerSearch.getSelectedItem();
            if (selectedCustomer != null) {
                paymentList = customerPaymentService.getPaymentsByCustomerAndDateRange(
                    selectedCustomer.getId(), fromDateTime, toDateTime);
            } else {
                paymentList = customerPaymentService.getPaymentsByDateRange(fromDateTime, toDateTime);
            }

            // Update table
            payments.setAll(paymentList);

            // Update summary
            updateSummary(paymentList);

            LOG.info("Loaded {} payment records", paymentList.size());

        } catch (Exception e) {
            LOG.error("Error loading payments", e);
            alertNotification.showError("Failed to load payments: " + e.getMessage());
        }
    }

    /**
     * Update summary statistics
     */
    private void updateSummary(List<CustomerPayment> paymentList) {
        java.math.BigDecimal total = paymentList.stream()
            .map(CustomerPayment::getPaymentAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        lblTotalPayments.setText(CurrencyFormatter.format(total));
        lblPaymentCount.setText(String.valueOf(paymentList.size()));

        // Count unique customers
        Set<Long> uniqueCustomers = new HashSet<>();
        paymentList.forEach(p -> uniqueCustomers.add(p.getCustomer().getId()));
        lblCustomerCount.setText(String.valueOf(uniqueCustomers.size()));

        lblRecordCount.setText(paymentList.size() + " records");
    }
}
