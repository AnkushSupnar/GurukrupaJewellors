package com.gurukrupa.controller.transaction;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.data.service.BillPdfService;
import com.gurukrupa.data.service.CustomerService;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class ViewBillsController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(ViewBillsController.class);
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private BillService billService;
    
    @Autowired
    private BillPdfService billPdfService;
    
    @Autowired
    private AlertNotification alertNotification;
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @FXML private Button btnClose;
    @FXML private TextField txtCustomerName;
    @FXML private TextField txtMobileNo;
    @FXML private Button btnSearchCustomer;
    @FXML private Button btnClearSearch;
    @FXML private HBox customerInfoBox;
    @FXML private Label lblCustomerName;
    @FXML private Label lblCustomerMobile;
    @FXML private Label lblPendingAmount;
    
    @FXML private RadioButton rbAllBills;
    @FXML private RadioButton rbToday;
    @FXML private RadioButton rbThisWeek;
    @FXML private RadioButton rbThisMonth;
    @FXML private RadioButton rbDateRange;
    @FXML private HBox dateRangeBox;
    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;
    @FXML private Button btnApplyFilter;
    @FXML private Button btnRefresh;
    @FXML private Label lblBillCount;
    
    @FXML private TableView<Bill> billsTable;
    @FXML private TableColumn<Bill, String> colBillNo;
    @FXML private TableColumn<Bill, String> colBillDate;
    @FXML private TableColumn<Bill, String> colCustomerName;
    @FXML private TableColumn<Bill, String> colMobile;
    @FXML private TableColumn<Bill, BigDecimal> colTotalAmount;
    @FXML private TableColumn<Bill, BigDecimal> colPaidAmount;
    @FXML private TableColumn<Bill, BigDecimal> colPendingAmount;
    @FXML private TableColumn<Bill, String> colPaymentType;
    @FXML private TableColumn<Bill, String> colStatus;
    @FXML private TableColumn<Bill, Void> colActions;
    
    @FXML private HBox summaryBox;
    @FXML private Label lblTotalBills;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblTotalPaid;
    @FXML private Label lblTotalPending;
    
    private Customer selectedCustomer;
    private final ObservableList<Bill> billsList = FXCollections.observableArrayList();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private SuggestionProvider<String> customerNameProvider;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupEventHandlers();
        setupTable();
        setupDateFilter();
        setupCustomerAutocomplete();
    }
    
    private void setupEventHandlers() {
        btnClose.setOnAction(e -> closeDialog());
        btnSearchCustomer.setOnAction(e -> searchCustomer());
        btnClearSearch.setOnAction(e -> clearSearch());
        btnRefresh.setOnAction(e -> refreshBills());
        btnApplyFilter.setOnAction(e -> applyDateFilter());
        
        // Enter key support for search
        txtCustomerName.setOnAction(e -> searchCustomer());
        txtMobileNo.setOnAction(e -> searchCustomer());
    }
    
    private void setupCustomerAutocomplete() {
        // Initialize customer name autocomplete
        List<String> customerNames = customerService.getAllCustomerFullNames();
        customerNameProvider = SuggestionProvider.create(customerNames);
        new AutoCompletionTextFieldBinding<>(txtCustomerName, customerNameProvider);
    }
    
    private void setupTable() {
        billsTable.setItems(billsList);
        
        // Configure columns
        colBillNo.setCellValueFactory(new PropertyValueFactory<>("billNumber"));
        colBillNo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1976D2;");
                }
            }
        });
        
        colBillDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getBillDate();
            return new SimpleStringProperty(date != null ? dateTimeFormatter.format(date) : "");
        });
        
        colCustomerName.setCellValueFactory(cellData -> {
            Customer customer = cellData.getValue().getCustomer();
            return new SimpleStringProperty(customer != null ? customer.getCustomerFullName() : "");
        });
        
        colMobile.setCellValueFactory(cellData -> {
            Customer customer = cellData.getValue().getCustomer();
            return new SimpleStringProperty(customer != null ? customer.getMobile() : "");
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
        
        colPendingAmount.setCellValueFactory(new PropertyValueFactory<>("pendingAmount"));
        colPendingAmount.setCellFactory(col -> new TableCell<>() {
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
            Bill.PaymentMethod method = cellData.getValue().getPaymentMethod();
            return new SimpleStringProperty(method != null ? method.toString() : "");
        });
        
        colStatus.setCellValueFactory(cellData -> {
            Bill.BillStatus status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.toString() : "");
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
            public TableCell<Bill, Void> call(final TableColumn<Bill, Void> param) {
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
                        viewBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 4; -fx-cursor: hand;");
                        viewBtn.setTooltip(new Tooltip("View Bill Details"));
                        
                        // Edit button
                        FontAwesomeIcon editIcon = new FontAwesomeIcon();
                        editIcon.setGlyphName("EDIT");
                        editIcon.setSize("1.2em");
                        editIcon.setFill(Color.WHITE);
                        editBtn.setGraphic(editIcon);
                        editBtn.setStyle("-fx-background-color: #F57C00; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 4; -fx-cursor: hand;");
                        editBtn.setTooltip(new Tooltip("Edit Bill"));
                        
                        // Print button
                        FontAwesomeIcon printIcon = new FontAwesomeIcon();
                        printIcon.setGlyphName("PRINT");
                        printIcon.setSize("1.2em");
                        printIcon.setFill(Color.WHITE);
                        printBtn.setGraphic(printIcon);
                        printBtn.setStyle("-fx-background-color: #388E3C; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 4; -fx-cursor: hand;");
                        printBtn.setTooltip(new Tooltip("Print Bill"));
                        
                        viewBtn.setOnAction(event -> {
                            Bill bill = getTableView().getItems().get(getIndex());
                            viewBillDetails(bill);
                        });
                        
                        editBtn.setOnAction(event -> {
                            Bill bill = getTableView().getItems().get(getIndex());
                            editBill(bill);
                        });
                        
                        printBtn.setOnAction(event -> {
                            Bill bill = getTableView().getItems().get(getIndex());
                            printBill(bill);
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
        rbAllBills.selectedProperty().addListener((obs, oldVal, newVal) -> {
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
    
    private void searchCustomer() {
        String name = txtCustomerName.getText().trim();
        String mobile = txtMobileNo.getText().trim();
        
        if (name.isEmpty() && mobile.isEmpty()) {
            alertNotification.showError ( "Please enter customer name or mobile number");
            return;
        }
        
        Customer customer = null;
        
        try {
            // Search by mobile number first (exact match)
            if (!mobile.isEmpty()) {
                customer = customerService.findByMobile(mobile).orElse(null);
            }
            
            // If not found by mobile, search by name
            if (customer == null && !name.isEmpty()) {
                // Try to parse the full name
                String[] nameParts = name.split(" ");
                String fname = nameParts.length >= 1 ? nameParts[0] : "";
                String mname = nameParts.length >= 2 ? nameParts[1] : "";
                String lname = nameParts.length >= 3 ? nameParts[2] : "";
                
                customer = customerService.searchByFullName(fname, mname, lname).orElse(null);
            }
            
            if (customer != null) {
                selectedCustomer = customer;
                displayCustomerInfo();
                applyDateFilter();
            } else {
                alertNotification.showError( "No customer found with the given search criteria");
                clearCustomerInfo();
            }
            
        } catch (Exception e) {
            LOG.error("Error searching customer", e);
            alertNotification.showError( "Error searching customer: " + e.getMessage());
        }
    }
    
    private void clearSearch() {
        txtCustomerName.clear();
        txtMobileNo.clear();
        selectedCustomer = null;
        clearCustomerInfo();
        billsList.clear();
        updateSummary();
    }
    
    private void displayCustomerInfo() {
        if (selectedCustomer != null) {
            lblCustomerName.setText(selectedCustomer.getCustomerFullName());
            lblCustomerMobile.setText("Mobile: " + selectedCustomer.getMobile());
            
            // Calculate pending amount
            BigDecimal totalPending = billService.getTotalPendingAmountForCustomer(selectedCustomer.getId());
            lblPendingAmount.setText(currencyFormatter.format(totalPending != null ? totalPending : BigDecimal.ZERO));
            
            customerInfoBox.setVisible(true);
            customerInfoBox.setManaged(true);
        }
    }
    
    private void clearCustomerInfo() {
        customerInfoBox.setVisible(false);
        customerInfoBox.setManaged(false);
        summaryBox.setVisible(false);
        summaryBox.setManaged(false);
    }
    
    private void applyDateFilter() {
        if (selectedCustomer == null) {
            // If no customer selected, load all bills based on date filter
            loadAllBillsByDateFilter();
        } else {
            // Load bills for selected customer with date filter
            loadCustomerBillsByDateFilter();
        }
    }
    
    private void loadAllBillsByDateFilter() {
        try {
            List<Bill> bills;
            
            if (rbAllBills.isSelected()) {
                bills = billService.getAllBills();
            } else if (rbToday.isSelected()) {
                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
                bills = billService.findByDateRange(startOfDay, endOfDay);
            } else if (rbThisWeek.isSelected()) {
                LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDateTime startDateTime = startOfWeek.atStartOfDay();
                LocalDateTime endDateTime = LocalDate.now().atTime(23, 59, 59);
                bills = billService.findByDateRange(startDateTime, endDateTime);
            } else if (rbThisMonth.isSelected()) {
                LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
                LocalDateTime startDateTime = startOfMonth.atStartOfDay();
                LocalDateTime endDateTime = LocalDate.now().atTime(23, 59, 59);
                bills = billService.findByDateRange(startDateTime, endDateTime);
            } else if (rbDateRange.isSelected()) {
                LocalDate fromDate = dpFromDate.getValue();
                LocalDate toDate = dpToDate.getValue();
                
                if (fromDate == null || toDate == null) {
                    alertNotification.showError ( "Please select both from and to dates");
                    return;
                }
                
                if (fromDate.isAfter(toDate)) {
                    alertNotification.showError( "From date cannot be after to date");
                    return;
                }
                
                LocalDateTime fromDateTime = fromDate.atStartOfDay();
                LocalDateTime toDateTime = toDate.atTime(23, 59, 59);
                bills = billService.findByDateRange(fromDateTime, toDateTime);
            } else {
                bills = billService.getAllBills();
            }
            
            updateBillsList(bills);
        } catch (Exception e) {
            LOG.error("Error loading bills by date filter", e);
            alertNotification.showError( "Error loading bills: " + e.getMessage());
        }
    }
    
    private void loadCustomerBillsByDateFilter() {
        try {
            List<Bill> bills;
            
            if (rbAllBills.isSelected()) {
                bills = billService.findByCustomerId(selectedCustomer.getId());
            } else if (rbToday.isSelected()) {
                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
                bills = billService.findByCustomerIdAndDateRange(selectedCustomer.getId(), startOfDay, endOfDay);
            } else if (rbThisWeek.isSelected()) {
                LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDateTime startDateTime = startOfWeek.atStartOfDay();
                LocalDateTime endDateTime = LocalDate.now().atTime(23, 59, 59);
                bills = billService.findByCustomerIdAndDateRange(selectedCustomer.getId(), startDateTime, endDateTime);
            } else if (rbThisMonth.isSelected()) {
                LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
                LocalDateTime startDateTime = startOfMonth.atStartOfDay();
                LocalDateTime endDateTime = LocalDate.now().atTime(23, 59, 59);
                bills = billService.findByCustomerIdAndDateRange(selectedCustomer.getId(), startDateTime, endDateTime);
            } else if (rbDateRange.isSelected()) {
                LocalDate fromDate = dpFromDate.getValue();
                LocalDate toDate = dpToDate.getValue();
                
                if (fromDate == null || toDate == null) {
                    alertNotification.showError( "Please select both from and to dates");
                    return;
                }
                
                if (fromDate.isAfter(toDate)) {
                    alertNotification.showError( "From date cannot be after to date");
                    return;
                }
                
                LocalDateTime fromDateTime = fromDate.atStartOfDay();
                LocalDateTime toDateTime = toDate.atTime(23, 59, 59);
                bills = billService.findByCustomerIdAndDateRange(selectedCustomer.getId(), fromDateTime, toDateTime);
            } else {
                bills = billService.findByCustomerId(selectedCustomer.getId());
            }
            
            updateBillsList(bills);
        } catch (Exception e) {
            LOG.error("Error loading customer bills by date filter", e);
            alertNotification.showError( "Error loading bills: " + e.getMessage());
        }
    }
    
    private void updateBillsList(List<Bill> bills) {
        billsList.clear();
        billsList.addAll(bills);
        lblBillCount.setText("(" + bills.size() + " bills found)");
        updateSummary();
    }
    
    private void updateSummary() {
        if (billsList.isEmpty()) {
            summaryBox.setVisible(false);
            summaryBox.setManaged(false);
            return;
        }
        
        // Calculate summary
        int totalBills = billsList.size();
        BigDecimal totalAmount = billsList.stream()
            .map(Bill::getGrandTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = billsList.stream()
            .map(Bill::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending = billsList.stream()
            .map(Bill::getPendingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Update labels
        lblTotalBills.setText(String.valueOf(totalBills));
        lblTotalAmount.setText(currencyFormatter.format(totalAmount));
        lblTotalPaid.setText(currencyFormatter.format(totalPaid));
        lblTotalPending.setText(currencyFormatter.format(totalPending));
        
        summaryBox.setVisible(true);
        summaryBox.setManaged(true);
    }
    
    private void refreshBills() {
        // Add rotation animation
        RotateTransition rotation = new RotateTransition(Duration.millis(500), btnRefresh);
        rotation.setByAngle(360);
        rotation.play();
        
        applyDateFilter();
    }
    
    private void viewBillDetails(Bill bill) {
        // TODO: Implement view bill details dialog
        LOG.info("View bill details for bill: {}", bill.getBillNumber());
        alertNotification.showSuccess( "View bill functionality to be implemented");
    }
    
    private void editBill(Bill bill) {
        try {
            // Load the billing frame with the bill data for editing
            Stage billingStage = new Stage();
            billingStage.initModality(Modality.APPLICATION_MODAL);
            billingStage.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and get both the root and controller
            Map.Entry<Parent, BillingController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.BILLING.getFxmlFile(), BillingController.class);
            
            Parent root = entry.getKey();
            BillingController controller = entry.getValue();
            
            // Set the bill for editing
            controller.setEditMode(bill);
            controller.setDialogStage(billingStage);
            
            // Set up the dialog
            billingStage.setScene(new Scene(root));
            billingStage.setTitle("Edit Bill - " + bill.getBillNumber());
            billingStage.setMaximized(true);
            
            // Show the dialog and wait for it to close
            billingStage.showAndWait();
            
            // Refresh the bills list after editing
            refreshBills();
            
        } catch (IOException e) {
            LOG.error("Error opening bill for editing", e);
            alertNotification.showError( "Error opening bill for editing: " + e.getMessage());
        }
    }
    
    private void printBill(Bill bill) {
        try {
            // Create directory if not exists
            File billsDir = new File("bills");
            if (!billsDir.exists()) {
                billsDir.mkdirs();
            }
            
            // Generate filename
            String fileName = "Bill_" + bill.getBillNumber().replace("/", "_") + ".pdf";
            String filePath = billsDir.getPath() + File.separator + fileName;
            
            // Generate PDF
            billPdfService.generateBillPdf(bill, filePath);
            
            LOG.info("PDF generated at: {}", filePath);
            
            // Ask user if they want to open the PDF
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("PDF Generated");
            confirmAlert.setHeaderText("Bill PDF has been generated successfully!");
            confirmAlert.setContentText("Do you want to open the PDF file now?");
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Open the PDF file
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(new File(filePath));
                }
            }
            
        } catch (Exception e) {
            LOG.error("Error generating PDF", e);
            alertNotification.showError("Error generating PDF: " + e.getMessage());
        }
    }
    
    private void closeDialog() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
}