package com.shop.auth.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password, // bcrypt chỉ dùng tối đa 72 byte
        @NotBlank @Size(max = 100) String fullName) {
}
