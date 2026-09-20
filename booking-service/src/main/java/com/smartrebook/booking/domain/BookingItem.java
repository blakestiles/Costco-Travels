package com.smartrebook.booking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_items")
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "item_type", nullable = false, length = 30)
    private String itemType;

    @Column(nullable = false, length = 50)
    private String supplier;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "supplier_confirmation", length = 60)
    private String supplierConfirmation;

    @Column(nullable = false, length = 30)
    private String status;

    protected BookingItem() {
    }

    public BookingItem(String itemType, String supplier, String description, BigDecimal amount,
                        String supplierConfirmation, String status) {
        this.itemType = itemType;
        this.supplier = supplier;
        this.description = description;
        this.amount = amount;
        this.supplierConfirmation = supplierConfirmation;
        this.status = status;
    }

    void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public String getItemType() {
        return itemType;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getSupplierConfirmation() {
        return supplierConfirmation;
    }

    public String getStatus() {
        return status;
    }
}
