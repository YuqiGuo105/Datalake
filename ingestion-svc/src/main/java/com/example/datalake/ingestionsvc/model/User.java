package com.example.datalake.ingestionsvc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"User\"", schema = "public")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @Column(name = "\"userId\"")
    private Short userId;

    @Column(name = "\"userName\"")
    private String userName;

    public User() {
    }

    public User(Short userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    // ─── Getter / Setter for userId ─────────────────────────────────────────
    public Short getUserId() {
        return userId;
    }

    public void setUserId(Short userId) {
        this.userId = userId;
    }

    // ─── Getter / Setter for userName ────────────────────────────────────────
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
