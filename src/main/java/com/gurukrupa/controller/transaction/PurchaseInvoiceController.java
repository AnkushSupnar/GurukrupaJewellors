package com.gurukrupa.controller.transaction;

import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.entities.PurchaseInvoice.PaymentMethod;
import com.gurukrupa.data.entities.PurchaseInvoice.PurchaseType;
import com.gurukrupa.data.entities.PurchaseTransaction.ItemType;
import com.gurukrupa.data.service.*;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.utility.CurrencyFormatter;
import com.gurukrupa.utility.MetalTypeEnum;
import com.gurukrupa.utility.WeightFormatter;
import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.controller.master.JewelryItemFormController;
import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.view.FxmlView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;


import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.StackPane;
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
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.IntegerStringConverter;
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
    @FXML private TextField txtItemName;
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
    @FXML private Button btnUpdateItem;
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
    @FXML private TableColumn<PurchaseTransaction, Void> colAction;
    
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
    @FXML private Button btnClearExchangeForm;
    
    // Exchange Items Table
    @FXML private TableView<ExchangeTransaction> exchangeItemsTable;
    @FXML private TableColumn<ExchangeTransaction, Integer> colExSno;
    @FXML private TableColumn<ExchangeTransaction, String> colExItemName;
    @FXML private TableColumn<ExchangeTransaction, String> colExMetalType;
    @FXML private TableColumn<ExchangeTransaction, BigDecimal> colExPurity;
    @FXML private TableColumn<ExchangeTransaction, BigDecimal> colExGrossWeight;
    @FXML private TableColumn<ExchangeTransaction, BigDecimal> colExDeduction;
    @FXML private TableColumn<ExchangeTransaction, BigDecimal> colExNetWeight;
    @FXML private TableColumn<ExchangeTransaction, BigDecimal> colExRate;
    @FXML private TableColumn<ExchangeTransaction, BigDecimal> colExAmount;
    @FXML private TableColumn<ExchangeTransaction, Void> colExAction;
    
    // Summary Panel
    @FXML private Label lblTotalPurchaseItems;
    @FXML private Label lblTotalPurchaseWeight;
    @FXML private Label lblTotalExchangeItems;
    @FXML private Label lblTotalExchangeWeight;
    @FXML private Label lblSubtotal;
    @FXML private Label lblGST;
    @FXML private Label lblTotalAmount;
    @FXML private TextField txtDiscount;
    @FXML private TextField txtGstRate;
    @FXML private Label lblNetTotal;
    @FXML private Label lblExchangeAmount;
    @FXML private Label lblGrandTotal;
    
    // Payment Details
    @FXML private ComboBox<PaymentMethod> cmbPaymentMode;
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
    private JewelryItemService jewelryItemService;
    
    @Autowired
    private AlertNotification alertNotification;
    
    @Autowired
    private SpringFXMLLoader springFXMLLoader;

    private ObservableList<PurchaseTransaction> purchaseItems = FXCollections.observableArrayList();
    private ObservableList<ExchangeTransaction> exchangeItems = FXCollections.observableArrayList();
    private ObservableList<JewelryItem> stockItems = FXCollections.observableArrayList();
    private FilteredList<JewelryItem> filteredStockItems;
    private BigDecimal gstRate = new BigDecimal("3.00");
    private JewelryItem selectedStockItem = null;
    private BigDecimal currentLabourPercentage = null;
    private AutoCompleteTextField itemNameAutoComplete;
    private boolean isProgrammaticallySettingFields = false;
    private PurchaseTransaction editingItem = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("Initializing PurchaseInvoiceController");
        
        setupTableColumns();
        setupComboBoxes();
        setupListeners();
        setupItemNameAutoComplete();
        setupStockCatalog();
        loadSuppliers();
        loadMetalStock();
        loadStockItems();
        updateDateTime();
        clearForm();
        
        // Initialize GST rate field
        if (txtGstRate != null) {
            txtGstRate.setText(gstRate.toString());
        }
        
        // Add listeners for collections
        purchaseItems.addListener((javafx.collections.ListChangeListener<PurchaseTransaction>) change -> updateSummary());
        exchangeItems.addListener((javafx.collections.ListChangeListener<ExchangeTransaction>) change -> updateSummary());
        cmbSupplier.valueProperty().addListener((obs, oldVal, newVal) -> updateSupplierInfo(newVal));
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
        if (colStoneWeight != null) colStoneWeight.setCellValueFactory(new PropertyValueFactory<>("stoneWeight"));
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
        
        // Add action buttons for purchase items
        if (colAction != null) {
            colAction.setCellFactory(column -> new TableCell<>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox actionBox = new HBox(5);
            
            {
                // Edit button
                FontAwesomeIcon editIcon = new FontAwesomeIcon();
                editIcon.setGlyphName("EDIT");
                editIcon.setSize("1.0em");
                editIcon.setFill(Color.WHITE);
                editButton.setGraphic(editIcon);
                editButton.setStyle("-fx-background-color: #2196F3; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
                editButton.setOnAction(event -> {
                    PurchaseTransaction item = getTableView().getItems().get(getIndex());
                    populateFormForEdit(item);
                });
                
                // Delete button
                FontAwesomeIcon deleteIcon = new FontAwesomeIcon();
                deleteIcon.setGlyphName("TRASH");
                deleteIcon.setSize("1.0em");
                deleteIcon.setFill(Color.WHITE);
                deleteButton.setGraphic(deleteIcon);
                deleteButton.setStyle("-fx-background-color: #F44336; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
                deleteButton.setOnAction(event -> {
                    PurchaseTransaction item = getTableView().getItems().get(getIndex());
                    purchaseItems.remove(item);
                    updateSummary();
                });
                
                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(editButton, deleteButton);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
            });
        }
        
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
        
        // Add action buttons for exchange items
        if (colExAction != null) {
            colExAction.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button();
            
            {
                FontAwesomeIcon deleteIcon = new FontAwesomeIcon();
                deleteIcon.setGlyphName("TRASH");
                deleteIcon.setSize("1.2em");
                deleteIcon.setFill(Color.WHITE);
                deleteButton.setGraphic(deleteIcon);
                deleteButton.setStyle("-fx-background-color: #F44336; -fx-cursor: hand;");
                deleteButton.setOnAction(event -> {
                    ExchangeTransaction item = getTableView().getItems().get(getIndex());
                    exchangeItems.remove(item);
                    updateSummary();
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
            });
        }
        
        if (exchangeItemsTable != null) {
            exchangeItemsTable.setItems(exchangeItems);
            exchangeItemsTable.setEditable(false);
        }
    }
    
    private void setupComboBoxes() {
        // Payment mode combo
        cmbPaymentMode.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        cmbPaymentMode.setValue(PaymentMethod.CASH);
        
        // Supplier combo
        cmbSupplier.setConverter(new StringConverter<Supplier>() {
            @Override
            public String toString(Supplier supplier) {
                return supplier != null ? supplier.getSupplierFullName() : "";
            }
            
            @Override
            public Supplier fromString(String string) {
                return null;
            }
        });
        
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
    
    private void loadSuppliers() {
        try {
            List<Supplier> suppliers = supplierService.getAllActiveSuppliers();
            cmbSupplier.setItems(FXCollections.observableArrayList(suppliers));
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
            txtInvoiceDate.setText(LocalDateTime.now().format(DATE_FORMATTER));
            lblInvoiceNumber.setText(purchaseInvoiceService.generateInvoiceNumber());
        });
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
            txtExchangeNetWeight.textProperty().addListener((obs, oldVal, newVal) -> calculateExchangeTotal());
            txtExchangeRate.textProperty().addListener((obs, oldVal, newVal) -> calculateExchangeTotal());
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
    
    private void setupItemNameAutoComplete() {
        if (txtItemName != null) {
            // Create AutoCompleteTextField for item name search
            itemNameAutoComplete = new AutoCompleteTextField(txtItemName);
            
            // Load item names from jewelry items
            updateItemNameSuggestions();
            
            // When an item is selected, populate other fields
            txtItemName.textProperty().addListener((obs, oldVal, newVal) -> {
                // Skip if we're programmatically setting fields
                if (isProgrammaticallySettingFields) {
                    return;
                }
                
                if (newVal != null && !newVal.isEmpty()) {
                    // Find matching item in stock
                    JewelryItem matchedItem = stockItems.stream()
                        .filter(item -> item.getItemName().equalsIgnoreCase(newVal))
                        .findFirst()
                        .orElse(null);
                    
                    if (matchedItem != null) {
                        // Use the same method to populate all fields consistently
                        populatePurchaseItemFields(matchedItem);
                    }
                }
            });
        }
    }
    
    private void updateItemNameSuggestions() {
        if (itemNameAutoComplete != null && stockItems != null) {
            List<String> itemNames = stockItems.stream()
                .map(JewelryItem::getItemName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            itemNameAutoComplete.setSuggestions(itemNames);
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
    
    @FXML
    private void handleAddToBill() {
        try {
            PurchaseTransaction newItem = new PurchaseTransaction();
            
            // Get values from form fields
            newItem.setItemCode(txtItemCode != null ? txtItemCode.getText() : "ITEM-" + System.currentTimeMillis());
            newItem.setItemName(txtItemName != null ? txtItemName.getText() : "New Item");
            Metal selectedMetal = cmbMetalType != null ? cmbMetalType.getValue() : null;
            newItem.setMetalType(selectedMetal != null ? selectedMetal.getMetalName() : "GOLD");
            
            // Parse numeric values
            newItem.setPurity(parseBigDecimal(txtPurity));
            newItem.setQuantity(parseInt(txtQuantity, 1));
            newItem.setGrossWeight(parseBigDecimal(txtGrossWeight));
            newItem.setNetWeight(parseBigDecimal(txtNetWeight));
            
            // Get rate per 10 grams and convert to per gram
            BigDecimal ratePerTenGrams = parseBigDecimal(txtRate);
            if (ratePerTenGrams.compareTo(BigDecimal.ZERO) == 0) {
                ratePerTenGrams = getCurrentMetalRate(newItem.getMetalType(), newItem.getPurity());
            }
            BigDecimal ratePerGram = ratePerTenGrams.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            newItem.setRatePerGram(ratePerGram);
            
            // Set making charges
            newItem.setMakingCharges(parseBigDecimal(txtLabourCharges));
            
            newItem.setItemType(ItemType.NEW_ITEM);
            newItem.calculateTotalAmount();
            
            purchaseItems.add(newItem);
            updateSummary();
            clearPurchaseForm();
            
            // Focus on the new item
            purchaseItemsTable.scrollTo(newItem);
            purchaseItemsTable.getSelectionModel().select(newItem);
        } catch (Exception e) {
            LOG.error("Error adding item", e);
            alertNotification.showError("Failed to add item: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAddExchangeItem() {
        try {
            ExchangeTransaction newItem = new ExchangeTransaction();
            
            // Get values from form fields
            newItem.setItemName(txtExchangeItemName != null ? txtExchangeItemName.getText() : "Exchange Item");
            Metal selectedExchangeMetal = cmbExchangeMetalType != null ? cmbExchangeMetalType.getValue() : null;
            newItem.setMetalType(selectedExchangeMetal != null ? selectedExchangeMetal.getMetalName() : "GOLD");
            newItem.setPurity(parseBigDecimal(txtExchangePurity));
            newItem.setGrossWeight(parseBigDecimal(txtExchangeGrossWeight));
            newItem.setDeduction(parseBigDecimal(txtExchangeDeduction));
            newItem.setNetWeight(parseBigDecimal(txtExchangeNetWeight));
            
            // Get rate per 10 grams
            BigDecimal ratePerTenGrams = parseBigDecimal(txtExchangeRate);
            if (ratePerTenGrams.compareTo(BigDecimal.ZERO) == 0) {
                ratePerTenGrams = getCurrentMetalRate(newItem.getMetalType(), newItem.getPurity());
            }
            newItem.setRatePerTenGrams(ratePerTenGrams);
            
            newItem.calculateNetWeightAndAmount();
            
            exchangeItems.add(newItem);
            updateSummary();
            clearExchangeForm();
            
            // Focus on the new item
            exchangeItemsTable.scrollTo(newItem);
            exchangeItemsTable.getSelectionModel().select(newItem);
        } catch (Exception e) {
            LOG.error("Error adding exchange item", e);
            alertNotification.showError("Failed to add exchange item: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClearForm() {
        clearPurchaseForm();
        editingItem = null;
        btnAddToBill.setVisible(true);
        btnUpdateItem.setVisible(false);
    }
    
    @FXML
    private void handleUpdateItem() {
        if (editingItem != null) {
            // Update the item with new values
            editingItem.setItemCode(txtItemCode != null ? txtItemCode.getText() : "");
            editingItem.setItemName(txtItemName != null ? txtItemName.getText() : "");
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
            btnAddToBill.setVisible(true);
            btnUpdateItem.setVisible(false);
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
            updateItemNameSuggestions();
        } catch (Exception e) {
            LOG.error("Error opening jewelry item form", e);
            alertNotification.showError("Failed to open jewelry item form: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClearExchangeForm() {
        clearExchangeForm();
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
        try {
            if (!validateForm()) {
                return;
            }
            
            Supplier supplier = cmbSupplier.getValue();
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
            for (ExchangeTransaction exItem : exchangeItems) {
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
                cmbPaymentMode.getValue(),
                txtPaymentReference != null ? txtPaymentReference.getText().trim() : "",
                null  // notes
            );
            
            // Process payment if needed
            if (invoice.getGrandTotal().compareTo(BigDecimal.ZERO) > 0) {
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
        if (cmbSupplier.getValue() == null) {
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
            paymentModeService.createPurchasePayment(
                invoice,
                cmbPaymentMode.getValue().name(),
                invoice.getGrandTotal(),
                txtPaymentReference.getText().trim()
            );
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
                .map(ExchangeTransaction::getNetWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal exchangeTotal = exchangeItems.stream()
                .map(ExchangeTransaction::getTotalAmount)
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
            
            // Update labels
            if (lblTotalPurchaseItems != null) lblTotalPurchaseItems.setText(String.valueOf(totalPurchaseItems));
            if (lblTotalPurchaseWeight != null) lblTotalPurchaseWeight.setText(WeightFormatter.format(totalPurchaseWeight) + " g");
            if (lblTotalExchangeItems != null) lblTotalExchangeItems.setText(String.valueOf(totalExchangeItems));
            if (lblTotalExchangeWeight != null) lblTotalExchangeWeight.setText(WeightFormatter.format(totalExchangeWeight) + " g");
            if (lblSubtotal != null) lblSubtotal.setText(CurrencyFormatter.format(subtotal));
            if (lblGST != null) lblGST.setText(CurrencyFormatter.format(gstAmount));
            if (lblNetTotal != null) lblNetTotal.setText(CurrencyFormatter.format(totalWithGst));
            if (lblTotalAmount != null) lblTotalAmount.setText(CurrencyFormatter.format(totalAfterDiscount));
            if (lblExchangeAmount != null) lblExchangeAmount.setText(CurrencyFormatter.format(exchangeTotal));
            if (lblGrandTotal != null) lblGrandTotal.setText(CurrencyFormatter.format(grandTotal));
            
            updatePendingAmount();
        });
    }
    
    private void clearForm() {
        purchaseItems.clear();
        exchangeItems.clear();
        cmbSupplier.setValue(null);
        if (txtSupplierInvoice != null) txtSupplierInvoice.clear();
        if (txtPaymentReference != null) txtPaymentReference.clear();
        if (txtPaidAmount != null) txtPaidAmount.clear();
        if (txtDiscount != null) txtDiscount.setText("0");
        cmbPaymentMode.setValue(PaymentMethod.CASH);
        clearPurchaseForm();
        clearExchangeForm();
        updateDateTime();
        updateSummary();
        updateSupplierInfo(null);
    }
    
    private void clearPurchaseForm() {
        if (txtItemCode != null) txtItemCode.clear();
        if (txtItemName != null) txtItemName.clear();
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
        if (txtItemName != null) txtItemName.setText(item.getItemName());
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
        if (txtItemName != null) txtItemName.setText(item.getItemName());
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
        
        // Show update button and hide add button
        btnAddToBill.setVisible(false);
        btnUpdateItem.setVisible(true);
        
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
}