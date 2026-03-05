package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.statistics.QuestionAnswerDetailDTO;
import com.frist.assesspro.dto.statistics.TesterDetailedAnswersDTO;
import com.frist.assesspro.dto.statistics.TesterStatisticsDTO;
import com.frist.assesspro.dto.statistics.TestSummaryDTO;
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
@DisplayName("Тесты контроллера статистики тестировщиков (TesterStatisticsController)")
class TesterStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TesterStatisticsService testerStatisticsService;

    @MockitoBean
    private TestService testService;

    @MockitoBean
    private UserService userService;

    private final Long TEST_ID = 100L;
    private final Long ATTEMPT_ID = 1L;
    private final String TESTER_USERNAME = "tester1";

    private Test test;
    private User tester;

    @BeforeEach
    void setUp() {
        test = new Test();
        test.setId(TEST_ID);
        test.setTitle("Sample Test");

        tester = new User();
        tester.setUsername(TESTER_USERNAME);
        tester.setFirstName("John");
        tester.setLastName("Doe");
    }

    // ---------- GET /creator/tests/{testId}/statistics/testers ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/testers: должен вернуть view со списком тестировщиков")
    void getTestersList_Success_ShouldReturnView() throws Exception {
        int page = 0, size = 20;
        Page<TesterStatisticsDTO> pageResult = createTesterStatisticsPage(page, size);
        TestSummaryDTO summary = new TestSummaryDTO();

        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(testerStatisticsService.getTestSummary(eq(TEST_ID), anyString())).thenReturn(summary);
        when(testerStatisticsService.getTestersStatistics(eq(TEST_ID), anyString(), isNull(), any(PageRequest.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/creator/tests/{testId}/statistics/testers", TEST_ID)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/tester-statistics-main"))
                .andExpect(model().attribute("test", test))
                .andExpect(model().attribute("testers", pageResult.getContent()))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("testSummary", summary));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/testers с поиском: должен вернуть view с отфильтрованным списком")
    void getTestersList_WithSearch_ShouldReturnView() throws Exception {
        int page = 0, size = 20;
        String search = "john";
        Page<TesterStatisticsDTO> pageResult = createTesterStatisticsPage(page, size);
        TestSummaryDTO summary = new TestSummaryDTO();

        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(testerStatisticsService.getTestSummary(eq(TEST_ID), anyString())).thenReturn(summary);
        when(testerStatisticsService.getTestersStatistics(eq(TEST_ID), anyString(), eq(search), any(PageRequest.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/creator/tests/{testId}/statistics/testers", TEST_ID)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("search", search))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/tester-statistics-main"))
                .andExpect(model().attribute("search", search));
    }

    // ---------- GET /creator/tests/{testId}/statistics/tester/{attemptId} ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/tester/{attemptId}: должен вернуть view с детальными ответами")
    void getTesterDetailedAnswers_Success_ShouldReturnView() throws Exception {
        TesterDetailedAnswersDTO detailed = createTesterDetailedAnswersDTO();

        when(testerStatisticsService.getTesterDetailedAnswers(eq(ATTEMPT_ID), anyString())).thenReturn(detailed);
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(userService.findByUsername(TESTER_USERNAME)).thenReturn(java.util.Optional.of(tester));

        mockMvc.perform(get("/creator/tests/{testId}/statistics/tester/{attemptId}", TEST_ID, ATTEMPT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/tester-detailed-answers"))
                .andExpect(model().attribute("test", test))
                .andExpect(model().attribute("detailedAnswers", detailed))
                .andExpect(model().attribute("testerFullName", "Doe John"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/tester/{attemptId}: при ошибке должен редиректить с сообщением")
    void getTesterDetailedAnswers_Error_ShouldRedirectWithMessage() throws Exception {
        when(testerStatisticsService.getTesterDetailedAnswers(eq(ATTEMPT_ID), anyString()))
                .thenThrow(new RuntimeException("Attempt not found"));

        mockMvc.perform(get("/creator/tests/{testId}/statistics/tester/{attemptId}", TEST_ID, ATTEMPT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/statistics/testers"));
    }

    // ---------- GET /creator/tests/{testId}/statistics/tester/{attemptId}/export ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/tester/{attemptId}/export: должен вернуть PDF")
    void exportTesterResults_Success_ShouldReturnPdf() throws Exception {
        TesterDetailedAnswersDTO detailed = createTesterDetailedAnswersDTO();

        when(testerStatisticsService.getTesterDetailedAnswers(eq(ATTEMPT_ID), anyString())).thenReturn(detailed);
        when(userService.findByUsername(TESTER_USERNAME)).thenReturn(java.util.Optional.of(tester));

        mockMvc.perform(get("/creator/tests/{testId}/statistics/tester/{attemptId}/export", TEST_ID, ATTEMPT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"attempt_" + ATTEMPT_ID + "_")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.endsWith(".pdf\"")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/tester/{attemptId}/export: при ошибке должен вернуть bad request")
    void exportTesterResults_Error_ShouldReturnBadRequest() throws Exception {
        when(testerStatisticsService.getTesterDetailedAnswers(eq(ATTEMPT_ID), anyString()))
                .thenThrow(new RuntimeException("Export failed"));

        mockMvc.perform(get("/creator/tests/{testId}/statistics/tester/{attemptId}/export", TEST_ID, ATTEMPT_ID))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /creator/tests/{testId}/statistics/tester/{attemptId}/quick-view ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/statistics/tester/{attemptId}/quick-view: должен вернуть JSON")
    void getQuickView_Success_ShouldReturnJson() throws Exception {
        TesterDetailedAnswersDTO detailed = createTesterDetailedAnswersDTO();

        when(testerStatisticsService.getTesterDetailedAnswers(eq(ATTEMPT_ID), anyString())).thenReturn(detailed);

        mockMvc.perform(get("/creator/tests/{testId}/statistics/tester/{attemptId}/quick-view", TEST_ID, ATTEMPT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.attemptId").value(ATTEMPT_ID))
                .andExpect(jsonPath("$.testerUsername").value(TESTER_USERNAME));
    }

    // ---------- Вспомогательные методы ----------

    private Page<TesterStatisticsDTO> createTesterStatisticsPage(int page, int size) {
        TesterStatisticsDTO dto = new TesterStatisticsDTO();
        dto.setAttemptId(ATTEMPT_ID);
        dto.setTesterUsername(TESTER_USERNAME);
        dto.setTesterFullName("John Doe");
        dto.setPercentage(75.0);
        dto.setDurationMinutes(10L);
        return new PageImpl<>(List.of(dto), PageRequest.of(page, size), 1);
    }

    private TesterDetailedAnswersDTO createTesterDetailedAnswersDTO() {
        TesterDetailedAnswersDTO dto = new TesterDetailedAnswersDTO();
        dto.setAttemptId(ATTEMPT_ID);
        dto.setTesterUsername(TESTER_USERNAME);
        dto.setStartTime(LocalDateTime.now().minusMinutes(15));
        dto.setEndTime(LocalDateTime.now());

        TestSummaryDTO summary = new TestSummaryDTO();
        summary.setTotalQuestions(10);
        summary.setCorrectAnswers(7);
        summary.setAnsweredQuestions(7);
        summary.setTotalScore(7);
        summary.setPercentage(70.0);
        dto.setSummary(summary);

        QuestionAnswerDetailDTO qa = new QuestionAnswerDetailDTO();
        qa.setQuestionText("Sample question");
        qa.setIsCorrect(true);
        dto.setQuestionAnswers(List.of(qa));

        return dto;
    }
}