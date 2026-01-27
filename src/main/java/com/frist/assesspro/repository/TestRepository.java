package com.frist.assesspro.repository;

import com.frist.assesspro.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<Test,Long> {
}
