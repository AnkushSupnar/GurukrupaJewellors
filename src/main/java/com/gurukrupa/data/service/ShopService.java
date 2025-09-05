package com.gurukrupa.data.service;


import com.gurukrupa.data.entities.LoginUser;
import com.gurukrupa.data.entities.ShopInfo;
import com.gurukrupa.data.repository.LoginUserRepository;
import com.gurukrupa.data.repository.ShopInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {

    @Autowired
    private ShopInfoRepository shopInfoRepository;

    @Autowired
    private LoginUserRepository loginUserRepository;


    @Transactional
    public boolean registerShop(ShopInfo shopInfo, String adminPassword) {
        // Save shop info
        ShopInfo savedShop = shopInfoRepository.save(shopInfo);

        // Create admin user login (username fixed as "admin")
        LoginUser adminUser = LoginUser.builder()
                .password(adminPassword)
                .username("Admin")
                .build();

        loginUserRepository.save(adminUser);
        return true;
    }
    public ShopInfo getShopInfo(){
        return shopInfoRepository.findById(1L).orElse(null);
    }

}
