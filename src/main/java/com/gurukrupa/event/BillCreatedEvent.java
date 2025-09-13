package com.gurukrupa.event;

import com.gurukrupa.data.entities.Bill;
import org.springframework.context.ApplicationEvent;

public class BillCreatedEvent extends ApplicationEvent {
    private final Bill bill;
    
    public BillCreatedEvent(Object source, Bill bill) {
        super(source);
        this.bill = bill;
    }
    
    public Bill getBill() {
        return bill;
    }
}