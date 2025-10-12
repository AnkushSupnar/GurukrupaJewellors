package com.gurukrupa.controller.transaction;

import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.entities.PurchaseInvoice.PurchaseType;
import com.gurukrupa.data.entities.PurchaseTransaction.ItemType;
import com.gurukrupa.data.entities.PurchaseExchangeTransaction;
import com.gurukrupa.data.entities.ExchangeMetalStock;
import com.gurukrupa.data.service.*;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.utility.CurrencyFormatter;
import com.gurukrupa.utility.WeightFormatter;
import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.controller.master.JewelryItemFormController;
import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.view.FxmlView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;


import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.util.StringConverter;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PurchaseInvoiceController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseInvoiceController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    // Stock Catalog Sidebar
    @FXML private TextField txtStockSearch;
    @FXML private Button btnClearSearch;
    @FXML private Label lblStockCount;
    @FXML private ListView<JewelryItem> listViewStock;
    @FXML private VBox metalStockContainer;
    @FXML private Button btnRefreshStock;
    
    // Header and Supplier Info
    @FXML private Label lblInvoiceNumber;
    @FXML private ComboBox<Supplier> cmbSupplier;
    @FXML private TextField txtSupplierName;
    @FXML private TextField txtGSTNumber;
    @FXML private TextField txtSupplierContact;
    @FXML private TextField txtInvoiceDate;
    @FXML private TextField txtSupplierInvoice;
    
    // Chip/Toggle Controls
    @FXML private ToggleButton chipPurchase;
    @FXML private ToggleButton chipExchange;
    @FXML private ToggleGroup modeToggleGroup;
    @FXML private VBox purchasePanel;
    @FXML private VBox exchangePanel;
    
    // Purchase Items Tab - Form Fields
    @FXML private TextField txtItemCode;
    @FXML private HBox itemSearchBox;
    private AutoCompleteTextField<JewelryItem> itemSearchField;
    @FXML private ComboBox<Metal> cmbMetalType;
    @FXML private TextField txtPurity;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtGrossWeight;
    @FXML private TextField txtStoneWeight;
    @FXML private TextField txtNetWeight;
    @FXML private TextField txtRate;
    @FXML private TextField txtAmount;
    @FXML private Label lblLabourCharges;
    @FXML private TextField txtLabourCharges;
    @FXML private Button btnAddToBill;
    @FXML private Button btnEditPurchaseItem;
    @FXML private Button btnDeletePurchaseItem;
    @FXML private Button btnClearForm;
    @FXML private Button btnAddNewItem;
    
    // Purchase Items Table
    @FXML private TableView<PurchaseTransaction> purchaseItemsTable;
    @FXML private TableColumn<PurchaseTransaction, Integer> colSno;
    @FXML private TableColumn<PurchaseTransaction, String> colItemCode;
    @FXML private TableColumn<PurchaseTransaction, String> colItemName;
    @FXML private TableColumn<PurchaseTransaction, String> colMetalType;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colPurity;
    @FXML private TableColumn<PurchaseTransaction, Integer> colQuantity;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colGrossWeight;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colStoneWeight;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colWeight;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colRate;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colLabour;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colAmount;
    
    // Exchange Items Tab - Form Fields
    @FXML private TextField txtExchangeItemName;
    @FXML private ComboBox<Metal> cmbExchangeMetalType;
    @FXML private TextField txtExchangePurity;
    @FXML private TextField txtExchangeGrossWeight;
    @FXML private TextField txtExchangeDeduction;
    @FXML private TextField txtExchangeNetWeight;
    @FXML private TextField txtExchangeRate;
    @FXML private TextField txtExchangeAmount;
    @FXML private Button btnAddExchangeItem;
    @FXML private Button btnEditExchangeItem;
    @FXML private Button btnDeleteExchangeItem;
    @FXML private Button btnClearExchangeForm;
    
    // Exchange Items Table
    @FXML private TableView<PurchaseExchangeTransaction> exchangeItemsTable;
    @FXML private TableColumn<PurchaseExchangeTransaction, Integer> colExSno;
    @FXML private TableColumn<PurchaseExchangeTransaction, String> colExItemName;
    @FXML private TableColumn<PurchaseExchangeTransaction, String> colExMetalType;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExPurity;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExGrossWeight;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExDeduction;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExNetWeight;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExRate;
    @FXML private TableColumn<PurchaseExchangeTransaction, BigDecimal> colExAmount;
    
    // Summary Panel
    @FXML private Label lblTotalPurchaseItems;
    @FXML private Label lblPurchaseAmount;
    @FXML private Label lblTotalExchangeItems;
    @FXML private Label lblExchangeDeduction;
    @FXML private Label lblSubtotal;
    @FXML private Label lblGST;
    @FXML private Label lblTotalAmount;
    @FXML private TextField txtDiscount;
    @FXML private TextField txtGstRate;
    @FXML private Label lblNetTotal;
    @FXML private Label lblExchangeAmount;
    @FXML private Label lblGrandTotal;
    
    // Payment Details
    @FXML private CheckBox chkCreditPurchase;
    @FXML private ComboBox<BankAccount> cmbBankAccount;
    @FXML private Label lblAvailableBalance;
    @FXML private TextField txtPaymentReference;
    @FXML private TextField txtPaidAmount;
    @FXML private Label lblPendingAmount;
    
    // Action Buttons
    @FXML private Button btnPrint;
    @FXML private Button btnClear;
    @FXML private Button btnSave;

    @Autowired
    private PurchaseInvoiceService purchaseInvoiceService;
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private ExchangeMetalStockService metalStockService;
    
    @Autowired
    private MetalRateService metalRateService;
    
    @Autowired
    private MetalService metalService;
    
    @Autowired
    private PaymentModeService paymentModeService;
    
    @Autowired
    private BankAccountService bankAccountService;
    
    @Autowired
    private BankTransactionService bankTransactionService;
    
    @Autowired
    private JewelryItemService jewelryItemService;
    
    @Autowired
    private AlertNotification alertNotification;
    
    @Autowired
    private SpringFXMLLoader springFXMLLoader;

    private ObservableList<PurchaseTransaction> purchaseItems = FXCollections.observableArrayList();
    private ObservableList<PurchaseExchangeTransaction> exchangeItems = FXCollections.observableArrayList();
    private ObservableList<JewelryItem> stockItems = FXCollections.observableArrayList();
    private FilteredList<JewelryItem> filteredStockItems;
    private BigDecimal gstRate = new BigDecimal("3.00");
    private JewelryItem selectedStockItem = null;
    private BigDecimal currentLabourPercentage = null;
    private boolean isProgrammaticallySettingFields = false;
    private PurchaseTransaction editingItem = null;
    private PurchaseExchangeTransaction editingExchangeItem = null;
    
    // Edit mode fields
    private Stage dialogStage;
    private boolean isEditMode = false;
    private PurchaseInvoice invoiceToEdit = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("Initializing PurchaseInvoiceController");
        
        // Initialize to new invoice mode by default
        setNewInvoiceMode();
        
        setupTableColumns();
        setupComboBoxes();
        setupListeners();
        setupStockCatalog();
        setupSupplierCombo();
        loadMetalStock();
        loadStockItems();
        setupItemSearchField(); // Must be after loadStockItems
        updateDateTime();
        clearForm();
        
        // Initialize GST rate field
        if (txtGstRate != null) {
            txtGstRate.setText(gstRate.toString());
        }
        
        // Add listeners for collections
        purchaseItems.addListener((javafx.collections.ListChangeListener<PurchaseTransaction>) change -> updateSummary());
        exchangeItems.addListener((javafx.collections.ListChangeListener<PurchaseExchangeTransaction>) change -> updateSummary());
    }
    
    private void setupTableColumns() {
        // Purchase Items Table
        if (colSno != null) {
            colSno.setCellValueFactory(cellData -> {
                int index = purchaseItemsTable.getItems().indexOf(cellData.getValue()) + 1;
                return new SimpleObjectProperty<>(index);
            });
        }
        if (colItemCode != null) colItemCode.setCellValueFactory(new PropertyValueFactory<>("itemCode"));
        if (colItemName != null) colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        if (colMetalType != null) colMetalType.setCellValueFactory(new PropertyValueFactory<>("metalType"));
        if (colPurity != null) colPurity.setCellValueFactory(new PropertyValueFactory<>("purity"));
        if (colQuantity != null) colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        if (colGrossWeight != null) colGrossWeight.setCellValueFactory(new PropertyValueFactory<>("grossWeight"));
        if (colStoneWeight != null) {
            colStoneWeight.setCellValueFactory(cellData -> {
                PurchaseTransaction item = cellData.getValue();
                BigDecimal grossWeight = item.getGrossWeight() != null ? item.getGrossWeight() : BigDecimal.ZERO;
                BigDecimal netWeight = item.getNetWeight() != null ? item.getNetWeight() : BigDecimal.ZERO;
                BigDecimal stoneWeight = grossWeight.subtract(netWeight);
                return new SimpleObjectProperty<>(stoneWeight);
            });
        }
        if (colWeight != null) colWeight.setCellValueFactory(new PropertyValueFactory<>("netWeight"));
        // Display rate per 10 grams
        if (colRate != null) {
            colRate.setCellValueFactory(cellData -> {
                BigDecimal ratePerGram = cellData.getValue().getRatePerGram();
                BigDecimal ratePerTenGrams = ratePerGram != null ? ratePerGram.multiply(BigDecimal.TEN) : BigDecimal.ZERO;
                return new SimpleObjectProperty<>(ratePerTenGrams);
            });
        }
        if (colLabour != null) colLabour.setCellValueFactory(new PropertyValueFactory<>("makingCharges"));
        if (colAmount != null) colAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        
        if (purchaseItemsTable != null) {
            purchaseItemsTable.setItems(purchaseItems);
            purchaseItemsTable.setEditable(false);
        }
        
        // Exchange Items Table
        if (colExSno != null) {
            colExSno.setCellValueFactory(cellData -> {
                int index = exchangeItemsTable != null ? exchangeItemsTable.getItems().indexOf(cellData.getValue()) + 1 : 1;
                return new SimpleObjectProperty<>(index);
            });
        }
        if (colExItemName != null) colExItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        if (colExMetalType != null) colExMetalType.setCellValueFactory(new PropertyValueFactory<>("metalType"));
        if (colExPurity != null) colExPurity.setCellValueFactory(new PropertyValueFactory<>("purity"));
        if (colExGrossWeight != null) colExGrossWeight.setCellValueFactory(new PropertyValueFactory<>("grossWeight"));
        if (colExDeduction != null) colExDeduction.setCellValueFactory(new PropertyValueFactory<>("deduction"));
        if (colExNetWeight != null) colExNetWeight.setCellValueFactory(new PropertyValueFactory<>("netWeight"));
        // Display rate per 10 grams
        if (colExRate != null) {
            colExRate.setCellValueFactory(cellData -> {
                BigDecimal ratePerTenGrams = cellData.getValue().getRatePerTenGrams();
                return new SimpleObjectProperty<>(ratePerTenGrams != null ? ratePerTenGrams : BigDecimal.ZERO);
            });
        }
        if (colExAmount != null) colExAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        
        if (exchangeItemsTable != null) {
            exchangeItemsTable.setItems(exchangeItems);
            exchangeItemsTable.setEditable(false);
        }
    }
    
    private void setupComboBoxes() {
        // Bank account combo
        loadBankAccounts();
        
        // Set up bank account selection listener
        if (cmbBankAccount != null) {
            cmbBankAccount.setConverter(new StringConverter<BankAccount>() {
                @Override
                public String toString(BankAccount account) {
                    if (account == null) return "";
                    return account.getBankName() + " - " + account.getAccountNumber();
                }
                
                @Override
                public BankAccount fromString(String string) {
                    return null;
                }
            });
            
            cmbBankAccount.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && lblAvailableBalance != null) {
                    lblAvailableBalance.setText(CurrencyFormatter.format(newVal.getCurrentBalance()));
                } else if (lblAvailableBalance != null) {
                    lblAvailableBalance.setText("₹ 0.00");
                }
            });
        }
        
        // Metal type combos - Load from database like in BillingController
        try {
            List<Metal> metals = metalService.getAllActiveMetals();
            ObservableList<Metal> metalList = FXCollections.observableArrayList(metals);
            
            if (cmbMetalType != null) {
                cmbMetalType.setItems(metalList);
                cmbMetalType.setConverter(new StringConverter<Metal>() {
                    @Override
                    public String toString(Metal metal) {
                        return metal != null ? metal.getMetalName() : "";
                    }
                    
                    @Override
                    public Metal fromString(String string) {
                        return null;
                    }
                });
                
                // Select first metal if available
                if (!metalList.isEmpty()) {
                    cmbMetalType.setValue(metalList.get(0));
                }
                
                // Add listener to update rate when metal is selected
                cmbMetalType.valueProperty().addListener((obs, oldMetal, newMetal) -> {
                    if (newMetal != null) {
                        updateMetalRate(newMetal);
                    }
                });
            }
            
            if (cmbExchangeMetalType != null) {
                cmbExchangeMetalType.setItems(metalList);
                cmbExchangeMetalType.setConverter(new StringConverter<Metal>() {
                    @Override
                    public String toString(Metal metal) {
                        return metal != null ? metal.getMetalName() : "";
                    }
                    
                    @Override
                    public Metal fromString(String string) {
                        return null;
                    }
                });
                
                // Select first metal if available
                if (!metalList.isEmpty()) {
                    cmbExchangeMetalType.setValue(metalList.get(0));
                }
            }
        } catch (Exception e) {
            LOG.error("Error loading metals", e);
            // Fallback to hardcoded values if database fetch fails
            ObservableList<String> metalTypes = FXCollections.observableArrayList("GOLD", "SILVER", "PLATINUM");
            alertNotification.showError("Failed to load metals from database. Using default values.");
        }
        

    }
    
    private void loadBankAccounts() {
        try {
            List<BankAccount> accounts = bankAccountService.getAllActiveBankAccounts();
            ObservableList<BankAccount> accountList = FXCollections.observableArrayList(accounts);
            cmbBankAccount.setItems(accountList);
            
            // Select the first account by default if available
            if (!accounts.isEmpty()) {
                cmbBankAccount.setValue(accounts.get(0));
            }
        } catch (Exception e) {
            LOG.error("Error loading bank accounts", e);
            alertNotification.showError("Failed to load bank accounts: " + e.getMessage());
        }
    }
    
    private void setupSupplierCombo() {
        try {
            List<Supplier> suppliers = supplierService.getAllActiveSuppliers();
            ObservableList<Supplier> supplierList = FXCollections.observableArrayList(suppliers);
            cmbSupplier.setItems(supplierList);
            
            // Set up supplier converter
            cmbSupplier.setConverter(new StringConverter<Supplier>() {
                @Override
                public String toString(Supplier supplier) {
                    if (supplier == null) return "";
                    return supplier.getSupplierFullName() + 
                           (supplier.getGstNumber() != null ? " (GST: " + supplier.getGstNumber() + ")" : "");
                }
                
                @Override
                public Supplier fromString(String string) {
                    return null;
                }
            });
            
            // Handle supplier selection
            cmbSupplier.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateSupplierInfo(newVal);
            });
            
        } catch (Exception e) {
            LOG.error("Error loading suppliers", e);
            alertNotification.showError("Failed to load suppliers: " + e.getMessage());
        }
    }
    
    private void loadMetalStock() {
        Platform.runLater(() -> {
            try {
                metalStockContainer.getChildren().clear();
                
                List<ExchangeMetalStock> stocks = metalStockService.getAllMetalStocks();
                for (ExchangeMetalStock stock : stocks) {
                    VBox stockCard = createMetalStockCard(stock);
                    metalStockContainer.getChildren().add(stockCard);
                }
                
                if (stocks.isEmpty()) {
                    Label noStockLabel = new Label("No exchange metal stock available");
                    noStockLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
                    metalStockContainer.getChildren().add(noStockLabel);
                }
            } catch (Exception e) {
                LOG.error("Error loading metal stock", e);
            }
        });
    }
    
    private VBox createMetalStockCard(ExchangeMetalStock stock) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8; -fx-padding: 10; " +
                     "-fx-border-color: #E0E0E0; -fx-border-radius: 8;");
        
        HBox metalInfo = new HBox(10);
        metalInfo.setAlignment(Pos.CENTER_LEFT);
        
        Label metalLabel = new Label(stock.getMetalType() + " " + stock.getPurity() + "k");
        metalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label availableLabel = new Label(WeightFormatter.format(stock.getAvailableWeight()) + " g");
        availableLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        
        metalInfo.getChildren().addAll(metalLabel, spacer, availableLabel);
        
        Label totalLabel = new Label("Total: " + WeightFormatter.format(stock.getTotalWeight()) + " g");
        totalLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        Label soldLabel = new Label("Sold: " + WeightFormatter.format(stock.getSoldWeight()) + " g");
        soldLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        card.getChildren().addAll(metalInfo, totalLabel, soldLabel);
        
        return card;
    }
    
    private void updateDateTime() {
        Platform.runLater(() -> {
            if (txtInvoiceDate != null) {
                txtInvoiceDate.setText(LocalDateTime.now().format(DATE_FORMATTER));
            }
            if (lblInvoiceNumber != null) {
                lblInvoiceNumber.setText("Invoice #: " + purchaseInvoiceService.generateInvoiceNumber());
            }
        });
    }
    
    private void updateExchangeStockDisplay() {
        if (cmbExchangeMetalType == null || cmbExchangeMetalType.getValue() == null || txtExchangePurity == null) {
            return;
        }
        
        try {
            Metal selectedMetal = cmbExchangeMetalType.getValue();
            String metalType = selectedMetal.getMetalName();
            BigDecimal purity = parseBigDecimal(txtExchangePurity);
            
            // Get available stock
            // Use purity 0 to match how billing system stores exchange metal
            BigDecimal stockPurity = new BigDecimal("0");
            Optional<ExchangeMetalStock> stockOpt = metalStockService.getMetalStock(metalType, stockPurity);
            BigDecimal availableWeight = BigDecimal.ZERO;
            if (stockOpt.isPresent()) {
                availableWeight = stockOpt.get().getAvailableWeight();
            }
            
            // Calculate already added weight (ignore purity for stock calculation)
            BigDecimal alreadyAddedWeight = BigDecimal.ZERO;
            for (PurchaseExchangeTransaction item : exchangeItems) {
                if (item.getMetalType().equals(metalType) &&
                    item != editingExchangeItem) {
                    alreadyAddedWeight = alreadyAddedWeight.add(item.getNetWeight() != null ? 
                        item.getNetWeight() : BigDecimal.ZERO);
                }
            }
            
            BigDecimal remainingWeight = availableWeight.subtract(alreadyAddedWeight);
            
            // Make variables final for lambda
            final BigDecimal finalAvailableWeight = availableWeight;
            final BigDecimal finalAlreadyAddedWeight = alreadyAddedWeight;
            final BigDecimal finalRemainingWeight = remainingWeight;
            
            // Update display
            Platform.runLater(() -> {
                String stockInfo = String.format("Available: %.3f g | Used: %.3f g | Remaining: %.3f g", 
                    finalAvailableWeight, finalAlreadyAddedWeight, finalRemainingWeight);
                
                // You can display this in a label or tooltip
                if (txtExchangeNetWeight != null) {
                    txtExchangeNetWeight.setPromptText(stockInfo);
                    
                    // Change border color based on availability
                    if (finalRemainingWeight.compareTo(BigDecimal.ZERO) <= 0) {
                        txtExchangeNetWeight.setStyle("-fx-border-color: red;");
                    } else if (finalRemainingWeight.compareTo(new BigDecimal("10")) < 0) {
                        txtExchangeNetWeight.setStyle("-fx-border-color: orange;");
                    } else {
                        txtExchangeNetWeight.setStyle("");
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Error updating exchange stock display", e);
        }
    }
    
    private void updateSupplierInfo(Supplier supplier) {
        if (supplier != null) {
            if (txtSupplierName != null) txtSupplierName.setText(supplier.getSupplierFullName());
            if (txtGSTNumber != null) txtGSTNumber.setText(supplier.getGstNumber() != null ? supplier.getGstNumber() : "");
            if (txtSupplierContact != null) txtSupplierContact.setText(supplier.getMobile() != null ? supplier.getMobile() : "");
        } else {
            if (txtSupplierName != null) txtSupplierName.clear();
            if (txtGSTNumber != null) txtGSTNumber.clear();
            if (txtSupplierContact != null) txtSupplierContact.clear();
        }
    }
    
    private void setupListeners() {
        // Item code listener to populate fields when item code is entered
        if (txtItemCode != null) {
            txtItemCode.textProperty().addListener((obs, oldVal, newVal) -> {
                // Skip if we're programmatically setting fields
                if (isProgrammaticallySettingFields) {
                    return;
                }
                
                if (newVal != null && !newVal.isEmpty() && !newVal.equals(oldVal)) {
                    // Find matching item by code
                    JewelryItem matchedItem = stockItems.stream()
                        .filter(item -> item.getItemCode().equalsIgnoreCase(newVal))
                        .findFirst()
                        .orElse(null);
                    
                    if (matchedItem != null) {
                        // Use the same method to populate all fields consistently
                        populatePurchaseItemFields(matchedItem);
                    }
                }
            });
        }
        
        // Calculate totals when fields change
        if (txtQuantity != null) {
            txtQuantity.textProperty().addListener((obs, oldVal, newVal) -> calculateItemTotal());
        }
        if (txtGrossWeight != null) {
            txtGrossWeight.textProperty().addListener((obs, oldVal, newVal) -> calculateNetWeight());
        }
        if (txtStoneWeight != null) {
            txtStoneWeight.textProperty().addListener((obs, oldVal, newVal) -> calculateNetWeight());
        }
        if (txtNetWeight != null) {
            txtNetWeight.textProperty().addListener((obs, oldVal, newVal) -> calculateItemTotal());
        }
        if (txtRate != null) {
            txtRate.textProperty().addListener((obs, oldVal, newVal) -> calculateItemTotal());
        }
        if (txtLabourCharges != null) {
            txtLabourCharges.textProperty().addListener((obs, oldVal, newVal) -> calculateItemTotal());
        }
        
        // Exchange item calculations
        if (txtExchangeGrossWeight != null && txtExchangeDeduction != null && txtExchangeNetWeight != null) {
            txtExchangeGrossWeight.textProperty().addListener((obs, oldVal, newVal) -> calculateExchangeNetWeight());
            txtExchangeDeduction.textProperty().addListener((obs, oldVal, newVal) -> calculateExchangeNetWeight());
        }
        if (txtExchangeNetWeight != null && txtExchangeRate != null) {
            txtExchangeNetWeight.textProperty().addListener((obs, oldVal, newVal) -> {
                calculateExchangeTotal();
                updateExchangeStockDisplay();
            });
            txtExchangeRate.textProperty().addListener((obs, oldVal, newVal) -> calculateExchangeTotal());
        }
        
        // Update stock display when metal type or purity changes
        if (cmbExchangeMetalType != null) {
            cmbExchangeMetalType.valueProperty().addListener((obs, oldVal, newVal) -> updateExchangeStockDisplay());
        }
        if (txtExchangePurity != null) {
            txtExchangePurity.textProperty().addListener((obs, oldVal, newVal) -> updateExchangeStockDisplay());
        }
        
        // Discount and GST rate listeners
        if (txtDiscount != null) {
            txtDiscount.textProperty().addListener((obs, oldVal, newVal) -> updateSummary());
        }
        if (txtGstRate != null) {
            txtGstRate.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    gstRate = new BigDecimal(newVal);
                    updateSummary();
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
            });
        }
        
        // Paid amount listener
        if (txtPaidAmount != null) {
            txtPaidAmount.textProperty().addListener((obs, oldVal, newVal) -> updatePendingAmount());
        }
        
        // Credit purchase checkbox listener
        if (chkCreditPurchase != null) {
            chkCreditPurchase.selectedProperty().addListener((obs, wasCreditPurchase, isCreditPurchase) -> {
                if (isCreditPurchase) {
                    // Credit mode - disable payment fields
                    if (cmbBankAccount != null) cmbBankAccount.setDisable(true);
                    if (txtPaymentReference != null) txtPaymentReference.setDisable(true);
                    if (txtPaidAmount != null) {
                        txtPaidAmount.setText("0");
                        txtPaidAmount.setDisable(true);
                    }
                } else {
                    // Cash/Bank mode - enable payment fields
                    if (cmbBankAccount != null) cmbBankAccount.setDisable(false);
                    if (txtPaymentReference != null) txtPaymentReference.setDisable(false);
                    if (txtPaidAmount != null) {
                        txtPaidAmount.setDisable(false);
                        // Set paid amount to grand total by default
                        if (lblGrandTotal != null) {
                            String grandTotalText = lblGrandTotal.getText().replaceAll("[₹,\\s]", "");
                            txtPaidAmount.setText(grandTotalText);
                        }
                    }
                }
            });
        }
        
        // Clear search on button click
        if (btnClearSearch != null) {
            btnClearSearch.setOnAction(e -> {
                if (txtStockSearch != null) txtStockSearch.clear();
            });
        }
        
        // Refresh stock button
        if (btnRefreshStock != null) {
            btnRefreshStock.setOnAction(e -> {
                loadStockItems();
                loadMetalStock();
                setupItemSearchField(); // Refresh the search field with new items
            });
        }
        
        // Chip selection handling
        if (modeToggleGroup != null) {
            modeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == chipPurchase) {
                    showPurchasePanel();
                } else if (newVal == chipExchange) {
                    showExchangePanel();
                }
            });
        }
        
        // Style chips on hover and selection
        setupChipStyles();
    }
    
    private String getSelectedItemName() {
        if (itemSearchField != null && itemSearchField.getSelectedItem() != null) {
            return itemSearchField.getSelectedItem().getItemName();
        }
        return "";
    }
    
    private void setupItemSearchField() {
        try {
            // Create AutoCompleteTextField for item search
            StringConverter<JewelryItem> itemConverter = new StringConverter<JewelryItem>() {
                @Override
                public String toString(JewelryItem item) {
                    if (item != null) {
                        return item.getItemName() + " - " + item.getItemCode() + 
                               " (" + item.getMetalType() + " " + item.getPurity() + "k)";
                    }
                    return "";
                }
                
                @Override
                public JewelryItem fromString(String string) {
                    return null;
                }
            };
            
            // Filter function for searching items
            Function<String, List<JewelryItem>> filterFunction = searchText -> {
                if (searchText == null || searchText.trim().isEmpty()) {
                    // Show all items when the search text is empty or only contains spaces
                    return stockItems;
                }
                String lowerSearch = searchText.trim().toLowerCase();
                return stockItems.stream()
                    .filter(item -> item.getItemName().toLowerCase().contains(lowerSearch) ||
                                    item.getItemCode().toLowerCase().contains(lowerSearch) ||
                                    item.getCategory().toLowerCase().contains(lowerSearch))
                    .sorted((a, b) -> {
                        // Prioritize items that start with the search text
                        boolean aStartsWith = a.getItemName().toLowerCase().startsWith(lowerSearch);
                        boolean bStartsWith = b.getItemName().toLowerCase().startsWith(lowerSearch);
                        if (aStartsWith && !bStartsWith) return -1;
                        if (!aStartsWith && bStartsWith) return 1;
                        // Then sort by name
                        return a.getItemName().compareTo(b.getItemName());
                    })
                    .collect(Collectors.toList());
            };
            
            // Create the AutoCompleteTextField
            itemSearchField = new AutoCompleteTextField<>(stockItems, itemConverter, filterFunction);
            itemSearchField.setPromptText("Search item by name, code or category...");
            
            // Custom cell factory to show more item info
            itemSearchField.setCellFactory(item -> {
                Label label = new Label();
                if (item != null) {
                    VBox content = new VBox(2);
                    content.setPadding(new Insets(4, 8, 4, 8));
                    
                    Label nameLabel = new Label(item.getItemName() + " - " + item.getItemCode());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                    
                    Label detailsLabel = new Label(item.getCategory() + " | " + 
                                                  item.getMetalType() + " " + item.getPurity() + "k");
                    detailsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
                    
                    content.getChildren().addAll(nameLabel, detailsLabel);
                    label.setGraphic(content);
                }
                return label;
            });
            
            // Add to the UI
            if (itemSearchBox != null) {
                itemSearchBox.getChildren().clear();
                itemSearchBox.getChildren().add(itemSearchField.getNode());
                HBox.setHgrow(itemSearchField.getNode(), Priority.ALWAYS);
            }
            
            // Handle item selection
            itemSearchField.selectedItemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    populatePurchaseItemFields(newItem);
                }
            });
            
        } catch (Exception e) {
            LOG.error("Error setting up item search", e);
            alertNotification.showError("Failed to setup item search: " + e.getMessage());
        }
    }
    
    
    
    private void setupStockCatalog() {
        if (listViewStock != null && stockItems != null) {
            filteredStockItems = new FilteredList<>(stockItems, p -> true);
            listViewStock.setItems(filteredStockItems);
            
            // Custom cell factory for stock items - simple list view
            listViewStock.setCellFactory(listView -> new ListCell<JewelryItem>() {
                @Override
                protected void updateItem(JewelryItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        // Display format: ItemName - MetalType Purity
                        String displayText = String.format("%s - %s %sk", 
                            item.getItemName(), 
                            item.getMetalType(), 
                            item.getPurity()
                        );
                        setText(displayText);
                        setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-padding: 8 12 8 12;");
                        
                        // Add hover effect
                        setOnMouseEntered(e -> setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-padding: 8 12 8 12; -fx-background-color: #F5F5F5;"));
                        setOnMouseExited(e -> setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-padding: 8 12 8 12;"));
                    }
                }
            });
            
            // Handle item selection
            listViewStock.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    if (chipExchange != null && chipExchange.isSelected()) {
                        populateExchangeItemFields(newVal);
                    } else {
                        // Default to purchase mode or when purchase chip is selected
                        populatePurchaseItemFields(newVal);
                    }
                }
            });
            
            // Search functionality
            if (txtStockSearch != null) {
                txtStockSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                    filteredStockItems.setPredicate(item -> {
                        if (newVal == null || newVal.isEmpty()) {
                            return true;
                        }
                        String lowerCaseFilter = newVal.toLowerCase();
                        return item.getItemName().toLowerCase().contains(lowerCaseFilter) ||
                               item.getItemCode().toLowerCase().contains(lowerCaseFilter) ||
                               item.getCategory().toLowerCase().contains(lowerCaseFilter);
                    });
                    updateStockCount();
                });
            }
        }
    }
    
    // Removed createStockItemDisplay method - now using simple list display
    
    private void loadStockItems() {
        try {
            List<JewelryItem> items = jewelryItemService.getAllActiveItems();
            stockItems.clear();
            stockItems.addAll(items);
            updateStockCount();
            updateItemNameSuggestions(); // Update autocomplete suggestions
        } catch (Exception e) {
            LOG.error("Error loading stock items", e);
        }
    }
    
    private void updateStockCount() {
        if (lblStockCount != null && filteredStockItems != null) {
            int count = filteredStockItems.size();
            lblStockCount.setText(count + " items found");
        }
    }
    
    private void updateItemNameSuggestions() {
        if (itemSearchField != null && stockItems != null) {
            itemSearchField.setSuggestions(stockItems);
        }
    }
    
    @FXML
    private void handleAddToBill() {
        try {
            // Validate inputs
            if (txtItemCode == null || txtItemCode.getText().trim().isEmpty()) {
                alertNotification.showError("Please enter item code");
                return;
            }
            
            String itemCode = txtItemCode.getText().trim();
            String itemName = getSelectedItemName();
            if (itemName.isEmpty()) {
                itemName = "New Item";
            }
            Metal selectedMetal = cmbMetalType != null ? cmbMetalType.getValue() : null;
            String metalType = selectedMetal != null ? selectedMetal.getMetalName() : "GOLD";
            
            PurchaseTransaction itemToUpdate;
            
            // Check if we're in edit mode
            if (editingItem != null) {
                itemToUpdate = editingItem;
            } else {
                // Check if item with same code already exists
                itemToUpdate = purchaseItems.stream()
                    .filter(item -> item.getItemCode().equalsIgnoreCase(itemCode))
                    .findFirst()
                    .orElse(null);
                    
                if (itemToUpdate == null) {
                    itemToUpdate = new PurchaseTransaction();
                    purchaseItems.add(itemToUpdate);
                }
            }
            
            // Update values
            itemToUpdate.setItemCode(itemCode);
            itemToUpdate.setItemName(itemName);
            itemToUpdate.setMetalType(metalType);
            itemToUpdate.setPurity(parseBigDecimal(txtPurity));
            itemToUpdate.setQuantity(parseInt(txtQuantity, 1));
            itemToUpdate.setGrossWeight(parseBigDecimal(txtGrossWeight));
            itemToUpdate.setNetWeight(parseBigDecimal(txtNetWeight));
            
            // Get rate per 10 grams and convert to per gram
            BigDecimal ratePerTenGrams = parseBigDecimal(txtRate);
            if (ratePerTenGrams.compareTo(BigDecimal.ZERO) == 0) {
                ratePerTenGrams = getCurrentMetalRate(metalType, itemToUpdate.getPurity());
            }
            BigDecimal ratePerGram = ratePerTenGrams.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            itemToUpdate.setRatePerGram(ratePerGram);
            
            // Set making charges
            itemToUpdate.setMakingCharges(parseBigDecimal(txtLabourCharges));
            
            itemToUpdate.setItemType(ItemType.NEW_ITEM);
            itemToUpdate.calculateTotalAmount();
            
            // Refresh table
            purchaseItemsTable.refresh();
            updateSummary();
            clearPurchaseForm();
            
            // Reset edit mode
            editingItem = null;
            
            // Focus on the item
            purchaseItemsTable.scrollTo(itemToUpdate);
            purchaseItemsTable.getSelectionModel().select(itemToUpdate);
        } catch (Exception e) {
            LOG.error("Error adding/updating item", e);
            alertNotification.showError("Failed to add/update item: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAddExchangeItem() {
        try {
            // Validate inputs
            if (txtExchangeItemName == null || txtExchangeItemName.getText().trim().isEmpty()) {
                alertNotification.showError("Please enter item name");
                return;
            }
            
            String itemName = txtExchangeItemName.getText().trim();
            Metal selectedExchangeMetal = cmbExchangeMetalType != null ? cmbExchangeMetalType.getValue() : null;
            String metalType = selectedExchangeMetal != null ? selectedExchangeMetal.getMetalName() : "GOLD";
            BigDecimal purity = parseBigDecimal(txtExchangePurity);
            BigDecimal netWeight = parseBigDecimal(txtExchangeNetWeight);
            
            // Check available exchange metal stock
            // Use purity 0 to match how billing system stores exchange metal
            BigDecimal stockPurity = new BigDecimal("0");
            Optional<ExchangeMetalStock> stockOpt = metalStockService.getMetalStock(metalType, stockPurity);
            BigDecimal availableWeight = BigDecimal.ZERO;
            if (stockOpt.isPresent()) {
                availableWeight = stockOpt.get().getAvailableWeight();
            }
            
            // Calculate total weight already added for this metal type (ignore purity for stock calculation)
            BigDecimal alreadyAddedWeight = BigDecimal.ZERO;
            for (PurchaseExchangeTransaction existingItem : exchangeItems) {
                if (existingItem.getMetalType().equals(metalType) &&
                    existingItem != editingExchangeItem) { // Don't count the item being edited
                    alreadyAddedWeight = alreadyAddedWeight.add(existingItem.getNetWeight() != null ? 
                        existingItem.getNetWeight() : BigDecimal.ZERO);
                }
            }
            
            // Calculate remaining available weight
            BigDecimal remainingWeight = availableWeight.subtract(alreadyAddedWeight);
            
            // Check if sufficient stock is available
            if (remainingWeight.compareTo(netWeight) < 0) {
                String message = String.format(
                    "Insufficient exchange metal stock!\n\n" +
                    "Metal: %s %s\n" +
                    "Available Stock: %.3f grams\n" +
                    "Already Added: %.3f grams\n" +
                    "Remaining: %.3f grams\n" +
                    "Requested: %.3f grams\n\n" +
                    "Please reduce the quantity or add exchange metal stock first.",
                    metalType, purity, availableWeight, alreadyAddedWeight, 
                    remainingWeight, netWeight
                );
                alertNotification.showError(message);
                return;
            }
            
            PurchaseExchangeTransaction itemToUpdate;
            
            // Check if we're in edit mode
            if (editingExchangeItem != null) {
                itemToUpdate = editingExchangeItem;
            } else {
                // Check if item with same name already exists
                itemToUpdate = exchangeItems.stream()
                    .filter(item -> item.getItemName().equalsIgnoreCase(itemName))
                    .findFirst()
                    .orElse(null);
                    
                if (itemToUpdate == null) {
                    itemToUpdate = new PurchaseExchangeTransaction();
                    exchangeItems.add(itemToUpdate);
                }
            }
            
            // Update values
            itemToUpdate.setItemName(itemName);
            itemToUpdate.setMetalType(metalType);
            itemToUpdate.setPurity(parseBigDecimal(txtExchangePurity));
            itemToUpdate.setGrossWeight(parseBigDecimal(txtExchangeGrossWeight));
            itemToUpdate.setDeduction(parseBigDecimal(txtExchangeDeduction));
            itemToUpdate.setNetWeight(parseBigDecimal(txtExchangeNetWeight));
            
            // Get rate per 10 grams
            BigDecimal ratePerTenGrams = parseBigDecimal(txtExchangeRate);
            if (ratePerTenGrams.compareTo(BigDecimal.ZERO) == 0) {
                ratePerTenGrams = getCurrentMetalRate(metalType, itemToUpdate.getPurity());
            }
            itemToUpdate.setRatePerTenGrams(ratePerTenGrams);
            
            itemToUpdate.calculateNetWeightAndAmount();
            
            // Refresh table
            exchangeItemsTable.refresh();
            updateSummary();
            clearExchangeForm();
            
            // Reset edit mode
            editingExchangeItem = null;
            
            // Focus on the item
            exchangeItemsTable.scrollTo(itemToUpdate);
            exchangeItemsTable.getSelectionModel().select(itemToUpdate);
        } catch (Exception e) {
            LOG.error("Error adding/updating exchange item", e);
            alertNotification.showError("Failed to add/update exchange item: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClearForm() {
        clearPurchaseForm();
        editingItem = null;
    }
    
    @FXML
    private void handleEditPurchaseItem() {
        PurchaseTransaction selectedItem = purchaseItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            alertNotification.showError("Please select an item to edit");
            return;
        }
        
        // Set edit mode
        editingItem = selectedItem;
        
        // Populate form fields with selected item data
        if (txtItemCode != null) txtItemCode.setText(selectedItem.getItemCode());
        // Item name is set through the search field
        
        // Find and set the metal type in combo box
        if (cmbMetalType != null) {
            Metal metal = cmbMetalType.getItems().stream()
                .filter(m -> m.getMetalName().equals(selectedItem.getMetalType()))
                .findFirst()
                .orElse(null);
            cmbMetalType.setValue(metal);
        }
        
        if (txtPurity != null) txtPurity.setText(selectedItem.getPurity().toString());
        if (txtQuantity != null) txtQuantity.setText(selectedItem.getQuantity().toString());
        if (txtGrossWeight != null) txtGrossWeight.setText(selectedItem.getGrossWeight().toString());
        if (txtStoneWeight != null) {
            BigDecimal stoneWeight = selectedItem.getGrossWeight().subtract(selectedItem.getNetWeight());
            txtStoneWeight.setText(stoneWeight.toString());
        }
        if (txtNetWeight != null) txtNetWeight.setText(selectedItem.getNetWeight().toString());
        if (txtRate != null) {
            // Convert rate per gram to rate per 10 grams for display
            BigDecimal ratePerTenGrams = selectedItem.getRatePerGram().multiply(BigDecimal.TEN);
            txtRate.setText(ratePerTenGrams.toString());
        }
        if (txtLabourCharges != null) txtLabourCharges.setText(selectedItem.getMakingCharges().toString());
        if (txtAmount != null) txtAmount.setText(selectedItem.getTotalAmount().toString());
    }
    
    @FXML
    private void handleDeletePurchaseItem() {
        PurchaseTransaction selectedItem = purchaseItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            alertNotification.showError("Please select an item to delete");
            return;
        }
        
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Purchase Item");
        alert.setContentText("Are you sure you want to delete: " + selectedItem.getItemName() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            purchaseItems.remove(selectedItem);
            updateSummary();
            clearPurchaseForm();
            
            // Clear edit mode if deleting the item being edited
            if (editingItem == selectedItem) {
                editingItem = null;
            }
        }
    }
    
    @FXML
    private void handleUpdateItem() {
        if (editingItem != null) {
            // Update the item with new values
            editingItem.setItemCode(txtItemCode != null ? txtItemCode.getText() : "");
            editingItem.setItemName(getSelectedItemName());
            Metal selectedMetal = cmbMetalType != null ? cmbMetalType.getValue() : null;
            editingItem.setMetalType(selectedMetal != null ? selectedMetal.getMetalName() : "GOLD");
            editingItem.setPurity(parseBigDecimal(txtPurity));
            editingItem.setQuantity(parseInt(txtQuantity, 1));
            editingItem.setGrossWeight(parseBigDecimal(txtGrossWeight));
            editingItem.setNetWeight(parseBigDecimal(txtNetWeight));
            
            // Get rate per 10 grams and convert to per gram
            BigDecimal ratePerTenGrams = parseBigDecimal(txtRate);
            if (ratePerTenGrams.compareTo(BigDecimal.ZERO) == 0) {
                ratePerTenGrams = getCurrentMetalRate(editingItem.getMetalType(), editingItem.getPurity());
            }
            BigDecimal ratePerGram = ratePerTenGrams.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            editingItem.setRatePerGram(ratePerGram);
            editingItem.setMakingCharges(parseBigDecimal(txtLabourCharges));
            editingItem.calculateTotalAmount();
            
            // Refresh table
            purchaseItemsTable.refresh();
            updateSummary();
            
            // Clear form and reset buttons
            clearPurchaseForm();
            editingItem = null;
        }
    }
    
    @FXML
    private void handleAddNewItem() {
        try {
            // Load the JewelryItemForm dialog using SpringFXMLLoader
            Map.Entry<Parent, JewelryItemFormController> entry = springFXMLLoader.loadWithController(
                FxmlView.JEWELRY_ITEM_FORM.getFxmlFile(), 
                JewelryItemFormController.class
            );
            Parent root = entry.getKey();
            JewelryItemFormController controller = entry.getValue();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Item");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(btnAddNewItem.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            
            // Show dialog and wait for it to close
            dialogStage.showAndWait();
            
            // Refresh stock items and autocomplete
            loadStockItems();
            setupItemSearchField(); // Refresh the item search field
        } catch (Exception e) {
            LOG.error("Error opening jewelry item form", e);
            alertNotification.showError("Failed to open jewelry item form: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClearExchangeForm() {
        clearExchangeForm();
    }
    
    @FXML
    private void handleEditExchangeItem() {
        PurchaseExchangeTransaction selectedItem = exchangeItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            alertNotification.showError("Please select an item to edit");
            return;
        }
        
        // Set edit mode
        editingExchangeItem = selectedItem;
        
        // Populate form fields with selected item data
        if (txtExchangeItemName != null) txtExchangeItemName.setText(selectedItem.getItemName());
        
        // Find and set the metal type in combo box
        if (cmbExchangeMetalType != null) {
            Metal metal = cmbExchangeMetalType.getItems().stream()
                .filter(m -> m.getMetalName().equals(selectedItem.getMetalType()))
                .findFirst()
                .orElse(null);
            cmbExchangeMetalType.setValue(metal);
        }
        
        if (txtExchangePurity != null) txtExchangePurity.setText(selectedItem.getPurity().toString());
        if (txtExchangeGrossWeight != null) txtExchangeGrossWeight.setText(selectedItem.getGrossWeight().toString());
        if (txtExchangeDeduction != null) txtExchangeDeduction.setText(selectedItem.getDeduction().toString());
        if (txtExchangeNetWeight != null) txtExchangeNetWeight.setText(selectedItem.getNetWeight().toString());
        if (txtExchangeRate != null) txtExchangeRate.setText(selectedItem.getRatePerTenGrams().toString());
        if (txtExchangeAmount != null) txtExchangeAmount.setText(selectedItem.getTotalAmount().toString());
    }
    
    @FXML
    private void handleDeleteExchangeItem() {
        PurchaseExchangeTransaction selectedItem = exchangeItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            alertNotification.showError("Please select an item to delete");
            return;
        }
        
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Exchange Item");
        alert.setContentText("Are you sure you want to delete: " + selectedItem.getItemName() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            exchangeItems.remove(selectedItem);
            updateSummary();
            clearExchangeForm();
            
            // Clear edit mode if deleting the item being edited
            if (editingExchangeItem == selectedItem) {
                editingExchangeItem = null;
            }
        }
    }
    
    private void updateMetalRate(Metal metal) {
        if (metal != null && txtRate != null) {
            try {
                // Get the purity value from the purity text field
                BigDecimal purity = parseBigDecimal(txtPurity);
                if (purity.compareTo(BigDecimal.ZERO) == 0) {
                    // Default purity based on metal type
                    purity = metal.getMetalName().toUpperCase().contains("GOLD") ? new BigDecimal("916") : new BigDecimal("925");
                    txtPurity.setText(purity.toString());
                }
                
                // Get the current rate for this metal
                BigDecimal rate = getCurrentMetalRate(metal.getMetalName(), purity);
                txtRate.setText(rate.toString());
                
                // Recalculate total if all fields are filled
                calculateItemTotal();
            } catch (Exception e) {
                LOG.error("Error updating metal rate", e);
            }
        }
    }
    
    private BigDecimal getCurrentMetalRate(String metalType, BigDecimal purity) {
        try {
            // Find the metal by type and purity
            String metalName = metalType + " " + purity.intValue() + "K";
            Optional<Metal> metalOpt = metalService.getMetalByName(metalName);
            
            if (metalOpt.isEmpty()) {
                // Try to find by metal type and purity separately
                List<Metal> metals = metalService.getMetalsByType(metalType);
                metalOpt = metals.stream()
                    .filter(m -> m.getPurity().equals(purity.intValue() + "K"))
                    .findFirst();
            }
            
            if (metalOpt.isPresent()) {
                // Get the latest rate for this metal
                Optional<MetalRate> rateOpt = metalRateService.getLatestMetalRate(
                    metalOpt.get().getId(), 
                    LocalDate.now()
                );
                
                if (rateOpt.isPresent()) {
                    return rateOpt.get().getRatePerTenGrams();
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting metal rate for {} {}", metalType, purity, e);
        }
        return BigDecimal.ZERO;
    }
    
    @FXML
    private void handleRefreshStock() {
        loadMetalStock();
    }
    
    @FXML
    private void handleClear() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Invoice");
        confirm.setHeaderText("Clear Purchase Invoice");
        confirm.setContentText("Are you sure you want to clear all items?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                clearForm();
            }
        });
    }
    
    @FXML
    private void handleSave() {
        LOG.info("handleSave() called - isEditMode: {}, invoiceToEdit: {}", isEditMode, invoiceToEdit != null ? invoiceToEdit.getId() : "null");
        // Check if we're in edit mode and delegate to the appropriate method
        if (isEditMode && invoiceToEdit != null) {
            LOG.info("Delegating to handleSaveInvoice() for edit mode");
            handleSaveInvoice();
            return;
        }
        
        LOG.info("Processing as new invoice in handleSave()");
        
        try {
            if (!validateForm()) {
                return;
            }
            
            Supplier supplier = cmbSupplier != null ? cmbSupplier.getValue() : null;
            PurchaseType purchaseType = PurchaseType.NEW_STOCK;
            
            // Determine purchase type based on items
            if (!exchangeItems.isEmpty() && purchaseItems.isEmpty()) {
                purchaseType = PurchaseType.EXCHANGE_ITEMS;
            } else if (!exchangeItems.isEmpty() && !purchaseItems.isEmpty()) {
                purchaseType = PurchaseType.NEW_STOCK; // Mixed type
            }
            
            // Combine all transactions
            List<PurchaseTransaction> allTransactions = new ArrayList<>(purchaseItems);
            
            // Convert exchange items to purchase transactions
            for (PurchaseExchangeTransaction exItem : exchangeItems) {
                PurchaseTransaction pTrans = new PurchaseTransaction();
                pTrans.setItemCode("EX-" + System.currentTimeMillis());
                pTrans.setItemName(exItem.getItemName());
                pTrans.setMetalType(exItem.getMetalType());
                pTrans.setPurity(exItem.getPurity());
                pTrans.setGrossWeight(exItem.getGrossWeight());
                pTrans.setNetWeight(exItem.getNetWeight());
                // Convert rate per 10 grams to rate per gram for PurchaseTransaction
                BigDecimal ratePerGram = exItem.getRatePerTenGrams() != null ? 
                    exItem.getRatePerTenGrams().divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                pTrans.setRatePerGram(ratePerGram);
                pTrans.setQuantity(1);
                pTrans.setItemType(ItemType.EXCHANGE_ITEM);
                pTrans.setTotalAmount(exItem.getTotalAmount());
                allTransactions.add(pTrans);
            }
            
            // Determine payment method
            PurchaseInvoice.PaymentMethod paymentMethod;
            boolean isCreditPurchase = chkCreditPurchase != null && chkCreditPurchase.isSelected();
            
            if (isCreditPurchase) {
                paymentMethod = PurchaseInvoice.PaymentMethod.CREDIT;
            } else {
                // Check if partial payment
                BigDecimal paidAmount = parseBigDecimal(txtPaidAmount);
                BigDecimal grandTotal = calculateGrandTotal();
                if (paidAmount.compareTo(grandTotal) < 0 && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                    paymentMethod = PurchaseInvoice.PaymentMethod.PARTIAL;
                } else {
                    paymentMethod = PurchaseInvoice.PaymentMethod.BANK_TRANSFER;
                }
            }
            
            // Create invoice
            PurchaseInvoice invoice = purchaseInvoiceService.createPurchaseInvoice(
                supplier,
                allTransactions,
                txtSupplierInvoice != null ? txtSupplierInvoice.getText().trim() : "",
                purchaseType,
                parseBigDecimal(txtDiscount),
                gstRate,
                BigDecimal.ZERO,  // transport charges
                BigDecimal.ZERO,  // other charges
                paymentMethod,
                txtPaymentReference != null ? txtPaymentReference.getText().trim() : "",
                null  // notes
            );
            
            // Set paid and pending amounts
            BigDecimal paidAmount = parseBigDecimal(txtPaidAmount);
            invoice.setPaidAmount(paidAmount);
            invoice.setPendingAmount(invoice.getGrandTotal().subtract(paidAmount));
            
            // Determine invoice status based on payment
            if (isCreditPurchase) {
                invoice.setStatus(PurchaseInvoice.InvoiceStatus.CONFIRMED);
            } else if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatus(PurchaseInvoice.InvoiceStatus.CONFIRMED);
            } else if (paidAmount.compareTo(invoice.getGrandTotal()) >= 0) {
                invoice.setStatus(PurchaseInvoice.InvoiceStatus.PAID);
            } else {
                invoice.setStatus(PurchaseInvoice.InvoiceStatus.PARTIAL);
            }
            
            // Save the invoice with updated status and amounts
            invoice = purchaseInvoiceService.savePurchaseInvoice(invoice);
            
            // Process payment if needed (not for credit purchases)
            if (!isCreditPurchase && invoice.getGrandTotal().compareTo(BigDecimal.ZERO) > 0 && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                processPayment(invoice);
            }
            
            alertNotification.showSuccess("Purchase invoice saved successfully!");
            clearForm();
            
        } catch (Exception e) {
            LOG.error("Error saving invoice", e);
            alertNotification.showError("Failed to save invoice: " + e.getMessage());
        }
    }
    
    private boolean validateForm() {
        Supplier selectedSupplier = cmbSupplier != null ? cmbSupplier.getValue() : null;
        if (selectedSupplier == null) {
            alertNotification.showError("Please select a supplier");
            return false;
        }
        
        if (purchaseItems.isEmpty() && exchangeItems.isEmpty()) {
            alertNotification.showError("Please add at least one purchase or exchange item");
            return false;
        }
        
        // Validate items
        for (PurchaseTransaction item : purchaseItems) {
            if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
                alertNotification.showError("Item name cannot be empty");
                return false;
            }
            if (item.getNetWeight() == null || item.getNetWeight().compareTo(BigDecimal.ZERO) <= 0) {
                alertNotification.showError("Net weight must be greater than zero");
                return false;
            }
        }
        
        return true;
    }
    
    private void processPayment(PurchaseInvoice invoice) {
        try {
            // Get paid amount
            BigDecimal paidAmount = parseBigDecimal(txtPaidAmount);
            if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return; // No payment to process
            }
            
            // Get selected bank account
            BankAccount selectedBankAccount = cmbBankAccount.getValue();
            if (selectedBankAccount != null) {
                // Check if sufficient balance
                if (selectedBankAccount.getCurrentBalance().compareTo(paidAmount) >= 0) {
                    // Create bank transaction
                    bankTransactionService.recordDebit(
                        selectedBankAccount,
                        paidAmount,
                        BankTransaction.TransactionSource.PURCHASE_PAYMENT,
                        "PURCHASE",
                        invoice.getId(),
                        invoice.getInvoiceNumber(),
                        txtPaymentReference.getText().trim(),
                        invoice.getSupplier().getSupplierName(),
                        String.format("Payment for purchase invoice %s to %s (Paid: %s)", 
                            invoice.getInvoiceNumber(), 
                            invoice.getSupplier().getSupplierName(),
                            CurrencyFormatter.format(paidAmount))
                    );
                    
                    // Reload bank accounts to refresh balances
                    loadBankAccounts();
                } else {
                    LOG.warn("Insufficient balance for payment of invoice " + invoice.getInvoiceNumber());
                }
            }
        } catch (Exception e) {
            LOG.error("Error processing payment for invoice " + invoice.getInvoiceNumber(), e);
        }
    }
    
    private void updateSummary() {
        Platform.runLater(() -> {
            // Purchase items summary
            int totalPurchaseItems = purchaseItems.size();
            BigDecimal totalPurchaseWeight = purchaseItems.stream()
                .map(PurchaseTransaction::getNetWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal purchaseTotal = purchaseItems.stream()
                .map(PurchaseTransaction::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Exchange items summary
            int totalExchangeItems = exchangeItems.size();
            BigDecimal totalExchangeWeight = exchangeItems.stream()
                .map(PurchaseExchangeTransaction::getNetWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal exchangeTotal = exchangeItems.stream()
                .map(PurchaseExchangeTransaction::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate totals
            // Step 1: Purchase Total (subtotal)
            BigDecimal subtotal = purchaseTotal;
            
            // Step 2: Add GST on purchase total (before discount)
            BigDecimal gstAmount = subtotal.multiply(gstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal totalWithGst = subtotal.add(gstAmount);
            
            // Step 3: Apply discount
            BigDecimal discount = parseBigDecimal(txtDiscount);
            BigDecimal totalAfterDiscount = totalWithGst.subtract(discount);
            
            // Step 4: Subtract exchange amount
            BigDecimal grandTotal = totalAfterDiscount.subtract(exchangeTotal);
            
            // Update labels - Purchase section
            if (lblTotalPurchaseItems != null) lblTotalPurchaseItems.setText("(" + totalPurchaseItems + " items)");
            if (lblPurchaseAmount != null) lblPurchaseAmount.setText(CurrencyFormatter.format(purchaseTotal));
            
            // Update labels - Exchange section
            if (lblTotalExchangeItems != null) lblTotalExchangeItems.setText("(" + totalExchangeItems + " items)");
            if (lblExchangeAmount != null) lblExchangeAmount.setText(CurrencyFormatter.format(exchangeTotal));
            if (lblExchangeDeduction != null) lblExchangeDeduction.setText("-" + CurrencyFormatter.format(exchangeTotal));
            
            // Update labels - Bill calculation
            if (lblSubtotal != null) lblSubtotal.setText(CurrencyFormatter.format(subtotal));
            if (lblGST != null) lblGST.setText(CurrencyFormatter.format(gstAmount));
            if (lblNetTotal != null) lblNetTotal.setText(CurrencyFormatter.format(totalWithGst));
            if (lblTotalAmount != null) lblTotalAmount.setText(CurrencyFormatter.format(totalAfterDiscount));
            if (lblGrandTotal != null) lblGrandTotal.setText(CurrencyFormatter.format(grandTotal));
            
            updatePendingAmount();
        });
    }
    
    private void clearForm() {
        purchaseItems.clear();
        exchangeItems.clear();
        if (cmbSupplier != null) cmbSupplier.setValue(null);
        if (txtSupplierName != null) txtSupplierName.clear();
        if (txtGSTNumber != null) txtGSTNumber.clear();
        if (txtSupplierContact != null) txtSupplierContact.clear();
        if (txtSupplierInvoice != null) txtSupplierInvoice.clear();
        if (txtPaymentReference != null) txtPaymentReference.clear();
        if (txtPaidAmount != null) txtPaidAmount.clear();
        if (txtDiscount != null) txtDiscount.setText("0");
        if (chkCreditPurchase != null) chkCreditPurchase.setSelected(false);
        if (!cmbBankAccount.getItems().isEmpty()) {
            cmbBankAccount.setValue(cmbBankAccount.getItems().get(0));
        }
        clearPurchaseForm();
        clearExchangeForm();
        updateDateTime();
        updateSummary();
        updateSupplierInfo(null);
    }
    
    private void clearPurchaseForm() {
        // Reset edit mode
        editingItem = null;
        
        if (txtItemCode != null) txtItemCode.clear();
        if (itemSearchField != null) itemSearchField.clear();
        if (cmbMetalType != null) {
            // Set to first metal (usually GOLD) or find GOLD metal
            if (!cmbMetalType.getItems().isEmpty()) {
                Metal goldMetal = cmbMetalType.getItems().stream()
                    .filter(m -> m.getMetalName().toUpperCase().contains("GOLD"))
                    .findFirst()
                    .orElse(cmbMetalType.getItems().get(0));
                cmbMetalType.setValue(goldMetal);
            }
        }
        if (txtPurity != null) txtPurity.setText("916");
        if (txtQuantity != null) txtQuantity.setText("1");
        if (txtGrossWeight != null) txtGrossWeight.clear();
        if (txtStoneWeight != null) txtStoneWeight.setText("0");
        if (txtNetWeight != null) txtNetWeight.clear();
        if (txtRate != null) txtRate.clear();
        if (txtLabourCharges != null) txtLabourCharges.clear();
        if (txtAmount != null) txtAmount.clear();
    }
    
    private void clearExchangeForm() {
        // Reset edit mode
        editingExchangeItem = null;
        
        if (txtExchangeItemName != null) txtExchangeItemName.clear();
        if (cmbExchangeMetalType != null) {
            // Set to first metal (usually GOLD) or find GOLD metal
            if (!cmbExchangeMetalType.getItems().isEmpty()) {
                Metal goldMetal = cmbExchangeMetalType.getItems().stream()
                    .filter(m -> m.getMetalName().toUpperCase().contains("GOLD"))
                    .findFirst()
                    .orElse(cmbExchangeMetalType.getItems().get(0));
                cmbExchangeMetalType.setValue(goldMetal);
            }
        }
        if (txtExchangePurity != null) txtExchangePurity.setText("916");
        if (txtExchangeGrossWeight != null) txtExchangeGrossWeight.clear();
        if (txtExchangeDeduction != null) txtExchangeDeduction.clear();
        if (txtExchangeNetWeight != null) txtExchangeNetWeight.clear();
        if (txtExchangeRate != null) txtExchangeRate.clear();
        if (txtExchangeAmount != null) txtExchangeAmount.clear();
    }
    
    private void calculateNetWeight() {
        try {
            BigDecimal grossWeight = new BigDecimal(txtGrossWeight.getText().isEmpty() ? "0" : txtGrossWeight.getText());
            BigDecimal stoneWeight = new BigDecimal(txtStoneWeight.getText().isEmpty() ? "0" : txtStoneWeight.getText());
            BigDecimal netWeight = grossWeight.subtract(stoneWeight);
            
            if (netWeight.compareTo(BigDecimal.ZERO) < 0) {
                netWeight = BigDecimal.ZERO;
            }
            
            txtNetWeight.setText(netWeight.toPlainString());
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }
    }
    
    private void calculateItemTotal() {
        try {
            int quantity = parseInt(txtQuantity, 1);
            BigDecimal weight = parseBigDecimal(txtNetWeight);
            BigDecimal ratePerTenGrams = parseBigDecimal(txtRate);
            BigDecimal labourPercentage = parseBigDecimal(txtLabourCharges);
            
            // Calculate gold value
            BigDecimal goldValue = weight.multiply(ratePerTenGrams).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            
            // Calculate labour charges as percentage of gold value
            BigDecimal labourAmount = goldValue.multiply(labourPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            
            // Total = (gold value + labour charges) * quantity
            BigDecimal total = goldValue.add(labourAmount).multiply(new BigDecimal(quantity));
            
            if (txtAmount != null) {
                txtAmount.setText(CurrencyFormatter.format(total));
            }
        } catch (Exception e) {
            // Ignore calculation errors
        }
    }
    
    private void calculateExchangeNetWeight() {
        try {
            BigDecimal grossWeight = parseBigDecimal(txtExchangeGrossWeight);
            BigDecimal deduction = parseBigDecimal(txtExchangeDeduction);
            BigDecimal netWeight = grossWeight.subtract(deduction);
            
            if (txtExchangeNetWeight != null) {
                txtExchangeNetWeight.setText(WeightFormatter.format(netWeight));
            }
        } catch (Exception e) {
            // Ignore calculation errors
        }
    }
    
    private void calculateExchangeTotal() {
        try {
            BigDecimal netWeight = parseBigDecimal(txtExchangeNetWeight);
            BigDecimal ratePerTenGrams = parseBigDecimal(txtExchangeRate);
            
            // Calculate value
            BigDecimal total = netWeight.multiply(ratePerTenGrams).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            
            if (txtExchangeAmount != null) {
                txtExchangeAmount.setText(CurrencyFormatter.format(total));
            }
        } catch (Exception e) {
            // Ignore calculation errors
        }
    }
    
    private void updatePendingAmount() {
        try {
            BigDecimal grandTotal = parseBigDecimal(lblGrandTotal != null ? lblGrandTotal.getText().replace("₹", "").replace(",", "").trim() : "0");
            BigDecimal paidAmount = parseBigDecimal(txtPaidAmount);
            BigDecimal pending = grandTotal.subtract(paidAmount);
            
            if (lblPendingAmount != null) {
                lblPendingAmount.setText(CurrencyFormatter.format(pending));
                if (pending.compareTo(BigDecimal.ZERO) > 0) {
                    lblPendingAmount.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                } else {
                    lblPendingAmount.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                }
            }
        } catch (Exception e) {
            // Ignore calculation errors
        }
    }
    
    private void populatePurchaseItemFields(JewelryItem item) {
        // Set flag to prevent autocomplete popup
        isProgrammaticallySettingFields = true;
        
        if (txtItemCode != null) txtItemCode.setText(item.getItemCode());
        // Set the item in the search field
        if (itemSearchField != null) {
            itemSearchField.setSelectedItem(item);
        }
        if (cmbMetalType != null) {
            // Find the Metal object that matches the item's metal type string
            for (Metal metal : cmbMetalType.getItems()) {
                if (metal.getMetalName().equalsIgnoreCase(item.getMetalType())) {
                    cmbMetalType.setValue(metal);
                    break;
                }
            }
        }
        if (txtPurity != null) txtPurity.setText(item.getPurity().toString());
        
        // Get current metal rate
        BigDecimal rate = getCurrentMetalRate(item.getMetalType(), item.getPurity());
        if (txtRate != null) {
            txtRate.setText(rate.toString());
        }
        
        // Set quantity from item
        if (txtQuantity != null) {
            txtQuantity.setText(item.getQuantity() != null ? item.getQuantity().toString() : "1");
        }
        
        // Set weight fields from item
        if (txtGrossWeight != null && item.getGrossWeight() != null) {
            txtGrossWeight.setText(item.getGrossWeight().toPlainString());
        }
        if (txtStoneWeight != null && item.getStoneWeight() != null) {
            txtStoneWeight.setText(item.getStoneWeight().toPlainString());
        } else if (txtStoneWeight != null) {
            txtStoneWeight.setText("0");
        }
        if (txtNetWeight != null && item.getNetWeight() != null) {
            txtNetWeight.setText(item.getNetWeight().toPlainString());
        }
        
        // If net weight is not set but gross and stone weights are available, calculate it
        if (txtNetWeight != null && (txtNetWeight.getText() == null || txtNetWeight.getText().isEmpty())) {
            calculateNetWeight();
        }
        
        // Set labour charges if available
        if (txtLabourCharges != null && item.getLabourCharges() != null) {
            txtLabourCharges.setText(item.getLabourCharges().toString());
        }
        
        // Store current labour percentage for calculations
        currentLabourPercentage = item.getLabourCharges();
        
        // Reset flag after all fields are set
        isProgrammaticallySettingFields = false;
        
        // Calculate total amount with populated values
        calculateItemTotal();
        
        // Move focus to quantity field to ensure autocomplete popup is hidden
        if (txtQuantity != null) {
            txtQuantity.requestFocus();
        }
    }
    
    private void populateFormForEdit(PurchaseTransaction item) {
        isProgrammaticallySettingFields = true;
        
        // Store reference to editing item
        editingItem = item;
        
        // Populate all fields with item data
        if (txtItemCode != null) txtItemCode.setText(item.getItemCode());
        // Item is already selected in the search field
        if (cmbMetalType != null) {
            // Find the Metal object that matches the item's metal type string
            for (Metal metal : cmbMetalType.getItems()) {
                if (metal.getMetalName().equalsIgnoreCase(item.getMetalType())) {
                    cmbMetalType.setValue(metal);
                    break;
                }
            }
        }
        if (txtPurity != null) txtPurity.setText(item.getPurity().toString());
        if (txtQuantity != null) txtQuantity.setText(item.getQuantity().toString());
        if (txtGrossWeight != null) txtGrossWeight.setText(item.getGrossWeight().toPlainString());
        // Calculate stone weight as difference between gross and net weight
        if (txtStoneWeight != null) {
            BigDecimal stoneWeight = item.getGrossWeight().subtract(item.getNetWeight());
            txtStoneWeight.setText(stoneWeight.toPlainString());
        }
        if (txtNetWeight != null) txtNetWeight.setText(item.getNetWeight().toPlainString());
        
        // Convert rate per gram to rate per 10 grams for display
        if (txtRate != null && item.getRatePerGram() != null) {
            BigDecimal ratePerTenGrams = item.getRatePerGram().multiply(BigDecimal.TEN);
            txtRate.setText(ratePerTenGrams.toPlainString());
        }
        
        if (txtLabourCharges != null) txtLabourCharges.setText(item.getMakingCharges().toString());
        
        // Set editing mode
        editingItem = item;
        
        isProgrammaticallySettingFields = false;
        
        // Calculate total to show amount
        calculateItemTotal();
    }
    
    private void populateExchangeItemFields(JewelryItem item) {
        if (txtExchangeItemName != null) txtExchangeItemName.setText(item.getItemName());
        if (cmbExchangeMetalType != null) {
            // Find the Metal object that matches the item's metal type string
            for (Metal metal : cmbExchangeMetalType.getItems()) {
                if (metal.getMetalName().equalsIgnoreCase(item.getMetalType())) {
                    cmbExchangeMetalType.setValue(metal);
                    break;
                }
            }
        }
        if (txtExchangePurity != null) txtExchangePurity.setText(item.getPurity().toString());
        
        // Get current metal rate
        BigDecimal rate = getCurrentMetalRate(item.getMetalType(), item.getPurity());
        if (txtExchangeRate != null) {
            txtExchangeRate.setText(rate.toString());
        }
    }
    
    private BigDecimal parseBigDecimal(TextField field) {
        if (field == null) return BigDecimal.ZERO;
        return parseBigDecimal(field.getText());
    }
    
    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            // Remove currency symbols and commas
            text = text.replace("₹", "").replace(",", "").trim();
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    private int parseInt(TextField field, int defaultValue) {
        if (field == null || field.getText().trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private void showPurchasePanel() {
        if (purchasePanel != null) {
            purchasePanel.setVisible(true);
            purchasePanel.setManaged(true);
        }
        if (exchangePanel != null) {
            exchangePanel.setVisible(false);
            exchangePanel.setManaged(false);
        }
        updateChipStyles();
    }
    
    private void showExchangePanel() {
        if (purchasePanel != null) {
            purchasePanel.setVisible(false);
            purchasePanel.setManaged(false);
        }
        if (exchangePanel != null) {
            exchangePanel.setVisible(true);
            exchangePanel.setManaged(true);
        }
        updateChipStyles();
    }
    
    private void setupChipStyles() {
        String activeStyle = "-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20 8 20; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-font-size: 12; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(156,39,176,0.3), 4, 0, 0, 2);";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #757575; -fx-background-radius: 20; -fx-padding: 8 20 8 20; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-font-size: 12; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: rgba(156,39,176,0.1); -fx-text-fill: #9C27B0; -fx-background-radius: 20; -fx-padding: 8 20 8 20; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-font-size: 12; -fx-cursor: hand;";
        
        if (chipPurchase != null) {
            chipPurchase.setOnMouseEntered(e -> {
                if (!chipPurchase.isSelected()) {
                    chipPurchase.setStyle(hoverStyle);
                }
            });
            chipPurchase.setOnMouseExited(e -> {
                if (!chipPurchase.isSelected()) {
                    chipPurchase.setStyle(inactiveStyle);
                }
            });
        }
        
        if (chipExchange != null) {
            chipExchange.setOnMouseEntered(e -> {
                if (!chipExchange.isSelected()) {
                    chipExchange.setStyle(hoverStyle);
                }
            });
            chipExchange.setOnMouseExited(e -> {
                if (!chipExchange.isSelected()) {
                    chipExchange.setStyle(inactiveStyle);
                }
            });
        }
    }
    
    private void updateChipStyles() {
        String activeStyle = "-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20 8 20; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-font-size: 12; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(156,39,176,0.3), 4, 0, 0, 2);";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #757575; -fx-background-radius: 20; -fx-padding: 8 20 8 20; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-font-size: 12; -fx-cursor: hand;";
        
        if (chipPurchase != null) {
            chipPurchase.setStyle(chipPurchase.isSelected() ? activeStyle : inactiveStyle);
            // Update icon color
            if (chipPurchase.getGraphic() instanceof FontAwesomeIcon) {
                FontAwesomeIcon icon = (FontAwesomeIcon) chipPurchase.getGraphic();
                icon.setFill(chipPurchase.isSelected() ? Color.WHITE : Color.valueOf("#757575"));
            }
        }
        
        if (chipExchange != null) {
            chipExchange.setStyle(chipExchange.isSelected() ? activeStyle : inactiveStyle);
            // Update icon color
            if (chipExchange.getGraphic() instanceof FontAwesomeIcon) {
                FontAwesomeIcon icon = (FontAwesomeIcon) chipExchange.getGraphic();
                icon.setFill(chipExchange.isSelected() ? Color.WHITE : Color.valueOf("#757575"));
            }
        }
    }
    
    @FXML
    private void handleSaveInvoice() {
        LOG.info("handleSaveInvoice() called - isEditMode: {}, invoiceToEdit: {}", isEditMode, invoiceToEdit != null ? invoiceToEdit.getId() : "null");
        // Validate required fields
        Supplier selectedSupplier = cmbSupplier.getValue();
        if (selectedSupplier == null) {
            alertNotification.showError("Please select a supplier");
            return;
        }
        
        if (purchaseItems.isEmpty() && exchangeItems.isEmpty()) {
            alertNotification.showError("Please add at least one item or exchange item");
            return;
        }
        
        // Check if credit purchase
        boolean isCreditPurchase = chkCreditPurchase != null && chkCreditPurchase.isSelected();
        
        // Validate bank account selection (not required for credit purchases)
        BankAccount selectedBankAccount = cmbBankAccount.getValue();
        if (!isCreditPurchase && selectedBankAccount == null) {
            alertNotification.showError("Please select a bank account");
            return;
        }
        
        try {
            // Get payment details
            String paymentReference = txtPaymentReference.getText();
            
            // Get paid amount
            BigDecimal paidAmount = BigDecimal.ZERO;
            if (!txtPaidAmount.getText().trim().isEmpty()) {
                try {
                    paidAmount = new BigDecimal(txtPaidAmount.getText().trim());
                } catch (NumberFormatException e) {
                    alertNotification.showError("Invalid paid amount");
                    return;
                }
            }
            
            // Get discount
            BigDecimal discount = BigDecimal.ZERO;
            if (!txtDiscount.getText().trim().isEmpty()) {
                try {
                    discount = new BigDecimal(txtDiscount.getText().trim());
                } catch (NumberFormatException e) {
                    alertNotification.showError("Invalid discount amount");
                    return;
                }
            }
            
            // Check if bank account has sufficient balance (skip for credit purchases)
            BigDecimal grandTotal = calculateGrandTotal();
            if (!isCreditPurchase && selectedBankAccount != null && selectedBankAccount.getCurrentBalance().compareTo(grandTotal) < 0) {
                alertNotification.showError(String.format("Insufficient balance in %s. Available: ₹%.2f, Required: ₹%.2f", 
                    selectedBankAccount.getBankName(), 
                    selectedBankAccount.getCurrentBalance(), 
                    grandTotal));
                return;
            }
            
            // Determine payment method and status based on credit purchase
            PurchaseInvoice.PaymentMethod paymentMethod;
            PurchaseInvoice.InvoiceStatus invoiceStatus;
            BigDecimal paidAmountFinal = BigDecimal.ZERO;
            
            if (isCreditPurchase) {
                paymentMethod = PurchaseInvoice.PaymentMethod.CREDIT;
                invoiceStatus = PurchaseInvoice.InvoiceStatus.CONFIRMED;
                paidAmountFinal = BigDecimal.ZERO;
            } else {
                paymentMethod = PurchaseInvoice.PaymentMethod.BANK_TRANSFER;
                invoiceStatus = PurchaseInvoice.InvoiceStatus.PAID;
                paidAmountFinal = grandTotal;
            }
            
            // Create or update purchase invoice entity
            PurchaseInvoice invoice;
            
            if (isEditMode && invoiceToEdit != null) {
                // For edit mode, use the dedicated update method
                // Create updated invoice data object
                invoice = PurchaseInvoice.builder()
                    .supplier(selectedSupplier)
                    .supplierInvoiceNumber(txtSupplierInvoice.getText().trim())
                    .purchaseType(chipExchange.isSelected() ? PurchaseType.EXCHANGE_ITEMS : PurchaseType.NEW_STOCK)
                    .paymentMethod(paymentMethod)
                    .paymentReference(paymentReference)
                    .status(invoiceStatus)
                    .discount(discount)
                    .gstRate(new BigDecimal(txtGstRate.getText()))
                    .transportCharges(BigDecimal.ZERO)
                    .otherCharges(BigDecimal.ZERO)
                    .paidAmount(paidAmountFinal)
                    .pendingAmount(isCreditPurchase ? grandTotal : BigDecimal.ZERO)
                    .build();
            } else {
                // Create new invoice
                invoice = PurchaseInvoice.builder()
                    .supplier(selectedSupplier)
                    .supplierInvoiceNumber(txtSupplierInvoice.getText().trim())
                    .purchaseType(chipExchange.isSelected() ? PurchaseType.EXCHANGE_ITEMS : PurchaseType.NEW_STOCK)
                    .invoiceDate(LocalDateTime.now())
                    .paymentMethod(paymentMethod)
                    .paymentReference(paymentReference)
                    .status(invoiceStatus)
                    .discount(discount)
                    .gstRate(new BigDecimal(txtGstRate.getText()))
                    .transportCharges(BigDecimal.ZERO)
                    .otherCharges(BigDecimal.ZERO)
                    .paidAmount(paidAmountFinal)
                    .pendingAmount(isCreditPurchase ? grandTotal : BigDecimal.ZERO)
                    .purchaseTransactions(new ArrayList<>(purchaseItems))
                    .purchaseExchangeTransactions(new ArrayList<>(exchangeItems))
                    .build();
            }
            
            // Save invoice
            PurchaseInvoice savedInvoice;
            if (isEditMode) {
                LOG.info("Using UPDATE mode for invoice ID: {}", invoiceToEdit != null ? invoiceToEdit.getId() : "null");
                // For edit mode, use the dedicated update method
                savedInvoice = purchaseInvoiceService.updatePurchaseInvoice(
                    invoiceToEdit.getId(),
                    new ArrayList<>(purchaseItems),
                    new ArrayList<>(exchangeItems),
                    invoice
                );
            } else {
                LOG.info("Using NEW SAVE mode for purchase invoice");
                // For new invoices, use save with stock updates
                savedInvoice = purchaseInvoiceService.savePurchaseInvoiceWithStockUpdate(invoice);
            }
            
            // Create bank transaction for the payment (only if not credit purchase and not in edit mode)
            if (!isCreditPurchase && selectedBankAccount != null && !isEditMode) {
                bankTransactionService.recordDebit(
                    selectedBankAccount,
                    grandTotal,
                    BankTransaction.TransactionSource.PURCHASE_PAYMENT,
                    "PURCHASE",
                    savedInvoice.getId(),
                    savedInvoice.getInvoiceNumber(),
                    paymentReference,
                    selectedSupplier.getSupplierName(),
                    String.format("Payment for purchase invoice %s to %s", 
                        savedInvoice.getInvoiceNumber(), 
                        selectedSupplier.getSupplierName())
                );
            }
            
            // Show success message first (before any other operations that might fail)
            if (isEditMode) {
                alertNotification.showSuccess("Purchase invoice " + savedInvoice.getInvoiceNumber() + " updated successfully!");
            } else {
                if (isCreditPurchase) {
                    alertNotification.showSuccess("Purchase invoice saved as credit. Payment pending: " + CurrencyFormatter.format(grandTotal));
                } else {
                    alertNotification.showSuccess("Purchase invoice saved and payment recorded successfully!");
                }
            }
            
            // Perform post-save operations (these can fail without affecting the success message)
            try {
                if (isEditMode) {
                    // Close dialog if in edit mode
                    if (dialogStage != null) {
                        dialogStage.close();
                    }
                } else {
                    // Clear the form for new invoice
                    clearAll();
                    
                    // Update date time display
                    updateDateTime();
                }
                
                // Reload bank accounts to refresh balances
                loadBankAccounts();
                
            } catch (Exception postSaveException) {
                LOG.warn("Error in post-save operations (success message already shown): " + postSaveException.getMessage(), postSaveException);
            }
            
        } catch (Exception e) {
            LOG.error("Error saving purchase invoice", e);
            alertNotification.showError("Error saving invoice: " + e.getMessage());
        }
    }
    
    private BigDecimal calculateGrandTotal() {
        // Calculate purchase total
        BigDecimal purchaseTotal = purchaseItems.stream()
            .map(PurchaseTransaction::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate exchange total
        BigDecimal exchangeTotal = exchangeItems.stream()
            .map(PurchaseExchangeTransaction::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Get GST rate
        BigDecimal gstRate = parseBigDecimal(txtGstRate);
        
        // Calculate GST on purchase amount
        BigDecimal gstAmount = purchaseTotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        // Calculate subtotal
        BigDecimal subtotal = purchaseTotal.add(gstAmount);
        
        // Get discount
        BigDecimal discount = parseBigDecimal(txtDiscount);
        
        // Calculate grand total
        BigDecimal grandTotal = subtotal.subtract(exchangeTotal).subtract(discount);
        
        // Ensure non-negative
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }
        
        return grandTotal;
    }
    
    private void clearAll() {
        // Clear supplier info
        if (cmbSupplier != null) {
            cmbSupplier.setValue(null);
        }
        txtSupplierName.clear();
        txtGSTNumber.clear();
        txtSupplierContact.clear();
        txtSupplierInvoice.clear();
        
        // Clear purchase form
        txtItemCode.clear();
        if (itemSearchField != null) itemSearchField.clear();
        cmbMetalType.setValue(null);
        txtPurity.clear();
        txtQuantity.clear();
        txtGrossWeight.clear();
        txtStoneWeight.clear();
        txtNetWeight.clear();
        txtRate.clear();
        txtLabourCharges.clear();
        txtAmount.clear();
        
        // Clear exchange form
        txtExchangeItemName.clear();
        cmbExchangeMetalType.setValue(null);
        txtExchangePurity.clear();
        txtExchangeGrossWeight.clear();
        txtExchangeDeduction.clear();
        txtExchangeNetWeight.clear();
        txtExchangeRate.clear();
        txtExchangeAmount.clear();
        
        // Clear payment details
        if (!cmbBankAccount.getItems().isEmpty()) {
            cmbBankAccount.setValue(cmbBankAccount.getItems().get(0));
        }
        txtPaymentReference.clear();
        txtPaidAmount.clear();
        txtDiscount.setText("0.00");
        
        // Clear tables
        purchaseItems.clear();
        exchangeItems.clear();
        
        // Reset mode to purchase
        chipPurchase.setSelected(true);
        
        // Update summary
        updateSummary();
    }
    
    /**
     * Set the dialog stage for this controller
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    /**
     * Set the controller to edit mode with an existing purchase invoice
     */
    public void setEditMode(PurchaseInvoice invoice) {
        LOG.info("Setting edit mode for invoice ID: {}", invoice != null ? invoice.getId() : "null");
        this.isEditMode = true;
        this.invoiceToEdit = invoice;
        
        // Load invoice data after initialization
        javafx.application.Platform.runLater(() -> {
            loadInvoiceDataForEditing();
        });
    }
    
    /**
     * Reset the controller to new invoice mode
     */
    public void setNewInvoiceMode() {
        LOG.info("Resetting to new invoice mode");
        this.isEditMode = false;
        this.invoiceToEdit = null;
    }
    
    /**
     * Load purchase invoice data into the form for editing
     */
    private void loadInvoiceDataForEditing() {
        if (invoiceToEdit == null) return;
        
        try {
            LOG.info("Loading purchase invoice data for editing: {}", invoiceToEdit.getInvoiceNumber());
            
            // Set supplier information
            Supplier supplier = invoiceToEdit.getSupplier();
            if (supplier != null && cmbSupplier != null) {
                cmbSupplier.setValue(supplier);
            }
            
            // Set supplier invoice number  
            if (txtSupplierInvoice != null) {
                txtSupplierInvoice.setText(invoiceToEdit.getSupplierInvoiceNumber() != null ? invoiceToEdit.getSupplierInvoiceNumber() : "");
            }
            
            // Set purchase type
            if (invoiceToEdit.getPurchaseType() != null) {
                switch (invoiceToEdit.getPurchaseType()) {
                    case NEW_STOCK:
                        if (chipPurchase != null) chipPurchase.setSelected(true);
                        break;
                    case EXCHANGE_ITEMS:
                        if (chipExchange != null) chipExchange.setSelected(true);
                        break;
                }
            }
            
            // Set payment method
            if (invoiceToEdit.getPaymentMethod() != null) {
                if (invoiceToEdit.getPaymentMethod() == PurchaseInvoice.PaymentMethod.CREDIT) {
                    if (chkCreditPurchase != null) {
                        chkCreditPurchase.setSelected(true);
                    }
                } else {
                    // For other payment methods (CASH, BANK_TRANSFER), uncheck credit purchase
                    if (chkCreditPurchase != null) {
                        chkCreditPurchase.setSelected(false);
                    }
                }
            }
            
            // Set financial details
            if (txtGstRate != null && invoiceToEdit.getGstRate() != null) {
                txtGstRate.setText(invoiceToEdit.getGstRate().toString());
            }
            if (txtDiscount != null && invoiceToEdit.getDiscount() != null) {
                txtDiscount.setText(invoiceToEdit.getDiscount().toString());
            }
            if (txtPaidAmount != null && invoiceToEdit.getPaidAmount() != null) {
                txtPaidAmount.setText(invoiceToEdit.getPaidAmount().toString());
            }
            if (txtPaymentReference != null && invoiceToEdit.getPaymentReference() != null) {
                txtPaymentReference.setText(invoiceToEdit.getPaymentReference());
            }
            
            // Load purchase transactions
            if (invoiceToEdit.getPurchaseTransactions() != null) {
                purchaseItems.clear();
                purchaseItems.addAll(invoiceToEdit.getPurchaseTransactions());
            }
            
            // Load exchange transactions
            if (invoiceToEdit.getPurchaseExchangeTransactions() != null) {
                exchangeItems.clear();
                exchangeItems.addAll(invoiceToEdit.getPurchaseExchangeTransactions());
            }
            
            // Update summary
            updateSummary();
            
            LOG.info("Purchase invoice data loaded successfully for editing");
            
        } catch (Exception e) {
            LOG.error("Error loading purchase invoice data for editing", e);
            if (alertNotification != null) {
                alertNotification.showError("Error loading purchase invoice data: " + e.getMessage());
            }
        }
    }
}