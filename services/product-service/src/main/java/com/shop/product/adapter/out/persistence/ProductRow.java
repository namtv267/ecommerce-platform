package com.shop.product.adapter.out.persistence;

import java.math.BigDecimal;

/** Kết quả thô MyBatis map từ ClickHouse (cần setter + constructor rỗng). */
public class ProductRow {
    private String id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private int stock;
    private boolean active;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
