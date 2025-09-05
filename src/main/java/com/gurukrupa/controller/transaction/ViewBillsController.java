package com.gurukrupa.controller.transaction;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.data.service.CustomerService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.animation.RotateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@Component
public class ViewBillsController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(ViewBillsController.class);
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private BillService billService;
    
    @FXML private Button btnClose;
    @FXML private TextField txtCustomerName;
    @FXML private TextField txtMobileNo;
    @FXML private Button btnSearchCustomer;
    @FXML private HBox customerInfoBox;
    @FXML private Label lblCustomerName;
    @FXML private Label lblCustomerMobile;
    @FXML private Label lblPendingAmount;
    
    @FXML private RadioButton rbAllBills;
    @FXML private RadioButton rbDateRange;
    @FXML private HBox dateRangeBox;
    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;
    @FXML private Button btnSearchBills;
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
    @FXML private TableColumn<Bill, Void> colActions;
    
    private Customer selectedCustomer;
    private final ObservableList<Bill> billsList = FXCollections.observableArrayList();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupEventHandlers();
        setupTable();
        setupDateFilter();
    }
    
    private void setupEventHandlers() {
        btnClose.setOnAction(e -> closeDialog());
        btnSearchCustomer.setOnAction(e -> searchCustomer());
        btnRefresh.setOnAction(e -> refreshBills());
        btnSearchBills.setOnAction(e -> searchBillsByDate());
        
        // Enter key support for search
        txtCustomerName.setOnAction(e -> searchCustomer());
        txtMobileNo.setOnAction(e -> searchCustomer());
    }
    
    private void setupTable() {
        billsTable.setItems(billsList);
        
        // Configure columns
        colBillNo.setCellValueFactory(new PropertyValueFactory<>("billNumber"));
        
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
        
        colTotalAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colTotalAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
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
                } else {
                    setText(currencyFormatter.format(item));
                    setStyle(item.compareTo(BigDecimal.ZERO) > 0 ? "-fx-text-fill: #D32F2F;" : "");
                }
            }
        });
        
        colPaymentType.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        
        // Action buttons column
        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Bill, Void> call(final TableColumn<Bill, Void> param) {
                return new TableCell<>() {
                    private final Button viewBtn = new Button("View");
                    private final Button printBtn = new Button("Print");
                    
                    {
                        viewBtn.setStyle("-fx-background-color: #1E88E5; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
                        printBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
                        
                        viewBtn.setOnAction(event -> {
                            Bill bill = getTableView().getItems().get(getIndex());
                            viewBillDetails(bill);
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
                            buttons.getChildren().addAll(viewBtn, printBtn);
                            setGraphic(buttons);
                        }
                    }
                };
            }
        });
    }
    
    private void setupDateFilter() {
        // Bind date range box to radio button selection
        dateRangeBox.disableProperty().bind(rbAllBills.selectedProperty());
        
        // Set default dates
        dpFromDate.setValue(LocalDate.now().minusDays(30));
        dpToDate.setValue(LocalDate.now());
        
        // Listen to radio button changes
        rbAllBills.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && selectedCustomer != null) {
                loadAllBillsForCustomer();
            }
        });
    }
    
    private void searchCustomer() {
        String name = txtCustomerName.getText().trim();
        String mobile = txtMobileNo.getText().trim();
        
        if (name.isEmpty() && mobile.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Search Criteria", "Please enter customer name or mobile number");
            return;
        }
        
        Customer customer = null;
        
        try {
            // Search by mobile number only
            if (name.isEmpty() && !mobile.isEmpty()) {
                customer = customerService.findByMobile(mobile).orElse(null);
            }
            // Search by name only
            else if (!name.isEmpty() && mobile.isEmpty()) {
                List<Customer> customers = customerService.searchByName(name);
                if (!customers.isEmpty()) {
                    customer = customers.get(0); // Take first match
                }
            }
            // Search by both
            else {
                customer = customerService.findByNameAndMobile(name, mobile).orElse(null);
            }
            
            if (customer != null) {
                selectedCustomer = customer;
                displayCustomerInfo();
                loadAllBillsForCustomer();
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Customer Not Found", "No customer found with the given search criteria");
            }
            
        } catch (Exception e) {
            LOG.error("Error searching customer", e);
            showAlert(Alert.AlertType.ERROR, "Search Error", "Error searching customer: " + e.getMessage());
        }
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
    
    private void loadAllBillsForCustomer() {
        if (selectedCustomer == null) return;
        
        try {
            List<Bill> bills = billService.findByCustomerId(selectedCustomer.getId());
            updateBillsList(bills);
        } catch (Exception e) {
            LOG.error("Error loading bills", e);
            showAlert(Alert.AlertType.ERROR, "Load Error", "Error loading bills: " + e.getMessage());
        }
    }
    
    private void searchBillsByDate() {
        if (selectedCustomer == null) {
            showAlert(Alert.AlertType.WARNING, "No Customer", "Please search and select a customer first");
            return;
        }
        
        LocalDate fromDate = dpFromDate.getValue();
        LocalDate toDate = dpToDate.getValue();
        
        if (fromDate == null || toDate == null) {
            showAlert(Alert.AlertType.WARNING, "Date Required", "Please select both from and to dates");
            return;
        }
        
        if (fromDate.isAfter(toDate)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Dates", "From date cannot be after to date");
            return;
        }
        
        try {
            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            LocalDateTime toDateTime = toDate.atTime(23, 59, 59);
            
            List<Bill> bills = billService.findByCustomerIdAndDateRange(
                selectedCustomer.getId(), fromDateTime, toDateTime);
            updateBillsList(bills);
        } catch (Exception e) {
            LOG.error("Error searching bills by date", e);
            showAlert(Alert.AlertType.ERROR, "Search Error", "Error searching bills: " + e.getMessage());
        }
    }
    
    private void updateBillsList(List<Bill> bills) {
        billsList.clear();
        billsList.addAll(bills);
        lblBillCount.setText("(" + bills.size() + " bills found)");
    }
    
    private void refreshBills() {
        // Add rotation animation
        RotateTransition rotation = new RotateTransition(Duration.millis(500), btnRefresh);
        rotation.setByAngle(360);
        rotation.play();
        
        if (selectedCustomer != null) {
            if (rbAllBills.isSelected()) {
                loadAllBillsForCustomer();
            } else {
                searchBillsByDate();
            }
        }
    }
    
    private void viewBillDetails(Bill bill) {
        // TODO: Implement view bill details dialog
        LOG.info("View bill details for bill: {}", bill.getBillNumber());
        showAlert(Alert.AlertType.INFORMATION, "View Bill", "View bill functionality to be implemented");
    }
    
    private void printBill(Bill bill) {
        // TODO: Implement print bill functionality
        LOG.info("Print bill: {}", bill.getBillNumber());
        showAlert(Alert.AlertType.INFORMATION, "Print Bill", "Print bill functionality to be implemented");
    }
    
    private void closeDialog() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}