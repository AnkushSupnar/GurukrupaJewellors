package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.ShopInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopInfoRepository extends JpaRepository<ShopInfo, Long> {

}
