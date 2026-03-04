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

    @EntityGraph(value = "TestAttempt.withTest", type = EntityGraph.EntityGraphType.LOAD)
    Optional<TestAttempt> findByTestIdAndUserIdAndStatus(
            Long testId, Long userId, TestAttempt.AttemptStatus status);

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

    @Query("SELECT COALESCE(MAX(ta.totalScore), 0) FROM TestAttempt ta " +
            "WHERE ta.user.id = :userId AND ta.status = 'COMPLETED'")
    Integer findBestScoreByUserId(@Param("userId") Long userId);

    /**
     * Получение последних попыток по тестам создателя
     */
    @EntityGraph(attributePaths = {"user", "test", "test.questions"})
    @Query("SELECT ta FROM TestAttempt ta " +
            "WHERE ta.test.createdBy.username = :creatorUsername " +
            "ORDER BY ta.startTime DESC")
    List<TestAttempt> findRecentAttemptsForCreator(
            @Param("creatorUsername") String creatorUsername);

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

    @Query("SELECT ta FROM TestAttempt ta " +
            "JOIN FETCH ta.user " +
            "JOIN FETCH ta.test " +
            "LEFT JOIN FETCH ta.test.questions " +
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


}