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
    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.deliveries")
    List<Message> findAllWithDeliveries();

    long countByUser(User user);
}
