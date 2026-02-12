package com.frist.assesspro.service;


import com.frist.assesspro.dto.UserStatisticsDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.test.*;
import com.frist.assesspro.entity.*;
import com.frist.assesspro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestPassingService {

    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final CategoryService categoryService;
    private final ProfileService profileService;
    private final CooldownService cooldownService;

    /**
     * 🔥 НОВОЕ: Получение ВСЕХ доступных тестов С ПАГИНАЦИЕЙ (для каталога)
     * Кэширование с учетом страницы и размера
     */
    @Cacheable(value = "publishedTests",
            key = "'all-tests-page-' + #page + '-' + #size",
            unless = "#result == null or #result.isEmpty()")
    @Transactional(readOnly = true)
    public Page<TestInfoDTO> getAllAvailableTestsDTOPaginated(int page, int size) {
        log.info("Запрос всех доступных тестов (страница: {}, размер: {})", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return testRepository.findPublishedTestInfoDTOs(pageable);
    }
    /**
     *  Получение тестов ПО КАТЕГОРИИ С ПАГИНАЦИЕЙ
     * Кэширование по ID категории + страница + размер
     */
    @Cacheable(value = "publishedTests",
            key = "'category-tests-page-' + #categoryId + '-' + #page + '-' + #size",
            unless = "#result == null or #result.isEmpty()")
    @Transactional(readOnly = true)
    public Page<TestInfoDTO> getAvailableTestsByCategoryDTOPaginated(Long categoryId, int page, int size) {
        log.info("Запрос тестов категории ID: {} (страница: {}, размер: {})", categoryId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (categoryId == null) {
            return testRepository.findPublishedTestInfoDTOs(pageable);
        }

        return testRepository.findPublishedTestInfoDTOsByCategoryId(categoryId, pageable);
    }
    /**
     * Получение теста для прохождения как DTO
     */
    @Transactional
    public Optional<TestTakingDTO> getTestForTaking(Long testId, String username) {

        if (!profileService.isProfileComplete(username)) {
            throw new RuntimeException("Для прохождения тестов необходимо заполнить профиль (ФИО)");
        }

        Test test = testRepository.findByIdAndIsPublishedTrue(testId)
                .orElse(null);

        if (test == null) {
            return Optional.empty();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!cooldownService.canUserTakeTest(test, user)) {
            LocalDateTime nextAvailable = cooldownService.getNextAvailableTime(test, user);
            String message = String.format(
                    "Вы уже проходили этот тест. Следующая попытка доступна %s",
                    formatDateTime(nextAvailable)
            );
            throw new RuntimeException(message);
        }

        Optional<TestAttempt> existingAttempt = testAttemptRepository
                .findByTestIdAndUserIdAndStatus(testId, user.getId(), TestAttempt.AttemptStatus.IN_PROGRESS);

        Long attemptId;
        if (existingAttempt.isPresent()) {
            attemptId = existingAttempt.get().getId();
        } else {
            TestAttempt attempt = new TestAttempt();
            attempt.setTest(test);
            attempt.setUser(user);
            attempt.setStartTime(LocalDateTime.now());
            attempt.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
            attempt.setTotalScore(0);

            TestAttempt savedAttempt = testAttemptRepository.save(attempt);
            attemptId = savedAttempt.getId();
            log.info("Создана новая попытка теста ID: {}", testId);
        }

        // Безопасное преобразование вопросов в DTO с проверкой на null
        List<QuestionForTakingDTO> questionDTOs = new ArrayList<>();
        if (test.getQuestions() != null && !test.getQuestions().isEmpty()) {
            questionDTOs = test.getQuestions().stream()
                    .sorted(Comparator.comparing(Question::getOrderIndex))
                    .map(this::convertToQuestionForTakingDTO)
                    .collect(Collectors.toList());
        }

        // Проверяем, что у вопросов есть варианты ответов
        questionDTOs = questionDTOs.stream()
                .peek(question -> {
                    if (question.getAnswerOptions() == null) {
                        question.setAnswerOptions(new ArrayList<>());
                    }
                })
                .collect(Collectors.toList());

        TestTakingDTO dto = new TestTakingDTO();
        dto.setAttemptId(attemptId);
        dto.setTestId(test.getId());
        dto.setTestTitle(test.getTitle());
        dto.setTimeLimitMinutes(test.getTimeLimitMinutes());
        dto.setQuestions(questionDTOs);  // ← Гарантировано не null
        dto.setTotalQuestions(questionDTOs.size());

        return Optional.of(dto);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return dateTime.format(formatter);
    }

    private QuestionForTakingDTO convertToQuestionForTakingDTO(Question question) {
        QuestionForTakingDTO dto = new QuestionForTakingDTO();
        dto.setId(question.getId());
        dto.setText(question.getText());
        dto.setOrderIndex(question.getOrderIndex());

        // Безопасное преобразование ответов
        if (question.getAnswerOptions() != null && !question.getAnswerOptions().isEmpty()) {
            List<QuestionForTakingDTO.AnswerOptionForTakingDTO> answerDTOs = question.getAnswerOptions().stream()
                    .map(this::convertToAnswerOptionForTakingDTO)
                    .collect(Collectors.toList());
            dto.setAnswerOptions(answerDTOs);
        }

        return dto;
    }

    private QuestionForTakingDTO.AnswerOptionForTakingDTO convertToAnswerOptionForTakingDTO(AnswerOption answer) {
        QuestionForTakingDTO.AnswerOptionForTakingDTO dto = new QuestionForTakingDTO.AnswerOptionForTakingDTO();
        dto.setId(answer.getId());
        dto.setText(answer.getText());
        return dto;
    }

    /**
     * Сохранение ответа
     */
    @Transactional
    public void saveAnswer(TestPassingDTO testPassingDTO, String username) {
        TestAttempt attempt = testAttemptRepository.findById(testPassingDTO.getAttemptId())
                .orElseThrow(() -> new RuntimeException("Попытка не найдена!"));

        if (!attempt.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав доступа к этой попытке!");
        }

        if (attempt.getStatus() != TestAttempt.AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Попытка уже завершена!");
        }

        Question question = questionRepository.findById(testPassingDTO.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        if (!question.getTest().getId().equals(attempt.getTest().getId())) {
            throw new RuntimeException("Вопрос не принадлежит этому тесту");
        }

        AnswerOption chosenAnswer = null;

        if (testPassingDTO.getAnswerOptionId() != null) {
            chosenAnswer = answerOptionRepository.findById(testPassingDTO.getAnswerOptionId())
                    .orElseThrow(() -> new RuntimeException("Вариант ответа не найдент!"));
        }

        Optional<UserAnswer> existingAnswer = userAnswerRepository
                .findByAttemptIdAndQuestionId(attempt.getId(), question.getId());

        UserAnswer userAnswer;
        if (existingAnswer.isPresent()) {
            userAnswer = existingAnswer.get();
            userAnswer.setChosenAnswerOption(chosenAnswer);
        } else {
            userAnswer = new UserAnswer();
            userAnswer.setAttempt(attempt);
            userAnswer.setQuestion(question);
            userAnswer.setChosenAnswerOption(chosenAnswer);
        }

        // Определяем, правильный ли ответ
        if (chosenAnswer != null) {
            userAnswer.setIsCorrect(chosenAnswer.getIsCorrect());
            userAnswer.setPointsEarned(chosenAnswer.getIsCorrect() ? 1 : 0);
        } else {
            userAnswer.setIsCorrect(false);
            userAnswer.setPointsEarned(0);
        }

        userAnswerRepository.save(userAnswer);

        // Обновляем общий счет
        updateAttemptTotalScore(attempt.getId());
    }
    /**
     * Получение ВСЕХ доступных тестов (без пагинации, для дашборда)
     * С кэшированием - ключ 'all-tests-list'
     */
    @Cacheable(value = "publishedTests",
            key = "'all-tests-list'",
            unless = "#result == null or #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<TestInfoDTO> getAllAvailableTestsDTO() {
        log.info("Запрос всех доступных тестов (список)");
        return testRepository.findPublishedTestInfoDTOs();
    }
    /**
     * 🔥 НОВОЕ: Получение тестов ПО КАТЕГОРИИ (без пагинации)
     * Кэширование по ID категории
     */
    @Cacheable(value = "publishedTests",
            key = "'category-tests-list-' + #categoryId",
            unless = "#result == null or #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<TestInfoDTO> getAvailableTestsByCategoryDTO(Long categoryId) {
        log.info("Запрос тестов категории ID: {} (список)", categoryId);

        if (categoryId == null) {
            return getAllAvailableTestsDTO();
        }

        return testRepository.findPublishedTestInfoDTOsByCategoryId(categoryId);
    }



    /**
     * Завершение теста и получение результатов как DTO
     */
    @Transactional
    public TestResultsDTO finishTestAndGetResults(Long attemptId, String username) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Попытка не найдена"));

        if (!attempt.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Нет доступа к этой попытке");
        }

        if (attempt.getStatus() == TestAttempt.AttemptStatus.IN_PROGRESS) {
            attempt.setStatus(TestAttempt.AttemptStatus.COMPLETED);
            attempt.setEndTime(LocalDateTime.now());
            testAttemptRepository.save(attempt);
            log.info("Завершена попытка теста ID: {}", attempt.getTest().getId());
        }
        return getTestResults(attemptId, username);
    }

    /**
     * Получение результатов теста как DTO
     */
    @Transactional(readOnly = true)
    public TestResultsDTO getTestResults(Long attemptId, String username) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Попытка не найдена"));


        if (!attempt.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Нет доступа к этой попытке");
        }

        Test test = attempt.getTest();


        List<UserAnswer> userAnswers = userAnswerRepository.findByAttemptId(attemptId);


        List<QuestionResultDTO> questionResults = userAnswers.stream()
                .map(this::convertToQuestionResultDTO)
                .collect(Collectors.toList());


        int totalQuestions = test.getQuestions().size();
        int answeredQuestions = userAnswers.size();
        int correctAnswers = (int) userAnswers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect()))
                .count();
        int maxPossibleScore = totalQuestions; // По 1 баллу за каждый правильный ответ

        TestResultsDTO dto = new TestResultsDTO();
        dto.setAttemptId(attempt.getId());
        dto.setTestId(test.getId());  // ← УСТАНАВЛИВАЕМ testId!
        dto.setTestTitle(test.getTitle());
        dto.setStartTime(attempt.getStartTime());
        dto.setEndTime(attempt.getEndTime());
        dto.setTotalScore(attempt.getTotalScore() != null ? attempt.getTotalScore() : 0);
        dto.setMaxPossibleScore(maxPossibleScore);
        dto.setTotalQuestions(totalQuestions);
        dto.setAnsweredQuestions(answeredQuestions);
        dto.setCorrectAnswers(correctAnswers);
        dto.setQuestionResults(questionResults);

        return dto;
    }

    private QuestionResultDTO convertToQuestionResultDTO(UserAnswer userAnswer) {
        QuestionResultDTO dto = new QuestionResultDTO();


        if (userAnswer.getQuestion() == null) {
            dto.setQuestionId(null);
            dto.setQuestionText("Вопрос не найден");
        } else {
            dto.setQuestionId(userAnswer.getQuestion().getId());
            dto.setQuestionText(userAnswer.getQuestion().getText());
        }


        if (userAnswer.getChosenAnswerOption() != null) {
            dto.setChosenAnswerId(userAnswer.getChosenAnswerOption().getId());
            dto.setChosenAnswerText(userAnswer.getChosenAnswerOption().getText());
        } else {
            dto.setChosenAnswerId(null);
            dto.setChosenAnswerText("Ответ не предоставлен");
        }

        // Правильный ответ
        if (userAnswer.getQuestion() != null && userAnswer.getQuestion().getAnswerOptions() != null) {
            Optional<AnswerOption> correctAnswer = userAnswer.getQuestion().getAnswerOptions().stream()
                    .filter(AnswerOption::getIsCorrect)
                    .findFirst();

            if (correctAnswer.isPresent()) {
                dto.setCorrectAnswerId(correctAnswer.get().getId());
                dto.setCorrectAnswerText(correctAnswer.get().getText());
            } else {
                dto.setCorrectAnswerId(null);
                dto.setCorrectAnswerText("Правильный ответ не указан");
            }
        } else {
            dto.setCorrectAnswerId(null);
            dto.setCorrectAnswerText("Нет вариантов ответа");
        }

        dto.setIsCorrect(userAnswer.getIsCorrect());
        dto.setPointsEarned(userAnswer.getPointsEarned() != null ? userAnswer.getPointsEarned() : 0);

        return dto;
    }

    /**
     * Получение истории тестов с пагинацией через DTO проекцию
     */
    @Transactional(readOnly = true)
    public Page<TestHistoryDTO> getUserTestHistory(String username, int page, int size, String status) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());

        // Используем DTO проекцию вместо преобразования Entity
        return testAttemptRepository.findTestHistoryDTOsByUserId(user.getId(), pageable);
    }

    /**
     * Получение истории тестов (без пагинации, для дашборда)
     */
    @Transactional(readOnly = true)
    public List<TestHistoryDTO> getUserTestHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime").descending());
        return testAttemptRepository.findTestHistoryDTOsByUserId(user.getId(), pageable).getContent();
    }

    /**
     * Обновление общего счета попытки
     */
    private void updateAttemptTotalScore(Long attemptId) {
        Integer totalScore = userAnswerRepository.sumPointsEarnedByAttemptId(attemptId);
        if (totalScore == null) {
            totalScore = 0;
        }

        testAttemptRepository.updateTotalScore(attemptId, totalScore);
    }

    /**
     * Получение статистики пользователя
     */
    @Transactional(readOnly = true)
    public UserStatisticsDTO getUserStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserStatisticsDTO statistics = new UserStatisticsDTO();

        statistics.setTotalAttempts(testAttemptRepository.countByUserId(user.getId()));
        statistics.setCompletedAttempts(testAttemptRepository.countByUserIdAndStatus(
                user.getId(), TestAttempt.AttemptStatus.COMPLETED));
        statistics.setInProgressAttempts(testAttemptRepository.countByUserIdAndStatus(
                user.getId(), TestAttempt.AttemptStatus.IN_PROGRESS));

        Double averageScore = testAttemptRepository.findAverageScoreByUserId(user.getId());
        statistics.setAverageScore(averageScore != null ? averageScore : 0.0);

        return statistics;
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAvailableCategories() {
        return categoryService.getAllActiveCategories();
    }

    /**
     * 🔥 НОВОЕ: Поиск тестов по названию (для тестера)
     */
    @Transactional(readOnly = true)
    public Page<TestInfoDTO> searchTests(String searchTerm, int page, int size) {
        log.info("Поиск тестов по запросу: '{}', страница: {}, размер: {}", searchTerm, page, size);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllAvailableTestsDTOPaginated(page, size);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return testRepository.searchPublishedTests(searchTerm.trim(), pageable);
    }

    /**
     * 🔥 НОВОЕ: Быстрый поиск для автодополнения (AJAX)
     */
    @Transactional(readOnly = true)
    public List<TestInfoDTO> quickSearchTests(String searchTerm, int limit) {
        if (searchTerm == null || searchTerm.trim().length() < 2) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit);
        return testRepository.searchPublishedTests(searchTerm.trim(), pageable).getContent();
    }

}
