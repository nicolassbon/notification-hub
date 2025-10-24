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
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsername(String username);

    /**
     * Verifica si existe un usuario con ese username (solo activos)
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    boolean existsByUsername(String username);

    /**
     * Obtiene todos los usuarios activos
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActive();

    /**
     * Busca usuario por ID solo si está activo
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdAndActive(Long id);

}
