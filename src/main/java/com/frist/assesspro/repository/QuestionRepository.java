package com.frist.assesspro.repository;

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


}
