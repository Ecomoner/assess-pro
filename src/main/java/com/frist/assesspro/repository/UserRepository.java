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

    /**
     * 🔥 НОВОЕ: Поиск пользователей, зарегистрированных после указанной даты
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findByCreatedAtAfter(@Param("date") LocalDateTime date);

    /**
     * 🔥 НОВОЕ: Подсчет всех пользователей
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();

    /**
     * 🔥 НОВОЕ: Подсчет активных/неактивных пользователей
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = :isActive")
    long countByIsActive(@Param("isActive") boolean isActive);

    /**
     * 🔥 НОВОЕ: Получение всех пользователей с пагинацией и фильтрацией
     */
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

    /**
     * 🔥 НОВОЕ: Получение всех создателей для фильтра
     */
    @Query("SELECT u FROM User u WHERE u.role = 'ROLE_CREATOR' ORDER BY u.username")
    List<User> findAllCreators();

    // Или с пагинацией, если много создателей
    @Query("SELECT u FROM User u WHERE u.role = 'ROLE_CREATOR' ORDER BY u.username")
    Page<User> findAllCreators(Pageable pageable);


}
