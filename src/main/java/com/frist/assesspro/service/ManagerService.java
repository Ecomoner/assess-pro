package com.frist.assesspro.service;


import com.frist.assesspro.dto.manager.ProjectDTO;
import com.frist.assesspro.dto.statistics.*;
import com.frist.assesspro.entity.Project;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.mapper.ProjectMapper;
import com.frist.assesspro.repository.ProjectRepository;
import com.frist.assesspro.repository.TestAttemptRepository;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {
    private final ProjectMapper projectMapper;

    private final UserRepository userRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final ProjectRepository projectRepository;
    private final TesterStatisticsService testerStatisticsService;

    @Transactional(readOnly = true)
    public List<ProjectDTO> getManagerProjects(String username) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Менеджер не найден"));
        return projectRepository.findByManagerId(manager.getId())
                .stream().map(projectMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getTesterCount(Long projectId) {
        return userRepository.countByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public ProjectAggregatedStatsDTO getProjectAggregatedStats(Long projectId) {
        return testAttemptRepository.getAggregatedStatsByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<TesterProjectStatsProjection> getTesterStatsByProject(Long projectId) {
        return testAttemptRepository.findTesterStatsByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getManagerTestersIds(String username) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Менеджер не найден"));

        List<Project> projects = projectRepository.findByManagerId(manager.getId());
        Set<Long> testerIds = new HashSet<>();
        for (Project project : projects) {
            List<User> testers = userRepository.findByProjectId(project.getId());
            testers.stream().map(User::getId).forEach(testerIds::add);
        }
        return testerIds;
    }

    @Transactional(readOnly = true)
    public Page<TesterAttemptDTO> getFilteredTestersByTest(Long testId, String managerUsername, Pageable pageable) {
        Set<Long> testerIds = getManagerTestersIds(managerUsername);
        Page<TestAttempt> attemptsPage = testAttemptRepository.findAttemptsByTestIdAndUserIds(testId, testerIds, pageable);
        return attemptsPage.map(testerStatisticsService::convertToTesterAttemptDTO);
    }

    @Transactional(readOnly = true)
    public TesterDetailedAnswersDTO getFilteredTesterDetailedAnswers(Long attemptId, String managerUsername) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Попытка не найдена"));
        Set<Long> testerIds = getManagerTestersIds(managerUsername);
        if (!testerIds.contains(attempt.getUser().getId())) {
            throw new RuntimeException("Доступ запрещён");
        }
        return testerStatisticsService.getTesterDetailedAnswers(attemptId, managerUsername);
    }

    @Transactional(readOnly = true)
    public TestSummaryDTO getFilteredTestSummary(Long testId, String managerUsername) {
        Set<Long> testerIds = getManagerTestersIds(managerUsername);
        List<TestAttempt> attempts = testAttemptRepository.findByTestId(testId).stream()
                .filter(a -> testerIds.contains(a.getUser().getId()))
                .collect(Collectors.toList());
        return testerStatisticsService.getFilteredTestSummary(testId, testerIds);
    }

    @Transactional(readOnly = true)
    public List<TesterAttemptDTO> getRecentAttemptsForManager(String managerUsername, int limit) {
        Set<Long> testerIds = getManagerTestersIds(managerUsername);
        if (testerIds.isEmpty()) {
            return List.of();
        }

        List<TestAttempt> attempts = testAttemptRepository.findTopByUserIdsOrderByStartTimeDesc(testerIds);
        return attempts.stream()
                .limit(limit)
                .map(testerStatisticsService::convertToTesterAttemptDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public User getTesterWithAccessCheck(String testerUsername, String managerUsername) {
        Set<Long> testerIds = getManagerTestersIds(managerUsername);
        User tester = userRepository.findByUsername(testerUsername)
                .orElseThrow(() -> new RuntimeException("Тестировщик не найден"));
        if (!testerIds.contains(tester.getId())) {
            throw new RuntimeException("Нет доступа к статистике этого тестировщика");
        }
        return tester;
    }

    @Transactional(readOnly = true)
    public List<TesterAttemptDTO> getAllFilteredTestersByTest(Long testId, String managerUsername) {
        Set<Long> testerIds = getManagerTestersIds(managerUsername);
        List<TestAttempt> attempts = testAttemptRepository.findByTestIdAndUserIds(testId, testerIds);
        return attempts.stream()
                .map(testerStatisticsService::convertToTesterAttemptDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TesterAttemptDTO> getAllAttemptsByTesterForManager(String testerUsername, String managerUsername) {
        User tester = getTesterWithAccessCheck(testerUsername, managerUsername);
        return testerStatisticsService.getAllAttemptsByTester(testerUsername);
    }

    /**
     * Получить всех менеджеров, ответственных за данного тестировщика.
     * Фактически — менеджер его проекта (если проект назначен и у проекта есть менеджер).
     */
    @Transactional(readOnly = true)
    public List<User> getManagersForTester(Long testerId) {
        User tester = userRepository.findById(testerId)
                .orElseThrow(() -> new RuntimeException("Тестировщик не найден"));
        if (tester.getProject() == null) return List.of();
        User manager = tester.getProject().getManager();
        if (manager == null) return List.of();
        return List.of(manager);
    }

}
