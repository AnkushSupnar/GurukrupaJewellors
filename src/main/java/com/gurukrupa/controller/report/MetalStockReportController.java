package com.gurukrupa.controller.report;

import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.entities.PurchaseMetalStock;
import com.gurukrupa.data.service.MetalService;
import com.gurukrupa.data.service.PurchaseMetalStockService;
import com.gurukrupa.service.MetalStockReportPdfService;
import com.gurukrupa.utility.WeightFormatter;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.StageManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class MetalStockReportController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MetalStockReportController.class);

    @Autowired
    @Lazy
    private StageManager stageManager;

    @Autowired
    private AlertNotification alert;

    @Autowired
    private PurchaseMetalStockService purchaseMetalStockService;

    @Autowired
    private MetalService metalService;

    @Autowired
    private MetalStockReportPdfService metalStockReportPdfService;

    // Header
    @FXML private Button btnBack;

    // Filters
    @FXML private ComboBox<String> cmbMetalType;
    @FXML private ComboBox<String> cmbStockStatus;
    @FXML private Button btnShow;

    // Statistics
    @FXML private Label lblMetalTypes;
    @FXML private Label lblTotalGross;
    @FXML private Label lblAvailableWeight;
    @FXML private Label lblUsedWeight;

    // Table
    @FXML private TableView<PurchaseMetalStock> tableStock;
    @FXML private TableColumn<PurchaseMetalStock, Integer> colSno;
    @FXML private TableColumn<PurchaseMetalStock, String> colMetalName;
    @FXML private TableColumn<PurchaseMetalStock, String> colMetalType;
    @FXML private TableColumn<PurchaseMetalStock, String> colPurity;
    @FXML private TableColumn<PurchaseMetalStock, String> colTotalGross;
    @FXML private TableColumn<PurchaseMetalStock, String> colTotalNet;
    @FXML private TableColumn<PurchaseMetalStock, String> colAvailable;
    @FXML private TableColumn<PurchaseMetalStock, String> colUsed;
    @FXML private TableColumn<PurchaseMetalStock, String> colUtilization;

    @FXML private Button btnExport;

    // Data
    private ObservableList<PurchaseMetalStock> allStock = FXCollections.observableArrayList();
    private ObservableList<PurchaseMetalStock> filteredStock = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MetalStockReportController");

        setupTableColumns();
        setupFilters();

        // Initially show empty table and zero statistics
        filteredStock.clear();
        lblMetalTypes.setText("0");
        lblTotalGross.setText("0.000g");
        lblAvailableWeight.setText("0.000g");
        lblUsedWeight.setText("0.000g");
    }

    private void setupTableColumns() {
        // Serial Number
        colSno.setCellValueFactory(cellData -> {
            int index = tableStock.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleIntegerProperty(index).asObject();
        });

        colMetalName.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getMetal() != null ?
                    cellData.getValue().getMetal().getMetalName() :
                    cellData.getValue().getMetalType() + " " + cellData.getValue().getPurity();
            return new SimpleStringProperty(name);
        });

        colMetalType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMetalType()));

        colPurity.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPurity() != null ?
                        cellData.getValue().getPurity().toString() : ""));

        colTotalGross.setCellValueFactory(cellData ->
                new SimpleStringProperty(WeightFormatter.format(cellData.getValue().getTotalGrossWeight())));

        colTotalNet.setCellValueFactory(cellData ->
                new SimpleStringProperty(WeightFormatter.format(cellData.getValue().getTotalNetWeight())));

        colAvailable.setCellValueFactory(cellData ->
                new SimpleStringProperty(WeightFormatter.format(cellData.getValue().getAvailableWeight())));

        colUsed.setCellValueFactory(cellData ->
                new SimpleStringProperty(WeightFormatter.format(cellData.getValue().getUsedWeight())));

        colUtilization.setCellValueFactory(cellData -> {
            BigDecimal totalNet = cellData.getValue().getTotalNetWeight();
            BigDecimal used = cellData.getValue().getUsedWeight();

            if (totalNet.compareTo(BigDecimal.ZERO) == 0) {
                return new SimpleStringProperty("0.00%");
            }

            BigDecimal utilization = used
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalNet, 2, RoundingMode.HALF_UP);

            return new SimpleStringProperty(String.format("%.2f%%", utilization));
        });

        tableStock.setItems(filteredStock);
    }

    private void setupFilters() {
        // Load Metal Names from metals table (e.g., "Gold 22K", "Gold 18K", "Silver 925")
        try {
            List<String> metalNames = metalService.getAllMetalNames();
            metalNames.add(0, "All");
            cmbMetalType.setItems(FXCollections.observableArrayList(metalNames));
            cmbMetalType.getSelectionModel().selectFirst(); // Select "All" by default
            logger.info("Loaded {} metals from database", metalNames.size() - 1);
        } catch (Exception e) {
            logger.error("Error loading metals", e);
            cmbMetalType.setItems(FXCollections.observableArrayList("All"));
            cmbMetalType.getSelectionModel().selectFirst();
        }

        // Stock Status filter
        cmbStockStatus.setItems(FXCollections.observableArrayList(
                "All", "Available Stock", "Fully Utilized"
        ));
        cmbStockStatus.getSelectionModel().selectFirst(); // Select "All" by default
    }

    @FXML
    private void handleShow() {
        try {
            logger.info("Loading metal stock data with filters");
            List<PurchaseMetalStock> stock = purchaseMetalStockService.getAllStock();
            allStock.setAll(stock);
            applyFilters();
            calculateStatistics();
            logger.info("Loaded {} metal stock entries, filtered to {}", stock.size(), filteredStock.size());
        } catch (Exception e) {
            logger.error("Error loading metal stock data", e);
            alert.showError("Error loading data: " + e.getMessage());
        }
    }

    private void applyFilters() {
        List<PurchaseMetalStock> filtered = allStock.stream()
                .filter(stock -> filterByMetalType(stock))
                .filter(stock -> filterByStockStatus(stock))
                .collect(Collectors.toList());

        filteredStock.setAll(filtered);
    }

    private boolean filterByMetalType(PurchaseMetalStock stock) {
        String selected = cmbMetalType.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            return true;
        }
        // Check if metal name matches (e.g., "Gold 22K")
        if (stock.getMetal() != null && stock.getMetal().getMetalName() != null) {
            return stock.getMetal().getMetalName().equalsIgnoreCase(selected);
        }
        // Fallback to metal type comparison
        return stock.getMetalType() != null && stock.getMetalType().equalsIgnoreCase(selected);
    }

    private boolean filterByStockStatus(PurchaseMetalStock stock) {
        String selected = cmbStockStatus.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            return true;
        }

        BigDecimal available = stock.getAvailableWeight();
        switch (selected) {
            case "Available Stock":
                return available.compareTo(BigDecimal.ZERO) > 0;
            case "Fully Utilized":
                return available.compareTo(BigDecimal.ZERO) == 0;
            default:
                return true;
        }
    }

    private void calculateStatistics() {
        int metalTypes = filteredStock.size();

        BigDecimal totalGross = filteredStock.stream()
                .map(PurchaseMetalStock::getTotalGrossWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableWeight = filteredStock.stream()
                .map(PurchaseMetalStock::getAvailableWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal usedWeight = filteredStock.stream()
                .map(PurchaseMetalStock::getUsedWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblMetalTypes.setText(String.valueOf(metalTypes));
        lblTotalGross.setText(WeightFormatter.format(totalGross));
        lblAvailableWeight.setText(WeightFormatter.format(availableWeight));
        lblUsedWeight.setText(WeightFormatter.format(usedWeight));
    }

    @FXML
    private void handleExport() {
        try {
            if (filteredStock.isEmpty()) {
                alert.showError("No data to export. Please click SHOW button first.");
                return;
            }

            // Get current filter values
            String metalType = cmbMetalType.getSelectionModel().getSelectedItem();
            String stockStatus = cmbStockStatus.getSelectionModel().getSelectedItem();

            // Generate filename with current date
            String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "MetalStockReport_" + dateStr + ".pdf";
            String filePath = "D:/software/" + fileName;

            logger.info("Generating Metal Stock Report PDF: {}", filePath);

            // Generate PDF
            metalStockReportPdfService.generateMetalStockReportPdf(
                    filteredStock,
                    metalType,
                    stockStatus,
                    filePath
            );

            alert.showSuccess("Report exported successfully to: " + filePath);

        } catch (Exception e) {
            logger.error("Error exporting report", e);
            alert.showError("Error exporting report: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            logger.info("Navigating back to Report Menu");
            BorderPane dashboard = (BorderPane) btnBack.getScene().getRoot();
            Parent reportMenu = stageManager.getSpringFXMLLoader().load("/fxml/report/ReportMenu.fxml");
            dashboard.setCenter(reportMenu);
            logger.info("Report Menu loaded successfully");
        } catch (Exception e) {
            logger.error("Error navigating back to Report Menu: {}", e.getMessage());
            e.printStackTrace();
            alert.showError("Error navigating back: " + e.getMessage());
        }
    }
}
