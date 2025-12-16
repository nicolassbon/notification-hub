package com.notificationhub.repository;

import com.notificationhub.dto.criteria.MessageFilterCriteria;
import com.notificationhub.entity.Message;
import com.notificationhub.entity.MessageDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageDeliveryRepository extends JpaRepository<MessageDelivery, Long> {
  @Query("""
  SELECT DISTINCT md.message FROM MessageDelivery md
  WHERE (:#{#criteria.user()} IS NULL OR md.message.user = :#{#criteria.user()})
  AND (:#{#criteria.from()} IS NULL OR md.message.createdAt >= :#{#criteria.from()})
  AND (:#{#criteria.to()} IS NULL OR md.message.createdAt <= :#{#criteria.to()})
  AND (:#{#criteria.platform()} IS NULL OR md.platformType = :#{#criteria.platform()})
  AND (:#{#criteria.status()} IS NULL OR md.status = :#{#criteria.status()})
  ORDER BY md.message.createdAt DESC
  """)
  List<Message> findMessagesByFilters(MessageFilterCriteria criteria);
}