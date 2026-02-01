package com.frist.assesspro.repository;

import com.frist.assesspro.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question,Long> {

    @Query("SELECT q FROM Question q WHERE q.test.id = :testId ORDER BY q.orderIndex")
    List<Question> findByTestIdOrderByOrderIndex(@Param("testId") Long testId);
}
