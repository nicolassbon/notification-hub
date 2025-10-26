package com.notificationhub.repository;

import com.notificationhub.entity.Message;
import com.notificationhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByUserOrderByCreatedAtDesc(User user);

    List<Message> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime from, LocalDateTime to);

    long countByUser(User user);
}
