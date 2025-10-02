package com.gurukrupa.controller.master;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.service.CustomerService;
import com.gurukrupa.view.AlertNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.collections.transformation.FilteredList;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class CustomerController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    @Autowired
    private AlertNotification alert;
    @Autowired
    CustomerService customerService;
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    @Getter
    @Setter
    private Parent view;
    @Setter
    private Stage dialogStage;
    @Getter
    private boolean saved = false;
    @FXML
    private Button btnBack;
    @FXML
    private Button btnClear;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnUpdate;
    @FXML
    private Button btnRefresh;
    @FXML
    private Text txtFormTitle;
    @FXML
    private TextField txtSearch;
    @FXML
    private TableColumn<Customer, String> colAddress;
    @FXML
    private TableColumn<Customer, String> colAlterMobile;
    @FXML
    private TableColumn<Customer, String> colMobile;
    @FXML
    private TableColumn<Customer, String> colName;
    @FXML
    private TableColumn<Customer, String> colSrNo;
    @FXML
    private TableView<Customer> table;

    private ObservableList<Customer> customers = FXCollections.observableArrayList();
    private FilteredList<Customer> filteredCustomers;
    private Customer currentEditingCustomer = null;
    private boolean isEditMode = false;

    @FXML
    private TextField txtAlterMobile,txtCity, txtDistrict,txtFname,txtLName, txtMName,txtMobile,txtPinCode,txtTaluka;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colAlterMobile.setCellValueFactory(new PropertyValueFactory<>("alternativeMobile"));
        colSrNo.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMobile.setCellValueFactory(new PropertyValueFactory<>("mobile"));
        colName.setCellValueFactory(cellData->new SimpleStringProperty(cellData.getValue().getFirstName()
                +" "+cellData.getValue().getMiddleName()
                +" "+cellData.getValue().getLastName()));
        colAddress.setCellValueFactory(cellData->new SimpleStringProperty("City:"+cellData.getValue().getCity()
        +" Taluka:"+cellData.getValue().getTaluka()
        +" District:"+cellData.getValue().getDistrict()
        +" Pin:"+cellData.getValue().getPinCode()));
        
        // Set up filtered list
        filteredCustomers = new FilteredList<>(customers, p -> true);
        table.setItems(filteredCustomers);
        
        // Load initial data
        loadCustomers();
        
        // Set up event handlers
        setupEventHandlers();
        
        // Set up search functionality
        setupSearch();
        
        // Set up table selection
        setupTableSelection();
        
        logger.info("Customer Controller initialized successfully");
    }
    
    private void setupEventHandlers() {
        btnBack.setOnAction(e -> navigateBackToMasterMenu());
        btnSave.setOnAction(e -> saveCustomer());
        btnUpdate.setOnAction(e -> updateCustomer());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> refreshTable());
    }
    
    private void setupSearch() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredCustomers.setPredicate(customer -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                String fullName = (customer.getFirstName() + " " + 
                                  customer.getMiddleName() + " " + 
                                  customer.getLastName()).toLowerCase();
                
                if (fullName.contains(lowerCaseFilter)) {
                    return true;
                } else if (customer.getMobile().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (customer.getAlternativeMobile() != null && 
                          customer.getAlternativeMobile().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (customer.getCity().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (customer.getDistrict().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
    }
    
    private void setupTableSelection() {
        table.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadCustomerForEditing(newSelection);
                }
            });
    }
    
    private void navigateBackToMasterMenu() {
        try {
            // Get the dashboard's center panel
            BorderPane dashboard = (BorderPane) btnBack.getScene().getRoot();
            
            // Load the Master Menu FXML
            Parent masterMenu = springFXMLLoader.load("/fxml/master/MasterMenu.fxml");
            
            // Set the master menu in the center of the dashboard
            dashboard.setCenter(masterMenu);
            
            logger.info("Navigated back to Master Menu");
            
        } catch (Exception e) {
            logger.error("Error navigating back to Master Menu: {}", e.getMessage());
            alert.showError("Error navigating back: " + e.getMessage());
        }
    }
    
    private void loadCustomers() {
        try {
            customers.clear();
            customers.addAll(customerService.getAllCustomers());
            logger.info("Loaded {} customers", customers.size());
        } catch (Exception e) {
            logger.error("Error loading customers: {}", e.getMessage());
            alert.showError("Error loading customers: " + e.getMessage());
        }
    }
    
    private void refreshTable() {
        loadCustomers();
        clearForm();
        alert.showSuccess("Customer list refreshed successfully");
    }
    
    private void loadCustomerForEditing(Customer customer) {
        currentEditingCustomer = customer;
        isEditMode = true;
        
        // Load customer data into form
        txtFname.setText(customer.getFirstName());
        txtMName.setText(customer.getMiddleName());
        txtLName.setText(customer.getLastName());
        txtCity.setText(customer.getCity());
        txtTaluka.setText(customer.getTaluka());
        txtDistrict.setText(customer.getDistrict());
        txtPinCode.setText(customer.getPinCode());
        txtMobile.setText(customer.getMobile());
        txtAlterMobile.setText(customer.getAlternativeMobile());
        
        // Update UI for edit mode
        txtFormTitle.setText("Edit Customer");
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
    }
    
    private void clearForm() {
        txtFname.clear();
        txtMName.clear();
        txtLName.clear();
        txtCity.clear();
        txtTaluka.clear();
        txtDistrict.clear();
        txtPinCode.clear();
        txtMobile.clear();
        txtAlterMobile.clear();
        txtSearch.clear();
        
        // Reset to add mode
        currentEditingCustomer = null;
        isEditMode = false;
        txtFormTitle.setText("Add New Customer");
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        
        // Clear table selection
        table.getSelectionModel().clearSelection();
    }
    
    private void saveCustomer() {
        try {
            if(!validate()){
                return;
            }
            
            Customer customer = Customer.builder()
                    .city(txtCity.getText().trim())
                    .alternativeMobile(txtAlterMobile.getText().trim())
                    .district(txtDistrict.getText().trim())
                    .firstName(txtFname.getText().trim())
                    .lastName(txtLName.getText().trim())
                    .mobile(txtMobile.getText().trim())
                    .pinCode(txtPinCode.getText().trim())
                    .taluka(txtTaluka.getText().trim())
                    .middleName(txtMName.getText().trim())
                    .build();

            customer = customerService.saveCustomer(customer);
            customers.add(customer);
            alert.showSuccess("Customer saved successfully!");
            clearForm();
            logger.info("Customer saved: {} {}", customer.getFirstName(), customer.getLastName());
            
        } catch (Exception e) {
            alert.showError("Error saving customer: " + e.getMessage());
            logger.error("Error saving customer: {}", e.getMessage());
        }
    }
    
    private void updateCustomer() {
        try {
            if (currentEditingCustomer == null) {
                alert.showError("No customer selected for update");
                return;
            }
            
            if (!validate()) {
                return;
            }
            
            // Update customer fields
            currentEditingCustomer.setFirstName(txtFname.getText().trim());
            currentEditingCustomer.setMiddleName(txtMName.getText().trim());
            currentEditingCustomer.setLastName(txtLName.getText().trim());
            currentEditingCustomer.setCity(txtCity.getText().trim());
            currentEditingCustomer.setTaluka(txtTaluka.getText().trim());
            currentEditingCustomer.setDistrict(txtDistrict.getText().trim());
            currentEditingCustomer.setPinCode(txtPinCode.getText().trim());
            currentEditingCustomer.setMobile(txtMobile.getText().trim());
            currentEditingCustomer.setAlternativeMobile(txtAlterMobile.getText().trim());
            
            // Save to database (JPA save handles update when ID exists)
            Customer updatedCustomer = customerService.saveCustomer(currentEditingCustomer);
            
            // Refresh table to show updated data
            table.refresh();
            
            alert.showSuccess("Customer updated successfully!");
            clearForm();
            logger.info("Customer updated: {} {}", updatedCustomer.getFirstName(), updatedCustomer.getLastName());
            
        } catch (Exception e) {
            alert.showError("Error updating customer: " + e.getMessage());
            logger.error("Error updating customer: {}", e.getMessage());
        }
    }

    private boolean validate() {
        if(txtFname.getText().trim().isEmpty()) {
            txtFname.requestFocus();
            alert.showError("First Name is required");
            return false;
        }
        if(txtLName.getText().trim().isEmpty()) {
            txtLName.requestFocus();
            alert.showError("Last Name is required");
            return false;
        }
        if(txtCity.getText().trim().isEmpty()) {
            txtCity.requestFocus();
            alert.showError("Village/City is required");
            return false;
        }
        if(txtTaluka.getText().trim().isEmpty()) {
            txtTaluka.requestFocus();
            alert.showError("Taluka is required");
            return false;
        }
        if(txtDistrict.getText().trim().isEmpty()) {
            txtDistrict.requestFocus();
            alert.showError("District is required");
            return false;
        }
        if(txtPinCode.getText().trim().isEmpty()) {
            txtPinCode.requestFocus();
            alert.showError("PIN Code is required");
            return false;
        }
        if(!txtPinCode.getText().trim().matches("\\d{6}")) {
            txtPinCode.requestFocus();
            alert.showError("PIN Code must be 6 digits");
            return false;
        }
        if(txtMobile.getText().trim().isEmpty()) {
            txtMobile.requestFocus();
            alert.showError("Mobile Number is required");
            return false;
        }
        if(!txtMobile.getText().trim().matches("\\d{10}")) {
            txtMobile.requestFocus();
            alert.showError("Mobile Number must be 10 digits");
            return false;
        }
        
        // Check for duplicate mobile only when saving new customer
        if (!isEditMode && customerService.findCustomerByMobile(txtMobile.getText().trim()).isPresent()) {
            txtMobile.requestFocus();
            alert.showError("This mobile number is already registered");
            return false;
        }
        
        // Alternative mobile is optional, but if provided, validate it
        if (!txtAlterMobile.getText().trim().isEmpty() && !txtAlterMobile.getText().trim().matches("\\d{10}")) {
            txtAlterMobile.requestFocus();
            alert.showError("Alternative Mobile Number must be 10 digits");
            return false;
        }
        return true;
    }
}