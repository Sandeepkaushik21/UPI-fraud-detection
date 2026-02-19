package com.upi.fraud.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String upiId;

    @NotBlank
    private String password;
}
