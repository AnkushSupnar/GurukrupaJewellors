package com.gurukrupa.utility;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;

import java.util.Optional;

public class AlertNotification {
    
    public static void showSuccess(String title, String message) {
        showAlert(AlertType.INFORMATION, title, message);
    }
    
    public static void showError(String title, String message) {
        showAlert(AlertType.ERROR, title, message);
    }
    
    public static void showWarning(String title, String message) {
        showAlert(AlertType.WARNING, title, message);
    }
    
    public static void showInfo(String title, String message) {
        showAlert(AlertType.INFORMATION, title, message);
    }
    
    public static Optional<ButtonType> showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(StageStyle.UTILITY);
        return alert.showAndWait();
    }
    
    private static void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();
    }
}