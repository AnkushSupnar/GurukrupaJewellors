package com.gurukrupa.controller.transaction;

import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.entities.PurchaseInvoice.PaymentMethod;
import com.gurukrupa.data.entities.PurchaseInvoice.PurchaseType;
import com.gurukrupa.data.entities.PurchaseTransaction.ItemType;
import com.gurukrupa.data.service.*;
import com.gurukrupa.utility.AlertNotification;
import com.gurukrupa.utility.CurrencyFormatter;
import com.gurukrupa.utility.MetalTypeEnum;
import com.gurukrupa.utility.WeightFormatter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;


import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

    @FXML private VBox metalStockContainer;
    @FXML private Button btnRefreshStock;
    
    @FXML private Label lblInvoiceNumber;
    @FXML private RadioButton rbNewStock, rbExchangeItems, rbRawMaterial;
    @FXML private ToggleGroup purchaseTypeGroup;
    
    @FXML private ComboBox<Supplier> cmbSupplier;
    @FXML private TextField txtSupplierInvoice;
    @FXML private TextField txtInvoiceDate;
    
    @FXML private TableView<PurchaseTransaction> purchaseItemsTable;
    @FXML private TableColumn<PurchaseTransaction, String> colItemName;
    @FXML private TableColumn<PurchaseTransaction, String> colMetalType;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colPurity;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colGrossWeight;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colNetWeight;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colRate;
    @FXML private TableColumn<PurchaseTransaction, BigDecimal> colAmount;
    @FXML private TableColumn<PurchaseTransaction, Void> colAction;
    
    @FXML private Button btnAddItem;
    @FXML private Button btnClear;
    @FXML private Button btnSave;
    
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalWeight;
    
    @FXML private Label lblSupplierName;
    @FXML private Label lblSupplierGST;
    @FXML private Label lblSupplierContact;
    @FXML private Label lblSubtotal;
    @FXML private Label lblGST;
    @FXML private Label lblTotalAmount;
    
    @FXML private ComboBox<PaymentMethod> cmbPaymentMode;
    @FXML private TextField txtPaymentReference;

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

    private ObservableList<PurchaseTransaction> purchaseItems = FXCollections.observableArrayList();
    private BigDecimal gstRate = new BigDecimal("3.00");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("Initializing PurchaseInvoiceController");
        
        setupTableColumns();
        setupComboBoxes();
        loadSuppliers();
        loadMetalStock();
        updateDateTime();
        clearForm();
        
        // Add listeners
        purchaseItems.addListener((javafx.collections.ListChangeListener<PurchaseTransaction>) change -> updateSummary());
        cmbSupplier.valueProperty().addListener((obs, oldVal, newVal) -> updateSupplierInfo(newVal));
        purchaseTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> handlePurchaseTypeChange());
    }
    
    private void setupTableColumns() {
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colMetalType.setCellValueFactory(new PropertyValueFactory<>("metalType"));
        colPurity.setCellValueFactory(new PropertyValueFactory<>("purity"));
        colGrossWeight.setCellValueFactory(new PropertyValueFactory<>("grossWeight"));
        colNetWeight.setCellValueFactory(new PropertyValueFactory<>("netWeight"));
        // Display rate per 10 grams even though we store per gram
        colRate.setCellValueFactory(cellData -> {
            BigDecimal ratePerGram = cellData.getValue().getRatePerGram();
            BigDecimal ratePerTenGrams = ratePerGram != null ? ratePerGram.multiply(BigDecimal.TEN) : BigDecimal.ZERO;
            return new SimpleObjectProperty<>(ratePerTenGrams);
        });
        colAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        
        // Make columns editable
        colItemName.setCellFactory(TextFieldTableCell.forTableColumn());
        colItemName.setOnEditCommit(event -> {
            event.getRowValue().setItemName(event.getNewValue());
            updateSummary();
        });
        
        colGrossWeight.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        colGrossWeight.setOnEditCommit(event -> {
            event.getRowValue().setGrossWeight(event.getNewValue());
            event.getRowValue().calculateTotalAmount();
            purchaseItemsTable.refresh();
            updateSummary();
        });
        
        colNetWeight.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        colNetWeight.setOnEditCommit(event -> {
            event.getRowValue().setNetWeight(event.getNewValue());
            event.getRowValue().calculateTotalAmount();
            purchaseItemsTable.refresh();
            updateSummary();
        });
        
        colRate.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        colRate.setOnEditCommit(event -> {
            BigDecimal ratePerTenGrams = event.getNewValue();
            // Convert rate per 10 grams to rate per gram
            BigDecimal ratePerGram = ratePerTenGrams.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            event.getRowValue().setRatePerGram(ratePerGram);
            event.getRowValue().calculateTotalAmount();
            purchaseItemsTable.refresh();
            updateSummary();
        });
        
        // Add action buttons
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button();
            
            {
                FontAwesomeIcon deleteIcon = new FontAwesomeIcon();
                deleteIcon.setGlyphName("TRASH");
                deleteIcon.setSize("1.2em");
                deleteIcon.setFill(Color.WHITE);
                deleteButton.setGraphic(deleteIcon);
                deleteButton.setStyle("-fx-background-color: #F44336; -fx-cursor: hand;");
                deleteButton.setOnAction(event -> {
                    PurchaseTransaction item = getTableView().getItems().get(getIndex());
                    purchaseItems.remove(item);
                    updateSummary();
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });
        
        purchaseItemsTable.setItems(purchaseItems);
        purchaseItemsTable.setEditable(true);
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
    }
    
    private void loadSuppliers() {
        try {
            List<Supplier> suppliers = supplierService.getAllActiveSuppliers();
            cmbSupplier.setItems(FXCollections.observableArrayList(suppliers));
        } catch (Exception e) {
            LOG.error("Error loading suppliers", e);
            AlertNotification.showError("Error", "Failed to load suppliers: " + e.getMessage());
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
            lblSupplierName.setText(supplier.getSupplierFullName());
            lblSupplierGST.setText(supplier.getGstNumber() != null ? "GST: " + supplier.getGstNumber() : "");
            lblSupplierContact.setText(supplier.getMobile() != null ? "Ph: " + supplier.getMobile() : "");
        } else {
            lblSupplierName.setText("Not Selected");
            lblSupplierGST.setText("");
            lblSupplierContact.setText("");
        }
    }
    
    private void handlePurchaseTypeChange() {
        // Logic can be added here if needed based on purchase type
        if (rbExchangeItems.isSelected()) {
            // Show only exchange metal stock items
            loadMetalStock();
        }
    }
    
    @FXML
    private void handleAddItem() {
        try {
            PurchaseTransaction newItem = new PurchaseTransaction();
            newItem.setItemCode("ITEM-" + System.currentTimeMillis());
            newItem.setItemName("New Item");
            newItem.setMetalType("GOLD");
            newItem.setPurity(new BigDecimal("22"));
            newItem.setGrossWeight(BigDecimal.ZERO);
            newItem.setNetWeight(BigDecimal.ZERO);
            
            // Get rate per 10 grams and convert to per gram
            BigDecimal ratePerTenGrams = getCurrentMetalRate("GOLD", new BigDecimal("22"));
            BigDecimal ratePerGram = ratePerTenGrams.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            newItem.setRatePerGram(ratePerGram);
            
            newItem.setQuantity(1);
            newItem.setItemType(rbExchangeItems.isSelected() ? ItemType.EXCHANGE_ITEM : ItemType.NEW_ITEM);
            newItem.calculateTotalAmount();
            
            purchaseItems.add(newItem);
            updateSummary();
            
            // Focus on the new item
            purchaseItemsTable.scrollTo(newItem);
            purchaseItemsTable.getSelectionModel().select(newItem);
        } catch (Exception e) {
            LOG.error("Error adding item", e);
            AlertNotification.showError("Error", "Failed to add item: " + e.getMessage());
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
            if (rbExchangeItems.isSelected()) {
                purchaseType = PurchaseType.EXCHANGE_ITEMS;
            } else if (rbRawMaterial.isSelected()) {
                purchaseType = PurchaseType.RAW_MATERIAL;
            }
            
            // Create invoice
            PurchaseInvoice invoice = purchaseInvoiceService.createPurchaseInvoice(
                supplier,
                new ArrayList<>(purchaseItems),
                txtSupplierInvoice.getText().trim(),
                purchaseType,
                BigDecimal.ZERO,  // discount
                gstRate,
                BigDecimal.ZERO,  // transport charges
                BigDecimal.ZERO,  // other charges
                cmbPaymentMode.getValue(),
                txtPaymentReference.getText().trim(),
                null  // notes
            );
            
            // Process payment if needed
            if (invoice.getGrandTotal().compareTo(BigDecimal.ZERO) > 0) {
                processPayment(invoice);
            }
            
            AlertNotification.showSuccess("Success", "Purchase invoice saved successfully!");
            clearForm();
            
        } catch (Exception e) {
            LOG.error("Error saving invoice", e);
            AlertNotification.showError("Error", "Failed to save invoice: " + e.getMessage());
        }
    }
    
    private boolean validateForm() {
        if (cmbSupplier.getValue() == null) {
            AlertNotification.showWarning("Validation", "Please select a supplier");
            return false;
        }
        
        if (purchaseItems.isEmpty()) {
            AlertNotification.showWarning("Validation", "Please add at least one item");
            return false;
        }
        
        // Validate items
        for (PurchaseTransaction item : purchaseItems) {
            if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
                AlertNotification.showWarning("Validation", "Item name cannot be empty");
                return false;
            }
            if (item.getNetWeight() == null || item.getNetWeight().compareTo(BigDecimal.ZERO) <= 0) {
                AlertNotification.showWarning("Validation", "Net weight must be greater than zero");
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
            int totalItems = purchaseItems.size();
            BigDecimal totalWeight = purchaseItems.stream()
                .map(PurchaseTransaction::getNetWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal subtotal = purchaseItems.stream()
                .map(PurchaseTransaction::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal gstAmount = subtotal.multiply(gstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal total = subtotal.add(gstAmount);
            
            lblTotalItems.setText(String.valueOf(totalItems));
            lblTotalWeight.setText(WeightFormatter.format(totalWeight));
            lblSubtotal.setText(CurrencyFormatter.format(subtotal));
            lblGST.setText(CurrencyFormatter.format(gstAmount));
            lblTotalAmount.setText(CurrencyFormatter.format(total));
        });
    }
    
    private void clearForm() {
        purchaseItems.clear();
        cmbSupplier.setValue(null);
        txtSupplierInvoice.clear();
        txtPaymentReference.clear();
        cmbPaymentMode.setValue(PaymentMethod.CASH);
        rbNewStock.setSelected(true);
        updateDateTime();
        updateSummary();
        updateSupplierInfo(null);
    }
}