package com.gurukrupa.controller.master;

import com.gurukrupa.data.entities.Supplier;
import com.gurukrupa.data.service.SupplierService;
import com.gurukrupa.view.AlertNotification;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class SupplierController implements Initializable {
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private AlertNotification alertNotification;
    
    @FXML private Button btnClose;
    @FXML private Button btnSave;
    @FXML private Button btnReset;
    
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
    
    private Stage dialogStage;
    private Supplier supplierToEdit;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupEventHandlers();
        setupSupplierTypes();
        setupValidation();
    }
    
    private void setupEventHandlers() {
        btnClose.setOnAction(e -> closeDialog());
        btnSave.setOnAction(e -> saveSupplier());
        btnReset.setOnAction(e -> resetForm());
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
            
            if (isEditMode && supplierToEdit != null) {
                // Update existing supplier
                supplierToEdit.setSupplierName(supplierName);
                supplierToEdit.setCompanyName(companyName.isEmpty() ? null : companyName);
                supplierToEdit.setGstNumber(gstNumber.isEmpty() ? null : gstNumber);
                supplierToEdit.setMobile(mobile);
                supplierToEdit.setAlternateMobile(alternateMobile.isEmpty() ? null : alternateMobile);
                supplierToEdit.setEmail(email.isEmpty() ? null : email);
                supplierToEdit.setAddress(address.isEmpty() ? null : address);
                supplierToEdit.setCity(city.isEmpty() ? null : city);
                supplierToEdit.setState(state.isEmpty() ? null : state);
                supplierToEdit.setPincode(pincode.isEmpty() ? null : pincode);
                supplierToEdit.setContactPerson(contactPerson.isEmpty() ? null : contactPerson);
                supplierToEdit.setSupplierType(supplierType);
                supplierToEdit.setCreditLimit(creditLimit);
                supplierToEdit.setNotes(notes.isEmpty() ? null : notes);
                
                supplier = supplierService.updateSupplier(supplierToEdit);
                alertNotification.showSuccess("Supplier updated successfully!");
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
            }
            
        } catch (IllegalArgumentException e) {
            alertNotification.showError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            alertNotification.showError("Error saving supplier: " + e.getMessage());
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
        txtNotes.clear();
        txtSupplierName.requestFocus();
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public void setSupplierToEdit(Supplier supplier) {
        this.supplierToEdit = supplier;
        this.isEditMode = true;
        
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
        txtNotes.setText(supplier.getNotes() != null ? supplier.getNotes() : "");
        
        // Update button text
        btnSave.setText("Update Supplier");
    }
    
    private void closeDialog() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
}