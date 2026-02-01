package com.frist.assesspro.repository;

import com.frist.assesspro.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption,Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM AnswerOption a WHERE a.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}
