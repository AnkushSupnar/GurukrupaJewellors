package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.UPIPaymentMethod;
import com.gurukrupa.data.repository.UPIPaymentMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UPIPaymentMethodService {
    
    @Autowired
    private UPIPaymentMethodRepository upiPaymentMethodRepository;
    
    public UPIPaymentMethod saveUPIPaymentMethod(UPIPaymentMethod upiPaymentMethod) {
        return upiPaymentMethodRepository.save(upiPaymentMethod);
    }
    
    public List<UPIPaymentMethod> getAllUPIPaymentMethods() {
        return upiPaymentMethodRepository.findAll();
    }
    
    public List<UPIPaymentMethod> getActiveUPIPaymentMethods() {
        return upiPaymentMethodRepository.findByActiveTrue();
    }
    
    public Optional<UPIPaymentMethod> findById(Long id) {
        return upiPaymentMethodRepository.findById(id);
    }
    
    public void deleteUPIPaymentMethod(Long id) {
        upiPaymentMethodRepository.deleteById(id);
    }
    
    public UPIPaymentMethod updateUPIPaymentMethod(UPIPaymentMethod upiPaymentMethod) {
        return upiPaymentMethodRepository.save(upiPaymentMethod);
    }
    
    public List<UPIPaymentMethod> findByBankAccountId(Long bankAccountId) {
        return upiPaymentMethodRepository.findByBankAccountId(bankAccountId);
    }
    
    public boolean existsByAppName(String appName) {
        return upiPaymentMethodRepository.existsByAppNameIgnoreCase(appName);
    }
}