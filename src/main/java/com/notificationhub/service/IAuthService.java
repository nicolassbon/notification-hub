package com.notificationhub.service;

import com.notificationhub.dto.request.LoginRequest;
import com.notificationhub.dto.request.RegisterRequest;
import com.notificationhub.dto.response.AuthResponse;
import com.notificationhub.dto.response.RegisterResponse;
import com.notificationhub.entity.User;

public interface IAuthService {

    /**
     * Registra un nuevo usuario
     *
     * @param request Datos de registro
     * @return Respuesta de registro con detalles del usuario
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Autentica un usuario existente
     *
     * @param request Datos de login
     * @return Respuesta de autenticación con token JWT
     */
    AuthResponse login(LoginRequest request);

    /**
     * Obtiene información del usuario actual
     *
     * @return Usuario actual (null si no hay usuario autenticado)
     */
    User getCurrentUser();

    /**
     * Verifica si el usuario actual puede realizar acciones de admin
     */
    boolean isCurrentUserAdmin();
}
