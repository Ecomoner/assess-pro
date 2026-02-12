package com.frist.assesspro.repository;

import com.frist.assesspro.entity.RetryCooldownException;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RetryCooldownExceptionRepository extends JpaRepository<RetryCooldownException, Long> {

    @EntityGraph(attributePaths = {"test", "user", "createdBy"})
    Optional<RetryCooldownException> findByTestAndUser(Test test, User user);

    @EntityGraph(attributePaths = {"test", "user", "createdBy"})
    List<RetryCooldownException> findByTest(Test test);

    @EntityGraph(attributePaths = {"test", "user", "createdBy"})
    List<RetryCooldownException> findByUser(User user);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM RetryCooldownException r " +
            "WHERE r.test = :test AND r.user = :user " +
            "AND (r.isPermanent = true OR r.expiresAt > :now)")
    boolean hasActiveException(@Param("test") Test test,
                               @Param("user") User user,
                               @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM RetryCooldownException r WHERE r.test = :test AND r.user = :user")
    void deleteByTestAndUser(@Param("test") Test test, @Param("user") User user);
}