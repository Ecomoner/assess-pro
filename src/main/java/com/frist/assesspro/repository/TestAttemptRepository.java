package com.frist.assesspro.repository;

import com.frist.assesspro.dto.test.TestHistoryDTO;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    @EntityGraph(value = "TestAttempt.withTestAndUser", type = EntityGraph.EntityGraphType.LOAD)
    List<TestAttempt> findByUserIdOrderByStartTimeDesc(Long userId);

    @EntityGraph(value = "TestAttempt.withTest", type = EntityGraph.EntityGraphType.LOAD)
    Optional<TestAttempt> findByTestIdAndUserIdAndStatus(
            Long testId, Long userId, TestAttempt.AttemptStatus status);

    @EntityGraph(value = "TestAttempt.withTest", type = EntityGraph.EntityGraphType.LOAD)
    Page<TestAttempt> findByUserIdAndStatusOrderByStartTimeDesc(
            Long userId, TestAttempt.AttemptStatus status, Pageable pageable);

    @EntityGraph(value = "TestAttempt.withTest", type = EntityGraph.EntityGraphType.LOAD)
    Page<TestAttempt> findByUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);

    List<TestAttempt> findByUserId(Long userId);

    // DTO проекция для истории тестов
    @Query("SELECT new com.frist.assesspro.dto.test.TestHistoryDTO(" +
            "ta.id, t.id, t.title, ta.startTime, ta.endTime, ta.status, ta.totalScore, " +
            "COUNT(q.id)) " +
            "FROM TestAttempt ta " +
            "JOIN ta.test t " +
            "LEFT JOIN t.questions q " +
            "WHERE ta.user.id = :userId " +
            "GROUP BY ta.id, t.id, t.title, ta.startTime, ta.endTime, ta.status, ta.totalScore " +
            "ORDER BY ta.startTime DESC")
    Page<TestHistoryDTO> findTestHistoryDTOsByUserId(@Param("userId") Long userId, Pageable pageable);

    // Метод для DashboardService
    @EntityGraph(value = "TestAttempt.withTest", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT ta FROM TestAttempt ta WHERE ta.test.createdBy = :creator")
    List<TestAttempt> findByTestCreatedBy(@Param("creator") User creator);

    // Оптимизированные счетчики и агрегации
    @Query("SELECT COUNT(ta) FROM TestAttempt ta WHERE ta.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(ta) FROM TestAttempt ta WHERE ta.user.id = :userId AND ta.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId,
                                @Param("status") TestAttempt.AttemptStatus status);

    @Query("SELECT COALESCE(AVG(ta.totalScore), 0) FROM TestAttempt ta " +
            "WHERE ta.user.id = :userId AND ta.status = 'COMPLETED'")
    Double findAverageScoreByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE TestAttempt ta SET ta.totalScore = :score WHERE ta.id = :attemptId")
    void updateTotalScore(@Param("attemptId") Long attemptId, @Param("score") Integer score);

    // Метод для лучшего балла
    @Query("SELECT COALESCE(MAX(ta.totalScore), 0) FROM TestAttempt ta " +
            "WHERE ta.user.id = :userId AND ta.status = 'COMPLETED'")
    Integer findBestScoreByUserId(@Param("userId") Long userId);

    /**
     * DTO проекция для списка тестировщиков теста
     */
    @EntityGraph(attributePaths = {"user", "test"})
    @Query("SELECT ta FROM TestAttempt ta " +
            "WHERE ta.test.id = :testId " +
            "ORDER BY ta.startTime DESC")
    Page<TestAttempt> findAttemptsByTestId(@Param("testId") Long testId, Pageable pageable);

    /**
     * Поиск тестировщиков по имени
     */
    @EntityGraph(attributePaths = {"user", "test", "test.questions"})
    @Query("SELECT ta FROM TestAttempt ta " +
            "WHERE ta.test.id = :testId " +
            "AND LOWER(ta.user.username) LIKE LOWER(CONCAT('%', :testerName, '%')) " +
            "ORDER BY ta.startTime DESC")
    List<TestAttempt> searchAttemptsByTestAndUsername(
            @Param("testId") Long testId,
            @Param("testerName") String testerName);

    /**
     * Получение последних попыток по тестам создателя
     */
    @EntityGraph(attributePaths = {"user", "test", "test.questions"})
    @Query("SELECT ta FROM TestAttempt ta " +
            "WHERE ta.test.createdBy.username = :creatorUsername " +
            "ORDER BY ta.startTime DESC")
    List<TestAttempt> findRecentAttemptsForCreator(
            @Param("creatorUsername") String creatorUsername);

    @Query("SELECT COUNT(DISTINCT ta.user.id) FROM TestAttempt ta " +
            "WHERE ta.test.createdBy.username = :creatorUsername")
    long countDistinctTestersByCreator(@Param("creatorUsername") String creatorUsername);

    @Query("SELECT ta FROM TestAttempt ta WHERE ta.test.id = :testId")
    List<TestAttempt> findByTestId(@Param("testId") Long testId);

    @Query(value = "SELECT " +
            "COUNT(*) as totalAttempts, " +
            "COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completedTests, " +
            "COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as inProgressTests, " +
            "COALESCE(SUM(EXTRACT(EPOCH FROM (end_time - start_time))/60), 0) as totalMinutes, " +
            "COALESCE(MAX(total_score), 0) as bestScore " +
            "FROM assess_pro_db.test_attempts ta " +
            "JOIN assess_pro_db.tests t ON ta.test_id = t.id " +
            "WHERE t.created_by = :creatorId",
            nativeQuery = true)
    List<Object[]> findAggregatedStatsByCreator(@Param("creatorId") Long creatorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM TestAttempt a WHERE a.id = :id")
    Optional<TestAttempt> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT ta FROM TestAttempt ta " +
            "JOIN FETCH ta.user " +
            "JOIN FETCH ta.test " +
            "LEFT JOIN FETCH ta.test.questions " +  // ← ДОБАВЛЕНО
            "WHERE ta.test.id = :testId " +
            "ORDER BY ta.startTime DESC")
    Page<TestAttempt> findByTestIdWithUser(@Param("testId") Long testId, Pageable pageable);

    @Query("SELECT ta FROM TestAttempt ta " +
            "JOIN FETCH ta.user " +
            "WHERE ta.test.id = :testId " +
            "AND (LOWER(ta.user.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(ta.user.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(ta.user.firstName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY ta.startTime DESC")
    Page<TestAttempt> searchByTestIdWithUser(@Param("testId") Long testId,
                                             @Param("search") String search,
                                             Pageable pageable);
    long countByTestIdAndUserIdAndStatus(Long testId, Long userId, TestAttempt.AttemptStatus status);

    @Query("SELECT ta FROM TestAttempt ta " +
            "JOIN FETCH ta.user " +
            "JOIN FETCH ta.test " +
            "LEFT JOIN FETCH ta.test.questions " +
            "WHERE ta.test.id = :testId " +
            "ORDER BY ta.startTime DESC")
    Page<TestAttempt> findAttemptsByTestIdWithAllData(@Param("testId") Long testId, Pageable pageable);

    @Query("SELECT ta.user.id, COUNT(ta), COALESCE(AVG(ta.totalScore), 0) " +
            "FROM TestAttempt ta " +
            "WHERE ta.user.role = 'ROLE_TESTER' AND ta.status = 'COMPLETED' " +
            "GROUP BY ta.user.id")
    List<Object[]> getTesterStats();

}