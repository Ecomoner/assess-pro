package com.frist.assesspro.controllers;

import com.frist.assesspro.dto.RegistrationDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.AuthService;
import com.frist.assesspro.service.UserService;
import com.frist.assesspro.service.metrics.MetricsService;
import com.frist.assesspro.service.ProfileService;
import org.junit.jupiter.api.Disabled;
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

    @Disabled//@todo отключена регистрацию по форме
    @Test
    void registerPage_ShouldReturnRegisterViewWithEmptyDto() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registrationDTO"))
                .andExpect(model().attribute("registrationDTO",
                        org.hamcrest.Matchers.hasProperty("role", org.hamcrest.Matchers.is(User.Roles.TESTER))));
    }

    @Disabled//TODO: отключена регистрация через форму регистрациии
    @Test
    void registerPage_ShouldNotOverwriteDtoIfAlreadyPresent() throws Exception {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("existing");
        mockMvc.perform(get("/register").flashAttr("registrationDTO", dto))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("registrationDTO", dto));
    }


}