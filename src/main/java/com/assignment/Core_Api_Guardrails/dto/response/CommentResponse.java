package com.assignment.Core_Api_Guardrails.dto.response;

import com.assignment.Core_Api_Guardrails.entity.AuthorType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long id;

    private Long authorId;

    private Long postId;

    private AuthorType authorType;

    private String content;

    private int depthLevel;

    private LocalDateTime createdAt;
}
