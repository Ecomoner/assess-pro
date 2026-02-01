package com.frist.assesspro.repository;

import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test,Long> {

    List<Test> findByIsPublishedTrue();
    List<Test> findByCreatedByAndIsPublishedTrue(User createdBy);
    List<Test> findByCreatedBy(User createdBy);
    Optional<Test> findByIdAndIsPublishedTrue(Long id);

    @Query("SELECT DISTINCT t FROM Test t " +
            "LEFT JOIN FETCH t.questions " +
            "WHERE t.createdBy = :user")
    List<Test> findByCreatedByWithQuestions(@Param("user") User user);

    @Query("SELECT COUNT(t) FROM Test t WHERE t.createdBy = :user")
    long countByCreator(@Param("user") User user);
}
