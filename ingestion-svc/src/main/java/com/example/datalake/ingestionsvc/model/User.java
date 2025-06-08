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
}
