package com.frist.assesspro.repository;

import com.frist.assesspro.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt,Long> {

    List<TestAttempt> findByUserIdOrderByStartTimeDesc(Long userId);

    Optional<TestAttempt> findByTestIdAndUserIdAndStatus(
            Long testId, Long userId, TestAttempt.AttemptStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE TestAttempt ta SET ta.totalScore = :score WHERE ta.id = :attemptId")
    void updateTotalScore(@Param("attemptId") Long attemptId, @Param("score") Integer score);
}
