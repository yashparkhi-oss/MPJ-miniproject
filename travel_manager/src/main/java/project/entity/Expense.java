package project.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private String description;
    private double amount;
    private LocalDate date;

    @ManyToOne
    private Trip trip;

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public Trip getTrip() { return trip; }

    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTrip(Trip trip) { this.trip = trip; }
}
