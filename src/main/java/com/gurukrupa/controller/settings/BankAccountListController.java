package com.gurukrupa.controller.settings;

import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.entities.BankTransaction;
import com.gurukrupa.data.service.BankAccountService;
import com.gurukrupa.data.service.BankTransactionService;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class BankAccountListController implements Initializable {

    @Autowired
    @Lazy
    private StageManager stageManager;

    @Autowired
    private BankAccountService bankAccountService;
    
    @Autowired
    private BankTransactionService bankTransactionService;

    @Autowired
    private AlertNotification alert;

    @FXML
    private TextField txtSearch;
    
    @FXML
    private Label lblTotalAccounts, lblTotalBalance, lblActiveAccounts;
    
    @FXML
    private Button btnRefresh, btnAddBank, btnBack;
    
    @FXML
    private FlowPane bankCardsContainer;
    
    private ObservableList<BankAccount> bankAccountsList = FXCollections.observableArrayList();
    private FilteredList<BankAccount> filteredList;
    private Stage dialogStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize button actions
        btnRefresh.setOnAction(e -> loadBankAccounts());
        btnAddBank.setOnAction(e -> openAddBankAccount());
        btnBack.setOnAction(e -> goBack());
        
        // Initialize search functionality
        filteredList = new FilteredList<>(bankAccountsList, p -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(bankAccount -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return bankAccount.getBankName().toLowerCase().contains(lowerCaseFilter) ||
                       bankAccount.getAccountNumber().toLowerCase().contains(lowerCaseFilter) ||
                       bankAccount.getAccountHolderName().toLowerCase().contains(lowerCaseFilter);
            });
            updateBankCards();
        });
        
        // Load data
        loadBankAccounts();
    }
    
    private void updateBankCards() {
        bankCardsContainer.getChildren().clear();
        
        for (BankAccount account : filteredList) {
            VBox card = createBankCard(account);
            bankCardsContainer.getChildren().add(card);
        }
    }
    
    private VBox createBankCard(BankAccount account) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-padding: 25; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 3); " +
                      "-fx-pref-width: 360; -fx-pref-height: 280;");
        
        // Header with bank icon and name
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Bank icon with colored background
        StackPane iconContainer = new StackPane();
        iconContainer.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 50; " +
                               "-fx-pref-width: 60; -fx-pref-height: 60;");
        FontAwesomeIcon bankIcon = new FontAwesomeIcon();
        bankIcon.setGlyphName("UNIVERSITY");
        bankIcon.setSize("2em");
        bankIcon.setFill(Color.web("#1976D2"));
        iconContainer.getChildren().add(bankIcon);
        
        // Bank info
        VBox bankInfo = new VBox(4);
        Label bankName = new Label(account.getBankName());
        bankName.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #263238;");
        
        Label accountNumber = new Label("A/C: " + account.getAccountNumber());
        accountNumber.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #546E7A;");
        
        bankInfo.getChildren().addAll(bankName, accountNumber);
        
        // Status badge
        Label statusBadge = new Label(account.getIsActive() ? "Active" : "Inactive");
        statusBadge.setStyle(account.getIsActive() ? 
            "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 6 16 6 16; " +
            "-fx-background-radius: 12; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: 600; " +
            "-fx-min-width: 80;" :
            "-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 6 16 6 16; " +
            "-fx-background-radius: 12; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: 600; " +
            "-fx-min-width: 80;");
        statusBadge.setAlignment(Pos.CENTER);
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        header.getChildren().addAll(iconContainer, bankInfo, headerSpacer, statusBadge);
        
        // Account details
        VBox details = new VBox(8);
        details.setStyle("-fx-padding: 15 0 15 0;");
        
        // Account holder
        HBox holderRow = createDetailRow("USER", "Account Holder", account.getAccountHolderName(), "#757575");
        
        // Account type and IFSC
        HBox typeRow = createDetailRow("CREDIT_CARD", "Account Type", account.getAccountType().getDisplayName(), "#757575");
        HBox ifscRow = createDetailRow("CODE", "IFSC Code", account.getIfscCode(), "#757575");
        
        details.getChildren().addAll(holderRow, typeRow, ifscRow);
        
        // Balance section
        VBox balanceSection = new VBox(5);
        balanceSection.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-padding: 15;");
        
        Label balanceLabel = new Label("Current Balance");
        balanceLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #757575;");
        
        Label balanceAmount = new Label(String.format("₹ %,.2f", account.getCurrentBalance()));
        balanceAmount.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 24px; -fx-font-weight: 700; " +
                               "-fx-text-fill: " + (account.getCurrentBalance().compareTo(BigDecimal.ZERO) >= 0 ? "#2E7D32" : "#D32F2F") + ";");
        
        balanceSection.getChildren().addAll(balanceLabel, balanceAmount);
        
        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        
        Button btnView = createActionButton("VIEW", "#2196F3", e -> viewBankAccount(account));
        Button btnEdit = createActionButton("EDIT", "#4CAF50", e -> editBankAccount(account));
        Button btnTransactions = createActionButton("TRANSACTIONS", "#FF9800", e -> viewTransactions(account));
        
        actions.getChildren().addAll(btnView, btnEdit, btnTransactions);
        
        // Add all components to card
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        card.getChildren().addAll(header, details, balanceSection, spacer, actions);
        
        return card;
    }
    
    private HBox createDetailRow(String iconName, String label, String value, String color) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName(iconName);
        icon.setSize("1em");
        icon.setFill(Color.web(color));
        
        Label labelText = new Label(label + ":");
        labelText.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #757575;");
        
        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #424242; -fx-font-weight: 600;");
        
        row.getChildren().addAll(icon, labelText, valueText);
        return row;
    }
    
    private Button createActionButton(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: 600; " +
                        "-fx-background-radius: 18; -fx-padding: 6 16 6 16; -fx-cursor: hand;");
        button.setOnAction(handler);
        
        // Add hover effect
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: derive(" + color + ", -10%); -fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: 600; " +
                        "-fx-background-radius: 18; -fx-padding: 6 16 6 16; -fx-cursor: hand;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: 600; " +
                        "-fx-background-radius: 18; -fx-padding: 6 16 6 16; -fx-cursor: hand;"));
        
        return button;
    }
    
    private FontAwesomeIcon createIcon(String iconName, String color) {
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName(iconName);
        icon.setSize("1.2em");
        icon.setFill(Color.web(color));
        return icon;
    }
    
    private void loadBankAccounts() {
        try {
            List<BankAccount> accounts = bankAccountService.getAllActiveBankAccounts();
            bankAccountsList.clear();
            bankAccountsList.addAll(accounts);
            
            // Update summary cards
            updateSummaryCards();
            
            // Update bank cards display
            updateBankCards();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error loading bank accounts: " + e.getMessage());
        }
    }
    
    private void updateSummaryCards() {
        // Total accounts
        int totalAccounts = bankAccountsList.size();
        lblTotalAccounts.setText(String.valueOf(totalAccounts));
        
        // Active accounts
        long activeAccounts = bankAccountsList.stream()
            .filter(BankAccount::getIsActive)
            .count();
        lblActiveAccounts.setText(String.valueOf(activeAccounts));
        
        // Total balance
        BigDecimal totalBalance = bankAccountService.getTotalBankBalance();
        lblTotalBalance.setText(String.format("₹ %.2f", totalBalance));
    }
    
    private void openAddBankAccount() {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and controller
            Map.Entry<Parent, BankAccountFormController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.BANKACCOUNTFORM.getFxmlFile(), BankAccountFormController.class);
            
            Parent root = entry.getKey();
            BankAccountFormController controller = entry.getValue();
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Add New Bank Account");
            dialog.setResizable(false);
            
            // Show the dialog and wait
            dialog.showAndWait();
            
            // Refresh the list after adding
            loadBankAccounts();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error opening add bank account form: " + e.getMessage());
        }
    }
    
    private void viewBankAccount(BankAccount account) {
        // Create a detail view dialog
        Alert detailAlert = new Alert(Alert.AlertType.INFORMATION);
        detailAlert.setTitle("Bank Account Details");
        detailAlert.setHeaderText(account.getBankName() + " - " + account.getAccountNumber());
        
        StringBuilder details = new StringBuilder();
        details.append("Account Holder: ").append(account.getAccountHolderName()).append("\n");
        details.append("Account Type: ").append(account.getAccountType().getDisplayName()).append("\n");
        details.append("IFSC Code: ").append(account.getIfscCode()).append("\n");
        details.append("Branch: ").append(account.getBranchName()).append("\n");
        details.append("Branch Address: ").append(account.getBranchAddress()).append("\n");
        details.append("Opening Balance: ₹").append(String.format("%.2f", account.getOpeningBalance())).append("\n");
        details.append("Current Balance: ₹").append(String.format("%.2f", account.getCurrentBalance())).append("\n");
        details.append("Balance Type: ").append(account.getBalanceType().getDisplayName()).append("\n");
        details.append("Status: ").append(account.getIsActive() ? "Active" : "Inactive").append("\n");
        details.append("Created Date: ").append(account.getCreatedDate()).append("\n");
        if (account.getUpdatedDate() != null) {
            details.append("Last Updated: ").append(account.getUpdatedDate());
        }
        
        detailAlert.setContentText(details.toString());
        detailAlert.showAndWait();
    }
    
    private void editBankAccount(BankAccount account) {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and controller
            Map.Entry<Parent, BankAccountFormController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.BANKACCOUNTFORM.getFxmlFile(), BankAccountFormController.class);
            
            Parent root = entry.getKey();
            BankAccountFormController controller = entry.getValue();
            
            // Set the account for editing
            controller.setEditMode(account);
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Edit Bank Account");
            dialog.setResizable(false);
            
            // Show the dialog and wait
            dialog.showAndWait();
            
            // Refresh the list after editing
            loadBankAccounts();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error opening edit bank account form: " + e.getMessage());
        }
    }
    
    private void viewTransactions(BankAccount account) {
        try {
            // Get recent transactions
            List<BankTransaction> transactions = bankTransactionService.findByBankAccount(account.getId());
            
            if (transactions.isEmpty()) {
                alert.showError("No Transactions, No transactions found for this bank account.");
                return;
            }
            
            // Create a custom dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(dialogStage != null ? dialogStage : stageManager.getPrimaryStage());
            dialog.setTitle("Bank Transactions - " + account.getBankName());
            
            // Main container
            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: #F5F5F5;");
            
            // Header
            VBox header = new VBox(10);
            header.setStyle("-fx-background-color: #1976D2; -fx-padding: 20;");
            
            Label bankNameLabel = new Label(account.getBankName());
            bankNameLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: white;");
            
            Label accountNumberLabel = new Label("Account: " + account.getAccountNumber());
            accountNumberLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-text-fill: white;");
            
            HBox balanceBox = new HBox(15);
            balanceBox.setAlignment(Pos.CENTER_LEFT);
            Label currentBalanceLabel = new Label("Current Balance:");
            currentBalanceLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-text-fill: white;");
            Label balanceAmountLabel = new Label(String.format("₹ %,.2f", account.getCurrentBalance()));
            balanceAmountLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: white;");
            balanceBox.getChildren().addAll(currentBalanceLabel, balanceAmountLabel);
            
            header.getChildren().addAll(bankNameLabel, accountNumberLabel, balanceBox);
            root.setTop(header);
            
            // Transactions container
            VBox transactionsContainer = new VBox(10);
            transactionsContainer.setStyle("-fx-padding: 20;");
            
            // Transactions list
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            
            VBox transactionsList = new VBox(10);
            transactionsList.setStyle("-fx-padding: 10;");
            
            // Create transaction cards
            int count = 0;
            for (BankTransaction transaction : transactions) {
                if (count++ >= 50) break; // Show up to 50 transactions
                
                HBox transactionCard = createTransactionCard(transaction);
                transactionsList.getChildren().add(transactionCard);
            }
            
            scrollPane.setContent(transactionsList);
            transactionsContainer.getChildren().add(scrollPane);
            root.setCenter(transactionsContainer);
            
            // Footer with close button
            HBox footer = new HBox();
            footer.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 1 0 0 0;");
            footer.setAlignment(Pos.CENTER_RIGHT);
            
            Button closeButton = new Button("CLOSE");
            closeButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; " +
                               "-fx-font-family: 'Segoe UI'; -fx-font-weight: 600; " +
                               "-fx-background-radius: 20; -fx-padding: 10 20 10 20; -fx-cursor: hand;");
            closeButton.setOnAction(e -> dialog.close());
            
            footer.getChildren().add(closeButton);
            root.setBottom(footer);
            
            // Set up the dialog
            Scene scene = new Scene(root, 800, 600);
            dialog.setScene(scene);
            dialog.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error loading transactions: " + e.getMessage());
        }
    }
    
    private HBox createTransactionCard(BankTransaction transaction) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 15; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 4, 0, 0, 1);");
        
        // Transaction type icon
        StackPane iconContainer = new StackPane();
        iconContainer.setStyle("-fx-background-color: " + 
                              (transaction.getTransactionType() == BankTransaction.TransactionType.CREDIT ? "#E8F5E9" : "#FFEBEE") + 
                              "; -fx-background-radius: 50; -fx-pref-width: 40; -fx-pref-height: 40;");
        
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName(transaction.getTransactionType() == BankTransaction.TransactionType.CREDIT ? "ARROW_DOWN" : "ARROW_UP");
        icon.setSize("1.2em");
        icon.setFill(Color.web(transaction.getTransactionType() == BankTransaction.TransactionType.CREDIT ? "#4CAF50" : "#F44336"));
        iconContainer.getChildren().add(icon);
        
        // Transaction details
        VBox details = new VBox(5);
        details.setMinWidth(300);
        
        Label descriptionLabel = new Label(transaction.getDescription() != null ? transaction.getDescription() : transaction.buildDescription());
        descriptionLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #263238;");
        
        Label dateLabel = new Label(transaction.getTransactionDate().toString());
        dateLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #757575;");
        
        details.getChildren().addAll(descriptionLabel, dateLabel);
        
        // Amount
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        VBox amountBox = new VBox(5);
        amountBox.setAlignment(Pos.CENTER_RIGHT);
        
        Label amountLabel = new Label((transaction.getTransactionType() == BankTransaction.TransactionType.CREDIT ? "+" : "-") + 
                                     String.format("₹ %,.2f", transaction.getAmount()));
        amountLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: 700; " +
                            "-fx-text-fill: " + (transaction.getTransactionType() == BankTransaction.TransactionType.CREDIT ? "#4CAF50" : "#F44336") + ";");
        
        Label balanceLabel = new Label("Balance: ₹ " + String.format("%,.2f", transaction.getBalanceAfterTransaction()));
        balanceLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #757575;");
        
        amountBox.getChildren().addAll(amountLabel, balanceLabel);
        
        card.getChildren().addAll(iconContainer, details, spacer, amountBox);
        
        return card;
    }
    
    private void goBack() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            stageManager.switchScene(FxmlView.BUSINESS_SETTINGS);
        }
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
}