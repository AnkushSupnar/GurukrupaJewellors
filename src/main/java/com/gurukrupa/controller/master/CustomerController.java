package com.gurukrupa.controller.master;

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
import javafx.stage.Stage;
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
    @Getter
    @Setter
    private Parent view;
    @Setter
    private Stage dialogStage;
    @Getter
    private boolean saved = false;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnLoad;
    @FXML
    private Button btnSave;
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
        customers.addAll(customerService.getAllCustomers());
        table.setItems(customers);
        btnSave.setOnAction(e->save());
        btnCancel.setOnAction(e->cancel());
    }

    private void save() {
        try {
        if(!validate()){
            return;
        }
        Customer customer = Customer.builder()
                .city(txtCity.getText())
                .alternativeMobile(txtAlterMobile.getText())
                .district(txtDistrict.getText())
                .firstName(txtFname.getText())
                .lastName(txtLName.getText())
                .mobile(txtMobile.getText())
                .pinCode(txtPinCode.getText())
                .taluka(txtTaluka.getText())
                .middleName(txtMName.getText())
                .build();

        customer = customerService.saveCustomer(customer);
        customers.add(customer);

            logger.info("Saving Customer success..{}", customer);
            alert.showSuccess("Customer "+customer.getFirstName()+" "+customer.getMiddleName()+" "+customer.getLastName()+"\n Saved Success");
            saved=true;
            
            // Close the dialog after successful save
            if (dialogStage != null) {
                dialogStage.close();
            }
        }catch (Exception e){
            alert.showError("Error in Customer Saving...."+e.getMessage());
            logger.error("Error in Customer Saving...{}", e.getMessage());
            saved=false;
        }

    }

    private boolean validate() {
        if(txtFname.getText().isEmpty()) {
            txtFname.requestFocus();
            alert.showError("Enter First Name");
            return false;
        }
        if(txtMName.getText().isEmpty()) {
            txtMName.requestFocus();
            alert.showError("Enter Middle Name");
            return false;
        }
        if(txtLName.getText().isEmpty()) {
            txtLName.requestFocus();
            alert.showError("Enter Last Name");
            return false;
        }
        if(txtCity.getText().isEmpty()) {
            txtCity.requestFocus();
            alert.showError("Enter City Name");
            return false;
        }
        if(txtTaluka.getText().isEmpty()) {
            txtTaluka.requestFocus();
            alert.showError("Enter Taluka Name");
            return false;
        }
        if(txtDistrict.getText().isEmpty()) {
            txtDistrict.requestFocus();
            alert.showError("Enter District Name");
            return false;
        }
        if(txtPinCode.getText().isEmpty()) {
            txtPinCode.requestFocus();
            alert.showError("Enter Pin Code");
            return false;
        }
        if(txtMobile.getText().isEmpty()) {
            txtMobile.requestFocus();
            alert.showError("Enter Mobile No");
            return false;
        }
        if(customerService.findCustomerByMobile(txtMobile.getText()).isPresent()){
           txtMobile.requestFocus();
           alert.showError("This mobile no is already registered");
           return false;
        }
        if(txtMobile.getText().length()!=10){
            txtMobile.requestFocus();
            alert.showError("Mobile No must be in 10 Digits");
            return false;
        }
        if(txtAlterMobile.getText().isEmpty()) {
            txtAlterMobile.requestFocus();
            alert.showError("Enter Alternative Mobile No");
            return false;
        }
        if(txtAlterMobile.getText().length()!=10){
            txtAlterMobile.requestFocus();
            alert.showError("Alternative Mobile No must be in 10 Digits");
            return false;
        }
        return true;
    }

    private void cancel() {
        saved = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
