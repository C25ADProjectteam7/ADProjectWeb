package com.expensehub.webbackend.dto;

import com.expensehub.webbackend.entity.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(
                min = 6,
                message = "Password must contain at least 6 characters"
        )
        String password,

        String fullName,

        String department,

        Role role
) {
}