package com.gurukrupa.view;

import java.util.ResourceBundle;

public enum FxmlView {
    LOGIN {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("login.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/home/login.fxml";
        }
    },
    CREATE_SHOPE {
        @Override
        String getTitle() {
            return "Registration Form";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/home/create_shope.fxml";
        }
    },

    DASHBOARD {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/home/Dashboard.fxml";
        }
    },
    BILLING {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/transaction/BillingFrame.fxml";
        }
    },
    CUSTOMER_FORM {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/master/customer.fxml";
        }
    },
    JEWELRY_ITEM_MENU {
        @Override
        String getTitle() {
            return "Master Data Management";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/master/MasterMenu.fxml";
        }
    },
    JEWELRY_ITEM_FORM {
        @Override
        String getTitle() {
            return "Add New Item";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/master/AddNewItem.fxml";
        }
    },
    SETTINGS_MENU {
        @Override
        String getTitle() {
            return "Settings";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/settings/SettingsMenu.fxml";
        }
    },
    METAL_FORM {
        @Override
        String getTitle() {
            return "Metal Management";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/master/MetalForm.fxml";
        }
    },
    APP_SETTINGS {
        @Override
        String getTitle() {
            return "Application Settings";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/settings/AppSettings.fxml";
        }
    },
    METAL_RATE_FORM {
        @Override
        String getTitle() {
            return "Metal Rate Management";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/settings/MetalRateFrame.fxml";
        }
    },
    VIEW_BILLS {
        @Override
        String getTitle() {
            return "View Bills";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/transaction/ViewBills.fxml";
        }
    },
    BANK_ACCOUNT_LIST {
        @Override
        String getTitle() {
            return "Bank Accounts";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/settings/BankAccountList.fxml";
        }
    },
    BANKACCOUNTFORM {
        @Override
        String getTitle() {
            return "Bank Account Form";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/settings/BankAccountForm.fxml";
        }
    },
    TAX_CONFIGURATION {
        @Override
        String getTitle() {
            return "Tax Configuration";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/settings/TaxConfiguration.fxml";
        }
    },
    ADD_UPI_PAYMENT {
        @Override
        String getTitle() {
            return "UPI Payment Method";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/settings/AddUPIPayment.fxml";
        }
    },
    UPI_PAYMENT_LIST {
        @Override
        String getTitle() {
            return "UPI Payment Methods";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/settings/UPIPaymentList.fxml";
        }
    },
    ADD_SUPPLIER {
        @Override
        String getTitle() {
            return "Add Supplier";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/master/AddSupplier.fxml";
        }
    },
    PURCHASE_INVOICE {
        @Override
        String getTitle() {
            return "Purchase Invoice";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/transaction/PurchaseInvoiceFrame.fxml";
        }
    }
    ;
    abstract String getTitle();
    public abstract String getFxmlFile();
    String getStringFromResourceBundle(String key){
        return ResourceBundle.getBundle("Bundle").getString(key);
    }
}
