package com.gurukrupa.data.dto;

import lombok.*;

import java.time.LocalDate;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DailySalesReport {
    int id;
    LocalDate date;
    String itemname;
    float openingstock;
    float totalsale;
    float price;
    float totalprice;
    float rate;
    float totalrate;
    float margine;

}
