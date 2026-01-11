package com.notificationhub.repository;

import com.notificationhub.entity.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Cacheable(value = "users", key = "#username")
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
