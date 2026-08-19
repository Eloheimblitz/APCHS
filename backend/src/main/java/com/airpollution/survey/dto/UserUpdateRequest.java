package com.airpollution.survey.dto;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        String role,
        Boolean enabled,
        @Size(min = 8, message = "Password must be at least 8 characters") String password
) {
}
