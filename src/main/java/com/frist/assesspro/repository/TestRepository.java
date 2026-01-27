package com.frist.assesspro.repository;

import com.frist.assesspro.model.Question;
import com.frist.assesspro.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<Test,Long> {
}
