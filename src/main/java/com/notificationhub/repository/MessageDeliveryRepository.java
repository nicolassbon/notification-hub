package com.notificationhub.repository;

import com.notificationhub.entity.Message;
import com.notificationhub.entity.MessageDelivery;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageDeliveryRepository extends JpaRepository<MessageDelivery, Long> {
    /**
     * Busca todas las entregas de un mensaje
     */
    List<MessageDelivery> findByMessage(Message message);

    /**
     * Busca entregas por mensaje y estado
     */
    List<MessageDelivery> findByMessageAndStatus(Message message, DeliveryStatus status);

    /**
     * Busca entregas por mensaje y plataforma
     */
    List<MessageDelivery> findByMessageAndPlatformType(Message message, PlatformType platformType);

    /**
     * Busca entregas pendientes
     */
    List<MessageDelivery> findByStatus(DeliveryStatus status);

    /**
     * Cuenta entregas exitosas por plataforma
     */
    @Query("SELECT COUNT(md) FROM MessageDelivery md WHERE md.platformType = :platform AND md.status = 'SUCCESS'")
    long countSuccessfulByPlatform(@Param("platform") PlatformType platform);

    /**
     * Cuenta entregas fallidas por plataforma
     */
    @Query("SELECT COUNT(md) FROM MessageDelivery md WHERE md.platformType = :platform AND md.status = 'FAILED'")
    long countFailedByPlatform(@Param("platform") PlatformType platform);
}
