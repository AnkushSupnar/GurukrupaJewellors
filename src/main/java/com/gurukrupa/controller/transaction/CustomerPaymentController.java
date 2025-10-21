package com.gurukrupa.controller.transaction;

import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.service.*;
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

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for Customer Payment Receipt Management
 */
@Component
public class CustomerPaymentController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerPaymentController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    // ==================== FXML Components ====================

    // Header
    @FXML private Button btnBack;
    @FXML private Label lblReceiptNumber;
    @FXML private TextField txtPaymentDate;
    @FXML private HBox customerSearchContainer;

    // Pending Amount Display
    @FXML private Label lblTotalPending;
    @FXML private Label lblPendingBillsCount;
    @FXML private Button btnViewPendingBills;

    // Payment Details
    @FXML private TextField txtPaymentAmount;
    @FXML private ComboBox<BankAccount> cmbBankAccount;
    @FXML private ComboBox<String> cmbPaymentMode;
    @FXML private TextField txtTransactionRef;
    @FXML private TextArea txtNotes;
    @FXML private Label lblRemainingAmount;

    // Action Buttons
    @FXML private Button btnSavePayment;
    @FXML private Button btnClearForm;
    @FXML private Button btnLoadCustomer;

    // Payment History Panel
    @FXML private DatePicker historyDateFrom;
    @FXML private DatePicker historyDateTo;
    @FXML private Button btnSearchHistory;
    @FXML private Button btnClearHistory;
    @FXML private Label lblPaymentCount;
    @FXML private TableView<CustomerPayment> paymentHistoryTable;
    @FXML private TableColumn<CustomerPayment, String> colReceiptNumber;
    @FXML private TableColumn<CustomerPayment, String> colPaymentDate;
    @FXML private TableColumn<CustomerPayment, String> colCustomerName;
    @FXML private TableColumn<CustomerPayment, String> colPaymentAmount;
    @FXML private TableColumn<CustomerPayment, String> colPaymentMode;
    @FXML private TableColumn<CustomerPayment, String> colBankAccount;
    @FXML private TableColumn<CustomerPayment, String> colTransactionRef;
    @FXML private Button btnViewReceipt;
    @FXML private Button btnPrintReceipt;

    // ==================== Spring Services ====================

    @Autowired private CustomerPaymentService customerPaymentService;
    @Autowired private CustomerService customerService;
    @Autowired private BankAccountService bankAccountService;
    @Autowired private BillingService billingService;
    @Autowired private AlertNotification alertNotification;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.gurukrupa.view.StageManager stageManager;

    // ==================== Data Collections ====================

    private AutoCompleteTextField<Customer> customerSearch;
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<BankAccount> bankAccounts = FXCollections.observableArrayList();
    private final ObservableList<CustomerPayment> paymentHistory = FXCollections.observableArrayList();

    // ==================== Initialization ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("Initializing Customer Payment Controller");

        setupCustomerSearch();
        setupComboBoxes();
        setupPaymentHistoryList();
        setupCalculations();
        loadInitialData();

        LOG.info("Customer Payment Controller initialized successfully");
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

        customerSearch.setPromptText("Search customer by name or mobile...");

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

        // Remove auto-load on selection - user must click LOAD button
        customerSearch.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                clearPendingDetails();
            }
        });

        customerSearchContainer.getChildren().add(customerSearch.getNode());
        HBox.setHgrow(customerSearch.getNode(), javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Setup combo boxes
     */
    private void setupComboBoxes() {
        // Bank Account ComboBox
        cmbBankAccount.setItems(bankAccounts);
        cmbBankAccount.setConverter(new StringConverter<>() {
            @Override
            public String toString(BankAccount account) {
                if (account == null) return "";
                return account.getBankName() + " - " + account.getAccountNumber() +
                       " (₹" + CurrencyFormatter.format(account.getCurrentBalance()) + ")";
            }

            @Override
            public BankAccount fromString(String string) {
                return null;
            }
        });

        // Set custom cell factory for bank account dropdown
        cmbBankAccount.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(BankAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) {
                    setText(null);
                } else {
                    setText(account.getBankName() + " - " + account.getAccountNumber() +
                           " (₹" + CurrencyFormatter.format(account.getCurrentBalance()) + ")");
                    setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-padding: 6;");
                }
            }
        });

        // Payment Mode ComboBox
        ObservableList<String> paymentModes = FXCollections.observableArrayList(
            "CASH", "CHEQUE", "BANK_TRANSFER", "UPI", "NEFT", "RTGS", "IMPS", "OTHER"
        );
        cmbPaymentMode.setItems(paymentModes);

        // Set custom cell factory for payment mode dropdown
        cmbPaymentMode.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String mode, boolean empty) {
                super.updateItem(mode, empty);
                if (empty || mode == null) {
                    setText(null);
                } else {
                    setText(mode);
                    setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-padding: 6;");
                }
            }
        });

        // Prevent ComboBoxes from expanding - aggressive width locking
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                // Lock Payment Mode width
                double paymentModeWidth = cmbPaymentMode.getWidth();
                if (paymentModeWidth > 0) {
                    cmbPaymentMode.setMinWidth(paymentModeWidth);
                    cmbPaymentMode.setMaxWidth(paymentModeWidth);
                    cmbPaymentMode.setPrefWidth(paymentModeWidth);

                    cmbPaymentMode.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                        if (newWidth.doubleValue() != paymentModeWidth) {
                            cmbPaymentMode.setMinWidth(paymentModeWidth);
                            cmbPaymentMode.setMaxWidth(paymentModeWidth);
                            cmbPaymentMode.setPrefWidth(paymentModeWidth);
                        }
                    });
                }

                // Lock Bank Account width
                double bankAccountWidth = cmbBankAccount.getWidth();
                if (bankAccountWidth > 0) {
                    cmbBankAccount.setMinWidth(bankAccountWidth);
                    cmbBankAccount.setMaxWidth(bankAccountWidth);
                    cmbBankAccount.setPrefWidth(bankAccountWidth);

                    cmbBankAccount.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                        if (newWidth.doubleValue() != bankAccountWidth) {
                            cmbBankAccount.setMinWidth(bankAccountWidth);
                            cmbBankAccount.setMaxWidth(bankAccountWidth);
                            cmbBankAccount.setPrefWidth(bankAccountWidth);
                        }
                    });
                }
            });
        });
    }

    /**
     * Setup payment history table
     */
    private void setupPaymentHistoryList() {
        colReceiptNumber.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReceiptNumber()));

        colPaymentDate.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));

        colCustomerName.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCustomer().getFullname()));

        colPaymentAmount.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                CurrencyFormatter.format(cellData.getValue().getPaymentAmount())));

        colPaymentMode.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPaymentMode().toString()));

        colBankAccount.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getBankAccount().getBankName() + " - " +
                cellData.getValue().getBankAccount().getAccountNumber()));

        colTransactionRef.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTransactionReference() != null ?
                cellData.getValue().getTransactionReference() : "-"));

        paymentHistoryTable.setItems(paymentHistory);

        // Make table height dynamic based on content
        paymentHistoryTable.setFixedCellSize(35);
        paymentHistoryTable.prefHeightProperty().bind(
            paymentHistoryTable.fixedCellSizeProperty()
                .multiply(javafx.beans.binding.Bindings.size(paymentHistoryTable.getItems()).add(1.1)));
    }

    /**
     * Setup automatic calculations
     */
    private void setupCalculations() {
        txtPaymentAmount.textProperty().addListener((obs, old, val) -> calculateRemainingAmount());
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

                // Load bank accounts
                List<BankAccount> accountList = bankAccountService.getAllActiveBankAccounts();
                bankAccounts.setAll(accountList);

                // Set payment date
                txtPaymentDate.setText(LocalDateTime.now().format(DATE_FORMATTER));

                // Generate receipt number
                lblReceiptNumber.setText("Receipt #: " + customerPaymentService.generateReceiptNumber());

                // Load recent payment history
                loadRecentPayments();

            } catch (Exception e) {
                LOG.error("Error loading initial data", e);
                alertNotification.showError("Failed to load initial data: " + e.getMessage());
            }
        });
    }

    // ==================== Event Handlers ====================

    /**
     * Handle load customer button - loads selected customer data
     */
    @FXML
    private void handleLoadCustomer() {
        Customer selectedCustomer = customerSearch.getSelectedItem();

        if (selectedCustomer == null) {
            alertNotification.showError("Please select a customer first");
            return;
        }

        try {
            // Load customer pending details
            loadCustomerPendingDetails(selectedCustomer);

            alertNotification.showSuccess(
                "Customer data loaded successfully!\n\n" +
                "Customer: " + selectedCustomer.getFullname()
            );

            LOG.info("Loaded data for customer: {}", selectedCustomer.getFullname());

        } catch (Exception e) {
            LOG.error("Error loading customer data", e);
            alertNotification.showError("Failed to load customer data: " + e.getMessage());
        }
    }

    /**
     * Handle back button - return to transaction menu
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
     * Handle save payment
     */
    @FXML
    private void handleSavePayment() {
        try {
            // Validate inputs
            if (customerSearch.getSelectedItem() == null) {
                alertNotification.showError("Please select a customer");
                return;
            }

            if (txtPaymentAmount.getText() == null || txtPaymentAmount.getText().trim().isEmpty()) {
                alertNotification.showError("Please enter payment amount");
                return;
            }

            if (cmbBankAccount.getValue() == null) {
                alertNotification.showError("Please select bank account");
                return;
            }

            if (cmbPaymentMode.getValue() == null) {
                alertNotification.showError("Please select payment mode");
                return;
            }

            BigDecimal paymentAmount = new BigDecimal(txtPaymentAmount.getText().trim());

            if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                alertNotification.showError("Payment amount must be greater than zero");
                return;
            }

            // Check if payment amount exceeds pending amount
            BigDecimal pendingAmount = parseBigDecimal(lblTotalPending.getText());
            if (paymentAmount.compareTo(pendingAmount) > 0) {
                boolean confirm = alertNotification.showConfirmation(
                    "Payment Exceeds Pending Amount",
                    "Payment amount (" + CurrencyFormatter.format(paymentAmount) +
                    ") exceeds total pending amount (" + CurrencyFormatter.format(pendingAmount) +
                    ").\n\nDo you want to continue?"
                );
                if (!confirm) {
                    return;
                }
            }

            // Record payment
            CustomerPayment payment = customerPaymentService.recordPayment(
                customerSearch.getSelectedItem(),
                cmbBankAccount.getValue(),
                paymentAmount,
                CustomerPayment.PaymentMode.valueOf(cmbPaymentMode.getValue()),
                txtTransactionRef.getText(),
                txtNotes.getText()
            );

            alertNotification.showSuccess(
                "Payment received successfully!\n\n" +
                "Receipt #: " + payment.getReceiptNumber() + "\n" +
                "Amount: " + CurrencyFormatter.format(payment.getPaymentAmount()) + "\n" +
                "Customer: " + payment.getCustomer().getFullname()
            );

            LOG.info("Payment saved: {}", payment.getReceiptNumber());

            // Refresh data
            loadRecentPayments();

            // Reload customer data if same customer is still selected
            if (customerSearch.getSelectedItem() != null) {
                loadCustomerPendingDetails(customerSearch.getSelectedItem());
            }

            handleClearForm();

        } catch (NumberFormatException e) {
            alertNotification.showError("Invalid payment amount format");
        } catch (Exception e) {
            LOG.error("Error saving payment", e);
            alertNotification.showError("Failed to save payment: " + e.getMessage());
        }
    }

    /**
     * Handle clear form
     */
    @FXML
    private void handleClearForm() {
        customerSearch.clear();
        txtPaymentAmount.clear();
        cmbBankAccount.setValue(null);
        cmbPaymentMode.setValue(null);
        txtTransactionRef.clear();
        txtNotes.clear();
        clearPendingDetails();
        lblReceiptNumber.setText("Receipt #: " + customerPaymentService.generateReceiptNumber());
    }

    /**
     * Handle view pending bills
     */
    @FXML
    private void handleViewPendingBills() {
        if (customerSearch.getSelectedItem() == null) {
            alertNotification.showError("Please select a customer first");
            return;
        }

        try {
            List<Billing> pendingBills = customerPaymentService
                .getCustomerPendingBills(customerSearch.getSelectedItem().getId());

            if (pendingBills.isEmpty()) {
                alertNotification.showInfo("No pending bills for this customer");
                return;
            }

            StringBuilder details = new StringBuilder();
            details.append("Pending Bills for: ")
                   .append(customerSearch.getSelectedItem().getFullname())
                   .append("\n\n");

            for (Billing bill : pendingBills) {
                details.append("Bill: ").append(bill.getBillNo())
                       .append("\nDate: ").append(bill.getBillDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                       .append("\nGrand Total: ").append(CurrencyFormatter.format(bill.getGrandTotal()))
                       .append("\nPaid: ").append(CurrencyFormatter.format(bill.getPaidAmount()))
                       .append("\nPending: ").append(CurrencyFormatter.format(bill.getPendingAmount()))
                       .append("\n\n");
            }

            alertNotification.showInfo(details.toString());

        } catch (Exception e) {
            LOG.error("Error viewing pending bills", e);
            alertNotification.showError("Failed to load pending bills: " + e.getMessage());
        }
    }

    /**
     * Handle search payment history
     */
    @FXML
    private void handleSearchHistory() {
        try {
            LocalDate fromDate = historyDateFrom.getValue();
            LocalDate toDate = historyDateTo.getValue();

            if (fromDate == null || toDate == null) {
                alertNotification.showError("Please select date range");
                return;
            }

            List<CustomerPayment> payments = customerPaymentService.getPaymentsByDateRange(
                fromDate.atStartOfDay(),
                toDate.atTime(23, 59, 59)
            );

            paymentHistory.setAll(payments);
            lblPaymentCount.setText(payments.size() + " payments");

        } catch (Exception e) {
            LOG.error("Error searching payment history", e);
            alertNotification.showError("Failed to search payments: " + e.getMessage());
        }
    }

    /**
     * Handle clear history search
     */
    @FXML
    private void handleClearHistory() {
        historyDateFrom.setValue(null);
        historyDateTo.setValue(null);
        loadRecentPayments();
    }

    /**
     * Handle view receipt
     */
    @FXML
    private void handleViewReceipt() {
        CustomerPayment selected = paymentHistoryTable.getSelectionModel().getSelectedItem();
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
        details.append("Date: ").append(selected.getPaymentDate().format(DATE_FORMATTER)).append("\n");
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
        details.append("Created: ").append(selected.getCreatedDate().format(DATE_FORMATTER)).append("\n");

        alertNotification.showInfo(details.toString());
    }

    /**
     * Handle print receipt
     */
    @FXML
    private void handlePrintReceipt() {
        CustomerPayment selected = paymentHistoryTable.getSelectionModel().getSelectedItem();
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
            receipt.append("Date: ").append(selected.getPaymentDate().format(DATE_FORMATTER)).append("\n\n");
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

    // ==================== Helper Methods ====================

    /**
     * Load customer pending details
     */
    private void loadCustomerPendingDetails(Customer customer) {
        try {
            BigDecimal pendingAmount = customerPaymentService.getCustomerPendingAmount(customer.getId());
            List<Billing> pendingBills = customerPaymentService.getCustomerPendingBills(customer.getId());

            lblTotalPending.setText(CurrencyFormatter.format(pendingAmount));
            lblPendingBillsCount.setText(String.valueOf(pendingBills.size()));

            calculateRemainingAmount();

        } catch (Exception e) {
            LOG.error("Error loading customer pending details", e);
            alertNotification.showError("Failed to load pending details: " + e.getMessage());
        }
    }

    /**
     * Clear pending details
     */
    private void clearPendingDetails() {
        lblTotalPending.setText("₹ 0.00");
        lblPendingBillsCount.setText("0");
        lblRemainingAmount.setText("₹ 0.00");
    }

    /**
     * Calculate remaining amount after payment
     */
    private void calculateRemainingAmount() {
        try {
            BigDecimal pendingAmount = parseBigDecimal(lblTotalPending.getText());
            BigDecimal paymentAmount = parseBigDecimal(txtPaymentAmount.getText());

            if (paymentAmount == null) paymentAmount = BigDecimal.ZERO;

            BigDecimal remaining = pendingAmount.subtract(paymentAmount);
            lblRemainingAmount.setText(CurrencyFormatter.format(remaining));

        } catch (Exception e) {
            // Ignore calculation errors during typing
        }
    }

    /**
     * Load recent payment history
     */
    private void loadRecentPayments() {
        Platform.runLater(() -> {
            try {
                List<CustomerPayment> payments = customerPaymentService.getAllPayments();
                paymentHistory.setAll(payments);
                lblPaymentCount.setText(payments.size() + " payments");

                LOG.info("Loaded {} payment records", payments.size());
            } catch (Exception e) {
                LOG.error("Error loading payment history", e);
                alertNotification.showError("Failed to load payment history: " + e.getMessage());
            }
        });
    }

    /**
     * Parse BigDecimal from formatted currency string
     */
    private BigDecimal parseBigDecimal(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }
            text = text.replace("₹", "").replace(",", "").trim();
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
