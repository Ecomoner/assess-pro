package com.frist.assesspro.repository;

import com.frist.assesspro.model.Question;
import com.frist.assesspro.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestAttemptRepository extends JpaRepository<TestAttempt,Long> {
}
