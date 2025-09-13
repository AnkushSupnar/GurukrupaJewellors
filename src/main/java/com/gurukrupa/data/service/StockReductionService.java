package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockReductionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(StockReductionService.class);
    
    @Autowired
    private JewelryItemService jewelryItemService;
    
    /**
     * Process stock reduction for a bill asynchronously
     * This runs in a completely separate transaction context
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processStockReductionAsync(Bill bill) {
        try {
            // Add a small delay to ensure the bill transaction has committed
            Thread.sleep(500);
            
            processStockReduction(bill);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Stock reduction interrupted for bill {}", bill.getBillNumber());
        }
    }
    
    /**
     * Process stock reduction for a bill synchronously
     * This runs in a new transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, 
                   rollbackFor = Exception.class,
                   noRollbackFor = {RuntimeException.class})
    public void processStockReduction(Bill bill) {
        if (bill == null || bill.getStatus() == Bill.BillStatus.CANCELLED) {
            return;
        }
        
        String customerName = bill.getCustomer() != null ? 
                            bill.getCustomer().getCustomerFullName() : "Customer";
        
        LOG.info("Processing stock reduction for bill {}", bill.getBillNumber());
        
        // Process each transaction to reduce stock
        for (BillTransaction transaction : bill.getBillTransactions()) {
            try {
                // Get quantity from transaction (default to 1 if not set)
                Integer quantity = transaction.getQuantity() != null ? transaction.getQuantity() : 1;
                
                // Skip if quantity is 0 or negative
                if (quantity <= 0) {
                    LOG.warn("Skipping stock reduction for item {} with quantity {} in bill {}", 
                            transaction.getItemCode(), quantity, bill.getBillNumber());
                    continue;
                }
                
                // Reduce stock using the jewelry item service
                jewelryItemService.reduceStockForSale(
                    null, // itemId will be looked up by itemCode
                    transaction.getItemCode(),
                    quantity,
                    bill.getId(),
                    bill.getBillNumber(),
                    customerName
                );
                
                LOG.info("Stock reduced for item {} quantity {} in bill {}", 
                        transaction.getItemCode(), quantity, bill.getBillNumber());
                
            } catch (Exception e) {
                // Log the error but continue with other items
                LOG.error("Error reducing stock for item {} in bill {}: {}", 
                         transaction.getItemCode(), bill.getBillNumber(), e.getMessage(), e);
            }
        }
        
        LOG.info("Completed stock reduction processing for bill {}", bill.getBillNumber());
    }
}