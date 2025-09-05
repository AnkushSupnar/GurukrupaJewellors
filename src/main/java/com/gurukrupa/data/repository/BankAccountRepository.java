package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    
    List<BankAccount> findByIsActiveTrueOrderByCreatedDateDesc();
    
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    
    List<BankAccount> findByBankNameContainingIgnoreCase(String bankName);
    
    List<BankAccount> findByAccountHolderNameContainingIgnoreCase(String accountHolderName);
    
    List<BankAccount> findByAccountType(BankAccount.AccountType accountType);
    
    @Query("SELECT ba FROM BankAccount ba WHERE ba.isActive = true AND " +
           "(LOWER(ba.bankName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(ba.accountHolderName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "ba.accountNumber LIKE CONCAT('%', :keyword, '%'))")
    List<BankAccount> searchActiveAccounts(@Param("keyword") String keyword);
    
    @Query("SELECT COUNT(ba) FROM BankAccount ba WHERE ba.isActive = true")
    Long countActiveAccounts();
    
    @Query("SELECT SUM(CASE WHEN ba.balanceType = 'CREDIT' THEN ba.currentBalance " +
           "ELSE -ba.currentBalance END) FROM BankAccount ba WHERE ba.isActive = true")
    java.math.BigDecimal getTotalBankBalance();
}