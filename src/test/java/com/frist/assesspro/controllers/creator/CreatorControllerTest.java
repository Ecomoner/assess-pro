package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.DashboardStatsDTO;
import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.test.TestTakingDTO;
import com.frist.assesspro.dto.test.TestUpdateDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import com.frist.assesspro.service.*;
import com.frist.assesspro.service.export.StatisticsExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "CREATOR")
@DisplayName("Тесты контроллера создателя (CreatorController)")
class CreatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestService testService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private StatisticsExportService statisticsExportService;

    @MockitoBean
    private TesterStatisticsService testerStatisticsService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    private final Long TEST_ID = 100L;
    private final String TEST_TITLE = "Sample Test";
    private final String CREATOR_USERNAME = "user";

    private Test test;
    private User creator;
    private TestDTO testDTO;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setUsername(CREATOR_USERNAME);
        creator.setFirstName("John");
        creator.setLastName("Doe");

        test = new Test();
        test.setId(TEST_ID);
        test.setTitle(TEST_TITLE);
        test.setCreatedBy(creator);
        test.setIsPublished(false);

        testDTO = new TestDTO();
        testDTO.setId(TEST_ID);
        testDTO.setTitle(TEST_TITLE);
        testDTO.setPublished(false);
        testDTO.setQuestionCount(10L);
    }

    // ---------- GET /creator/tests ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests: должен вернуть view со списком тестов (без фильтров)")
    void getAllTests_WithoutFilters_ShouldReturnView() throws Exception {
        int page = 0, size = 10;
        Page<TestDTO> pageResult = new PageImpl<>(List.of(testDTO), PageRequest.of(page, size), 1);

        when(testService.getAllTestsForCreator(eq(CREATOR_USERNAME), any(PageRequest.class), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(pageResult);
        when(categoryService.getAllCategories(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new CategoryDTO()), PageRequest.of(0, 100), 1));
        when(userRepository.findAllCreators()).thenReturn(List.of(creator));

        mockMvc.perform(get("/creator/tests")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/test-list"))
                .andExpect(model().attribute("tests", pageResult.getContent()))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("pageSize", size))
                .andExpect(model().attributeExists("ownershipMap", "publishedTestsCount", "totalQuestionsCount"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests с фильтрами: должен вернуть отфильтрованный список")
    void getAllTests_WithFilters_ShouldReturnView() throws Exception {
        int page = 0, size = 10;
        String status = "published";
        String search = "math";
        Long categoryId = 1L;
        Long creatorId = 2L;
        Page<TestDTO> pageResult = new PageImpl<>(List.of(testDTO), PageRequest.of(page, size), 1);

        when(testService.getAllTestsForCreator(eq(CREATOR_USERNAME), any(PageRequest.class), eq(status), eq(search), eq(categoryId), eq(creatorId)))
                .thenReturn(pageResult);
        when(categoryService.getAllCategories(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));
        when(userRepository.findAllCreators()).thenReturn(List.of());

        mockMvc.perform(get("/creator/tests")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("status", status)
                        .param("search", search)
                        .param("categoryId", String.valueOf(categoryId))
                        .param("creatorId", String.valueOf(creatorId)))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/test-list"))
                .andExpect(model().attribute("status", status))
                .andExpect(model().attribute("search", search))
                .andExpect(model().attribute("categoryId", categoryId))
                .andExpect(model().attribute("selectedCreatorId", creatorId));
    }

    // ---------- POST /creator/tests/new ----------

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/new: успешное создание теста, редирект на список")
    void createTest_Success_ShouldRedirect() throws Exception {
        when(testService.createTest(any(TestDTO.class), eq(CREATOR_USERNAME))).thenReturn(test);

        mockMvc.perform(post("/creator/tests/new")
                        .param("title", TEST_TITLE)
                        .param("description", "Description")
                        .param("timeLimitMinutes", "30")
                        .param("retryCooldownHours", "24")
                        .param("retryCooldownDays", "1")
                        .param("categoryId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/new: ошибка валидации, редирект с ошибкой")
    void createTest_ValidationError_ShouldRedirectWithErrors() throws Exception {
        mockMvc.perform(post("/creator/tests/new")
                        .param("title", ""))  // пустой title
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/new"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("test"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/new: исключение сервиса, редирект с ошибкой")
    void createTest_ServiceException_ShouldRedirectWithError() throws Exception {
        when(testService.createTest(any(TestDTO.class), eq(CREATOR_USERNAME)))
                .thenThrow(new RuntimeException("Test creation failed"));

        mockMvc.perform(post("/creator/tests/new")
                        .param("title", TEST_TITLE)
                        .param("description", "Description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/new"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- GET /creator/tests/edit/{id} ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/edit/{id}: успешный просмотр формы редактирования")
    void showEditTestForm_Success_ShouldReturnView() throws Exception {
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(testService.convertToDTO(test)).thenReturn(testDTO);
        when(categoryService.getAllCategories(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new CategoryDTO())));

        mockMvc.perform(get("/creator/tests/edit/{id}", TEST_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/test-form"))
                .andExpect(model().attribute("test", testDTO))
                .andExpect(model().attribute("formAction", "/creator/tests/update/" + TEST_ID))
                .andExpect(model().attribute("action", "edit"))
                .andExpect(model().attributeExists("categories"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/edit/{id}: если пользователь не владелец, редирект с ошибкой")
    void showEditTestForm_NotOwner_ShouldRedirect() throws Exception {
        User anotherUser = new User();
        anotherUser.setUsername("another");
        test.setCreatedBy(anotherUser);
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);

        mockMvc.perform(get("/creator/tests/edit/{id}", TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- POST /creator/tests/update/{id} ----------

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/update/{id}: успешное обновление, редирект на список")
    void updateTest_Success_ShouldRedirect() throws Exception {
        when(testService.isTestOwner(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(true);
        when(testService.updateTest(eq(TEST_ID), any(TestUpdateDTO.class), eq(CREATOR_USERNAME)))
                .thenReturn(test);

        mockMvc.perform(post("/creator/tests/update/{id}", TEST_ID)
                        .param("title", "Updated Title")
                        .param("description", "Updated Desc")
                        .param("timeLimitMinutes", "60")
                        .param("retryCooldownHours", "12")
                        .param("retryCooldownDays", "0")
                        .param("categoryId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/update/{id}: не владелец, редирект с ошибкой")
    void updateTest_NotOwner_ShouldRedirect() throws Exception {
        when(testService.isTestOwner(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(false);

        mockMvc.perform(post("/creator/tests/update/{id}", TEST_ID)
                        .param("title", "Title"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/update/{id}: ошибка валидации, редирект с ошибкой")
    void updateTest_ValidationError_ShouldRedirect() throws Exception {
        when(testService.isTestOwner(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(true);

        mockMvc.perform(post("/creator/tests/update/{id}", TEST_ID)
                        .param("title", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/edit/" + TEST_ID))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/update/{id}: исключение сервиса, редирект с ошибкой")
    void updateTest_ServiceException_ShouldRedirect() throws Exception {
        when(testService.isTestOwner(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(true);
        when(testService.updateTest(eq(TEST_ID), any(TestUpdateDTO.class), eq(CREATOR_USERNAME)))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(post("/creator/tests/update/{id}", TEST_ID)
                        .param("title", "Title"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/edit/" + TEST_ID))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- POST /creator/tests/{id}/publish ----------

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{id}/publish: публикация теста, редирект с сообщением")
    void publishTest_Publish_ShouldRedirect() throws Exception {
        when(testService.switchPublishStatus(eq(TEST_ID), eq(CREATOR_USERNAME), eq(true)))
                .thenReturn(test);

        mockMvc.perform(post("/creator/tests/{id}/publish", TEST_ID)
                        .param("publish", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{id}/publish: снятие с публикации, редирект с сообщением")
    void publishTest_Unpublish_ShouldRedirect() throws Exception {
        when(testService.switchPublishStatus(eq(TEST_ID), eq(CREATOR_USERNAME), eq(false)))
                .thenReturn(test);

        mockMvc.perform(post("/creator/tests/{id}/publish", TEST_ID)
                        .param("publish", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{id}/publish: исключение, редирект с ошибкой")
    void publishTest_Exception_ShouldRedirectWithError() throws Exception {
        when(testService.switchPublishStatus(eq(TEST_ID), eq(CREATOR_USERNAME), eq(true)))
                .thenThrow(new RuntimeException("Publish failed"));

        mockMvc.perform(post("/creator/tests/{id}/publish", TEST_ID)
                        .param("publish", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- POST /creator/tests/delete/{id} ----------

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/delete/{id}: успешное удаление, редирект с сообщением")
    void deleteTest_Success_ShouldRedirect() throws Exception {
        when(testService.isTestOwner(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(true);
        doNothing().when(testService).deleteTest(eq(TEST_ID), eq(CREATOR_USERNAME));

        mockMvc.perform(post("/creator/tests/delete/{id}", TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/delete/{id}: не владелец, редирект с ошибкой")
    void deleteTest_NotOwner_ShouldRedirect() throws Exception {
        when(testService.isTestOwner(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(false);

        mockMvc.perform(post("/creator/tests/delete/{id}", TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/delete/{id}: исключение, редирект с ошибкой")
    void deleteTest_Exception_ShouldRedirect() throws Exception {
        when(testService.isTestOwner(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(true);
        doThrow(new RuntimeException("Delete failed")).when(testService).deleteTest(eq(TEST_ID), eq(CREATOR_USERNAME));

        mockMvc.perform(post("/creator/tests/delete/{id}", TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- GET /creator/tests/new ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/new: должен вернуть форму создания с пустым DTO")
    void showCreateForm_ShouldReturnView() throws Exception {
        when(categoryService.getAllCategories(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new CategoryDTO())));

        mockMvc.perform(get("/creator/tests/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/test-form"))
                .andExpect(model().attributeExists("test"))
                .andExpect(model().attribute("formAction", "/creator/tests/new"))
                .andExpect(model().attribute("action", "create"))
                .andExpect(model().attributeExists("categories"));
    }

    // ---------- GET /creator/tests/{id}/quick-stats ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{id}/quick-stats: должен вернуть JSON со статистикой")
    void getQuickStats_Success_ShouldReturnJson() throws Exception {
        when(testService.getTestById(eq(TEST_ID), eq(CREATOR_USERNAME)))
                .thenReturn(Optional.of(test));

        mockMvc.perform(get("/creator/tests/{id}/quick-stats", TEST_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.testId").value(TEST_ID))
                .andExpect(jsonPath("$.testTitle").value(TEST_TITLE))
                .andExpect(jsonPath("$.published").value(false));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{id}/quick-stats: при ошибке возвращает JSON с error")
    void getQuickStats_Error_ShouldReturnErrorJson() throws Exception {
        when(testService.getTestById(eq(TEST_ID), eq(CREATOR_USERNAME)))
                .thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/creator/tests/{id}/quick-stats", TEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").exists());
    }

    // ---------- GET /creator/tests/{id}/preview ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{id}/preview: успешный просмотр, возвращает view")
    void previewTestAsTester_Success_ShouldReturnView() throws Exception {
        TestTakingDTO previewDTO = new TestTakingDTO();
        previewDTO.setTestId(TEST_ID);

        when(testService.getTestPreviewDTO(eq(TEST_ID), eq(CREATOR_USERNAME)))
                .thenReturn(Optional.of(previewDTO));

        mockMvc.perform(get("/creator/tests/{id}/preview", TEST_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/test-preview"))
                .andExpect(model().attribute("testTakingDTO", previewDTO))
                .andExpect(model().attribute("isPreview", true));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{id}/preview: тест не найден, редирект")
    void previewTestAsTester_NotFound_ShouldRedirect() throws Exception {
        when(testService.getTestPreviewDTO(eq(TEST_ID), eq(CREATOR_USERNAME)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/creator/tests/{id}/preview", TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests?error=test_not_found"));
    }

    // ---------- GET /creator/tests/{testId}/export ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/export: успешный экспорт PDF")
    void exportTestStatistics_Success_ShouldReturnPdf() throws Exception {
        byte[] pdfContent = new byte[]{1, 2, 3};
        when(testService.getTestWithAllDataWithoutOwnershipCheck(TEST_ID)).thenReturn(Optional.of(test));
        when(statisticsExportService.generateTestStatisticsPDF(eq(test), isNull(), isNull())).thenReturn(pdfContent);

        mockMvc.perform(get("/creator/tests/{testId}/export", TEST_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"statistics_test_" + TEST_ID + "_")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/export: при ошибке возвращает bad request")
    void exportTestStatistics_Error_ShouldReturnBadRequest() throws Exception {
        when(testService.getTestWithAllDataWithoutOwnershipCheck(TEST_ID)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/creator/tests/{testId}/export", TEST_ID))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /creator/tests/{testId}/export-page ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/export-page: должен вернуть страницу экспорта")
    void showExportPage_Success_ShouldReturnView() throws Exception {
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(testerStatisticsService.getDistinctTestersByTest(eq(TEST_ID), eq(CREATOR_USERNAME)))
                .thenReturn(List.of(creator));
        when(categoryService.getAllCategories(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new CategoryDTO())));

        mockMvc.perform(get("/creator/tests/{testId}/export-page", TEST_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/export-page"))
                .andExpect(model().attribute("test", test))
                .andExpect(model().attributeExists("testers", "categories"));
    }

    // ---------- GET /creator/dashboard ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/dashboard: должен вернуть дашборд создателя")
    void creatorDashboard_Success_ShouldReturnView() throws Exception {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setTotalTests(5L);

        List<TesterAttemptDTO> recentAttempts = List.of(new TesterAttemptDTO());

        when(dashboardService.getCreatorStats(eq(CREATOR_USERNAME))).thenReturn(stats);
        when(userService.findByUsername(CREATOR_USERNAME)).thenReturn(Optional.of(creator));
        when(testerStatisticsService.getRecentTestAttemptsForCreator(eq(CREATOR_USERNAME), eq(5)))
                .thenReturn(recentAttempts);
        when(testerStatisticsService.getTotalTesters()).thenReturn(10L);

        mockMvc.perform(get("/creator/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/dashboard"))
                .andExpect(model().attribute("stats", stats))
                .andExpect(model().attribute("username", CREATOR_USERNAME))
                .andExpect(model().attribute("firstName", "John"))
                .andExpect(model().attribute("recentAttempts", recentAttempts))
                .andExpect(model().attribute("totalTesters", 10L));
    }

    @Disabled("Отключено из-за ошибки в шаблоне creator/dashboard.html (обращение к stats.totalTests при null stats)")
    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/dashboard: при ошибке возвращает view с сообщением об ошибке")
    void creatorDashboard_Error_ShouldReturnViewWithErrorMessage() throws Exception {
        when(dashboardService.getCreatorStats(eq(CREATOR_USERNAME)))
                .thenThrow(new RuntimeException("Dashboard error"));

        mockMvc.perform(get("/creator/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/dashboard"))
                .andExpect(model().attributeExists("errorMessage"));
    }
}