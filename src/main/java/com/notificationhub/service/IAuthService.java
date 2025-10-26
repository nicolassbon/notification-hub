package com.notificationhub.service;

import com.notificationhub.dto.request.LoginRequest;
import com.notificationhub.dto.request.RegisterRequest;
import com.notificationhub.dto.response.AuthResponse;
import com.notificationhub.dto.response.RegisterResponse;
import com.notificationhub.entity.User;

public interface IAuthService {
    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    User getCurrentUser();

    boolean isCurrentUserAdmin();
}
