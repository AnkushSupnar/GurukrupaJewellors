package com.gurukrupa.controller.report;

import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.data.service.CustomerService;
import com.gurukrupa.utility.CurrencyFormatter;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.StageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class CustomerReportController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerReportController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @Autowired
    private AlertNotification alert;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private BillService billService;
    
    // Header Controls
    @FXML private Button btnBack;
    
    // Filter Controls
    @FXML private HBox customerSearchBox;
    private AutoCompleteTextField<Customer> customerAutoComplete;
    @FXML private TextField txtMobileSearch;
    @FXML private Button btnSearchCustomer;
    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;
    @FXML private ToggleButton btnAllStatus;
    @FXML private ToggleButton btnPaidStatus;
    @FXML private ToggleButton btnUnpaidStatus;
    @FXML private ToggleGroup statusToggleGroup;
    @FXML private Button btnGenerateReport;
    
    // Statistics Labels
    @FXML private Label lblTotalTransactions;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblPaidAmount;
    @FXML private Label lblPendingAmount;
    
    // Customer Details Card
    @FXML private VBox customerDetailsCard;
    @FXML private Label lblCustomerName;
    @FXML private Label lblCustomerMobile;
    @FXML private Label lblCustomerEmail;
    @FXML private Label lblCustomerAddress;
    
    // Transaction Table
    @FXML private TableView<Bill> tableTransactions;
    @FXML private TableColumn<Bill, String> colDate;
    @FXML private TableColumn<Bill, String> colInvoiceNumber;
    @FXML private TableColumn<Bill, String> colType;
    @FXML private TableColumn<Bill, String> colDescription;
    @FXML private TableColumn<Bill, String> colTotalAmount;
    @FXML private TableColumn<Bill, String> colPaidAmount;
    @FXML private TableColumn<Bill, String> colPendingAmount;
    @FXML private TableColumn<Bill, String> colStatus;
    @FXML private TableColumn<Bill, Void> colActions;
    @FXML private Button btnExportReport;
    
    // Data
    private ObservableList<Customer> customers = FXCollections.observableArrayList();
    private ObservableList<Bill> allBills = FXCollections.observableArrayList();
    private FilteredList<Bill> filteredBills;
    private Customer selectedCustomer = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing CustomerReportController");
        
        setupTableColumns();
        setupComboBoxes();
        setupDatePickers();
        setupToggleButtons();
        loadInitialData();
        setupTableSelectionListener();
        
        // Set default filter to "All Status"
        btnAllStatus.setSelected(true);
        
        // Set default date range (last 30 days)
        dpToDate.setValue(LocalDate.now());
        dpFromDate.setValue(LocalDate.now().minusDays(30));
        
        // Initially hide customer details card
        customerDetailsCard.setVisible(false);
        customerDetailsCard.setManaged(false);
    }
    
    private void setupTableColumns() {
        // Date column
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getBillDate().format(DATETIME_FORMATTER)));
        
        // Invoice Number column
        colInvoiceNumber.setCellValueFactory(new PropertyValueFactory<>("billNumber"));
        
        // Type column (hardcoded as "Sale" for now - can be enhanced)
        colType.setCellValueFactory(cellData -> new SimpleStringProperty("Sale"));
        
        // Description column
        colDescription.setCellValueFactory(cellData -> {
            Bill bill = cellData.getValue();
            int itemCount = bill.getBillTransactions() != null ? bill.getBillTransactions().size() : 0;
            return new SimpleStringProperty("Sale of " + itemCount + " items");
        });
        
        // Total Amount column
        colTotalAmount.setCellValueFactory(cellData -> 
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getGrandTotal())));
        
        // Paid Amount column
        colPaidAmount.setCellValueFactory(cellData -> 
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getPaidAmount())));
        
        // Pending Amount column
        colPendingAmount.setCellValueFactory(cellData -> 
            new SimpleStringProperty(CurrencyFormatter.format(cellData.getValue().getPendingAmount())));
        
        // Status column
        colStatus.setCellValueFactory(cellData -> {
            Bill bill = cellData.getValue();
            if (bill.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return new SimpleStringProperty("Paid");
            } else if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return new SimpleStringProperty("Unpaid");
            } else {
                return new SimpleStringProperty("Partial");
            }
        });
        
        // Actions column
        setupActionsColumn();
        
        // Setup filtered list
        filteredBills = new FilteredList<>(allBills, p -> true);
        tableTransactions.setItems(filteredBills);
    }
    
    private void setupActionsColumn() {
        colActions.setCellFactory(new Callback<TableColumn<Bill, Void>, TableCell<Bill, Void>>() {
            @Override
            public TableCell<Bill, Void> call(TableColumn<Bill, Void> param) {
                return new TableCell<Bill, Void>() {
                    private final Button btnView = new Button("View");
                    
                    {
                        btnView.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                                       "-fx-font-family: 'Segoe UI'; -fx-font-weight: 600; " +
                                       "-fx-padding: 4 12; -fx-background-radius: 4;");
                        btnView.setOnAction(event -> {
                            Bill bill = getTableView().getItems().get(getIndex());
                            viewBillDetails(bill);
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnView);
                        }
                    }
                };
            }
        });
    }
    
    private void setupComboBoxes() {
        // Setup customer autocomplete
        StringConverter<Customer> customerConverter = new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                if (customer == null) return "";
                return customer.getCustomerFullName() + " (" + customer.getMobile() + ")";
            }
            
            @Override
            public Customer fromString(String string) {
                return null; // Not needed for autocomplete
            }
        };
        
        customerAutoComplete = new AutoCompleteTextField<>(
            customers,
            customerConverter,
            searchText -> customers.stream()
                .filter(customer -> {
                    String fullDisplay = customerConverter.toString(customer).toLowerCase();
                    return fullDisplay.contains(searchText.toLowerCase());
                })
                .collect(java.util.stream.Collectors.toList())
        );
        
        customerAutoComplete.setPromptText("Search customer by name or mobile...");
        
        // Add the autocomplete component to the HBox
        customerSearchBox.getChildren().clear();
        customerSearchBox.getChildren().add(customerAutoComplete.getNode());
        
        // Customer selection listener
        customerAutoComplete.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedCustomer = newVal;
            if (newVal != null) {
                showCustomerDetails(newVal);
                txtMobileSearch.clear();
            } else {
                hideCustomerDetails();
            }
        });
    }
    
    private void setupDatePickers() {
        // Set date picker constraints
        dpFromDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && dpToDate.getValue() != null && newVal.isAfter(dpToDate.getValue())) {
                dpToDate.setValue(newVal);
            }
        });
        
        dpToDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && dpFromDate.getValue() != null && newVal.isBefore(dpFromDate.getValue())) {
                dpFromDate.setValue(newVal);
            }
        });
    }
    
    private void setupToggleButtons() {
        // Style active/inactive toggle buttons
        statusToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            styleToggleButtons();
        });
        
        styleToggleButtons();
    }
    
    private void styleToggleButtons() {
        String activeStyle = "-fx-background-color: %s; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
                           "-fx-font-weight: 600; -fx-padding: 8 16; -fx-background-radius: 6;";
        String inactiveStyle = "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: 'Segoe UI'; " +
                             "-fx-font-weight: 600; -fx-padding: 8 16; -fx-background-radius: 6;";
        
        if (btnAllStatus.isSelected()) {
            btnAllStatus.setStyle(String.format(activeStyle, "#1976D2"));
        } else {
            btnAllStatus.setStyle(String.format(inactiveStyle, "#E3F2FD", "#1976D2"));
        }
        
        if (btnPaidStatus.isSelected()) {
            btnPaidStatus.setStyle(String.format(activeStyle, "#388E3C"));
        } else {
            btnPaidStatus.setStyle(String.format(inactiveStyle, "#E8F5E9", "#388E3C"));
        }
        
        if (btnUnpaidStatus.isSelected()) {
            btnUnpaidStatus.setStyle(String.format(activeStyle, "#D32F2F"));
        } else {
            btnUnpaidStatus.setStyle(String.format(inactiveStyle, "#FFEBEE", "#D32F2F"));
        }
    }
    
    private void setupTableSelectionListener() {
        tableTransactions.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            // Can add row selection handling here if needed
        });
    }
    
    private void loadInitialData() {
        try {
            // Load customers
            List<Customer> customerList = customerService.getAllCustomers();
            customers.setAll(customerList);
            logger.info("Loaded {} customers", customerList.size());
            
            // Update autocomplete suggestions if already initialized
            if (customerAutoComplete != null) {
                customerAutoComplete.setSuggestions(customerList);
            }
            
        } catch (Exception e) {
            logger.error("Error loading initial data", e);
            alert.showError("Error loading data: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            logger.info("Navigating back to Report Menu");
            
            // Get the dashboard's center panel
            BorderPane dashboard = (BorderPane) btnBack.getScene().getRoot();
            
            // Load the Report Menu FXML
            Parent reportMenu = stageManager.getSpringFXMLLoader().load("/fxml/report/ReportMenu.fxml");
            
            // Set the report menu in the center of the dashboard
            dashboard.setCenter(reportMenu);
            
            logger.info("Report Menu loaded in dashboard successfully");
            
        } catch (Exception e) {
            logger.error("Error navigating back to Report Menu: {}", e.getMessage());
            e.printStackTrace();
            alert.showError("Error navigating back: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleSearchCustomer() {
        String mobileNumber = txtMobileSearch.getText().trim();
        if (mobileNumber.isEmpty()) {
            alert.showError("Please enter mobile number to search");
            return;
        }
        
        try {
            List<Customer> foundCustomers = customerService.findByMobileOrAlternativeMobile(mobileNumber);
            if (foundCustomers.isEmpty()) {
                alert.showError("No customer found with mobile number: " + mobileNumber);
                customerAutoComplete.setSelectedItem(null);
            } else if (foundCustomers.size() == 1) {
                Customer customer = foundCustomers.get(0);
                customerAutoComplete.setSelectedItem(customer);
                alert.showSuccess("Customer found: " + customer.getFirstName() + " " + customer.getLastName());
            } else {
                // Multiple customers found - update autocomplete suggestions
                customers.setAll(foundCustomers);
                customerAutoComplete.setSuggestions(foundCustomers);
                alert.showSuccess("Multiple customers found with this mobile number. Please select from suggestions.");
            }
        } catch (Exception e) {
            logger.error("Error searching customer by mobile", e);
            alert.showError("Error searching customer: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleGenerateReport() {
        try {
            logger.info("Generating customer report");
            
            // Validate date range
            LocalDate fromDate = dpFromDate.getValue();
            LocalDate toDate = dpToDate.getValue();
            
            if (fromDate == null || toDate == null) {
                alert.showError("Please select both from and to dates");
                return;
            }
            
            if (fromDate.isAfter(toDate)) {
                alert.showError("From date cannot be after to date");
                return;
            }
            
            // Load bills based on filters
            loadBillsData();
            
            // Apply filters
            applyFilters();
            
            // Update statistics
            updateStatistics();
            
            alert.showSuccess("Report generated successfully");
            
        } catch (Exception e) {
            logger.error("Error generating report", e);
            alert.showError("Error generating report: " + e.getMessage());
        }
    }
    
    private void loadBillsData() {
        try {
            LocalDateTime fromDateTime = dpFromDate.getValue().atStartOfDay();
            LocalDateTime toDateTime = dpToDate.getValue().atTime(23, 59, 59);
            
            List<Bill> bills;
            if (selectedCustomer != null) {
                bills = billService.findByCustomerIdAndDateRange(selectedCustomer.getId(), fromDateTime, toDateTime);
            } else {
                bills = billService.findByDateRange(fromDateTime, toDateTime);
            }
            
            allBills.setAll(bills);
            logger.info("Loaded {} bills for report", bills.size());
            
        } catch (Exception e) {
            logger.error("Error loading bills data", e);
            throw new RuntimeException("Failed to load bills data", e);
        }
    }
    
    private void applyFilters() {
        filteredBills.setPredicate(bill -> {
            // Payment status filter
            if (btnPaidStatus.isSelected()) {
                return bill.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0;
            } else if (btnUnpaidStatus.isSelected()) {
                return bill.getPendingAmount().compareTo(BigDecimal.ZERO) > 0;
            }
            // "All" is selected or no filter
            return true;
        });
    }
    
    private void updateStatistics() {
        List<Bill> currentBills = filteredBills.stream().collect(Collectors.toList());
        
        // Calculate statistics
        int totalTransactions = currentBills.size();
        BigDecimal totalAmount = currentBills.stream()
                .map(Bill::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = currentBills.stream()
                .map(Bill::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingAmount = currentBills.stream()
                .map(Bill::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Update labels
        lblTotalTransactions.setText(String.valueOf(totalTransactions));
        lblTotalAmount.setText(CurrencyFormatter.format(totalAmount));
        lblPaidAmount.setText(CurrencyFormatter.format(paidAmount));
        lblPendingAmount.setText(CurrencyFormatter.format(pendingAmount));
    }
    
    private void showCustomerDetails(Customer customer) {
        lblCustomerName.setText(customer.getCustomerFullName());
        lblCustomerMobile.setText(customer.getMobile());
        lblCustomerEmail.setText(customer.getAlternativeMobile() != null ? customer.getAlternativeMobile() : "-");
        
        String address = String.format("%s, %s, %s - %s", 
                customer.getCity() != null ? customer.getCity() : "",
                customer.getTaluka() != null ? customer.getTaluka() : "",
                customer.getDistrict() != null ? customer.getDistrict() : "",
                customer.getPinCode() != null ? customer.getPinCode() : "");
        lblCustomerAddress.setText(address);
        
        customerDetailsCard.setVisible(true);
        customerDetailsCard.setManaged(true);
    }
    
    private void hideCustomerDetails() {
        customerDetailsCard.setVisible(false);
        customerDetailsCard.setManaged(false);
    }
    
    @FXML
    private void handleExportReport() {
        try {
            // TODO: Implement export functionality (PDF/Excel)
            alert.showSuccess("Export functionality will be available soon!");
            
        } catch (Exception e) {
            logger.error("Error exporting report", e);
            alert.showError("Error exporting report: " + e.getMessage());
        }
    }
    
    private void viewBillDetails(Bill bill) {
        try {
            // TODO: Open bill details dialog or navigate to bill view
            alert.showSuccess("Bill details view will be available soon!");
            
        } catch (Exception e) {
            logger.error("Error viewing bill details", e);
            alert.showError("Error viewing bill details: " + e.getMessage());
        }
    }
}