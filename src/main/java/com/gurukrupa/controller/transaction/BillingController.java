package com.gurukrupa.controller.transaction;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.controller.master.CustomerController;
import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.service.CustomerService;
import com.gurukrupa.data.service.MetalService;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.data.service.JewelryItemService;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.data.service.BillTransactionService;
import com.gurukrupa.data.service.AppSettingsService;
import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import javafx.stage.Stage;
import java.util.List;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import java.math.BigDecimal;
import com.gurukrupa.data.service.BillPdfService;
import java.io.File;
import javafx.stage.FileChooser;
import java.awt.Desktop;

@Component
public class BillingController implements Initializable {
    @Autowired
    @Lazy
    private StageManager stageManager;
    @Autowired
    CustomerService customerService;
    @Autowired
    JewelryItemService jewelryItemService;
    @Autowired
    private MetalService metalService;
    @Autowired
    private BillService billService;
    @Autowired
    private BillTransactionService billTransactionService;
    @Autowired
    private AppSettingsService appSettingsService;
    @Autowired
    private BillPdfService billPdfService;
    @Autowired
    private AlertNotification alert;
    
    private Stage dialogStage;
    @FXML
    private Button btnAddCustomer, btnSearchCustomer, btnCloseFrame, btnAddItem, btnClearItem, 
                   btnSearchItem, btnSelectCustomer, btnGenerateBill, btnPrintInvoice, 
                   btnSaveDraft, btnAddExchange, btnClearExchange,
                   btnRefreshItems, btnNewBill, btnCloseBilling, btnClearSearch, btnRefreshCatalog,
                   btnCash, btnUPI, btnCard;
    @FXML
    private TextField txtCustomerName, txtMobileNo, txtCustomerAddress, txtItemCode, 
                      txtItemName, txtRate, txtQuantity, txtWeight, txtLabour, txtTotalAmount,
                      txtExchangeItemName, txtExchangeRate, txtExchangeWeight, 
                      txtExchangeDeduction, txtExchangeAmount;
    @FXML
    private ComboBox<String> cmbMetal, cmbExchangeMetal;
    @FXML
    private TableView itemsTable, exchangeTable;
    @FXML
    private Label lblBillAmount, lblExchangeAmount, lblFinalAmount, lblItemCount, lblSubtotal, lblCGST, lblSGST, lblGrandTotal;
    @FXML
    private TextField txtItemSearch;
    @FXML
    private ListView<String> listViewItems;
    @FXML
    private TextField txtDiscount, txtGSTRate;

    
    private ObservableList<JewelryItem> allJewelryItems = FXCollections.observableArrayList();
    private FilteredList<JewelryItem> filteredItems;
    private ObservableList<BillingItem> billingItems = FXCollections.observableArrayList();
    private ObservableList<ExchangeItem> exchangeItems = FXCollections.observableArrayList();
    private JewelryItem selectedJewelryItem;

    private AutoCompleteTextField autoCompleteTextField;
    private final List<String> customerNameSuggestions = new ArrayList<>();
    private final String[] filteredSuggestions = {""}; // hold currently filtered suggestions string, will update below
    private final List<String>[] filteredListHolder = new List[1]; // to hold filtered list
    private final int[] selectedIndex = {0}; // track current selected suggestion index
    private SuggestionProvider<String> customerNames;
    private Bill currentBill = null; // Store the current bill after generation
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize button actions
        btnAddCustomer.setOnAction(event -> addNewCustomer());
        btnSearchCustomer.setOnAction(e -> searchCustomer());
        btnCloseFrame.setOnAction(e -> closeFrame());
        btnAddItem.setOnAction(e -> addItem());
        btnClearItem.setOnAction(e -> clearItemFields());
        btnSearchItem.setOnAction(e -> searchItem());
        btnSelectCustomer.setOnAction(e -> selectCustomer());
        btnGenerateBill.setOnAction(e -> generateBill());
        btnPrintInvoice.setOnAction(e -> printInvoice());
        btnSaveDraft.setOnAction(e -> saveDraft());
        
        // Exchange section buttons
        btnAddExchange.setOnAction(e -> addExchange());
        btnClearExchange.setOnAction(e -> clearExchangeFields());
        
        // Navigation buttons
        btnRefreshItems.setOnAction(e -> refreshItemCatalog());
        btnNewBill.setOnAction(e -> newBill());
        btnCloseBilling.setOnAction(e -> closeFrame());
        
        // Payment method buttons
        btnCash.setOnAction(e -> processCashPayment());
        btnUPI.setOnAction(e -> processUPIPayment());
        btnCard.setOnAction(e -> processCardPayment());
        
