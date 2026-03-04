package com.frist.assesspro.repository;

import com.frist.assesspro.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserAnswerRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserAnswerRepository userAnswerRepository;

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
    EntityManager entityManager;

    /* Предварительно созданные сущности */
    private User creator;
    private User tester;
    private Test test;
    private Question question;
    private AnswerOption answer;
    private TestAttempt attempt;
    private UserAnswer userAnswer;


    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testRepository.deleteAll();
        questionRepository.deleteAll();
        answerOptionRepository.deleteAll();
        testAttemptRepository.deleteAll();
        userAnswerRepository.deleteAll();

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
        attempt.setStartTime(java.time.LocalDateTime.now());
        attempt.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
        testAttemptRepository.save(attempt);

        userAnswer = new UserAnswer();
        userAnswer.setAttempt(attempt);
        userAnswer.setQuestion(question);
        userAnswer.setChosenAnswerOption(answer);
        userAnswer.setIsCorrect(true);
        userAnswer.setPointsEarned(1);
        userAnswerRepository.save(userAnswer);
    }

    @org.junit.jupiter.api.Test
    void findByAttemptId_ShouldReturnAnswers() {
        List<UserAnswer> answers = userAnswerRepository.findByAttemptId(attempt.getId());
        assertThat(answers).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void sumPointsEarnedByAttemptId_ShouldReturnSum() {
        Integer sum = userAnswerRepository.sumPointsEarnedByAttemptId(attempt.getId());
        assertThat(sum).isEqualTo(1);
    }

    @org.junit.jupiter.api.Test
    void findByAttemptIdWithDetails_ShouldFetchQuestionAndAnswer() {
        entityManager.flush();
        entityManager.clear();
        List<UserAnswer> answers = userAnswerRepository.findByAttemptIdWithDetails(attempt.getId());
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).getQuestion()).isNotNull();
        assertThat(answers.get(0).getQuestion().getAnswerOptions()).isNotEmpty();
        assertThat(answers.get(0).getChosenAnswerOption()).isNotNull();
    }

    @org.junit.jupiter.api.Test
    void countByAttemptId_ShouldReturnCount() {
        long count = userAnswerRepository.countByAttemptId(attempt.getId());
        assertThat(count).isEqualTo(1L);
    }

//    @org.junit.jupiter.api.Test
//    void upsertAnswer_ShouldInsertOrUpdate() {
//        // тест временно отключён
//    }

    @org.junit.jupiter.api.Test
    void countByAttemptIds_ShouldReturnCounts() {
        List<Object[]> results = userAnswerRepository.countByAttemptIds(List.of(attempt.getId()));
        assertThat(results).hasSize(1);
        Object[] row = results.get(0);
        assertThat(((Number) row[0]).longValue()).isEqualTo(attempt.getId());
        assertThat(((Number) row[1]).longValue()).isEqualTo(1L);
    }
}