package com.notificationhub.repository;

import com.notificationhub.entity.Message;
import com.notificationhub.entity.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Cacheable(value = "messageCounts", key = "#user.id")
    long countByUser(User user);
}
