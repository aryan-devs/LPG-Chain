package com.lpgchain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "distributor_id")
    private Distributor distributor;

    private String deliveryAddress;
    private LocalDateTime bookingDate;
    private Integer quantity;
    private String status;

    public Booking() {
    }

    public Booking(User customer, Distributor distributor, String deliveryAddress,
                   LocalDateTime bookingDate, Integer quantity, String status) {
        this.customer = customer;
        this.distributor = distributor;
        this.deliveryAddress = deliveryAddress;
        this.bookingDate = bookingDate;
        this.quantity = quantity;
        this.status = status;
    }

    public Booking(Long id, User customer, Distributor distributor, String deliveryAddress,
                   LocalDateTime bookingDate, Integer quantity, String status) {
        this.id = id;
        this.customer = customer;
        this.distributor = distributor;
        this.deliveryAddress = deliveryAddress;
        this.bookingDate = bookingDate;
        this.quantity = quantity;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public Distributor getDistributor() { return distributor; }
    public void setDistributor(Distributor distributor) { this.distributor = distributor; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
