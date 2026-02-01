package com.frist.assesspro.repository;

import com.frist.assesspro.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAnswerRepository extends JpaRepository<UserAnswer,Long> {

    Optional<UserAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    List<UserAnswer> findByAttemptId(Long attemptId);

    @Query("SELECT SUM(ua.pointsEarned) FROM UserAnswer ua WHERE ua.attempt.id = :attemptId")
    Integer sumPointsEarnedByAttemptId(@Param("attemptId") Long attemptId);
}
