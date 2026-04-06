package project.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double budget;
    private String location;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getBudget() { return budget; }
    public String getLocation() { return location; }
    public List<Expense> getExpenses() { return expenses; }
    public User getUser() { return user; }
    

    public void setName(String name) { this.name = name; }
    public void setBudget(double budget) { this.budget = budget; }
    public void setLocation(String location) { this.location = location; }
    public void setUser(User user) { this.user = user; }
    
}

