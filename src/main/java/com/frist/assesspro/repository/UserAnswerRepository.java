package com.frist.assesspro.repository;

import com.frist.assesspro.entity.UserAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserAnswerRepository extends JpaRepository<UserAnswer,Long> {

    Optional<UserAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    List<UserAnswer> findByAttemptId(Long attemptId);

    @Query("SELECT SUM(ua.pointsEarned) FROM UserAnswer ua WHERE ua.attempt.id = :attemptId")
    Integer sumPointsEarnedByAttemptId(@Param("attemptId") Long attemptId);

    /**
     * Загружаем ответы пользователя с вопросами и вариантами ответов
     */
    @EntityGraph(attributePaths = {
            "question",
            "question.answerOptions",
            "chosenAnswerOption"
    })
    @Query("SELECT ua FROM UserAnswer ua " +
            "WHERE ua.attempt.id = :attemptId " +
            "ORDER BY ua.question.orderIndex")
    List<UserAnswer> findByAttemptIdWithDetails(@Param("attemptId") Long attemptId);

    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.attempt.id = :attemptId")
    long countByAttemptId(@Param("attemptId") Long attemptId);

    @Modifying
    @Query(value = "INSERT INTO assess_pro_db.user_answers (attempt_id, question_id, chosen_answer_option_id, is_correct, points_earned) " +
            "VALUES (:attemptId, :questionId, :answerOptionId, :isCorrect, :points) " +
            "ON CONFLICT (attempt_id, question_id) DO UPDATE SET " +
            "chosen_answer_option_id = EXCLUDED.chosen_answer_option_id, " +
            "is_correct = EXCLUDED.is_correct, " +
            "points_earned = EXCLUDED.points_earned",
            nativeQuery = true)
    void upsertAnswer(@Param("attemptId") Long attemptId,
                      @Param("questionId") Long questionId,
                      @Param("answerOptionId") Long answerOptionId,
                      @Param("isCorrect") Boolean isCorrect,
                      @Param("points") Integer points);

    @Query("SELECT ua.attempt.id, COUNT(ua) FROM UserAnswer ua " +
            "WHERE ua.attempt.id IN :attemptIds " +
            "GROUP BY ua.attempt.id")
    List<Object[]> countByAttemptIds(@Param("attemptIds") List<Long> attemptIds);
}
