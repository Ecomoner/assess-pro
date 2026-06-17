package com.frist.assesspro.repository;

import com.frist.assesspro.dto.admin.AppStatisticsCountsDTO;
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

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ROLE_TESTER'")
    long countAllTesters();


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


    @Query("SELECT new com.frist.assesspro.dto.admin.AppStatisticsCountsDTO(" +
            "COUNT(u), " +
            "SUM(CASE WHEN u.role = 'ROLE_ADMIN' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN u.role = 'ROLE_CREATOR' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN u.role = 'ROLE_TESTER' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN u.isProfileComplete = false THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN u.isActive = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN u.isActive = false THEN 1 ELSE 0 END), " +
            "(SELECT COUNT(t) FROM Test t), " +
            "(SELECT COUNT(t) FROM Test t WHERE t.isPublished = true), " +
            "(SELECT COUNT(t) FROM Test t WHERE t.isPublished = false), " +
            "(SELECT COUNT(q) FROM Question q), " +
            "(SELECT COUNT(c) FROM Category c), " +
            "(SELECT COUNT(ta) FROM TestAttempt ta), " +
            "(SELECT COUNT(ta) FROM TestAttempt ta WHERE ta.status = 'COMPLETED'), " +
            "(SELECT COUNT(ta) FROM TestAttempt ta WHERE ta.status = 'IN_PROGRESS') " +
            ") FROM User u")
    AppStatisticsCountsDTO getAppStatisticsCounts();

    long countByProjectId(Long id);

    @Query("SELECT COUNT(u) FROM User u WHERE u.project.id = :projectId")
    long countUsersByProjectId(@Param("projectId") Long projectId);

    List<User> findByProjectId(Long projectId);

}