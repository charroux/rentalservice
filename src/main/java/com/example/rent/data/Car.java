package com.example.rent.data;

import com.example.rent.Dates;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Car {

    String plateNumber;
    int id;
    Dates dates;
    List<Person> persons = new ArrayList<Person>();

    @OneToMany(cascade = CascadeType.ALL)
    public List<Person> getPersons() {
        return persons;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Transient
    public Dates getDates() {
        return dates;
    }

    public void setDates(Dates dates) {
        this.dates = dates;
    }
}
