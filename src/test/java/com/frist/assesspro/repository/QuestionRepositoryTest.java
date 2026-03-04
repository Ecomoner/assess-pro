package com.frist.assesspro.repository;

import com.frist.assesspro.dto.AnswerOptionDTO;
import com.frist.assesspro.dto.QuestionDTO;
import com.frist.assesspro.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User creator;
    private Test test;
    private Question question;
    private AnswerOption answer1, answer2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testRepository.deleteAll();
        questionRepository.deleteAll();
        answerOptionRepository.deleteAll();

        creator = new User();
        creator.setUsername("creator");
        creator.setPassword("pass");
        creator.setRole(User.Roles.CREATOR);
        userRepository.save(creator);

        test = new Test();
        test.setTitle("Тест");
        test.setCreatedBy(creator);
        testRepository.save(test);

        question = new Question();
        question.setText("2+2=?");
        question.setOrderIndex(1);
        question.setTest(test);
        questionRepository.save(question);

        answer1 = new AnswerOption();
        answer1.setText("4");
        answer1.setIsCorrect(true);
        answer1.setQuestion(question);
        answerOptionRepository.save(answer1);

        answer2 = new AnswerOption();
        answer2.setText("5");
        answer2.setIsCorrect(false);
        answer2.setQuestion(question);
        answerOptionRepository.save(answer2);
    }

    @org.junit.jupiter.api.Test
    void findByTestIdOrderByOrderIndex_ShouldReturnQuestionsOrdered() {
        List<Question> questions = questionRepository.findByTestIdOrderByOrderIndex(test.getId());
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getText()).isEqualTo("2+2=?");
    }

    @org.junit.jupiter.api.Test
    void findById_WithEntityGraph_ShouldLoadAnswers() {
        entityManager.flush();
        entityManager.clear();

        Optional<Question> found = questionRepository.findById(question.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAnswerOptions()).hasSize(2);
    }

    @org.junit.jupiter.api.Test
    void findQuestionsWithAnswersByTestId_ShouldFetchAnswers() {
        entityManager.flush();
        entityManager.clear();

        // when
        List<Question> questions = questionRepository.findQuestionsWithAnswersByTestId(test.getId());

        // then
        assertThat(questions).hasSize(1);
        Question foundQuestion = questions.get(0);
        assertThat(foundQuestion.getAnswerOptions()).hasSize(2);
    }


}