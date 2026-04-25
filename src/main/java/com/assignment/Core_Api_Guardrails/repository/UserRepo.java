package com.assignment.Core_Api_Guardrails.repository;

import com.assignment.Core_Api_Guardrails.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
}
