package com.notificationhub.repository;

import com.notificationhub.entity.PlatformConfiguration;
import com.notificationhub.enums.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformConfigurationRepository extends JpaRepository<PlatformConfiguration, Long> {
    /**
     * Busca configuración por tipo de plataforma
     */
    Optional<PlatformConfiguration> findByPlatformType(PlatformType platformType);

    /**
     * Busca configuración activa por tipo de plataforma
     */
    @Query("SELECT pc FROM PlatformConfiguration pc WHERE pc.platformType = :platformType AND pc.isActive = true")
    Optional<PlatformConfiguration> findByPlatformTypeAndActive(PlatformType platformType);

    /**
     * Obtiene todas las configuraciones activas
     */
    @Query("SELECT pc FROM PlatformConfiguration pc WHERE pc.isActive = true")
    List<PlatformConfiguration> findAllActive();

    /**
     * Verifica si existe configuración para una plataforma
     */
    boolean existsByPlatformType(PlatformType platformType);
}
