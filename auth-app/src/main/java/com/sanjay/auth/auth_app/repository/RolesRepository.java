package com.sanjay.auth.auth_app.repository;

import com.sanjay.auth.auth_app.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface RolesRepository extends JpaRepository<Role, UUID> {

}
