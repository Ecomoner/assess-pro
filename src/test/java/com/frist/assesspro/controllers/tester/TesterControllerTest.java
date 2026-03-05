package com.frist.assesspro.controllers.tester;

import com.frist.assesspro.dto.*;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.test.*;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.repository.TestAttemptRepository;
import com.frist.assesspro.service.DashboardService;
import com.frist.assesspro.service.TestPassingService;
import com.frist.assesspro.service.UserService;
import com.frist.assesspro.service.metrics.MetricsService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.Optional;

import static com.frist.assesspro.util.TestConstants.STATUS_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "TESTER")
@DisplayName("Тесты контроллера тестировщика")
class TesterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestPassingService testPassingService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private TestAttemptRepository testAttemptRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MetricsService metricsService;

    private final String TEST_USERNAME = "user";
    private final Long TEST_ATTEMPT_ID = 1L;
    private final Long TEST_TEST_ID = 100L;

    // ---------- GET /tester/tests ----------

    @Test
    @DisplayName("GET /tester/tests: должен вернуть view с тестами (без поиска)")
    void testCatalog_WithoutSearch_ShouldReturnView() throws Exception {
        int page = 0, size = 12;
        Page<TestInfoDTO> pageResult = createTestInfoPage(page, size);
        when(testPassingService.getAllAvailableTestsDTOPaginated(eq(page), eq(size)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/tester/tests")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-catalog"))
                .andExpect(model().attribute("tests", pageResult.getContent()))
                .andExpect(model().attribute("testsPage", pageResult))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("pageSize", size));
    }

    @Test
    @DisplayName("GET /tester/tests: должен вернуть view с тестами (с поиском)")
    void testCatalog_WithSearch_ShouldReturnView() throws Exception {
        int page = 0, size = 12;
        String search = "math";
        Page<TestInfoDTO> pageResult = createTestInfoPage(page, size);
        when(testPassingService.searchTests(eq(search), eq(page), eq(size)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/tester/tests")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("search", search))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-catalog"))
                .andExpect(model().attribute("tests", pageResult.getContent()))
                .andExpect(model().attribute("testsPage", pageResult))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("pageSize", size))
                .andExpect(model().attribute("searchTerm", search));
    }

    // ---------- GET /tester/tests/search/quick ----------

    @Test
    @DisplayName("GET /tester/tests/search/quick: должен вернуть список тестов в JSON")
    void quickSearch_ShouldReturnJsonList() throws Exception {
        String term = "math";
        int limit = 5;
        List<TestInfoDTO> tests = List.of(createTestInfo());
        when(testPassingService.quickSearchTests(eq(term), eq(limit))).thenReturn(tests);

        mockMvc.perform(get("/tester/tests/search/quick")
                        .param("term", term)
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(tests.size()))
                .andExpect(jsonPath("$[0].id").value(tests.get(0).getId()));
    }

    // ---------- GET /tester/test/{testId}/start ----------

    @Test
    @DisplayName("GET /tester/test/{id}/start: должен создать попытку и редиректить на страницу прохождения")
    void startTest_Success_ShouldRedirectToAttempt() throws Exception {
        TestTakingDTO testTakingDTO = new TestTakingDTO();
        testTakingDTO.setAttemptId(TEST_ATTEMPT_ID);
        when(testPassingService.getTestForTaking(eq(TEST_TEST_ID), eq(TEST_USERNAME)))
                .thenReturn(Optional.of(testTakingDTO));

        mockMvc.perform(get("/tester/test/{testId}/start", TEST_TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tester/attempt/" + TEST_ATTEMPT_ID));

        verify(metricsService).incrementTestsStarted();
        verify(metricsService).incrementActiveUsers();
    }

    @Test
    @DisplayName("GET /tester/test/{id}/start: при ошибке должен редиректить с сообщением")
    void startTest_Error_ShouldRedirectWithMessage() throws Exception {
        when(testPassingService.getTestForTaking(eq(TEST_TEST_ID), eq(TEST_USERNAME)))
                .thenThrow(new RuntimeException("Test not found"));

        mockMvc.perform(get("/tester/test/{testId}/start", TEST_TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tester/tests"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- GET /tester/attempt/{attemptId} ----------

    @Test
    @DisplayName("GET /tester/attempt/{id}: должен показать страницу прохождения теста")
    void takeTest_WithValidAttempt_ShouldReturnView() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(TEST_ATTEMPT_ID);
        attempt.setUser(createUser());
        attempt.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
        attempt.setTest(createTest());

        TestTakingDTO testTakingDTO = new TestTakingDTO();
        testTakingDTO.setAttemptId(TEST_ATTEMPT_ID);
        testTakingDTO.setQuestions(List.of(new QuestionForTakingDTO()));
        testTakingDTO.setTotalQuestions(1);

        when(testAttemptRepository.findById(TEST_ATTEMPT_ID)).thenReturn(Optional.of(attempt));
        when(testPassingService.getTestForTaking(anyLong(), eq(TEST_USERNAME)))
                .thenReturn(Optional.of(testTakingDTO));

        mockMvc.perform(get("/tester/attempt/{attemptId}", TEST_ATTEMPT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-taking"))
                .andExpect(model().attribute("testTakingDTO", testTakingDTO));
    }

    @Test
    @DisplayName("GET /tester/attempt/{id}: если тест завершён, редирект на результаты")
    void takeTest_WhenCompleted_ShouldRedirectToResults() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(TEST_ATTEMPT_ID);
        attempt.setUser(createUser());
        attempt.setStatus(TestAttempt.AttemptStatus.COMPLETED);

        when(testAttemptRepository.findById(TEST_ATTEMPT_ID)).thenReturn(Optional.of(attempt));

        mockMvc.perform(get("/tester/attempt/{attemptId}", TEST_ATTEMPT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tester/attempt/" + TEST_ATTEMPT_ID + "/results"));
    }

    @Test
    @DisplayName("GET /tester/attempt/{id}: если нет вопросов, показать страницу ошибки")
    void takeTest_WhenNoQuestions_ShouldShowErrorView() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(TEST_ATTEMPT_ID);
        attempt.setUser(createUser());
        attempt.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
        attempt.setTest(createTest());

        TestTakingDTO testTakingDTO = new TestTakingDTO();
        testTakingDTO.setAttemptId(TEST_ATTEMPT_ID);
        testTakingDTO.setQuestions(null);

        when(testAttemptRepository.findById(TEST_ATTEMPT_ID)).thenReturn(Optional.of(attempt));
        when(testPassingService.getTestForTaking(anyLong(), eq(TEST_USERNAME)))
                .thenReturn(Optional.of(testTakingDTO));

        mockMvc.perform(get("/tester/attempt/{attemptId}", TEST_ATTEMPT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("error/general"));
    }

    // ---------- POST /tester/attempt/{attemptId}/answer ----------

    @Test
    @DisplayName("POST /tester/attempt/{id}/answer: успешное сохранение ответа")
    void saveAnswer_Success_ShouldReturnSuccessJson() throws Exception {
        TestPassingDTO dto = new TestPassingDTO();
        dto.setQuestionId(1L);
        dto.setAnswerOptionId(2L);

        mockMvc.perform(post("/tester/attempt/{attemptId}/answer", TEST_ATTEMPT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":1,\"answerOptionId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.completed").value(false));

        verify(testPassingService).saveAnswer(any(TestPassingDTO.class), eq(TEST_USERNAME));
    }

    @Test
    @DisplayName("POST /tester/attempt/{id}/answer: при исключении сервиса возвращает ошибку")
    void saveAnswer_WhenServiceThrowsException_ShouldReturnErrorJson() throws Exception {
        doThrow(new RuntimeException("DB error"))
                .when(testPassingService).saveAnswer(any(TestPassingDTO.class), eq(TEST_USERNAME));

        mockMvc.perform(post("/tester/attempt/{attemptId}/answer", TEST_ATTEMPT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":1,\"answerOptionId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // ---------- POST /tester/attempt/{attemptId}/finish ----------

    @Test
    @DisplayName("POST /tester/attempt/{id}/finish: завершение теста, редирект на результаты")
    void finishTest_Success_ShouldRedirectToResults() throws Exception {
        mockMvc.perform(post("/tester/attempt/{attemptId}/finish", TEST_ATTEMPT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tester/attempt/" + TEST_ATTEMPT_ID + "/results"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(testPassingService).finishTestAndGetResults(eq(TEST_ATTEMPT_ID), eq(TEST_USERNAME));
        verify(metricsService).incrementTestsCompleted();
        verify(metricsService).decrementActiveUsers();
    }

    @Test
    @DisplayName("POST /tester/attempt/{id}/finish: при ошибке редирект с сообщением")
    void finishTest_Error_ShouldRedirectWithMessage() throws Exception {
        doThrow(new RuntimeException("Finish error"))
                .when(testPassingService).finishTestAndGetResults(eq(TEST_ATTEMPT_ID), eq(TEST_USERNAME));

        mockMvc.perform(post("/tester/attempt/{attemptId}/finish", TEST_ATTEMPT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tester/attempt/" + TEST_ATTEMPT_ID))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- GET /tester/attempt/{attemptId}/results ----------

    @Test
    @DisplayName("GET /tester/attempt/{id}/results: должен показать результаты")
    void testResults_Success_ShouldReturnView() throws Exception {
        TestResultsDTO results = new TestResultsDTO();
        results.setAttemptId(TEST_ATTEMPT_ID);
        results.setQuestionResults(List.of());
        results.setTotalQuestions(10);
        results.setCorrectAnswers(7);
        results.setTotalScore(7);

        when(testPassingService.getTestResults(eq(TEST_ATTEMPT_ID), eq(TEST_USERNAME)))
                .thenReturn(results);

        mockMvc.perform(get("/tester/attempt/{attemptId}/results", TEST_ATTEMPT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-results"))
                .andExpect(model().attribute("results", results));
    }

    @Test
    @DisplayName("GET /tester/attempt/{id}/results: когда results == null, показать ошибку")
    void testResults_WhenNull_ShouldShowErrorView() throws Exception {
        when(testPassingService.getTestResults(eq(TEST_ATTEMPT_ID), eq(TEST_USERNAME)))
                .thenReturn(null);

        mockMvc.perform(get("/tester/attempt/{attemptId}/results", TEST_ATTEMPT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("error/general"));
    }

    @Test
    @DisplayName("GET /tester/attempt/{id}/results: при исключении показать общую ошибку")
    void testResults_WhenException_ShouldShowGeneralError() throws Exception {
        when(testPassingService.getTestResults(eq(TEST_ATTEMPT_ID), eq(TEST_USERNAME)))
                .thenThrow(new RuntimeException("Something went wrong"));

        mockMvc.perform(get("/tester/attempt/{attemptId}/results", TEST_ATTEMPT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("error/general"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    // ---------- GET /tester/attempt/{attemptId}/question/{questionIndex} ----------

    @Test
    @DisplayName("GET /tester/attempt/{id}/question/{index}: должен показать вопрос")
    void nextQuestion_WithValidIndex_ShouldReturnView() throws Exception {
        int questionIndex = 2;
        TestTakingDTO testTakingDTO = new TestTakingDTO();
        testTakingDTO.setAttemptId(TEST_ATTEMPT_ID);
        testTakingDTO.setQuestions(List.of(
                new QuestionForTakingDTO(),
                new QuestionForTakingDTO(),
                new QuestionForTakingDTO()
        ));
        testTakingDTO.setTotalQuestions(3);

        when(testPassingService.getTestForTakingByAttemptId(eq(TEST_ATTEMPT_ID), eq(TEST_USERNAME)))
                .thenReturn(Optional.of(testTakingDTO));

        mockMvc.perform(get("/tester/attempt/{attemptId}/question/{questionIndex}",
                        TEST_ATTEMPT_ID, questionIndex))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-taking"))
                .andExpect(model().attribute("testTakingDTO", testTakingDTO));
    }

    @Test
    @DisplayName("GET /tester/attempt/{id}/question/{index}: с некорректным индексом должен показать последний вопрос")
    void nextQuestion_WithInvalidIndex_ShouldShowLastQuestion() throws Exception {
        int questionIndex = 10; // больше, чем есть
        TestTakingDTO testTakingDTO = new TestTakingDTO();
        testTakingDTO.setAttemptId(TEST_ATTEMPT_ID);
        testTakingDTO.setQuestions(List.of(
                new QuestionForTakingDTO(),
                new QuestionForTakingDTO()
        ));
        testTakingDTO.setTotalQuestions(2);

        when(testPassingService.getTestForTakingByAttemptId(eq(TEST_ATTEMPT_ID), eq(TEST_USERNAME)))
                .thenReturn(Optional.of(testTakingDTO));

        mockMvc.perform(get("/tester/attempt/{attemptId}/question/{questionIndex}",
                        TEST_ATTEMPT_ID, questionIndex))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-taking"))
                .andExpect(model().attribute("testTakingDTO", testTakingDTO));
    }

    // ---------- GET /tester/history ----------

    @Test
    @DisplayName("GET /tester/history: должен вернуть страницу истории")
    void testHistory_ShouldReturnView() throws Exception {
        int page = 0, size = 10;
        Page<TestHistoryDTO> pageResult = createTestHistoryPage(page, size);
        UserStatisticsDTO statistics = new UserStatisticsDTO();

        when(testPassingService.getUserTestHistory(eq(TEST_USERNAME), eq(page), eq(size), isNull()))
                .thenReturn(pageResult);
        when(testPassingService.getUserStatistics(eq(TEST_USERNAME)))
                .thenReturn(statistics);

        mockMvc.perform(get("/tester/history")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-history"))
                .andExpect(model().attribute("history", pageResult.getContent()))
                .andExpect(model().attribute("historyPage", pageResult))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("pageSize", size))
                .andExpect(model().attribute("statistics", statistics));
    }

    // ---------- GET /tester/dashboard ----------

    @Test
    @DisplayName("GET /tester/dashboard: должен вернуть дашборд")
    void testerDashboard_Success_ShouldReturnView() throws Exception {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setTotalAttempts(10L);
        stats.setCompletedTests(5L);
        stats.setInProgressTests(2L);
        stats.setAverageScore(75);
        stats.setBestScore(100);
        stats.setAvailableTests(20L);

        TestHistoryDTO inProgress = new TestHistoryDTO();
        inProgress.setAttemptId(1L);
        inProgress.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
        inProgress.setTestTitle("Test 1");

        TestHistoryDTO completed = new TestHistoryDTO();
        completed.setAttemptId(2L);
        completed.setStatus(TestAttempt.AttemptStatus.COMPLETED);
        completed.setTestTitle("Test 2");

        List<TestHistoryDTO> allAttempts = List.of(inProgress, completed);
        List<TestHistoryDTO> inProgressAttempts = List.of(inProgress);
        List<TestHistoryDTO> recentCompleted = List.of(completed);

        List<CategoryDTO> categories = List.of(createCategoryDTO(1L, "Math"));
        List<TestInfoDTO> recommendedTests = List.of(createTestInfo());

        when(dashboardService.getTesterStats(eq(TEST_USERNAME))).thenReturn(stats);
        when(testPassingService.getUserTestHistory(eq(TEST_USERNAME))).thenReturn(allAttempts);
        when(testPassingService.getAvailableCategories()).thenReturn(categories);
        when(testPassingService.getAllAvailableTestsDTO()).thenReturn(recommendedTests);

        mockMvc.perform(get("/tester/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/dashboard"))
                .andExpect(model().attribute("stats", stats))
                .andExpect(model().attribute("recentAttempts", allAttempts))
                .andExpect(model().attribute("inProgressAttempts", inProgressAttempts))
                .andExpect(model().attribute("recentCompleted", recentCompleted))
                .andExpect(model().attribute("categories", categories))
                .andExpect(model().attribute("recommendedTests", recommendedTests))
                .andExpect(model().attribute("username", TEST_USERNAME));
    }

    @Test
    @DisplayName("GET /tester/dashboard: при ошибке должен показать дашборд с пустыми данными")
    void testerDashboard_Error_ShouldReturnViewWithEmptyData() throws Exception {
        when(dashboardService.getTesterStats(eq(TEST_USERNAME))).thenThrow(new RuntimeException("Stats error"));

        mockMvc.perform(get("/tester/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/dashboard"))
                .andExpect(model().attributeExists("stats", "recentAttempts", "inProgressAttempts",
                        "recentCompleted", "categories", "recommendedTests"))
                .andExpect(model().attribute("errorMessage", "Ошибка при загрузке данных: Stats error"));
    }

    // ---------- GET /tester/attempts/last-in-progress ----------

    @Test
    @DisplayName("GET /tester/attempts/last-in-progress: когда нет попыток, должен вернуть пустой ответ")
    void getLastInProgressAttempt_WhenNone_ShouldReturnEmpty() throws Exception {
        when(testPassingService.getUserTestHistory(eq(TEST_USERNAME)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tester/attempts/last-in-progress")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("GET /tester/attempts/last-in-progress: когда нет попыток, должен вернуть null")
    void getLastInProgressAttempt_WhenNone_ShouldReturnNull() throws Exception {
        when(testPassingService.getUserTestHistory(eq(TEST_USERNAME)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tester/attempts/last-in-progress"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    // ---------- Вспомогательные методы ----------

    private com.frist.assesspro.entity.User createUser() {
        com.frist.assesspro.entity.User user = new com.frist.assesspro.entity.User();
        user.setUsername(TEST_USERNAME);
        return user;
    }

    private com.frist.assesspro.entity.Test createTest() {
        com.frist.assesspro.entity.Test test = new com.frist.assesspro.entity.Test();
        test.setId(TEST_TEST_ID);
        return test;
    }

    private TestInfoDTO createTestInfo() {
        TestInfoDTO dto = new TestInfoDTO();
        dto.setId(1L);
        dto.setTitle("Test");
        return dto;
    }

    private Page<TestInfoDTO> createTestInfoPage(int page, int size) {
        return new PageImpl<>(List.of(createTestInfo()), PageRequest.of(page, size), 1);
    }

    private CategoryDTO createCategoryDTO(Long id, String name) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }

    private TestHistoryDTO createTestHistory() {
        TestHistoryDTO dto = new TestHistoryDTO();
        dto.setAttemptId(TEST_ATTEMPT_ID);
        dto.setStatus(TestAttempt.AttemptStatus.IN_PROGRESS);
        return dto;
    }

    private Page<TestHistoryDTO> createTestHistoryPage(int page, int size) {
        return new PageImpl<>(List.of(createTestHistory()), PageRequest.of(page, size), 1);
    }
}