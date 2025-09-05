package com.gurukrupa.controller;

import com.gurukrupa.data.entities.LoginUser;
import com.gurukrupa.data.entities.ShopInfo;
import com.gurukrupa.data.service.ShopService;
import com.gurukrupa.view.AlertNotification;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateShopeController {

    @FXML
    private Button cancelButton;

    @FXML
    private Button registerButton;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtShopAddress;

    @FXML
    private TextField txtShopContact;

    @FXML
    private TextField txtShopName;

    @FXML
    private TextField txtShoplicense;

    @FXML
    private TextField txtWonerContact;

    @FXML
    private TextField txtWonerName;
    @Autowired
    private ShopService shopService;
    @Autowired
    AlertNotification alertNotification;

    @FXML
    private void initialize() {
        registerButton.setOnAction(e -> registerShop());
    }

    private void registerShop() {
        if(!validateInputs()) return;
        ShopInfo shopInfo = ShopInfo.builder()
                .shopAddress(txtShopAddress.getText())
                .shopContactNo(txtShopContact.getText())
                .shopName(txtShopName.getText())
                .shopLicenseKey(txtShoplicense.getText())
                .shopOwnerMobileNo(txtWonerContact.getText())
                .shopOwnerName(txtWonerName.getText())
                .build();

       if( shopService.registerShop(shopInfo,txtPassword.getText()))
           alertNotification.showSuccess("Shop info saved success please relaunch the application");
    }
    private boolean validateInputs() {
        if(txtShopName.getText().isEmpty()){
            alertNotification.showError("Enter Shop Name");
            return false;
        }
        if(txtShopAddress .getText().isEmpty()){
            alertNotification.showError("Enter Shop Address");
            return false;
        }
        if(txtShopContact .getText().isEmpty()){
            alertNotification.showError("Enter Shop Contact No");
            return false;
        }
        if(txtShoplicense .getText().isEmpty()){
            alertNotification.showError("Enter Shop license");
            return false;
        }
        if(txtWonerName .getText().isEmpty()){
            alertNotification.showError("Enter Shop Woner Name");
            return false;
        }
        if(txtWonerContact .getText().isEmpty()){
            alertNotification.showError("Enter Shop Woner Contact");
            return false;
        }
        if(txtPassword .getText().isEmpty()){
            alertNotification.showError("Enter Admin Password");
            return false;
        }
        if(txtConfirmPassword .getText().isEmpty()){
            alertNotification.showError("Enter Admin Confirm Password");
            return false;
        }
        if(!txtPassword .getText().equals(txtConfirmPassword.getText())){
            alertNotification.showError("Password And Confirm Password Must Be Same");
            return false;
        }
        return true;
    }



    private void clearForm() {

    }
}
