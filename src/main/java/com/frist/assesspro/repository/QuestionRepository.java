package com.frist.assesspro.repository;

import com.frist.assesspro.dto.QuestionDTO;
import com.frist.assesspro.entity.Question;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface QuestionRepository extends JpaRepository<Question,Long> {

    @EntityGraph(value = "Question.withAnswers", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT q FROM Question q WHERE q.test.id = :testId ORDER BY q.orderIndex")
    List<Question> findByTestIdOrderByOrderIndex(@Param("testId") Long testId);

    @EntityGraph(value = "Question.withTestAndAnswers", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Question> findById(Long id);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.test.id = :testId")
    int countByTestId(@Param("testId") Long testId);

    @Query("SELECT q FROM Question q " +
            "LEFT JOIN FETCH q.answerOptions " +
            "WHERE q.test.id = :testId " +
            "ORDER BY q.orderIndex")
    List<Question> findQuestionsWithAnswersByTestId(@Param("testId") Long testId);

    @Query("SELECT new com.frist.assesspro.dto.QuestionDTO(" +
            "q.id, q.text, q.orderIndex, " +
            "new com.frist.assesspro.dto.AnswerOptionDTO(a.id, a.text, a.isCorrect)) " +
            "FROM Question q " +
            "LEFT JOIN q.answerOptions a " +
            "WHERE q.test.id = :testId " +
            "ORDER BY q.orderIndex")
    List<QuestionDTO> findQuestionDTOsByTestId(@Param("testId") Long testId);

}