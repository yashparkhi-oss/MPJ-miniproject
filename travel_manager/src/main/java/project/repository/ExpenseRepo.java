package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import project.entity.Expense;

public interface ExpenseRepo extends JpaRepository<Expense, Long> {
}