package com.frist.assesspro.repository;

import com.frist.assesspro.dto.test.TestHistoryDTO;
import com.frist.assesspro.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TestAttemptRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    @Autowired
    private EntityManager entityManager;

    private User creator;
    private User tester;
    private Test test;
    private Question question;
    private AnswerOption answer;
    private TestAttempt attempt;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testRepository.deleteAll();
        questionRepository.deleteAll();
        answerOptionRepository.deleteAll();
        testAttemptRepository.deleteAll();

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

        question = new Question();
        question.setText("Вопрос");
        question.setTest(test);
        questionRepository.save(question);

        answer = new AnswerOption();
        answer.setText("Ответ");
        answer.setIsCorrect(true);
        answer.setQuestion(question);
        answerOptionRepository.save(answer);

        attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUser(tester);
        attempt.setStartTime(LocalDateTime.now().minusHours(2));
        attempt.setEndTime(LocalDateTime.now().minusHours(1));
        attempt.setTotalScore(1);
        attempt.setStatus(TestAttempt.AttemptStatus.COMPLETED);
        testAttemptRepository.save(attempt);
    }

    @org.junit.jupiter.api.Test
    void findByTestIdAndUserIdAndStatus_ShouldReturnAttempt() {
        Optional<TestAttempt> found = testAttemptRepository.findByTestIdAndUserIdAndStatus(
                test.getId(), tester.getId(), TestAttempt.AttemptStatus.COMPLETED);
        assertThat(found).isPresent();
    }

    @org.junit.jupiter.api.Test
    void findByUserId_ShouldReturnAttempts() {
        List<TestAttempt> attempts = testAttemptRepository.findByUserId(tester.getId());
        assertThat(attempts).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void findTestHistoryDTOsByUserId_ShouldReturnDTOs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestHistoryDTO> page = testAttemptRepository.findTestHistoryDTOsByUserId(tester.getId(), pageable);
        assertThat(page.getContent()).hasSize(1);
        TestHistoryDTO dto = page.getContent().get(0);
        assertThat(dto.getAttemptId()).isEqualTo(attempt.getId());
        assertThat(dto.getTestTitle()).isEqualTo("Тест");
    }

    @org.junit.jupiter.api.Test
    void countByUserId_ShouldReturnCount() {
        assertThat(testAttemptRepository.countByUserId(tester.getId())).isEqualTo(1L);
    }

    @org.junit.jupiter.api.Test
    void countByUserIdAndStatus_ShouldReturnCount() {
        long count = testAttemptRepository.countByUserIdAndStatus(tester.getId(), TestAttempt.AttemptStatus.COMPLETED);
        assertThat(count).isEqualTo(1L);
    }

    @org.junit.jupiter.api.Test
    void findAverageScoreByUserId_ShouldReturnAverage() {
        Double avg = testAttemptRepository.findAverageScoreByUserId(tester.getId());
        assertThat(avg).isEqualTo(1.0);
    }

    @org.junit.jupiter.api.Test
    void updateTotalScore_ShouldUpdate() {
        entityManager.flush();
        entityManager.clear();
        testAttemptRepository.updateTotalScore(attempt.getId(), 5);
        TestAttempt updated = testAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(updated.getTotalScore()).isEqualTo(5);
    }

    @org.junit.jupiter.api.Test
    void findBestScoreByUserId_ShouldReturnMax() {
        Integer best = testAttemptRepository.findBestScoreByUserId(tester.getId());
        assertThat(best).isEqualTo(1);
    }

    @org.junit.jupiter.api.Test
    void findRecentAttemptsForCreator_ShouldReturnAttempts() {
        List<TestAttempt> recent = testAttemptRepository.findRecentAttemptsForCreator("creator");
        assertThat(recent).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void findByTestId_ShouldReturnAttempts() {
        List<TestAttempt> attempts = testAttemptRepository.findByTestId(test.getId());
        assertThat(attempts).hasSize(1);
    }

//    @org.junit.jupiter.api.Test
//    void findAggregatedStatsByCreator_ShouldReturnStats() {
//        List<Object[]> stats = testAttemptRepository.findAggregatedStatsByCreator(creator.getId());
//        assertThat(stats).isNotEmpty();
//        Object[] row = stats.get(0);
//        assertThat(((Number) row[0]).longValue()).isEqualTo(1L); // totalAttempts
//        assertThat(((Number) row[1]).longValue()).isEqualTo(1L); // completedTests
//    }

    @org.junit.jupiter.api.Test
    void findByTestIdWithUser_ShouldFetchUser() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestAttempt> page = testAttemptRepository.findByTestIdWithUser(test.getId(), pageable);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUser()).isNotNull();
    }

    @org.junit.jupiter.api.Test
    void searchByTestIdWithUser_ShouldFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestAttempt> page = testAttemptRepository.searchByTestIdWithUser(test.getId(), "tester", pageable);
        assertThat(page.getContent()).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void countByTestIdAndUserIdAndStatus_ShouldReturnCount() {
        long count = testAttemptRepository.countByTestIdAndUserIdAndStatus(
                test.getId(), tester.getId(), TestAttempt.AttemptStatus.COMPLETED);
        assertThat(count).isEqualTo(1L);
    }

    @org.junit.jupiter.api.Test
    void findAttemptsByTestIdWithAllData_ShouldFetchAll() {
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestAttempt> page = testAttemptRepository.findAttemptsByTestIdWithAllData(test.getId(), pageable);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTest().getQuestions()).isNotEmpty();
    }
}