package com.charroux.carRental.dto;

/**
 * DTO for rental confirmation request from Angular frontend
 */
public class RentalConfirmationDTO {
    private String plateNumber;
    private String brand;
    private String model;
    private Integer carModelId;
    private Double finalPrice;
    private Double originalPrice;
    private Double discountAmount;
    private Boolean discountApplied;
    private CustomerInfoDTO customerInfo;
    
    // Nested class for customer information
    public static class CustomerInfoDTO {
        private String firstName;
        private String lastName;
        private String email;
        
        public CustomerInfoDTO() {}
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
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
    
    public CustomerInfoDTO getCustomerInfo() {
        return customerInfo;
    }
    
    public void setCustomerInfo(CustomerInfoDTO customerInfo) {
        this.customerInfo = customerInfo;
    }
}
