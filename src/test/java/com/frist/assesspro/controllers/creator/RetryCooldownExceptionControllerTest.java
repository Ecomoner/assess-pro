package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.entity.RetryCooldownException;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.CooldownService;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
import com.frist.assesspro.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "CREATOR")
@DisplayName("Тесты контроллера исключений повторного прохождения (RetryCooldownExceptionController)")
class RetryCooldownExceptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CooldownService cooldownService;

    @MockitoBean
    private TestService testService;

    @MockitoBean
    private TesterStatisticsService testerStatisticsService;

    @MockitoBean
    private UserService userService;

    private final Long TEST_ID = 100L;
    private final String TEST_TITLE = "Sample Test";
    private final String TESTER_USERNAME = "tester1";
    private final String CREATOR_USERNAME = "user";

    private Test test;
    private User tester;
    private User creator;

    @BeforeEach
    void setUp() {
        test = new Test();
        test.setId(TEST_ID);
        test.setTitle(TEST_TITLE);
        test.setRetryCooldownHours(24);
        test.setRetryCooldownDays(1);

        tester = new User();
        tester.setUsername(TESTER_USERNAME);
        tester.setFirstName("John");
        tester.setLastName("Doe");

        creator = new User();
        creator.setUsername(CREATOR_USERNAME);
        creator.setFirstName("Admin");
        creator.setLastName("User");
    }

    // ---------- GET /creator/tests/{testId}/exceptions ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/exceptions: должен вернуть view с тестом и списком тестировщиков")
    void manageExceptions_Success_ShouldReturnView() throws Exception {
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(testerStatisticsService.getDistinctTestersByTest(eq(TEST_ID), eq(CREATOR_USERNAME)))
                .thenReturn(List.of(tester));

        mockMvc.perform(get("/creator/tests/{testId}/exceptions", TEST_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/retry-exceptions"))
                .andExpect(model().attribute("test", test))
                .andExpect(model().attribute("testers", List.of(tester)));
    }

    // ---------- POST /creator/tests/{testId}/exceptions/remove/{testerUsername} ----------

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/exceptions/remove/{testerUsername}: успешное снятие ограничений, редирект на статистику")
    void removeException_Success_ShouldRedirectToStatistics() throws Exception {
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(userService.getUserByUsername(TESTER_USERNAME)).thenReturn(tester);
        doNothing().when(cooldownService).removeException(test, tester);

        mockMvc.perform(post("/creator/tests/{testId}/exceptions/remove/{testerUsername}", TEST_ID, TESTER_USERNAME))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/statistics/testers"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/exceptions/remove/{testerUsername}: ошибка сервиса, редирект с errorMessage")
    void removeException_Error_ShouldRedirectWithErrorMessage() throws Exception {
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(userService.getUserByUsername(TESTER_USERNAME)).thenReturn(tester);
        doThrow(new RuntimeException("Remove failed")).when(cooldownService).removeException(test, tester);

        mockMvc.perform(post("/creator/tests/{testId}/exceptions/remove/{testerUsername}", TEST_ID, TESTER_USERNAME))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/statistics/testers"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- POST /creator/tests/{testId}/exceptions/create ----------

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/exceptions/create: успешное создание исключения, редирект на статистику")
    void createException_Success_ShouldRedirectToStatistics() throws Exception {
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(userService.getUserByUsername(TESTER_USERNAME)).thenReturn(tester);
        when(userService.getUserByUsername(CREATOR_USERNAME)).thenReturn(creator);
        when(cooldownService.createException(eq(test), eq(tester), eq(creator), any(), anyBoolean(), any()))
                .thenReturn(new RetryCooldownException());

        mockMvc.perform(post("/creator/tests/{testId}/exceptions/create", TEST_ID)
                        .param("testerUsername", TESTER_USERNAME)
                        .param("hours", "12")
                        .param("permanent", "false")
                        .param("reason", "Test reason"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/statistics/testers"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/exceptions/create: ошибка сервиса, редирект с errorMessage")
    void createException_Error_ShouldRedirectWithErrorMessage() throws Exception {
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(userService.getUserByUsername(TESTER_USERNAME)).thenReturn(tester);
        when(userService.getUserByUsername(CREATOR_USERNAME)).thenReturn(creator);
        when(cooldownService.createException(any(), any(), any(), any(), anyBoolean(), any()))
                .thenThrow(new RuntimeException("Create failed"));

        mockMvc.perform(post("/creator/tests/{testId}/exceptions/create", TEST_ID)
                        .param("testerUsername", TESTER_USERNAME)
                        .param("hours", "12")
                        .param("permanent", "false")
                        .param("reason", "Test reason"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/statistics/testers"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- GET /creator/tests/{testId}/exceptions/status/{testerUsername} ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/exceptions/status/{testerUsername}: успешный возврат статуса в JSON")
    void getCooldownStatus_Success_ShouldReturnJson() throws Exception {
        LocalDateTime nextAvailable = LocalDateTime.now().plusHours(5);
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenReturn(test);
        when(userService.getUserByUsername(TESTER_USERNAME)).thenReturn(tester);
        when(cooldownService.getCooldownStatus(test, tester)).thenReturn("Недоступно");
        when(cooldownService.getNextAvailableTime(test, tester)).thenReturn(nextAvailable);

        mockMvc.perform(get("/creator/tests/{testId}/exceptions/status/{testerUsername}", TEST_ID, TESTER_USERNAME)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("Недоступно"))
                .andExpect(jsonPath("$.nextAvailable").value(nextAvailable.toString()))
                .andExpect(jsonPath("$.hasCooldown").value(true))
                .andExpect(jsonPath("$.testerName").value("Doe John"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/exceptions/status/{testerUsername}: при ошибке возвращает JSON с success=false")
    void getCooldownStatus_Error_ShouldReturnErrorJson() throws Exception {
        when(testService.getTestByIdWithoutOwnershipCheck(TEST_ID)).thenThrow(new RuntimeException("Test not found"));

        mockMvc.perform(get("/creator/tests/{testId}/exceptions/status/{testerUsername}", TEST_ID, TESTER_USERNAME)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }
}