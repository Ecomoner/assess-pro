package com.frist.assesspro.service;


import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.test.QuestionForTakingDTO;
import com.frist.assesspro.dto.test.TestTakingDTO;
import com.frist.assesspro.dto.test.TestUpdateDTO;
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

        // Проверка уникальности (case-insensitive)
        boolean titleExists = testRepository.findByCreatedBy(user).stream()
                .anyMatch(t -> t.getTitle().equalsIgnoreCase(title));

        if (titleExists) {
            throw new IllegalArgumentException("Тест с таким названием уже существует");
        }

        // Проверка на максимальную длину описания
        String description = testDTO.getDescription();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAX_DESCRIPTION_LENGTH);
            log.warn("Описание теста обрезано до {} символов", MAX_DESCRIPTION_LENGTH);
        }

        Test test = new Test();
        test.setTitle(title);
        test.setDescription(description);
        test.setCreatedBy(user);
        test.setIsPublished(false);
        test.setTimeLimitMinutes(testDTO.getTimeLimitMinutes() != null ?
                Math.min(testDTO.getTimeLimitMinutes(), 300) : 0);
        test.setRetryCooldownHours(testDTO.getRetryCooldownHours() != null ?
                Math.min(testDTO.getRetryCooldownHours(), 336) : 0);
        test.setRetryCooldownDays(testDTO.getRetryCooldownDays() != null ?
                Math.min(testDTO.getRetryCooldownDays(), 14) : 0);

        if (testDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(testDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Категория не найдена"));
            test.setCategory(category);
        }

        return testRepository.save(test);
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

        // Строим спецификацию
        Specification<Test> spec = Specification
                .where(TestSpecifications.byCreator(creator))
                .and(TestSpecifications.byPublishedStatus(published))
                .and(TestSpecifications.byTitleContaining(search))
                .and(TestSpecifications.byCategoryId(categoryId));

        // Получаем ID тестов, удовлетворяющих условиям
        List<Long> testIds = testRepository.findAll(spec)
                .stream()
                .map(Test::getId)
                .collect(Collectors.toList());

        if (testIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Получаем DTO проекцию для найденных ID с пагинацией
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

        // Обновляем только разрешенные поля
        existingTest.setTitle(updateDTO.getTitle().trim());
        existingTest.setDescription(updateDTO.getDescription());
        existingTest.setTimeLimitMinutes(
                updateDTO.getTimeLimitMinutes() != null ?
                        Math.min(updateDTO.getTimeLimitMinutes(), 300) : 0
        );
        existingTest.setRetryCooldownHours(
                updateDTO.getRetryCooldownHours() != null ?
                        Math.min(updateDTO.getRetryCooldownHours(), 336) : 0
        );
        existingTest.setRetryCooldownDays(
                updateDTO.getRetryCooldownDays() != null ?
                        Math.min(updateDTO.getRetryCooldownDays(), 14) : 0
        );

        // Обновляем категорию
        if (updateDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(updateDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Категория не найдена"));
            existingTest.setCategory(category);
        }

        log.info("Обновлен тест '{}' (ID: {}) пользователем {}",
                existingTest.getTitle(), testId, username);

        return testRepository.save(existingTest);
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

        // Проверяем права
        if (!test.getCreatedBy().getId().equals(creator.getId())) {
            throw new RuntimeException("Нет прав для изменения статуса теста");
        }

        if (publish) {
            // 1. Проверяем наличие вопросов
            int questionCount = questionRepository.countByTestId(testId);
            if (questionCount == 0) {
                throw new RuntimeException("Нельзя опубликовать тест без вопросов");
            }

            // 2. Проверяем, что у ВСЕХ вопросов есть варианты ответов
            List<Question> questions = questionRepository.findByTestIdOrderByOrderIndex(testId);
            if (questions.isEmpty()) {
                throw new RuntimeException("Тест не содержит вопросов");
            }

            for (Question question : questions) {
                if (question.getAnswerOptions() == null || question.getAnswerOptions().isEmpty()) {
                    throw new RuntimeException(
                            String.format("Вопрос '%s' не содержит вариантов ответов",
                                    question.getText().length() > 50 ?
                                            question.getText().substring(0, 50) + "..." :
                                            question.getText())
                    );
                }

                // 3. Проверяем, что есть хотя бы один правильный ответ
                boolean hasCorrectAnswer = question.getAnswerOptions().stream()
                        .anyMatch(AnswerOption::getIsCorrect);

                if (!hasCorrectAnswer) {
                    throw new RuntimeException(
                            String.format("В вопросе '%s' нет правильного ответа",
                                    question.getText().length() > 50 ?
                                            question.getText().substring(0, 50) + "..." :
                                            question.getText())
                    );
                }
            }
        }

        test.setIsPublished(publish);
        Test savedTest = testRepository.save(test);

        log.info("Тест '{}' (ID: {}) {} пользователем {}",
                savedTest.getTitle(), testId,
                publish ? "опубликован" : "снят с публикации",
                username);

        return savedTest;
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
        Test test = testRepository.findById(testId)
                .orElse(null);

        if (test == null) {
            return Optional.empty();
        }

        // Проверяем права создателя
        if (!test.getCreatedBy().getUsername().equals(creatorUsername)) {
            throw new RuntimeException("Нет прав для просмотра этого теста");
        }

        // Конвертируем в DTO для просмотра
        TestTakingDTO dto = new TestTakingDTO();
        dto.setTestId(test.getId());
        dto.setTestTitle(test.getTitle());
        dto.setTimeLimitMinutes(test.getTimeLimitMinutes());

        // Безопасное преобразование вопросов
        List<QuestionForTakingDTO> questionDTOs = new ArrayList<>();
        if (test.getQuestions() != null && !test.getQuestions().isEmpty()) {
            questionDTOs = test.getQuestions().stream()
                    .sorted(Comparator.comparing(Question::getOrderIndex))
                    .map(question -> {
                        QuestionForTakingDTO questionDTO = new QuestionForTakingDTO();
                        questionDTO.setId(question.getId());
                        questionDTO.setText(question.getText());
                        questionDTO.setOrderIndex(question.getOrderIndex());

                        // ВАЖНО: Для создателя показываем ВСЕ варианты, включая правильные
                        if (question.getAnswerOptions() != null) {
                            List<QuestionForTakingDTO.AnswerOptionForTakingDTO> answerDTOs =
                                    question.getAnswerOptions().stream()
                                            .map(answer -> {
                                                QuestionForTakingDTO.AnswerOptionForTakingDTO answerDTO =
                                                        new QuestionForTakingDTO.AnswerOptionForTakingDTO();
                                                answerDTO.setId(answer.getId());
                                                answerDTO.setText(answer.getText() +
                                                        (answer.getIsCorrect() ? " ✓" : ""));
                                                return answerDTO;
                                            })
                                            .collect(Collectors.toList());
                            questionDTO.setAnswerOptions(answerDTOs);
                        }

                        return questionDTO;
                    })
                    .collect(Collectors.toList());
        }

        dto.setQuestions(questionDTOs);
        dto.setTotalQuestions(questionDTOs.size());

        return Optional.of(dto);
    }

    @Transactional
    public void removeRetryCooldownForUser(Long testId, String testerUsername, String creatorUsername) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        if (!test.getCreatedBy().getUsername().equals(creatorUsername)) {
            throw new RuntimeException("Нет прав для управления этим тестом");
        }

        User tester = userRepository.findByUsername(testerUsername)
                .orElseThrow(() -> new RuntimeException("Тестировщик не найден"));

        // Логируем снятие ограничений
        log.info("Создатель {} снял ограничения на повторное прохождение теста '{}' для пользователя {}",
                creatorUsername, test.getTitle(), testerUsername);

        // Здесь мы не удаляем ограничение полностью, а создаем запись об исключении
        // Для простоты сейчас просто логируем - в следующем шаге добавим таблицу exceptions
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
    public Optional<Test> getTestWithQuestionsAndAnswers(Long testId, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Используем оптимизированный запрос с EntityGraph
        return testRepository.findByIdWithQuestionsAndAnswers(testId, creator);
    }

    @Transactional(readOnly = true)
    public Optional<Test> getTestForEdit(Long testId, String username) {
        // Используем тот же метод - он загружает все что нужно
        return getTestWithQuestionsAndAnswers(testId, username);
    }

    /**
     * Конвертирует Entity Test в TestDTO
     */
    private TestDTO convertToDTO(Test test) {
        TestDTO dto = new TestDTO();
        dto.setId(test.getId());
        dto.setTitle(test.getTitle());
        dto.setDescription(test.getDescription());
        dto.setPublished(Boolean.TRUE.equals(test.getIsPublished()));
        dto.setQuestionCount((long) test.getQuestionCount());
        dto.setCreatedAt(test.getCreatedAt());
        dto.setTimeLimitMinutes(test.getTimeLimitMinutes());
        return dto;
    }
}
