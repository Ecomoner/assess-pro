package com.frist.assesspro.controllers;

import com.frist.assesspro.dto.RegistrationDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.AuthService;
import com.frist.assesspro.service.UserService;
import com.frist.assesspro.service.metrics.MetricsService;
import com.frist.assesspro.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MetricsService metricsService;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserService userService;

    @Test
    void loginPage_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void registerPage_ShouldReturnRegisterViewWithEmptyDto() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registrationDTO"))
                .andExpect(model().attribute("registrationDTO",
                        org.hamcrest.Matchers.hasProperty("role", org.hamcrest.Matchers.is(User.Roles.TESTER))));
    }

    @Test
    void registerPage_ShouldNotOverwriteDtoIfAlreadyPresent() throws Exception {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("existing");
        mockMvc.perform(get("/register").flashAttr("registrationDTO", dto))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("registrationDTO", dto));
    }

    @Test
    void registerUser_WithValidData_ShouldRedirectToLoginAndIncrementMetrics() throws Exception {
        RegistrationDTO validDto = new RegistrationDTO();
        validDto.setUsername("validuser");
        validDto.setPassword("pass123");
        validDto.setConfirmPassword("pass123");
        validDto.setRole(User.Roles.TESTER);

        User savedUser = new User();
        savedUser.setUsername("validuser");
        when(authService.registerUser(any(RegistrationDTO.class))).thenReturn(savedUser);

        mockMvc.perform(post("/register")
                        .param("username", "validuser")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123")
                        .param("role", User.Roles.TESTER))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?success=true"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(authService).registerUser(any(RegistrationDTO.class));
        verify(metricsService).incrementUsersRegistered();
    }

    @Test
    void registerUser_WithValidationErrors_ShouldRedirectToRegisterWithErrors() throws Exception {
        // Пустой username вызовет ошибку валидации
        mockMvc.perform(post("/register")
                        .param("username", "")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123")
                        .param("role", User.Roles.TESTER))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.registrationDTO"))
                .andExpect(flash().attributeExists("registrationDTO"));

        verify(authService, never()).registerUser(any());
        verify(metricsService, never()).incrementUsersRegistered();
    }

    @Test
    void registerUser_WhenServiceThrowsIllegalArgumentException_ShouldHandleError() throws Exception {
        String errorMessage = "Имя пользователя уже занято";
        doThrow(new IllegalArgumentException(errorMessage)).when(authService).registerUser(any());

        mockMvc.perform(post("/register")
                        .param("username", "taken")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123")
                        .param("role", User.Roles.TESTER))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("errorMessage", errorMessage))
                .andExpect(flash().attributeExists("registrationDTO"));

        verify(authService).registerUser(any());
        verify(metricsService, never()).incrementUsersRegistered();
    }
}