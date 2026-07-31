package com.expensehub.webbackend.service;

import com.expensehub.webbackend.dto.LoginRequest;
import com.expensehub.webbackend.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
