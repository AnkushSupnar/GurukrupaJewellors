package com.gurukrupa.data.entities;


import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@Entity
@Table(name = "shopinfo")
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ShopInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="shop_name", nullable = false)
    private String shopName;

    @Column(name="address")
    private String shopAddress;

    @Column(name="contact_no")
    private String shopContactNo;

    @Column(name="license_key")
    private String shopLicenseKey;

    @Column(name="owner_name")
    private String shopOwnerName;

    @Column(name="owner_mobile_no")
    private String shopOwnerMobileNo;
    
    @Column(name="shop_mobile")
    private String shopMobile;
    
    @Column(name="shop_email")
    private String shopEmail;
    
    @Column(name="gstin_number")
    private String gstinNumber;
    
    @Column(name="terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    // Getters and Setters

}
