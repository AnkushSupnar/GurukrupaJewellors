package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "app_settings")
public class AppSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String settingName;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String settingValue;
    
    @Column
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    @Column
    private LocalDateTime updatedDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
    
    // Static constants for common settings
    public static final String BILL_NUMBER_PREFIX = "BILL_NUMBER_PREFIX";
    public static final String LAST_BILL_NUMBER = "LAST_BILL_NUMBER";
    public static final String COMPANY_NAME = "COMPANY_NAME";
    public static final String COMPANY_ADDRESS = "COMPANY_ADDRESS";
    public static final String COMPANY_PHONE = "COMPANY_PHONE";
    public static final String DEFAULT_GST_RATE = "DEFAULT_GST_RATE";
    public static final String DEFAULT_CGST_RATE = "DEFAULT_CGST_RATE";
    public static final String DEFAULT_SGST_RATE = "DEFAULT_SGST_RATE";
}