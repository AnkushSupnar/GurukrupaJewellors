package com.gurukrupa.controller.purchase;

import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.service.*;
import com.gurukrupa.utility.CurrencyFormatter;
import com.gurukrupa.view.AlertNotification;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
 * Controller for Supplier Payment Management
 */
@Component
public class SupplierPaymentController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SupplierPaymentController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    // ==================== FXML Components ====================

    // Header
    @FXML private Button btnBack;
    @FXML private Label lblReceiptNumber;
    @FXML private TextField txtPaymentDate;
    @FXML private HBox supplierSearchContainer;

    // Pending Amount Display
    @FXML private Label lblTotalPending;
    @FXML private Label lblPendingInvoicesCount;
    @FXML private Button btnViewPendingInvoices;

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

    // Payment History Panel
    @FXML private DatePicker historyDateFrom;
    @FXML private DatePicker historyDateTo;
    @FXML private Button btnSearchHistory;
    @FXML private Button btnClearHistory;
    @FXML private Label lblPaymentCount;
    @FXML private TableView<SupplierPayment> paymentHistoryTable;
    @FXML private TableColumn<SupplierPayment, String> colReceiptNumber;
    @FXML private TableColumn<SupplierPayment, String> colPaymentDate;
    @FXML private TableColumn<SupplierPayment, String> colSupplierName;
    @FXML private TableColumn<SupplierPayment, String> colPaymentAmount;
    @FXML private TableColumn<SupplierPayment, String> colPaymentMode;
    @FXML private TableColumn<SupplierPayment, String> colBankAccount;
    @FXML private TableColumn<SupplierPayment, String> colTransactionRef;
    @FXML private Button btnViewReceipt;
    @FXML private Button btnPrintReceipt;
    @FXML private Button btnLoadSupplier;

    // ==================== Spring Services ====================

    @Autowired private SupplierPaymentService supplierPaymentService;
    @Autowired private SupplierService supplierService;
    @Autowired private BankAccountService bankAccountService;
    @Autowired private PurchaseInvoiceService purchaseInvoiceService;
    @Autowired private AlertNotification alertNotification;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.gurukrupa.view.StageManager stageManager;

    // ==================== Data Collections ====================

    private AutoCompleteTextField<Supplier> supplierSearch;
    private final ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
    private final ObservableList<BankAccount> bankAccounts = FXCollections.observableArrayList();
    private final ObservableList<SupplierPayment> paymentHistory = FXCollections.observableArrayList();

    // ==================== Initialization ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("Initializing Supplier Payment Controller");

        setupSupplierSearch();
        setupComboBoxes();
        setupPaymentHistoryList();
        setupCalculations();
        loadInitialData();

        LOG.info("Supplier Payment Controller initialized successfully");
    }

    /**
     * Setup supplier search with AutoCompleteTextField
     */
    private void setupSupplierSearch() {
        supplierSearch = new AutoCompleteTextField<>(
            suppliers,
            new StringConverter<>() {
                @Override
                public String toString(Supplier supplier) {
                    return supplier == null ? "" : supplier.getSupplierFullName();
                }

                @Override
                public Supplier fromString(String string) {
                    return null;
                }
            },
            searchText -> suppliers.stream()
                .filter(supplier -> supplier.getSupplierFullName().toLowerCase().contains(searchText.toLowerCase()) ||
                                  (supplier.getSupplierName() != null && supplier.getSupplierName().toLowerCase().contains(searchText.toLowerCase())) ||
                                  (supplier.getMobile() != null && supplier.getMobile().contains(searchText)))
                .toList()
        );

        supplierSearch.setPromptText("Search supplier by name or mobile...");

        // Set custom cell factory
        supplierSearch.setCellFactory(supplier -> {
            Label label = new Label();
            VBox vbox = new VBox(2);
            vbox.setStyle("-fx-padding: 4;");

            Label nameLabel = new Label(supplier.getSupplierFullName());
            nameLabel.setStyle("-fx-font-family: 'Segoe UI Semibold'; -fx-font-size: 13px;");

            String details = "";
            if (supplier.getMobile() != null && !supplier.getMobile().isEmpty()) {
                details = supplier.getMobile();
            }
            if (supplier.getCity() != null && !supplier.getCity().isEmpty()) {
                details += (details.isEmpty() ? "" : " • ") + supplier.getCity();
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
        supplierSearch.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                clearPendingDetails();
            }
        });

        supplierSearchContainer.getChildren().add(supplierSearch.getNode());
        HBox.setHgrow(supplierSearch.getNode(), javafx.scene.layout.Priority.ALWAYS);
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
            // Wait for layout to complete (double runLater)
            Platform.runLater(() -> {
                // Lock Payment Mode width
                double paymentModeWidth = cmbPaymentMode.getWidth();
                if (paymentModeWidth > 0) {
                    cmbPaymentMode.setMinWidth(paymentModeWidth);
                    cmbPaymentMode.setMaxWidth(paymentModeWidth);
                    cmbPaymentMode.setPrefWidth(paymentModeWidth);

                    // Add width change listener to prevent any expansion
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

                    // Add width change listener to prevent any expansion
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
        // Setup table columns
        colReceiptNumber.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReceiptNumber()));

        colPaymentDate.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));

        colSupplierName.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSupplier().getSupplierFullName()));

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

        // Set items to table
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
                // Load suppliers
                List<Supplier> supplierList = supplierService.getAllActiveSuppliers();
                suppliers.setAll(supplierList);
                if (supplierSearch != null) {
                    supplierSearch.setSuggestions(suppliers);
                }

                // Load bank accounts
                List<BankAccount> accountList = bankAccountService.getAllActiveBankAccounts();
                bankAccounts.setAll(accountList);

                // Set payment date
                txtPaymentDate.setText(LocalDateTime.now().format(DATE_FORMATTER));

                // Generate receipt number
                lblReceiptNumber.setText("Receipt #: " + supplierPaymentService.generateReceiptNumber());

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
     * Handle load supplier button - loads selected supplier data
     */
    @FXML
    private void handleLoadSupplier() {
        Supplier selectedSupplier = supplierSearch.getSelectedItem();

        if (selectedSupplier == null) {
            alertNotification.showError("Please select a supplier first");
            return;
        }

        try {
            // Load supplier pending details
            loadSupplierPendingDetails(selectedSupplier);

            alertNotification.showSuccess(
                "Supplier data loaded successfully!\n\n" +
                "Supplier: " + selectedSupplier.getSupplierFullName()
            );

            LOG.info("Loaded data for supplier: {}", selectedSupplier.getSupplierFullName());

        } catch (Exception e) {
            LOG.error("Error loading supplier data", e);
            alertNotification.showError("Failed to load supplier data: " + e.getMessage());
        }
    }

    /**
     * Handle back button - return to purchase menu
     */
    @FXML
    private void handleBack() {
        try {
            // Get the dashboard's center panel through the parent hierarchy
            javafx.scene.layout.BorderPane dashboard = (javafx.scene.layout.BorderPane) btnBack.getScene().getRoot();

            // Load the Purchase Menu FXML
            javafx.scene.Parent purchaseMenu = stageManager.getSpringFXMLLoader()
                    .load("/fxml/purchase/PurchaseMenu.fxml");

            // Set the purchase menu back in the center of the dashboard
            dashboard.setCenter(purchaseMenu);

            LOG.info("Returned to Purchase Menu");

        } catch (Exception e) {
            LOG.error("Error returning to purchase menu", e);
        }
    }

    /**
     * Handle save payment
     */
    @FXML
    private void handleSavePayment() {
        try {
            // Validate inputs
            if (supplierSearch.getSelectedItem() == null) {
                alertNotification.showError("Please select a supplier");
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

            // Check bank balance
            BankAccount selectedBank = cmbBankAccount.getValue();
            if (selectedBank.getCurrentBalance().compareTo(paymentAmount) < 0) {
                alertNotification.showError(String.format(
                    "Insufficient bank balance!\n\n" +
                    "Bank Account: %s\n" +
                    "Current Balance: %s\n" +
                    "Payment Amount: %s\n" +
                    "Shortfall: %s",
                    selectedBank.getBankName(),
                    CurrencyFormatter.format(selectedBank.getCurrentBalance()),
                    CurrencyFormatter.format(paymentAmount),
                    CurrencyFormatter.format(paymentAmount.subtract(selectedBank.getCurrentBalance()))
                ));
                return;
            }

            // Record payment
            SupplierPayment payment = supplierPaymentService.recordPayment(
                supplierSearch.getSelectedItem(),
                selectedBank,
                paymentAmount,
                SupplierPayment.PaymentMode.valueOf(cmbPaymentMode.getValue()),
                txtTransactionRef.getText(),
                txtNotes.getText()
            );

            alertNotification.showSuccess(
                "Payment recorded successfully!\n\n" +
                "Receipt #: " + payment.getReceiptNumber() + "\n" +
                "Amount: " + CurrencyFormatter.format(payment.getPaymentAmount()) + "\n" +
                "Supplier: " + payment.getSupplier().getSupplierFullName()
            );

            LOG.info("Payment saved: {}", payment.getReceiptNumber());

            // Refresh data
            loadRecentPayments();

            // Reload supplier data if same supplier is still selected
            if (supplierSearch.getSelectedItem() != null) {
                loadSupplierPendingDetails(supplierSearch.getSelectedItem());
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
        supplierSearch.clear();
        txtPaymentAmount.clear();
        cmbBankAccount.setValue(null);
        cmbPaymentMode.setValue(null);
        txtTransactionRef.clear();
        txtNotes.clear();
        clearPendingDetails();
        lblReceiptNumber.setText("Receipt #: " + supplierPaymentService.generateReceiptNumber());
    }

    /**
     * Handle view pending invoices
     */
    @FXML
    private void handleViewPendingInvoices() {
        if (supplierSearch.getSelectedItem() == null) {
            alertNotification.showError("Please select a supplier first");
            return;
        }

        try {
            List<PurchaseInvoice> pendingInvoices = supplierPaymentService
                .getSupplierPendingInvoices(supplierSearch.getSelectedItem().getId());

            if (pendingInvoices.isEmpty()) {
                alertNotification.showInfo("No pending invoices for this supplier");
                return;
            }

            // Create dialog to show pending invoices
            StringBuilder details = new StringBuilder();
            details.append("Pending Invoices for: ")
                   .append(supplierSearch.getSelectedItem().getSupplierFullName())
                   .append("\n\n");

            for (PurchaseInvoice invoice : pendingInvoices) {
                details.append("Invoice: ").append(invoice.getInvoiceNumber())
                       .append("\nDate: ").append(invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                       .append("\nGrand Total: ").append(CurrencyFormatter.format(invoice.getGrandTotal()))
                       .append("\nPaid: ").append(CurrencyFormatter.format(invoice.getPaidAmount()))
                       .append("\nPending: ").append(CurrencyFormatter.format(invoice.getPendingAmount()))
                       .append("\n\n");
            }

            alertNotification.showInfo(details.toString());

        } catch (Exception e) {
            LOG.error("Error viewing pending invoices", e);
            alertNotification.showError("Failed to load pending invoices: " + e.getMessage());
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

            List<SupplierPayment> payments = supplierPaymentService.getPaymentsByDateRange(
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
        SupplierPayment selected = paymentHistoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showError("Please select a payment to view");
            return;
        }

        // Display payment details in a dialog
        StringBuilder details = new StringBuilder();
        details.append("═══════════════════════════════════════\n");
        details.append("        PAYMENT RECEIPT DETAILS\n");
        details.append("═══════════════════════════════════════\n\n");
        details.append("Receipt Number: ").append(selected.getReceiptNumber()).append("\n\n");
        details.append("---  Payment Information  ---\n");
        details.append("Supplier: ").append(selected.getSupplier().getSupplierFullName()).append("\n");
        details.append("Date: ").append(selected.getPaymentDate().format(DATE_FORMATTER)).append("\n");
        details.append("Amount Paid: ").append(CurrencyFormatter.format(selected.getPaymentAmount())).append("\n\n");
        details.append("---  Transaction Details  ---\n");
        details.append("Payment Mode: ").append(selected.getPaymentMode()).append("\n");
        details.append("Bank Account: ").append(selected.getBankAccount().getBankName())
               .append(" - ").append(selected.getBankAccount().getAccountNumber()).append("\n");

        if (selected.getTransactionReference() != null && !selected.getTransactionReference().isEmpty()) {
            details.append("Transaction Ref: ").append(selected.getTransactionReference()).append("\n");
        }

        details.append("\n---  Amount Summary  ---\n");
        details.append("Previous Pending: ").append(CurrencyFormatter.format(selected.getPreviousPendingAmount())).append("\n");
        details.append("Amount Paid: ").append(CurrencyFormatter.format(selected.getPaymentAmount())).append("\n");
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
        SupplierPayment selected = paymentHistoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showError("Please select a payment to print");
            return;
        }

        try {
            // Generate receipt content for printing
            StringBuilder receipt = new StringBuilder();
            receipt.append("\n\n");
            receipt.append("═════════════════════════════════════════════════════════════\n");
            receipt.append("                      GURUKRUPA JEWELRY\n");
            receipt.append("                   SUPPLIER PAYMENT RECEIPT\n");
            receipt.append("═════════════════════════════════════════════════════════════\n\n");
            receipt.append("Receipt Number: ").append(selected.getReceiptNumber()).append("\n");
            receipt.append("Date: ").append(selected.getPaymentDate().format(DATE_FORMATTER)).append("\n\n");
            receipt.append("─────────────────────────────────────────────────────────────\n");
            receipt.append("SUPPLIER DETAILS\n");
            receipt.append("─────────────────────────────────────────────────────────────\n");
            receipt.append("Name: ").append(selected.getSupplier().getSupplierFullName()).append("\n");
            if (selected.getSupplier().getMobile() != null) {
                receipt.append("Mobile: ").append(selected.getSupplier().getMobile()).append("\n");
            }
            if (selected.getSupplier().getCity() != null) {
                receipt.append("City: ").append(selected.getSupplier().getCity()).append("\n");
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
            receipt.append("Payment Made: ").append(CurrencyFormatter.format(selected.getPaymentAmount())).append("\n");
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
            receipt.append("             Thank you for your business!\n");
            receipt.append("═════════════════════════════════════════════════════════════\n");

            // Create a text area to show printable receipt
            javafx.scene.control.TextArea printArea = new javafx.scene.control.TextArea(receipt.toString());
            printArea.setEditable(false);
            printArea.setPrefSize(600, 500);
            printArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

            // Create print dialog
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
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
     * Load supplier pending details
     */
    private void loadSupplierPendingDetails(Supplier supplier) {
        try {
            BigDecimal pendingAmount = supplierPaymentService.getSupplierPendingAmount(supplier.getId());
            List<PurchaseInvoice> pendingInvoices = supplierPaymentService.getSupplierPendingInvoices(supplier.getId());

            lblTotalPending.setText(CurrencyFormatter.format(pendingAmount));
            lblPendingInvoicesCount.setText(String.valueOf(pendingInvoices.size()));

            // Calculate remaining amount
            calculateRemainingAmount();

        } catch (Exception e) {
            LOG.error("Error loading supplier pending details", e);
            alertNotification.showError("Failed to load pending details: " + e.getMessage());
        }
    }

    /**
     * Clear pending details
     */
    private void clearPendingDetails() {
        lblTotalPending.setText("₹ 0.00");
        lblPendingInvoicesCount.setText("0");
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
                List<SupplierPayment> payments = supplierPaymentService.getAllPayments();
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
            // Remove currency symbol and commas
            text = text.replace("₹", "").replace(",", "").trim();
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
