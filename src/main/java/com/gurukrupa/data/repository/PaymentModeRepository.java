package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.PaymentMode;
import com.gurukrupa.data.entities.PaymentMode.PaymentType;
import com.gurukrupa.data.entities.PaymentMode.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentModeRepository extends JpaRepository<PaymentMode, Long> {
    
    List<PaymentMode> findByBillId(Long billId);
    
    List<PaymentMode> findByPaymentType(PaymentType paymentType);
    
    List<PaymentMode> findByStatus(PaymentStatus status);
    
    List<PaymentMode> findByBillIdAndPaymentType(Long billId, PaymentType paymentType);
    
    @Query("SELECT p FROM PaymentMode p WHERE p.bill.id = :billId ORDER BY p.createdDate")
    List<PaymentMode> findByBillIdOrderByCreatedDate(@Param("billId") Long billId);
    
    @Query("SELECT p FROM PaymentMode p WHERE p.referenceNumber = :referenceNumber")
    List<PaymentMode> findByReferenceNumber(@Param("referenceNumber") String referenceNumber);
    
    @Query("SELECT p FROM PaymentMode p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    List<PaymentMode> findByPaymentDateBetween(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM PaymentMode p WHERE p.bankAccount.id = :bankAccountId")
    List<PaymentMode> findByBankAccountId(@Param("bankAccountId") Long bankAccountId);
    
    @Query("SELECT SUM(p.amount) FROM PaymentMode p WHERE p.bill.id = :billId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaidAmountByBillId(@Param("billId") Long billId);
    
    @Query("SELECT p FROM PaymentMode p WHERE p.paymentType = :paymentType AND p.paymentDate BETWEEN :startDate AND :endDate")
    List<PaymentMode> findByPaymentTypeAndDateRange(@Param("paymentType") PaymentType paymentType,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
}