package com.assignment.Core_Api_Guardrails.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeRequest {

    @NotNull
    private Long userId;
}
