package com.example.datalake.ingestionsvc.dao;

import com.example.datalake.ingestionsvc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Short> {
}
