package com.gurukrupa.controller.stock;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.service.*;
import com.gurukrupa.utility.AlertNotification;
import com.gurukrupa.utility.WeightFormatter;
import com.gurukrupa.view.FxmlView;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class StockEntryController implements Initializable {

    // ===== Services =====
    @Autowired
    private StockEntryService stockEntryService;

    @Autowired
    private PurchaseInvoiceService purchaseInvoiceService;

    @Autowired
    private JewelryItemService jewelryItemService;

    @Autowired
    private MetalService metalService;

    @Autowired
    private SpringFXMLLoader fxmlLoader;

    // ===== Left Panel - Previous Entries =====
    @FXML private TextField txtSearchEntry;
    @FXML private DatePicker datePickerFrom;
    @FXML private DatePicker datePickerTo;
    @FXML private Label lblEntriesCount;
    @FXML private Label lblEntriesTotal;
    @FXML private ListView<StockEntryMaster> entriesList;

    // ===== Header =====
    @FXML private Label lblEntryNumber;
    @FXML private TextField txtEntryDate;

    // ===== Purchase Invoice Selection =====
    @FXML private HBox invoiceSearchContainer;
    @FXML private Label lblSupplierName;
    @FXML private Label lblInvoiceDate;
    @FXML private VBox metalStockPanel;
    @FXML private VBox metalStockList;

    // ===== Item Entry =====
    @FXML private HBox itemSearchContainer;
    @FXML private Label lblItemName;
    @FXML private Label lblItemCategory;
    @FXML private Label lblItemMetal;
    @FXML private Label lblItemWeight;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtItemRemarks;

    // ===== Items Table =====
    @FXML private Label lblItemCount;
    @FXML private TableView<StockEntryItem> itemsTable;
    @FXML private TableColumn<StockEntryItem, Integer> colSno;
    @FXML private TableColumn<StockEntryItem, String> colItemCode;
    @FXML private TableColumn<StockEntryItem, String> colItemName;
    @FXML private TableColumn<StockEntryItem, String> colCategory;
    @FXML private TableColumn<StockEntryItem, String> colMetalType;
    @FXML private TableColumn<StockEntryItem, String> colPurity;
    @FXML private TableColumn<StockEntryItem, String> colGrossWeight;
    @FXML private TableColumn<StockEntryItem, String> colNetWeight;
    @FXML private TableColumn<StockEntryItem, Integer> colQuantity;
    @FXML private TableColumn<StockEntryItem, String> colTotalWeight;

    // ===== Summary =====
    @FXML private VBox metalConsumptionPanel;
    @FXML private VBox metalConsumptionList;
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalGrossWeight;
    @FXML private Label lblTotalNetWeight;
    @FXML private TextArea txtRemarks;

    // ===== Internal State =====
    private AutoCompleteTextField<PurchaseInvoice> invoiceSearchField;
    private AutoCompleteTextField<JewelryItem> itemSearchField;
    private PurchaseInvoice selectedPurchaseInvoice;
    private JewelryItem selectedJewelryItem;
    private ObservableList<StockEntryItem> currentItems = FXCollections.observableArrayList();
    private StockEntryMaster currentEntry;
    private Map<String, BigDecimal> availableMetalStock = new HashMap<>();
    private Map<String, BigDecimal> consumedMetalStock = new HashMap<>();

    // Flag to prevent infinite loop when programmatically updating item selection
    private boolean isUpdatingItemSelection = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPurchaseInvoiceAutocomplete();
        setupJewelryItemAutocomplete();
        setupEntriesList();
        setupItemsTable();
        setupCurrentEntry();
        loadPreviousEntries();
    }

    // ===== Setup Methods =====

    private void setupPurchaseInvoiceAutocomplete() {
        // Get all invoices
        List<PurchaseInvoice> allInvoices = purchaseInvoiceService.getAllInvoices();

        // Filter out invoices that have no remaining metal available
        List<PurchaseInvoice> availableInvoices = allInvoices.stream()
                .filter(invoice -> {
                    // Check if invoice has metal transactions
                    if (invoice.getPurchaseMetalTransactions() == null ||
                        invoice.getPurchaseMetalTransactions().isEmpty()) {
                        return false;
                    }
                    // Check if there's remaining metal available
                    Map<String, BigDecimal> remaining = stockEntryService.getRemainingMetalForInvoice(invoice);
                    return !remaining.isEmpty();
                })
                .collect(Collectors.toList());

        // Create string converter for display
        StringConverter<PurchaseInvoice> converter = new StringConverter<PurchaseInvoice>() {
            @Override
            public String toString(PurchaseInvoice invoice) {
                if (invoice == null) return "";
                return invoice.getInvoiceNumber() + " - " +
                       (invoice.getSupplier() != null ? invoice.getSupplier().getSupplierName() : "Unknown");
            }

            @Override
            public PurchaseInvoice fromString(String string) {
                return null; // Not used
            }
        };

        // Create filter function
        java.util.function.Function<String, List<PurchaseInvoice>> filterFunction = searchText -> {
            return availableInvoices.stream()
                    .filter(invoice -> {
                        String searchLower = searchText.toLowerCase();
                        return invoice.getInvoiceNumber().toLowerCase().contains(searchLower) ||
                               (invoice.getSupplier() != null &&
                                invoice.getSupplier().getSupplierName().toLowerCase().contains(searchLower));
                    })
                    .collect(Collectors.toList());
        };

        // Create autocomplete field
        invoiceSearchField = new AutoCompleteTextField<>(availableInvoices, converter, filterFunction);
        invoiceSearchField.setPromptText("Search purchase invoice...");
        invoiceSearchField.setMaxWidth(Double.MAX_VALUE);

        // Handle selection
        invoiceSearchField.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectPurchaseInvoice(newVal.getInvoiceNumber());
            }
        });

        invoiceSearchContainer.getChildren().clear();
        invoiceSearchContainer.getChildren().add(invoiceSearchField.getNode());
        HBox.setHgrow(invoiceSearchField.getNode(), javafx.scene.layout.Priority.ALWAYS);
    }

    private void setupJewelryItemAutocomplete() {
        // Create string converter for display
        StringConverter<JewelryItem> converter = new StringConverter<JewelryItem>() {
            @Override
            public String toString(JewelryItem item) {
                if (item == null) return "";
                return item.getItemCode() + " - " + item.getItemName() +
                       " (" + item.getMetalType() + " " + item.getPurity() + ")";
            }

            @Override
            public JewelryItem fromString(String string) {
                return null; // Not used
            }
        };

        // Create filter function (initially empty, will be rebuilt when invoice is selected)
        java.util.function.Function<String, List<JewelryItem>> filterFunction = searchText -> {
            return new ArrayList<>();
        };

        // Create autocomplete field (disabled initially)
        itemSearchField = new AutoCompleteTextField<>(new ArrayList<>(), converter, filterFunction);
        itemSearchField.setPromptText("Select purchase invoice first...");
        itemSearchField.setMaxWidth(Double.MAX_VALUE);
        itemSearchField.getTextField().setDisable(true);

        // Handle selection
        itemSearchField.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Prevent infinite loop: ignore programmatic selection updates
            if (isUpdatingItemSelection) {
                log.debug("Ignoring programmatic item selection update");
                return;
            }

            if (newVal != null) {
                selectJewelryItem(newVal.getItemCode());
            }
        });

        HBox node = itemSearchField.getNode();
        itemSearchContainer.getChildren().clear();
        itemSearchContainer.getChildren().add(node);
        HBox.setHgrow(node, javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Rebuild item autocomplete based on available metals from selected invoice
     * IMPORTANT: Only show items matching the metal types available in the invoice
     */
    private void rebuildItemAutocomplete() {
        log.debug("rebuildItemAutocomplete called. selectedPurchaseInvoice: {}, availableMetalStock size: {}",
                selectedPurchaseInvoice != null ? selectedPurchaseInvoice.getInvoiceNumber() : "null",
                availableMetalStock.size());

        if (selectedPurchaseInvoice == null || availableMetalStock.isEmpty()) {
            itemSearchField.getTextField().setDisable(true);
            itemSearchField.setPromptText("Select purchase invoice first...");
            log.debug("Item search field disabled - no invoice or no metal stock available");
            return;
        }

        // Get all active items
        List<JewelryItem> allItems = jewelryItemService.getAllActiveItems();
        log.debug("Retrieved {} total active jewelry items", allItems.size());

        // Get available metal types from selected invoice
        Set<String> availableMetalKeys = availableMetalStock.keySet();
        log.info("Available metal keys from invoice: {}", availableMetalKeys);

        // Filter items to only show those matching available metal types
        // Uses Metal entity reference if available, otherwise falls back to string comparison
        log.info("=== Filtering Jewelry Items ===");
        List<JewelryItem> matchingItems = allItems.stream()
                .filter(item -> {
                    if (!item.getIsActive()) {
                        log.info("Skipping inactive item: {}", item.getItemCode());
                        return false;
                    }

                    // Log item details
                    log.info("Checking item: {} ({})", item.getItemCode(), item.getItemName());
                    log.info("  Item Metal Entity: {}, MetalType: '{}', Purity: {}",
                            item.getMetal() != null ?
                                ("ID=" + item.getMetal().getId() + ", Name=" + item.getMetal().getMetalName()) : "NULL",
                            item.getMetalType(),
                            item.getPurity());

                    // Generate metal key for this item using same logic as service layer
                    String itemMetalKey = getMetalKey(item.getMetal(), item.getMetalType(), item.getPurity());

                    boolean matches = availableMetalKeys.contains(itemMetalKey);
                    log.info("  Generated Item Metal Key: '{}' - Matches Available Keys: {}", itemMetalKey, matches);

                    // Check if this item's metal type is available in the invoice
                    return matches;
                })
                .collect(Collectors.toList());
        log.info("=== Item Filtering Complete ===");

        log.info("Filtering {} total items. Found {} matching items for metals: {}",
                allItems.size(), matchingItems.size(), availableMetalKeys);

        // Create string converter for display
        StringConverter<JewelryItem> converter = new StringConverter<JewelryItem>() {
            @Override
            public String toString(JewelryItem item) {
                if (item == null) return "";
                return item.getItemCode() + " - " + item.getItemName() +
                       " (" + item.getMetalType() + " " + item.getPurity() + ")";
            }

            @Override
            public JewelryItem fromString(String string) {
                return null; // Not used
            }
        };

        // Create filter function for search
        java.util.function.Function<String, List<JewelryItem>> filterFunction = searchText -> {
            return matchingItems.stream()
                    .filter(item -> {
                        String searchLower = searchText.toLowerCase();
                        return item.getItemCode().toLowerCase().contains(searchLower) ||
                               item.getItemName().toLowerCase().contains(searchLower);
                    })
                    .collect(Collectors.toList());
        };

        // Create new autocomplete field with filtered items
        AutoCompleteTextField<JewelryItem> newItemSearchField = new AutoCompleteTextField<>(matchingItems, converter, filterFunction);
        newItemSearchField.setPromptText("Search item by code or name...");
        newItemSearchField.setMaxWidth(Double.MAX_VALUE);

        // Ensure the field is enabled
        newItemSearchField.getTextField().setDisable(false);

        // Handle selection
        newItemSearchField.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Prevent infinite loop: ignore programmatic selection updates
            if (isUpdatingItemSelection) {
                log.debug("Ignoring programmatic item selection update");
                return;
            }

            if (newVal != null) {
                log.debug("Item selected from autocomplete: {} - {}", newVal.getItemCode(), newVal.getItemName());
                selectJewelryItem(newVal.getItemCode());
            } else {
                log.debug("Item selection cleared (newVal is null)");
            }
        });

        // Replace the old field with the new one
        HBox newNode = newItemSearchField.getNode();
        itemSearchContainer.getChildren().clear();
        itemSearchContainer.getChildren().add(newNode);
        HBox.setHgrow(newNode, javafx.scene.layout.Priority.ALWAYS);

        // Update reference
        itemSearchField = newItemSearchField;

        log.info("Item autocomplete rebuilt with {} matching items for available metals: {}",
                matchingItems.size(), availableMetalKeys);

        // Log first few items for debugging
        if (!matchingItems.isEmpty()) {
            log.info("Sample items: {}", matchingItems.stream()
                .limit(3)
                .map(item -> item.getItemCode() + " - " + item.getMetalType() + " " + item.getPurity())
                .collect(Collectors.joining(", ")));
        }
    }

    private void setupEntriesList() {
        entriesList.setCellFactory(param -> new ListCell<StockEntryMaster>() {
            @Override
            protected void updateItem(StockEntryMaster entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    VBox vbox = new VBox(4);
                    vbox.setPadding(new Insets(8));
                    vbox.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");

                    Label entryLabel = new Label(entry.getEntryNumber());
                    entryLabel.setStyle("-fx-font-family: 'Segoe UI Semibold'; -fx-font-size: 13px; -fx-text-fill: #212121;");

                    Label invoiceLabel = new Label("Purchase: " + entry.getPurchaseInvoice().getInvoiceNumber());
                    invoiceLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #757575;");

                    Label detailsLabel = new Label(String.format("Items: %d | Weight: %s",
                            entry.getTotalItems(),
                            WeightFormatter.format(entry.getTotalGrossWeight())));
                    detailsLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #757575;");

                    vbox.getChildren().addAll(entryLabel, invoiceLabel, detailsLabel);
                    setGraphic(vbox);
                }
            }
        });

        entriesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Handle selection if needed
        });
    }

    private void setupItemsTable() {
        colSno.setCellValueFactory(cellData -> {
            int index = itemsTable.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleObjectProperty<>(index);
        });

        colItemCode.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getJewelryItem().getItemCode()));

        colItemName.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getJewelryItem().getItemName()));

        colCategory.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getJewelryItem().getCategory()));

        colMetalType.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getJewelryItem().getMetalType()));

        colPurity.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getJewelryItem().getPurity().toString()));

        colGrossWeight.setCellValueFactory(cellData ->
            new SimpleStringProperty(WeightFormatter.format(cellData.getValue().getJewelryItem().getGrossWeight())));

        colNetWeight.setCellValueFactory(cellData ->
            new SimpleStringProperty(WeightFormatter.format(cellData.getValue().getJewelryItem().getNetWeight())));

        colQuantity.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getQuantity()));

        colTotalWeight.setCellValueFactory(cellData ->
            new SimpleStringProperty(WeightFormatter.format(cellData.getValue().getTotalNetWeight())));

        itemsTable.setItems(currentItems);

        // Handle row selection
        itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadItemForEditing(newVal);
            }
        });
    }

    private void setupCurrentEntry() {
        currentEntry = StockEntryMaster.builder()
                .entryNumber(stockEntryService.generateEntryNumber())
                .entryDate(LocalDateTime.now())
                .status(StockEntryMaster.EntryStatus.ACTIVE)
                .build();

        lblEntryNumber.setText("Entry #: " + currentEntry.getEntryNumber());
        txtEntryDate.setText(currentEntry.getEntryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }

    // ===== Purchase Invoice Selection =====

    private void selectPurchaseInvoice(String invoiceNumber) {
        log.debug("selectPurchaseInvoice called with invoiceNumber: {}", invoiceNumber);
        Optional<PurchaseInvoice> invoiceOpt = purchaseInvoiceService.findByInvoiceNumber(invoiceNumber);

        if (invoiceOpt.isPresent()) {
            PurchaseInvoice invoice = invoiceOpt.get();
            log.debug("Found purchase invoice: {}", invoice.getInvoiceNumber());

            // IMPORTANT: Validate that invoice hasn't been used
            String validationError = stockEntryService.validatePurchaseInvoice(invoice);
            if (validationError != null) {
                log.warn("Invoice validation failed: {}", validationError);
                AlertNotification.showWarning("Invoice Already Used", validationError);
                invoiceSearchField.clear();
                return;
            }

            selectedPurchaseInvoice = invoice;
            currentEntry.setPurchaseInvoice(invoice);
            log.debug("Purchase invoice set to current entry");

            // Update UI
            if (invoice.getSupplier() != null) {
                lblSupplierName.setText(invoice.getSupplier().getSupplierName());
                log.debug("Supplier: {}", invoice.getSupplier().getSupplierName());
            }
            lblInvoiceDate.setText(invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            // Calculate and display available metal stock
            calculateAvailableMetalStock();
            displayMetalStock();
            log.debug("Metal stock calculated and displayed");

            // Rebuild item autocomplete to show only matching metal types
            log.debug("Rebuilding item autocomplete...");
            rebuildItemAutocomplete();

            log.info("Selected purchase invoice: {} with {} metal types available",
                    invoiceNumber, availableMetalStock.size());
        } else {
            log.warn("Purchase invoice not found: {}", invoiceNumber);
        }
    }

    private void calculateAvailableMetalStock() {
        availableMetalStock.clear();
        log.info("=== Calculating Available Metal Stock ===");

        if (selectedPurchaseInvoice != null) {
            log.info("Purchase Invoice: {}", selectedPurchaseInvoice.getInvoiceNumber());

            // Log all metal transactions in the invoice
            if (selectedPurchaseInvoice.getPurchaseMetalTransactions() != null) {
                log.info("Found {} metal transactions in invoice:",
                        selectedPurchaseInvoice.getPurchaseMetalTransactions().size());

                for (PurchaseMetalTransaction transaction : selectedPurchaseInvoice.getPurchaseMetalTransactions()) {
                    log.info("  Transaction: Metal Entity: {}, MetalType: '{}', Purity: {}, GrossWeight: {}",
                            transaction.getMetal() != null ?
                                ("ID=" + transaction.getMetal().getId() + ", Name=" + transaction.getMetal().getMetalName()) : "NULL",
                            transaction.getMetalType(),
                            transaction.getPurity(),
                            transaction.getGrossWeight());
                }
            }

            // Get remaining metal available (already accounts for previous consumptions)
            Map<String, BigDecimal> remainingMetal = stockEntryService.getRemainingMetalForInvoice(selectedPurchaseInvoice);
            availableMetalStock.putAll(remainingMetal);

            log.info("Available metal stock keys: {}", availableMetalStock.keySet());
        } else {
            log.info("No purchase invoice selected");
        }

        // Reset consumed stock for current entry
        consumedMetalStock.clear();
        log.info("=== Available Metal Stock Calculation Complete ===");
    }

    private void displayMetalStock() {
        metalStockList.getChildren().clear();

        if (availableMetalStock.isEmpty()) {
            metalStockPanel.setManaged(false);
            metalStockPanel.setVisible(false);
            return;
        }

        metalStockPanel.setManaged(true);
        metalStockPanel.setVisible(true);

        // Get previously consumed metal for this invoice
        Map<String, BigDecimal> previouslyConsumed = stockEntryService.getConsumedMetalForInvoice(
            selectedPurchaseInvoice.getId()
        );

        // Get total available from invoice
        Map<String, BigDecimal> totalAvailable = new HashMap<>();
        if (selectedPurchaseInvoice.getPurchaseMetalTransactions() != null) {
            for (PurchaseMetalTransaction transaction : selectedPurchaseInvoice.getPurchaseMetalTransactions()) {
                // Use consistent metal key generation
                String key = getMetalKey(transaction.getMetal(),
                        transaction.getMetalType(),
                        transaction.getPurity());
                BigDecimal current = totalAvailable.getOrDefault(key, BigDecimal.ZERO);
                totalAvailable.put(key, current.add(transaction.getGrossWeight()));
            }
        }

        for (Map.Entry<String, BigDecimal> entry : availableMetalStock.entrySet()) {
            String metalKey = entry.getKey();
            BigDecimal remaining = entry.getValue();
            BigDecimal consumed = previouslyConsumed.getOrDefault(metalKey, BigDecimal.ZERO);
            BigDecimal total = totalAvailable.getOrDefault(metalKey, BigDecimal.ZERO);

            VBox metalBox = new VBox(3);

            // Display human-readable metal name
            String displayName = getMetalDisplayName(metalKey);
            Label titleLabel = new Label(displayName);
            titleLabel.setStyle("-fx-font-family: 'Segoe UI Semibold'; -fx-font-size: 11px; -fx-text-fill: #2E7D32;");

            Label infoLabel = new Label(String.format("Remaining: %s / %s",
                WeightFormatter.format(remaining),
                WeightFormatter.format(total)));
            infoLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 10px; -fx-text-fill: #2E7D32;");

            metalBox.getChildren().addAll(titleLabel, infoLabel);

            // Show consumed info if there was previous consumption
            if (consumed.compareTo(BigDecimal.ZERO) > 0) {
                Label consumedLabel = new Label(String.format("(Previously used: %s)",
                    WeightFormatter.format(consumed)));
                consumedLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 9px; -fx-text-fill: #757575;");
                metalBox.getChildren().add(consumedLabel);
            }

            metalStockList.getChildren().add(metalBox);
        }
    }

    // ===== Jewelry Item Selection =====

    private void selectJewelryItem(String itemCode) {
        log.debug("selectJewelryItem called with itemCode: {}", itemCode);
        Optional<JewelryItem> itemOpt = jewelryItemService.findByItemCode(itemCode);

        if (itemOpt.isPresent()) {
            selectedJewelryItem = itemOpt.get();
            log.debug("Found jewelry item: {} - {}", selectedJewelryItem.getItemCode(), selectedJewelryItem.getItemName());

            // Update UI labels
            lblItemName.setText(selectedJewelryItem.getItemName());
            lblItemCategory.setText(selectedJewelryItem.getCategory());
            lblItemMetal.setText(selectedJewelryItem.getMetalType() + " " + selectedJewelryItem.getPurity());
            lblItemWeight.setText(WeightFormatter.format(selectedJewelryItem.getNetWeight()));

            // IMPORTANT: Update the autocomplete field to show the selected item
            // Set flag to prevent infinite loop with the listener
            isUpdatingItemSelection = true;
            try {
                itemSearchField.setSelectedItem(selectedJewelryItem);
                log.debug("Set selected item in autocomplete field: {}", selectedJewelryItem.getItemCode());
            } finally {
                isUpdatingItemSelection = false;
            }

            txtQuantity.setText("1");
            txtItemRemarks.clear();

            log.info("Selected jewelry item: {} - {}", itemCode, selectedJewelryItem.getItemName());
        } else {
            log.warn("Jewelry item not found for itemCode: {}", itemCode);
        }
    }

    private void loadItemForEditing(StockEntryItem item) {
        log.debug("loadItemForEditing called for item: {}", item.getJewelryItem().getItemCode());
        selectedJewelryItem = item.getJewelryItem();
        selectJewelryItem(item.getJewelryItem().getItemCode());
        txtQuantity.setText(String.valueOf(item.getQuantity()));
        txtItemRemarks.setText(item.getRemarks());
        log.debug("Item loaded for editing with quantity: {}", item.getQuantity());
    }

    // ===== Action Handlers =====

    @FXML
    private void handleCreateNewItem() {
        try {
            Parent root = fxmlLoader.load(FxmlView.JEWELRY_ITEM_FORM.getFxmlFile());
            Stage stage = new Stage();
            stage.setTitle("Create New Jewelry Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh item autocomplete - rebuild if invoice is selected, otherwise reset
            if (selectedPurchaseInvoice != null && !availableMetalStock.isEmpty()) {
                rebuildItemAutocomplete();
            } else {
                setupJewelryItemAutocomplete();
            }
        } catch (Exception e) {
            log.error("Error opening jewelry item form", e);
            AlertNotification.showError("Error", "Failed to open item creation form");
        }
    }

    @FXML
    private void handleAddItem() {
        if (selectedPurchaseInvoice == null) {
            AlertNotification.showWarning("Validation Error", "Please select a purchase invoice first");
            return;
        }

        if (selectedJewelryItem == null) {
            AlertNotification.showWarning("Validation Error", "Please select a jewelry item");
            return;
        }

        try {
            int quantity = Integer.parseInt(txtQuantity.getText());
            if (quantity <= 0) {
                AlertNotification.showWarning("Validation Error", "Quantity must be greater than 0");
                return;
            }

            // IMPORTANT: Validate that item's metal type matches available metals
            // Use consistent metal key generation
            String itemMetalKey = getMetalKey(selectedJewelryItem.getMetal(),
                    selectedJewelryItem.getMetalType(),
                    selectedJewelryItem.getPurity());

            // Check if metal is available in stock
            if (!availableMetalStock.containsKey(itemMetalKey)) {
                // Get display names for error message
                String itemDisplayName = getMetalDisplayName(itemMetalKey);
                List<String> availableDisplayNames = availableMetalStock.keySet().stream()
                        .map(this::getMetalDisplayName)
                        .collect(Collectors.toList());

                AlertNotification.showWarning("Metal Type Mismatch",
                    String.format("Cannot add this item. Item metal type '%s' does not match any available metal in the selected purchase invoice.\n\n" +
                                "Available metals: %s",
                                itemDisplayName,
                                String.join(", ", availableDisplayNames)));
                return;
            }

            // Calculate total weight needed for this item
            BigDecimal itemTotalWeight = selectedJewelryItem.getNetWeight().multiply(BigDecimal.valueOf(quantity));

            // Check if there's enough metal available
            BigDecimal currentAvailable = availableMetalStock.get(itemMetalKey);
            BigDecimal currentConsumed = consumedMetalStock.getOrDefault(itemMetalKey, BigDecimal.ZERO);
            BigDecimal remainingAfterAdd = currentAvailable.subtract(currentConsumed).subtract(itemTotalWeight);

            if (remainingAfterAdd.compareTo(BigDecimal.ZERO) < 0) {
                AlertNotification.showWarning("Insufficient Metal",
                    String.format("Not enough %s available.\n\n" +
                                "Required: %s\n" +
                                "Available: %s\n" +
                                "Already used in this entry: %s\n" +
                                "Remaining: %s",
                                itemMetalKey,
                                WeightFormatter.format(itemTotalWeight),
                                WeightFormatter.format(currentAvailable),
                                WeightFormatter.format(currentConsumed),
                                WeightFormatter.format(currentAvailable.subtract(currentConsumed))));
                return;
            }

            // Create new stock entry item
            StockEntryItem newItem = StockEntryItem.builder()
                    .jewelryItem(selectedJewelryItem)
                    .quantity(quantity)
                    .remarks(txtItemRemarks.getText())
                    .build();

            currentItems.add(newItem);
            log.info("Added item to stock entry: {} (Metal: {}, Weight: {})",
                    selectedJewelryItem.getItemCode(), itemMetalKey, WeightFormatter.format(itemTotalWeight));

            updateSummary();
            clearItemSelection();
        } catch (NumberFormatException e) {
            AlertNotification.showWarning("Validation Error", "Invalid quantity");
        }
    }

    @FXML
    private void handleDeleteItem() {
        StockEntryItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertNotification.showWarning("Selection Required", "Please select an item to remove");
            return;
        }

        currentItems.remove(selected);
        updateSummary();
        clearItemSelection();
    }

    @FXML
    private void handleClearItem() {
        clearItemSelection();
    }

    @FXML
    private void handleSave() {
        if (selectedPurchaseInvoice == null) {
            AlertNotification.showWarning("Validation Error", "Please select a purchase invoice");
            return;
        }

        if (currentItems.isEmpty()) {
            AlertNotification.showWarning("Validation Error", "Please add at least one item");
            return;
        }

        try {
            // Set up the entry
            currentEntry.setPurchaseInvoice(selectedPurchaseInvoice);
            currentEntry.setRemarks(txtRemarks.getText());

            // Add items
            currentEntry.getStockEntryItems().clear();
            for (StockEntryItem item : currentItems) {
                item.setStockEntry(currentEntry);
                currentEntry.addItem(item);
            }

            // Save
            StockEntryMaster savedEntry = stockEntryService.save(currentEntry);

            AlertNotification.showSuccess("Success", "Stock entry saved successfully: " + savedEntry.getEntryNumber());

            // Reset form
            handleClearAll();
            loadPreviousEntries();

        } catch (Exception e) {
            log.error("Error saving stock entry", e);
            AlertNotification.showError("Error", "Failed to save stock entry: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearAll() {
        setupCurrentEntry();
        clearPurchaseInvoiceSelection();
        clearItemSelection();
        currentItems.clear();
        txtRemarks.clear();
        updateSummary();
    }

    @FXML
    private void handleSearchEntries() {
        String searchTerm = txtSearchEntry.getText();
        LocalDate fromDate = datePickerFrom.getValue();
        LocalDate toDate = datePickerTo.getValue();

        List<StockEntryMaster> results;

        if (fromDate != null && toDate != null) {
            results = stockEntryService.findByDateRange(
                    fromDate.atStartOfDay(),
                    toDate.atTime(23, 59, 59));
        } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            results = stockEntryService.searchEntries(searchTerm);
        } else {
            results = stockEntryService.findAllActive();
        }

        entriesList.setItems(FXCollections.observableArrayList(results));
        lblEntriesTotal.setText(results.size() + " entries");
    }

    @FXML
    private void handleClearSearch() {
        txtSearchEntry.clear();
        datePickerFrom.setValue(null);
        datePickerTo.setValue(null);
        loadPreviousEntries();
    }

    @FXML
    private void handleLoadEntry() {
        StockEntryMaster selected = entriesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertNotification.showWarning("Selection Required", "Please select an entry to load");
            return;
        }

        loadEntry(selected);
    }

    @FXML
    private void handleViewEntry() {
        StockEntryMaster selected = entriesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertNotification.showWarning("Selection Required", "Please select an entry to view");
            return;
        }

        // For now, just load it
        loadEntry(selected);
    }

    // ===== Helper Methods =====

    private void loadPreviousEntries() {
        List<StockEntryMaster> entries = stockEntryService.findAllActive();
        entriesList.setItems(FXCollections.observableArrayList(entries));
        lblEntriesTotal.setText(entries.size() + " entries");
    }

    private void loadEntry(StockEntryMaster entry) {
        currentEntry = entry;
        lblEntryNumber.setText("Entry #: " + entry.getEntryNumber());
        txtEntryDate.setText(entry.getEntryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        selectedPurchaseInvoice = entry.getPurchaseInvoice();
        invoiceSearchField.setSelectedItem(entry.getPurchaseInvoice());

        if (entry.getPurchaseInvoice().getSupplier() != null) {
            lblSupplierName.setText(entry.getPurchaseInvoice().getSupplier().getSupplierName());
        }
        lblInvoiceDate.setText(entry.getPurchaseInvoice().getInvoiceDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        currentItems.clear();
        currentItems.addAll(entry.getStockEntryItems());

        txtRemarks.setText(entry.getRemarks());

        calculateAvailableMetalStock();
        displayMetalStock();

        // Rebuild item autocomplete to show only matching metal types
        rebuildItemAutocomplete();

        updateSummary();
    }

    private void clearPurchaseInvoiceSelection() {
        selectedPurchaseInvoice = null;
        invoiceSearchField.clear();
        lblSupplierName.setText("-");
        lblInvoiceDate.setText("-");
        metalStockPanel.setManaged(false);
        metalStockPanel.setVisible(false);

        // Disable item search field when no invoice is selected
        itemSearchField.getTextField().setDisable(true);
        itemSearchField.setPromptText("Select purchase invoice first...");
    }

    private void clearItemSelection() {
        log.debug("clearItemSelection called");
        selectedJewelryItem = null;
        itemSearchField.clear();
        lblItemName.setText("-");
        lblItemCategory.setText("-");
        lblItemMetal.setText("-");
        lblItemWeight.setText("-");
        txtQuantity.setText("1");
        txtItemRemarks.clear();
        itemsTable.getSelectionModel().clearSelection();
        log.debug("Item selection cleared");
    }

    private void updateSummary() {
        int totalItems = currentItems.stream().mapToInt(StockEntryItem::getQuantity).sum();
        BigDecimal totalGrossWeight = currentItems.stream()
                .map(StockEntryItem::getTotalGrossWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNetWeight = currentItems.stream()
                .map(StockEntryItem::getTotalNetWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalItems.setText(String.valueOf(totalItems));
        lblTotalGrossWeight.setText(WeightFormatter.format(totalGrossWeight));
        lblTotalNetWeight.setText(WeightFormatter.format(totalNetWeight));
        lblItemCount.setText(currentItems.size() + " items");

        // Update metal consumption
        updateMetalConsumption();
    }

    private void updateMetalConsumption() {
        consumedMetalStock.clear();

        for (StockEntryItem item : currentItems) {
            JewelryItem jewelry = item.getJewelryItem();

            // Use consistent metal key generation
            String itemMetalKey = getMetalKey(jewelry.getMetal(),
                    jewelry.getMetalType(),
                    jewelry.getPurity());

            // Add to consumed metal
            BigDecimal consumed = consumedMetalStock.getOrDefault(itemMetalKey, BigDecimal.ZERO);
            consumed = consumed.add(item.getTotalNetWeight());
            consumedMetalStock.put(itemMetalKey, consumed);
        }

        // Display consumption
        displayMetalConsumption();
    }

    private void displayMetalConsumption() {
        metalConsumptionList.getChildren().clear();

        if (consumedMetalStock.isEmpty()) {
            metalConsumptionPanel.setManaged(false);
            metalConsumptionPanel.setVisible(false);
            return;
        }

        metalConsumptionPanel.setManaged(true);
        metalConsumptionPanel.setVisible(true);

        for (Map.Entry<String, BigDecimal> entry : consumedMetalStock.entrySet()) {
            BigDecimal available = availableMetalStock.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal consumed = entry.getValue();
            BigDecimal remaining = available.subtract(consumed);

            VBox vbox = new VBox(4);
            // Display human-readable metal name
            String displayName = getMetalDisplayName(entry.getKey());
            Label metalLabel = new Label(displayName);
            metalLabel.setStyle("-fx-font-family: 'Segoe UI Semibold'; -fx-font-size: 11px; -fx-text-fill: #2E7D32;");

            Label detailsLabel = new Label(String.format("Used: %s / %s",
                    WeightFormatter.format(consumed),
                    WeightFormatter.format(available)));
            detailsLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 10px; -fx-text-fill: #616161;");

            Label remainingLabel = new Label("Remaining: " + WeightFormatter.format(remaining));
            remainingLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 10px; -fx-text-fill: " +
                    (remaining.compareTo(BigDecimal.ZERO) >= 0 ? "#2E7D32" : "#C62828") + ";");

            vbox.getChildren().addAll(metalLabel, detailsLabel, remainingLabel);
            metalConsumptionList.getChildren().add(vbox);
        }
    }

    /**
     * Generate consistent metal key for matching
     * Uses Metal entity ID if available, otherwise creates key from metalType and purity
     * This mirrors the logic in StockEntryService
     */
    private String getMetalKey(Metal metal, String metalType, BigDecimal purity) {
        String key;
        if (metal != null && metal.getId() != null) {
            // Use Metal ID as key for exact matching
            key = "M-" + metal.getId();
            log.info("Generated metal key from Metal entity: {} (Metal ID: {}, Name: {})",
                    key, metal.getId(), metal.getMetalName());
        } else {
            // Fallback to denormalized fields with normalized purity
            String normalizedPurity = purity.stripTrailingZeros().toPlainString();
            key = metalType + " " + normalizedPurity;
            log.info("Generated metal key from denormalized fields: {} (metalType: '{}', purity: {} -> normalized: '{}')",
                    key, metalType, purity, normalizedPurity);
        }
        return key;
    }

    /**
     * Get display name for metal key
     * Converts metal key to human-readable format
     */
    private String getMetalDisplayName(String metalKey) {
        if (metalKey.startsWith("M-")) {
            // Extract Metal ID and look up the metal
            try {
                Long metalId = Long.parseLong(metalKey.substring(2));
                Optional<Metal> metalOpt = metalService.getMetalById(metalId);
                if (metalOpt.isPresent()) {
                    Metal metal = metalOpt.get();
                    return metal.getMetalName(); // e.g., "Gold 22K"
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid metal key format: {}", metalKey);
            }
        }
        // Return the key as-is if it's in "Type Purity" format
        return metalKey;
    }
}
