package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.entities.BankTransaction;
import com.gurukrupa.data.entities.BankTransaction.TransactionType;
import com.gurukrupa.data.entities.BankTransaction.TransactionSource;
import com.gurukrupa.data.repository.BankTransactionRepository;
import com.gurukrupa.data.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BankTransactionService {
    
    @Autowired
    private BankTransactionRepository bankTransactionRepository;
    
    @Autowired
    private BankAccountRepository bankAccountRepository;
    
    /**
     * Record a credit transaction (money coming in)
     */
    public BankTransaction recordCredit(BankAccount bankAccount, BigDecimal amount, 
                                       TransactionSource source, String referenceType,
                                       Long referenceId, String referenceNumber,
                                       String transactionReference, String party,
                                       String description) {
        
        // Get current balance
        BigDecimal currentBalance = bankAccount.getCurrentBalance();
        BigDecimal newBalance = currentBalance.add(amount);
        
        // Create transaction record
        BankTransaction transaction = BankTransaction.builder()
                .bankAccount(bankAccount)
                .transactionType(TransactionType.CREDIT)
                .amount(amount)
                .balanceAfterTransaction(newBalance)
                .source(source)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .transactionReference(transactionReference)
                .party(party)
                .description(description)
                .transactionDate(LocalDateTime.now())
                .isReconciled(false)
                .build();
        
        // Save transaction
        transaction = bankTransactionRepository.save(transaction);
        
        // Update bank account balance directly
        bankAccount.setCurrentBalance(newBalance);
        bankAccountRepository.save(bankAccount);
        
        return transaction;
    }
    
    /**
     * Record a debit transaction (money going out)
     */
    public BankTransaction recordDebit(BankAccount bankAccount, BigDecimal amount, 
                                      TransactionSource source, String referenceType,
                                      Long referenceId, String referenceNumber,
                                      String transactionReference, String party,
                                      String description) {
        
        // Get current balance
        BigDecimal currentBalance = bankAccount.getCurrentBalance();
        BigDecimal newBalance = currentBalance.subtract(amount);
        
        // Create transaction record
        BankTransaction transaction = BankTransaction.builder()
                .bankAccount(bankAccount)
                .transactionType(TransactionType.DEBIT)
                .amount(amount)
                .balanceAfterTransaction(newBalance)
                .source(source)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .transactionReference(transactionReference)
                .party(party)
                .description(description)
                .transactionDate(LocalDateTime.now())
                .isReconciled(false)
                .build();
        
        // Save transaction
        transaction = bankTransactionRepository.save(transaction);
        
        // Update bank account balance directly
        bankAccount.setCurrentBalance(newBalance);
        bankAccountRepository.save(bankAccount);
        
        return transaction;
    }
    
    /**
     * Record a bill payment received
     */
    public BankTransaction recordBillPayment(BankAccount bankAccount, BigDecimal amount,
                                           Long billId, String billNumber,
                                           String transactionReference, String customerName) {
        
        String description = String.format("Payment received from %s for Bill %s", 
                                         customerName != null ? customerName : "Customer", 
                                         billNumber);
        
        return recordCredit(bankAccount, amount, TransactionSource.BILL_PAYMENT,
                          "BILL", billId, billNumber, transactionReference,
                          customerName, description);
    }
    
    /**
     * Record a purchase payment made
     */
    public BankTransaction recordPurchasePayment(BankAccount bankAccount, BigDecimal amount,
                                               Long purchaseId, String invoiceNumber,
                                               String transactionReference, String supplierName) {
        
        String description = String.format("Payment to %s for Purchase Invoice %s", 
                                         supplierName != null ? supplierName : "Supplier", 
                                         invoiceNumber);
        
        return recordDebit(bankAccount, amount, TransactionSource.PURCHASE_PAYMENT,
                         "PURCHASE", purchaseId, invoiceNumber, transactionReference,
                         supplierName, description);
    }
    
    /**
     * Record a customer refund payment (when exchange value > purchase value)
     * This debits from the bank account as we're paying the customer
     */
    public BankTransaction recordCustomerRefund(BankAccount bankAccount, BigDecimal amount,
                                              Long billId, String billNumber,
                                              String transactionReference, String customerName) {

        String description = String.format("Refund paid to %s for Bill %s (Exchange value exceeded purchase)",
                                         customerName != null ? customerName : "Customer",
                                         billNumber);

        return recordDebit(bankAccount, amount, TransactionSource.BILL_PAYMENT,
                         "BILL", billId, billNumber, transactionReference,
                         customerName, description);
    }

    /**
     * Record an exchange related transaction
     */
    public BankTransaction recordExchangeTransaction(BankAccount bankAccount, BigDecimal amount,
                                                   TransactionType type, Long exchangeId,
                                                   String exchangeNumber, String transactionReference,
                                                   String customerName) {

        String description = String.format("%s for Exchange %s - %s",
                                         type == TransactionType.CREDIT ? "Received" : "Paid",
                                         exchangeNumber,
                                         customerName != null ? customerName : "Customer");
        
        if (type == TransactionType.CREDIT) {
            return recordCredit(bankAccount, amount, TransactionSource.EXCHANGE_PAYMENT,
                              "EXCHANGE", exchangeId, exchangeNumber, transactionReference,
                              customerName, description);
        } else {
            return recordDebit(bankAccount, amount, TransactionSource.EXCHANGE_PAYMENT,
                             "EXCHANGE", exchangeId, exchangeNumber, transactionReference,
                             customerName, description);
        }
    }
    
    /**
     * Find all transactions for a bank account
     */
    @Transactional(readOnly = true)
    public List<BankTransaction> findByBankAccount(Long bankAccountId) {
        return bankTransactionRepository.findByBankAccountIdOrderByTransactionDateDesc(bankAccountId);
    }
    
    /**
     * Find transactions by date range
     */
    @Transactional(readOnly = true)
    public List<BankTransaction> findByDateRange(Long bankAccountId, LocalDateTime startDate, LocalDateTime endDate) {
        return bankTransactionRepository.findByBankAccountAndDateRange(bankAccountId, startDate, endDate);
    }
    
    /**
     * Find unreconciled transactions
     */
    @Transactional(readOnly = true)
    public List<BankTransaction> findUnreconciled(Long bankAccountId) {
        return bankTransactionRepository.findUnreconciledByBankAccount(bankAccountId);
    }
    
    /**
     * Mark transaction as reconciled
     */
    public BankTransaction reconcileTransaction(Long transactionId, String reconciledBy) {
        Optional<BankTransaction> transactionOpt = bankTransactionRepository.findById(transactionId);
        if (transactionOpt.isPresent()) {
            BankTransaction transaction = transactionOpt.get();
            transaction.setIsReconciled(true);
            transaction.setReconciledDate(LocalDateTime.now());
            transaction.setReconciledBy(reconciledBy);
            return bankTransactionRepository.save(transaction);
        }
        throw new IllegalArgumentException("Transaction not found with ID: " + transactionId);
    }
    
    /**
     * Get total credits for an account
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCredits(Long bankAccountId) {
        BigDecimal total = bankTransactionRepository.getTotalCreditsForAccount(bankAccountId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Get total debits for an account
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalDebits(Long bankAccountId) {
        BigDecimal total = bankTransactionRepository.getTotalDebitsForAccount(bankAccountId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Search transactions by keyword
     */
    @Transactional(readOnly = true)
    public List<BankTransaction> searchTransactions(Long bankAccountId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findByBankAccount(bankAccountId);
        }
        return bankTransactionRepository.searchByKeyword(bankAccountId, keyword);
    }
    
    /**
     * Get account balance at a specific date
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalanceAtDate(Long bankAccountId, LocalDateTime date) {
        List<BankTransaction> transactions = bankTransactionRepository.findTransactionsBeforeDate(bankAccountId, date);
        if (!transactions.isEmpty()) {
            return transactions.get(0).getBalanceAfterTransaction();
        }
        // If no transactions, return opening balance
        Optional<BankAccount> accountOpt = bankAccountRepository.findById(bankAccountId);
        return accountOpt.map(BankAccount::getOpeningBalance).orElse(BigDecimal.ZERO);
    }
    
    /**
     * Get latest transaction for an account
     */
    @Transactional(readOnly = true)
    public Optional<BankTransaction> getLatestTransaction(Long bankAccountId) {
        return bankTransactionRepository.findLatestByBankAccount(bankAccountId);
    }
    
    /**
     * Check if a reference already has a transaction
     */
    @Transactional(readOnly = true)
    public boolean existsByReference(String referenceType, Long referenceId) {
        return bankTransactionRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId).isPresent();
    }
}