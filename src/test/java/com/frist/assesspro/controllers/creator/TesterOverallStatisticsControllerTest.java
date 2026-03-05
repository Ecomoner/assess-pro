package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.statistics.TestTesterStatisticsDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
import com.frist.assesspro.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "CREATOR")
@DisplayName("Тесты контроллера общей статистики тестировщика (TesterOverallStatisticsController)")
class TesterOverallStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TesterStatisticsService testerStatisticsService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TestService testService;

    private final Long TESTER_ID = 1L;
    private final Long TEST_ID_1 = 100L;
    private final Long TEST_ID_2 = 101L;
    private final String TESTER_USERNAME = "tester1";
    private final String CREATOR_USERNAME = "user";

    private User tester;
    private Test test1;
    private Test test2;

    @BeforeEach
    void setUp() {
        tester = new User();
        tester.setId(TESTER_ID);
        tester.setUsername(TESTER_USERNAME);
        tester.setFirstName("John");
        tester.setLastName("Doe");
        tester.setRole("ROLE_TESTER");

        test1 = new Test();
        test1.setId(TEST_ID_1);
        test1.setTitle("Math Test");
        test1.setCreatedBy(new User());

        test2 = new Test();
        test2.setId(TEST_ID_2);
        test2.setTitle("Science Test");
        test2.setCreatedBy(new User());
    }

    // ---------- GET /creator/testers/{testerId}/statistics ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/testers/{testerId}/statistics: успешное получение статистики")
    void getTesterOverallStatistics_Success_ShouldReturnView() throws Exception {
        // Создаем тестовые попытки
        TesterAttemptDTO attempt1 = createAttempt(TEST_ID_1, 5, 10, 80.0, LocalDateTime.now().minusDays(1));
        TesterAttemptDTO attempt2 = createAttempt(TEST_ID_1, 7, 10, 70.0, LocalDateTime.now().minusDays(2));
        TesterAttemptDTO attempt3 = createAttempt(TEST_ID_2, 8, 10, 90.0, LocalDateTime.now().minusDays(3));
        List<TesterAttemptDTO> allAttempts = List.of(attempt1, attempt2, attempt3);

        // Настройка моков
        when(userService.getUserById(TESTER_ID)).thenReturn(Optional.of(tester));
        when(testerStatisticsService.getAllAttemptsByTester(TESTER_USERNAME)).thenReturn(allAttempts);
        when(testService.getTestBasicById(eq(TEST_ID_1), anyString())).thenReturn(Optional.of(test1));
        when(testService.getTestBasicById(eq(TEST_ID_2), anyString())).thenReturn(Optional.of(test2));

        mockMvc.perform(get("/creator/testers/{testerId}/statistics", TESTER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/tester-statistics"))
                .andExpect(model().attribute("tester", tester))
                .andExpect(model().attributeExists("testStatistics"))
                .andExpect(model().attribute("totalAttempts", 3L))
                .andExpect(model().attribute("completedAttempts", 3L))
                .andExpect(model().attribute("overallAverage", 80.0));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/testers/{testerId}/statistics: тестировщик не найден -> редирект на список тестировщиков")
    void getTesterOverallStatistics_TesterNotFound_ShouldRedirect() throws Exception {
        when(userService.getUserById(TESTER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/creator/testers/{testerId}/statistics", TESTER_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/testers"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/testers/{testerId}/statistics: пользователь не тестировщик -> редирект с ошибкой")
    void getTesterOverallStatistics_NotATester_ShouldRedirect() throws Exception {
        tester.setRole("ROLE_CREATOR"); // меняем роль
        when(userService.getUserById(TESTER_ID)).thenReturn(Optional.of(tester));

        mockMvc.perform(get("/creator/testers/{testerId}/statistics", TESTER_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/testers"));

    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/testers/{testerId}/statistics: общая ошибка сервиса -> редирект с ошибкой")
    void getTesterOverallStatistics_Error_ShouldRedirect() throws Exception {
        when(userService.getUserById(TESTER_ID)).thenReturn(Optional.of(tester));
        when(testerStatisticsService.getAllAttemptsByTester(TESTER_USERNAME))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/creator/testers/{testerId}/statistics", TESTER_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/testers"));
    }

    // ---------- Вспомогательные методы ----------

    private TesterAttemptDTO createAttempt(Long testId, int score, int maxScore, double percentage, LocalDateTime startTime) {
        TesterAttemptDTO dto = new TesterAttemptDTO();
        dto.setTestId(testId);
        dto.setScore(score);
        dto.setMaxScore(maxScore);
        dto.setPercentage(percentage);
        dto.setStartTime(startTime);
        dto.setEndTime(startTime.plusMinutes(15)); // условное окончание
        return dto;
    }
}