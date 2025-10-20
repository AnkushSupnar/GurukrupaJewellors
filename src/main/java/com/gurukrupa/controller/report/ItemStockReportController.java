package com.gurukrupa.controller.report;

import com.gurukrupa.data.entities.Category;
import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.service.CategoryService;
import com.gurukrupa.data.service.JewelryItemService;
import com.gurukrupa.data.service.MetalService;
import com.gurukrupa.service.ItemStockReportPdfService;
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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class ItemStockReportController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ItemStockReportController.class);

    @Autowired
    @Lazy
    private StageManager stageManager;

    @Autowired
    private AlertNotification alert;

    @Autowired
    private JewelryItemService jewelryItemService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MetalService metalService;

    @Autowired
    private ItemStockReportPdfService itemStockReportPdfService;

    // Header
    @FXML private Button btnBack;

    // Filters
    @FXML private ComboBox<String> cmbMetalType;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private ComboBox<String> cmbStockStatus;
    @FXML private Button btnShow;

    // Statistics
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalQuantity;
    @FXML private Label lblTotalWeight;
    @FXML private Label lblLowStock;

    // Table
    @FXML private TableView<JewelryItem> tableStock;
    @FXML private TableColumn<JewelryItem, Integer> colSno;
    @FXML private TableColumn<JewelryItem, String> colItemCode;
    @FXML private TableColumn<JewelryItem, String> colItemName;
    @FXML private TableColumn<JewelryItem, String> colCategory;
    @FXML private TableColumn<JewelryItem, String> colMetalType;
    @FXML private TableColumn<JewelryItem, String> colPurity;
    @FXML private TableColumn<JewelryItem, String> colGrossWeight;
    @FXML private TableColumn<JewelryItem, String> colNetWeight;
    @FXML private TableColumn<JewelryItem, Integer> colQuantity;
    @FXML private TableColumn<JewelryItem, String> colTotalWeight;
    @FXML private TableColumn<JewelryItem, String> colStatus;

    @FXML private Button btnExport;

    // Data
    private ObservableList<JewelryItem> allItems = FXCollections.observableArrayList();
    private ObservableList<JewelryItem> filteredItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing ItemStockReportController");

        setupTableColumns();
        setupFilters();

        // Initially show empty table and zero statistics
        filteredItems.clear();
        lblTotalItems.setText("0");
        lblTotalQuantity.setText("0");
        lblTotalWeight.setText("0.000g");
        lblLowStock.setText("0");
    }

    private void setupTableColumns() {
        // Serial Number
        colSno.setCellValueFactory(cellData -> {
            int index = tableStock.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleIntegerProperty(index).asObject();
        });

        colItemCode.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItemCode()));

        colItemName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItemName()));

        colCategory.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCategory()));

        colMetalType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMetalType()));

        colPurity.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPurity() != null ?
                        cellData.getValue().getPurity().toString() : ""));

        colGrossWeight.setCellValueFactory(cellData ->
                new SimpleStringProperty(WeightFormatter.format(cellData.getValue().getGrossWeight())));

        colNetWeight.setCellValueFactory(cellData ->
                new SimpleStringProperty(WeightFormatter.format(cellData.getValue().getNetWeight())));

        colQuantity.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());

        colTotalWeight.setCellValueFactory(cellData -> {
            BigDecimal totalWeight = cellData.getValue().getNetWeight()
                    .multiply(BigDecimal.valueOf(cellData.getValue().getQuantity()));
            return new SimpleStringProperty(WeightFormatter.format(totalWeight));
        });

        colStatus.setCellValueFactory(cellData -> {
            int qty = cellData.getValue().getQuantity();
            String status;
            if (qty == 0) {
                status = "Out of Stock";
            } else if (qty <= 5) {
                status = "Low Stock";
            } else {
                status = "In Stock";
            }
            return new SimpleStringProperty(status);
        });

        // Apply status-based styling
        colStatus.setCellFactory(column -> new TableCell<JewelryItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Out of Stock":
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                            break;
                        case "Low Stock":
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                            break;
                        case "In Stock":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            break;
                    }
                }
            }
        });

        tableStock.setItems(filteredItems);
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

        // Load Categories from category table
        try {
            List<String> categories = categoryService.getAllActiveCategories().stream()
                    .map(Category::getCategoryName)
                    .collect(Collectors.toList());
            categories.add(0, "All");
            cmbCategory.setItems(FXCollections.observableArrayList(categories));
            cmbCategory.getSelectionModel().selectFirst(); // Select "All" by default
            logger.info("Loaded {} categories from database", categories.size() - 1);
        } catch (Exception e) {
            logger.error("Error loading categories", e);
            cmbCategory.setItems(FXCollections.observableArrayList("All"));
            cmbCategory.getSelectionModel().selectFirst();
        }

        // Stock Status filter
        cmbStockStatus.setItems(FXCollections.observableArrayList(
                "All", "In Stock", "Low Stock", "Out of Stock"
        ));
        cmbStockStatus.getSelectionModel().selectFirst(); // Select "All" by default
    }

    @FXML
    private void handleShow() {
        try {
            logger.info("Loading item stock data with filters");
            List<JewelryItem> items = jewelryItemService.getAllJewelryItems();
            allItems.setAll(items);
            applyFilters();
            calculateStatistics();
            logger.info("Loaded {} items, filtered to {} items", items.size(), filteredItems.size());
        } catch (Exception e) {
            logger.error("Error loading item stock data", e);
            alert.showError("Error loading data: " + e.getMessage());
        }
    }

    private void applyFilters() {
        List<JewelryItem> filtered = allItems.stream()
                .filter(item -> filterByMetalType(item))
                .filter(item -> filterByCategory(item))
                .filter(item -> filterByStockStatus(item))
                .collect(Collectors.toList());

        filteredItems.setAll(filtered);
    }

    private boolean filterByCategory(JewelryItem item) {
        String selected = cmbCategory.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            return true;
        }
        return item.getCategory() != null && item.getCategory().equals(selected);
    }

    private boolean filterByMetalType(JewelryItem item) {
        String selected = cmbMetalType.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            return true;
        }
        // Metal type dropdown contains metal names (e.g., "Gold 22K", "Silver 925")
        // We need to match against the metalType field in JewelryItem
        return item.getMetalType() != null && item.getMetalType().equalsIgnoreCase(selected);
    }

    private boolean filterByStockStatus(JewelryItem item) {
        String selected = cmbStockStatus.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            return true;
        }

        int qty = item.getQuantity();
        switch (selected) {
            case "In Stock":
                return qty > 5;
            case "Low Stock":
                return qty > 0 && qty <= 5;
            case "Out of Stock":
                return qty == 0;
            default:
                return true;
        }
    }

    private void calculateStatistics() {
        int totalItems = filteredItems.size();
        int totalQuantity = filteredItems.stream()
                .mapToInt(JewelryItem::getQuantity)
                .sum();

        BigDecimal totalWeight = filteredItems.stream()
                .map(item -> item.getNetWeight().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long lowStockCount = filteredItems.stream()
                .filter(item -> item.getQuantity() > 0 && item.getQuantity() <= 5)
                .count();

        lblTotalItems.setText(String.valueOf(totalItems));
        lblTotalQuantity.setText(String.valueOf(totalQuantity));
        lblTotalWeight.setText(WeightFormatter.format(totalWeight));
        lblLowStock.setText(String.valueOf(lowStockCount));
    }

    @FXML
    private void handleExport() {
        try {
            if (filteredItems.isEmpty()) {
                alert.showError("No data to export. Please click SHOW button first.");
                return;
            }

            // Get current filter values
            String metalType = cmbMetalType.getSelectionModel().getSelectedItem();
            String category = cmbCategory.getSelectionModel().getSelectedItem();
            String stockStatus = cmbStockStatus.getSelectionModel().getSelectedItem();

            // Generate filename with current date
            String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "ItemStockReport_" + dateStr + ".pdf";
            String filePath = "D:/software/" + fileName;

            logger.info("Generating Item Stock Report PDF: {}", filePath);

            // Generate PDF
            itemStockReportPdfService.generateItemStockReportPdf(
                    filteredItems,
                    metalType,
                    category,
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
