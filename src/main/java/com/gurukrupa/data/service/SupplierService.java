package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Supplier;
import com.gurukrupa.data.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SupplierService {
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }
    
    public Supplier createSupplier(String supplierName, String companyName, String gstNumber,
                                 String mobile, String alternateMobile, String email,
                                 String address, String city, String state, String pincode,
                                 String contactPerson, String supplierType, BigDecimal creditLimit,
                                 String notes) {
        
        // Check if mobile already exists
        if (supplierRepository.findByMobile(mobile).isPresent()) {
            throw new IllegalArgumentException("Supplier with this mobile number already exists");
        }
        
        // Check if GST number already exists
        if (gstNumber != null && !gstNumber.isEmpty() && 
            supplierRepository.findByGstNumber(gstNumber).isPresent()) {
            throw new IllegalArgumentException("Supplier with this GST number already exists");
        }
        
        Supplier supplier = Supplier.builder()
                .supplierName(supplierName)
                .companyName(companyName)
                .gstNumber(gstNumber)
                .mobile(mobile)
                .alternateMobile(alternateMobile)
                .email(email)
                .address(address)
                .city(city)
                .state(state)
                .pincode(pincode)
                .contactPerson(contactPerson)
                .supplierType(supplierType)
                .creditLimit(creditLimit != null ? creditLimit : BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO)
                .notes(notes)
                .isActive(true)
                .build();
        
        return supplierRepository.save(supplier);
    }
    
    public Optional<Supplier> findById(Long id) {
        return supplierRepository.findById(id);
    }
    
    public Optional<Supplier> findByMobile(String mobile) {
        return supplierRepository.findByMobile(mobile);
    }
    
    public Optional<Supplier> findByGstNumber(String gstNumber) {
        return supplierRepository.findByGstNumber(gstNumber);
    }
    
    public List<Supplier> getAllActiveSuppliers() {
        return supplierRepository.findByIsActiveTrueOrderBySupplierNameAsc();
    }
    
    public List<Supplier> searchSuppliers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllActiveSuppliers();
        }
        return supplierRepository.searchActiveSuppliers(searchTerm.trim());
    }
    
    public List<Supplier> getSuppliersByType(String supplierType) {
        return supplierRepository.findBySupplierTypeAndIsActiveTrue(supplierType);
    }
    
    public List<Supplier> getSuppliersWithOutstandingBalance() {
        return supplierRepository.findSuppliersWithOutstandingBalance();
    }
    
    public Supplier updateSupplier(Supplier supplier) {
        if (supplier.getId() == null) {
            throw new IllegalArgumentException("Supplier ID cannot be null for update");
        }
        
        Optional<Supplier> existingSupplier = supplierRepository.findById(supplier.getId());
        if (existingSupplier.isEmpty()) {
            throw new IllegalArgumentException("Supplier not found with ID: " + supplier.getId());
        }
        
        // Check if mobile is being changed and if new mobile already exists
        Supplier existing = existingSupplier.get();
        if (!existing.getMobile().equals(supplier.getMobile())) {
            Optional<Supplier> mobileCheck = supplierRepository.findByMobile(supplier.getMobile());
            if (mobileCheck.isPresent()) {
                throw new IllegalArgumentException("Another supplier with this mobile number already exists");
            }
        }
        
        // Check if GST number is being changed and if new GST already exists
        if (supplier.getGstNumber() != null && !supplier.getGstNumber().isEmpty() && 
            (existing.getGstNumber() == null || !existing.getGstNumber().equals(supplier.getGstNumber()))) {
            Optional<Supplier> gstCheck = supplierRepository.findByGstNumber(supplier.getGstNumber());
            if (gstCheck.isPresent()) {
                throw new IllegalArgumentException("Another supplier with this GST number already exists");
            }
        }
        
        return supplierRepository.save(supplier);
    }
    
    public void deactivateSupplier(Long id) {
        Optional<Supplier> supplier = supplierRepository.findById(id);
        if (supplier.isPresent()) {
            Supplier s = supplier.get();
            s.setIsActive(false);
            supplierRepository.save(s);
        } else {
            throw new IllegalArgumentException("Supplier not found with ID: " + id);
        }
    }
    
    public void activateSupplier(Long id) {
        Optional<Supplier> supplier = supplierRepository.findById(id);
        if (supplier.isPresent()) {
            Supplier s = supplier.get();
            s.setIsActive(true);
            supplierRepository.save(s);
        } else {
            throw new IllegalArgumentException("Supplier not found with ID: " + id);
        }
    }
    
    public void deleteSupplier(Long id) {
        if (supplierRepository.existsById(id)) {
            supplierRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Supplier not found with ID: " + id);
        }
    }
    
    public Long getTotalSuppliersCount() {
        return supplierRepository.countActiveSuppliers();
    }
    
    public Double getTotalOutstandingAmount() {
        return supplierRepository.getTotalOutstandingAmount();
    }
    
    public List<String> getAllSupplierNames() {
        return supplierRepository.findAllSupplierNames();
    }
    
    public List<String> getAllSupplierNamesWithMobile() {
        return supplierRepository.findAllSupplierNamesWithMobile();
    }
    
    public Supplier updateBalance(Long id, BigDecimal amount) {
        Optional<Supplier> supplier = supplierRepository.findById(id);
        if (supplier.isPresent()) {
            Supplier s = supplier.get();
            s.setCurrentBalance(s.getCurrentBalance().add(amount));
            return supplierRepository.save(s);
        } else {
            throw new IllegalArgumentException("Supplier not found with ID: " + id);
        }
    }
    
    public Supplier setBalance(Long id, BigDecimal balance) {
        Optional<Supplier> supplier = supplierRepository.findById(id);
        if (supplier.isPresent()) {
            Supplier s = supplier.get();
            s.setCurrentBalance(balance);
            return supplierRepository.save(s);
        } else {
            throw new IllegalArgumentException("Supplier not found with ID: " + id);
        }
    }
}