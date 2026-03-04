package com.frist.assesspro.repository;

import com.frist.assesspro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username")
    boolean existsByUsername(@Param("username") String username);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") String role);


    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") String role);


    @Query("SELECT u FROM User u WHERE u.isProfileComplete = false")
    List<User> findByProfileNotComplete();


    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findByCreatedAtAfter(@Param("date") LocalDateTime date);


    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();


    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = :isActive")
    long countByIsActive(@Param("isActive") boolean isActive);


    @Query("SELECT u FROM User u WHERE " +
            "(:role IS NULL OR :role = '' OR u.role = :role) AND " +  // 🔥 Добавлена проверка на пустую строку
            "(:isActive IS NULL OR u.isActive = :isActive) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.middleName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.createdAt DESC")
    Page<User> findUsersWithFilters(@Param("role") String role,
                                    @Param("isActive") Boolean isActive,
                                    @Param("search") String search,
                                    Pageable pageable);

    Page<User> findByRole(String role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = 'ROLE_TESTER' AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.middleName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchTesters(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = 'ROLE_CREATOR' ORDER BY u.username")
    List<User> findAllCreators();

}