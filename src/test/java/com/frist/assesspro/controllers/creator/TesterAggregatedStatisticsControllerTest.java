package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.statistics.TesterAggregatedStatsDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "CREATOR")
@DisplayName("Тесты контроллера агрегированной статистики тестировщиков (TesterAggregatedStatisticsController)")
class TesterAggregatedStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TesterStatisticsService testerStatisticsService;

    @MockitoBean
    private TestService testService;

    private final Long TEST_ID = 100L;
    private final String TEST_TITLE = "Sample Test";
    private final String TESTER_USERNAME = "tester1";
    private final LocalDateTime DATE_FROM = LocalDateTime.now().minusDays(7);
    private final LocalDateTime DATE_TO = LocalDateTime.now();

    private Test test;

    @BeforeEach
    void setUp() {
        test = new Test();
        test.setId(TEST_ID);
        test.setTitle(TEST_TITLE);
    }

    // ---------- GET /creator/tests/{testId}/statistics/aggregated/testers ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/aggregated/testers: должен вернуть view с агрегированной статистикой")
    void getAggregatedTesters_Success_ShouldReturnView() throws Exception {
        int page = 0, size = 20;
        Page<TesterAggregatedStatsDTO> pageResult = createAggregatedPage(page, size);

        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(testerStatisticsService.getAggregatedTestersByTest(
                eq(TEST_ID), anyString(), any(PageRequest.class), isNull(), isNull()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/creator/tests/{testId}/statistics/aggregated/testers", TEST_ID)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/aggregated-testers-statistics"))
                .andExpect(model().attribute("test", test))
                .andExpect(model().attribute("aggregatedTesters", pageResult.getContent()))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/aggregated/testers с фильтрацией по датам: должен передать параметры")
    void getAggregatedTesters_WithDateFilters_ShouldPassParameters() throws Exception {
        int page = 0, size = 20;

        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(testerStatisticsService.getAggregatedTestersByTest(
                eq(TEST_ID), anyString(), any(PageRequest.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/creator/tests/{testId}/statistics/aggregated/testers", TEST_ID)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("dateFrom", DATE_FROM.toString())
                        .param("dateTo", DATE_TO.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/aggregated-testers-statistics"))
                .andExpect(model().attribute("dateFrom", DATE_FROM))
                .andExpect(model().attribute("dateTo", DATE_TO));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/aggregated/testers: при ошибке редирект на страницу статистики")
    void getAggregatedTesters_Error_ShouldRedirect() throws Exception {
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID))
                .thenThrow(new RuntimeException("Test not found"));

        mockMvc.perform(get("/creator/tests/{testId}/statistics/aggregated/testers", TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/statistics?error=Test not found"));
    }

    // ---------- GET /creator/tests/{testId}/statistics/aggregated/tester/{testerUsername} ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/aggregated/tester/{testerUsername}: должен вернуть view с детальной статистикой")
    void getTesterAggregatedStats_Success_ShouldReturnView() throws Exception {
        TesterAggregatedStatsDTO stats = createAggregatedStatsDTO();

        when(testService.getTestById(eq(TEST_ID), anyString())).thenReturn(java.util.Optional.of(test));
        when(testerStatisticsService.getTesterAggregatedStats(eq(TEST_ID), eq(TESTER_USERNAME), anyString()))
                .thenReturn(stats);

        mockMvc.perform(get("/creator/tests/{testId}/statistics/aggregated/tester/{testerUsername}", TEST_ID, TESTER_USERNAME))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/tester-aggregated-stats"))
                .andExpect(model().attribute("test", test))
                .andExpect(model().attribute("aggregatedStats", stats))
                .andExpect(model().attribute("testerUsername", TESTER_USERNAME));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/aggregated/tester/{testerUsername} с датами: должны передаваться в модель")
    void getTesterAggregatedStats_WithDates_ShouldPassToModel() throws Exception {
        TesterAggregatedStatsDTO stats = createAggregatedStatsDTO();

        when(testService.getTestById(eq(TEST_ID), anyString())).thenReturn(java.util.Optional.of(test));
        when(testerStatisticsService.getTesterAggregatedStats(eq(TEST_ID), eq(TESTER_USERNAME), anyString()))
                .thenReturn(stats);

        mockMvc.perform(get("/creator/tests/{testId}/statistics/aggregated/tester/{testerUsername}", TEST_ID, TESTER_USERNAME)
                        .param("dateFrom", DATE_FROM.toString())
                        .param("dateTo", DATE_TO.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/tester-aggregated-stats"))
                .andExpect(model().attribute("dateFrom", DATE_FROM))
                .andExpect(model().attribute("dateTo", DATE_TO));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/aggregated/tester/{testerUsername}: при ошибке редирект на список агрегированных")
    void getTesterAggregatedStats_Error_ShouldRedirect() throws Exception {
        when(testService.getTestById(eq(TEST_ID), anyString()))
                .thenThrow(new RuntimeException("Test not found"));

        mockMvc.perform(get("/creator/tests/{testId}/statistics/aggregated/tester/{testerUsername}", TEST_ID, TESTER_USERNAME))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/statistics/aggregated/testers?error=Test not found"));
    }

    // ---------- GET /creator/tests/{testId}/statistics/aggregated/tester/{testerUsername}/quick-view ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/aggregated/tester/{testerUsername}/quick-view: должен вернуть JSON")
    void getTesterAggregatedQuickView_Success_ShouldReturnJson() throws Exception {
        TesterAggregatedStatsDTO stats = createAggregatedStatsDTO();

        when(testerStatisticsService.getTesterAggregatedStats(eq(TEST_ID), eq(TESTER_USERNAME), anyString()))
                .thenReturn(stats);

        mockMvc.perform(get("/creator/tests/{testId}/statistics/aggregated/tester/{testerUsername}/quick-view", TEST_ID, TESTER_USERNAME)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.testerUsername").value(TESTER_USERNAME))
                .andExpect(jsonPath("$.totalAttempts").value(5))
                .andExpect(jsonPath("$.averagePercentage").value(75.0));
    }

    // ---------- Вспомогательные методы ----------

    private Page<TesterAggregatedStatsDTO> createAggregatedPage(int page, int size) {
        TesterAggregatedStatsDTO dto = createAggregatedStatsDTO();
        return new PageImpl<>(List.of(dto), PageRequest.of(page, size), 1);
    }

    private TesterAggregatedStatsDTO createAggregatedStatsDTO() {
        return new TesterAggregatedStatsDTO(
                TESTER_USERNAME,
                1L,           // userId
                5L,           // totalAttempts
                5L,           // completedAttempts
                75.0,         // averagePercentage
                100.0,        // bestPercentage
                50.0,         // worstPercentage
                60L,          // totalDurationMinutes
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now(),
                List.of(1L, 2L, 3L),
                List.of()     // recentAttempts
        );
    }
}