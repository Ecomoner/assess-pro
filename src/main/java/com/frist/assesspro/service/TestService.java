package com.frist.assesspro.service;


import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.test.*;
import com.frist.assesspro.entity.*;
import com.frist.assesspro.repository.*;
import com.frist.assesspro.repository.specification.TestSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    /**
     * Создание нового теста
     */
    @Transactional
    public Test createTest(TestDTO testDTO, String username) {

        if (testDTO.getTitle() == null || testDTO.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Название теста обязательно");
        }

        String title = testDTO.getTitle().trim();
        if (title.length() < 3 || title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Название теста должно быть от 3 до " + MAX_TITLE_LENGTH + " символов");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        boolean titleExists = testRepository.findByCreatedBy(user).stream()
                .anyMatch(t -> t.getTitle().equalsIgnoreCase(title));
        if (titleExists) {
            throw new IllegalArgumentException("Тест с таким названием уже существует");
        }

        Test test = new Test();
        test.setTitle(title);
        test.setDescription(testDTO.getDescription());
        test.setCreatedBy(user);
        test.setIsPublished(false);

        test.setTimeLimitMinutes(testDTO.getTimeLimitMinutes() != null ?
                Math.min(testDTO.getTimeLimitMinutes(), 300) : 0);


        if (testDTO.getRetryCooldownDays() != null && testDTO.getRetryCooldownDays() > 0) {
            test.setRetryCooldownDays(Math.min(testDTO.getRetryCooldownDays(), 14));
            test.setRetryCooldownHours(testDTO.getRetryCooldownDays() * 24);
        } else if (testDTO.getRetryCooldownHours() != null && testDTO.getRetryCooldownHours() > 0) {
            test.setRetryCooldownHours(Math.min(testDTO.getRetryCooldownHours(), 336));
            test.setRetryCooldownDays(testDTO.getRetryCooldownHours() / 24);
        } else {
            test.setRetryCooldownHours(0);
            test.setRetryCooldownDays(0);
        }

        if (testDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(testDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Категория не найдена"));
            test.setCategory(category);
        }

        Test savedTest = testRepository.save(test);
        log.info("Создан тест: {} с ограничением на повтор: {}",
                savedTest.getTitle(), savedTest.getRetryCooldownDisplay());

        return savedTest;
    }

    /**
     * Получение всех тестов создателя через DTO проекцию
     */
    @Transactional(readOnly = true)
    public Page<TestDTO> getTestsByCreator(String username,
                                           Pageable pageable,
                                           Boolean published,
                                           String search,
                                           Long categoryId) {

        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Specification<Test> spec = Specification
                .where(TestSpecifications.byCreator(creator))
                .and(TestSpecifications.byPublishedStatus(published))
                .and(TestSpecifications.byTitleContaining(search))
                .and(TestSpecifications.byCategoryId(categoryId));

        List<Long> testIds = testRepository.findAll(spec)
                .stream()
                .map(Test::getId)
                .collect(Collectors.toList());

        if (testIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return testRepository.findTestDTOsByIds(testIds, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TestDTO> getTestsByCreator(String username, Pageable pageable) {
        return getTestsByCreator(username, pageable, null, null, null);
    }

    /**
     * Получение теста по ID с проверкой прав
     */
    @Transactional(readOnly = true)
    public Optional<Test> getTestById(Long testId, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));


        return testRepository.findByIdAndCreatedBy(testId, creator);
    }

    /**
     * Обновление теста
     */
    @Transactional
    public Test updateTest(Long testId, TestUpdateDTO updateDTO, String username) {
        Test existingTest = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!existingTest.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав для редактирования теста");
        }

        existingTest.setTitle(updateDTO.getTitle().trim());
        existingTest.setDescription(updateDTO.getDescription());
        existingTest.setTimeLimitMinutes(
                updateDTO.getTimeLimitMinutes() != null ?
                        Math.min(updateDTO.getTimeLimitMinutes(), 300) : 0
        );


        if (updateDTO.getRetryCooldownDays() != null && updateDTO.getRetryCooldownDays() > 0) {
            existingTest.setRetryCooldownDays(Math.min(updateDTO.getRetryCooldownDays(), 14));
            existingTest.setRetryCooldownHours(updateDTO.getRetryCooldownDays() * 24);
        } else if (updateDTO.getRetryCooldownHours() != null && updateDTO.getRetryCooldownHours() > 0) {
            existingTest.setRetryCooldownHours(Math.min(updateDTO.getRetryCooldownHours(), 336));
            existingTest.setRetryCooldownDays(updateDTO.getRetryCooldownHours() / 24);
        } else {
            existingTest.setRetryCooldownHours(0);
            existingTest.setRetryCooldownDays(0);
        }

        if (updateDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(updateDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Категория не найдена"));
            existingTest.setCategory(category);
        }

        Test updatedTest = testRepository.save(existingTest);
        log.info("Обновлен тест: {}, ограничение на повтор: {}",
                updatedTest.getTitle(), updatedTest.getRetryCooldownDisplay());

        return updatedTest;
    }

    /**
     * Публикация/снятие с публикации теста
     */
    @CacheEvict(value = "publishedTests", allEntries = true)
    @Transactional
    public Test switchPublishStatus(Long testId, String username, boolean publish) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getId().equals(creator.getId())) {
            throw new RuntimeException("Нет прав для изменения статуса теста");
        }

        if (publish) {
            List<Question> questions = questionRepository.findQuestionsWithAnswersByTestId(testId);

            if (questions.isEmpty()) {
                throw new RuntimeException("Нельзя опубликовать тест без вопросов");
            }

            for (Question question : questions) {
                if (question.getAnswerOptions() == null || question.getAnswerOptions().isEmpty()) {
                    throw new RuntimeException("Вопрос не содержит вариантов ответов");
                }

                boolean hasCorrectAnswer = question.getAnswerOptions().stream()
                        .anyMatch(AnswerOption::getIsCorrect);

                if (!hasCorrectAnswer) {
                    throw new RuntimeException("В вопросе нет правильного ответа");
                }
            }

        }
        log.info("Тест '{}' (ID: {}) {} пользователем {}",
                test.getTitle(), testId,
                publish ? "опубликован" : "снят с публикации",
                username);

        test.setIsPublished(publish);
        return testRepository.save(test);
    }


    /**
     * Удаление теста
     */
    @Transactional
    public void deleteTest(Long testId, String username) {
        log.info("Удаление теста ID: {} пользователем: {}", testId, username);

        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Получаем тест с проверкой прав
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getId().equals(creator.getId())) {
            throw new RuntimeException("Нет прав для удаления этого теста");
        }

        // Проверяем наличие попыток
        if (test.getAttempts() != null && !test.getAttempts().isEmpty()) {
            throw new RuntimeException("Нельзя удалить тест, у которого есть попытки прохождения");
        }

        // Удаляем вопросы и варианты ответов
        List<Question> questions = questionRepository.findByTestIdOrderByOrderIndex(testId);
        for (Question question : questions) {
            if (question.getAnswerOptions() != null && !question.getAnswerOptions().isEmpty()) {
                answerOptionRepository.deleteAll(question.getAnswerOptions());
            }
        }

        if (!questions.isEmpty()) {
            questionRepository.deleteAll(questions);
        }

        testRepository.delete(test);

        log.info("Тест '{}' (ID: {}) успешно удален", test.getTitle(), testId);
    }

    /**
     * Получение теста для просмотра создателем (без создания попытки)
     */
    @Transactional(readOnly = true)
    public Optional<TestTakingDTO> getTestForPreview(Long testId, String creatorUsername) {
        // 1. Получаем основную информацию о тесте
        TestPreviewDTO testPreview = testRepository.findTestPreviewDTO(testId)
                .orElse(null);

        if (testPreview == null) {
            return Optional.empty();
        }

        // 2. Проверяем права создателя
        if (!testPreview.getCreatorUsername().equals(creatorUsername)) {
            throw new RuntimeException("Нет прав для просмотра этого теста");
        }

        // 3. Получаем все вопросы теста
        List<QuestionPreviewDTO> questions = testRepository.findQuestionPreviewDTOs(testId);

        // 4. Для каждого вопроса загружаем ответы
        for (QuestionPreviewDTO question : questions) {
            List<AnswerPreviewDTO> answers = testRepository.findAnswerPreviewDTOs(question.getId());
            question.setAnswerOptions(answers);
        }

        testPreview.setQuestions(questions);

        // 5. Конвертируем в TestTakingDTO для отображения
        TestTakingDTO dto = new TestTakingDTO();
        dto.setTestId(testPreview.getId());
        dto.setTestTitle(testPreview.getTitle());
        dto.setTimeLimitMinutes(testPreview.getTimeLimitMinutes());

        // 6. Преобразуем вопросы
        List<QuestionForTakingDTO> questionDTOs = questions.stream()
                .map(q -> {
                    QuestionForTakingDTO questionDTO = new QuestionForTakingDTO();
                    questionDTO.setId(q.getId());
                    questionDTO.setText(q.getText());
                    questionDTO.setOrderIndex(q.getOrderIndex());

                    List<QuestionForTakingDTO.AnswerOptionForTakingDTO> answerDTOs =
                            q.getAnswerOptions().stream()
                                    .map(a -> {
                                        QuestionForTakingDTO.AnswerOptionForTakingDTO answerDTO =
                                                new QuestionForTakingDTO.AnswerOptionForTakingDTO();
                                        answerDTO.setId(a.getId());
                                        answerDTO.setText(a.getText() + (a.getIsCorrect() ? " ✓" : ""));
                                        return answerDTO;
                                    })
                                    .collect(Collectors.toList());

                    questionDTO.setAnswerOptions(answerDTOs);
                    return questionDTO;
                })
                .sorted(Comparator.comparing(QuestionForTakingDTO::getOrderIndex))
                .collect(Collectors.toList());

        dto.setQuestions(questionDTOs);
        dto.setTotalQuestions(questionDTOs.size());

        return Optional.of(dto);
    }

    /**
     *  Поиск тестов создателя по названию
     */
    @Transactional(readOnly = true)
    public Page<TestDTO> searchTestsByCreator(String username, String searchTerm, Pageable pageable) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getTestsByCreator(username, pageable);
        }

        return testRepository.searchTestsByCreator(creator, searchTerm.trim(), pageable);
    }

    /**
     * Получение теста без загрузки вопросов и ответов (для статистики)
     */
    @Transactional(readOnly = true)
    public Optional<Test> getTestBasicById(Long testId, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Простой запрос без загрузки коллекций
        return testRepository.findById(testId)
                .filter(test -> test.getCreatedBy().getId().equals(creator.getId()));
    }

    /**
     * Получение теста с вопросами и ответами (только когда нужно)
     */
    @Transactional(readOnly = true)
    public Optional<TestTakingDTO> getTestPreviewDTO(Long testId, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 1. Загружаем тест для проверки прав
        Optional<Test> testOpt = testRepository.findByIdWithCreatorAndCategory(testId, creator);

        if (testOpt.isEmpty()) {
            return Optional.empty();
        }

        Test test = testOpt.get();

        // 2. Загружаем вопросы с ответами
        List<Question> questions = questionRepository.findQuestionsWithAnswersByTestId(testId);

        // 3. Сразу конвертируем в DTO внутри транзакции
        TestTakingDTO dto = new TestTakingDTO();
        dto.setTestId(test.getId());
        dto.setTestTitle(test.getTitle());
        dto.setTimeLimitMinutes(test.getTimeLimitMinutes());

        List<QuestionForTakingDTO> questionDTOs = questions.stream()
                .sorted(Comparator.comparing(Question::getOrderIndex))
                .map(this::convertToQuestionForTakingDTO)
                .collect(Collectors.toList());

        dto.setQuestions(questionDTOs);
        dto.setTotalQuestions(questionDTOs.size());
        dto.setAnsweredQuestions(0);
        dto.setRemainingQuestions(questionDTOs.size());

        return Optional.of(dto);
    }

    private QuestionForTakingDTO convertToQuestionForTakingDTO(Question question) {
        QuestionForTakingDTO dto = new QuestionForTakingDTO();
        dto.setId(question.getId());
        dto.setText(question.getText());
        dto.setOrderIndex(question.getOrderIndex());

        if (question.getAnswerOptions() != null) {
            List<QuestionForTakingDTO.AnswerOptionForTakingDTO> answerDTOs =
                    question.getAnswerOptions().stream()
                            .map(this::convertToAnswerOptionForTakingDTO)
                            .collect(Collectors.toList());
            dto.setAnswerOptions(answerDTOs);
        }

        return dto;
    }

    private QuestionForTakingDTO.AnswerOptionForTakingDTO convertToAnswerOptionForTakingDTO(AnswerOption answer) {
        QuestionForTakingDTO.AnswerOptionForTakingDTO dto =
                new QuestionForTakingDTO.AnswerOptionForTakingDTO();
        dto.setId(answer.getId());
        dto.setText(answer.getText() + (answer.getIsCorrect() ? " ✓" : ""));
        return dto;
    }
    /**
     * Получение ВСЕХ тестов для создателя (не только своих)
     */
    @Transactional(readOnly = true)
    public Page<TestDTO> getAllTestsForCreator(String username,
                                               Pageable pageable,
                                               String status,
                                               String search,
                                               Long categoryId,
                                               Long creatorId) {

        log.debug("Получение всех тестов с фильтрацией: status={}, search={}, categoryId={}, creatorId={}",
                status, search, categoryId, creatorId);

        return testRepository.findAllTestsWithFilters(status, categoryId, creatorId, search, pageable);
    }

    /**
     * Проверка, является ли пользователь владельцем теста
     */
    @Transactional(readOnly = true)
    public boolean isTestOwner(Long testId, String username) {
        return testRepository.findById(testId)
                .map(test -> test.getCreatedBy().getUsername().equals(username))
                .orElse(false);
    }

    /**
     * Получение теста без проверки владельца (для статистики и ограничений)
     */
    @Transactional(readOnly = true)
    public Test getTestByIdWithoutOwnershipCheck(Long testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));
    }

    @Transactional(readOnly = true)
    public Optional<Test> getTestWithAllDataWithoutOwnershipCheck(Long testId) {
        // 1. Сначала загружаем тест с категорией (без вопросов)
        Optional<Test> testOpt = testRepository.findByIdWithCategory(testId);

        if (testOpt.isPresent()) {
            Test test = testOpt.get();
            // 2. Затем отдельно загружаем вопросы с ответами
            List<Question> questions = questionRepository.findQuestionsWithAnswersByTestId(testId);
            test.setQuestions(questions);
            return Optional.of(test);
        }

        return Optional.empty();
    }


    /**
     * Конвертирует Entity Test в TestDTO
     */
    public TestDTO convertToDTO(Test test) {
        TestDTO dto = new TestDTO();
        dto.setId(test.getId());
        dto.setTitle(test.getTitle());
        dto.setDescription(test.getDescription());
        dto.setPublished(Boolean.TRUE.equals(test.getIsPublished()));
        dto.setQuestionCount((long) test.getQuestionCount());
        dto.setCreatedAt(test.getCreatedAt());
        dto.setTimeLimitMinutes(test.getTimeLimitMinutes());
        dto.setRetryCooldownHours(test.getRetryCooldownHours());
        dto.setRetryCooldownDays(test.getRetryCooldownDays());

        if (test.getCategory() != null) {
            dto.setCategoryId(test.getCategory().getId());
            dto.setCategoryName(test.getCategory().getName());
        }

        return dto;
    }
}
