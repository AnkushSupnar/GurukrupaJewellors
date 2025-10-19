package com.gurukrupa.controller.transaction;

import com.gurukrupa.controller.purchase.PurchaseInvoiceController;
import com.gurukrupa.data.entities.PurchaseInvoice;
import com.gurukrupa.data.entities.Supplier;
import com.gurukrupa.data.service.PurchaseInvoiceService;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.animation.RotateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class ViewPurchasesController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(ViewPurchasesController.class);
    
    @Autowired
    private PurchaseInvoiceService purchaseInvoiceService;
    
    @Autowired
    private AlertNotification alertNotification;
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @FXML private Button btnClose;
    @FXML private TextField txtSupplierName;
    @FXML private TextField txtInvoiceNo;
    @FXML private Button btnSearchSupplier;
    @FXML private Button btnClearSearch;
    @FXML private HBox supplierInfoBox;
    @FXML private Label lblSupplierName;
    @FXML private Label lblSupplierContact;
    @FXML private Label lblTotalPurchases;
    
    @FXML private RadioButton rbAllPurchases;
    @FXML private RadioButton rbToday;
    @FXML private RadioButton rbThisWeek;
    @FXML private RadioButton rbThisMonth;
    @FXML private RadioButton rbDateRange;
    @FXML private HBox dateRangeBox;
    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;
    @FXML private Button btnApplyFilter;
    @FXML private Button btnRefresh;
    @FXML private Label lblPurchaseCount;
    
    @FXML private TableView<PurchaseInvoice> purchasesTable;
    @FXML private TableColumn<PurchaseInvoice, String> colInvoiceNo;
    @FXML private TableColumn<PurchaseInvoice, String> colInvoiceDate;
    @FXML private TableColumn<PurchaseInvoice, String> colSupplierName;
    @FXML private TableColumn<PurchaseInvoice, String> colSupplierContact;
    @FXML private TableColumn<PurchaseInvoice, BigDecimal> colTotalAmount;
    @FXML private TableColumn<PurchaseInvoice, BigDecimal> colPaidAmount;
    @FXML private TableColumn<PurchaseInvoice, BigDecimal> colPendingAmount;
    @FXML private TableColumn<PurchaseInvoice, String> colPaymentType;
    @FXML private TableColumn<PurchaseInvoice, String> colStatus;
    @FXML private TableColumn<PurchaseInvoice, Void> colActions;
    
    @FXML private HBox summaryBox;
    @FXML private Label lblTotalPurchasesCount;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblTotalPaid;
    @FXML private Label lblTotalPending;
    
    private String selectedSupplier;
    private final ObservableList<PurchaseInvoice> purchasesList = FXCollections.observableArrayList();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupEventHandlers();
        setupTable();
        setupDateFilter();
        loadAllPurchases();
    }
    
    private void setupEventHandlers() {
        btnClose.setOnAction(e -> closeDialog());
        btnSearchSupplier.setOnAction(e -> searchSupplier());
        btnClearSearch.setOnAction(e -> clearSearch());
        btnRefresh.setOnAction(e -> refreshPurchases());
        btnApplyFilter.setOnAction(e -> applyDateFilter());
        
        // Enter key support for search
        txtSupplierName.setOnAction(e -> searchSupplier());
        txtInvoiceNo.setOnAction(e -> searchSupplier());
    }
    
    private void setupTable() {
        purchasesTable.setItems(purchasesList);
        
        // Configure columns
        colInvoiceNo.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        colInvoiceNo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #F4511E;");
                }
            }
        });
        
        colInvoiceDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getInvoiceDate();
            return new SimpleStringProperty(date != null ? dateTimeFormatter.format(date) : "");
        });
        
        colSupplierName.setCellValueFactory(cellData -> {
            Supplier supplier = cellData.getValue().getSupplier();
            return new SimpleStringProperty(supplier != null ? supplier.getSupplierName() : "");
        });
        
        colSupplierContact.setCellValueFactory(cellData -> {
            Supplier supplier = cellData.getValue().getSupplier();
            return new SimpleStringProperty(supplier != null ? supplier.getMobile() : "");
        });
        
        colTotalAmount.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        colTotalAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
                    setStyle("-fx-font-weight: 600;");
                }
            }
        });
        
        colPaidAmount.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        colPaidAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
                    setStyle("-fx-text-fill: #388E3C;");
                }
            }
        });
        
        colPendingAmount.setCellValueFactory(cellData -> {
            PurchaseInvoice invoice = cellData.getValue();
            BigDecimal pending = (invoice.getGrandTotal() != null && invoice.getPaidAmount() != null) 
                ? invoice.getGrandTotal().subtract(invoice.getPaidAmount()) : BigDecimal.ZERO;
            return new javafx.beans.property.SimpleObjectProperty<>(pending);
        });
        
        colPendingAmount.setCellFactory(col -> new TableCell<PurchaseInvoice, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(currencyFormatter.format(item));
                    if (item.compareTo(BigDecimal.ZERO) > 0) {
                        setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: 600;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        colPaymentType.setCellValueFactory(cellData -> {
            PurchaseInvoice.PaymentMethod method = cellData.getValue().getPaymentMethod();
            return new SimpleStringProperty(method != null ? method.toString() : "");
        });
        
        colStatus.setCellValueFactory(cellData -> {
            PurchaseInvoice invoice = cellData.getValue();
            BigDecimal pending = (invoice.getGrandTotal() != null && invoice.getPaidAmount() != null) 
                ? invoice.getGrandTotal().subtract(invoice.getPaidAmount()) : BigDecimal.ZERO;
            
            if (invoice.getPaidAmount() == null || invoice.getPaidAmount().equals(BigDecimal.ZERO)) {
                return new SimpleStringProperty("PENDING");
            } else if (pending.compareTo(BigDecimal.ZERO) > 0) {
                return new SimpleStringProperty("PARTIAL");
            } else {
                return new SimpleStringProperty("PAID");
            }
        });
        
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(item);
                    statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 2 8 2 8; -fx-background-radius: 12;");
                    
                    switch (item) {
                        case "PAID":
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;");
                            break;
                        case "PENDING":
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FFEBEE; -fx-text-fill: #C62828;");
                            break;
                        case "PARTIAL":
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FFF3E0; -fx-text-fill: #EF6C00;");
                            break;
                        default:
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0;");
                    }
                    
                    setGraphic(statusLabel);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        // Action buttons column
        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<PurchaseInvoice, Void> call(final TableColumn<PurchaseInvoice, Void> param) {
                return new TableCell<>() {
                    private final Button viewBtn = new Button();
                    private final Button editBtn = new Button();
                    private final Button printBtn = new Button();
                    
                    {
                        // View button
                        FontAwesomeIcon viewIcon = new FontAwesomeIcon();
                        viewIcon.setGlyphName("EYE");
                        viewIcon.setSize("1.2em");
                        viewIcon.setFill(Color.WHITE);
                        viewBtn.setGraphic(viewIcon);
                        viewBtn.setStyle("-fx-background-color: #F4511E; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 4; -fx-cursor: hand;");
                        viewBtn.setTooltip(new Tooltip("View Purchase Details"));
                        
                        // Edit button
                        FontAwesomeIcon editIcon = new FontAwesomeIcon();
                        editIcon.setGlyphName("EDIT");
                        editIcon.setSize("1.2em");
                        editIcon.setFill(Color.WHITE);
                        editBtn.setGraphic(editIcon);
                        editBtn.setStyle("-fx-background-color: #F57C00; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 4; -fx-cursor: hand;");
                        editBtn.setTooltip(new Tooltip("Edit Purchase"));
                        
                        // Print button
                        FontAwesomeIcon printIcon = new FontAwesomeIcon();
                        printIcon.setGlyphName("PRINT");
                        printIcon.setSize("1.2em");
                        printIcon.setFill(Color.WHITE);
                        printBtn.setGraphic(printIcon);
                        printBtn.setStyle("-fx-background-color: #388E3C; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 4; -fx-cursor: hand;");
                        printBtn.setTooltip(new Tooltip("Print Purchase"));
                        
                        viewBtn.setOnAction(event -> {
                            PurchaseInvoice invoice = getTableView().getItems().get(getIndex());
                            viewPurchaseDetails(invoice);
                        });
                        
                        editBtn.setOnAction(event -> {
                            PurchaseInvoice invoice = getTableView().getItems().get(getIndex());
                            editPurchase(invoice);
                        });
                        
                        printBtn.setOnAction(event -> {
                            PurchaseInvoice invoice = getTableView().getItems().get(getIndex());
                            printPurchase(invoice);
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox buttons = new HBox(5);
                            buttons.setAlignment(Pos.CENTER);
                            buttons.getChildren().addAll(viewBtn, editBtn, printBtn);
                            setGraphic(buttons);
                        }
                    }
                };
            }
        });
    }
    
    private void setupDateFilter() {
        // Bind date range box to radio button selection
        dateRangeBox.disableProperty().bind(rbDateRange.selectedProperty().not());
        
        // Set default dates
        dpFromDate.setValue(LocalDate.now().minusDays(30));
        dpToDate.setValue(LocalDate.now());
        
        // Add listeners to radio buttons
        rbAllPurchases.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) applyDateFilter();
        });
        
        rbToday.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) applyDateFilter();
        });
        
        rbThisWeek.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) applyDateFilter();
        });
        
        rbThisMonth.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) applyDateFilter();
        });
    }
    
    private void searchSupplier() {
        String supplierName = txtSupplierName.getText().trim();
        String invoiceNo = txtInvoiceNo.getText().trim();
        
        if (supplierName.isEmpty() && invoiceNo.isEmpty()) {
            alertNotification.showError("Please enter supplier name or invoice number");
            return;
        }
        
        try {
            List<PurchaseInvoice> purchases;
            
            if (!invoiceNo.isEmpty()) {
                // Search by invoice number first
                Optional<PurchaseInvoice> invoiceOpt = purchaseInvoiceService.findByInvoiceNumber(invoiceNo);
                purchases = invoiceOpt.map(List::of).orElse(new ArrayList<>());
            } else {
                // Search by supplier name - filter from all invoices
                String searchName = supplierName.toLowerCase();
                purchases = purchaseInvoiceService.getAllInvoices().stream()
                    .filter(p -> p.getSupplier() != null &&
                                 p.getSupplier().getSupplierName() != null &&
                                 p.getSupplier().getSupplierName().toLowerCase().contains(searchName))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            if (!purchases.isEmpty()) {
                selectedSupplier = !purchases.isEmpty() && purchases.get(0).getSupplier() != null 
                    ? purchases.get(0).getSupplier().getSupplierName() : supplierName;
                displaySupplierInfo(purchases);
                updatePurchasesList(purchases);
            } else {
                alertNotification.showError("No purchases found with the given search criteria");
                clearSupplierInfo();
            }
            
        } catch (Exception e) {
            LOG.error("Error searching purchases", e);
            alertNotification.showError("Error searching purchases: " + e.getMessage());
        }
    }
    
    private void clearSearch() {
        txtSupplierName.clear();
        txtInvoiceNo.clear();
        selectedSupplier = null;
        clearSupplierInfo();
        loadAllPurchases();
    }
    
    private void displaySupplierInfo(List<PurchaseInvoice> purchases) {
        if (!purchases.isEmpty()) {
            PurchaseInvoice firstPurchase = purchases.get(0);
            Supplier supplier = firstPurchase.getSupplier();
            if (supplier != null) {
                lblSupplierName.setText(supplier.getSupplierName());
                lblSupplierContact.setText("Contact: " + (supplier.getMobile() != null ? supplier.getMobile() : "N/A"));
            } else {
                lblSupplierName.setText("Unknown Supplier");
                lblSupplierContact.setText("Contact: N/A");
            }
            
            // Calculate total purchases for this supplier
            BigDecimal totalPurchases = purchases.stream()
                .map(PurchaseInvoice::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTotalPurchases.setText(currencyFormatter.format(totalPurchases));
            
            supplierInfoBox.setVisible(true);
            supplierInfoBox.setManaged(true);
        }
    }
    
    private void clearSupplierInfo() {
        supplierInfoBox.setVisible(false);
        supplierInfoBox.setManaged(false);
        summaryBox.setVisible(false);
        summaryBox.setManaged(false);
    }
    
    private void loadAllPurchases() {
        try {
            List<PurchaseInvoice> purchases = purchaseInvoiceService.getAllInvoices();
            updatePurchasesList(purchases);
        } catch (Exception e) {
            LOG.error("Error loading all purchases", e);
            alertNotification.showError("Error loading purchases: " + e.getMessage());
        }
    }
    
    private void applyDateFilter() {
        try {
            List<PurchaseInvoice> purchases;
            
            if (rbAllPurchases.isSelected()) {
                if (selectedSupplier != null) {
                    // Filter by selected supplier name
                    String searchName = selectedSupplier.toLowerCase();
                    purchases = purchaseInvoiceService.getAllInvoices().stream()
                        .filter(p -> p.getSupplier() != null &&
                                     p.getSupplier().getSupplierName() != null &&
                                     p.getSupplier().getSupplierName().toLowerCase().contains(searchName))
                        .collect(java.util.stream.Collectors.toList());
                } else {
                    purchases = purchaseInvoiceService.getAllInvoices();
                }
            } else if (rbToday.isSelected()) {
                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
                purchases = purchaseInvoiceService.findByDateRange(startOfDay, endOfDay);
                if (selectedSupplier != null) {
                    purchases = purchases.stream()
                        .filter(p -> p.getSupplier() != null && selectedSupplier.equals(p.getSupplier().getSupplierName()))
                        .collect(java.util.stream.Collectors.toList());
                }
            } else if (rbThisWeek.isSelected()) {
                LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDateTime startDateTime = startOfWeek.atStartOfDay();
                LocalDateTime endDateTime = LocalDate.now().atTime(23, 59, 59);
                purchases = purchaseInvoiceService.findByDateRange(startDateTime, endDateTime);
                if (selectedSupplier != null) {
                    purchases = purchases.stream()
                        .filter(p -> p.getSupplier() != null && selectedSupplier.equals(p.getSupplier().getSupplierName()))
                        .collect(java.util.stream.Collectors.toList());
                }
            } else if (rbThisMonth.isSelected()) {
                LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
                LocalDateTime startDateTime = startOfMonth.atStartOfDay();
                LocalDateTime endDateTime = LocalDate.now().atTime(23, 59, 59);
                purchases = purchaseInvoiceService.findByDateRange(startDateTime, endDateTime);
                if (selectedSupplier != null) {
                    purchases = purchases.stream()
                        .filter(p -> p.getSupplier() != null && selectedSupplier.equals(p.getSupplier().getSupplierName()))
                        .collect(java.util.stream.Collectors.toList());
                }
            } else if (rbDateRange.isSelected()) {
                LocalDate fromDate = dpFromDate.getValue();
                LocalDate toDate = dpToDate.getValue();
                
                if (fromDate == null || toDate == null) {
                    alertNotification.showError("Please select both from and to dates");
                    return;
                }
                
                if (fromDate.isAfter(toDate)) {
                    alertNotification.showError("From date cannot be after to date");
                    return;
                }
                
                LocalDateTime fromDateTime = fromDate.atStartOfDay();
                LocalDateTime toDateTime = toDate.atTime(23, 59, 59);
                purchases = purchaseInvoiceService.findByDateRange(fromDateTime, toDateTime);
                if (selectedSupplier != null) {
                    purchases = purchases.stream()
                        .filter(p -> p.getSupplier() != null && selectedSupplier.equals(p.getSupplier().getSupplierName()))
                        .collect(java.util.stream.Collectors.toList());
                }
            } else {
                if (selectedSupplier != null) {
                    // Filter by selected supplier name
                    String searchName = selectedSupplier.toLowerCase();
                    purchases = purchaseInvoiceService.getAllInvoices().stream()
                        .filter(p -> p.getSupplier() != null &&
                                     p.getSupplier().getSupplierName() != null &&
                                     p.getSupplier().getSupplierName().toLowerCase().contains(searchName))
                        .collect(java.util.stream.Collectors.toList());
                } else {
                    purchases = purchaseInvoiceService.getAllInvoices();
                }
            }
            
            updatePurchasesList(purchases);
        } catch (Exception e) {
            LOG.error("Error applying date filter", e);
            alertNotification.showError("Error loading purchases: " + e.getMessage());
        }
    }
    
    private void updatePurchasesList(List<PurchaseInvoice> purchases) {
        purchasesList.clear();
        purchasesList.addAll(purchases);
        lblPurchaseCount.setText("(" + purchases.size() + " purchases found)");
        updateSummary();
    }
    
    private void updateSummary() {
        if (purchasesList.isEmpty()) {
            summaryBox.setVisible(false);
            summaryBox.setManaged(false);
            return;
        }
        
        // Calculate summary
        int totalPurchases = purchasesList.size();
        BigDecimal totalAmount = purchasesList.stream()
            .map(PurchaseInvoice::getGrandTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = purchasesList.stream()
            .map(p -> p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending = purchasesList.stream()
            .map(p -> {
                BigDecimal total = p.getGrandTotal() != null ? p.getGrandTotal() : BigDecimal.ZERO;
                BigDecimal paid = p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO;
                return total.subtract(paid);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Update labels
        lblTotalPurchasesCount.setText(String.valueOf(totalPurchases));
        lblTotalAmount.setText(currencyFormatter.format(totalAmount));
        lblTotalPaid.setText(currencyFormatter.format(totalPaid));
        lblTotalPending.setText(currencyFormatter.format(totalPending));
        
        summaryBox.setVisible(true);
        summaryBox.setManaged(true);
    }
    
    private void refreshPurchases() {
        // Add rotation animation
        RotateTransition rotation = new RotateTransition(Duration.millis(500), btnRefresh);
        rotation.setByAngle(360);
        rotation.play();
        
        applyDateFilter();
    }
    
    private void viewPurchaseDetails(PurchaseInvoice invoice) {
        LOG.info("View purchase details for invoice: {}", invoice.getInvoiceNumber());
        alertNotification.showSuccess("View purchase details functionality to be implemented");
    }
    
    private void editPurchase(PurchaseInvoice invoice) {
        try {
            // Load the purchase invoice frame with the invoice data for editing
            Stage purchaseStage = new Stage();
            purchaseStage.initModality(Modality.APPLICATION_MODAL);
            purchaseStage.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and get both the root and controller
            Map.Entry<Parent, PurchaseInvoiceController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.PURCHASE_INVOICE.getFxmlFile(), PurchaseInvoiceController.class);
            
            Parent root = entry.getKey();
            PurchaseInvoiceController controller = entry.getValue();

            // Load the invoice into the form for editing
            controller.loadInvoiceIntoForm(invoice);

            // Set up the dialog
            purchaseStage.setScene(new Scene(root));
            purchaseStage.setTitle("Edit Purchase Invoice - " + invoice.getInvoiceNumber());
            purchaseStage.setMaximized(true);
            
            // Show the dialog and wait for it to close
            purchaseStage.showAndWait();
            
            // Refresh the purchases list after editing
            refreshPurchases();
            
        } catch (IOException e) {
            LOG.error("Error opening purchase invoice for editing", e);
            alertNotification.showError("Error opening purchase invoice for editing: " + e.getMessage());
        }
    }
    
    private void printPurchase(PurchaseInvoice invoice) {
        LOG.info("Print purchase invoice: {}", invoice.getInvoiceNumber());
        alertNotification.showSuccess("Print purchase functionality to be implemented");
    }
    
    private void closeDialog() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
}