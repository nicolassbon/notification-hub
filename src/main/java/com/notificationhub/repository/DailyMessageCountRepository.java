package com.notificationhub.repository;

import com.notificationhub.entity.DailyMessageCount;
import com.notificationhub.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyMessageCountRepository extends JpaRepository<DailyMessageCount, Long> {
    @Modifying
    @Query("UPDATE DailyMessageCount d SET d.count = d.count + 1 WHERE d.user = :user AND d.date = :date")
    int incrementCountAtomic(@Param("user") User user, @Param("date") LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyMessageCount d WHERE d.user = :user AND d.date = :date")
    Optional<DailyMessageCount> findByUserAndDate(@Param("user") User user, @Param("date") LocalDate date);
}
