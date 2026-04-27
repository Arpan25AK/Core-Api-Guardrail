package com.assignment.Core_Api_Guardrails.service;

import com.assignment.Core_Api_Guardrails.repository.CommentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepo commentRepo;

}
