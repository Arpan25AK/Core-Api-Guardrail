package com.assignment.Core_Api_Guardrails.dto.response;

import com.assignment.Core_Api_Guardrails.entity.AuthorType;
import lombok.Builder;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Data
@Builder
public class PostResponse {

    private Long id;

    private Long authorId;

    private AuthorType authorType;

    private String content;

    private LocalDateTime createdAt;
}
