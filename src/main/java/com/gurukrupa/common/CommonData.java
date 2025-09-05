package com.gurukrupa.common;

import com.gurukrupa.data.entities.LoginUser;
import com.gurukrupa.data.entities.ShopInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
@Setter
public class CommonData {
    public static ShopInfo shopInfo;
    public static LoginUser loginUser;


    /*
    @Getter
    public static Login loginUser;
    @Getter
    public static List<String>ITEMNAMES = new ArrayList<>();
    public static List<String>customerNames = new ArrayList<>();
    @Getter
    public static ShopeInfo shopeeInfo;
    public CommonData() {
        super();
    }

    public static void setLoginUser(Login loginUser) {
        CommonData.loginUser = loginUser;
    }

    public static void setITEMNAMES(List<String> ITEMNAMES) {
        CommonData.ITEMNAMES.addAll(ITEMNAMES);
    }
    public static void setShopeeInfo(ShopeInfo shopeeInfo){
        CommonData.shopeeInfo = shopeeInfo;
    }*/
}
