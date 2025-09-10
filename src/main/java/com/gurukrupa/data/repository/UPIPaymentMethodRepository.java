package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.UPIPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UPIPaymentMethodRepository extends JpaRepository<UPIPaymentMethod, Long> {
    
    List<UPIPaymentMethod> findByActiveTrue();
    
    List<UPIPaymentMethod> findByBankAccountId(Long bankAccountId);
    
    boolean existsByAppNameIgnoreCase(String appName);
}