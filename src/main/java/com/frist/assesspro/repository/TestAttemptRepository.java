package com.frist.assesspro.repository;

import com.frist.assesspro.dto.statistics.ProjectAggregatedStatsDTO;
import com.frist.assesspro.dto.statistics.TesterProjectStatsDTO;
import com.frist.assesspro.dto.statistics.TesterProjectStatsProjection;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    @EntityGraph(value = "TestAttempt.withTest", type = EntityGraph.EntityGraphType.LOAD)
    Optional<TestAttempt> findByTestIdAndUserIdAndStatus(
            Long testId, Long userId, TestAttempt.AttemptStatus status);

    List<TestAttempt> findByUserId(Long userId);

    @Query("SELECT COUNT(ta) FROM TestAttempt ta WHERE ta.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(ta) FROM TestAttempt ta WHERE ta.user.id = :userId AND ta.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId,
                                @Param("status") TestAttempt.AttemptStatus status);

    @Query("SELECT COALESCE(AVG(ta.totalScore), 0) FROM TestAttempt ta " +
            "WHERE ta.user.id = :userId AND ta.status = 'COMPLETED'")
    Double findAverageScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(MAX(ta.totalScore), 0) FROM TestAttempt ta " +
            "WHERE ta.user.id = :userId AND ta.status = 'COMPLETED'")
    Integer findBestScoreByUserId(@Param("userId") Long userId);

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
            "WHERE ta.test.id = :testId " +
            "ORDER BY ta.startTime DESC")
    Page<TestAttempt> findAttemptsByTestIdWithUserAndTest(@Param("testId") Long testId, Pageable pageable);

    @Query("SELECT ta FROM TestAttempt ta " +
            "WHERE ta.test.id = :testId AND ta.user.id = :userId AND ta.status = :status " +
            "ORDER BY ta.endTime DESC")
    List<TestAttempt> findLatestByTestIdAndUserIdAndStatus(@Param("testId") Long testId,
                                                           @Param("userId") Long userId,
                                                           @Param("status") TestAttempt.AttemptStatus status);

    @Query("SELECT ta FROM TestAttempt ta WHERE ta.test.id = :testId " +
            "AND (:dateFromIsNull = true OR ta.startTime >= :dateFrom) " +
            "AND (:dateToIsNull = true OR ta.startTime <= :dateTo)")
    List<TestAttempt> findByTestIdAndDateRange(@Param("testId") Long testId,
                                               @Param("dateFromIsNull") boolean dateFromIsNull,
                                               @Param("dateFrom") LocalDateTime dateFrom,
                                               @Param("dateToIsNull") boolean dateToIsNull,
                                               @Param("dateTo") LocalDateTime dateTo);

    @Query(value = "SELECT " +
            "COALESCE(AVG(total_score), 0), " +
            "COALESCE(SUM(EXTRACT(EPOCH FROM (end_time - start_time)) / 60), 0) " +
            "FROM assess_pro_db.test_attempts " +
            "WHERE status = 'COMPLETED' AND end_time IS NOT NULL AND start_time IS NOT NULL",
            nativeQuery = true)
    List<Object[]> findAverageScoreAndTotalMinutes();

    @Query("SELECT COUNT(ta) > 0 FROM TestAttempt ta " +
            "WHERE ta.test.id = :testId " +
            "AND ta.user.id = :userId " +
            "AND ta.status = 'COMPLETED' " +
            "AND ta.totalScore < :requiredScore")
    boolean hasFailedAttempt(@Param("testId") Long testId,
                             @Param("userId") Long userId,
                             @Param("requiredScore") Integer requiredScore);

    @Query("SELECT new com.frist.assesspro.dto.statistics.ProjectAggregatedStatsDTO(" +
            "COUNT(ta.id), COALESCE(AVG(ta.totalScore), 0)) " +
            "FROM TestAttempt ta " +
            "JOIN ta.user u " +
            "WHERE u.project.id = :projectId AND ta.status = 'COMPLETED'")
    ProjectAggregatedStatsDTO getAggregatedStatsByProjectId(@Param("projectId") Long projectId);

    @Query(value = "SELECT u.id AS userId, u.username AS username, " +
            "u.first_name AS firstName, u.last_name AS lastName, " +
            "COUNT(ta.id) AS attemptCount, " +
            "COALESCE(AVG(ta.total_score), 0.0) AS averageScore, " +
            "MAX(ta.start_time) AS lastAttemptDate " +
            "FROM users u " +
            "LEFT JOIN test_attempts ta ON u.id = ta.user_id AND ta.status = 'COMPLETED' " +
            "WHERE u.project_id = :projectId AND u.role = 'ROLE_TESTER' " +
            "GROUP BY u.id, u.username, u.first_name, u.last_name", nativeQuery = true)
    List<TesterProjectStatsProjection> findTesterStatsByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT ta FROM TestAttempt ta " +
            "JOIN FETCH ta.user " +
            "JOIN FETCH ta.test " +
            "WHERE ta.test.id = :testId AND ta.user.id IN :userIds " +
            "ORDER BY ta.startTime DESC")
    Page<TestAttempt> findAttemptsByTestIdAndUserIds(@Param("testId") Long testId,
                                                     @Param("userIds") Set<Long> userIds,
                                                     Pageable pageable);

    @Query("SELECT ta FROM TestAttempt ta JOIN FETCH ta.test JOIN FETCH ta.user WHERE ta.user.id IN :userIds ORDER BY ta.startTime DESC")
    List<TestAttempt> findTopByUserIdsOrderByStartTimeDesc(@Param("userIds") Set<Long> userIds);

    @Query("SELECT a FROM TestAttempt a WHERE a.test.id = :testId AND a.user.id IN :userIds ORDER BY a.startTime DESC")
    List<TestAttempt> findByTestIdAndUserIds(@Param("testId") Long testId, @Param("userIds") Set<Long> userIds);

    boolean existsByUserIdAndStatus(Long userId, TestAttempt.AttemptStatus status);

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

}