package com.frist.assesspro.repository;

import com.frist.assesspro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username")
    boolean existsByUsername(@Param("username") String username);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") String role);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))")
    List<User> findByUsernameContainingIgnoreCase(@Param("username") String username);

    // 🔥 НОВОЕ: Поиск пользователей по роли
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") String role);

    // 🔥 НОВОЕ: Поиск пользователей с незаполненным профилем
    @Query("SELECT u FROM User u WHERE u.isProfileComplete = false")
    List<User> findByProfileNotComplete();

    // 🔥 НОВОЕ: Поиск пользователей по имени/фамилии
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(u.middleName) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<User> searchByName(@Param("term") String term);

}
