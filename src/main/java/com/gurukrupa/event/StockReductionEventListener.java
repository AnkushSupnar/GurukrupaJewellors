package com.gurukrupa.event;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import com.gurukrupa.data.service.JewelryItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StockReductionEventListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(StockReductionEventListener.class);
    
    @Autowired
    private JewelryItemService jewelryItemService;
    
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBillCreated(BillCreatedEvent event) {
        Bill bill = event.getBill();
        
        if (bill == null || bill.getStatus() == Bill.BillStatus.CANCELLED) {
            return;
        }
        
        try {
            // Wait a moment to ensure the bill transaction has committed
            Thread.sleep(1000);
            
            LOG.info("Processing stock reduction for bill {} via event", bill.getBillNumber());
            
            String customerName = bill.getCustomer() != null ? 
                                bill.getCustomer().getCustomerFullName() : "Customer";
            
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
                             transaction.getItemCode(), bill.getBillNumber(), e.getMessage());
                }
            }
            
            LOG.info("Completed stock reduction processing for bill {} via event", bill.getBillNumber());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Stock reduction interrupted for bill {}", bill.getBillNumber());
        } catch (Exception e) {
            LOG.error("Unexpected error in stock reduction for bill {}: {}", 
                     bill.getBillNumber(), e.getMessage(), e);
        }
    }
}