package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.PurchaseExchangeTransaction;
import com.gurukrupa.data.entities.PurchaseInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseExchangeTransactionRepository extends JpaRepository<PurchaseExchangeTransaction, Long> {
    
    List<PurchaseExchangeTransaction> findByPurchaseInvoiceOrderByIdAsc(PurchaseInvoice purchaseInvoice);
    
    List<PurchaseExchangeTransaction> findByPurchaseInvoiceId(Long purchaseInvoiceId);
    
    @Query("SELECT pet FROM PurchaseExchangeTransaction pet WHERE pet.purchaseInvoice.id = :invoiceId")
    List<PurchaseExchangeTransaction> findAllByInvoiceId(@Param("invoiceId") Long invoiceId);
    
    @Query("SELECT COUNT(pet) FROM PurchaseExchangeTransaction pet WHERE pet.purchaseInvoice.id = :invoiceId")
    Long countByInvoiceId(@Param("invoiceId") Long invoiceId);
    
    void deleteByPurchaseInvoiceId(Long purchaseInvoiceId);
}