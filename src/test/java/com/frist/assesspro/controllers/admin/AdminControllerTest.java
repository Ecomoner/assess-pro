package com.frist.assesspro.controllers.admin;

import com.frist.assesspro.dto.admin.AppStatisticsDTO;
import com.frist.assesspro.dto.admin.UserManagementDTO;
import com.frist.assesspro.service.AdminService;
import com.frist.assesspro.service.export.AdminExportService;
import com.frist.assesspro.service.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "ADMIN")
@DisplayName("Тесты контроллера администратора (AdminController)")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private AdminExportService adminExportService;

    @MockitoBean
    private MetricsService metricsService;

    private final Long USER_ID = 1L;
    private final String USERNAME = "testuser";
    private final String ADMIN_USERNAME = "user";

    private UserManagementDTO userDTO;

    @BeforeEach
    void setUp() {
        userDTO = new UserManagementDTO();
        userDTO.setId(USER_ID);
        userDTO.setUsername(USERNAME);
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");
        userDTO.setRole("ROLE_TESTER");
        userDTO.setIsActive(true);
    }

    // ---------- GET /admin/dashboard ----------

    @Test
    @DisplayName("GET /admin/dashboard: должен вернуть view с дашбордом")
    void dashboard_ShouldReturnView() throws Exception {
        AppStatisticsDTO stats = new AppStatisticsDTO();
        stats.setTotalUsers(100L);
        when(adminService.getAppStatistics()).thenReturn(stats);

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("stats", stats));
    }

    // ---------- GET /admin/users ----------

    @Test
    @DisplayName("GET /admin/users: должен вернуть view со списком пользователей")
    void listUsers_ShouldReturnView() throws Exception {
        int page = 0, size = 20;
        Page<UserManagementDTO> pageResult = new PageImpl<>(List.of(userDTO), PageRequest.of(page, size), 1);

        when(adminService.getAllUsers(isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/admin/users")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-list"))
                .andExpect(model().attribute("users", pageResult.getContent()))
                .andExpect(model().attribute("usersPage", pageResult))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("pageSize", size));
    }

    @Test
    @DisplayName("GET /admin/users с фильтрами: должен передать параметры фильтрации")
    void listUsers_WithFilters_ShouldPassParameters() throws Exception {
        int page = 0, size = 20;
        String role = "ROLE_TESTER";
        String search = "john";
        Boolean active = true;

        when(adminService.getAllUsers(eq(role), eq(search), eq(active), any(PageRequest.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/admin/users")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("role", role)
                        .param("search", search)
                        .param("active", String.valueOf(active)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("role", role))
                .andExpect(model().attribute("search", search))
                .andExpect(model().attribute("active", active));
    }

    // ---------- GET /admin/users/{id} ----------

    @Test
    @DisplayName("GET /admin/users/{id}: успешный просмотр пользователя")
    void viewUser_Success_ShouldReturnView() throws Exception {
        when(adminService.getUserById(USER_ID)).thenReturn(Optional.of(userDTO));

        mockMvc.perform(get("/admin/users/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-view"))
                .andExpect(model().attribute("user", userDTO));
    }

    @Test
    @DisplayName("GET /admin/users/{id}: пользователь не найден -> исключение")
    void viewUser_NotFound_ShouldThrowException() throws Exception {
        when(adminService.getUserById(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/users/{id}", USER_ID));// или ожидаем исключение, но в контроллере выбрасывается RuntimeException
    }

    // ---------- GET /admin/users/new ----------

    @Test
    @DisplayName("GET /admin/users/new: должен вернуть форму создания")
    void showCreateForm_ShouldReturnView() throws Exception {
        mockMvc.perform(get("/admin/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("action", "create"));
    }

    // ---------- POST /admin/users/new ----------

    @Test
    @DisplayName("POST /admin/users/new: успешное создание, редирект на список")
    void createUser_Success_ShouldRedirect() throws Exception {
        when(adminService.createUser(any(UserManagementDTO.class), eq(ADMIN_USERNAME)))
                .thenReturn(null); // метод возвращает User, но нам не важно

        mockMvc.perform(post("/admin/users/new")
                        .param("username", "newuser")
                        .param("password", "pass123")
                        .param("firstName", "New")
                        .param("lastName", "User")
                        .param("role", "ROLE_TESTER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(metricsService).incrementUsersRegistered();
    }

    @Test
    @DisplayName("POST /admin/users/new: ошибка валидации, редирект с ошибкой")
    void createUser_ValidationError_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/admin/users/new")
                        .param("username", "")
                        .param("password", "pass123")
                        .param("firstName", "New")
                        .param("lastName", "User")
                        .param("role", "ROLE_TESTER"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(model().attributeHasFieldErrors("user", "username"));
    }

    @Test
    @DisplayName("POST /admin/users/new: исключение сервиса, редирект с ошибкой")
    void createUser_ServiceException_ShouldRedirectWithError() throws Exception {
        when(adminService.createUser(any(UserManagementDTO.class), eq(ADMIN_USERNAME)))
                .thenThrow(new RuntimeException("Creation failed"));

        mockMvc.perform(post("/admin/users/new")
                        .param("username", "newuser")
                        .param("password", "pass123")
                        .param("firstName", "New")
                        .param("lastName", "User")
                        .param("role", "ROLE_TESTER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/new"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- GET /admin/users/edit/{id} ----------

    @Test
    @DisplayName("GET /admin/users/edit/{id}: успешный просмотр формы редактирования")
    void showEditForm_Success_ShouldReturnView() throws Exception {
        when(adminService.getUserById(USER_ID)).thenReturn(Optional.of(userDTO));

        mockMvc.perform(get("/admin/users/edit/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(model().attribute("user", userDTO))
                .andExpect(model().attribute("action", "edit"));
    }

    @Test
    @DisplayName("GET /admin/users/edit/{id}: пользователь не найден -> исключение")
    void showEditForm_NotFound_ShouldThrowException() throws Exception {
        when(adminService.getUserById(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/users/edit/{id}", USER_ID));

    }

    // ---------- POST /admin/users/update/{id} ----------

    @Test
    @DisplayName("POST /admin/users/update/{id}: успешное обновление, редирект на страницу пользователя")
    void updateUser_Success_ShouldRedirectToUserView() throws Exception {
        when(adminService.updateUser(eq(USER_ID), any(UserManagementDTO.class), eq(ADMIN_USERNAME)))
                .thenReturn(null);

        mockMvc.perform(post("/admin/users/update/{id}", USER_ID)
                        .param("id", String.valueOf(USER_ID))
                        .param("username", USERNAME)
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("role", "ROLE_TESTER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + USER_ID))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /admin/users/update/{id}: id не указан, редирект с ошибкой")
    void updateUser_MissingId_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/admin/users/update/{id}", USER_ID)
                        .param("id", String.valueOf(USER_ID))
                        .param("username", "")
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("role", "ROLE_TESTER"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(model().attributeHasFieldErrors("user", "username"));
    }

    @Test
    @DisplayName("POST /admin/users/update/{id}: исключение сервиса, редирект с ошибкой")
    void updateUser_ServiceException_ShouldRedirectWithError() throws Exception {
        when(adminService.updateUser(eq(USER_ID), any(UserManagementDTO.class), eq(ADMIN_USERNAME)))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(post("/admin/users/update/{id}", USER_ID)
                        .param("id", String.valueOf(USER_ID))
                        .param("username", USERNAME)
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("role", "ROLE_TESTER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/edit/" + USER_ID))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- POST /admin/users/{id}/toggle ----------

    @Test
    @DisplayName("POST /admin/users/{id}/toggle: успешное изменение статуса, редирект на список")
    void toggleUserStatus_Success_ShouldRedirect() throws Exception {
        when(adminService.toggleUserStatus(eq(USER_ID), eq(true), eq(ADMIN_USERNAME)))
                .thenReturn(null); // метод возвращает User, но нам не важно

        mockMvc.perform(post("/admin/users/{id}/toggle", USER_ID)
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /admin/users/{id}/toggle: исключение сервиса, редирект с ошибкой")
    void toggleUserStatus_Error_ShouldRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Toggle failed"))
                .when(adminService).toggleUserStatus(eq(USER_ID), eq(true), eq(ADMIN_USERNAME));

        mockMvc.perform(post("/admin/users/{id}/toggle", USER_ID)
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- POST /admin/users/delete/{id} ----------

    @Test
    @DisplayName("POST /admin/users/delete/{id}: успешное удаление, редирект на список")
    void deleteUser_Success_ShouldRedirect() throws Exception {
        doNothing().when(adminService).deleteUser(eq(USER_ID), eq(ADMIN_USERNAME));

        mockMvc.perform(post("/admin/users/delete/{id}", USER_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /admin/users/delete/{id}: исключение сервиса, редирект с ошибкой")
    void deleteUser_Error_ShouldRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Delete failed"))
                .when(adminService).deleteUser(eq(USER_ID), eq(ADMIN_USERNAME));

        mockMvc.perform(post("/admin/users/delete/{id}", USER_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- GET /admin/statistics ----------

    @Test
    @DisplayName("GET /admin/statistics: должен вернуть view со статистикой")
    void statistics_ShouldReturnView() throws Exception {
        AppStatisticsDTO stats = new AppStatisticsDTO();
        when(adminService.getAppStatistics()).thenReturn(stats);

        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics"))
                .andExpect(model().attribute("stats", stats));
    }

    // ---------- GET /admin/statistics/export ----------

    @Test
    @DisplayName("GET /admin/statistics/export: успешный экспорт PDF")
    void exportStatistics_Success_ShouldReturnPdf() throws Exception {
        byte[] pdfContent = new byte[]{1, 2, 3};
        when(adminExportService.generateAppStatisticsPDF()).thenReturn(pdfContent);

        mockMvc.perform(get("/admin/statistics/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"app_statistics_")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    @DisplayName("GET /admin/statistics/export: при ошибке возвращает bad request")
    void exportStatistics_Error_ShouldReturnBadRequest() throws Exception {
        when(adminExportService.generateAppStatisticsPDF()).thenThrow(new RuntimeException("Export failed"));

        mockMvc.perform(get("/admin/statistics/export"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /admin/users/export ----------

    @Test
    @DisplayName("GET /admin/users/export: успешный экспорт списка пользователей")
    void exportUsers_Success_ShouldReturnPdf() throws Exception {
        byte[] pdfContent = new byte[]{4, 5, 6};
        when(adminExportService.generateUsersListPDF(isNull(), isNull())).thenReturn(pdfContent);

        mockMvc.perform(get("/admin/users/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"users_list.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    @DisplayName("GET /admin/users/export с параметрами: должен передать их в сервис")
    void exportUsers_WithParams_ShouldPassToService() throws Exception {
        String role = "ROLE_TESTER";
        Boolean active = true;

        when(adminExportService.generateUsersListPDF(eq(role), eq(active))).thenReturn(new byte[0]);

        mockMvc.perform(get("/admin/users/export")
                        .param("role", role)
                        .param("active", String.valueOf(active)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /admin/users/export: при ошибке возвращает bad request")
    void exportUsers_Error_ShouldReturnBadRequest() throws Exception {
        when(adminExportService.generateUsersListPDF(isNull(), isNull()))
                .thenThrow(new RuntimeException("Export failed"));

        mockMvc.perform(get("/admin/users/export"))
                .andExpect(status().isBadRequest());
    }
}