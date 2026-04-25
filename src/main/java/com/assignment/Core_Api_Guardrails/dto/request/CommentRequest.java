package com.assignment.Core_Api_Guardrails.dto.request;

import com.assignment.Core_Api_Guardrails.entity.AuthorType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {

    @NotNull
    private Long authorId;

    @NotNull
    private AuthorType authorType;

    @NotBlank
    private String content;

    @Min(0)
    private int depthLevel;
}
