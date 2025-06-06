package com.example.datalake.ingestionsvc.controller;

import com.example.datalake.ingestionsvc.dao.UserRepository;
import com.example.datalake.ingestionsvc.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 1) Create a new User (POST /api/users) */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User payload) {
        // If payload.userId is null, JPA will error (because userId is NOT NULL / PK).
        User saved = userRepository.save(payload);
        return ResponseEntity.ok(saved);
    }

    /** 2) Read all users (GET /api/users) */
    @GetMapping
    public ResponseEntity<List<User>> listAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    /** 3) Read a single User by ID (GET /api/users/{id}) */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") Short id) {
        Optional<User> opt = userRepository.findById(id);
        return opt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 4) Update a User by ID (PUT /api/users/{id}) */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable("id") Short id,
            @RequestBody User user
    ) {
        return userRepository.findById(id).map(existing -> {
            existing.setUserName(user.getUserName());
            // We do not change userId, since it’s the primary key.
            User updated = userRepository.save(existing);
            return ResponseEntity.ok(updated);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 5) Delete a User by ID (DELETE /api/users/{id}) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Short id) {
        return userRepository.findById(id).map(existing -> {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
