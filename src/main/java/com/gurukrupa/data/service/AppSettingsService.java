package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.AppSettings;
import com.gurukrupa.data.repository.AppSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AppSettingsService {
    
    @Autowired
    private AppSettingsRepository appSettingsRepository;
    
    public AppSettings saveSetting(String settingName, String settingValue) {
        return saveSetting(settingName, settingValue, null);
    }
    
    public AppSettings saveSetting(String settingName, String settingValue, String description) {
        Optional<AppSettings> existingSetting = appSettingsRepository.findBySettingName(settingName);
        
        if (existingSetting.isPresent()) {
            AppSettings setting = existingSetting.get();
            setting.setSettingValue(settingValue);
            if (description != null) {
                setting.setDescription(description);
            }
            return appSettingsRepository.save(setting);
        } else {
            AppSettings newSetting = AppSettings.builder()
                    .settingName(settingName)
                    .settingValue(settingValue)
                    .description(description)
                    .build();
            return appSettingsRepository.save(newSetting);
        }
    }
    
    public Optional<String> getSettingValue(String settingName) {
        return appSettingsRepository.findSettingValueByName(settingName);
    }
    
    public String getSettingValue(String settingName, String defaultValue) {
        return getSettingValue(settingName).orElse(defaultValue);
    }
    
    public Optional<AppSettings> getSetting(String settingName) {
        return appSettingsRepository.findBySettingName(settingName);
    }
    
    public List<AppSettings> getAllSettings() {
        return appSettingsRepository.findAll();
    }
    
    public List<AppSettings> getSettingsByPattern(String pattern) {
        return appSettingsRepository.findBySettingNameContaining(pattern);
    }
    
    public boolean settingExists(String settingName) {
        return appSettingsRepository.existsBySettingName(settingName);
    }
    
    public void deleteSetting(String settingName) {
        appSettingsRepository.findBySettingName(settingName).ifPresent(setting -> 
            appSettingsRepository.delete(setting));
    }
    
    // Specific methods for bill number management
    public String getBillNumberPrefix() {
        return getSettingValue(AppSettings.BILL_NUMBER_PREFIX, "INV");
    }
    
    public void setBillNumberPrefix(String prefix) {
        saveSetting(AppSettings.BILL_NUMBER_PREFIX, prefix, "Prefix for bill numbers");
    }
    
    public Long getLastBillNumber() {
        String lastBillStr = getSettingValue(AppSettings.LAST_BILL_NUMBER, "0");
        try {
            return Long.parseLong(lastBillStr);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    public Long getNextBillNumber() {
        Long lastBillNumber = getLastBillNumber();
        Long nextBillNumber = lastBillNumber + 1;
        saveSetting(AppSettings.LAST_BILL_NUMBER, nextBillNumber.toString(), "Last used bill number sequence");
        return nextBillNumber;
    }
    
    public String generateBillNumber() {
        String prefix = getBillNumberPrefix();
        Long nextNumber = getNextBillNumber();
        return prefix + String.format("%06d", nextNumber); // Format as 6-digit number with leading zeros
    }
    
    // GST Rate Management Methods
    public Double getDefaultGstRate() {
        String gstRateStr = getSettingValue(AppSettings.DEFAULT_GST_RATE, "3.00");
        try {
            return Double.parseDouble(gstRateStr);
        } catch (NumberFormatException e) {
            return 3.00;
        }
    }
    
    public void setDefaultGstRate(Double gstRate) {
        saveSetting(AppSettings.DEFAULT_GST_RATE, gstRate.toString(), "Default GST rate percentage for billing");
        // When GST rate is set, automatically set CGST and SGST as half each
        Double halfRate = gstRate / 2;
        setDefaultCgstRate(halfRate);
        setDefaultSgstRate(halfRate);
    }
    
    public Double getDefaultCgstRate() {
        String cgstRateStr = getSettingValue(AppSettings.DEFAULT_CGST_RATE, "1.50");
        try {
            return Double.parseDouble(cgstRateStr);
        } catch (NumberFormatException e) {
            return 1.50;
        }
    }
    
    public void setDefaultCgstRate(Double cgstRate) {
        saveSetting(AppSettings.DEFAULT_CGST_RATE, cgstRate.toString(), "Default CGST rate percentage");
    }
    
    public Double getDefaultSgstRate() {
        String sgstRateStr = getSettingValue(AppSettings.DEFAULT_SGST_RATE, "1.50");
        try {
            return Double.parseDouble(sgstRateStr);
        } catch (NumberFormatException e) {
            return 1.50;
        }
    }
    
    public void setDefaultSgstRate(Double sgstRate) {
        saveSetting(AppSettings.DEFAULT_SGST_RATE, sgstRate.toString(), "Default SGST rate percentage");
    }
    
    // Initialize default settings
    public void initializeDefaultSettings() {
        if (!settingExists(AppSettings.BILL_NUMBER_PREFIX)) {
            setBillNumberPrefix("INV");
        }
        if (!settingExists(AppSettings.LAST_BILL_NUMBER)) {
            saveSetting(AppSettings.LAST_BILL_NUMBER, "0", "Last used bill number sequence");
        }
        if (!settingExists(AppSettings.DEFAULT_GST_RATE)) {
            saveSetting(AppSettings.DEFAULT_GST_RATE, "3.00", "Default GST rate percentage");
        }
        if (!settingExists(AppSettings.DEFAULT_CGST_RATE)) {
            saveSetting(AppSettings.DEFAULT_CGST_RATE, "1.50", "Default CGST rate percentage");
        }
        if (!settingExists(AppSettings.DEFAULT_SGST_RATE)) {
            saveSetting(AppSettings.DEFAULT_SGST_RATE, "1.50", "Default SGST rate percentage");
        }
    }
}