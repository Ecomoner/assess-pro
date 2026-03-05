package com.frist.assesspro.controllers;

import com.frist.assesspro.service.metrics.MetricsService;
import com.frist.assesspro.service.ProfileService;
import com.frist.assesspro.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(GlobalControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetricsService metricsService;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserService userService;

    @Test
    void dashboard_WhenNotAuthenticated_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void dashboard_WithAdminRole_ShouldRedirectToAdminDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @WithMockUser(username = "creator", roles = "CREATOR")
    void dashboard_WithCreatorRole_ShouldRedirectToCreatorDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/dashboard"));
    }

    @Test
    @WithMockUser(username = "tester", roles = "TESTER")
    void dashboard_WithTesterRole_ShouldRedirectToTesterDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tester/dashboard"));
    }
}