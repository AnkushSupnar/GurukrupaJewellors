package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.entities.PurchaseMetalStock;
import com.gurukrupa.data.repository.PurchaseMetalStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PurchaseMetalStockService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseMetalStockService.class);

    @Autowired
    private PurchaseMetalStockRepository purchaseMetalStockRepository;

    /**
     * Add metal to purchase stock using Metal entity reference
     * This is the preferred method for adding purchased metal
     */
    public PurchaseMetalStock addPurchasedMetal(Metal metal, BigDecimal grossWeight, BigDecimal netWeight,
                                                 String reference, String description) {
        LOG.info("Adding purchased metal: {} (ID: {}) - Gross: {}g, Net: {}g - Ref: {}",
                metal.getMetalName(), metal.getId(), grossWeight, netWeight, reference);

        // Find or create stock entry using Metal reference
        Optional<PurchaseMetalStock> existingStock = purchaseMetalStockRepository.findByMetal(metal);

        PurchaseMetalStock stock;
        if (existingStock.isPresent()) {
            stock = existingStock.get();
            stock.addMetal(grossWeight, netWeight);
            LOG.info("Updated existing stock for {} (ID: {}) - New available: {}g",
                    metal.getMetalName(), metal.getId(), stock.getAvailableWeight());
        } else {
            // Parse purity from Metal.purity string (e.g., "22K" -> 916, "18K" -> 750)
            BigDecimal purityValue = parsePurity(metal.getPurity());

            stock = PurchaseMetalStock.builder()
                    .metal(metal)
                    .metalType(metal.getMetalType())
                    .purity(purityValue)
                    .totalGrossWeight(grossWeight)
                    .totalNetWeight(netWeight)
                    .availableWeight(netWeight)
                    .usedWeight(BigDecimal.ZERO)
                    .build();
            LOG.info("Created new stock entry for {} (ID: {}) - Available: {}g",
                    metal.getMetalName(), metal.getId(), stock.getAvailableWeight());
        }

        return purchaseMetalStockRepository.save(stock);
    }

    /**
     * Add metal to purchase stock (backward compatibility method)
     * This is called when metal is purchased from suppliers
     */
    public PurchaseMetalStock addPurchasedMetal(String metalType, BigDecimal purity,
                                                 BigDecimal grossWeight, BigDecimal netWeight,
                                                 String reference, String description) {
        LOG.info("Adding purchased metal: {} {} - Gross: {}g, Net: {}g - Ref: {}",
                metalType, purity, grossWeight, netWeight, reference);

        // Find or create stock entry
        Optional<PurchaseMetalStock> existingStock =
                purchaseMetalStockRepository.findByMetalTypeAndPurity(metalType, purity);

        PurchaseMetalStock stock;
        if (existingStock.isPresent()) {
            stock = existingStock.get();
            stock.addMetal(grossWeight, netWeight);
            LOG.info("Updated existing stock for {} {} - New available: {}g",
                    metalType, purity, stock.getAvailableWeight());
        } else {
            stock = PurchaseMetalStock.builder()
                    .metalType(metalType)
                    .purity(purity)
                    .totalGrossWeight(grossWeight)
                    .totalNetWeight(netWeight)
                    .availableWeight(netWeight)
                    .usedWeight(BigDecimal.ZERO)
                    .build();
            LOG.info("Created new stock entry for {} {} - Available: {}g",
                    metalType, purity, stock.getAvailableWeight());
        }

        return purchaseMetalStockRepository.save(stock);
    }

    /**
     * Parse purity string to BigDecimal value
     * Examples: "22K" -> 916, "18K" -> 750, "14K" -> 585, "92.5" -> 925
     */
    private BigDecimal parsePurity(String purityStr) {
        if (purityStr == null || purityStr.isEmpty()) {
            return BigDecimal.ZERO;
        }

        purityStr = purityStr.trim().toUpperCase();

        // Handle karat values
        if (purityStr.endsWith("K")) {
            String karatStr = purityStr.substring(0, purityStr.length() - 1);
            try {
                int karat = Integer.parseInt(karatStr);
                // Convert karat to purity (e.g., 22K = 916, 18K = 750)
                return BigDecimal.valueOf((karat * 1000) / 24);
            } catch (NumberFormatException e) {
                LOG.warn("Could not parse karat value: {}", purityStr);
            }
        }

        // Handle decimal values (e.g., "92.5")
        try {
            double purityDouble = Double.parseDouble(purityStr);
            // If value is less than 100, assume it's a percentage and convert to parts per thousand
            if (purityDouble < 100) {
                return BigDecimal.valueOf(purityDouble * 10);
            }
            return BigDecimal.valueOf(purityDouble);
        } catch (NumberFormatException e) {
            LOG.warn("Could not parse purity value: {}", purityStr);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Use metal from purchase stock using Metal entity reference (preferred method)
     */
    public PurchaseMetalStock useMetal(Metal metal, BigDecimal weight, String reference, String description) {
        LOG.info("Using metal from purchase stock: {} (ID: {}) - Weight: {}g - Ref: {}",
                metal.getMetalName(), metal.getId(), weight, reference);

        Optional<PurchaseMetalStock> stockOpt = purchaseMetalStockRepository.findByMetal(metal);

        if (stockOpt.isEmpty()) {
            throw new IllegalStateException("No purchase stock found for " + metal.getMetalName() + " (ID: " + metal.getId() + ")");
        }

        PurchaseMetalStock stock = stockOpt.get();
        stock.useMetal(weight);

        LOG.info("Metal used successfully - Remaining available: {}g", stock.getAvailableWeight());
        return purchaseMetalStockRepository.save(stock);
    }

    /**
     * Use metal from purchase stock (for manufacturing/sales)
     */
    public PurchaseMetalStock useMetal(String metalType, BigDecimal purity,
                                       BigDecimal weight, String reference, String description) {
        LOG.info("Using metal from purchase stock: {} {} - Weight: {}g - Ref: {}",
                metalType, purity, weight, reference);

        Optional<PurchaseMetalStock> stockOpt =
                purchaseMetalStockRepository.findByMetalTypeAndPurity(metalType, purity);

        if (stockOpt.isEmpty()) {
            throw new IllegalStateException("No purchase stock found for " + metalType + " " + purity);
        }

        PurchaseMetalStock stock = stockOpt.get();
        stock.useMetal(weight);

        LOG.info("Metal used successfully - Remaining available: {}g", stock.getAvailableWeight());
        return purchaseMetalStockRepository.save(stock);
    }

    /**
     * Return metal to purchase stock using Metal entity reference (preferred method)
     */
    public PurchaseMetalStock returnMetal(Metal metal, BigDecimal weight, String reference, String description) {
        LOG.info("Returning metal to purchase stock: {} (ID: {}) - Weight: {}g - Ref: {}",
                metal.getMetalName(), metal.getId(), weight, reference);

        Optional<PurchaseMetalStock> stockOpt = purchaseMetalStockRepository.findByMetal(metal);

        if (stockOpt.isEmpty()) {
            throw new IllegalStateException("No purchase stock found for " + metal.getMetalName() + " (ID: " + metal.getId() + ")");
        }

        PurchaseMetalStock stock = stockOpt.get();
        stock.returnMetal(weight);

        LOG.info("Metal returned successfully - New available: {}g", stock.getAvailableWeight());
        return purchaseMetalStockRepository.save(stock);
    }

    /**
     * Return metal to purchase stock (from cancelled orders, etc.)
     */
    public PurchaseMetalStock returnMetal(String metalType, BigDecimal purity,
                                          BigDecimal weight, String reference, String description) {
        LOG.info("Returning metal to purchase stock: {} {} - Weight: {}g - Ref: {}",
                metalType, purity, weight, reference);

        Optional<PurchaseMetalStock> stockOpt =
                purchaseMetalStockRepository.findByMetalTypeAndPurity(metalType, purity);

        if (stockOpt.isEmpty()) {
            throw new IllegalStateException("No purchase stock found for " + metalType + " " + purity);
        }

        PurchaseMetalStock stock = stockOpt.get();
        stock.returnMetal(weight);

        LOG.info("Metal returned successfully - New available: {}g", stock.getAvailableWeight());
        return purchaseMetalStockRepository.save(stock);
    }

    /**
     * Get available weight for a specific metal type and purity
     */
    public BigDecimal getAvailableWeight(String metalType, BigDecimal purity) {
        BigDecimal weight = purchaseMetalStockRepository.getAvailableWeight(metalType, purity);
        return weight != null ? weight : BigDecimal.ZERO;
    }

    /**
     * Get total available weight for a metal type (all purities)
     */
    public BigDecimal getTotalAvailableWeightByMetalType(String metalType) {
        BigDecimal weight = purchaseMetalStockRepository.getTotalAvailableWeightByMetalType(metalType);
        return weight != null ? weight : BigDecimal.ZERO;
    }

    /**
     * Get all stock entries
     */
    public List<PurchaseMetalStock> getAllStock() {
        return purchaseMetalStockRepository.findAllByOrderByMetalTypeAscPurityDesc();
    }

    /**
     * Get all stock entries with available weight > 0
     */
    public List<PurchaseMetalStock> getAllAvailableStock() {
        return purchaseMetalStockRepository.findAllWithAvailableStock();
    }

    /**
     * Get stock for a specific metal type
     */
    public List<PurchaseMetalStock> getStockByMetalType(String metalType) {
        return purchaseMetalStockRepository.findByMetalType(metalType);
    }

    /**
     * Get stock entry for specific metal type and purity
     */
    public Optional<PurchaseMetalStock> getStock(String metalType, BigDecimal purity) {
        return purchaseMetalStockRepository.findByMetalTypeAndPurity(metalType, purity);
    }

    /**
     * Check if sufficient stock is available
     */
    public boolean isStockAvailable(String metalType, BigDecimal purity, BigDecimal requiredWeight) {
        BigDecimal availableWeight = getAvailableWeight(metalType, purity);
        return availableWeight.compareTo(requiredWeight) >= 0;
    }

    /**
     * Get stock by ID
     */
    public Optional<PurchaseMetalStock> findById(Long id) {
        return purchaseMetalStockRepository.findById(id);
    }

    /**
     * Save or update stock
     */
    public PurchaseMetalStock save(PurchaseMetalStock stock) {
        return purchaseMetalStockRepository.save(stock);
    }

    /**
     * Delete stock entry
     */
    public void delete(Long id) {
        purchaseMetalStockRepository.deleteById(id);
    }

    /**
     * Get distinct metal types from purchase metal stock
     */
    public List<String> getDistinctMetalTypes() {
        return purchaseMetalStockRepository.findDistinctMetalTypes();
    }
}
