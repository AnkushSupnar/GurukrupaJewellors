package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.entities.BankTransaction;
import com.gurukrupa.data.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BankAccountService {
    
    @Autowired
    private BankAccountRepository bankAccountRepository;
    
    @Autowired
    private BankTransactionService bankTransactionService;
    
    public BankAccount saveBankAccount(BankAccount bankAccount) {
        return bankAccountRepository.save(bankAccount);
    }
    
    public BankAccount createBankAccount(String bankName, String accountNumber, String ifscCode,
                                       String accountHolderName, BankAccount.AccountType accountType,
                                       String branchName, String branchAddress, BigDecimal openingBalance,
                                       BankAccount.BalanceType balanceType) {
        
        // Check if account number already exists
        if (bankAccountRepository.findByAccountNumber(accountNumber).isPresent()) {
            throw new IllegalArgumentException("Bank account with this account number already exists");
        }
        
        // Set initial balance to zero for new accounts
        // The opening balance will be added via transaction
        BankAccount bankAccount = BankAccount.builder()
                .bankName(bankName)
                .accountNumber(accountNumber)
                .ifscCode(ifscCode)
                .accountHolderName(accountHolderName)
                .accountType(accountType)
                .branchName(branchName)
                .branchAddress(branchAddress)
                .openingBalance(openingBalance != null ? openingBalance : BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO) // Start with zero balance
                .balanceType(balanceType)
                .isActive(true)
                .build();
        
        BankAccount savedAccount = saveBankAccount(bankAccount);
        
        // Create opening balance transaction if balance is greater than zero
        if (openingBalance != null && openingBalance.compareTo(BigDecimal.ZERO) > 0) {
            // This will update the current balance to the opening balance
            bankTransactionService.recordCredit(
                savedAccount,
                openingBalance,
                BankTransaction.TransactionSource.OPENING_BALANCE,
                "OPENING",
                savedAccount.getId(),
                "OPENING-" + savedAccount.getAccountNumber(),
                null,
                accountHolderName,
                "Opening balance for account " + accountNumber
            );
            
            // Refresh the account to get the updated balance
            savedAccount = bankAccountRepository.findById(savedAccount.getId()).orElse(savedAccount);
        }
        
        return savedAccount;
    }
    
    public Optional<BankAccount> findById(Long id) {
        return bankAccountRepository.findById(id);
    }
    
    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        return bankAccountRepository.findByAccountNumber(accountNumber);
    }
    
    public List<BankAccount> getAllActiveBankAccounts() {
        return bankAccountRepository.findByIsActiveTrueOrderByCreatedDateDesc();
    }
    
    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }
    
    public List<BankAccount> searchBankAccounts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveBankAccounts();
        }
        return bankAccountRepository.searchActiveAccounts(keyword.trim());
    }
    
    public List<BankAccount> findByBankName(String bankName) {
        return bankAccountRepository.findByBankNameContainingIgnoreCase(bankName);
    }
    
    public List<BankAccount> findByAccountType(BankAccount.AccountType accountType) {
        return bankAccountRepository.findByAccountType(accountType);
    }
    
    public BankAccount updateBankAccount(BankAccount bankAccount) {
        if (bankAccount.getId() == null) {
            throw new IllegalArgumentException("Bank account ID cannot be null for update");
        }
        
        Optional<BankAccount> existingAccount = bankAccountRepository.findById(bankAccount.getId());
        if (existingAccount.isEmpty()) {
            throw new IllegalArgumentException("Bank account not found with ID: " + bankAccount.getId());
        }
        
        // Check if account number is being changed and if new number already exists
        BankAccount existing = existingAccount.get();
        if (!existing.getAccountNumber().equals(bankAccount.getAccountNumber())) {
            if (bankAccountRepository.findByAccountNumber(bankAccount.getAccountNumber()).isPresent()) {
                throw new IllegalArgumentException("Bank account with this account number already exists");
            }
        }
        
        return bankAccountRepository.save(bankAccount);
    }
    
    public void deactivateBankAccount(Long id) {
        Optional<BankAccount> bankAccount = bankAccountRepository.findById(id);
        if (bankAccount.isPresent()) {
            BankAccount account = bankAccount.get();
            account.setIsActive(false);
            bankAccountRepository.save(account);
        } else {
            throw new IllegalArgumentException("Bank account not found with ID: " + id);
        }
    }
    
    public void activateBankAccount(Long id) {
        Optional<BankAccount> bankAccount = bankAccountRepository.findById(id);
        if (bankAccount.isPresent()) {
            BankAccount account = bankAccount.get();
            account.setIsActive(true);
            bankAccountRepository.save(account);
        } else {
            throw new IllegalArgumentException("Bank account not found with ID: " + id);
        }
    }
    
    public void deleteBankAccount(Long id) {
        if (bankAccountRepository.existsById(id)) {
            bankAccountRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Bank account not found with ID: " + id);
        }
    }
    
    public Long getTotalBankAccountsCount() {
        return bankAccountRepository.countActiveAccounts();
    }
    
    public BigDecimal getTotalBankBalance() {
        BigDecimal total = bankAccountRepository.getTotalBankBalance();
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public BankAccount updateBalance(Long id, BigDecimal newBalance) {
        Optional<BankAccount> bankAccount = bankAccountRepository.findById(id);
        if (bankAccount.isPresent()) {
            BankAccount account = bankAccount.get();
            account.setCurrentBalance(newBalance);
            return bankAccountRepository.save(account);
        } else {
            throw new IllegalArgumentException("Bank account not found with ID: " + id);
        }
    }
    
    public BankAccount addToBalance(Long id, BigDecimal amount) {
        Optional<BankAccount> bankAccount = bankAccountRepository.findById(id);
        if (bankAccount.isPresent()) {
            BankAccount account = bankAccount.get();
            BigDecimal newBalance = account.getCurrentBalance().add(amount);
            account.setCurrentBalance(newBalance);
            return bankAccountRepository.save(account);
        } else {
            throw new IllegalArgumentException("Bank account not found with ID: " + id);
        }
    }
    
    public BankAccount subtractFromBalance(Long id, BigDecimal amount) {
        Optional<BankAccount> bankAccount = bankAccountRepository.findById(id);
        if (bankAccount.isPresent()) {
            BankAccount account = bankAccount.get();
            BigDecimal newBalance = account.getCurrentBalance().subtract(amount);
            account.setCurrentBalance(newBalance);
            return bankAccountRepository.save(account);
        } else {
            throw new IllegalArgumentException("Bank account not found with ID: " + id);
        }
    }
}