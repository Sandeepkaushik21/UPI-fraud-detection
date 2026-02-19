package com.upi.fraud.auth.repository;

import com.upi.fraud.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUpiId(String upiId);

    boolean existsByUpiId(String upiId);
}
