package com.frist.assesspro.repository;

import com.frist.assesspro.model.AnswerOption;
import com.frist.assesspro.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption,Long> {
}
