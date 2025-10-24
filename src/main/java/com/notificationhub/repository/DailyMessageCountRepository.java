package com.notificationhub.repository;

import com.notificationhub.entity.DailyMessageCount;
import com.notificationhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyMessageCountRepository extends JpaRepository<DailyMessageCount, Long> {
    /**
     * Busca el contador de un usuario para una fecha específica
     */
    Optional<DailyMessageCount> findByUserAndDate(User user, LocalDate date);

    /**
     * Verifica si existe un contador para un usuario y fecha
     */
    boolean existsByUserAndDate(User user, LocalDate date);
}
