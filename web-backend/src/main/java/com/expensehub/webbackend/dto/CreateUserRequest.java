package com.expensehub.webbackend.dto;

import com.expensehub.webbackend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(
                min = 6,
                message = "Password must contain at least 6 characters"
        )
        String password,

        @NotBlank(message = "Full name cannot be blank")
        String fullName,

        String department,

        @NotNull(message = "Role cannot be null")
        Role role

) {
}