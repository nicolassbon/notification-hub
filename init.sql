-- ============================================
-- Notification Hub - Database Schema (MySQL)
-- ============================================

-- Crear la base de datos
CREATE DATABASE IF NOT EXISTS notification_hub
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE notification_hub;

-- ============================================
-- Tabla: users
-- Almacena los usuarios del sistema
-- ============================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER' NOT NULL,
    daily_message_limit INT DEFAULT 100 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    
    INDEX idx_username (username),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Tabla: platform_configurations
-- Configuración global de plataformas
-- ============================================
CREATE TABLE platform_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_type ENUM('TELEGRAM', 'DISCORD') UNIQUE NOT NULL,
    configuration JSON NOT NULL COMMENT 'Configuración específica de cada plataforma (tokens, webhooks, etc.)',
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_platform_active (platform_type, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Tabla: messages
-- Mensajes enviados por los usuarios
-- ============================================
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_created (created_at),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Tabla: message_deliveries
-- Registro de entregas a cada plataforma
-- ============================================
CREATE TABLE message_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    platform_type ENUM('TELEGRAM', 'DISCORD') NOT NULL,
    destination VARCHAR(255) NOT NULL COMMENT 'Chat ID, Channel ID, etc.',
    status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING' NOT NULL,
    provider_response JSON COMMENT 'Respuesta completa del proveedor',
    error_message TEXT,
    sent_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    INDEX idx_message (message_id),
    INDEX idx_status (status),
    INDEX idx_platform (platform_type),
    INDEX idx_status_platform (status, platform_type),
    INDEX idx_sent_at (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Tabla: daily_message_counts
-- Contador de mensajes diarios por usuario
-- ============================================
CREATE TABLE daily_message_counts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    count INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_date (user_id, date),
    INDEX idx_user_date (user_id, date),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Datos iniciales
-- ============================================

-- Usuario Admin por defecto
-- Password: admin123 (debe ser hasheado con BCrypt en la aplicación)
-- Aquí un ejemplo con BCrypt rounds=10: $2a$10$...
INSERT INTO users (username, password_hash, role, daily_message_limit) 
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 1000);

-- Configuraciones de plataformas (ajustar con tus credenciales reales)
INSERT INTO platform_configurations (platform_type, configuration, is_active) VALUES
('TELEGRAM', JSON_OBJECT(
    'botToken', 'YOUR_TELEGRAM_BOT_TOKEN',
    'defaultChatId', 'YOUR_DEFAULT_CHAT_ID'
), TRUE),
('DISCORD', JSON_OBJECT(
    'webhookUrl', 'YOUR_DISCORD_WEBHOOK_URL'
), TRUE);

-- ============================================
-- Queries útiles para desarrollo
-- ============================================

-- Ver todos los usuarios
-- SELECT id, username, role, daily_message_limit, created_at FROM users WHERE deleted_at IS NULL;

-- Ver configuraciones de plataformas
-- SELECT platform_type, is_active, created_at FROM platform_configurations;

-- Ver mensajes con entregas (join)
-- SELECT 
--     m.id,
--     u.username,
--     m.content,
--     md.platform_type,
--     md.status,
--     md.destination,
--     m.created_at
-- FROM messages m
-- JOIN users u ON m.user_id = u.id
-- LEFT JOIN message_deliveries md ON m.message_id = md.message_id
-- WHERE m.deleted_at IS NULL
-- ORDER BY m.created_at DESC;

-- Ver contadores diarios
-- SELECT 
--     u.username,
--     dmc.date,
--     dmc.count,
--     u.daily_message_limit,
--     (u.daily_message_limit - dmc.count) as remaining
-- FROM daily_message_counts dmc
-- JOIN users u ON dmc.user_id = u.id
-- WHERE dmc.date = CURDATE();

-- Métricas generales (para admin)
-- SELECT 
--     u.username,
--     COUNT(m.id) as total_messages,
--     COALESCE(dmc.count, 0) as messages_today,
--     (u.daily_message_limit - COALESCE(dmc.count, 0)) as remaining_today
-- FROM users u
-- LEFT JOIN messages m ON u.id = m.user_id AND m.deleted_at IS NULL
-- LEFT JOIN daily_message_counts dmc ON u.id = dmc.user_id AND dmc.date = CURDATE()
-- WHERE u.deleted_at IS NULL
-- GROUP BY u.id, u.username, u.daily_message_limit, dmc.count;