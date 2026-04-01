package com.sanjay.auth.auth_app.repository;

import com.sanjay.auth.auth_app.dtos.TokenResponse;
import com.sanjay.auth.auth_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);


}
