package com.gurukrupa.controller.transaction;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.controller.master.CustomerController;
import com.gurukrupa.customUI.AutoCompleteTextField;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.service.CustomerService;
import com.gurukrupa.data.service.MetalService;
import com.gurukrupa.data.service.MetalRateService;
import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.entities.MetalRate;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import java.math.BigDecimal;
import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.data.service.JewelryItemService;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.data.service.BillTransactionService;
import com.gurukrupa.data.service.ExchangeTransactionService;
import com.gurukrupa.data.service.AppSettingsService;
import com.gurukrupa.event.BillCreatedEvent;
import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import com.gurukrupa.data.entities.ExchangeTransaction;
import com.gurukrupa.data.entities.Exchange;
import com.gurukrupa.data.service.ExchangeService;
import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.service.BankAccountService;
import com.gurukrupa.data.entities.PaymentMode;
import com.gurukrupa.data.service.PaymentModeService;
import com.gurukrupa.data.entities.UPIPaymentMethod;
import com.gurukrupa.data.service.UPIPaymentMethodService;
import javafx.stage.Stage;
import java.util.List;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEventPublisher;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
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
    private MetalRateService metalRateService;
    @Autowired
    private BillService billService;
    @Autowired
    private BillTransactionService billTransactionService;
    @Autowired
    private ExchangeTransactionService exchangeTransactionService;
    @Autowired
    private ExchangeService exchangeService;
    @Autowired
    private BankAccountService bankAccountService;
    @Autowired
    private PaymentModeService paymentModeService;
    @Autowired
    private UPIPaymentMethodService upiPaymentMethodService;
    @Autowired
    private AppSettingsService appSettingsService;
    @Autowired
    private BillPdfService billPdfService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private AlertNotification alert;
    
    private Stage dialogStage;
    @FXML
    private Button btnAddCustomer, btnSearchCustomer, btnCloseFrame, btnAddItem, btnClearItem, 
                   btnSearchItem, btnSelectCustomer, btnPrintBill, 
                   btnAddExchange, btnClearExchange,
                   btnNewBill, btnClearSearch, btnRefreshCatalog,
                   btnCash, btnUPI, btnCard, btnPartial, btnCredit, btnBank, btnBankDone, btnUPIDone;
    @FXML
    private TextField txtCustomerName, txtMobileNo, txtCustomerAddress, txtItemCode, 
                      txtItemName, txtRate, txtQuantity, txtWeight, txtLabour, txtTotalAmount,
                      txtExchangeItemName, txtExchangeRate, txtExchangeWeight, 
                      txtExchangeDeduction, txtExchangeAmount, txtPartialAmount,
                      txtBankTransactionNo, txtBankAmount,
                      txtUPITransactionNo, txtUPIAmount;
    @FXML
    private ComboBox<Metal> cmbMetal, cmbExchangeMetal;
    @FXML
    private ComboBox<BankAccount> cmbBankAccount;
    @FXML
    private ComboBox<UPIPaymentMethod> cmbUPIPayment;
    @FXML
    private TableView itemsTable, exchangeTable;
    @FXML
    private Label lblBillAmount, lblExchangeAmount, lblFinalAmount, lblItemCount, lblSubtotal, lblCGST, lblSGST, lblGrandTotal, lblLabourCharges;
    @FXML
    private VBox partialPaymentBox, bankPaymentBox, upiPaymentBox;
    @FXML
    private TextField txtItemSearch;
    @FXML
    private ListView<String> listViewItems;
    @FXML
    private TextField txtDiscount, txtGSTRate;
    @FXML
    private ToggleButton chipBilling, chipExchange;
    @FXML
    private VBox billingPanel, exchangePanel;

    
    private ObservableList<JewelryItem> allJewelryItems = FXCollections.observableArrayList();
    private FilteredList<JewelryItem> filteredItems;
    private ObservableList<BillingItem> billingItems = FXCollections.observableArrayList();
    private ObservableList<ExchangeItem> exchangeItems = FXCollections.observableArrayList();
    private JewelryItem selectedJewelryItem;
    private BigDecimal currentLabourPercentage = null;
    private Customer selectedCustomer; // Store the selected customer

    private AutoCompleteTextField autoCompleteTextField;
    private final List<String> customerNameSuggestions = new ArrayList<>();
    private final String[] filteredSuggestions = {""}; // hold currently filtered suggestions string, will update below
    private final List<String>[] filteredListHolder = new List[1]; // to hold filtered list
    private final int[] selectedIndex = {0}; // track current selected suggestion index
    private SuggestionProvider<String> customerNames;
    private SuggestionProvider<String> itemNames; // For item name autocomplete
    private SuggestionProvider<String> exchangeItemNames; // For exchange item name autocomplete
    private Bill currentBill = null; // Store the current bill after generation
    private Exchange currentExchange = null; // Store the current exchange
    private boolean isPartialPayment = false; // Track if partial payment is selected
    private boolean isBankPayment = false; // Track if bank payment is selected
    private boolean isUPIPayment = false; // Track if UPI payment is selected
    private boolean isEditMode = false; // Track if in edit mode
    private Bill billToEdit = null; // The bill being edited
    
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
        btnPrintBill.setOnAction(e -> printBill());
        
        // Exchange section buttons
        btnAddExchange.setOnAction(e -> addExchange());
        btnClearExchange.setOnAction(e -> clearExchangeFields());
        
        // Navigation buttons
        btnNewBill.setOnAction(e -> newBill());
        
        // Setup toggle buttons for switching between billing and exchange panels
        setupToggleButtons();
        
        // Payment method buttons
        btnCash.setOnAction(e -> processCashPayment());
        btnUPI.setOnAction(e -> toggleUPIPayment());
        btnCard.setOnAction(e -> processCardPayment());
        btnPartial.setOnAction(e -> togglePartialPayment());
        btnCredit.setOnAction(e -> processCreditPayment());
        btnBank.setOnAction(e -> toggleBankPayment());
        btnBankDone.setOnAction(e -> processBankPayment());
        btnUPIDone.setOnAction(e -> processUPIPayment());
        
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
        
        // Initialize item name autocomplete
        List<String> itemNameList = jewelryItemService.getAllJewelryItems().stream()
            .map(JewelryItem::getItemName)
            .collect(Collectors.toList());
        itemNames = SuggestionProvider.create(itemNameList);
        AutoCompletionTextFieldBinding<String> itemNameBinding = new AutoCompletionTextFieldBinding<>(txtItemName, itemNames);
        
        // When user selects an item from autocomplete
        itemNameBinding.setOnAutoCompleted(e -> {
            String selectedItemName = e.getCompletion();
            // Find the jewelry item by name
            jewelryItemService.searchByItemName(selectedItemName).stream()
                .findFirst()
                .ifPresent(this::populateItemFields);
        });
        
        // Initialize exchange item name autocomplete (self-learning)
        initializeExchangeItemAutocomplete();
        
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
        makeNumericOnly(txtBankAmount, 10);
        makeNumericOnly(txtUPIAmount, 10);
        
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
        
        // Load bank accounts
        loadBankAccounts();
        
        // Configure bank account ComboBox
        configureBankAccountComboBox();
        
        // Load UPI payment methods
        loadUPIPaymentMethods();
        
        // Configure UPI payment ComboBox
        configureUPIPaymentComboBox();
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
    
    private void refreshItemSuggestions() {
        // Reload item names from database
        List<String> itemNameList = jewelryItemService.getAllJewelryItems().stream()
            .map(JewelryItem::getItemName)
            .collect(Collectors.toList());
        
        // Update the suggestion provider with new data
        itemNames = SuggestionProvider.create(itemNameList);
        
        // Recreate the auto-completion binding with updated suggestions
        AutoCompletionTextFieldBinding<String> itemNameBinding = new AutoCompletionTextFieldBinding<>(txtItemName, itemNames);
        
        // Reattach the auto-complete handler
        itemNameBinding.setOnAutoCompleted(e -> {
            String selectedItemName = e.getCompletion();
            jewelryItemService.searchByItemName(selectedItemName).stream()
                .findFirst()
                .ifPresent(this::populateItemFields);
        });
    }
    
    private void initializeExchangeItemAutocomplete() {
        // Load exchange item names from database (self-learning from previous entries)
        List<String> exchangeItemNameList = exchangeTransactionService.getAllDistinctItemNames();
        
        // Create suggestion provider
        exchangeItemNames = SuggestionProvider.create(exchangeItemNameList);
        
        // Create autocomplete binding for exchange item name
        AutoCompletionTextFieldBinding<String> exchangeItemBinding = new AutoCompletionTextFieldBinding<>(txtExchangeItemName, exchangeItemNames);
        
        // No need for onAutoCompleted handler as we just want to fill the name
        // The user will manually enter other details like metal type, weight, etc.
    }
    
    private void refreshExchangeItemSuggestions() {
        // Reload exchange item names from database
        List<String> exchangeItemNameList = exchangeTransactionService.getAllDistinctItemNames();
        
        // Update the suggestion provider with new data
        exchangeItemNames = SuggestionProvider.create(exchangeItemNameList);
        
        // Recreate the auto-completion binding with updated suggestions
        new AutoCompletionTextFieldBinding<>(txtExchangeItemName, exchangeItemNames);
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
            Metal selectedMetal = cmbMetal.getSelectionModel().getSelectedItem();
            String metalName = selectedMetal.getMetalName();
            double rate = Double.parseDouble(txtRate.getText());
            int quantity = txtQuantity.getText().isEmpty() ? 1 : Integer.parseInt(txtQuantity.getText());
            double weight = Double.parseDouble(txtWeight.getText());
            double labour = txtLabour.getText().isEmpty() ? 0 : Double.parseDouble(txtLabour.getText());
            double totalAmount = Double.parseDouble(txtTotalAmount.getText());
            
            // Add item to billing table
            int sno = billingItems.size() + 1;
            Long jewelryItemId = selectedJewelryItem != null ? selectedJewelryItem.getId() : null;
            BillingItem billingItem = new BillingItem(sno, jewelryItemId, itemCode, itemName, metalName, rate, quantity, weight, labour, totalAmount);
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
        txtLabour.setEditable(true); // Make labour field editable again
        txtLabour.setStyle(""); // Reset style
        txtTotalAmount.clear();
        selectedJewelryItem = null;
        currentLabourPercentage = null; // Clear labour percentage
        if (lblLabourCharges != null) {
            lblLabourCharges.setText("Labour Charges"); // Reset label
        }
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
        // Store the selected jewelry item
        this.selectedJewelryItem = item;
        
        txtItemCode.setText(item.getItemCode());
        txtItemName.setText(item.getItemName());
        
        // Set metal type
        if (item.getMetalType() != null) {
            String itemMetalType = item.getMetalType();
            System.out.println("Item metal type from database: '" + itemMetalType + "'");
            
            boolean found = false;
            
            // First try exact match by metal name
            for (Metal metal : cmbMetal.getItems()) {
                if (metal.getMetalName().equals(itemMetalType)) {
                    cmbMetal.getSelectionModel().select(metal);
                    found = true;
                    System.out.println("Found exact match: " + metal.getMetalName());
                    break;
                }
            }
            
            // If no exact match, try case-insensitive match
            if (!found) {
                for (Metal metal : cmbMetal.getItems()) {
                    if (metal.getMetalName().equalsIgnoreCase(itemMetalType)) {
                        cmbMetal.getSelectionModel().select(metal);
                        found = true;
                        System.out.println("Found case-insensitive match: " + metal.getMetalName());
                        break;
                    }
                }
            }
            
            // If still no match, try contains
            if (!found) {
                for (Metal metal : cmbMetal.getItems()) {
                    // Check if dropdown metal contains item metal or vice versa
                    if (metal.getMetalName().toUpperCase().contains(itemMetalType.toUpperCase()) || 
                        itemMetalType.toUpperCase().contains(metal.getMetalName().split(" ")[0].toUpperCase())) {
                        cmbMetal.getSelectionModel().select(metal);
                        found = true;
                        System.out.println("Found partial match: " + metal.getMetalName());
                        break;
                    }
                }
            }
            
            if (!found) {
                System.out.println("No matching metal found in dropdown for: " + itemMetalType);
            }
        }
        
        // Don't set rate from item - it will be set automatically from metal rates when metal is selected
        
        // Set weight - use net weight if available, otherwise gross weight
        if (item.getNetWeight() != null) {
            txtWeight.setText(item.getNetWeight().toString());
        } else if (item.getGrossWeight() != null) {
            txtWeight.setText(item.getGrossWeight().toString());
        }
        
        // Store labour percentage from the item
        if (item.getLabourCharges() != null) {
            currentLabourPercentage = item.getLabourCharges();
            txtLabour.setEditable(false); // Make labour field read-only when item is selected
            txtLabour.setStyle("-fx-background-color: #E8F5E8; -fx-opacity: 1;"); // Visual indication
            
            // Update label to show percentage
            if (lblLabourCharges != null) {
                lblLabourCharges.setText("Labour (" + currentLabourPercentage + "%)");
            }
            
            // Calculate initial labour amount (will be recalculated when rate/weight changes)
            calculateTotal();
        } else {
            currentLabourPercentage = null;
            txtLabour.setEditable(true);
            txtLabour.setStyle("");
            if (lblLabourCharges != null) {
                lblLabourCharges.setText("Labour Charges");
            }
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
    
    private Bill createBillFromCurrentData() {
        try {
            // Validate customer information
            if (txtCustomerName.getText().trim().isEmpty()) {
                alert.showError("Please enter customer information");
                return null;
            }
            
            // Validate that we have items to bill
            if (billingItems.isEmpty()) {
                alert.showError("Please add items to generate bill");
                return null;
            }
            
            // Find customer by mobile number first
            Customer customer = null;
            if (!txtMobileNo.getText().trim().isEmpty()) {
                Optional<Customer> customerOpt = customerService.findByMobile(txtMobileNo.getText().trim());
                if (customerOpt.isPresent()) {
                    customer = customerOpt.get();
                } else {
                    alert.showError("Customer not found. Please search and select a customer first.");
                    return null;
                }
            } else {
                alert.showError("Please search and select a customer first.");
                return null;
            }
            
            // Create bill transactions from billing items
            List<BillTransaction> billTransactions = new ArrayList<>();
            for (BillingItem item : billingItems) {
                BillTransaction transaction = billTransactionService.createBillTransaction(
                    item.getItemCode(),
                    item.getItemName(),
                    item.getMetal(),
                    item.getQuantity(),
                    new BigDecimal(item.getWeight()),
                    new BigDecimal(item.getRate()),
                    new BigDecimal(item.getLabour())
                );
                billTransactions.add(transaction);
            }
            
            // Create exchange if we have exchange items
            Exchange exchange = null;
            if (!exchangeItems.isEmpty()) {
                // Create exchange transactions
                List<ExchangeTransaction> exchangeTransactions = new ArrayList<>();
                for (ExchangeItem item : exchangeItems) {
                    ExchangeTransaction transaction = exchangeTransactionService.createExchangeTransaction(
                        item.getItemName(),
                        item.getMetal(),
                        BigDecimal.valueOf(item.getWeight()),
                        BigDecimal.valueOf(item.getDeduction()),
                        BigDecimal.valueOf(item.getRate())
                    );
                    exchangeTransactions.add(transaction);
                }
                
                // Create exchange entity with customer and transactions
                exchange = exchangeService.createExchange(customer, exchangeTransactions, 
                    "Exchange at billing time for customer: " + customer.getCustomerFullName());
                currentExchange = exchange;
                System.out.println("Exchange created with total amount: " + exchange.getTotalExchangeAmount());
            }
            
            // Get payment details
            BigDecimal discount = txtDiscount.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(txtDiscount.getText());
            BigDecimal gstRate = txtGSTRate.getText().isEmpty() ? 
                new BigDecimal(appSettingsService.getDefaultGstRate().toString()) : 
                new BigDecimal(txtGSTRate.getText());
            
            // Create and save bill (without specifying payment method yet)
            Bill savedBill = billService.createBillFromTransaction(
                customer,
                billTransactions,
                exchange,
                discount,
                gstRate,
                null // Payment method will be set when user clicks payment button
            );
            
            System.out.println("Bill generated successfully with ID: " + savedBill.getId() + ", Bill Number: " + savedBill.getBillNumber());
            
            // Publish event for stock reduction (will be handled asynchronously)
            eventPublisher.publishEvent(new BillCreatedEvent(this, savedBill));
            
            // Store the current bill for payment processing
            currentBill = savedBill;
            
            return savedBill;
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error creating bill: " + e.getMessage());
            return null;
        }
    }
    
    private void printBill() {
        try {
            if (currentBill == null) {
                alert.showError("No bill available to print. Please complete a transaction first.");
                return;
            }
            
            if (currentBill.getStatus() != Bill.BillStatus.PAID && 
                currentBill.getStatus() != Bill.BillStatus.CONFIRMED) {
                alert.showError("Please complete the payment first by selecting a payment method.");
                return;
            }
            
            // Generate and open PDF
            generateAndSavePdf(currentBill);
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error printing bill: " + e.getMessage());
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
            
            // Calculate total weight and gold value
            double totalWeight = weight * quantity;
            double goldValue = (rate * totalWeight) / 10;
            
            double labour = 0;
            
            // If we have a selected item with labour percentage, calculate labour from percentage
            if (currentLabourPercentage != null && currentLabourPercentage.compareTo(BigDecimal.ZERO) > 0) {
                // Calculate labour as percentage of gold value
                labour = goldValue * currentLabourPercentage.doubleValue() / 100;
                // Update the labour field to show calculated amount
                txtLabour.setText(String.format("%.2f", labour));
            } else {
                // Otherwise, use manual labour entry
                labour = txtLabour.getText().isEmpty() ? 0 : Double.parseDouble(txtLabour.getText());
            }
            
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
            Metal selectedMetal = cmbExchangeMetal.getSelectionModel().getSelectedItem();
            String metalName = selectedMetal.getMetalName();
            double rate = Double.parseDouble(txtExchangeRate.getText());
            double weight = Double.parseDouble(txtExchangeWeight.getText());
            double deduction = txtExchangeDeduction.getText().isEmpty() ? 0 : Double.parseDouble(txtExchangeDeduction.getText());
            double amount = Double.parseDouble(txtExchangeAmount.getText());
            
            // Add item to exchange table
            int sno = exchangeItems.size() + 1;
            ExchangeItem exchangeItem = new ExchangeItem(sno, itemName, metalName, rate, weight, deduction, amount);
            exchangeItems.add(exchangeItem);
            System.out.println("Adding exchange item: " + itemName + ", Amount: " + amount);
            
            // Update exchange total
            updateExchangeTotal();
            
            // Clear fields after adding
            clearExchangeFields();
            
            alert.showSuccess("Exchange item added successfully!");
            
            // Refresh exchange item suggestions to include the newly added item name
            // This enables the self-learning feature
            refreshExchangeItemSuggestions();
            
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
            // GST should be calculated only on billing items (billAmount), not on exchange
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
        currentExchange = null; // Reset current exchange
        
        // Reset partial payment state
        isPartialPayment = false;
        partialPaymentBox.setVisible(false);
        partialPaymentBox.setManaged(false);
        txtPartialAmount.clear();
        btnPartial.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        
        // Reset bank payment state
        isBankPayment = false;
        bankPaymentBox.setVisible(false);
        bankPaymentBox.setManaged(false);
        clearBankPaymentFields();
        btnBank.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        
        // Reset UPI payment state
        isUPIPayment = false;
        upiPaymentBox.setVisible(false);
        upiPaymentBox.setManaged(false);
        clearUPIPaymentFields();
        btnUPI.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        
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
            
            // Refresh item suggestions for autocomplete
            refreshItemSuggestions();
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
        TableColumn<BillingItem, String> labourCol = (TableColumn<BillingItem, String>) itemsTable.getColumns().get(7);
        TableColumn<BillingItem, Double> amountCol = (TableColumn<BillingItem, Double>) itemsTable.getColumns().get(8);
        
        snoCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSno()).asObject());
        itemCodeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItemCode()));
        itemNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItemName()));
        metalCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMetal()));
        rateCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getRate()).asObject());
        quantityCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());
        weightCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getWeight()).asObject());
        labourCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.1f%%", cellData.getValue().getLabourPercentage())));
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
    
    private void setupToggleButtons() {
        // Style constants
        String activeStyle = "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 700; -fx-font-size: 14px; -fx-padding: 10 24 10 24; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: #E0E0E0; -fx-text-fill: #757575; -fx-font-family: 'Segoe UI'; -fx-font-weight: 700; -fx-font-size: 14px; -fx-padding: 10 24 10 24; -fx-cursor: hand;";
        String leftRoundStyle = "-fx-background-radius: 8 0 0 8;";
        String rightRoundStyle = "-fx-background-radius: 0 8 8 0;";
        
        // Set initial styles
        chipBilling.setStyle(activeStyle + leftRoundStyle);
        chipExchange.setStyle(inactiveStyle + rightRoundStyle);
        
        // Add toggle listeners
        chipBilling.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                // Show billing panel, hide exchange panel
                billingPanel.setVisible(true);
                billingPanel.setManaged(true);
                exchangePanel.setVisible(false);
                exchangePanel.setManaged(false);
                
                // Update styles
                chipBilling.setStyle(activeStyle + leftRoundStyle);
                chipExchange.setStyle(inactiveStyle + rightRoundStyle);
                
                // Update icons
                ((FontAwesomeIcon) chipBilling.getGraphic()).setFill(javafx.scene.paint.Color.WHITE);
                ((FontAwesomeIcon) chipExchange.getGraphic()).setFill(javafx.scene.paint.Color.web("#757575"));
            }
        });
        
        chipExchange.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                // Show exchange panel, hide billing panel
                exchangePanel.setVisible(true);
                exchangePanel.setManaged(true);
                billingPanel.setVisible(false);
                billingPanel.setManaged(false);
                
                // Update styles
                chipExchange.setStyle(activeStyle + rightRoundStyle);
                chipBilling.setStyle(inactiveStyle + leftRoundStyle);
                
                // Update icons
                ((FontAwesomeIcon) chipExchange.getGraphic()).setFill(javafx.scene.paint.Color.WHITE);
                ((FontAwesomeIcon) chipBilling.getGraphic()).setFill(javafx.scene.paint.Color.web("#757575"));
            }
        });
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
        private double labourPercentage;
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
            // Calculate labour percentage based on gold value
            double goldValue = (weight * rate) / 10;
            this.labourPercentage = goldValue > 0 ? (labour / goldValue) * 100 : 0;
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
        public double getLabourPercentage() { return labourPercentage; }
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
            // Get active metals from Metal table
            List<Metal> metals = metalService.getAllActiveMetals();
            ObservableList<Metal> metalList = FXCollections.observableArrayList(metals);
            
            // Configure ComboBox to display metal name
            cmbMetal.setItems(metalList);
            cmbMetal.setConverter(new javafx.util.StringConverter<Metal>() {
                @Override
                public String toString(Metal metal) {
                    return metal != null ? metal.getMetalName() : "";
                }
                
                @Override
                public Metal fromString(String string) {
                    return null;
                }
            });
            
            cmbExchangeMetal.setItems(metalList);
            cmbExchangeMetal.setConverter(new javafx.util.StringConverter<Metal>() {
                @Override
                public String toString(Metal metal) {
                    return metal != null ? metal.getMetalName() : "";
                }
                
                @Override
                public Metal fromString(String string) {
                    return null;
                }
            });
            
            // Add listener to update rate when metal is selected for billing
            cmbMetal.valueProperty().addListener((obs, oldMetal, newMetal) -> {
                if (newMetal != null) {
                    updateMetalRate(newMetal);
                }
            });
            
            // Add listener to update rate when metal is selected for exchange
            cmbExchangeMetal.valueProperty().addListener((obs, oldMetal, newMetal) -> {
                if (newMetal != null) {
                    updateExchangeMetalRate(newMetal);
                }
            });
            
            System.out.println("Loaded " + metalList.size() + " metal types from database");
        } catch (Exception e) {
            System.err.println("Error loading metal types: " + e.getMessage());
            alert.showError("Failed to load metal types: " + e.getMessage());
        }
    }
    
    private void updateMetalRate(Metal metal) {
        try {
            // Get the latest rate for the selected metal
            Optional<MetalRate> latestRate = metalRateService.getLatestMetalRate(metal.getId(), LocalDate.now());
            
            if (latestRate.isPresent()) {
                // Set the selling rate in the rate field
                BigDecimal sellingRate = latestRate.get().getSellingRate();
                if (sellingRate == null) {
                    // If no selling rate, use rate per 10 grams
                    sellingRate = latestRate.get().getRatePerTenGrams();
                }
                txtRate.setText(sellingRate.setScale(2, RoundingMode.HALF_UP).toString());
            } else {
                // No rate found for today or earlier
                alert.showError("No rate found for " + metal.getMetalName() + ". Please set the metal rate first.");
                txtRate.setText("0.00");
            }
        } catch (Exception e) {
            System.err.println("Error fetching metal rate: " + e.getMessage());
            alert.showError("Error fetching metal rate: " + e.getMessage());
        }
    }
    
    private void updateExchangeMetalRate(Metal metal) {
        try {
            // Get the latest rate for the selected metal
            Optional<MetalRate> latestRate = metalRateService.getLatestMetalRate(metal.getId(), LocalDate.now());
            
            if (latestRate.isPresent()) {
                // Set the buying rate for exchange
                BigDecimal buyingRate = latestRate.get().getBuyingRate();
                if (buyingRate == null) {
                    // If no buying rate, use rate per 10 grams
                    buyingRate = latestRate.get().getRatePerTenGrams();
                }
                txtExchangeRate.setText(buyingRate.setScale(2, RoundingMode.HALF_UP).toString());
            } else {
                // No rate found for today or earlier
                alert.showError("No rate found for " + metal.getMetalName() + ". Please set the metal rate first.");
                txtExchangeRate.setText("0.00");
            }
        } catch (Exception e) {
            System.err.println("Error fetching metal rate: " + e.getMessage());
            alert.showError("Error fetching metal rate: " + e.getMessage());
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
            // Create bill if not already created
            if (currentBill == null) {
                currentBill = createBillFromCurrentData();
                if (currentBill == null) {
                    return; // Error already shown in createBillFromCurrentData
                }
            }
            
            // Check if partial payment is active
            BigDecimal paidAmount = currentBill.getGrandTotal(); // Default to full payment
            if (isPartialPayment) {
                String amountText = txtPartialAmount.getText().trim();
                if (amountText.isEmpty()) {
                    alert.showError("Please enter a valid partial payment amount.");
                    return;
                }
                
                try {
                    paidAmount = new BigDecimal(amountText);
                    if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        alert.showError("Partial payment amount must be greater than zero.");
                        return;
                    }
                    if (paidAmount.compareTo(currentBill.getGrandTotal()) > 0) {
                        alert.showError("Payment amount cannot exceed the bill total.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    alert.showError("Please enter a valid numeric amount.");
                    return;
                }
            }
            
            // Update bill payment details
            currentBill.setPaymentMethod(Bill.PaymentMethod.CASH);
            currentBill.setPaidAmount(paidAmount);
            currentBill.setPendingAmount(currentBill.getGrandTotal().subtract(paidAmount));
            currentBill.setStatus(paidAmount.compareTo(currentBill.getGrandTotal()) >= 0 ? 
                                 Bill.BillStatus.PAID : Bill.BillStatus.CONFIRMED);
            
            // Save the updated bill
            Bill updatedBill = billService.saveBill(currentBill);
            
            // Create PaymentMode entry for cash payment
            PaymentMode cashPayment = paymentModeService.createCashPayment(updatedBill, paidAmount);
            
            // Generate PDF
            generateAndSavePdf(updatedBill);
            
            // Hide partial payment box
            if (isPartialPayment) {
                partialPaymentBox.setVisible(false);
                partialPaymentBox.setManaged(false);
                txtPartialAmount.clear();
                isPartialPayment = false;
            }
            
            // Show success message
            if (updatedBill.getPendingAmount().compareTo(BigDecimal.ZERO) > 0) {
                alert.showSuccess(String.format(
                    "Cash payment processed successfully!\n" +
                    "Paid Amount: ₹%.2f\n" +
                    "Pending Amount: ₹%.2f\n" +
                    "Bill saved and PDF generated.",
                    paidAmount, updatedBill.getPendingAmount()
                ));
            } else {
                alert.showSuccess("Cash payment processed successfully!\nBill saved and PDF generated.");
            }
            
            // Reset for new bill
            newBill();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error processing cash payment: " + e.getMessage());
        }
    }
    
    
    private void processCardPayment() {
        try {
            // Create bill if not already created
            if (currentBill == null) {
                currentBill = createBillFromCurrentData();
                if (currentBill == null) {
                    return; // Error already shown in createBillFromCurrentData
                }
            }
            
            // Check if partial payment is active
            BigDecimal paidAmount = currentBill.getGrandTotal(); // Default to full payment
            if (isPartialPayment) {
                String amountText = txtPartialAmount.getText().trim();
                if (amountText.isEmpty()) {
                    alert.showError("Please enter a valid partial payment amount.");
                    return;
                }
                
                try {
                    paidAmount = new BigDecimal(amountText);
                    if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        alert.showError("Partial payment amount must be greater than zero.");
                        return;
                    }
                    if (paidAmount.compareTo(currentBill.getGrandTotal()) > 0) {
                        alert.showError("Payment amount cannot exceed the bill total.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    alert.showError("Please enter a valid numeric amount.");
                    return;
                }
            }
            
            // Update bill payment details
            currentBill.setPaymentMethod(Bill.PaymentMethod.CARD);
            currentBill.setPaidAmount(paidAmount);
            currentBill.setPendingAmount(currentBill.getGrandTotal().subtract(paidAmount));
            currentBill.setStatus(paidAmount.compareTo(currentBill.getGrandTotal()) >= 0 ? 
                                 Bill.BillStatus.PAID : Bill.BillStatus.CONFIRMED);
            
            // Save the updated bill
            Bill updatedBill = billService.saveBill(currentBill);
            
            // Create PaymentMode entry for card payment (TODO: need to collect card details)
            PaymentMode cardPayment = paymentModeService.createCardPayment(updatedBill, paidAmount, "CARD-REF-" + System.currentTimeMillis(), null, null, null);
            
            // Generate PDF
            generateAndSavePdf(updatedBill);
            
            // Hide partial payment box
            if (isPartialPayment) {
                partialPaymentBox.setVisible(false);
                partialPaymentBox.setManaged(false);
                txtPartialAmount.clear();
                isPartialPayment = false;
            }
            
            // Show success message
            if (updatedBill.getPendingAmount().compareTo(BigDecimal.ZERO) > 0) {
                alert.showSuccess(String.format(
                    "Card payment processed successfully!\n" +
                    "Paid Amount: ₹%.2f\n" +
                    "Pending Amount: ₹%.2f\n" +
                    "Bill saved and PDF generated.",
                    paidAmount, updatedBill.getPendingAmount()
                ));
            } else {
                alert.showSuccess("Card payment processed successfully!\nBill saved and PDF generated.");
            }
            
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
    
    private void togglePartialPayment() {
        // Toggle partial payment mode
        isPartialPayment = !isPartialPayment;
        
        if (isPartialPayment) {
            // Show the partial payment input box
            partialPaymentBox.setVisible(true);
            partialPaymentBox.setManaged(true);
            
            // Hide bank payment box if visible
            if (isBankPayment) {
                bankPaymentBox.setVisible(false);
                bankPaymentBox.setManaged(false);
                isBankPayment = false;
                // Reset bank button style
                btnBank.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
            }
            
            // Set focus to the amount input field
            txtPartialAmount.requestFocus();
            
            // Don't clear if there's already a value (user might be toggling back)
            // This preserves the amount when switching between payment methods
            if (txtPartialAmount.getText().trim().isEmpty()) {
                txtPartialAmount.clear();
            }
            
            // Update button style to show it's active
            btnPartial.setStyle("-fx-background-color: #6A1B9A; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        } else {
            // Hide the partial payment input box
            partialPaymentBox.setVisible(false);
            partialPaymentBox.setManaged(false);
            // Don't clear the partial amount - preserve it for potential bank payment
            
            // Reset button style
            btnPartial.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        }
    }
    
    private void showPartialPaymentInput() {
        if (currentBill == null) {
            alert.showError("Please generate the bill first before processing payment.");
            return;
        }
        
        // Show the partial payment input box
        partialPaymentBox.setVisible(true);
        partialPaymentBox.setManaged(true);
        
        // Set focus to the amount input field
        txtPartialAmount.requestFocus();
        
        // Pre-fill with grand total for convenience
        txtPartialAmount.setText(currentBill.getGrandTotal().toString());
        txtPartialAmount.selectAll();
    }
    
    private void processPartialPayment() {
        try {
            if (currentBill == null) {
                alert.showError("Please generate the bill first before processing payment.");
                return;
            }
            
            // Validate partial amount
            String amountText = txtPartialAmount.getText().trim();
            if (amountText.isEmpty()) {
                alert.showError("Please enter a valid partial payment amount.");
                return;
            }
            
            BigDecimal partialAmount = new BigDecimal(amountText);
            BigDecimal grandTotal = currentBill.getGrandTotal();
            
            if (partialAmount.compareTo(BigDecimal.ZERO) <= 0) {
                alert.showError("Partial payment amount must be greater than zero.");
                return;
            }
            
            if (partialAmount.compareTo(grandTotal) >= 0) {
                alert.showError("Partial payment amount must be less than the total bill amount.\nFor full payment, please use Cash/UPI/Card options.");
                return;
            }
            
            // Update bill with partial payment
            currentBill.setPaymentMethod(Bill.PaymentMethod.PARTIAL);
            currentBill.setPaidAmount(partialAmount);
            currentBill.setPendingAmount(grandTotal.subtract(partialAmount));
            currentBill.setStatus(Bill.BillStatus.CONFIRMED);
            
            // Save the updated bill
            Bill updatedBill = billService.saveBill(currentBill);
            
            // Create PaymentMode entry for partial cash payment
            PaymentMode partialPayment = paymentModeService.createCashPayment(updatedBill, partialAmount);
            
            // Generate PDF
            generateAndSavePdf(updatedBill);
            
            // Hide partial payment box
            partialPaymentBox.setVisible(false);
            partialPaymentBox.setManaged(false);
            txtPartialAmount.clear();
            
            // Show success message
            alert.showSuccess(String.format(
                "Partial payment processed successfully!\n" +
                "Paid Amount: ₹%.2f\n" +
                "Pending Amount: ₹%.2f\n" +
                "Bill saved and PDF generated.",
                partialAmount, updatedBill.getPendingAmount()
            ));
            
            // Reset for new bill
            newBill();
            
        } catch (NumberFormatException e) {
            alert.showError("Please enter a valid numeric amount.");
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error processing partial payment: " + e.getMessage());
        }
    }
    
    private void processCreditPayment() {
        try {
            // Create bill if not already created
            if (currentBill == null) {
                currentBill = createBillFromCurrentData();
                if (currentBill == null) {
                    return; // Error already shown in createBillFromCurrentData
                }
            }
            
            // Get customer name
            String customerName = currentBill.getCustomer() != null ? 
                                 currentBill.getCustomer().getCustomerFullName() : 
                                 txtCustomerName.getText();
            
            // Confirm credit sale
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Credit Sale Confirmation");
            confirmAlert.setHeaderText("Confirm Credit Sale");
            confirmAlert.setContentText(String.format(
                "This will mark the entire amount as pending:\n" +
                "Total Amount: ₹%.2f\n" +
                "Customer: %s\n\n" +
                "Do you want to proceed with credit sale?",
                currentBill.getGrandTotal(),
                customerName
            ));
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Update bill for credit sale
                currentBill.setPaymentMethod(Bill.PaymentMethod.CREDIT);
                currentBill.setPaidAmount(BigDecimal.ZERO);
                currentBill.setPendingAmount(currentBill.getGrandTotal());
                currentBill.setStatus(Bill.BillStatus.CONFIRMED);
                
                // Save the updated bill
                Bill updatedBill = billService.saveBill(currentBill);
                
                // Note: No PaymentMode entry for credit sale as no payment is made
                
                // Generate PDF
                generateAndSavePdf(updatedBill);
                
                // Hide partial payment box if visible
                if (isPartialPayment) {
                    partialPaymentBox.setVisible(false);
                    partialPaymentBox.setManaged(false);
                    txtPartialAmount.clear();
                    isPartialPayment = false;
                }
                
                // Show success message
                alert.showSuccess(String.format(
                    "Credit sale processed successfully!\n" +
                    "Total Pending Amount: ₹%.2f\n" +
                    "Bill saved and PDF generated.",
                    updatedBill.getPendingAmount()
                ));
                
                // Reset for new bill
                newBill();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error processing credit sale: " + e.getMessage());
        }
    }
    
    private void loadBankAccounts() {
        try {
            List<BankAccount> bankAccounts = bankAccountService.getAllActiveBankAccounts();
            cmbBankAccount.getItems().clear();
            cmbBankAccount.getItems().addAll(bankAccounts);
        } catch (Exception e) {
            System.err.println("Error loading bank accounts: " + e.getMessage());
        }
    }
    
    private void configureBankAccountComboBox() {
        // Set string converter for displaying bank account names
        cmbBankAccount.setConverter(new javafx.util.StringConverter<BankAccount>() {
            @Override
            public String toString(BankAccount bankAccount) {
                return bankAccount != null ? bankAccount.getBankName() + " - " + bankAccount.getAccountNumber() : "";
            }
            
            @Override
            public BankAccount fromString(String string) {
                return cmbBankAccount.getItems().stream()
                        .filter(item -> (item.getBankName() + " - " + item.getAccountNumber()).equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }
    
    private void toggleBankPayment() {
        // Toggle bank payment mode
        isBankPayment = !isBankPayment;
        
        if (isBankPayment) {
            // Store partial payment amount if it exists
            String partialAmount = txtPartialAmount.getText().trim();
            
            // Show the bank payment details box
            bankPaymentBox.setVisible(true);
            bankPaymentBox.setManaged(true);
            
            // Hide partial payment box if visible
            if (isPartialPayment) {
                partialPaymentBox.setVisible(false);
                partialPaymentBox.setManaged(false);
                // Don't clear the partial amount field - preserve the value
                isPartialPayment = false;
            }
            
            // Set focus to the bank account dropdown
            cmbBankAccount.requestFocus();
            
            // Pre-fill bank amount based on context
            if (!partialAmount.isEmpty()) {
                // If there was a partial payment amount, use it for bank payment
                txtBankAmount.setText(partialAmount);
            } else if (currentBill != null) {
                // Otherwise use the grand total if bill exists
                txtBankAmount.setText(currentBill.getGrandTotal().toString());
            } else if (billingItems.size() > 0 || exchangeItems.size() > 0) {
                // Or calculate from current items
                BigDecimal total = calculateGrandTotal();
                txtBankAmount.setText(total.toString());
            }
            
            // Update button style to show it's active
            btnBank.setStyle("-fx-background-color: #4A148C; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        } else {
            // Hide the bank payment details box
            bankPaymentBox.setVisible(false);
            bankPaymentBox.setManaged(false);
            clearBankPaymentFields();
            
            // Reset button style
            btnBank.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        }
    }
    
    private void clearBankPaymentFields() {
        cmbBankAccount.getSelectionModel().clearSelection();
        txtBankTransactionNo.clear();
        txtBankAmount.clear();
    }
    
    private void processBankPayment() {
        try {
            // Validate bank payment fields
            if (cmbBankAccount.getValue() == null) {
                alert.showError("Please select a bank account.");
                return;
            }
            
            if (txtBankTransactionNo.getText().trim().isEmpty()) {
                alert.showError("Please enter transaction/reference number.");
                return;
            }
            
            if (txtBankAmount.getText().trim().isEmpty()) {
                alert.showError("Please enter the payment amount.");
                return;
            }
            
            BigDecimal bankAmount;
            try {
                bankAmount = new BigDecimal(txtBankAmount.getText().trim());
                if (bankAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    alert.showError("Payment amount must be greater than zero.");
                    return;
                }
            } catch (NumberFormatException e) {
                alert.showError("Please enter a valid numeric amount.");
                return;
            }
            
            // Create bill if not already created
            if (currentBill == null) {
                currentBill = createBillFromCurrentData();
                if (currentBill == null) {
                    return; // Error already shown in createBillFromCurrentData
                }
            }
            
            // Validate payment amount doesn't exceed bill total
            if (bankAmount.compareTo(currentBill.getGrandTotal()) > 0) {
                alert.showError("Payment amount cannot exceed the bill total.");
                return;
            }
            
            // Update bill payment details
            currentBill.setPaymentMethod(Bill.PaymentMethod.BANK_TRANSFER);
            currentBill.setPaidAmount(bankAmount);
            currentBill.setPendingAmount(currentBill.getGrandTotal().subtract(bankAmount));
            currentBill.setStatus(bankAmount.compareTo(currentBill.getGrandTotal()) >= 0 ? 
                                 Bill.BillStatus.PAID : Bill.BillStatus.CONFIRMED);
            
            // Save the bill first
            Bill updatedBill = billService.saveBill(currentBill);
            
            // Create PaymentMode entry for bank payment
            BankAccount selectedBank = cmbBankAccount.getValue();
            String transactionNo = txtBankTransactionNo.getText().trim();
            
            PaymentMode bankPayment = paymentModeService.createBankPayment(
                updatedBill, 
                bankAmount, 
                selectedBank, 
                transactionNo
            );
            
            // Generate PDF
            generateAndSavePdf(updatedBill);
            
            // Hide bank payment box
            bankPaymentBox.setVisible(false);
            bankPaymentBox.setManaged(false);
            clearBankPaymentFields();
            isBankPayment = false;
            
            // Reset button style
            btnBank.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
            
            // Show success message
            if (updatedBill.getPendingAmount().compareTo(BigDecimal.ZERO) > 0) {
                alert.showSuccess(String.format(
                    "Bank payment processed successfully!\n" +
                    "Bank: %s\n" +
                    "Transaction No: %s\n" +
                    "Paid Amount: ₹%.2f\n" +
                    "Pending Amount: ₹%.2f\n" +
                    "Bill saved and PDF generated.",
                    selectedBank.getBankName(),
                    transactionNo,
                    bankAmount, 
                    updatedBill.getPendingAmount()
                ));
            } else {
                alert.showSuccess(String.format(
                    "Bank payment processed successfully!\n" +
                    "Bank: %s\n" +
                    "Transaction No: %s\n" +
                    "Amount: ₹%.2f\n" +
                    "Bill saved and PDF generated.",
                    selectedBank.getBankName(),
                    transactionNo,
                    bankAmount
                ));
            }
            
            // Reset for new bill
            newBill();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error processing bank payment: " + e.getMessage());
        }
    }
    
    private void loadUPIPaymentMethods() {
        try {
            List<UPIPaymentMethod> upiMethods = upiPaymentMethodService.getActiveUPIPaymentMethods();
            cmbUPIPayment.getItems().clear();
            cmbUPIPayment.getItems().addAll(upiMethods);
        } catch (Exception e) {
            System.err.println("Error loading UPI payment methods: " + e.getMessage());
        }
    }
    
    private void configureUPIPaymentComboBox() {
        // Set string converter for displaying UPI payment app names
        cmbUPIPayment.setConverter(new javafx.util.StringConverter<UPIPaymentMethod>() {
            @Override
            public String toString(UPIPaymentMethod upiMethod) {
                if (upiMethod != null) {
                    // Show app name and linked bank account
                    return upiMethod.getAppName() + " (" + upiMethod.getBankAccount().getBankName() + ")";
                }
                return "";
            }
            
            @Override
            public UPIPaymentMethod fromString(String string) {
                return cmbUPIPayment.getItems().stream()
                        .filter(item -> {
                            String displayString = item.getAppName() + " (" + item.getBankAccount().getBankName() + ")";
                            return displayString.equals(string);
                        })
                        .findFirst()
                        .orElse(null);
            }
        });
    }
    
    private void toggleUPIPayment() {
        // Toggle UPI payment mode
        isUPIPayment = !isUPIPayment;
        
        if (isUPIPayment) {
            // Store partial payment amount if it exists
            String partialAmount = txtPartialAmount.getText().trim();
            
            // Show the UPI payment details box
            upiPaymentBox.setVisible(true);
            upiPaymentBox.setManaged(true);
            
            // Hide partial payment box if visible
            if (isPartialPayment) {
                partialPaymentBox.setVisible(false);
                partialPaymentBox.setManaged(false);
                isPartialPayment = false;
                // Reset partial button style
                btnPartial.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
            }
            
            // Hide bank payment box if visible
            if (isBankPayment) {
                bankPaymentBox.setVisible(false);
                bankPaymentBox.setManaged(false);
                isBankPayment = false;
                // Reset bank button style
                btnBank.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
            }
            
            // Pre-fill UPI amount based on context
            if (!partialAmount.isEmpty()) {
                // If there was a partial payment amount, use it for UPI payment
                txtUPIAmount.setText(partialAmount);
            } else if (currentBill != null) {
                // Otherwise use the grand total if bill exists
                txtUPIAmount.setText(currentBill.getGrandTotal().toString());
            } else if (billingItems.size() > 0 || exchangeItems.size() > 0) {
                // Or calculate from current items
                BigDecimal total = calculateGrandTotal();
                txtUPIAmount.setText(total.toString());
            }
            
            // Update button style to show it's active
            btnUPI.setStyle("-fx-background-color: #E65100; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        } else {
            // Hide the UPI payment details box
            upiPaymentBox.setVisible(false);
            upiPaymentBox.setManaged(false);
            clearUPIPaymentFields();
            
            // Reset button style
            btnUPI.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        }
    }
    
    private void clearUPIPaymentFields() {
        cmbUPIPayment.getSelectionModel().clearSelection();
        txtUPITransactionNo.clear();
        txtUPIAmount.clear();
    }
    
    private void processUPIPayment() {
        try {
            // Validate UPI payment fields
            if (cmbUPIPayment.getValue() == null) {
                alert.showError("Please select a UPI payment app.");
                return;
            }
            
            if (txtUPITransactionNo.getText().trim().isEmpty()) {
                alert.showError("Please enter UPI transaction/reference number.");
                return;
            }
            
            if (txtUPIAmount.getText().trim().isEmpty()) {
                alert.showError("Please enter the payment amount.");
                return;
            }
            
            BigDecimal upiAmount;
            try {
                upiAmount = new BigDecimal(txtUPIAmount.getText().trim());
                if (upiAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    alert.showError("Payment amount must be greater than zero.");
                    return;
                }
            } catch (NumberFormatException e) {
                alert.showError("Please enter a valid numeric amount.");
                return;
            }
            
            // Create bill if not already created
            if (currentBill == null) {
                currentBill = createBillFromCurrentData();
                if (currentBill == null) {
                    return; // Error already shown in createBillFromCurrentData
                }
            }
            
            // Validate payment amount doesn't exceed bill total
            if (upiAmount.compareTo(currentBill.getGrandTotal()) > 0) {
                alert.showError("Payment amount cannot exceed the bill total.");
                return;
            }
            
            // Set payment details
            currentBill.setPaymentMethod(Bill.PaymentMethod.UPI);
            currentBill.setPaidAmount(upiAmount);
            currentBill.setPendingAmount(currentBill.getGrandTotal().subtract(upiAmount));
            
            // Get selected UPI method's bank account
            UPIPaymentMethod selectedUPI = cmbUPIPayment.getValue();
            BankAccount linkedBank = selectedUPI.getBankAccount();
            
            // Update bill status based on payment
            if (currentBill.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                currentBill.setStatus(Bill.BillStatus.PAID);
            } else {
                currentBill.setStatus(Bill.BillStatus.CONFIRMED);
            }
            
            // Save the bill
            Bill updatedBill = billService.saveBill(currentBill);
            currentBill = updatedBill;
            
            // Create UPI payment with bank transaction
            paymentModeService.createUPIPaymentWithBankAccount(
                updatedBill,
                upiAmount,
                txtUPITransactionNo.getText().trim(),
                selectedUPI.getUpiId(),
                linkedBank
            );
            
            // Save the exchange transaction if exists
            if (currentExchange != null) {
                currentExchange.setBill(updatedBill);
                exchangeService.saveExchange(currentExchange);
            }
            
            alert.showSuccess("UPI payment processed successfully!");
            
            // Generate PDF
            generateAndSavePdf(updatedBill);
            
            // Hide UPI payment box
            upiPaymentBox.setVisible(false);
            upiPaymentBox.setManaged(false);
            clearUPIPaymentFields();
            isUPIPayment = false;
            
            // Reset button style
            btnUPI.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
            
            // Start new bill
            newBill();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error processing UPI payment: " + e.getMessage());
        }
    }
    
    private BigDecimal calculateGrandTotal() {
        BigDecimal subtotal = billingItems.stream()
            .map(item -> BigDecimal.valueOf(item.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal exchangeTotal = exchangeItems.stream()
            .map(item -> BigDecimal.valueOf(item.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal discount = BigDecimal.ZERO;
        if (!txtDiscount.getText().isEmpty()) {
            try {
                discount = new BigDecimal(txtDiscount.getText());
            } catch (NumberFormatException e) {
                // Ignore invalid discount
            }
        }
        
        BigDecimal gstRate = new BigDecimal("3.00");
        if (!txtGSTRate.getText().isEmpty()) {
            try {
                gstRate = new BigDecimal(txtGSTRate.getText());
            } catch (NumberFormatException e) {
                // Ignore invalid GST rate
            }
        }
        
        BigDecimal netTotal = subtotal.subtract(discount);
        BigDecimal taxableAmount = subtotal; // GST only on billing items
        BigDecimal totalTax = taxableAmount.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        return netTotal.add(totalTax).subtract(exchangeTotal);
    }
    
    
    private void hideAllPaymentBoxes() {
        // Hide partial payment box
        partialPaymentBox.setVisible(false);
        partialPaymentBox.setManaged(false);
        isPartialPayment = false;
        btnPartial.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        
        // Hide bank payment box
        bankPaymentBox.setVisible(false);
        bankPaymentBox.setManaged(false);
        isBankPayment = false;
        btnBank.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        
        // Hide UPI payment box
        upiPaymentBox.setVisible(false);
        upiPaymentBox.setManaged(false);
        isUPIPayment = false;
        btnUPI.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
    }
    
    /**
     * Set the controller to edit mode with an existing bill
     */
    public void setEditMode(Bill bill) {
        this.isEditMode = true;
        this.billToEdit = bill;
        this.currentBill = bill;
        
        // Load bill data after initialization
        javafx.application.Platform.runLater(() -> {
            loadBillDataForEditing();
        });
    }
    
    /**
     * Load bill data into the form for editing
     */
    private void loadBillDataForEditing() {
        if (billToEdit == null) return;
        
        try {
            // Set customer information
            Customer customer = billToEdit.getCustomer();
            if (customer != null) {
                txtCustomerName.setText(customer.getCustomerFullName());
                txtMobileNo.setText(customer.getMobile());
                txtCustomerAddress.setText(customer.getCustomerAddress());
                selectedCustomer = customer;
            }
            
            // Load bill transactions into the billing table
            List<BillTransaction> billTransactions = billTransactionService.findByBillId(billToEdit.getId());
            billingItems.clear();
            int sno = 1;
            for (BillTransaction transaction : billTransactions) {
                BillingItem item = new BillingItem(
                    sno++,
                    null, // jewelryItemId not stored in BillTransaction
                    transaction.getItemCode(),
                    transaction.getItemName(),
                    transaction.getMetalType(),
                    transaction.getRatePerTenGrams().doubleValue(),
                    1, // Quantity always 1 in current implementation
                    transaction.getWeight().doubleValue(),
                    transaction.getLabourCharges().doubleValue(),
                    transaction.getTotalAmount().doubleValue()
                );
                billingItems.add(item);
            }
            
            // Load exchange transactions if any
            if (billToEdit.getExchange() != null) {
                Exchange exchange = billToEdit.getExchange();
                List<ExchangeTransaction> exchangeTransactions = exchangeTransactionService.findByExchangeId(exchange.getId());
                exchangeItems.clear();
                sno = 1;
                for (ExchangeTransaction transaction : exchangeTransactions) {
                    ExchangeItem item = new ExchangeItem(
                        sno++,
                        transaction.getItemName(),
                        transaction.getMetalType(),
                        transaction.getRatePerTenGrams().doubleValue(),
                        transaction.getGrossWeight().doubleValue(),
                        transaction.getDeduction().doubleValue(),
                        transaction.getTotalAmount().doubleValue()
                    );
                    exchangeItems.add(item);
                }
                currentExchange = exchange;
            }
            
            // Set discount and GST
            txtDiscount.setText(billToEdit.getDiscount() != null ? billToEdit.getDiscount().toString() : "0");
            txtGSTRate.setText(billToEdit.getGstRate() != null ? billToEdit.getGstRate().toString() : "3");
            
            // Update all totals
            updateBillTotal();
            updateExchangeTotal();
            updateAllTotals();
            
            // Show payment status
            if (billToEdit.getStatus() == Bill.BillStatus.PAID) {
                alert.showSuccess("This bill is fully paid. You can only view or reprint it.");
            } else if (billToEdit.getPendingAmount().compareTo(BigDecimal.ZERO) > 0) {
                alert.showSuccess(String.format("This bill has pending amount: ₹%.2f", billToEdit.getPendingAmount()));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error loading bill data: " + e.getMessage());
        }
    }
}
