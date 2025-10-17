package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.repository.MetalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MetalService {
    
    private final MetalRepository metalRepository;
    
    @Autowired
    public MetalService(MetalRepository metalRepository) {
        this.metalRepository = metalRepository;
    }
    
    // Basic CRUD operations
    public Metal saveMetal(Metal metal) {
        return metalRepository.save(metal);
    }
    
    public List<Metal> getAllMetals() {
        return metalRepository.findAll();
    }
    
    public List<Metal> getAllActiveMetals() {
        return metalRepository.findByIsActiveTrue();
    }
    
    public Optional<Metal> getMetalById(Long id) {
        return metalRepository.findById(id);
    }
    
    public Optional<Metal> getMetalByName(String metalName) {
        return metalRepository.findByMetalName(metalName);
    }
    
    public void deleteMetalById(Long id) {
        metalRepository.deleteById(id);
    }
    
    public boolean metalExists(Long id) {
        return metalRepository.existsById(id);
    }
    
    // Custom methods
    public List<Metal> getMetalsByType(String metalType) {
        return metalRepository.findByMetalType(metalType);
    }
    
    public List<Metal> getActiveMetalsByType(String metalType) {
        return metalRepository.findByMetalTypeAndIsActiveTrue(metalType);
    }
    
    public List<String> getDistinctMetalTypes() {
        return metalRepository.findDistinctMetalTypes();
    }
    
    public List<Metal> searchMetals(String query) {
        return metalRepository.searchMetals(query);
    }
    
    public List<String> getAllMetalNames() {
        return metalRepository.findAllMetalNames();
    }
    
    public boolean isMetalNameUnique(String metalName) {
        return !metalRepository.existsByMetalName(metalName);
    }
    
    public boolean isMetalNameUnique(String metalName, Long excludeId) {
        return !metalRepository.existsByMetalNameAndIdNot(metalName, excludeId);
    }
    
    public Metal updateMetal(Long id, Metal updatedMetal) {
        Optional<Metal> existingMetal = metalRepository.findById(id);
        if (existingMetal.isPresent()) {
            Metal metal = existingMetal.get();
            metal.setMetalName(updatedMetal.getMetalName());
            metal.setMetalType(updatedMetal.getMetalType());
            metal.setPurity(updatedMetal.getPurity());
            metal.setDescription(updatedMetal.getDescription());
            return metalRepository.save(metal);
        }
        throw new RuntimeException("Metal not found with id: " + id);
    }
    
    public void deactivateMetal(Long id) {
        Optional<Metal> existingMetal = metalRepository.findById(id);
        if (existingMetal.isPresent()) {
            Metal metal = existingMetal.get();
            metal.setIsActive(false);
            metalRepository.save(metal);
        } else {
            throw new RuntimeException("Metal not found with id: " + id);
        }
    }
    
    public void activateMetal(Long id) {
        Optional<Metal> existingMetal = metalRepository.findById(id);
        if (existingMetal.isPresent()) {
            Metal metal = existingMetal.get();
            metal.setIsActive(true);
            metalRepository.save(metal);
        } else {
            throw new RuntimeException("Metal not found with id: " + id);
        }
    }
    
    /**
     * Find Metal by metal type and numeric purity value
     * Handles conversion from numeric purity (e.g., 22, 18) to string format (e.g., "22K", "18K")
     * @param metalType The metal type (e.g., "Gold", "Silver")
     * @param numericPurity The numeric purity value (e.g., 22.00, 18.00)
     * @return Optional<Metal> matching metal or empty
     */
    public Optional<Metal> findByMetalTypeAndNumericPurity(String metalType, java.math.BigDecimal numericPurity) {
        if (metalType == null || numericPurity == null) {
            return Optional.empty();
        }

        // Get all metals of this type
        List<Metal> metals = metalRepository.findByMetalType(metalType);

        // Find matching metal by parsing purity
        for (Metal metal : metals) {
            try {
                java.math.BigDecimal metalPurity = metal.getPurityNumeric();
                // Compare with stripTrailingZeros for consistent comparison
                if (metalPurity.stripTrailingZeros().compareTo(numericPurity.stripTrailingZeros()) == 0) {
                    return Optional.of(metal);
                }
            } catch (Exception e) {
                // Skip if purity cannot be parsed
                continue;
            }
        }

        return Optional.empty();
    }

    /**
     * Find Metal ID by metal type and numeric purity value
     * @param metalType The metal type (e.g., "Gold", "Silver")
     * @param numericPurity The numeric purity value (e.g., 22.00, 18.00)
     * @return Metal ID or null if not found
     */
    public Long findMetalIdByTypeAndPurity(String metalType, java.math.BigDecimal numericPurity) {
        return findByMetalTypeAndNumericPurity(metalType, numericPurity)
                .map(Metal::getId)
                .orElse(null);
    }

    // Initialize default metals if none exist
    public void initializeDefaultMetals() {
        if (metalRepository.count() == 0) {
            // Gold types
            metalRepository.save(Metal.builder()
                .metalName("Gold 24K")
                .metalType("Gold")
                .purity("24K")
                .description("Pure gold with 99.9% purity")
                .build());
                
            metalRepository.save(Metal.builder()
                .metalName("Gold 22K")
                .metalType("Gold")
                .purity("22K")
                .description("Standard gold with 91.6% purity")
                .build());
                
            metalRepository.save(Metal.builder()
                .metalName("Gold 18K")
                .metalType("Gold")
                .purity("18K")
                .description("Gold with 75% purity")
                .build());
                
            metalRepository.save(Metal.builder()
                .metalName("Gold 14K")
                .metalType("Gold")
                .purity("14K")
                .description("Gold with 58.3% purity")
                .build());
                
            // Silver
            metalRepository.save(Metal.builder()
                .metalName("Silver 925")
                .metalType("Silver")
                .purity("92.5")
                .description("Sterling silver with 92.5% purity")
                .build());
                
            metalRepository.save(Metal.builder()
                .metalName("Silver 999")
                .metalType("Silver")
                .purity("99.9")
                .description("Fine silver with 99.9% purity")
                .build());
                
            // Platinum
            metalRepository.save(Metal.builder()
                .metalName("Platinum 950")
                .metalType("Platinum")
                .purity("95.0")
                .description("Platinum with 95% purity")
                .build());
        }
    }
}