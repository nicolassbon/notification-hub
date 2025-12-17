package com.notificationhub.repository;

import com.notificationhub.entity.Message;
import com.notificationhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.deliveries WHERE m.user = :user ORDER BY m.createdAt DESC")
    List<Message> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.deliveries WHERE m.user = :user AND m.createdAt BETWEEN :from AND :to ORDER BY m.createdAt DESC")
    List<Message> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime from, LocalDateTime to);

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.deliveries")
    List<Message> findAllWithDeliveries();

    long countByUser(User user);
}
