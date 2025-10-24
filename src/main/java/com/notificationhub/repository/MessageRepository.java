package com.notificationhub.repository;

import com.notificationhub.entity.Message;
import com.notificationhub.entity.User;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Busca mensajes por usuario (solo mensajes activos)
     */
    @Query("SELECT m FROM Message m WHERE m.user = :user AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Message> findByUserAndActive(User user);

    /**
     * Busca mensaje por ID solo si está activo
     */
    @Query("SELECT m FROM Message m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Message> findByIdAndActive(Long id);

    /**
     * Busca mensajes por usuario en un rango de fechas
     */
    @Query("SELECT m FROM Message m WHERE m.user = :user AND m.createdAt BETWEEN :from AND :to AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Message> findByUserAndDateRange(@Param("user") User user,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /**
     * Filtro por estado (a traves de deliveries)
     */
    @Query("SELECT DISTINCT m FROM Message m JOIN m.deliveries d WHERE m.user = :user AND d.status = :status AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Message> findByUserAndDeliveryStatus(@Param("user") User user, @Param("status") DeliveryStatus status);

    /**
     * Filtro por plataforma (a traves de deliveries)
     */
    @Query("SELECT DISTINCT m FROM Message m JOIN m.deliveries d WHERE m.user = :user AND d.platformType = :platformType AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Message> findByUserAndPlatform(@Param("user") User user, @Param("platformType") PlatformType platformType);

    /**
     * Cuenta mensajes de un usuario en un día específico
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.user = :user AND DATE(m.createdAt) = DATE(:date) AND m.deletedAt IS NULL")
    long countByUserAndDate(@Param("user") User user, @Param("date") LocalDateTime date);

    /**
     * Obtiene todos los mensajes activos (admin)
     */
    @Query("SELECT m FROM Message m WHERE m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Message> findAllActive();
}
