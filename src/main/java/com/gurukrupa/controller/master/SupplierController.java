package com.gurukrupa.controller.master;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.data.entities.Supplier;
import com.gurukrupa.data.service.SupplierService;
import com.gurukrupa.view.AlertNotification;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class SupplierController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SupplierController.class);
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private AlertNotification alertNotification;
    
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    
    @FXML private Button btnBack;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnClear;
    @FXML private Button btnRefresh;
    @FXML private Button btnClearFilters;
    @FXML private Text txtFormTitle;
    @FXML private TextField txtSearch;
    @FXML private TextField txtCurrentBalance;
    @FXML private ComboBox<String> cmbFilterType;
    
    @FXML private TextField txtSupplierName;
    @FXML private TextField txtCompanyName;
    @FXML private TextField txtGstNumber;
    @FXML private ComboBox<String> cmbSupplierType;
    
    @FXML private TextField txtMobile;
    @FXML private TextField txtAlternateMobile;
    @FXML private TextField txtEmail;
    @FXML private TextField txtContactPerson;
    
    @FXML private TextArea txtAddress;
    @FXML private TextField txtCity;
    @FXML private TextField txtState;
    @FXML private TextField txtPincode;
    
    @FXML private TextField txtCreditLimit;
    @FXML private TextArea txtNotes;
    
    // Table columns
    @FXML private TableView<Supplier> tableSuppliers;
    @FXML private TableColumn<Supplier, Long> colId;
    @FXML private TableColumn<Supplier, String> colSupplierName;
    @FXML private TableColumn<Supplier, String> colCompanyName;
    @FXML private TableColumn<Supplier, String> colMobile;
    @FXML private TableColumn<Supplier, String> colGstNumber;
    @FXML private TableColumn<Supplier, String> colType;
    @FXML private TableColumn<Supplier, String> colBalance;
    @FXML private TableColumn<Supplier, String> colStatus;
    
    private ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
    private FilteredList<Supplier> filteredSuppliers;
    private Supplier currentEditingSupplier = null;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupEventHandlers();
        setupSupplierTypes();
        setupValidation();
        setupFilters();
        setupSearch();
        setupTableSelection();
        
        // Load initial data
        loadSuppliers();
        
        logger.info("Supplier Controller initialized successfully");
    }
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSupplierName.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        colCompanyName.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colMobile.setCellValueFactory(new PropertyValueFactory<>("mobile"));
        colGstNumber.setCellValueFactory(new PropertyValueFactory<>("gstNumber"));
        colType.setCellValueFactory(cellData -> {
            String type = cellData.getValue().getSupplierType();
            if (type != null) {
                // Convert GOLD_SUPPLIER to Gold
                type = type.replace("_SUPPLIER", "")
                          .replace("_", " ")
                          .toLowerCase();
                if (!type.isEmpty()) {
                    type = type.substring(0, 1).toUpperCase() + type.substring(1);
                }
            }
            return new SimpleStringProperty(type);
        });
        colBalance.setCellValueFactory(cellData -> 
            new SimpleStringProperty("₹ " + cellData.getValue().getCurrentBalance().toString())
        );
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getIsActive() ? "Active" : "Inactive")
        );
        
        // Set up filtered list
        filteredSuppliers = new FilteredList<>(suppliers, p -> true);
        tableSuppliers.setItems(filteredSuppliers);
    }
    
    private void setupEventHandlers() {
        btnBack.setOnAction(e -> navigateBackToMasterMenu());
        btnSave.setOnAction(e -> saveSupplier());
        btnUpdate.setOnAction(e -> updateSupplier());
        btnClear.setOnAction(e -> resetForm());
        btnRefresh.setOnAction(e -> refreshTable());
        btnClearFilters.setOnAction(e -> clearFilters());
    }
    
    private void setupSupplierTypes() {
        List<String> supplierTypes = Arrays.asList(
            "GOLD_SUPPLIER",
            "SILVER_SUPPLIER",
            "DIAMOND_SUPPLIER",
            "GEMSTONE_SUPPLIER",
            "JEWELRY_SUPPLIER",
            "RAW_MATERIAL_SUPPLIER",
            "EXCHANGE_DEALER",
            "OTHER"
        );
        cmbSupplierType.setItems(FXCollections.observableArrayList(supplierTypes));
        cmbSupplierType.getSelectionModel().selectFirst();
        
        // Setup filter types
        List<String> filterTypes = new ArrayList<>();
        filterTypes.add("All Supplier Types");
        filterTypes.addAll(supplierTypes);
        cmbFilterType.setItems(FXCollections.observableArrayList(filterTypes));
        cmbFilterType.getSelectionModel().selectFirst();
    }
    
    private void setupValidation() {
        // Mobile number validation - only digits
        txtMobile.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtMobile.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (txtMobile.getText().length() > 10) {
                txtMobile.setText(txtMobile.getText().substring(0, 10));
            }
        });
        
        txtAlternateMobile.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtAlternateMobile.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (txtAlternateMobile.getText().length() > 10) {
                txtAlternateMobile.setText(txtAlternateMobile.getText().substring(0, 10));
            }
        });
        
        // Pincode validation
        txtPincode.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtPincode.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (txtPincode.getText().length() > 6) {
                txtPincode.setText(txtPincode.getText().substring(0, 6));
            }
        });
        
        // Credit limit validation - numbers and decimal
        txtCreditLimit.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                txtCreditLimit.setText(oldVal);
            }
        });
    }
    
    private void setupFilters() {
        cmbFilterType.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }
    
    private void setupSearch() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }
    
    private void setupTableSelection() {
        tableSuppliers.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadSupplierForEditing(newSelection);
                }
            });
    }
    
    private void applyFilters() {
        filteredSuppliers.setPredicate(supplier -> {
            boolean matchesSearch = true;
            boolean matchesType = true;
            
            // Search filter
            String searchText = txtSearch.getText();
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                matchesSearch = supplier.getSupplierName().toLowerCase().contains(lowerCaseFilter) ||
                               (supplier.getCompanyName() != null && supplier.getCompanyName().toLowerCase().contains(lowerCaseFilter)) ||
                               supplier.getMobile().contains(lowerCaseFilter) ||
                               (supplier.getGstNumber() != null && supplier.getGstNumber().toLowerCase().contains(lowerCaseFilter));
            }
            
            // Type filter
            String selectedType = cmbFilterType.getValue();
            if (selectedType != null && !selectedType.equals("All Supplier Types")) {
                matchesType = selectedType.equals(supplier.getSupplierType());
            }
            
            return matchesSearch && matchesType;
        });
    }
    
    private void clearFilters() {
        txtSearch.clear();
        cmbFilterType.getSelectionModel().selectFirst();
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
            alertNotification.showError("Error navigating back: " + e.getMessage());
        }
    }
    
    private void loadSuppliers() {
        try {
            suppliers.clear();
            suppliers.addAll(supplierService.getAllActiveSuppliers());
            logger.info("Loaded {} suppliers", suppliers.size());
        } catch (Exception e) {
            logger.error("Error loading suppliers: {}", e.getMessage());
            alertNotification.showError("Error loading suppliers: " + e.getMessage());
        }
    }
    
    private void refreshTable() {
        loadSuppliers();
        clearFilters();
        resetForm();
        alertNotification.showSuccess("Supplier list refreshed successfully");
    }
    
    private void loadSupplierForEditing(Supplier supplier) {
        currentEditingSupplier = supplier;
        isEditMode = true;
        
        // Populate form with supplier data
        txtSupplierName.setText(supplier.getSupplierName());
        txtCompanyName.setText(supplier.getCompanyName() != null ? supplier.getCompanyName() : "");
        txtGstNumber.setText(supplier.getGstNumber() != null ? supplier.getGstNumber() : "");
        cmbSupplierType.setValue(supplier.getSupplierType());
        txtMobile.setText(supplier.getMobile());
        txtAlternateMobile.setText(supplier.getAlternateMobile() != null ? supplier.getAlternateMobile() : "");
        txtEmail.setText(supplier.getEmail() != null ? supplier.getEmail() : "");
        txtContactPerson.setText(supplier.getContactPerson() != null ? supplier.getContactPerson() : "");
        txtAddress.setText(supplier.getAddress() != null ? supplier.getAddress() : "");
        txtCity.setText(supplier.getCity() != null ? supplier.getCity() : "");
        txtState.setText(supplier.getState() != null ? supplier.getState() : "");
        txtPincode.setText(supplier.getPincode() != null ? supplier.getPincode() : "");
        txtCreditLimit.setText(supplier.getCreditLimit().toString());
        txtCurrentBalance.setText("₹ " + supplier.getCurrentBalance().toString());
        txtNotes.setText(supplier.getNotes() != null ? supplier.getNotes() : "");
        
        // Update UI for edit mode
        txtFormTitle.setText("Edit Supplier");
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
    }
    
    private void saveSupplier() {
        if (!validateForm()) {
            return;
        }
        
        try {
            String supplierName = txtSupplierName.getText().trim();
            String companyName = txtCompanyName.getText().trim();
            String gstNumber = txtGstNumber.getText().trim().toUpperCase();
            String mobile = txtMobile.getText().trim();
            String alternateMobile = txtAlternateMobile.getText().trim();
            String email = txtEmail.getText().trim();
            String address = txtAddress.getText().trim();
            String city = txtCity.getText().trim();
            String state = txtState.getText().trim();
            String pincode = txtPincode.getText().trim();
            String contactPerson = txtContactPerson.getText().trim();
            String supplierType = cmbSupplierType.getValue();
            
            BigDecimal creditLimit = BigDecimal.ZERO;
            if (!txtCreditLimit.getText().trim().isEmpty()) {
                creditLimit = new BigDecimal(txtCreditLimit.getText().trim());
            }
            
            String notes = txtNotes.getText().trim();
            
            Supplier supplier;
            
            if (isEditMode && currentEditingSupplier != null) {
                // Should never reach here as we use updateSupplier method
                alertNotification.showError("Please use Update button to update supplier");
                return;
            } else {
                // Create new supplier
                supplier = supplierService.createSupplier(
                    supplierName,
                    companyName.isEmpty() ? null : companyName,
                    gstNumber.isEmpty() ? null : gstNumber,
                    mobile,
                    alternateMobile.isEmpty() ? null : alternateMobile,
                    email.isEmpty() ? null : email,
                    address.isEmpty() ? null : address,
                    city.isEmpty() ? null : city,
                    state.isEmpty() ? null : state,
                    pincode.isEmpty() ? null : pincode,
                    contactPerson.isEmpty() ? null : contactPerson,
                    supplierType,
                    creditLimit,
                    notes.isEmpty() ? null : notes
                );
                alertNotification.showSuccess("Supplier added successfully!");
                resetForm();
                loadSuppliers();
            }
            
        } catch (IllegalArgumentException e) {
            alertNotification.showError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            alertNotification.showError("Error saving supplier: " + e.getMessage());
        }
    }
    
    private void updateSupplier() {
        try {
            if (currentEditingSupplier == null) {
                alertNotification.showError("No supplier selected for update");
                return;
            }
            
            if (!validateForm()) {
                return;
            }
            
            // Update supplier fields
            currentEditingSupplier.setSupplierName(txtSupplierName.getText().trim());
            currentEditingSupplier.setCompanyName(txtCompanyName.getText().trim().isEmpty() ? null : txtCompanyName.getText().trim());
            currentEditingSupplier.setGstNumber(txtGstNumber.getText().trim().isEmpty() ? null : txtGstNumber.getText().trim().toUpperCase());
            currentEditingSupplier.setMobile(txtMobile.getText().trim());
            currentEditingSupplier.setAlternateMobile(txtAlternateMobile.getText().trim().isEmpty() ? null : txtAlternateMobile.getText().trim());
            currentEditingSupplier.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
            currentEditingSupplier.setAddress(txtAddress.getText().trim().isEmpty() ? null : txtAddress.getText().trim());
            currentEditingSupplier.setCity(txtCity.getText().trim().isEmpty() ? null : txtCity.getText().trim());
            currentEditingSupplier.setState(txtState.getText().trim().isEmpty() ? null : txtState.getText().trim());
            currentEditingSupplier.setPincode(txtPincode.getText().trim().isEmpty() ? null : txtPincode.getText().trim());
            currentEditingSupplier.setContactPerson(txtContactPerson.getText().trim().isEmpty() ? null : txtContactPerson.getText().trim());
            currentEditingSupplier.setSupplierType(cmbSupplierType.getValue());
            
            BigDecimal creditLimit = BigDecimal.ZERO;
            if (!txtCreditLimit.getText().trim().isEmpty()) {
                creditLimit = new BigDecimal(txtCreditLimit.getText().trim());
            }
            currentEditingSupplier.setCreditLimit(creditLimit);
            currentEditingSupplier.setNotes(txtNotes.getText().trim().isEmpty() ? null : txtNotes.getText().trim());
            
            // Update supplier
            Supplier updatedSupplier = supplierService.updateSupplier(currentEditingSupplier);
            
            // Update in the table
            int index = suppliers.indexOf(currentEditingSupplier);
            if (index >= 0) {
                suppliers.set(index, updatedSupplier);
            }
            
            // Refresh table to show updated data
            tableSuppliers.refresh();
            
            alertNotification.showSuccess("Supplier updated successfully!");
            resetForm();
            logger.info("Supplier updated: {}", updatedSupplier.getSupplierFullName());
            
        } catch (Exception e) {
            logger.error("Error updating supplier: {}", e.getMessage());
            alertNotification.showError("Error updating supplier: " + e.getMessage());
        }
    }
    
    private boolean validateForm() {
        if (txtSupplierName.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter supplier name");
            txtSupplierName.requestFocus();
            return false;
        }
        
        if (txtMobile.getText().trim().isEmpty()) {
            alertNotification.showError("Please enter mobile number");
            txtMobile.requestFocus();
            return false;
        }
        
        if (txtMobile.getText().trim().length() != 10) {
            alertNotification.showError("Mobile number must be 10 digits");
            txtMobile.requestFocus();
            return false;
        }
        
        // Validate email format if provided
        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            alertNotification.showError("Please enter a valid email address");
            txtEmail.requestFocus();
            return false;
        }
        
        // Validate GST number format if provided
        String gst = txtGstNumber.getText().trim();
      /*  if (!gst.isEmpty() && !gst.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) {
            alertNotification.showError("Please enter a valid GST number");
            txtGstNumber.requestFocus();
            return false;
        }*/
        
        return true;
    }
    
    private void resetForm() {
        txtSupplierName.clear();
        txtCompanyName.clear();
        txtGstNumber.clear();
        cmbSupplierType.getSelectionModel().selectFirst();
        txtMobile.clear();
        txtAlternateMobile.clear();
        txtEmail.clear();
        txtContactPerson.clear();
        txtAddress.clear();
        txtCity.clear();
        txtState.clear();
        txtPincode.clear();
        txtCreditLimit.clear();
        txtCurrentBalance.setText("₹ 0.00");
        txtNotes.clear();
        txtSupplierName.requestFocus();
        
        // Reset to add mode
        currentEditingSupplier = null;
        isEditMode = false;
        txtFormTitle.setText("Add New Supplier");
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        
        // Clear table selection
        tableSuppliers.getSelectionModel().clearSelection();
    }
}