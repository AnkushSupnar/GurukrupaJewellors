package com.gurukrupa.controller.purchase;

import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.service.*;
import com.gurukrupa.utility.CurrencyFormatter;
import com.gurukrupa.utility.WeightFormatter;
import com.gurukrupa.view.AlertNotification;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Clean controller for metal-based purchase invoice system
 * Uses Material Design UI and modern Spring Boot practices
 */
@Component
public class PurchaseInvoiceController_New implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseInvoiceController_New.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    // ==================== FXML Components ====================

    // Left Panel - Metal Stock
    @FXML private VBox metalStockContainer;
    @FXML private Button btnRefreshStock;

    // Header
    @FXML private Label lblInvoiceNumber;
    @FXML private TextField txtInvoiceDate;
    @FXML private HBox supplierSearchContainer;
    @FXML private TextField txtSupplierInvoice;

    // AutoCompleteTextField for supplier search
    private AutoCompleteTextField<Supplier> supplierSearch;

    // Mode Toggle
    @FXML private ToggleGroup modeToggleGroup;
    @FXML private ToggleButton chipPurchase;
    @FXML private ToggleButton chipExchange;
    @FXML private VBox purchasePanel;
    @FXML private VBox exchangePanel;

    // Purchase Metal Form
    @FXML private ComboBox<Metal> cmbPurchaseMetalType;
    @FXML private TextField txtPurchasePurity;
    @FXML private TextField txtPurchaseGrossWeight;
    @FXML private TextField txtSellerPercentage;
    @FXML private TextField txtPurchaseNetWeight;
    @FXML private TextField txtPurchaseRate;
    @FXML private TextField txtPurchaseAmount;
    @FXML private Button btnAddPurchase;
    @FXML private Button btnEditPurchase;
    @FXML private Button btnDeletePurchase;
    @FXML private Button btnClearPurchase;

    // Purchase Table
    @FXML private TableView<PurchaseMetalTransaction> purchaseMetalTable;
    @FXML private TableColumn<PurchaseMetalTransaction, Integer> colPurchaseSno;
    @FXML private TableColumn<PurchaseMetalTransaction, String> colPurchaseMetalType;
    @FXML private TableColumn<PurchaseMetalTransaction, BigDecimal> colPurchasePurity;
    @FXML private TableColumn<PurchaseMetalTransaction, BigDecimal> colPurchaseGrossWeight;
    @FXML private TableColumn<PurchaseMetalTransaction, BigDecimal> colPurchaseSellerPct;
    @FXML private TableColumn<PurchaseMetalTransaction, BigDecimal> colPurchaseNetWeight;
    @FXML private TableColumn<PurchaseMetalTransaction, BigDecimal> colPurchaseRate;
    @FXML private TableColumn<PurchaseMetalTransaction, BigDecimal> colPurchaseAmount;
    @FXML private Label lblPurchaseItemCount;

    // Exchange Metal Form
    @FXML private TextField txtExchangeItemName;
    @FXML private ComboBox<Metal> cmbExchangeMetalType;
    @FXML private TextField txtExchangePurity;
    @FXML private TextField txtExchangeGrossWeight;
    @FXML private TextField txtFinePercentage;
    @FXML private TextField txtExchangeNetWeight;
    @FXML private TextField txtExchangeRate;
    @FXML private TextField txtExchangeAmount;
    @FXML private Button btnAddExchange;
    @FXML private Button btnEditExchange;
    @FXML private Button btnDeleteExchange;
    @FXML private Button btnClearExchange;

    // Exchange Table
    @FXML private TableView<PurchaseExchangeTransaction> exchangeMetalTable;
    @FXML private TableColumn<PurchaseExchangeTransaction, Integer> colExchangeSno;
    @FXML private TableColumn<PurchaseExchangeTransaction, String> colExchangeItemName;
    @FXML private TableColumn<PurchaseExchangeTransaction, String> colExchangeMetalType;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExchangePurity;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExchangeGrossWeight;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExchangeFine;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExchangeNetWeight;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExchangeRate;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExchangeAmount;
    @FXML private Label lblExchangeItemCount;

    // Summary Panel
    @FXML private Label lblPurchaseCount;
    @FXML private Label lblPurchaseTotal;
    @FXML private Label lblExchangeCount;
    @FXML private Label lblExchangeTotal;
    @FXML private Label lblSubtotal;
    @FXML private Label lblGST;
    @FXML private Label lblNetTotal;
    @FXML private Label lblExchangeDeduction;
    @FXML private TextField txtDiscount;
    @FXML private Label lblGrandTotal;

    // Payment Details
    @FXML private CheckBox chkCreditPurchase;
    @FXML private CheckBox chkTaxApplicable;
    @FXML private ComboBox<BankAccount> cmbBankAccount;
    @FXML private TextField txtPaidAmount;
    @FXML private Label lblPendingAmount;

    // Action Buttons
    @FXML private Button btnSave;
    @FXML private Button btnPrint;
    @FXML private Button btnClear;

    // ==================== Spring Services ====================

    @Autowired private PurchaseInvoiceService_New purchaseInvoiceService;
    @Autowired private PurchaseMetalStockService purchaseMetalStockService;
    @Autowired private SupplierService supplierService;
    @Autowired private MetalService metalService;
    @Autowired private MetalRateService metalRateService;
    @Autowired private BankAccountService bankAccountService;
    @Autowired private AlertNotification alertNotification;

    // ==================== Data Collections ====================

    private final ObservableList<PurchaseMetalTransaction> purchaseTransactions = FXCollections.observableArrayList();
    private final ObservableList<PurchaseExchangeTransaction> exchangeTransactions = FXCollections.observableArrayList();
    private final ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
    private final ObservableList<Metal> metals = FXCollections.observableArrayList();
    private final ObservableList<BankAccount> bankAccounts = FXCollections.observableArrayList();

    // Editing mode tracking
    private PurchaseMetalTransaction editingPurchaseTransaction = null;
    private PurchaseExchangeTransaction editingExchangeTransaction = null;

    // ==================== Initialization ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("Initializing Purchase Invoice Controller (Metal-Based System)");

        setupModeToggle();
        setupTables();
        setupComboBoxes();
        setupSupplierSearch();
        setupCalculations();
        loadInitialData();

        LOG.info("Purchase Invoice Controller initialized successfully");
    }

    /**
     * Setup mode toggle between purchase and exchange
     */
    private void setupModeToggle() {
        modeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == chipPurchase) {
                purchasePanel.setVisible(true);
                purchasePanel.setManaged(true);
                exchangePanel.setVisible(false);
                exchangePanel.setManaged(false);

                chipPurchase.setStyle("-fx-background-color: #6A1B9A; -fx-text-fill: white; -fx-font-family: 'Segoe UI Bold'; -fx-font-size: 13px; -fx-padding: 12 32; -fx-background-radius: 25 0 0 25; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(106,27,154,0.3), 8, 0, 0, 2);");
                chipExchange.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #616161; -fx-font-family: 'Segoe UI Bold'; -fx-font-size: 13px; -fx-padding: 12 32; -fx-background-radius: 0 25 25 0; -fx-cursor: hand;");
            } else {
                purchasePanel.setVisible(false);
                purchasePanel.setManaged(false);
                exchangePanel.setVisible(true);
                exchangePanel.setManaged(true);

                chipPurchase.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #616161; -fx-font-family: 'Segoe UI Bold'; -fx-font-size: 13px; -fx-padding: 12 32; -fx-background-radius: 25 0 0 25; -fx-cursor: hand;");
                chipExchange.setStyle("-fx-background-color: #FF6F00; -fx-text-fill: white; -fx-font-family: 'Segoe UI Bold'; -fx-font-size: 13px; -fx-padding: 12 32; -fx-background-radius: 0 25 25 0; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(255,111,0,0.3), 8, 0, 0, 2);");
            }
        });
    }

    /**
     * Setup tables for purchase and exchange items
     */
    private void setupTables() {
        setupPurchaseTable();
        setupExchangeTable();
    }

    /**
     * Setup purchase metal table
     */
    private void setupPurchaseTable() {
        // Serial Number
        colPurchaseSno.setCellValueFactory(cellData -> {
            int index = purchaseMetalTable.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleObjectProperty<>(index);
        });

        // Metal Type
        colPurchaseMetalType.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getMetalType()));

        // Purity
        colPurchasePurity.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getPurity()));
        colPurchasePurity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString());
            }
        });

        // Gross Weight
        colPurchaseGrossWeight.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getGrossWeight()));
        colPurchaseGrossWeight.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : WeightFormatter.format(item) + " g");
            }
        });

        // Seller Percentage
        colPurchaseSellerPct.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getSellerPercentage()));
        colPurchaseSellerPct.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString() + "%");
            }
        });

        // Net Weight
        colPurchaseNetWeight.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getNetWeightCharged()));
        colPurchaseNetWeight.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : WeightFormatter.format(item) + " g");
                setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
            }
        });

        // Rate
        colPurchaseRate.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getRatePerGram()));
        colPurchaseRate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CurrencyFormatter.format(item));
            }
        });

        // Amount
        colPurchaseAmount.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getTotalAmount()));
        colPurchaseAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CurrencyFormatter.format(item));
                setStyle("-fx-text-fill: #1976D2; -fx-font-weight: bold;");
            }
        });

        purchaseMetalTable.setItems(purchaseTransactions);

        // Add double-click listener to edit
        purchaseMetalTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && purchaseMetalTable.getSelectionModel().getSelectedItem() != null) {
                handleEditPurchase();
            }
        });
    }

    /**
     * Setup exchange metal table
     */
    private void setupExchangeTable() {
        // Serial Number
        colExchangeSno.setCellValueFactory(cellData -> {
            int index = exchangeMetalTable.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleObjectProperty<>(index);
        });

        // Item Name
        colExchangeItemName.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getItemName()));

        // Metal Type
        colExchangeMetalType.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getMetalType()));

        // Purity
        colExchangePurity.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getPurity()));
        colExchangePurity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString());
            }
        });

        // Gross Weight
        colExchangeGrossWeight.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getGrossWeight()));
        colExchangeGrossWeight.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : WeightFormatter.format(item) + " g");
            }
        });

        // Fine Percentage
        colExchangeFine.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getFinePercentage()));
        colExchangeFine.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString() + "%");
            }
        });

        // Net Weight
        colExchangeNetWeight.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getNetWeight()));
        colExchangeNetWeight.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : WeightFormatter.format(item) + " g");
                setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
            }
        });

        // Rate
        colExchangeRate.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getRatePerGram()));
        colExchangeRate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CurrencyFormatter.format(item));
            }
        });

        // Amount
        colExchangeAmount.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getTotalAmount()));
        colExchangeAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CurrencyFormatter.format(item));
                setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
            }
        });

        exchangeMetalTable.setItems(exchangeTransactions);

        // Add double-click listener to edit
        exchangeMetalTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && exchangeMetalTable.getSelectionModel().getSelectedItem() != null) {
                handleEditExchange();
            }
        });
    }

    /**
     * Setup supplier search with AutoCompleteTextField
     */
    private void setupSupplierSearch() {
        // Create AutoCompleteTextField for suppliers
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
                .collect(java.util.stream.Collectors.toList())
        );

        supplierSearch.setPromptText("Search supplier by name or mobile...");

        // Set custom cell factory for better display
        supplierSearch.setCellFactory(supplier -> {
            javafx.scene.control.Label label = new javafx.scene.control.Label();
            VBox vbox = new VBox(2);
            vbox.setStyle("-fx-padding: 4;");

            // Supplier name
            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(supplier.getSupplierFullName());
            nameLabel.setStyle("-fx-font-family: 'Segoe UI Semibold'; -fx-font-size: 13px;");

            // Supplier mobile and city
            String details = "";
            if (supplier.getMobile() != null && !supplier.getMobile().isEmpty()) {
                details = supplier.getMobile();
            }
            if (supplier.getCity() != null && !supplier.getCity().isEmpty()) {
                details += (details.isEmpty() ? "" : " • ") + supplier.getCity();
            }

            if (!details.isEmpty()) {
                javafx.scene.control.Label detailsLabel = new javafx.scene.control.Label(details);
                detailsLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #757575;");
                vbox.getChildren().addAll(nameLabel, detailsLabel);
            } else {
                vbox.getChildren().add(nameLabel);
            }

            label.setGraphic(vbox);
            label.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            return label;
        });

        // Add to container
        supplierSearchContainer.getChildren().add(supplierSearch.getNode());
        HBox.setHgrow(supplierSearch.getNode(), javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Setup combo boxes
     */
    private void setupComboBoxes() {
        // Purchase Metal Type ComboBox
        cmbPurchaseMetalType.setItems(metals);
        cmbPurchaseMetalType.setConverter(new StringConverter<>() {
            @Override
            public String toString(Metal metal) {
                return metal == null ? "" : metal.getMetalName();
            }

            @Override
            public Metal fromString(String string) {
                return null;
            }
        });

        // Exchange Metal Type ComboBox
        cmbExchangeMetalType.setItems(metals);
        cmbExchangeMetalType.setConverter(new StringConverter<>() {
            @Override
            public String toString(Metal metal) {
                return metal == null ? "" : metal.getMetalName();
            }

            @Override
            public Metal fromString(String string) {
                return null;
            }
        });

        // Bank Account ComboBox
        cmbBankAccount.setItems(bankAccounts);
        cmbBankAccount.setConverter(new StringConverter<>() {
            @Override
            public String toString(BankAccount account) {
                return account == null ? "" : account.getBankName() + " - " + account.getAccountNumber();
            }

            @Override
            public BankAccount fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Setup automatic calculations
     */
    private void setupCalculations() {
        // Purchase calculations
        txtPurchaseGrossWeight.textProperty().addListener((obs, old, val) -> calculatePurchaseNetWeight());
        txtSellerPercentage.textProperty().addListener((obs, old, val) -> calculatePurchaseNetWeight());
        txtPurchaseRate.textProperty().addListener((obs, old, val) -> calculatePurchaseAmount());

        // Exchange calculations
        txtExchangeGrossWeight.textProperty().addListener((obs, old, val) -> calculateExchangeNetWeight());
        txtFinePercentage.textProperty().addListener((obs, old, val) -> calculateExchangeNetWeight());
        txtExchangeRate.textProperty().addListener((obs, old, val) -> calculateExchangeAmount());

        // Discount calculation
        txtDiscount.textProperty().addListener((obs, old, val) -> calculateTotals());

        // Tax checkbox listener
        chkTaxApplicable.selectedProperty().addListener((obs, old, val) -> calculateTotals());

        // Paid amount calculation
        txtPaidAmount.textProperty().addListener((obs, old, val) -> calculatePendingAmount());
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

                // Load metals
                List<Metal> metalList = metalService.getAllActiveMetals();
                metals.setAll(metalList);

                // Load bank accounts
                List<BankAccount> accountList = bankAccountService.getAllActiveBankAccounts();
                bankAccounts.setAll(accountList);

                // Set invoice date
                txtInvoiceDate.setText(LocalDateTime.now().format(DATE_FORMATTER));

                // Generate invoice number
                lblInvoiceNumber.setText("Invoice #: " + purchaseInvoiceService.generateInvoiceNumber());

                // Load metal stock
                refreshMetalStock();

            } catch (Exception e) {
                LOG.error("Error loading initial data", e);
                alertNotification.showError("Failed to load initial data: " + e.getMessage());
            }
        });
    }

    // ==================== Event Handlers ====================

    /**
     * Handle add purchase metal
     */
    @FXML
    private void handleAddPurchase() {
        try {
            // Validate inputs
            if (cmbPurchaseMetalType.getValue() == null) {
                alertNotification.showError("Please select metal type");
                return;
            }

            BigDecimal purity = new BigDecimal(txtPurchasePurity.getText().trim());
            BigDecimal grossWeight = new BigDecimal(txtPurchaseGrossWeight.getText().trim());
            BigDecimal sellerPercentage = new BigDecimal(txtSellerPercentage.getText().trim());
            BigDecimal rate = new BigDecimal(txtPurchaseRate.getText().trim());

            // Check if editing or adding new
            if (editingPurchaseTransaction != null) {
                // Update existing transaction
                editingPurchaseTransaction.setMetal(cmbPurchaseMetalType.getValue()); // Set Metal reference
                editingPurchaseTransaction.setMetalType(cmbPurchaseMetalType.getValue().getMetalType());
                editingPurchaseTransaction.setPurity(purity);
                editingPurchaseTransaction.setGrossWeight(grossWeight);
                editingPurchaseTransaction.setSellerPercentage(sellerPercentage);
                editingPurchaseTransaction.setRatePerGram(rate);
                editingPurchaseTransaction.calculateNetWeightAndAmount();

                LOG.info("Updated purchase metal: {} {} - {}g @ {}",
                        editingPurchaseTransaction.getMetalType(), editingPurchaseTransaction.getPurity(),
                        editingPurchaseTransaction.getGrossWeight(), editingPurchaseTransaction.getRatePerGram());

                editingPurchaseTransaction = null;

                // Refresh table
                purchaseMetalTable.refresh();
            } else {
                // Create new transaction
                PurchaseMetalTransaction transaction = PurchaseMetalTransaction.builder()
                        .metal(cmbPurchaseMetalType.getValue()) // Set Metal reference
                        .metalType(cmbPurchaseMetalType.getValue().getMetalType())
                        .purity(purity)
                        .grossWeight(grossWeight)
                        .sellerPercentage(sellerPercentage)
                        .ratePerGram(rate)
                        .build();

                transaction.calculateNetWeightAndAmount();

                // Add to list
                purchaseTransactions.add(transaction);

                LOG.info("Added purchase metal: {} {} - {}g @ {}",
                        transaction.getMetalType(), transaction.getPurity(),
                        transaction.getGrossWeight(), transaction.getRatePerGram());
            }

            // Clear form
            clearPurchaseForm();

            // Update totals
            calculateTotals();

        } catch (NumberFormatException e) {
            alertNotification.showError("Please enter valid numbers");
        } catch (Exception e) {
            LOG.error("Error adding purchase metal", e);
            alertNotification.showError("Failed to add purchase: " + e.getMessage());
        }
    }

    /**
     * Handle edit purchase metal
     */
    @FXML
    private void handleEditPurchase() {
        PurchaseMetalTransaction selected = purchaseMetalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showError("Please select a purchase item to edit");
            return;
        }

        // Populate form with selected item data
        editingPurchaseTransaction = selected;

        // Find and set metal type
        for (Metal metal : metals) {
            if (metal.getMetalType().equals(selected.getMetalType())) {
                cmbPurchaseMetalType.setValue(metal);
                break;
            }
        }

        txtPurchasePurity.setText(selected.getPurity().toString());
        txtPurchaseGrossWeight.setText(selected.getGrossWeight().toString());
        txtSellerPercentage.setText(selected.getSellerPercentage().toString());
        txtPurchaseRate.setText(selected.getRatePerGram().toString());

        // Calculations will auto-update via listeners
        btnAddPurchase.setText("UPDATE");
        btnAddPurchase.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-family: 'Segoe UI Bold'; -fx-font-size: 13px; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(255,152,0,0.3), 8, 0, 0, 2);");

        LOG.info("Editing purchase metal: {} {}", selected.getMetalType(), selected.getPurity());
    }

    /**
     * Handle delete purchase metal
     */
    @FXML
    private void handleDeletePurchase() {
        PurchaseMetalTransaction selected = purchaseMetalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showError("Please select a purchase item to delete");
            return;
        }

        // Remove from list
        purchaseTransactions.remove(selected);

        // Clear form if this was the editing item
        if (editingPurchaseTransaction == selected) {
            clearPurchaseForm();
        }

        // Update totals
        calculateTotals();

        LOG.info("Deleted purchase metal: {} {}", selected.getMetalType(), selected.getPurity());
    }

    /**
     * Handle clear purchase form
     */
    @FXML
    private void handleClearPurchase() {
        clearPurchaseForm();
    }

    /**
     * Handle add exchange metal
     */
    @FXML
    private void handleAddExchange() {
        try {
            // Validate inputs
            if (txtExchangeItemName.getText().trim().isEmpty()) {
                alertNotification.showError("Please enter item description");
                return;
            }

            if (cmbExchangeMetalType.getValue() == null) {
                alertNotification.showError("Please select metal type");
                return;
            }

            BigDecimal purity = new BigDecimal(txtExchangePurity.getText().trim());
            BigDecimal grossWeight = new BigDecimal(txtExchangeGrossWeight.getText().trim());
            BigDecimal finePercentage = new BigDecimal(txtFinePercentage.getText().trim());
            BigDecimal rate = new BigDecimal(txtExchangeRate.getText().trim());

            // Check if editing or adding new
            if (editingExchangeTransaction != null) {
                // Update existing transaction
                editingExchangeTransaction.setItemName(txtExchangeItemName.getText().trim());
                editingExchangeTransaction.setMetalType(cmbExchangeMetalType.getValue().getMetalType());
                editingExchangeTransaction.setPurity(purity);
                editingExchangeTransaction.setGrossWeight(grossWeight);
                editingExchangeTransaction.setFinePercentage(finePercentage);
                editingExchangeTransaction.setRatePerGram(rate);
                editingExchangeTransaction.calculateNetWeightAndAmount();

                LOG.info("Updated exchange metal: {} - {} {} - {}g (fine: {}%)",
                        editingExchangeTransaction.getItemName(), editingExchangeTransaction.getMetalType(),
                        editingExchangeTransaction.getPurity(), editingExchangeTransaction.getGrossWeight(),
                        editingExchangeTransaction.getFinePercentage());

                editingExchangeTransaction = null;

                // Refresh table
                exchangeMetalTable.refresh();
            } else {
                // Create new transaction
                PurchaseExchangeTransaction transaction = PurchaseExchangeTransaction.builder()
                        .itemName(txtExchangeItemName.getText().trim())
                        .metalType(cmbExchangeMetalType.getValue().getMetalType())
                        .purity(purity)
                        .grossWeight(grossWeight)
                        .finePercentage(finePercentage)
                        .ratePerGram(rate)
                        .build();

                transaction.calculateNetWeightAndAmount();

                // Add to list
                exchangeTransactions.add(transaction);

                LOG.info("Added exchange metal: {} - {} {} - {}g (fine: {}%)",
                        transaction.getItemName(), transaction.getMetalType(),
                        transaction.getPurity(), transaction.getGrossWeight(),
                        transaction.getFinePercentage());
            }

            // Clear form
            clearExchangeForm();

            // Update totals
            calculateTotals();

        } catch (NumberFormatException e) {
            alertNotification.showError("Please enter valid numbers");
        } catch (Exception e) {
            LOG.error("Error adding exchange metal", e);
            alertNotification.showError("Failed to add exchange: " + e.getMessage());
        }
    }

    /**
     * Handle edit exchange metal
     */
    @FXML
    private void handleEditExchange() {
        PurchaseExchangeTransaction selected = exchangeMetalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showError("Please select an exchange item to edit");
            return;
        }

        // Populate form with selected item data
        editingExchangeTransaction = selected;

        txtExchangeItemName.setText(selected.getItemName());

        // Find and set metal type
        for (Metal metal : metals) {
            if (metal.getMetalType().equals(selected.getMetalType())) {
                cmbExchangeMetalType.setValue(metal);
                break;
            }
        }

        txtExchangePurity.setText(selected.getPurity().toString());
        txtExchangeGrossWeight.setText(selected.getGrossWeight().toString());
        txtFinePercentage.setText(selected.getFinePercentage().toString());
        txtExchangeRate.setText(selected.getRatePerGram().toString());

        // Calculations will auto-update via listeners
        btnAddExchange.setText("UPDATE");
        btnAddExchange.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-family: 'Segoe UI Bold'; -fx-font-size: 13px; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(255,152,0,0.3), 8, 0, 0, 2);");

        LOG.info("Editing exchange metal: {}", selected.getItemName());
    }

    /**
     * Handle delete exchange metal
     */
    @FXML
    private void handleDeleteExchange() {
        PurchaseExchangeTransaction selected = exchangeMetalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertNotification.showError("Please select an exchange item to delete");
            return;
        }

        // Remove from list
        exchangeTransactions.remove(selected);

        // Clear form if this was the editing item
        if (editingExchangeTransaction == selected) {
            clearExchangeForm();
        }

        // Update totals
        calculateTotals();

        LOG.info("Deleted exchange metal: {}", selected.getItemName());
    }

    /**
     * Handle clear exchange form
     */
    @FXML
    private void handleClearExchange() {
        clearExchangeForm();
    }

    /**
     * Handle refresh stock
     */
    @FXML
    private void handleRefreshStock() {
        refreshMetalStock();
    }

    /**
     * Handle save invoice
     */
    @FXML
    private void handleSave() {
        try {
            // Validate
            if (supplierSearch.getSelectedItem() == null) {
                alertNotification.showError("Please select supplier");
                return;
            }

            if (purchaseTransactions.isEmpty() && exchangeTransactions.isEmpty()) {
                alertNotification.showError("Please add at least one purchase or exchange item");
                return;
            }

            // Create invoice
            PurchaseInvoice invoice = PurchaseInvoice.builder()
                    .supplier(supplierSearch.getSelectedItem())
                    .supplierInvoiceNumber(txtSupplierInvoice.getText().trim())
                    .invoiceDate(LocalDateTime.now())
                    .purchaseType(PurchaseInvoice.PurchaseType.RAW_MATERIAL)
                    .status(PurchaseInvoice.InvoiceStatus.CONFIRMED)
                    .purchaseMetalTransactions(new ArrayList<>(purchaseTransactions))
                    .purchaseExchangeTransactions(new ArrayList<>(exchangeTransactions))
                    .discount(parseBigDecimal(txtDiscount.getText()))
                    .isTaxApplicable(chkTaxApplicable.isSelected())
                    .gstRate(new BigDecimal("3.00"))
                    .build();

            // Set payment details
            if (chkCreditPurchase.isSelected()) {
                invoice.setPaymentMethod(PurchaseInvoice.PaymentMethod.CREDIT);
                invoice.setPaidAmount(BigDecimal.ZERO);
            } else {
                invoice.setPaymentMethod(PurchaseInvoice.PaymentMethod.BANK_TRANSFER);
                invoice.setPaidAmount(parseBigDecimal(txtPaidAmount.getText()));
            }

            // Save invoice
            PurchaseInvoice savedInvoice = purchaseInvoiceService.savePurchaseInvoice(invoice);

            alertNotification.showSuccess("Purchase invoice saved successfully!\nInvoice #: " + savedInvoice.getInvoiceNumber());

            LOG.info("Saved purchase invoice: {}", savedInvoice.getInvoiceNumber());

            // Clear all
            handleClearAll();

        } catch (Exception e) {
            LOG.error("Error saving purchase invoice", e);
            alertNotification.showError("Failed to save invoice: " + e.getMessage());
        }
    }

    /**
     * Handle clear all
     */
    @FXML
    private void handleClearAll() {
        purchaseTransactions.clear();
        exchangeTransactions.clear();
        clearPurchaseForm();
        clearExchangeForm();

        supplierSearch.clear();
        txtSupplierInvoice.clear();
        txtDiscount.setText("0.00");
        txtPaidAmount.clear();
        chkCreditPurchase.setSelected(false);
        chkTaxApplicable.setSelected(true);

        lblInvoiceNumber.setText("Invoice #: " + purchaseInvoiceService.generateInvoiceNumber());

        calculateTotals();
    }

    // ==================== Helper Methods ====================

    /**
     * Calculate purchase net weight
     */
    private void calculatePurchaseNetWeight() {
        try {
            BigDecimal grossWeight = parseBigDecimal(txtPurchaseGrossWeight.getText());
            BigDecimal sellerPct = parseBigDecimal(txtSellerPercentage.getText());

            if (grossWeight != null && sellerPct != null) {
                BigDecimal netWeight = grossWeight.multiply(sellerPct)
                        .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP);
                txtPurchaseNetWeight.setText(WeightFormatter.format(netWeight));
                calculatePurchaseAmount();
            }
        } catch (Exception e) {
            // Ignore calculation errors during typing
        }
    }

    /**
     * Calculate purchase amount
     */
    private void calculatePurchaseAmount() {
        try {
            BigDecimal netWeight = parseBigDecimal(txtPurchaseNetWeight.getText());
            BigDecimal rate = parseBigDecimal(txtPurchaseRate.getText());

            if (netWeight != null && rate != null) {
                BigDecimal amount = netWeight.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                txtPurchaseAmount.setText(CurrencyFormatter.format(amount));
            }
        } catch (Exception e) {
            // Ignore calculation errors during typing
        }
    }

    /**
     * Calculate exchange net weight
     */
    private void calculateExchangeNetWeight() {
        try {
            BigDecimal grossWeight = parseBigDecimal(txtExchangeGrossWeight.getText());
            BigDecimal finePct = parseBigDecimal(txtFinePercentage.getText());

            if (grossWeight != null && finePct != null) {
                BigDecimal netWeight = grossWeight.multiply(finePct)
                        .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP);
                txtExchangeNetWeight.setText(WeightFormatter.format(netWeight));
                calculateExchangeAmount();
            }
        } catch (Exception e) {
            // Ignore calculation errors during typing
        }
    }

    /**
     * Calculate exchange amount
     */
    private void calculateExchangeAmount() {
        try {
            BigDecimal netWeight = parseBigDecimal(txtExchangeNetWeight.getText());
            BigDecimal rate = parseBigDecimal(txtExchangeRate.getText());

            if (netWeight != null && rate != null) {
                BigDecimal amount = netWeight.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                txtExchangeAmount.setText(CurrencyFormatter.format(amount));
            }
        } catch (Exception e) {
            // Ignore calculation errors during typing
        }
    }

    /**
     * Calculate all totals
     */
    private void calculateTotals() {
        // Purchase total
        BigDecimal purchaseTotal = purchaseTransactions.stream()
                .map(PurchaseMetalTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Exchange total
        BigDecimal exchangeTotal = exchangeTransactions.stream()
                .map(PurchaseExchangeTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Subtotal, GST, Net Total
        BigDecimal subtotal = purchaseTotal;
        BigDecimal gst = BigDecimal.ZERO;

        // Only calculate GST if tax checkbox is checked
        if (chkTaxApplicable.isSelected()) {
            gst = subtotal.multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal netTotal = subtotal.add(gst);

        // Discount
        BigDecimal discount = parseBigDecimal(txtDiscount.getText());
        if (discount == null) discount = BigDecimal.ZERO;

        // Grand total
        BigDecimal grandTotal = netTotal.subtract(exchangeTotal).subtract(discount);

        // Update labels
        lblPurchaseCount.setText("(" + purchaseTransactions.size() + " items)");
        lblPurchaseTotal.setText(CurrencyFormatter.format(purchaseTotal));
        lblExchangeCount.setText("(" + exchangeTransactions.size() + " items)");
        lblExchangeTotal.setText(CurrencyFormatter.format(exchangeTotal));
        lblSubtotal.setText(CurrencyFormatter.format(subtotal));
        lblGST.setText(CurrencyFormatter.format(gst));
        lblNetTotal.setText(CurrencyFormatter.format(netTotal));
        lblExchangeDeduction.setText("- " + CurrencyFormatter.format(exchangeTotal));
        lblGrandTotal.setText(CurrencyFormatter.format(grandTotal));

        // Update item counts
        lblPurchaseItemCount.setText(purchaseTransactions.size() + " items");
        lblExchangeItemCount.setText(exchangeTransactions.size() + " items");

        // Calculate pending amount
        calculatePendingAmount();
    }

    /**
     * Calculate pending amount
     */
    private void calculatePendingAmount() {
        try {
            String grandTotalText = lblGrandTotal.getText().replace("₹", "").replace(",", "").trim();
            BigDecimal grandTotal = new BigDecimal(grandTotalText);

            BigDecimal paidAmount = parseBigDecimal(txtPaidAmount.getText());
            if (paidAmount == null) paidAmount = BigDecimal.ZERO;

            BigDecimal pending = grandTotal.subtract(paidAmount);
            lblPendingAmount.setText(CurrencyFormatter.format(pending));
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Clear purchase form
     */
    private void clearPurchaseForm() {
        cmbPurchaseMetalType.setValue(null);
        txtPurchasePurity.setText("916");
        txtPurchaseGrossWeight.clear();
        txtSellerPercentage.setText("97.00");
        txtPurchaseNetWeight.clear();
        txtPurchaseRate.clear();
        txtPurchaseAmount.clear();

        // Reset editing mode
        editingPurchaseTransaction = null;
        btnAddPurchase.setText("ADD TO BILL");
        btnAddPurchase.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI Bold'; -fx-font-size: 13px; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(76,175,80,0.3), 8, 0, 0, 2);");

        // Clear table selection
        purchaseMetalTable.getSelectionModel().clearSelection();
    }

    /**
     * Clear exchange form
     */
    private void clearExchangeForm() {
        txtExchangeItemName.clear();
        cmbExchangeMetalType.setValue(null);
        txtExchangePurity.setText("916");
        txtExchangeGrossWeight.clear();
        txtFinePercentage.setText("80.00");
        txtExchangeNetWeight.clear();
        txtExchangeRate.clear();
        txtExchangeAmount.clear();

        // Reset editing mode
        editingExchangeTransaction = null;
        btnAddExchange.setText("ADD EXCHANGE");
        btnAddExchange.setStyle("-fx-background-color: #FF6F00; -fx-text-fill: white; -fx-font-family: 'Segoe UI Bold'; -fx-font-size: 13px; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(255,111,0,0.3), 8, 0, 0, 2);");

        // Clear table selection
        exchangeMetalTable.getSelectionModel().clearSelection();
    }

    /**
     * Refresh metal stock display
     */
    private void refreshMetalStock() {
        Platform.runLater(() -> {
            try {
                metalStockContainer.getChildren().clear();

                List<PurchaseMetalStock> stockList = purchaseMetalStockService.getAllAvailableStock();

                if (stockList.isEmpty()) {
                    Label noStock = new Label("No metal stock available");
                    noStock.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #757575; -fx-padding: 16;");
                    metalStockContainer.getChildren().add(noStock);
                    return;
                }

                for (PurchaseMetalStock stock : stockList) {
                    VBox stockCard = createMetalStockCard(stock);
                    metalStockContainer.getChildren().add(stockCard);
                }

            } catch (Exception e) {
                LOG.error("Error refreshing metal stock", e);
            }
        });
    }

    /**
     * Create metal stock card
     */
    private VBox createMetalStockCard(PurchaseMetalStock stock) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-padding: 12; -fx-border-color: #BDBDBD; -fx-border-radius: 8; -fx-border-width: 1;");

        // Metal name
        Label lblMetal = new Label(stock.getMetalType() + " " + stock.getPurity());
        lblMetal.setStyle("-fx-font-family: 'Segoe UI Semibold'; -fx-font-size: 13px; -fx-text-fill: #424242;");

        // Available weight
        HBox weightBox = new HBox(5);
        weightBox.setAlignment(Pos.CENTER_LEFT);
        Label lblAvailLabel = new Label("Available:");
        lblAvailLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #616161;");
        Label lblAvailWeight = new Label(WeightFormatter.format(stock.getAvailableWeight()) + " g");
        lblAvailWeight.setStyle("-fx-font-family: 'Segoe UI Bold'; -fx-font-size: 12px; -fx-text-fill: #2E7D32;");
        weightBox.getChildren().addAll(lblAvailLabel, lblAvailWeight);

        card.getChildren().addAll(lblMetal, weightBox);
        return card;
    }

    /**
     * Parse BigDecimal safely
     */
    private BigDecimal parseBigDecimal(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
