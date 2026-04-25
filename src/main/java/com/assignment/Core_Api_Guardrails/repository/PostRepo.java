package com.assignment.Core_Api_Guardrails.repository;

import com.assignment.Core_Api_Guardrails.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepo extends JpaRepository<Post, Long> {
}
