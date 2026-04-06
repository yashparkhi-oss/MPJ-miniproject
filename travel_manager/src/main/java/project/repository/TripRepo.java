package project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.entity.Trip;
import project.entity.User;

public interface TripRepo extends JpaRepository<Trip, Long> {
    List<Trip> findByUser(User user);
}