package com.charroux.carRental.web;

public class RentCarCommand {

    String firstName;
    String lastName;
    String email;
    int price;

    public RentCarCommand() {
    }

    public RentCarCommand(String firstName, String lastName, String email, int price) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.price = price;
    }

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

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
