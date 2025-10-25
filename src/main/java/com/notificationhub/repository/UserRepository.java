package com.notificationhub.repository;

import com.notificationhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por username (solo usuarios activos)
     */
    Optional<User> findByUsername(String username);

    /**
     * Verifica si existe un usuario con ese username (solo activos)
     */
    boolean existsByUsername(String username);


}
