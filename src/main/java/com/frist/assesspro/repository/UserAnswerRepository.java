package com.frist.assesspro.repository;

import com.frist.assesspro.model.Question;
import com.frist.assesspro.model.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnswerRepository extends JpaRepository<UserAnswer,Long> {
}