        // Item catalog functionality
        btnClearSearch.setOnAction(e -> clearSearch());
        btnRefreshCatalog.setOnAction(e -> refreshItemCatalog());
        txtItemSearch.textProperty().addListener((obs, old, newVal) -> filterItems(newVal));
        listViewItems.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                selectItemFromCatalog();
            }
        });
        
        // Initialize customer autocomplete
        customerNameSuggestions.addAll(customerService.getAllCustomerFullNames());
        customerNames = SuggestionProvider.create(customerService.getAllCustomerFullNames());
        new AutoCompletionTextFieldBinding<>(txtCustomerName, customerNames);
        txtCustomerName.setOnAction(e -> {
            System.out.println("Action performed");
            btnSearchCustomer.fire();
        });
        
        // Initialize metal dropdowns from database
        loadMetalTypes();
        
        // Initialize field restrictions
        addTextLimiter(txtItemCode, 10);
        makeNumericOnly(txtRate, 10);
        makeIntegerOnly(txtQuantity, 5);
        makeNumericOnly(txtWeight, 10);
        makeNumericOnly(txtLabour, 10);
        
        // Exchange field restrictions
        makeNumericOnly(txtExchangeRate, 10);
        makeNumericOnly(txtExchangeWeight, 10);
        makeNumericOnly(txtExchangeDeduction, 10);
        
        // Payment field restrictions
        makeNumericOnly(txtDiscount, 10);
        makeNumericOnly(txtGSTRate, 5);
        
        // Auto-calculate total amount for billing
        txtRate.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        txtQuantity.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        txtWeight.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        txtLabour.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        
        // Auto-calculate exchange amount
        txtExchangeRate.textProperty().addListener((obs, old, newVal) -> calculateExchangeAmount());
        txtExchangeWeight.textProperty().addListener((obs, old, newVal) -> calculateExchangeAmount());
        txtExchangeDeduction.textProperty().addListener((obs, old, newVal) -> calculateExchangeAmount());
        
        // Auto-calculate totals when discount or GST rate changes
        txtDiscount.textProperty().addListener((obs, old, newVal) -> updateAllTotals());
        txtGSTRate.textProperty().addListener((obs, old, newVal) -> updateAllTotals());
        
        // Initialize tables
        initializeTables();
        
        // Load jewelry items
        loadJewelryItems();
        setupItemCatalog();
        
        // Refresh metal types when dialog is shown
        loadMetalTypes();
        
        // Load default GST rate from settings
        loadDefaultGstRate();
    }
    void searchCustomer(){
        System.out.println("searching customer");
        Customer customer = null;
        if(txtCustomerName.getText().isEmpty() && !txtMobileNo.getText().isEmpty()){
            customer = customerService.findByMobile (txtMobileNo.getText()).orElse(null);
        }
        if(!txtCustomerName.getText().isEmpty()) {
            System.out.println("search by name "+txtCustomerName.getText());
            String[] name = txtCustomerName.getText().split(" ");
            String fname="",mname="",lname="";
            if(name.length>=1)fname = name[0];
            if(name.length>=2)mname = name[1];
            if(name.length>=3)lname = name[2];

            if (!txtCustomerName.getText().isEmpty() && !txtMobileNo.getText().isEmpty()) {
                customer = customerService.searchByFullNameAndMobile(fname, mname, lname, txtMobileNo.getText()).orElse(null);
            }
            if (!txtCustomerName.getText().isEmpty() && txtMobileNo.getText().isEmpty()) {
                System.out.println("search by name "+fname+mname+lname);
                customer = customerService.searchByFullName(fname,mname,lname).orElse(null);
            }
        }
        if(customer==null){
            alert.showError("Customer Not found");
            return;
        }
        txtCustomerAddress.setText(customer.getCustomerAddress());
        txtCustomerName.setText(customer.getCustomerFullName());
        txtMobileNo.setText(customer.getMobile());
    }




    private void addNewCustomer() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stageManager.getPrimaryStage());

        try {
            // Load the FXML and get both the root and controller
            Map.Entry<Parent, CustomerController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.CUSTOMER_FORM.getFxmlFile(), CustomerController.class);

            Parent root = entry.getKey();
            CustomerController controller = entry.getValue();

            // Set up the dialog stage in controller
            controller.setDialogStage(dialog);

            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Add new customer");
            dialog.setResizable(false);

            // Show the dialog and wait for it to close
            dialog.showAndWait();

            // Handle the result after dialog closes
            if (controller.isSaved()) {
                System.out.println("Customer saved successfully!");
                // Refresh customer suggestions for auto-completion
                refreshCustomerSuggestions();
            } else {
                System.out.println("Customer addition was cancelled");
            }

        } catch (IOException e) {
            e.printStackTrace();
            // Show error message to user
        }
    }

    private void refreshCustomerSuggestions() {
        // Clear existing suggestions
        customerNameSuggestions.clear();
        
        // Reload customer names from database
        customerNameSuggestions.addAll(customerService.getAllCustomerFullNames());
        
        // Update the suggestion provider with new data
        customerNames = SuggestionProvider.create(customerService.getAllCustomerFullNames());
        
        // Recreate the auto-completion binding with updated suggestions
        new AutoCompletionTextFieldBinding<>(txtCustomerName, customerNames);
    }
    
    private void closeFrame() {
        Stage stage = (Stage) btnCloseFrame.getScene().getWindow();
        stage.close();
    }
    
    private void addItem() {
        try {
            // Validate required fields
            if (txtItemName.getText().trim().isEmpty()) {
                alert.showError("Please enter item name");
                return;
            }
            if (cmbMetal.getSelectionModel().isEmpty()) {
                alert.showError("Please select metal type");
                return;
            }
            if (txtRate.getText().trim().isEmpty()) {
                alert.showError("Please enter rate");
                return;
            }
            if (txtWeight.getText().trim().isEmpty()) {
                alert.showError("Please enter weight");
                return;
            }
            
            // Get values
            String itemCode = txtItemCode.getText().trim();
            String itemName = txtItemName.getText().trim();
            String metal = cmbMetal.getSelectionModel().getSelectedItem();
            double rate = Double.parseDouble(txtRate.getText());
            int quantity = txtQuantity.getText().isEmpty() ? 1 : Integer.parseInt(txtQuantity.getText());
            double weight = Double.parseDouble(txtWeight.getText());
            double labour = txtLabour.getText().isEmpty() ? 0 : Double.parseDouble(txtLabour.getText());
            double totalAmount = Double.parseDouble(txtTotalAmount.getText());
            
            // Add item to billing table
            int sno = billingItems.size() + 1;
            Long jewelryItemId = selectedJewelryItem != null ? selectedJewelryItem.getId() : null;
            BillingItem billingItem = new BillingItem(sno, jewelryItemId, itemCode, itemName, metal, rate, quantity, weight, labour, totalAmount);
            billingItems.add(billingItem);
            System.out.println("Adding item: " + itemName + ", Amount: " + totalAmount);
            
            // Update bill total
            updateBillTotal();
            
            // Clear fields after adding
            clearItemFields();
            
            alert.showSuccess("Item added successfully!");
            
        } catch (NumberFormatException e) {
            alert.showError("Please enter valid numbers for rate, weight, and labour");
        } catch (Exception e) {
            alert.showError("Error adding item: " + e.getMessage());
        }
    }
    
    private void clearItemFields() {
        txtItemCode.clear();
        txtItemName.clear();
        cmbMetal.getSelectionModel().clearSelection();
        txtRate.clear();
        txtQuantity.setText("1");
        txtWeight.clear();
        txtLabour.clear();
        txtTotalAmount.clear();
        selectedJewelryItem = null;
    }
    
    private void searchItem() {
        try {
            String itemCode = txtItemCode.getText().trim();
            String itemName = txtItemName.getText().trim();
            
            if (itemCode.isEmpty() && itemName.isEmpty()) {
                alert.showError("Please enter item code or name to search");
                return;
            }
            
            // Search by item code first
            if (!itemCode.isEmpty()) {
                jewelryItemService.findByItemCode(itemCode).ifPresentOrElse(
                    this::populateItemFields,
                    () -> alert.showError("Item not found with code: " + itemCode)
                );
            }
            // Search by item name if code search fails
            else if (!itemName.isEmpty()) {
                List<JewelryItem> items = jewelryItemService.searchByItemName(itemName);
                if (!items.isEmpty()) {
                    populateItemFields(items.get(0)); // Take first match
                } else {
                    alert.showError("No items found with name: " + itemName);
                }
            }
            
        } catch (Exception e) {
            alert.showError("Error searching item: " + e.getMessage());
        }
    }
    
    private void populateItemFields(JewelryItem item) {
        txtItemCode.setText(item.getItemCode());
        txtItemName.setText(item.getItemName());
        
        // Set metal type with detailed debugging
        if (item.getMetalType() != null) {
            String itemMetalType = item.getMetalType();
            System.out.println("Item metal type from database: '" + itemMetalType + "'");
            System.out.println("Available metals in dropdown: " + cmbMetal.getItems());
            
            boolean found = false;
            
            // First try exact match
            for (String metal : cmbMetal.getItems()) {
                if (metal.equals(itemMetalType)) {
                    cmbMetal.getSelectionModel().select(metal);
                    found = true;
                    System.out.println("Found exact match: " + metal);
                    break;
                }
            }
            
            // If no exact match, try case-insensitive match
            if (!found) {
                for (String metal : cmbMetal.getItems()) {
                    if (metal.equalsIgnoreCase(itemMetalType)) {
                        cmbMetal.getSelectionModel().select(metal);
                        found = true;
                        System.out.println("Found case-insensitive match: " + metal);
                        break;
                    }
                }
            }
            
            // If still no match, try contains
            if (!found) {
                for (String metal : cmbMetal.getItems()) {
                    // Check if dropdown metal contains item metal or vice versa
                    if (metal.toUpperCase().contains(itemMetalType.toUpperCase()) || 
                        itemMetalType.toUpperCase().contains(metal.split(" ")[0].toUpperCase())) {
                        cmbMetal.getSelectionModel().select(metal);
                        found = true;
                        System.out.println("Found partial match: " + metal);
                        break;
                    }
                }
            }
            
            if (!found) {
                System.out.println("No matching metal found in dropdown for: " + itemMetalType);
            }
        }
        
        // Set rate
        if (item.getGoldRate() != null) {
            txtRate.setText(item.getGoldRate().toString());
        }
        
        // Set weight - use net weight if available, otherwise gross weight
        if (item.getNetWeight() != null) {
            txtWeight.setText(item.getNetWeight().toString());
        } else if (item.getGrossWeight() != null) {
            txtWeight.setText(item.getGrossWeight().toString());
        }
        
        // Set labour charges
        if (item.getLabourCharges() != null) {
            txtLabour.setText(item.getLabourCharges().toString());
        }
        
        System.out.println("Item details populated: " + item.getItemName() + ", Metal selected: " + cmbMetal.getValue() + ", Weight: " + txtWeight.getText());
    }
    
    private void selectCustomer() {
        try {
            // Validate customer fields
            if (txtCustomerName.getText().trim().isEmpty() && txtMobileNo.getText().trim().isEmpty()) {
                alert.showError("Please enter customer name or mobile number to select");
                return;
            }
            
            // Use existing search customer functionality
            searchCustomer();
            
        } catch (Exception e) {
            alert.showError("Error selecting customer: " + e.getMessage());
        }
    }
    
    private void generateBill() {
        try {
            // Validate customer information
            if (txtCustomerName.getText().trim().isEmpty()) {
                alert.showError("Please enter customer information");
                return;
            }
            
            // Validate that we have items to bill
            if (billingItems.isEmpty()) {
                alert.showError("Please add items to generate bill");
                return;
            }
            
            // Create sale transactions from billing items
            List<BillTransaction> saleTransactions = new ArrayList<>();
            for (BillingItem item : billingItems) {
                BillTransaction transaction = billTransactionService.createSaleTransaction(
                    item.getJewelryItemId(),
                    item.getItemCode(),
                    item.getItemName(),
                    item.getMetal(),
                    item.getQuantity(),
                    new BigDecimal(item.getWeight()),
                    new BigDecimal(item.getRate()),
                    new BigDecimal(item.getLabour()),
                    null, // stone charges
                    null  // other charges
                );
                saleTransactions.add(transaction);
            }
            
            // Create exchange transactions from exchange items
            List<BillTransaction> exchangeTransactions = new ArrayList<>();
            for (ExchangeItem item : exchangeItems) {
                BillTransaction transaction = billTransactionService.createExchangeTransaction(
                    item.getItemName(),
                    item.getMetal(),
                        BigDecimal.valueOf(item.getWeight()),
                        BigDecimal.valueOf(item.getDeduction()),
                        BigDecimal.valueOf(item.getRate())
                );
                exchangeTransactions.add(transaction);
            }
            
            // Find customer by mobile number
            Customer customer = null;
            if (!txtMobileNo.getText().trim().isEmpty()) {
                Optional<Customer> customerOpt = customerService.findByMobile(txtMobileNo.getText().trim());
                if (customerOpt.isPresent()) {
                    customer = customerOpt.get();
                } else {
                    alert.showError("Customer not found. Please search and select a customer first.");
                    return;
                }
            } else {
                alert.showError("Please search and select a customer first.");
                return;
            }
            
            // Get payment details
            BigDecimal discount = txtDiscount.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(txtDiscount.getText());
            BigDecimal gstRate = txtGSTRate.getText().isEmpty() ? 
                new BigDecimal(appSettingsService.getDefaultGstRate().toString()) : 
                new BigDecimal(txtGSTRate.getText());
            
            // Create and save bill (without specifying payment method yet)
            Bill savedBill = billService.createBillFromTransaction(
                customer,
                saleTransactions,
                exchangeTransactions,
                discount,
                gstRate,
                null // Payment method will be set when user clicks payment button
            );
            
            System.out.println("Bill generated successfully with ID: " + savedBill.getId() + ", Bill Number: " + savedBill.getBillNumber());
            
            // Store the current bill for payment processing
            currentBill = savedBill;
            
            alert.showSuccess("Bill generated successfully! Bill Number: " + savedBill.getBillNumber() + "\nNow select a payment method.");
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error generating bill: " + e.getMessage());
        }
    }
    
    private void printInvoice() {
        try {
            // Validate if bill is generated
            if (currentBill == null) {
                alert.showError("Please generate and save the bill first by selecting a payment method.");
                return;
            }
            
            // Check if bill is paid
            if (currentBill.getStatus() != Bill.BillStatus.PAID) {
                alert.showError("Please complete the payment first by selecting a payment method.");
                return;
            }
            
            // Generate filename
            String fileName = "Bill_" + currentBill.getBillNumber().replace("/", "_") + ".pdf";
            File pdfFile = new File("bills" + File.separator + fileName);
            
            // Check if PDF file exists
            if (!pdfFile.exists()) {
                // Generate PDF if it doesn't exist
                billPdfService.generateBillPdf(currentBill, pdfFile.getPath());
            }
            
            // Open the PDF file
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
                System.out.println("Opening PDF: " + pdfFile.getPath());
            } else {
                alert.showError("Desktop operations are not supported on this system.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error opening invoice: " + e.getMessage());
        }
    }
    
    private void saveDraft() {
        try {
            // TODO: Implement actual draft saving to database
            System.out.println("Saving draft bill");
            alert.showSuccess("Draft saved successfully!");
            
        } catch (Exception e) {
            alert.showError("Error saving draft: " + e.getMessage());
        }
    }
    
    private void clearAll() {
        // Clear customer fields
        txtCustomerName.clear();
        txtMobileNo.clear();
        txtCustomerAddress.clear();
        
        // Clear item fields
        clearItemFields();
        
        // TODO: Clear tables
        System.out.println("Clear all data");
    }
    
    private void makeNumericOnly(TextField textField, int maxLength) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?$")) {
                textField.setText(oldValue);
            } else if (newValue.length() > maxLength) {
                textField.setText(oldValue);
            }
        });
    }
    
    private void makeIntegerOnly(TextField textField, int maxLength) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*$")) {
                textField.setText(oldValue);
            } else if (newValue.length() > maxLength) {
                textField.setText(oldValue);
            }
        });
    }
    
    private void addTextLimiter(TextField textField, int maxLength) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > maxLength) {
                textField.setText(oldValue);
            }
        });
    }
    
    private void calculateTotal() {
        try {
            double rate = txtRate.getText().isEmpty() ? 0 : Double.parseDouble(txtRate.getText());
            int quantity = txtQuantity.getText().isEmpty() ? 1 : Integer.parseInt(txtQuantity.getText());
            double weight = txtWeight.getText().isEmpty() ? 0 : Double.parseDouble(txtWeight.getText());
            double labour = txtLabour.getText().isEmpty() ? 0 : Double.parseDouble(txtLabour.getText());
            
            // Calculate total: ((rate * (weight * quantity)) / 10) + labour
            double totalWeight = weight * quantity;
            double goldValue = (rate * totalWeight) / 10;
            double total = goldValue + labour;
            txtTotalAmount.setText(String.format("%.2f", total));
        } catch (NumberFormatException e) {
            txtTotalAmount.setText("0.00");
        }
    }
    
    private void addExchange() {
        try {
            // Validate required fields
            if (txtExchangeItemName.getText().trim().isEmpty()) {
                alert.showError("Please enter exchange item name");
                return;
            }
            if (cmbExchangeMetal.getSelectionModel().isEmpty()) {
                alert.showError("Please select metal type");
                return;
            }
            if (txtExchangeRate.getText().trim().isEmpty()) {
                alert.showError("Please enter rate");
                return;
            }
            if (txtExchangeWeight.getText().trim().isEmpty()) {
                alert.showError("Please enter weight");
                return;
            }
            
            // Get values
            String itemName = txtExchangeItemName.getText().trim();
            String metal = cmbExchangeMetal.getSelectionModel().getSelectedItem();
            double rate = Double.parseDouble(txtExchangeRate.getText());
            double weight = Double.parseDouble(txtExchangeWeight.getText());
            double deduction = txtExchangeDeduction.getText().isEmpty() ? 0 : Double.parseDouble(txtExchangeDeduction.getText());
            double amount = Double.parseDouble(txtExchangeAmount.getText());
            
            // Add item to exchange table
            int sno = exchangeItems.size() + 1;
            ExchangeItem exchangeItem = new ExchangeItem(sno, itemName, metal, rate, weight, deduction, amount);
            exchangeItems.add(exchangeItem);
            System.out.println("Adding exchange item: " + itemName + ", Amount: " + amount);
            
            // Update exchange total
            updateExchangeTotal();
            
            // Clear fields after adding
            clearExchangeFields();
            
            alert.showSuccess("Exchange item added successfully!");
            
        } catch (NumberFormatException e) {
            alert.showError("Please enter valid numbers for rate, weight, and deductions");
        } catch (Exception e) {
            alert.showError("Error adding exchange item: " + e.getMessage());
        }
    }
    
    private void clearExchangeFields() {
        txtExchangeItemName.clear();
        cmbExchangeMetal.getSelectionModel().clearSelection();
        txtExchangeRate.clear();
        txtExchangeWeight.clear();
        txtExchangeDeduction.clear();
        txtExchangeAmount.clear();
    }
    
    private void calculateExchangeAmount() {
        try {
            double rate = txtExchangeRate.getText().isEmpty() ? 0 : Double.parseDouble(txtExchangeRate.getText());
            double weight = txtExchangeWeight.getText().isEmpty() ? 0 : Double.parseDouble(txtExchangeWeight.getText());
            double deduction = txtExchangeDeduction.getText().isEmpty() ? 0 : Double.parseDouble(txtExchangeDeduction.getText());
            
            // Calculate exchange amount: (rate * (weight - deduction) / 10)
            double netWeight = Math.max(0, weight - deduction);
            double total = (rate * netWeight / 10);
            txtExchangeAmount.setText(String.format("%.2f", total));
        } catch (NumberFormatException e) {
            txtExchangeAmount.setText("0.00");
        }
    }
    
    private void updateExchangeTotal() {
        // Calculate total from exchange table
        double total = exchangeItems.stream().mapToDouble(ExchangeItem::getAmount).sum();
        lblExchangeAmount.setText(String.format("₹ %.2f", total));
        updateFinalAmount();
    }
    
    private void updateBillTotal() {
        // Calculate total from billing items table
        double total = billingItems.stream().mapToDouble(BillingItem::getAmount).sum();
        lblBillAmount.setText(String.format("₹ %.2f", total));
        updateFinalAmount();
    }
    
    private void updateAllTotals() {
        updateFinalAmount();
    }
    
    private void updateFinalAmount() {
        try {
            // Get bill amount (remove currency symbol and parse)
            String billText = lblBillAmount.getText().replace("₹ ", "").replace(",", "");
            double billAmount = billText.isEmpty() || billText.equals("0.00") ? 0 : Double.parseDouble(billText);
            
            // Get exchange amount
            String exchangeText = lblExchangeAmount.getText().replace("₹ ", "").replace(",", "");
            double exchangeAmount = exchangeText.isEmpty() || exchangeText.equals("0.00") ? 0 : Double.parseDouble(exchangeText);
            
            // Calculate subtotal = Bill amount - Exchange amount
            double subtotal = billAmount - exchangeAmount;
            lblFinalAmount.setText(String.format("₹ %.2f", Math.max(0, subtotal)));
            lblSubtotal.setText(String.format("₹ %.2f", Math.max(0, subtotal)));
            
            // Get discount
            double discount = txtDiscount.getText().isEmpty() ? 0 : Double.parseDouble(txtDiscount.getText());
            
            // Calculate amount after discount
            double afterDiscount = subtotal - discount;
            
            // Get GST rate
            double gstRate = txtGSTRate.getText().isEmpty() ? 3 : Double.parseDouble(txtGSTRate.getText());
            
            // Calculate GST amounts (GST is split equally between CGST and SGST)
            double gstAmount = (billAmount * gstRate) / 100;
            double cgstAmount = gstAmount / 2;
            double sgstAmount = gstAmount / 2;
            
            // Update GST labels
            lblCGST.setText(String.format("₹ %.2f", cgstAmount));
            lblSGST.setText(String.format("₹ %.2f", sgstAmount));
            
            // Calculate grand total
            double grandTotal = afterDiscount + gstAmount;
            lblGrandTotal.setText(String.format("₹ %.2f", grandTotal));
            
        } catch (NumberFormatException e) {
            lblFinalAmount.setText("₹ 0.00");
            lblSubtotal.setText("₹ 0.00");
            lblCGST.setText("₹ 0.00");
            lblSGST.setText("₹ 0.00");
            lblGrandTotal.setText("₹ 0.00");
        }
    }
    
    private void refreshItemCatalog() {
        loadJewelryItems();
        System.out.println("Item catalog refreshed");
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    private void newBill() {
        // Clear all data and start fresh
        clearAllData();
        clearExchangeFields();
        lblBillAmount.setText("₹ 0.00");
        lblExchangeAmount.setText("₹ 0.00");
        lblFinalAmount.setText("₹ 0.00");
        lblSubtotal.setText("₹ 0.00");
        lblCGST.setText("₹ 0.00");
        lblSGST.setText("₹ 0.00");
        lblGrandTotal.setText("₹ 0.00");
        txtDiscount.setText("");
        txtGSTRate.setText("3");
        currentBill = null; // Reset current bill
        System.out.println("Started new bill");
    }
    
    private void clearAllData() {
        // Clear customer fields
        txtCustomerName.clear();
        txtMobileNo.clear();
        txtCustomerAddress.clear();
        
        // Clear item fields
        clearItemFields();
        
        // Clear tables
        billingItems.clear();
        exchangeItems.clear();
        
        // Reset payment fields
        txtDiscount.setText("");
        txtGSTRate.setText("3");
        
        System.out.println("All data cleared");
    }
    
    private void loadJewelryItems() {
        try {
            allJewelryItems.clear();
            allJewelryItems.addAll(jewelryItemService.getAllJewelryItems());
            System.out.println("Loaded " + allJewelryItems.size() + " jewelry items");
            updateItemCount();
        } catch (Exception e) {
            System.err.println("Error loading jewelry items: " + e.getMessage());
            alert.showError("Failed to load jewelry items: " + e.getMessage());
        }
    }
    
    private void setupItemCatalog() {
        // Create filtered list
        filteredItems = new FilteredList<>(allJewelryItems, item -> true);
        
        // Create observable list for ListView (item names only)
        ObservableList<String> itemNames = FXCollections.observableArrayList();
        
        // Update ListView when filtered items change
        filteredItems.addListener((javafx.collections.ListChangeListener<? super JewelryItem>) change -> {
            itemNames.clear();
            filteredItems.forEach(item -> itemNames.add(item.getItemName()));
            listViewItems.setItems(itemNames);
            updateItemCount();
        });
        
        // Initial setup
        filteredItems.forEach(item -> itemNames.add(item.getItemName()));
        listViewItems.setItems(itemNames);
        
        // Style ListView cells
        listViewItems.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-padding: 12; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-text-fill: #424242;");
                }
            }
        });
    }
    
    private void filterItems(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredItems.setPredicate(item -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase().trim();
            filteredItems.setPredicate(item -> 
                item.getItemName().toLowerCase().contains(lowerCaseFilter) ||
                item.getItemCode().toLowerCase().contains(lowerCaseFilter) ||
                (item.getCategory() != null && item.getCategory().toLowerCase().contains(lowerCaseFilter))
            );
        }
    }
    
    private void clearSearch() {
        txtItemSearch.clear();
    }
    
    private void selectItemFromCatalog() {
        String selectedItemName = listViewItems.getSelectionModel().getSelectedItem();
        if (selectedItemName != null) {
            // Find the jewelry item
            JewelryItem selectedItem = filteredItems.stream()
                .filter(item -> item.getItemName().equals(selectedItemName))
                .findFirst()
                .orElse(null);
            
            if (selectedItem != null) {
                // Store the selected jewelry item for later use in billing
                this.selectedJewelryItem = selectedItem;
                
                // Refresh metal types to ensure we have latest data
                loadMetalTypes();
                
                // Small delay to ensure dropdown is populated
                javafx.application.Platform.runLater(() -> {
                    // Populate billing fields using the existing method
                    populateItemFields(selectedItem);
                });
                
                System.out.println("Selected item: " + selectedItemName);
            }
        }
    }
    
    private void updateItemCount() {
        int count = filteredItems != null ? filteredItems.size() : allJewelryItems.size();
        lblItemCount.setText(count + " item" + (count != 1 ? "s" : "") + " found");
    }
    
    private void initializeTables() {
        // Initialize billing items table
        TableColumn<BillingItem, Integer> snoCol = (TableColumn<BillingItem, Integer>) itemsTable.getColumns().get(0);
        TableColumn<BillingItem, String> itemCodeCol = (TableColumn<BillingItem, String>) itemsTable.getColumns().get(1);
        TableColumn<BillingItem, String> itemNameCol = (TableColumn<BillingItem, String>) itemsTable.getColumns().get(2);
        TableColumn<BillingItem, String> metalCol = (TableColumn<BillingItem, String>) itemsTable.getColumns().get(3);
        TableColumn<BillingItem, Double> rateCol = (TableColumn<BillingItem, Double>) itemsTable.getColumns().get(4);
        TableColumn<BillingItem, Integer> quantityCol = (TableColumn<BillingItem, Integer>) itemsTable.getColumns().get(5);
        TableColumn<BillingItem, Double> weightCol = (TableColumn<BillingItem, Double>) itemsTable.getColumns().get(6);
        TableColumn<BillingItem, Double> labourCol = (TableColumn<BillingItem, Double>) itemsTable.getColumns().get(7);
        TableColumn<BillingItem, Double> amountCol = (TableColumn<BillingItem, Double>) itemsTable.getColumns().get(8);
        
        snoCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSno()).asObject());
        itemCodeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItemCode()));
        itemNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItemName()));
        metalCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMetal()));
        rateCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getRate()).asObject());
        quantityCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());
        weightCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getWeight()).asObject());
        labourCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getLabour()).asObject());
        amountCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
        
        itemsTable.setItems(billingItems);
        
        // Initialize exchange items table
        TableColumn<ExchangeItem, Integer> exSnoCol = (TableColumn<ExchangeItem, Integer>) exchangeTable.getColumns().get(0);
        TableColumn<ExchangeItem, String> exItemNameCol = (TableColumn<ExchangeItem, String>) exchangeTable.getColumns().get(1);
        TableColumn<ExchangeItem, String> exMetalCol = (TableColumn<ExchangeItem, String>) exchangeTable.getColumns().get(2);
        TableColumn<ExchangeItem, Double> exRateCol = (TableColumn<ExchangeItem, Double>) exchangeTable.getColumns().get(3);
        TableColumn<ExchangeItem, Double> exWeightCol = (TableColumn<ExchangeItem, Double>) exchangeTable.getColumns().get(4);
        TableColumn<ExchangeItem, Double> exDeductionCol = (TableColumn<ExchangeItem, Double>) exchangeTable.getColumns().get(5);
        TableColumn<ExchangeItem, Double> exAmountCol = (TableColumn<ExchangeItem, Double>) exchangeTable.getColumns().get(6);
        
        exSnoCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSno()).asObject());
        exItemNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItemName()));
        exMetalCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMetal()));
        exRateCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getRate()).asObject());
        exWeightCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getWeight()).asObject());
        exDeductionCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getDeduction()).asObject());
        exAmountCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
        
        exchangeTable.setItems(exchangeItems);
        
        System.out.println("Tables initialized with proper columns and data binding");
    }
    
    // Inner classes for table data models
    public static class BillingItem {
        private int sno;
        private Long jewelryItemId;
        private String itemCode;
        private String itemName;
        private String metal;
        private double rate;
        private int quantity;
        private double weight;
        private double labour;
        private double amount;
        
        public BillingItem(int sno, Long jewelryItemId, String itemCode, String itemName, String metal, 
                          double rate, int quantity, double weight, double labour, double amount) {
            this.sno = sno;
            this.jewelryItemId = jewelryItemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.metal = metal;
            this.rate = rate;
            this.quantity = quantity;
            this.weight = weight;
            this.labour = labour;
            this.amount = amount;
        }
        
        // Getters
        public int getSno() { return sno; }
        public Long getJewelryItemId() { return jewelryItemId; }
        public String getItemCode() { return itemCode; }
        public String getItemName() { return itemName; }
        public String getMetal() { return metal; }
        public double getRate() { return rate; }
        public int getQuantity() { return quantity; }
        public double getWeight() { return weight; }
        public double getLabour() { return labour; }
        public double getAmount() { return amount; }
    }
    
    public static class ExchangeItem {
        private int sno;
        private String itemName;
        private String metal;
        private double rate;
        private double weight;
        private double deduction;
        private double amount;
        
        public ExchangeItem(int sno, String itemName, String metal, double rate, 
                           double weight, double deduction, double amount) {
            this.sno = sno;
            this.itemName = itemName;
            this.metal = metal;
            this.rate = rate;
            this.weight = weight;
            this.deduction = deduction;
            this.amount = amount;
        }
        
        // Getters
        public int getSno() { return sno; }
        public String getItemName() { return itemName; }
        public String getMetal() { return metal; }
        public double getRate() { return rate; }
        public double getWeight() { return weight; }
        public double getDeduction() { return deduction; }
        public double getAmount() { return amount; }
    }
    
    private void loadMetalTypes() {
        try {
            // Get metal names from Metal table
            List<String> metalNames = metalService.getAllMetalNames();
            ObservableList<String> metalTypes = FXCollections.observableArrayList(metalNames);
            cmbMetal.setItems(metalTypes);
            cmbExchangeMetal.setItems(metalTypes);
            
            System.out.println("Loaded " + metalTypes.size() + " metal types from database: " + metalTypes);
        } catch (Exception e) {
            System.err.println("Error loading metal types: " + e.getMessage());
            alert.showError("Failed to load metal types: " + e.getMessage());
        }
    }
    
    private void loadDefaultGstRate() {
        try {
            // Initialize settings if they don't exist
            appSettingsService.initializeDefaultSettings();
            
            // Load default GST rate from settings
            Double defaultGstRate = appSettingsService.getDefaultGstRate();
            txtGSTRate.setText(String.format("%.2f", defaultGstRate));
            
            System.out.println("Loaded default GST rate: " + defaultGstRate + "%");
        } catch (Exception e) {
            System.err.println("Error loading default GST rate: " + e.getMessage());
            // Fallback to 3.00 if settings can't be loaded
            txtGSTRate.setText("3.00");
        }
    }
    
    private void processCashPayment() {
        try {
            if (currentBill == null) {
                alert.showError("Please generate the bill first before processing payment.");
                return;
            }
            
            // Update bill payment method to CASH
            currentBill.setPaymentMethod(Bill.PaymentMethod.CASH);
            currentBill.setStatus(Bill.BillStatus.PAID);
            
            // Save the updated bill
            Bill updatedBill = billService.saveBill(currentBill);
            
            // Generate PDF
            generateAndSavePdf(updatedBill);
            
            // Show success message
            alert.showSuccess("Cash payment processed successfully!\nBill saved and PDF generated.");
            
            // Reset for new bill
            newBill();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error processing cash payment: " + e.getMessage());
        }
    }
    
    private void processUPIPayment() {
        try {
            if (currentBill == null) {
                alert.showError("Please generate the bill first before processing payment.");
                return;
            }
            
            // Update bill payment method to UPI
            currentBill.setPaymentMethod(Bill.PaymentMethod.UPI);
            currentBill.setStatus(Bill.BillStatus.PAID);
            
            // Save the updated bill
            Bill updatedBill = billService.saveBill(currentBill);
            
            // Generate PDF
            generateAndSavePdf(updatedBill);
            
            // Show success message
            alert.showSuccess("UPI payment processed successfully!\nBill saved and PDF generated.");
            
            // Reset for new bill
            newBill();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error processing UPI payment: " + e.getMessage());
        }
    }
    
    private void processCardPayment() {
        try {
            if (currentBill == null) {
                alert.showError("Please generate the bill first before processing payment.");
                return;
            }
            
            // Update bill payment method to CARD
            currentBill.setPaymentMethod(Bill.PaymentMethod.CARD);
            currentBill.setStatus(Bill.BillStatus.PAID);
            
            // Save the updated bill
            Bill updatedBill = billService.saveBill(currentBill);
            
            // Generate PDF
            generateAndSavePdf(updatedBill);
            
            // Show success message
            alert.showSuccess("Card payment processed successfully!\nBill saved and PDF generated.");
            
            // Reset for new bill
            newBill();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error processing card payment: " + e.getMessage());
        }
    }
    
    private void generateAndSavePdf(Bill bill) {
        try {
            // Create bills directory if it doesn't exist
            File billsDir = new File("bills");
            if (!billsDir.exists()) {
                billsDir.mkdirs();
            }
            
            // Generate filename
            String fileName = "Bill_" + bill.getBillNumber().replace("/", "_") + ".pdf";
            String filePath = billsDir.getPath() + File.separator + fileName;
            
            // Generate PDF
            billPdfService.generateBillPdf(bill, filePath);
            
            System.out.println("PDF generated at: " + filePath);
            
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
            e.printStackTrace();
            alert.showError("Error generating PDF: " + e.getMessage());
        }
    }
}
