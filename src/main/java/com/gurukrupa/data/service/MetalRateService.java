package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.entities.MetalRate;
import com.gurukrupa.data.repository.MetalRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MetalRateService {
    
    @Autowired
    private MetalRateRepository metalRateRepository;
    
    /**
     * Save or update metal rate for a specific date
     */
    public MetalRate saveMetalRate(MetalRate metalRate) {
        // Check if rate already exists for this metal and date
        Optional<MetalRate> existingRate = metalRateRepository.findByMetalAndRateDate(
                metalRate.getMetal(), metalRate.getRateDate());
        
        if (existingRate.isPresent()) {
            // Update existing rate
            MetalRate existing = existingRate.get();
            existing.setRatePerGram(metalRate.getRatePerGram());
            existing.setRatePerTenGrams(metalRate.getRatePerTenGrams());
            existing.setBuyingRate(metalRate.getBuyingRate());
            existing.setSellingRate(metalRate.getSellingRate());
            existing.setRemarks(metalRate.getRemarks());
            return metalRateRepository.save(existing);
        }
        
        return metalRateRepository.save(metalRate);
    }
    
    /**
     * Get metal rate for a specific metal and date
     */
    public Optional<MetalRate> getMetalRate(Metal metal, LocalDate date) {
        return metalRateRepository.findByMetalAndRateDate(metal, date);
    }
    
    /**
     * Get latest rate for a metal (on or before given date)
     */
    public Optional<MetalRate> getLatestMetalRate(Long metalId, LocalDate date) {
        List<MetalRate> rates = metalRateRepository.findLatestRateForMetal(metalId, date);
        return rates.isEmpty() ? Optional.empty() : Optional.of(rates.get(0));
    }
    
    /**
     * Get all rates for a specific date
     */
    public List<MetalRate> getRatesByDate(LocalDate date) {
        return metalRateRepository.findByRateDateWithMetal(date);
    }
    
    /**
     * Get rates for a metal type on a specific date
     */
    public List<MetalRate> getRatesByMetalTypeAndDate(String metalType, LocalDate date) {
        return metalRateRepository.findByMetalTypeAndDate(metalType, date);
    }
    
    /**
     * Get rate history for a specific metal
     */
    public List<MetalRate> getMetalRateHistory(Long metalId) {
        return metalRateRepository.findByMetalIdOrderByDateDesc(metalId);
    }
    
    /**
     * Get rates within a date range
     */
    public List<MetalRate> getRatesByDateRange(LocalDate startDate, LocalDate endDate) {
        return metalRateRepository.findByDateRange(startDate, endDate);
    }
    
    /**
     * Delete a metal rate
     */
    public void deleteMetalRate(Long id) {
        metalRateRepository.deleteById(id);
    }
    
    /**
     * Quick save method with just essential fields
     */
    public MetalRate saveMetalRate(Metal metal, LocalDate date, BigDecimal ratePerTenGrams) {
        MetalRate metalRate = MetalRate.builder()
                .metal(metal)
                .rateDate(date)
                .ratePerTenGrams(ratePerTenGrams)
                .ratePerGram(ratePerTenGrams.divide(new BigDecimal("10"), 2, BigDecimal.ROUND_HALF_UP))
                .build();
        return saveMetalRate(metalRate);
    }
}