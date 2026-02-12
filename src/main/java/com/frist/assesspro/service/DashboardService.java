package com.frist.assesspro.service;

import com.frist.assesspro.dto.DashboardStatsDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = {"dashboardStats"})
public class DashboardService {

    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = "creatorStats", key = "#username", unless = "#result.totalTests == 0")
    @Transactional(readOnly = true)
    public DashboardStatsDTO getCreatorStats(String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 1. Используем оптимизированные запросы
        stats.setTotalTests(testRepository.countByCreator(creator));
        stats.setPublishedTests(testRepository.countPublishedByCreator(creator));

        // 2. Используем нативный запрос для подсчета вопросов
        stats.setTotalQuestions(countTotalQuestionsByCreator(creator));
        stats.setTotalCategories(categoryRepository.countByCreatedBy(creator));

        // 3. Получаем статистику попыток через оптимизированный запрос
        List<Object[]> attemptsStats = testAttemptRepository.findAggregatedStatsByCreator(creator.getId());

        if (!attemptsStats.isEmpty()) {
            Object[] statsRow = attemptsStats.get(0);
            stats.setTotalAttempts(((Number) statsRow[0]).longValue());
            stats.setCompletedTests(((Number) statsRow[1]).longValue());
            stats.setInProgressTests(((Number) statsRow[2]).longValue());
            stats.setTotalMinutes(((Number) statsRow[3]).longValue());
            stats.setBestScore(((Number) statsRow[4]).intValue());
        } else {
            stats.setTotalAttempts(0L);
            stats.setCompletedTests(0L);
            stats.setInProgressTests(0L);
            stats.setTotalMinutes(0L);
            stats.setBestScore(0);
        }

        return stats;
    }

    private Long countTotalQuestionsByCreator(User creator) {
        return testRepository.countQuestionsByCreatorId(creator.getId());
    }

    private Long countQuestionsByCreator(User creator) {
        return testRepository.findAllByCreatedBy(creator).stream()
                .mapToLong(test -> questionRepository.countByTestId(test.getId()))
                .sum();
    }

    @Cacheable(value = "testerStats", key = "#username", unless = "#result.totalAttempts == 0")
    @Transactional(readOnly = true)
    public DashboardStatsDTO getTesterStats(String username) {
        User tester = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        DashboardStatsDTO stats = new DashboardStatsDTO();

        // Используем оптимизированные запросы
        stats.setTotalAttempts(testAttemptRepository.countByUserId(tester.getId()));
        stats.setCompletedTests(testAttemptRepository.countByUserIdAndStatus(
                tester.getId(), TestAttempt.AttemptStatus.COMPLETED));
        stats.setInProgressTests(testAttemptRepository.countByUserIdAndStatus(
                tester.getId(), TestAttempt.AttemptStatus.IN_PROGRESS));

        // Средний балл
        Double avgScore = testAttemptRepository.findAverageScoreByUserId(tester.getId());
        stats.setAverageScore(avgScore != null ? avgScore.intValue() : 0);

        // Лучший балл
        Integer bestScore = testAttemptRepository.findBestScoreByUserId(tester.getId());
        stats.setBestScore(bestScore != null ? bestScore : 0);

        // Доступные тесты через DTO проекцию
        List<?> publishedTests = testRepository.findPublishedTestInfoDTOs();
        stats.setAvailableTests((long) publishedTests.size());

        return stats;
    }

    private void calculateAttemptStats(DashboardStatsDTO stats, List<TestAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            stats.setTotalAttempts(0L);
            stats.setCompletedTests(0L);
            stats.setInProgressTests(0L);
            stats.setTotalMinutes(0L);
            stats.setBestScore(0);
            return;
        }

        long totalAttempts = attempts.size();
        long completedTests = 0L;
        long inProgressTests = 0L;
        long totalMinutes = 0L;
        int bestScore = 0;
        double totalScore = 0.0;
        int completedCount = 0;

        for (TestAttempt attempt : attempts) {
            if (TestAttempt.AttemptStatus.COMPLETED.equals(attempt.getStatus())) {
                completedTests++;
                completedCount++;

                if (attempt.getTotalScore() != null) {
                    totalScore += attempt.getTotalScore();
                    bestScore = Math.max(bestScore, attempt.getTotalScore());
                }

                if (attempt.getStartTime() != null && attempt.getEndTime() != null) {
                    totalMinutes += java.time.Duration.between(
                            attempt.getStartTime(), attempt.getEndTime()
                    ).toMinutes();
                }

            } else if (TestAttempt.AttemptStatus.IN_PROGRESS.equals(attempt.getStatus())) {
                inProgressTests++;
            }
        }

        stats.setTotalAttempts(totalAttempts);
        stats.setCompletedTests(completedTests);
        stats.setInProgressTests(inProgressTests);
        stats.setTotalMinutes(totalMinutes);
        stats.setBestScore(bestScore);
        stats.setAverageScore(completedCount > 0 ? (int)(totalScore / completedCount) : 0);
    }

    @CacheEvict(value = {"creatorStats", "testerStats"}, key = "#username")
    public void clearStatsCache(String username) {
        log.debug("Кэш статистики для пользователя {} очищен", username);
    }
}