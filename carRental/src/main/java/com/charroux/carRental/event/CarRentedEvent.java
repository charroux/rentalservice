package com.charroux.carRental.event;

import java.time.LocalDateTime;

/**
 * Event representing a confirmed car rental
 * This event is published to Kafka when a customer confirms a rental
 */
public class CarRentedEvent {
    
    private String plateNumber;
    private String brand;
    private String model;
    private Integer carModelId;
    private Double finalPrice;
    private Double originalPrice;
    private Double discountAmount;
    private Boolean discountApplied;
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private LocalDateTime rentalConfirmedAt;
    
    // Default constructor for Jackson
    public CarRentedEvent() {
        this.rentalConfirmedAt = LocalDateTime.now();
    }
    
    // Constructor with all fields
    public CarRentedEvent(String plateNumber, String brand, String model, Integer carModelId,
                         Double finalPrice, Double originalPrice, Double discountAmount, 
                         Boolean discountApplied, String customerFirstName, 
                         String customerLastName, String customerEmail) {
        this.plateNumber = plateNumber;
        this.brand = brand;
        this.model = model;
        this.carModelId = carModelId;
        this.finalPrice = finalPrice;
        this.originalPrice = originalPrice;
        this.discountAmount = discountAmount;
        this.discountApplied = discountApplied;
        this.customerFirstName = customerFirstName;
        this.customerLastName = customerLastName;
        this.customerEmail = customerEmail;
        this.rentalConfirmedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getPlateNumber() {
        return plateNumber;
    }
    
    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public Integer getCarModelId() {
        return carModelId;
    }
    
    public void setCarModelId(Integer carModelId) {
        this.carModelId = carModelId;
    }
    
    public Double getFinalPrice() {
        return finalPrice;
    }
    
    public void setFinalPrice(Double finalPrice) {
        this.finalPrice = finalPrice;
    }
    
    public Double getOriginalPrice() {
        return originalPrice;
    }
    
    public void setOriginalPrice(Double originalPrice) {
        this.originalPrice = originalPrice;
    }
    
    public Double getDiscountAmount() {
        return discountAmount;
    }
    
    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    public Boolean getDiscountApplied() {
        return discountApplied;
    }
    
    public void setDiscountApplied(Boolean discountApplied) {
        this.discountApplied = discountApplied;
    }
    
    public String getCustomerFirstName() {
        return customerFirstName;
    }
    
    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }
    
    public String getCustomerLastName() {
        return customerLastName;
    }
    
    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public LocalDateTime getRentalConfirmedAt() {
        return rentalConfirmedAt;
    }
    
    public void setRentalConfirmedAt(LocalDateTime rentalConfirmedAt) {
        this.rentalConfirmedAt = rentalConfirmedAt;
    }
    
    @Override
    public String toString() {
        return "CarRentedEvent{" +
                "plateNumber='" + plateNumber + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", carModelId=" + carModelId +
                ", finalPrice=" + finalPrice +
                ", originalPrice=" + originalPrice +
                ", discountAmount=" + discountAmount +
                ", discountApplied=" + discountApplied +
                ", customerFirstName='" + customerFirstName + '\'' +
                ", customerLastName='" + customerLastName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", rentalConfirmedAt=" + rentalConfirmedAt +
                '}';
    }
}
