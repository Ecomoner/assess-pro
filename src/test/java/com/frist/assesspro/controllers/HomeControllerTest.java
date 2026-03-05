package com.frist.assesspro.controllers;

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

@WebMvcTest(HomeController.class)
@Import(GlobalControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserService userService;

    @Test
    void root_ShouldRedirectToHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void homePage_WhenNotAuthenticated_ShouldShowHomePageWithIsAuthenticatedFalse() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("isAuthenticated", false))
                .andExpect(model().attributeDoesNotExist("username", "role"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "TESTER")
    void homePage_WhenAuthenticated_ShouldShowHomePageWithUserAttributes() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("isAuthenticated", true))
                .andExpect(model().attribute("username", "testuser"))
                .andExpect(model().attribute("role", "ROLE_TESTER"));
    }

    @Test
    void accessDenied_ShouldReturnAccessDeniedView() throws Exception {
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/access-denied"));
    }
}