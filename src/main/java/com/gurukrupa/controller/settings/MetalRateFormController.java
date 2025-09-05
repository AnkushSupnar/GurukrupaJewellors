package com.gurukrupa.controller.settings;

import com.gurukrupa.data.entities.LoginUser;
import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.entities.MetalRate;
import com.gurukrupa.data.service.MetalRateService;
import com.gurukrupa.data.service.MetalService;
import com.gurukrupa.view.AlertNotification;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class MetalRateFormController implements Initializable {
    
    @Autowired
    private MetalService metalService;
    
    @Autowired
    private MetalRateService metalRateService;
    
    @Autowired
    private AlertNotification alert;
    
    // Form controls
    @FXML private DatePicker dpRateDate;
    @FXML private ComboBox<String> cmbMetalType;
    @FXML private ComboBox<Metal> cmbMetalName;
    @FXML private TextField txtRatePerTenGrams;
    @FXML private TextField txtBuyingRate;
    @FXML private TextField txtSellingRate;
    @FXML private TextField txtRemarks;
    
    // Buttons
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnClear;
    @FXML private Button btnDelete;
    
    // Search controls
    @FXML private DatePicker dpSearchDate;
    @FXML private ComboBox<String> cmbSearchMetalType;
    @FXML private Button btnSearch;
    @FXML private Button btnShowAll;
    
    // Table
    @FXML private TableView<MetalRate> tblMetalRates;
    @FXML private TableColumn<MetalRate, Integer> colNo;
    @FXML private TableColumn<MetalRate, String> colDate;
    @FXML private TableColumn<MetalRate, String> colMetalType;
    @FXML private TableColumn<MetalRate, String> colMetalName;
    @FXML private TableColumn<MetalRate, String> colPurity;
    @FXML private TableColumn<MetalRate, String> colRatePerGram;
    @FXML private TableColumn<MetalRate, String> colRatePerTenGrams;
    @FXML private TableColumn<MetalRate, String> colBuyingRate;
    @FXML private TableColumn<MetalRate, String> colSellingRate;
    @FXML private TableColumn<MetalRate, String> colRemarks;
    
    private ObservableList<MetalRate> metalRatesList = FXCollections.observableArrayList();
    private MetalRate selectedMetalRate;
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupControls();
        setupTable();
        loadData();
        setupEventHandlers();
    }
    
    private void setupControls() {
        // Set default date to today
        dpRateDate.setValue(LocalDate.now());
        
        // Configure metal name ComboBox to display metal name
        cmbMetalName.setConverter(new StringConverter<Metal>() {
            @Override
            public String toString(Metal metal) {
                return metal != null ? metal.getMetalName() : "";
            }
            
            @Override
            public Metal fromString(String string) {
                return null;
            }
        });
        
        // Numeric validation for rate fields
        addNumericValidation(txtRatePerTenGrams);
        addNumericValidation(txtBuyingRate);
        addNumericValidation(txtSellingRate);
    }
    
    private void setupTable() {
        // Setup table columns
        colNo.setCellFactory(column -> new TableCell<MetalRate, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRateDate().format(dateFormatter)));
        
        colMetalType.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMetal().getMetalType()));
        
        colMetalName.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMetal().getMetalName()));
        
        colPurity.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMetal().getPurity()));
        
        colRatePerGram.setCellValueFactory(cellData -> 
            new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getRatePerGram())));
        
        colRatePerTenGrams.setCellValueFactory(cellData -> 
            new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getRatePerTenGrams())));
        
        colBuyingRate.setCellValueFactory(cellData -> {
            BigDecimal buyingRate = cellData.getValue().getBuyingRate();
            return new SimpleStringProperty(buyingRate != null ? currencyFormatter.format(buyingRate) : "-");
        });
        
        colSellingRate.setCellValueFactory(cellData -> {
            BigDecimal sellingRate = cellData.getValue().getSellingRate();
            return new SimpleStringProperty(sellingRate != null ? currencyFormatter.format(sellingRate) : "-");
        });
        
        colRemarks.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRemarks() != null ? 
                cellData.getValue().getRemarks() : ""));
        
        tblMetalRates.setItems(metalRatesList);
        
        // Add selection listener
        tblMetalRates.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedMetalRate = newSelection;
                    populateForm(newSelection);
                }
            }
        );
    }
    
    private void loadData() {
        // Load distinct metal types
        List<String> metalTypes = metalService.getAllActiveMetals().stream()
                .map(Metal::getMetalType)
                .distinct()
                .collect(Collectors.toList());
        
        cmbMetalType.setItems(FXCollections.observableArrayList(metalTypes));
        
        // Add "All" option for search
        List<String> searchMetalTypes = metalTypes.stream().collect(Collectors.toList());
        searchMetalTypes.add(0, "All");
        cmbSearchMetalType.setItems(FXCollections.observableArrayList(searchMetalTypes));
        cmbSearchMetalType.setValue("All");
        
        // Load today's rates
        loadTodaysRates();
    }
    
    private void setupEventHandlers() {
        // Metal type selection change
        cmbMetalType.setOnAction(e -> {
            String selectedType = cmbMetalType.getValue();
            if (selectedType != null) {
                List<Metal> metals = metalService.getAllActiveMetals().stream()
                        .filter(m -> m.getMetalType().equals(selectedType))
                        .collect(Collectors.toList());
                cmbMetalName.setItems(FXCollections.observableArrayList(metals));
                cmbMetalName.setValue(null);
            }
        });
    }
    
    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateForm()) return;
        
        try {
            MetalRate metalRate = MetalRate.builder()
                    .metal(cmbMetalName.getValue())
                    .rateDate(dpRateDate.getValue())
                    .ratePerTenGrams(new BigDecimal(txtRatePerTenGrams.getText().trim()))
                    .build();
            
            // Set optional fields
            if (!txtBuyingRate.getText().trim().isEmpty()) {
                metalRate.setBuyingRate(new BigDecimal(txtBuyingRate.getText().trim()));
            }
            if (!txtSellingRate.getText().trim().isEmpty()) {
                metalRate.setSellingRate(new BigDecimal(txtSellingRate.getText().trim()));
            }
            metalRate.setRemarks(txtRemarks.getText().trim());
            
            metalRateService.saveMetalRate(metalRate);
            alert.showSuccess("Metal rate saved successfully!");
            handleClear(null);
            loadTodaysRates();
        } catch (Exception e) {
            alert.showError("Error saving metal rate: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleUpdate(ActionEvent event) {
        if (!validateForm() || selectedMetalRate == null) return;
        
        try {
            selectedMetalRate.setMetal(cmbMetalName.getValue());
            selectedMetalRate.setRateDate(dpRateDate.getValue());
            selectedMetalRate.setRatePerTenGrams(new BigDecimal(txtRatePerTenGrams.getText().trim()));
            
            // Update optional fields
            selectedMetalRate.setBuyingRate(txtBuyingRate.getText().trim().isEmpty() ? 
                null : new BigDecimal(txtBuyingRate.getText().trim()));
            selectedMetalRate.setSellingRate(txtSellingRate.getText().trim().isEmpty() ? 
                null : new BigDecimal(txtSellingRate.getText().trim()));
            selectedMetalRate.setRemarks(txtRemarks.getText().trim());
            
            metalRateService.saveMetalRate(selectedMetalRate);
            alert.showSuccess("Metal rate updated successfully!");
            handleClear(null);
            loadTodaysRates();
        } catch (Exception e) {
            alert.showError("Error updating metal rate: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedMetalRate == null) {
            alert.showError("Please select a rate to delete");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Metal Rate");
        confirmAlert.setContentText("Are you sure you want to delete this rate?");
        
        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            try {
                metalRateService.deleteMetalRate(selectedMetalRate.getId());
                alert.showSuccess("Metal rate deleted successfully!");
                handleClear(null);
                loadTodaysRates();
            } catch (Exception e) {
                alert.showError("Error deleting metal rate: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleClear(ActionEvent event) {
        dpRateDate.setValue(LocalDate.now());
        cmbMetalType.setValue(null);
        cmbMetalName.setValue(null);
        cmbMetalName.setItems(FXCollections.observableArrayList());
        txtRatePerTenGrams.clear();
        txtBuyingRate.clear();
        txtSellingRate.clear();
        txtRemarks.clear();
        
        selectedMetalRate = null;
        tblMetalRates.getSelectionModel().clearSelection();
        
        // Show save button
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        btnDelete.setVisible(false);
    }
    
    @FXML
    private void handleSearch(ActionEvent event) {
        metalRatesList.clear();
        
        if (dpSearchDate.getValue() != null && !"All".equals(cmbSearchMetalType.getValue())) {
            // Search by date and metal type
            metalRatesList.addAll(metalRateService.getRatesByMetalTypeAndDate(
                cmbSearchMetalType.getValue(), dpSearchDate.getValue()));
        } else if (dpSearchDate.getValue() != null) {
            // Search by date only
            metalRatesList.addAll(metalRateService.getRatesByDate(dpSearchDate.getValue()));
        } else {
            handleShowAll(null);
        }
    }
    
    @FXML
    private void handleShowAll(ActionEvent event) {
        metalRatesList.clear();
        // Get rates for last 30 days
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        metalRatesList.addAll(metalRateService.getRatesByDateRange(startDate, endDate));
    }
    
    private void loadTodaysRates() {
        metalRatesList.clear();
        metalRatesList.addAll(metalRateService.getRatesByDate(LocalDate.now()));
    }
    
    private void populateForm(MetalRate metalRate) {
        dpRateDate.setValue(metalRate.getRateDate());
        cmbMetalType.setValue(metalRate.getMetal().getMetalType());
        
        // Load metals for the selected type
        List<Metal> metals = metalService.getAllActiveMetals().stream()
                .filter(m -> m.getMetalType().equals(metalRate.getMetal().getMetalType()))
                .collect(Collectors.toList());
        cmbMetalName.setItems(FXCollections.observableArrayList(metals));
        cmbMetalName.setValue(metalRate.getMetal());
        
        txtRatePerTenGrams.setText(metalRate.getRatePerTenGrams().toString());
        txtBuyingRate.setText(metalRate.getBuyingRate() != null ? metalRate.getBuyingRate().toString() : "");
        txtSellingRate.setText(metalRate.getSellingRate() != null ? metalRate.getSellingRate().toString() : "");
        txtRemarks.setText(metalRate.getRemarks() != null ? metalRate.getRemarks() : "");
        
        // Show update/delete buttons
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
        btnDelete.setVisible(true);
    }
    
    private boolean validateForm() {
        if (dpRateDate.getValue() == null) {
            alert.showError("Please select a date");
            return false;
        }
        if (cmbMetalType.getValue() == null) {
            alert.showError("Please select metal type");
            return false;
        }
        if (cmbMetalName.getValue() == null) {
            alert.showError("Please select metal name");
            return false;
        }
        if (txtRatePerTenGrams.getText().trim().isEmpty()) {
            alert.showError("Please enter rate per 10 grams");
            return false;
        }
        try {
            new BigDecimal(txtRatePerTenGrams.getText().trim());
        } catch (NumberFormatException e) {
            alert.showError("Please enter valid rate");
            return false;
        }
        return true;
    }
    
    private void addNumericValidation(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                textField.setText(oldValue);
            }
        });
    }
}