package com.gurukrupa.view;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AlertNotification {
    public void showSuccess(String msg)
    {
        try {
            Notifications message = Notifications.create().
                    title("Success Message").
                    text(msg).hideAfter(Duration.seconds(5)).
                    position(Pos.TOP_CENTER);
            message.showInformation();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    public void showError(String msg)
    {
        try {
            Notifications message = Notifications.create().
                    title("Error Message").
                    text(msg).hideAfter(Duration.seconds(5)).
                    position(Pos.TOP_CENTER);
            message.showError();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void showInfo(String msg)
    {
        try {
            Notifications message = Notifications.create().
                    title("Information").
                    text(msg).hideAfter(Duration.seconds(5)).
                    position(Pos.TOP_CENTER);
            message.showInformation();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean showConfirmation(String title,String message)
    {
        AtomicBoolean result= new AtomicBoolean(false);
        Alert notice = new Alert(Alert.AlertType.CONFIRMATION);
        notice.setTitle(title);
        notice.setContentText(message);
        ButtonType btnOk = new ButtonType("Yes",ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType("No", ButtonBar.ButtonData.NO);
        ButtonType btnCancel = new ButtonType("Cancel",ButtonBar.ButtonData.CANCEL_CLOSE);
        notice.getButtonTypes().setAll(btnOk,btnNo,btnCancel);
        notice.showAndWait().ifPresent(type->{

            if(type.getText().equals("Yes"))
            {
               result.set(true);
            }
            else if(type.getText().equals("No"))
            {
                result.set(false);
            }
            else
            {
                result.set(false);
            }
        });
        return result.get();

    }
}
