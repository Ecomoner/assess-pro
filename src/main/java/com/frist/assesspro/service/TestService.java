package com.frist.assesspro.service;

import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.entity.AnswerOption;
import com.frist.assesspro.entity.Question;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.AnswerOptionRepository;
import com.frist.assesspro.repository.QuestionRepository;
import com.frist.assesspro.repository.TestRepository;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserRepository userRepository;


    /**
     * Создание нового теста
     */
    @Transactional
    public Test createdTest(Test test, String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        test.setCreatedBy(user);
        test.setIsPublished(false);

        Test savedTest = testRepository.save(test);

        if (test.getQuestions() != null){
            for (Question question: test.getQuestions()){
                question.setTest(savedTest);
                Question savedQuestion = questionRepository.save(question);

                if (question.getAnswerOptions() != null){
                    for (AnswerOption answerOption: question.getAnswerOptions()){
                        answerOption.setQuestion(savedQuestion);
                        answerOptionRepository.save(answerOption);
                    }
                }
            }
        }
        log.info("Создан новый тест '{}' (ID: {}) пользователем {}",
                test.getTitle(),savedTest.getId(),username);
        return savedTest;
    }

    /**
     * Получение всех тестов создателя
     */
    @Transactional(readOnly = true)
    public List<TestDTO> getTestsByCreator(String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Test> tests = testRepository.findByCreatedByWithQuestions(creator);

        return tests.stream()
                .map(test -> new TestDTO(
                        test.getId(),
                        test.getTitle(),
                        test.getDescription(),
                        test.getIsPublished() != null && test.getIsPublished(),
                        test.getQuestions().size(),
                        test.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
    /**
     * Получение теста по ID с проверкой прав
     */
    @Transactional(readOnly = true)
    public Optional<Test> getTestById(Long testId,String username){
        return testRepository.findById(testId)
                .filter(test -> test.getCreatedBy().getUsername().equals(username));
    }

    /**
     * Обновление теста
     */
    @Transactional(readOnly = true)
    public Test updateTest(Long testId,Test updateTest,String username){
        Test existingTest = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!existingTest.getCreatedBy().getUsername().equals(username)){
            throw new RuntimeException("Нет прав для редактирования теста");
        }

        existingTest.setTitle(updateTest.getTitle());
        existingTest.setDescription(updateTest.getDescription());
        existingTest.setTimeLimitMinutes(updateTest.getTimeLimitMinutes());

        log.info("Обновлен тест '{}' (ID: {}) пользователем {}",
                existingTest.getTitle(), testId, username);

        return testRepository.save(existingTest);
    }

    /**
     * Публикация/снятие с публикации теста
     */
    @Transactional
    public Test switchPublishStatus(Long testId,String username,boolean publish){
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getUsername().equals(username)){
            throw new RuntimeException("Нет прав для изменения статуса теста");
        }

        test.setIsPublished(publish);
        log.info("Тест '{}' (ID: {}) {} пользователем {}",
                test.getTitle(), testId,
                publish ? "опубликован" : "снят с публикации",
                username);

        return testRepository.save(test);
    }

    /**
     * Удаление теста
     */

    @Transactional
    public void deleteTest(Long testId, String username) {
        log.info("Удаление теста ID: {} пользователем: {}", testId, username);

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для удаления этого теста");
        }


        if (test.getAttempts() != null && !test.getAttempts().isEmpty()) {
            throw new RuntimeException("Нельзя удалить тест, у которого есть попытки прохождения");
        }


        if (test.getQuestions() != null) {
            for (Question question : test.getQuestions()) {
                if (question.getAnswerOptions() != null) {
                    answerOptionRepository.deleteAll(question.getAnswerOptions());
                }
            }
            questionRepository.deleteAll(test.getQuestions());
        }

        testRepository.delete(test);

        log.info("Тест '{}' (ID: {}) успешно удален", test.getTitle(), testId);
    }


}
