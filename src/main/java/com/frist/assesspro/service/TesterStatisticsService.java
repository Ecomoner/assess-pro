package com.frist.assesspro.service;

import com.frist.assesspro.dto.statistics.*;
import com.frist.assesspro.entity.*;
import com.frist.assesspro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TesterStatisticsService {

    private final TestAttemptRepository testAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final RetryCooldownExceptionRepository exceptionRepository;

    /**
     * 1. Получить список всех тестировщиков, проходивших тест
     */
    @Cacheable(value = "testerStatistics", key = "T(String).format('%d_%d_%d_%s', " +
            "#testId, #pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString())")
    @Transactional(readOnly = true)
    public Page<TesterAttemptDTO> getTestersByTest(Long testId,
                                                   String creatorUsername,
                                                   Pageable pageable) {
        validateTestExists(testId,creatorUsername);

        Page<TestAttempt> attemptsPage = testAttemptRepository.findAttemptsByTestId(testId, pageable);

        List<TesterAttemptDTO> dtos = attemptsPage.getContent().stream()
                .map(this::convertToTesterAttemptDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, attemptsPage.getTotalElements());
    }

    /**
     * Конвертация TestAttempt в TesterAttemptDTO
     */
    private TesterAttemptDTO convertToTesterAttemptDTO(TestAttempt attempt) {
        TesterAttemptDTO dto = new TesterAttemptDTO();
        dto.setAttemptId(attempt.getId());
        dto.setTesterUsername(attempt.getUser().getUsername());
        dto.setTesterFullName(attempt.getUser().getFullName());
        dto.setStartTime(attempt.getStartTime());
        dto.setEndTime(attempt.getEndTime());
        dto.setScore(attempt.getTotalScore() != null ? attempt.getTotalScore() : 0);

        if (attempt.getTest() != null) {
            dto.setTestId(attempt.getTest().getId());
            dto.setTestTitle(attempt.getTest().getTitle());  // ← ДОБАВЛЕНО
        }

        int questionCount = 0;
        if (attempt.getTest() != null && attempt.getTest().getQuestions() != null) {
            questionCount = attempt.getTest().getQuestions().size();
            log.debug("convertToTesterAttemptDTO: attemptId={}, testId={}, questionCount={}",
                    attempt.getId(), attempt.getTest().getId(), questionCount);
        }
        dto.setMaxScore(questionCount);

        if (questionCount > 0) {
            dto.setPercentage((double) dto.getScore() / questionCount * 100);
        } else {
            dto.setPercentage(0.0);
        }

        dto.setDurationMinutes(calculateDurationMinutes(dto.getStartTime(), dto.getEndTime()));

        return dto;
    }

    /**
     * Вычисление длительности в минутах
     */
    private Long calculateDurationMinutes(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        try {
            return Duration.between(start, end).toMinutes();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 2. Получить детальные ответы конкретного тестировщика
     */
    @Transactional(readOnly = true)
    public TesterDetailedAnswersDTO getTesterDetailedAnswers(Long attemptId,String creatorUsername) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Попытка не найдена"));

        validateTestExists(attempt.getTest().getId(),creatorUsername);

        return buildTesterDetailedAnswersDTO(attempt);
    }

    /**
     * 3. Поиск тестировщиков по имени
     */
    @Transactional(readOnly = true)
    public List<TesterAttemptDTO> searchTestersByTestAndName(
            Long testId,String creatorUsername, String testerName) {
        validateTestExists(testId,creatorUsername);

        List<TestAttempt> attempts = testAttemptRepository.searchAttemptsByTestAndUsername(testId, testerName);

        return attempts.stream()
                .map(this::convertToTesterAttemptDTO)
                .collect(Collectors.toList());
    }

    /**
     * 4. Получить последние попытки по тестам создателя
     */
    @Transactional(readOnly = true)
    public List<TesterAttemptDTO> getRecentTestAttemptsForCreator(String creatorUsername, int limit) {
        List<TestAttempt> attempts = testAttemptRepository.findRecentAttemptsForCreator(creatorUsername);

        return attempts.stream()
                .limit(limit)
                .map(this::convertToTesterAttemptDTO)
                .collect(Collectors.toList());
    }

    /**
     * 5. Получить общую статистику по тесту
     */
    @Transactional(readOnly = true)
    public TestSummaryDTO getTestSummary(Long testId, String creatorUsername) {
        validateTestExists(testId, creatorUsername);

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));

        List<TestAttempt> attempts = testAttemptRepository.findByTestId(testId);

        long totalAttempts = attempts.size();
        long uniqueTesters = attempts.stream()
                .map(attempt -> attempt.getUser().getId())
                .distinct()
                .count();

        List<TestAttempt> completedAttempts = attempts.stream()
                .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.COMPLETED)
                .collect(Collectors.toList());

        double averageScore = completedAttempts.stream()
                .mapToInt(a -> a.getTotalScore() != null ? a.getTotalScore() : 0)
                .average()
                .orElse(0.0);

        double bestScore = completedAttempts.stream()
                .mapToInt(a -> a.getTotalScore() != null ? a.getTotalScore() : 0)
                .max()
                .orElse(0);

        int maxPossibleScore = test.getQuestions() != null ? test.getQuestions().size() : 0;
        double averagePercentage = maxPossibleScore > 0 ? (averageScore / maxPossibleScore) * 100 : 0;
        double bestPercentage = maxPossibleScore > 0 ? (bestScore / maxPossibleScore) * 100 : 0;

        TestSummaryDTO summary = calculateTestSummary(test, attempts);

        summary.setTotalAttempts(totalAttempts);
        summary.setUniqueTesters(uniqueTesters);
        summary.setAverageScore(averagePercentage);
        summary.setBestScore(bestPercentage);

        return summary;
    }

    /**
     * 6. Агрегированная статистика по тестировщикам (группировка всех попыток одного тестировщика)
     */
    @Transactional(readOnly = true)
    public Page<TesterAggregatedStatsDTO> getAggregatedTestersByTest(
            Long testId,
            String creatorUsername,
            Pageable pageable,
            LocalDateTime dateFrom,
            LocalDateTime dateTo) {

        validateTestExists(testId, creatorUsername);

        // Получаем все попытки по тесту с фильтрацией по дате
        List<TestAttempt> allAttempts = getFilteredAttempts(testId, dateFrom, dateTo);

        // Группируем по тестировщикам
        Map<User, List<TestAttempt>> attemptsByTester = allAttempts.stream()
                .collect(Collectors.groupingBy(TestAttempt::getUser));

        // Конвертируем в агрегированные DTO
        List<TesterAggregatedStatsDTO> aggregatedList = attemptsByTester.entrySet().stream()
                .map(entry -> buildTesterAggregatedStats(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // Применяем пагинацию
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), aggregatedList.size());

        return new PageImpl<>(
                aggregatedList.subList(start, end),
                pageable,
                aggregatedList.size()
        );
    }

    /**
     * 7. Получение детальной статистики конкретного тестировщика по всем его попыткам теста
     */
    @Transactional(readOnly = true)
    public TesterAggregatedStatsDTO getTesterAggregatedStats(
            Long testId,
            String testerUsername,
            String creatorUsername) {

        validateTestExists(testId, creatorUsername);

        User tester = userRepository.findByUsername(testerUsername)
                .orElseThrow(() -> new RuntimeException("Тестировщик не найден"));

        List<TestAttempt> testerAttempts = testAttemptRepository.findByTestId(testId).stream()
                .filter(attempt -> attempt.getUser().getId().equals(tester.getId()))
                .collect(Collectors.toList());

        return buildTesterAggregatedStats(tester, testerAttempts);
    }

    /**
     * 8. Поиск тестировщиков с фильтрацией по дате
     */
    private List<TestAttempt> getFilteredAttempts(Long testId, LocalDateTime dateFrom, LocalDateTime dateTo) {
        List<TestAttempt> attempts = testAttemptRepository.findByTestId(testId);

        if (dateFrom != null) {
            attempts = attempts.stream()
                    .filter(attempt -> !attempt.getStartTime().isBefore(dateFrom))
                    .collect(Collectors.toList());
        }

        if (dateTo != null) {
            attempts = attempts.stream()
                    .filter(attempt -> !attempt.getStartTime().isAfter(dateTo))
                    .collect(Collectors.toList());
        }

        return attempts;
    }

    /**
     * 9. Построение агрегированной статистики тестировщика
     */
    private TesterAggregatedStatsDTO buildTesterAggregatedStats(User tester, List<TestAttempt> attempts) {
        if (attempts.isEmpty()) {
            return new TesterAggregatedStatsDTO(
                    tester.getUsername(),
                    tester.getId(),
                    0L, 0L, 0.0, 0.0, 0.0, 0L,
                    null, null, List.of(), List.of()
            );
        }

        // Вычисляем агрегированные метрики
        long totalAttempts = attempts.size();
        long completedAttempts = attempts.stream()
                .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.COMPLETED)
                .count();

        // Вычисляем проценты для завершенных попыток
        List<Double> percentages = attempts.stream()
                .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.COMPLETED)
                .map(attempt -> {
                    int totalQuestions = attempt.getTest().getQuestions() != null ?
                            attempt.getTest().getQuestions().size() : 0;
                    if (totalQuestions > 0) {
                        return (double) (attempt.getTotalScore() != null ? attempt.getTotalScore() : 0)
                                / totalQuestions * 100;
                    }
                    return 0.0;
                })
                .filter(p -> p > 0)
                .collect(Collectors.toList());

        Double averagePercentage = percentages.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double bestPercentage = percentages.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        Double worstPercentage = percentages.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        // Вычисляем общую длительность
        Long totalDurationMinutes = attempts.stream()
                .filter(a -> a.getStartTime() != null && a.getEndTime() != null)
                .mapToLong(a -> Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                .sum();

        // Даты первой и последней попытки
        LocalDateTime firstAttemptDate = attempts.stream()
                .map(TestAttempt::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime lastAttemptDate = attempts.stream()
                .map(TestAttempt::getStartTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // ID всех попыток
        List<Long> attemptIds = attempts.stream()
                .map(TestAttempt::getId)
                .collect(Collectors.toList());

        // Последние 3 попытки (для быстрого просмотра)
        List<TesterAttemptDTO> recentAttempts = attempts.stream()
                .sorted((a1, a2) -> a2.getStartTime().compareTo(a1.getStartTime()))
                .limit(3)
                .map(this::convertToTesterAttemptDTO)
                .collect(Collectors.toList());

        return new TesterAggregatedStatsDTO(
                tester.getUsername(),
                tester.getId(),
                totalAttempts,
                completedAttempts,
                averagePercentage,
                bestPercentage,
                worstPercentage,
                totalDurationMinutes,
                firstAttemptDate,
                lastAttemptDate,
                attemptIds,
                recentAttempts
        );
    }

    /**
     * 10. Получение списка уникальных тестировщиков по тесту
     */
    @Transactional(readOnly = true)
    public List<User> getDistinctTestersByTest(Long testId, String creatorUsername) {
        validateTestExists(testId, creatorUsername);

        List<TestAttempt> attempts = testAttemptRepository.findByTestId(testId);

        return attempts.stream()
                .map(TestAttempt::getUser)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Получить все попытки тестировщика
     */
    @Transactional(readOnly = true)
    public List<TesterAttemptDTO> getAllAttemptsByTester(String testerUsername) {
        User tester = userRepository.findByUsername(testerUsername)
                .orElseThrow(() -> new RuntimeException("Тестировщик не найден"));

        List<TestAttempt> attempts = testAttemptRepository.findByUserId(tester.getId());

        return attempts.stream()
                .map(this::convertToTesterAttemptDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TesterStatisticsDTO> getTestersStatistics(Long testId,
                                                          String creatorUsername,
                                                          String search,
                                                          Pageable pageable) {
        validateTestExists(testId, creatorUsername);

        // 1. Получаем попытки с пользователями
        Page<TestAttempt> attemptsPage;
        if (search != null && !search.trim().isEmpty()) {
            attemptsPage = testAttemptRepository.searchByTestIdWithUser(testId, search.trim(), pageable);
        } else {
            attemptsPage = testAttemptRepository.findByTestIdWithUser(testId, pageable);
        }

        if (attemptsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2. Получаем все исключения для этих пользователей одним запросом
        Set<Long> userIdsWithExceptions = exceptionRepository.findUserIdsWithExceptions(testId);

        // 3. Преобразуем в DTO
        List<TesterStatisticsDTO> dtos = attemptsPage.getContent().stream()
                .map(attempt -> convertToTesterStatisticsDTO(attempt, userIdsWithExceptions))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, attemptsPage.getTotalElements());
    }

    private TesterStatisticsDTO convertToTesterStatisticsDTO(TestAttempt attempt, Set<Long> userIdsWithExceptions) {
        TesterStatisticsDTO dto = new TesterStatisticsDTO();

        // Основные данные
        dto.setAttemptId(attempt.getId());
        dto.setTestId(attempt.getTest().getId());
        dto.setTestTitle(attempt.getTest().getTitle());
        dto.setStartTime(attempt.getStartTime());
        dto.setEndTime(attempt.getEndTime());
        dto.setScore(attempt.getTotalScore() != null ? attempt.getTotalScore() : 0);

        // Данные пользователя
        User user = attempt.getUser();
        dto.setTesterUsername(user.getUsername());
        dto.setTesterFullName(user.getFullName());
        dto.setProfileComplete(user.isProfileComplete());

        // Максимальный балл (количество вопросов)
        int maxScore = attempt.getTest().getQuestions() != null ?
                attempt.getTest().getQuestions().size() : 0;
        dto.setMaxScore(maxScore);

        // Процент
        if (maxScore > 0) {
            dto.setPercentage((double) dto.getScore() / maxScore * 100);
        } else {
            dto.setPercentage(0.0);
        }

        // Длительность
        if (attempt.getStartTime() != null && attempt.getEndTime() != null) {
            dto.setDurationMinutes(Duration.between(attempt.getStartTime(), attempt.getEndTime()).toMinutes());
        } else {
            dto.setDurationMinutes(0L);
        }

        // Статус ограничений
        dto.setCooldownStatus(determineCooldownStatus(attempt, userIdsWithExceptions));

        return dto;
    }

    private String determineCooldownStatus(TestAttempt attempt, Set<Long> userIdsWithExceptions) {
        Long userId = attempt.getUser().getId();

        // Проверяем исключение
        if (userIdsWithExceptions.contains(userId)) {
            return "Исключение";
        }

        // Проверяем ограничения
        Test test = attempt.getTest();
        if (test.hasRetryCooldown()) {
            // Проверяем, есть ли завершенные попытки
            long completedAttempts = testAttemptRepository.countByTestIdAndUserIdAndStatus(
                    test.getId(),
                    userId,
                    TestAttempt.AttemptStatus.COMPLETED
            );

            if (completedAttempts > 0) {
                return "Ограничение";
            }
        }

        return "Доступен";
    }




    private TesterDetailedAnswersDTO buildTesterDetailedAnswersDTO(TestAttempt attempt) {
        TesterDetailedAnswersDTO dto = new TesterDetailedAnswersDTO();
        dto.setAttemptId(attempt.getId());
        dto.setTesterUsername(attempt.getUser().getUsername());
        dto.setStartTime(attempt.getStartTime());
        dto.setEndTime(attempt.getEndTime());

        List<UserAnswer> userAnswers = userAnswerRepository.findByAttemptIdWithDetails(attempt.getId());

        List<QuestionAnswerDetailDTO> answerDetails = userAnswers.stream()
                .map(this::convertToQuestionAnswerDetailDTO)
                .collect(Collectors.toList());

        dto.setQuestionAnswers(answerDetails);
        dto.setSummary(calculateSummary(attempt, answerDetails));

        return dto;
    }

    private QuestionAnswerDetailDTO convertToQuestionAnswerDetailDTO(UserAnswer userAnswer) {
        QuestionAnswerDetailDTO dto = new QuestionAnswerDetailDTO();
        dto.setQuestionId(userAnswer.getQuestion().getId());
        dto.setQuestionText(userAnswer.getQuestion().getText());
        dto.setQuestionOrder(userAnswer.getQuestion().getOrderIndex());

        // Выбранный ответ
        if (userAnswer.getChosenAnswerOption() != null) {
            AnswerDetailDTO chosenAnswer = new AnswerDetailDTO();
            chosenAnswer.setAnswerId(userAnswer.getChosenAnswerOption().getId());
            chosenAnswer.setAnswerText(userAnswer.getChosenAnswerOption().getText());
            chosenAnswer.setIsCorrect(userAnswer.getChosenAnswerOption().getIsCorrect());
            dto.setChosenAnswer(chosenAnswer);
        }

        // Находим правильный ответ
        AnswerOption correctAnswer = findCorrectAnswer(userAnswer.getQuestion());
        if (correctAnswer != null) {
            AnswerDetailDTO correctAnswerDTO = new AnswerDetailDTO();
            correctAnswerDTO.setAnswerId(correctAnswer.getId());
            correctAnswerDTO.setAnswerText(correctAnswer.getText());
            correctAnswerDTO.setIsCorrect(true);
            dto.setCorrectAnswer(correctAnswerDTO);
        }

        dto.setIsCorrect(Boolean.TRUE.equals(userAnswer.getIsCorrect()));
        dto.setPointsEarned(userAnswer.getPointsEarned() != null ? userAnswer.getPointsEarned() : 0);

        return dto;
    }

    private AnswerOption findCorrectAnswer(Question question) {
        if (question.getAnswerOptions() == null || question.getAnswerOptions().isEmpty()) {
            return null;
        }

        return question.getAnswerOptions().stream()
                .filter(AnswerOption::getIsCorrect)
                .findFirst()
                .orElse(null);
    }

    private TestSummaryDTO calculateSummary(TestAttempt attempt, List<QuestionAnswerDetailDTO> answers) {
        TestSummaryDTO summary = new TestSummaryDTO();

        int totalQuestions = attempt.getTest().getQuestions() != null ?
                attempt.getTest().getQuestions().size() : 0;

        int answeredQuestions = (int) answers.stream()
                .filter(a -> a.getChosenAnswer() != null)
                .count();

        int correctAnswers = (int) answers.stream()
                .filter(QuestionAnswerDetailDTO::getIsCorrect)
                .count();

        summary.setTotalQuestions(totalQuestions);
        summary.setAnsweredQuestions(answeredQuestions);
        summary.setCorrectAnswers(correctAnswers);
        summary.setTotalScore(attempt.getTotalScore() != null ? attempt.getTotalScore() : 0);

        if (totalQuestions > 0) {
            summary.setPercentage((double) correctAnswers / totalQuestions * 100);
        } else {
            summary.setPercentage(0.0);
        }

        return summary;
    }

    private TestSummaryDTO calculateTestSummary(Test test, List<TestAttempt> attempts) {
        TestSummaryDTO summary = new TestSummaryDTO();

        int totalQuestions = test.getQuestions() != null ? test.getQuestions().size() : 0;
        long totalAttempts = attempts.size();
        long completedAttempts = attempts.stream()
                .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.COMPLETED)
                .count();

        double totalScore = attempts.stream()
                .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.COMPLETED)
                .mapToInt(a -> a.getTotalScore() != null ? a.getTotalScore() : 0)
                .average()
                .orElse(0.0);

        summary.setTotalQuestions(totalQuestions);
        summary.setTotalScore((int) totalScore);
        summary.setCorrectAnswers((int) totalScore);

        if (completedAttempts > 0 && totalQuestions > 0) {
            summary.setPercentage(totalScore / totalQuestions * 100);
        } else {
            summary.setPercentage(0.0);
        }

        return summary;
    }

    private void validateTestExists(Long testId, String creatorUsername) {
        if (!testRepository.existsById(testId)) {
            throw new RuntimeException("Тест не найден");
        }
    }

    /**
     * Подсчет уникальных тестировщиков
     */
    @Transactional(readOnly = true)
    public long getTotalTesters() {
        return userRepository.countAllUsers();
    }
}