package com.frist.assesspro.repository;

import com.frist.assesspro.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnswerRepository extends JpaRepository<UserAnswer,Long> {
}
