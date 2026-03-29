package com.sanjay.auth.auth_app.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "roles")
public class Role {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(unique = true , nullable = false)
    private String name;
}
