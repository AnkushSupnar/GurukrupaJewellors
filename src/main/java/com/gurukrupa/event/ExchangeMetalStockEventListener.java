package com.gurukrupa.event;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.Exchange;
import com.gurukrupa.data.entities.ExchangeTransaction;
import com.gurukrupa.data.service.ExchangeService;
import com.gurukrupa.data.service.ExchangeMetalStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class ExchangeMetalStockEventListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeMetalStockEventListener.class);
    
    @Autowired
    private ExchangeService exchangeService;
    
    @Autowired
    private ExchangeMetalStockService metalStockService;
    
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBillCreated(BillCreatedEvent event) {
        Bill bill = event.getBill();
        
        if (bill == null || bill.getStatus() == Bill.BillStatus.CANCELLED) {
            return;
        }
        
        try {
            // Wait a moment to ensure the bill and exchange transactions have committed
            Thread.sleep(1500);
            
            // Check if bill has exchange
            if (bill.getExchangeAmount() != null && bill.getExchangeAmount().compareTo(BigDecimal.ZERO) > 0) {
                LOG.info("Processing exchange metal stock for bill {}", bill.getBillNumber());
                
                // Find the exchange associated with this bill
                exchangeService.findByBillId(bill.getId()).ifPresent(exchange -> {
                    processExchangeMetalStock(exchange, bill);
                });
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Exchange metal stock processing interrupted for bill {}", bill.getBillNumber());
        } catch (Exception e) {
            LOG.error("Error processing exchange metal stock for bill {}: {}", 
                     bill.getBillNumber(), e.getMessage(), e);
        }
    }
    
    private void processExchangeMetalStock(Exchange exchange, Bill bill) {
        String customerName = bill.getCustomer() != null ? 
                            bill.getCustomer().getCustomerFullName() : "Customer";
        
        // Check if already processed
        if (metalStockService.isExchangeProcessed(exchange.getId())) {
            LOG.info("Exchange {} already processed for metal stock", exchange.getId());
            return;
        }
        
        // Process each exchange transaction
        for (ExchangeTransaction transaction : exchange.getExchangeTransactions()) {
            try {
                // Get metal type and weight
                String metalType = transaction.getMetalType();
                BigDecimal weight = transaction.getNetWeight();
                BigDecimal purity = transaction.getPurity() != null ? transaction.getPurity() : new BigDecimal("0");
                
                // Skip if no weight
                if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                    LOG.warn("Skipping exchange transaction with zero weight for item {} in exchange {}", 
                            transaction.getItemName(), exchange.getId());
                    continue;
                }
                
                // Add to metal stock
                metalStockService.addExchangeMetalWeight(
                    metalType,
                    purity,
                    weight,
                    "EXCHANGE",
                    exchange.getId(),
                    bill.getBillNumber(),
                    customerName
                );
                
                LOG.info("Added {} grams of {} ({}k) to exchange metal stock from bill {}", 
                        weight, metalType, purity, bill.getBillNumber());
                
            } catch (Exception e) {
                LOG.error("Error processing exchange transaction {} for metal stock: {}", 
                         transaction.getId(), e.getMessage());
            }
        }
        
        LOG.info("Completed exchange metal stock processing for exchange {} in bill {}", 
                exchange.getId(), bill.getBillNumber());
    }
}