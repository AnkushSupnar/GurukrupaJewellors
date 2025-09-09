package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.BankTransaction;
import com.gurukrupa.data.entities.BankTransaction.TransactionType;
import com.gurukrupa.data.entities.BankTransaction.TransactionSource;
import com.gurukrupa.data.entities.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    
    // Find by bank account
    List<BankTransaction> findByBankAccountIdOrderByTransactionDateDesc(Long bankAccountId);
    
    List<BankTransaction> findByBankAccountOrderByTransactionDateDesc(BankAccount bankAccount);
    
    // Find by transaction type
    List<BankTransaction> findByBankAccountIdAndTransactionType(Long bankAccountId, TransactionType type);
    
    // Find by source
    List<BankTransaction> findBySourceOrderByTransactionDateDesc(TransactionSource source);
    
    List<BankTransaction> findByBankAccountIdAndSource(Long bankAccountId, TransactionSource source);
    
    // Find by reference
    Optional<BankTransaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    
    List<BankTransaction> findByReferenceNumber(String referenceNumber);
    
    // Find by date range
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId " +
           "AND bt.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY bt.transactionDate DESC")
    List<BankTransaction> findByBankAccountAndDateRange(@Param("bankAccountId") Long bankAccountId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);
    
    // Find unreconciled transactions
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId " +
           "AND bt.isReconciled = false ORDER BY bt.transactionDate")
    List<BankTransaction> findUnreconciledByBankAccount(@Param("bankAccountId") Long bankAccountId);
    
    // Calculate totals
    @Query("SELECT SUM(CASE WHEN bt.transactionType = 'CREDIT' THEN bt.amount ELSE 0 END) " +
           "FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId")
    BigDecimal getTotalCreditsForAccount(@Param("bankAccountId") Long bankAccountId);
    
    @Query("SELECT SUM(CASE WHEN bt.transactionType = 'DEBIT' THEN bt.amount ELSE 0 END) " +
           "FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId")
    BigDecimal getTotalDebitsForAccount(@Param("bankAccountId") Long bankAccountId);
    
    // Get balance at a specific date
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId " +
           "AND bt.transactionDate <= :date " +
           "ORDER BY bt.transactionDate DESC, bt.id DESC")
    List<BankTransaction> findTransactionsBeforeDate(@Param("bankAccountId") Long bankAccountId,
                                                    @Param("date") LocalDateTime date);
    
    // Get latest transaction for an account
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId " +
           "ORDER BY bt.transactionDate DESC, bt.id DESC LIMIT 1")
    Optional<BankTransaction> findLatestByBankAccount(@Param("bankAccountId") Long bankAccountId);
    
    // Search transactions
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId " +
           "AND (LOWER(bt.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(bt.party) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(bt.referenceNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY bt.transactionDate DESC")
    List<BankTransaction> searchByKeyword(@Param("bankAccountId") Long bankAccountId,
                                         @Param("keyword") String keyword);
    
    // Monthly summary
    @Query("SELECT MONTH(bt.transactionDate) as month, YEAR(bt.transactionDate) as year, " +
           "SUM(CASE WHEN bt.transactionType = 'CREDIT' THEN bt.amount ELSE 0 END) as totalCredits, " +
           "SUM(CASE WHEN bt.transactionType = 'DEBIT' THEN bt.amount ELSE 0 END) as totalDebits " +
           "FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId " +
           "AND bt.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY MONTH(bt.transactionDate), YEAR(bt.transactionDate) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlySummary(@Param("bankAccountId") Long bankAccountId,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
}