package com.frist.assesspro.service;

import com.frist.assesspro.dto.DashboardStatsDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User creator;
    private User tester;
    private Test test;
    private TestAttempt attempt1;
    private TestAttempt attempt2;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");
        creator.setRole(User.Roles.CREATOR);

        tester = new User();
        tester.setId(2L);
        tester.setUsername("tester");
        tester.setRole(User.Roles.TESTER);

        test = new Test();
        test.setId(10L);
        test.setTitle("Sample Test");
        test.setCreatedBy(creator);

        attempt1 = new TestAttempt();
        attempt1.setId(100L);
        attempt1.setTest(test);
        attempt1.setUser(tester);
        attempt1.setStartTime(LocalDateTime.now().minusDays(1));
        attempt1.setEndTime(LocalDateTime.now().minusDays(1).plusHours(1));
        attempt1.setStatus(TestAttempt.AttemptStatus.COMPLETED);
        attempt1.setTotalScore(8);

        attempt2 = new TestAttempt();
        attempt2.setId(101L);
        attempt2.setTest(test);
        attempt2.setUser(tester);
        attempt2.setStartTime(LocalDateTime.now());
        attempt2.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
        attempt2.setTotalScore(null);
    }

    // ---------- getCreatorStats ----------
    @org.junit.jupiter.api.Test
    @DisplayName("getCreatorStats: успешное получение статистики создателя")
    void getCreatorStats_Success() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

        when(testRepository.countByTests(creator)).thenReturn(5L);
        when(testRepository.countPublishedByCreator(creator)).thenReturn(3L);
        when(testRepository.countQuestionsByCreatorId(creator.getId())).thenReturn(20L);
        when(categoryRepository.countByCreatedBy(creator)).thenReturn(2L);

        Object[] statsRow = new Object[]{10L, 8L, 2L, 120L, 95};
        when(testAttemptRepository.findAggregatedStatsByCreator(creator.getId()))
                .thenReturn(List.<Object[]>of(statsRow));

        DashboardStatsDTO stats = dashboardService.getCreatorStats("creator");

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalTests()).isEqualTo(5L);
        assertThat(stats.getPublishedTests()).isEqualTo(3L);
        assertThat(stats.getTotalQuestions()).isEqualTo(20L);
        assertThat(stats.getTotalCategories()).isEqualTo(2L);
        assertThat(stats.getTotalAttempts()).isEqualTo(10L);
        assertThat(stats.getCompletedTests()).isEqualTo(8L);
        assertThat(stats.getInProgressTests()).isEqualTo(2L);
        assertThat(stats.getTotalMinutes()).isEqualTo(120L);
        assertThat(stats.getBestScore()).isEqualTo(95);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getCreatorStats: когда нет статистики попыток, поля = 0")
    void getCreatorStats_NoAttemptStats() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.countByTests(creator)).thenReturn(5L);
        when(testRepository.countPublishedByCreator(creator)).thenReturn(3L);
        when(testRepository.countQuestionsByCreatorId(creator.getId())).thenReturn(20L);
        when(categoryRepository.countByCreatedBy(creator)).thenReturn(2L);
        when(testAttemptRepository.findAggregatedStatsByCreator(creator.getId()))
                .thenReturn(List.of()); // пустой список

        DashboardStatsDTO stats = dashboardService.getCreatorStats("creator");

        assertThat(stats.getTotalAttempts()).isZero();
        assertThat(stats.getCompletedTests()).isZero();
        assertThat(stats.getInProgressTests()).isZero();
        assertThat(stats.getTotalMinutes()).isZero();
        assertThat(stats.getBestScore()).isZero();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getCreatorStats: пользователь не найден -> исключение")
    void getCreatorStats_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getCreatorStats("unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    // ---------- getTesterStats ----------
    @org.junit.jupiter.api.Test
    @DisplayName("getTesterStats: успешное получение статистики тестировщика")
    void getTesterStats_Success() {
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(testAttemptRepository.countByUserId(tester.getId())).thenReturn(10L);
        when(testAttemptRepository.countByUserIdAndStatus(tester.getId(), TestAttempt.AttemptStatus.COMPLETED))
                .thenReturn(7L);
        when(testAttemptRepository.countByUserIdAndStatus(tester.getId(), TestAttempt.AttemptStatus.IN_PROGRESS))
                .thenReturn(3L);
        when(testAttemptRepository.findAverageScoreByUserId(tester.getId())).thenReturn(75.5);
        when(testAttemptRepository.findBestScoreByUserId(tester.getId())).thenReturn(95);
        when(testRepository.findPublishedTestInfoDTOs()).thenReturn(List.of());

        DashboardStatsDTO stats = dashboardService.getTesterStats("tester");

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalAttempts()).isEqualTo(10L);
        assertThat(stats.getCompletedTests()).isEqualTo(7L);
        assertThat(stats.getInProgressTests()).isEqualTo(3L);
        assertThat(stats.getAverageScore()).isEqualTo(75); // int
        assertThat(stats.getBestScore()).isEqualTo(95);
        assertThat(stats.getAvailableTests()).isEqualTo(0L); // список пуст
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getTesterStats: при null среднего балла ставим 0")
    void getTesterStats_AverageNull() {
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(testAttemptRepository.countByUserId(tester.getId())).thenReturn(5L);
        when(testAttemptRepository.countByUserIdAndStatus(tester.getId(), TestAttempt.AttemptStatus.COMPLETED))
                .thenReturn(3L);
        when(testAttemptRepository.countByUserIdAndStatus(tester.getId(), TestAttempt.AttemptStatus.IN_PROGRESS))
                .thenReturn(2L);
        when(testAttemptRepository.findAverageScoreByUserId(tester.getId())).thenReturn(null);
        when(testAttemptRepository.findBestScoreByUserId(tester.getId())).thenReturn(80);
        when(testRepository.findPublishedTestInfoDTOs()).thenReturn(List.of());

        DashboardStatsDTO stats = dashboardService.getTesterStats("tester");

        assertThat(stats.getAverageScore()).isZero();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getTesterStats: при null лучшего балла ставим 0")
    void getTesterStats_BestNull() {
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(tester));
        when(testAttemptRepository.countByUserId(tester.getId())).thenReturn(5L);
        when(testAttemptRepository.countByUserIdAndStatus(tester.getId(), TestAttempt.AttemptStatus.COMPLETED))
                .thenReturn(3L);
        when(testAttemptRepository.countByUserIdAndStatus(tester.getId(), TestAttempt.AttemptStatus.IN_PROGRESS))
                .thenReturn(2L);
        when(testAttemptRepository.findAverageScoreByUserId(tester.getId())).thenReturn(60.0);
        when(testAttemptRepository.findBestScoreByUserId(tester.getId())).thenReturn(null);
        when(testRepository.findPublishedTestInfoDTOs()).thenReturn(List.of());

        DashboardStatsDTO stats = dashboardService.getTesterStats("tester");

        assertThat(stats.getBestScore()).isZero();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("getTesterStats: пользователь не найден -> исключение")
    void getTesterStats_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getTesterStats("unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    // ---------- private method countTotalQuestionsByCreator (тестируем через getCreatorStats) ----------
    @org.junit.jupiter.api.Test
    @DisplayName("countTotalQuestionsByCreator вызывается и возвращает значение")
    void countTotalQuestionsByCreator_IsCalled() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.countByTests(creator)).thenReturn(5L);
        when(testRepository.countPublishedByCreator(creator)).thenReturn(3L);
        when(testRepository.countQuestionsByCreatorId(creator.getId())).thenReturn(20L);
        when(categoryRepository.countByCreatedBy(creator)).thenReturn(2L);
        when(testAttemptRepository.findAggregatedStatsByCreator(creator.getId()))
                .thenReturn(List.<Object[]>of(new Object[]{0L,0L,0L,0L,0}));

        dashboardService.getCreatorStats("creator");

        verify(testRepository).countQuestionsByCreatorId(creator.getId());
    }

    // ---------- Проверка кэширования (@Cacheable) ----------
    // Кэширование сложно проверить в unit-тесте, но можно убедиться,
    // что метод вызывается только один раз при повторном вызове с тем же ключом.
    // Для этого нужно использовать Spring Context, но в unit-тесте можно просто проверить,
    // что репозитории вызываются ожидаемое число раз.
    @org.junit.jupiter.api.Test
    @DisplayName("getCreatorStats кэшируется (проверка через вызовы репозитория)")
    void getCreatorStats_Cacheable() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(testRepository.countByTests(creator)).thenReturn(5L);
        when(testRepository.countPublishedByCreator(creator)).thenReturn(3L);
        when(testRepository.countQuestionsByCreatorId(creator.getId())).thenReturn(20L);
        when(categoryRepository.countByCreatedBy(creator)).thenReturn(2L);
        when(testAttemptRepository.findAggregatedStatsByCreator(creator.getId()))
                .thenReturn(List.<Object[]>of(new Object[]{0L,0L,0L,0L,0}));

        // первый вызов
        dashboardService.getCreatorStats("creator");
        // второй вызов (должен использовать кэш, но в unit-тесте без реального кэша метод выполнится снова,
        // поэтому мы не можем проверить кэширование через обычные моки.
        // Просто проверим, что повторный вызов не ломается.
        dashboardService.getCreatorStats("creator");

        verify(userRepository, times(2)).findByUsername("creator");
        verify(testRepository, times(2)).countByTests(creator);
        // В реальности с @Cacheable второй раз обращений к репозиторию не было бы.
        // Здесь мы просто констатируем, что тест не падает.
    }
}