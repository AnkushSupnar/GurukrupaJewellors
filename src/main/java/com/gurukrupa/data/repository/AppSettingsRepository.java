package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {
    
    Optional<AppSettings> findBySettingName(String settingName);
    
    @Query("SELECT s.settingValue FROM AppSettings s WHERE s.settingName = :settingName")
    Optional<String> findSettingValueByName(@Param("settingName") String settingName);
    
    @Query("SELECT s FROM AppSettings s WHERE s.settingName LIKE :pattern")
    List<AppSettings> findBySettingNameContaining(@Param("pattern") String pattern);
    
    boolean existsBySettingName(String settingName);
}