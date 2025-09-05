package com.gurukrupa.controller;

import com.gurukrupa.data.service.LoginUserService;
import com.gurukrupa.data.service.ShopService;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class LoginController {
    @FXML
    private Button btnCancel;

    @FXML
    private Button btnLogin;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtUserName;

    @Autowired
    ShopService shopService;

    @Autowired @Lazy
    StageManager stageManager;

    @Autowired
    AlertNotification alertNotification;

    @Autowired
    LoginUserService loginUserService;

    @FXML
    private void initialize() {
       btnLogin.setOnAction(event -> login());
    }
    public void login(){

        if(txtUserName.getText().isEmpty()){
            alertNotification.showError("Please Enter User Name");
            return;
        }
        if(txtPassword.getText().isEmpty()){
            alertNotification.showError("Please Enter Password");
            return;
        }
        if(loginUserService.authenticate(txtUserName.getText(),txtPassword.getText())){
            alertNotification.showSuccess("Login Success:\n Welcome "+txtUserName.getText());
            stageManager.switchScene(FxmlView.DASHBOARD);
        }else{
            alertNotification.showError("Wrong User Name Or Password");
        }
    }

}
