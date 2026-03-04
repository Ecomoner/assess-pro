package com.frist.assesspro.repository;

import com.frist.assesspro.entity.RetryCooldownException;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RetryCooldownExceptionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RetryCooldownExceptionRepository exceptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestRepository testRepository;

    private User creator;
    private User tester;
    private Test test;
    private RetryCooldownException exception;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testRepository.deleteAll();
        exceptionRepository.deleteAll();

        creator = new User();
        creator.setUsername("creator");
        creator.setPassword("pass");
        creator.setRole(User.Roles.CREATOR);
        userRepository.save(creator);

        tester = new User();
        tester.setUsername("tester");
        tester.setPassword("pass");
        tester.setRole(User.Roles.TESTER);
        userRepository.save(tester);

        test = new Test();
        test.setTitle("Тест");
        test.setCreatedBy(creator);
        testRepository.save(test);

        exception = new RetryCooldownException();
        exception.setTest(test);
        exception.setUser(tester);
        exception.setCreatedBy(creator);
        exception.setIsPermanent(true);
        exception.setReason("Причина");
        exceptionRepository.save(exception);
    }

    @org.junit.jupiter.api.Test
    void hasActiveException_ShouldReturnTrueForPermanent() {
        LocalDateTime now = LocalDateTime.now();
        boolean active = exceptionRepository.hasActiveException(test, tester, now);
        assertThat(active).isTrue();
    }

    @org.junit.jupiter.api.Test
    void hasActiveException_ShouldReturnFalseForExpired() {
        exception.setIsPermanent(false);
        exception.setExpiresAt(LocalDateTime.now().minusHours(1));
        exceptionRepository.save(exception);

        LocalDateTime now = LocalDateTime.now();
        boolean active = exceptionRepository.hasActiveException(test, tester, now);
        assertThat(active).isFalse();
    }

    @org.junit.jupiter.api.Test
    void deleteByTestAndUser_ShouldRemoveException() {
        exceptionRepository.deleteByTestAndUser(test, tester);
        LocalDateTime now = LocalDateTime.now();
        boolean active = exceptionRepository.hasActiveException(test, tester, now);
        assertThat(active).isFalse();
    }

    @org.junit.jupiter.api.Test
    void findUserIdsWithExceptions_ShouldReturnSetOfUserIds() {
        Set<Long> userIds = exceptionRepository.findUserIdsWithExceptions(test.getId());
        assertThat(userIds).containsExactly(tester.getId());
    }
}
