package com.notificationhub.repository;

import com.notificationhub.entity.Message;
import com.notificationhub.entity.User;
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
     * Busca mensajes por usuario
     */
    List<Message> findMessagesByUser(User user);

    /**
     * Busca mensajes por usuario en un rango de fechas
     */
    @Query("SELECT m FROM Message m WHERE m.user = :user AND m.createdAt BETWEEN :from AND :to ORDER BY m.createdAt DESC")
    List<Message> findByUserAndDateRange(@Param("user") User user,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

}
