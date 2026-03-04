package com.frist.assesspro.service;

import com.frist.assesspro.dto.statistics.*;
import com.frist.assesspro.entity.*;
import com.frist.assesspro.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TesterStatisticsServiceTest {

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private UserAnswerRepository userAnswerRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RetryCooldownExceptionRepository exceptionRepository;

    @InjectMocks
    private TesterStatisticsService testerStatisticsService;

    private User creator;
    private User tester;
    private com.frist.assesspro.entity.Test test;
    private TestAttempt attempt;
    private Question question;
    private AnswerOption answerOption;
    private UserAnswer userAnswer;
    private RetryCooldownException exception;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");

        tester = new User();
        tester.setId(2L);
        tester.setUsername("tester");
        tester.setFirstName("Иван");
        tester.setLastName("Петров");
        tester.setMiddleName("Сергеевич");

        test = new com.frist.assesspro.entity.Test();
        test.setId(1L);
        test.setTitle("Тест по математике");
        test.setCreatedBy(creator);
        test.setRetryCooldownHours(24);

        question = new Question();
        question.setId(1L);
        question.setText("2 + 2 = ?");
        question.setOrderIndex(1);
        question.setTest(test);
        test.setQuestions(List.of(question));

        answerOption = new AnswerOption();
        answerOption.setId(1L);
        answerOption.setText("4");
        answerOption.setIsCorrect(true);
        question.setAnswerOptions(List.of(answerOption));

        attempt = new TestAttempt();
        attempt.setId(1L);
        attempt.setTest(test);
        attempt.setUser(tester);
        attempt.setStartTime(LocalDateTime.now().minusHours(2));
        attempt.setEndTime(LocalDateTime.now().minusHours(1));
        attempt.setTotalScore(1);
        attempt.setStatus(TestAttempt.AttemptStatus.COMPLETED);

        userAnswer = new UserAnswer();
        userAnswer.setId(1L);
        userAnswer.setAttempt(attempt);
        userAnswer.setQuestion(question);
        userAnswer.setChosenAnswerOption(answerOption);
        userAnswer.setIsCorrect(true);
        userAnswer.setPointsEarned(1);

        exception = new RetryCooldownException();
        exception.setId(1L);
        exception.setTest(test);
        exception.setUser(tester);
        exception.setIsPermanent(true);
    }

    @Test
    @DisplayName("getTestersByTest: успешное получение списка тестировщиков с пагинацией")
    void getTestersByTest_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestAttempt> attemptsPage = new PageImpl<>(List.of(attempt), pageable, 1);

        when(testRepository.existsById(1L)).thenReturn(true);
        when(testAttemptRepository.findAttemptsByTestIdWithAllData(1L, pageable))
                .thenReturn(attemptsPage);

        Page<TesterAttemptDTO> result = testerStatisticsService.getTestersByTest(1L, "creator", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        TesterAttemptDTO dto = result.getContent().get(0);
        assertThat(dto.getAttemptId()).isEqualTo(1L);
        assertThat(dto.getTesterUsername()).isEqualTo("tester");
        assertThat(dto.getTesterFullName()).isEqualTo("Петров Иван Сергеевич");
        assertThat(dto.getTestId()).isEqualTo(1L);
        assertThat(dto.getTestTitle()).isEqualTo("Тест по математике");
        assertThat(dto.getScore()).isEqualTo(1);
        assertThat(dto.getMaxScore()).isEqualTo(1);
        assertThat(dto.getPercentage()).isEqualTo(100.0);
        assertThat(dto.getDurationMinutes()).isEqualTo(60L);
    }

    @Test
    @DisplayName("getTestersByTest: тест не найден -> ошибка")
    void getTestersByTest_TestNotFound_ThrowsException() {
        when(testRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> testerStatisticsService.getTestersByTest(99L, "creator", PageRequest.of(0, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Тест не найден");
    }

    @Test
    @DisplayName("getTesterDetailedAnswers: успешное получение детальных ответов")
    void getTesterDetailedAnswers_Success() {
        when(testAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(testRepository.existsById(1L)).thenReturn(true);
        when(userAnswerRepository.findByAttemptIdWithDetails(1L)).thenReturn(List.of(userAnswer));

        TesterDetailedAnswersDTO result = testerStatisticsService.getTesterDetailedAnswers(1L, "creator");

        assertThat(result).isNotNull();
        assertThat(result.getAttemptId()).isEqualTo(1L);
        assertThat(result.getTesterUsername()).isEqualTo("tester");
        assertThat(result.getQuestionAnswers()).hasSize(1);

        QuestionAnswerDetailDTO answerDetail = result.getQuestionAnswers().get(0);
        assertThat(answerDetail.getQuestionId()).isEqualTo(1L);
        assertThat(answerDetail.getQuestionText()).isEqualTo("2 + 2 = ?");
        assertThat(answerDetail.getChosenAnswer()).isNotNull();
        assertThat(answerDetail.getChosenAnswer().getAnswerText()).isEqualTo("4");
        assertThat(answerDetail.getCorrectAnswer()).isNotNull();
        assertThat(answerDetail.getIsCorrect()).isTrue();

        TestSummaryDTO summary = result.getSummary();
        assertThat(summary.getTotalQuestions()).isEqualTo(1);
        assertThat(summary.getCorrectAnswers()).isEqualTo(1);
        assertThat(summary.getPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getRecentTestAttemptsForCreator: успешное получение последних попыток")
    void getRecentTestAttemptsForCreator_Success() {
        when(testAttemptRepository.findRecentAttemptsForCreator("creator"))
                .thenReturn(List.of(attempt, attempt));

        List<TesterAttemptDTO> result = testerStatisticsService.getRecentTestAttemptsForCreator("creator", 2);

        assertThat(result).hasSize(2);
        verify(testAttemptRepository).findRecentAttemptsForCreator("creator");
    }

    @Test
    @DisplayName("getTestSummary: успешное получение общей статистики по тесту")
    void getTestSummary_Success() {
        when(testRepository.existsById(1L)).thenReturn(true);
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(testAttemptRepository.findByTestId(1L)).thenReturn(List.of(attempt));

        TestSummaryDTO result = testerStatisticsService.getTestSummary(1L, "creator");

        assertThat(result.getAverageScore()).isEqualTo(100.0);

    }

    @Test
    @DisplayName("getAggregatedTestersByTest: агрегация по тестировщикам")
    void getAggregatedTestersByTest_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(testRepository.existsById(1L)).thenReturn(true);
        when(testAttemptRepository.findByTestId(1L)).thenReturn(List.of(attempt));

        Page<TesterAggregatedStatsDTO> result = testerStatisticsService.getAggregatedTestersByTest(
                1L, "creator", pageable, null, null);

        assertThat(result.getContent()).hasSize(1);
        TesterAggregatedStatsDTO dto = result.getContent().get(0);
        assertThat(dto.getTesterUsername()).isEqualTo("tester");
        assertThat(dto.getTotalAttempts()).isEqualTo(1L);
        assertThat(dto.getCompletedAttempts()).isEqualTo(1L);
        assertThat(dto.getAveragePercentage()).isEqualTo(100.0);
        assertThat(dto.getBestPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getTesterAggregatedStats: статистика конкретного тестировщика")
    void getTesterAggregatedStats_Success() {
        when(testRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(testAttemptRepository.findByTestId(1L)).thenReturn(List.of(attempt));

        TesterAggregatedStatsDTO result = testerStatisticsService.getTesterAggregatedStats(1L, "tester", "creator");

        assertThat(result.getTesterUsername()).isEqualTo("tester");
        assertThat(result.getTesterId()).isEqualTo(2L);
        assertThat(result.getTotalAttempts()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getDistinctTestersByTest: список уникальных тестировщиков")
    void getDistinctTestersByTest_Success() {
        when(testRepository.existsById(1L)).thenReturn(true);
        when(testAttemptRepository.findByTestId(1L)).thenReturn(List.of(attempt, attempt));

        List<User> result = testerStatisticsService.getDistinctTestersByTest(1L, "creator");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getAllAttemptsByTester: все попытки тестировщика")
    void getAllAttemptsByTester_Success() {
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(testAttemptRepository.findByUserId(2L)).thenReturn(List.of(attempt));

        List<TesterAttemptDTO> result = testerStatisticsService.getAllAttemptsByTester("tester");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getTestersStatistics: статистика тестировщиков с поиском")
    void getTestersStatistics_WithSearch_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestAttempt> attemptsPage = new PageImpl<>(List.of(attempt), pageable, 1);

        when(testRepository.existsById(1L)).thenReturn(true);
        when(testAttemptRepository.searchByTestIdWithUser(eq(1L), eq("петров"), eq(pageable)))
                .thenReturn(attemptsPage);
        when(exceptionRepository.findUserIdsWithExceptions(1L)).thenReturn(Set.of());

        Page<TesterStatisticsDTO> result = testerStatisticsService.getTestersStatistics(
                1L, "creator", "петров", pageable);

        assertThat(result.getContent()).hasSize(1);
        TesterStatisticsDTO dto = result.getContent().get(0);
        assertThat(dto.getTesterUsername()).isEqualTo("tester");
        assertThat(dto.getTesterFullName()).isEqualTo("Петров Иван Сергеевич");
        assertThat(dto.getCooldownStatus()).isEqualTo("Доступен");
    }

    @Test
    @DisplayName("getTestersStatistics: без поиска")
    void getTestersStatistics_WithoutSearch_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestAttempt> attemptsPage = new PageImpl<>(List.of(attempt), pageable, 1);

        when(testRepository.existsById(1L)).thenReturn(true);
        when(testAttemptRepository.findByTestIdWithUser(1L, pageable))
                .thenReturn(attemptsPage);
        when(exceptionRepository.findUserIdsWithExceptions(1L)).thenReturn(Set.of(2L)); // есть исключение

        Page<TesterStatisticsDTO> result = testerStatisticsService.getTestersStatistics(
                1L, "creator", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCooldownStatus()).isEqualTo("Исключение");
    }

    @Test
    @DisplayName("getTotalTesters: общее количество тестировщиков")
    void getTotalTesters_Success() {
        when(userRepository.countAllUsers()).thenReturn(100L);

        long result = testerStatisticsService.getTotalTesters();

        assertThat(result).isEqualTo(100L);
    }
}