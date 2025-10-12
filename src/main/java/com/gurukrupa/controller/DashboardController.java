package com.gurukrupa.controller;

import com.gurukrupa.common.CommonData;
import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.view.StageManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
@Component
public class DashboardController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    @Autowired @Lazy
    StageManager stageManager;
    @Autowired
    SpringFXMLLoader loader;
    @FXML private Label lblShopeeName;
    @FXML private Label lblCurrentMenu;
    @FXML private BorderPane mainPane;
    @FXML private HBox menuDashboard;
    @FXML private HBox menuPurchase;
    @FXML private HBox menuTransaction;
    @FXML private HBox menuCreate;
    @FXML private HBox menuInventary;
    @FXML private HBox menuMaster;
    @FXML private HBox menuReport;
    @FXML private HBox menuSettings;
    @FXML private HBox menuSupport;
    @FXML private Text txtUserName;
    @FXML private HBox menuExit;

    private Pane pane;
    private List<HBox> menuItems;
    
    // Define styles for selected and unselected menu items with modern material design
    private final String SELECTED_STYLE = "-fx-background-color: #2196F3; -fx-background-radius: 12; -fx-cursor: hand;";
    private final String UNSELECTED_STYLE = "-fx-background-color: #FAFAFA; -fx-background-radius: 12; -fx-cursor: hand;";
    private final String HOVER_STYLE = "-fx-background-color: #EEEEEE; -fx-background-radius: 12; -fx-cursor: hand;";
    private final String LOGOUT_STYLE = "-fx-background-color: #F44336; -fx-background-radius: 12; -fx-cursor: hand;";
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize menu items list
        menuItems = new ArrayList<>();
        menuItems.add(menuDashboard);
        menuItems.add(menuPurchase);
        menuItems.add(menuTransaction);
        menuItems.add(menuMaster);
        menuItems.add(menuReport);
        menuItems.add(menuSettings);
        menuItems.add(menuSupport);
        
        if(CommonData.shopInfo !=null){
            lblShopeeName.setText(CommonData.shopInfo.getShopName());
        }
        if(CommonData.loginUser!=null)
        {
            txtUserName.setText(CommonData.loginUser.getUsername());
        }
        
        // Set Dashboard as initially selected
        setSelectedMenu(menuDashboard, "Dashboard");
        
        menuDashboard.setOnMouseClicked(e->{
            setSelectedMenu(menuDashboard, "Dashboard");
            pane =loader.getPage("/fxml/dashboard/Dashboard.fxml");
            mainPane.setCenter(pane);
        });

        menuPurchase.setOnMouseClicked(e -> {
            setSelectedMenu(menuPurchase, "Purchase");
            pane = loader.getPage("/fxml/purchase/PurchaseMenu.fxml");
            mainPane.setCenter(pane);
        });

        menuTransaction.setOnMouseClicked(e -> {
            setSelectedMenu(menuTransaction, "Daily Billing");
            pane =loader.getPage("/fxml/transaction/TransactionMenu.fxml");
            mainPane.setCenter(pane);
        });
//        menuCreate.setOnMouseClicked(e->{
//            pane =loader.getPage("/fxml/create/CreateMenu.fxml");
//            mainPane.setCenter(pane);
//        });

        menuReport.setOnMouseClicked(e->{
            setSelectedMenu(menuReport, "Reports");
            pane = loader.getPage("/fxml/report/ReportMenu.fxml");
            mainPane.setCenter(pane);
        });
        menuMaster.setOnMouseClicked(e->{
            setSelectedMenu(menuMaster, "Master Data");
            pane =loader.getPage("/fxml/master/MasterMenu.fxml");
            mainPane.setCenter(pane);
        });
        
        menuSettings.setOnMouseClicked(e->{
            setSelectedMenu(menuSettings, "Settings");
            pane = loader.getPage("/fxml/settings/SettingsMenu.fxml");
            mainPane.setCenter(pane);
        });
        
        menuSupport.setOnMouseClicked(e->{
            setSelectedMenu(menuSupport, "Support");
            // Add support page loading logic here when ready
        });
        
        menuExit.setOnMouseClicked(e->System.exit(0));
    }
    
    /**
     * Sets the selected menu item and updates visual states
     */
    private void setSelectedMenu(HBox selectedMenu, String menuName) {
        // Reset all menu items to unselected state
        for (HBox menuItem : menuItems) {
            menuItem.setStyle(UNSELECTED_STYLE);
            updateMenuTextColor(menuItem, "#424242", "#616161"); // Dark text, gray icon
            
            // Add hover effects
            menuItem.setOnMouseEntered(e -> {
                if (menuItem != selectedMenu) {
                    menuItem.setStyle(HOVER_STYLE);
                }
            });
            menuItem.setOnMouseExited(e -> {
                if (menuItem != selectedMenu) {
                    menuItem.setStyle(UNSELECTED_STYLE);
                }
            });
        }
        
        // Set the selected menu item
        selectedMenu.setStyle(SELECTED_STYLE);
        updateMenuTextColor(selectedMenu, "#FFFFFF", "#FFFFFF"); // White text, white icon
        
        // Remove hover effect from selected item
        selectedMenu.setOnMouseEntered(null);
        selectedMenu.setOnMouseExited(null);
        
        // Update header title
        lblCurrentMenu.setText(menuName);
        
        // Keep logout button with its special style
        menuExit.setStyle(LOGOUT_STYLE);
        updateMenuTextColor(menuExit, "#FFFFFF", "#FFFFFF");
    }
    
    /**
     * Updates the text and icon color of a menu item
     */
    private void updateMenuTextColor(HBox menuItem, String textColor, String iconColor) {
        menuItem.getChildren().forEach(node -> {
            if (node instanceof Text) {
                Text text = (Text) node;
                text.setFill(javafx.scene.paint.Color.web(textColor));
            } else if (node instanceof de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon) {
                ((de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon) node).setFill(javafx.scene.paint.Color.web(iconColor));
            } else if (node instanceof javafx.scene.layout.StackPane) {
                // Handle icon inside StackPane
                javafx.scene.layout.StackPane stackPane = (javafx.scene.layout.StackPane) node;
                stackPane.getChildren().forEach(child -> {
                    if (child instanceof de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon) {
                        ((de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon) child).setFill(javafx.scene.paint.Color.web(iconColor));
                    }
                });
            }
        });
    }
}