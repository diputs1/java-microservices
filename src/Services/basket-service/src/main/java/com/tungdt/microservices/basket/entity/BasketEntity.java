package com.tungdt.microservices.basket.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BasketEntity {
    private String customerId;
    private List<BasketItemEntity> items = new ArrayList<>();
    private BigDecimal totalAmount;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<BasketItemEntity> getItems() {
        return items;
    }

    public void setItems(List<BasketItemEntity> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
