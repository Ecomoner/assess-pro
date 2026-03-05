package com.frist.assesspro.controllers;

import com.frist.assesspro.dto.profile.ProfileCompletionDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.ProfileService;
import com.frist.assesspro.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // фильтры отключены
@Import(GlobalControllerAdvice.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserService userService;

    private final String TEST_USERNAME = "testuser";

    // ---------- GET /profile/complete ----------

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void showProfileForm_WhenProfileAlreadyComplete_ShouldRedirectToDashboard() throws Exception {
        when(profileService.isProfileComplete(TEST_USERNAME)).thenReturn(true);

        mockMvc.perform(get("/profile/complete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(profileService).isProfileComplete(TEST_USERNAME);
        verify(profileService, never()).getProfile(any());
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void showProfileForm_WhenProfileNotComplete_ShouldShowFormWithEmptyDto() throws Exception {
        when(profileService.isProfileComplete(TEST_USERNAME)).thenReturn(false);

        mockMvc.perform(get("/profile/complete"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/complete"))
                .andExpect(model().attributeExists("profileDTO"))
                .andExpect(model().attribute("profileDTO",
                        org.hamcrest.Matchers.hasProperty("firstName", org.hamcrest.Matchers.nullValue())));

        verify(profileService).isProfileComplete(TEST_USERNAME);
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void showProfileForm_WhenFlashAttributeExists_ShouldUseIt() throws Exception {
        when(profileService.isProfileComplete(TEST_USERNAME)).thenReturn(false);
        ProfileCompletionDTO flashDto = new ProfileCompletionDTO();
        flashDto.setFirstName("John");

        mockMvc.perform(get("/profile/complete").flashAttr("profileDTO", flashDto))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/complete"))
                .andExpect(model().attribute("profileDTO", flashDto));
    }

    // ---------- POST /profile/complete ----------

    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "CREATOR")
    void completeProfile_WithValidDataAndCreatorRole_ShouldRedirectToCreatorDashboard() throws Exception {
        User savedUser = new User();
        savedUser.setUsername(TEST_USERNAME);
        savedUser.setRole("ROLE_CREATOR");
        when(profileService.completeProfile(eq(TEST_USERNAME), any(ProfileCompletionDTO.class)))
                .thenReturn(savedUser);

        mockMvc.perform(post("/profile/complete")
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("middleName", "Middle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).completeProfile(eq(TEST_USERNAME), any(ProfileCompletionDTO.class));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "TESTER")
    void completeProfile_WithValidDataAndTesterRole_ShouldRedirectToTesterDashboard() throws Exception {
        User savedUser = new User();
        savedUser.setUsername(TEST_USERNAME);
        savedUser.setRole("ROLE_TESTER");
        when(profileService.completeProfile(eq(TEST_USERNAME), any(ProfileCompletionDTO.class)))
                .thenReturn(savedUser);

        mockMvc.perform(post("/profile/complete")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tester/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).completeProfile(eq(TEST_USERNAME), any(ProfileCompletionDTO.class));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void completeProfile_WithValidationErrors_ShouldRedirectWithErrors() throws Exception {
        mockMvc.perform(post("/profile/complete")
                        .param("firstName", "")
                        .param("lastName", "Doe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/complete"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.profileDTO"))
                .andExpect(flash().attributeExists("profileDTO"));

        verify(profileService, never()).completeProfile(any(), any());
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void completeProfile_WhenServiceThrowsIllegalArgumentException_ShouldHandleError() throws Exception {
        String errorMessage = "Invalid name";
        doThrow(new IllegalArgumentException(errorMessage))
                .when(profileService).completeProfile(eq(TEST_USERNAME), any());

        mockMvc.perform(post("/profile/complete")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/complete"))
                .andExpect(flash().attribute("errorMessage", errorMessage))
                .andExpect(flash().attributeExists("profileDTO"));

        verify(profileService).completeProfile(eq(TEST_USERNAME), any());
    }

    // ---------- GET /profile/edit ----------

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void editProfile_ShouldReturnEditViewWithProfileDto() throws Exception {
        ProfileCompletionDTO existingDto = new ProfileCompletionDTO();
        existingDto.setFirstName("John");
        existingDto.setLastName("Doe");
        when(profileService.getProfile(TEST_USERNAME)).thenReturn(existingDto);

        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attribute("profileDTO", existingDto));

        verify(profileService).getProfile(TEST_USERNAME);
    }

    // ---------- POST /profile/edit ----------

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void updateProfile_WithValidData_ShouldRedirectToDashboard() throws Exception {
        when(profileService.completeProfile(eq(TEST_USERNAME), any(ProfileCompletionDTO.class)))
                .thenReturn(new User());

        mockMvc.perform(post("/profile/edit")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).completeProfile(eq(TEST_USERNAME), any(ProfileCompletionDTO.class));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void updateProfile_WithValidationErrors_ShouldReturnEditView() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .param("firstName", "")
                        .param("lastName", "Doe"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("profileDTO"))
                .andExpect(model().attributeHasFieldErrors("profileDTO", "firstName"));

        verify(profileService, never()).completeProfile(any(), any());
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void updateProfile_WhenServiceThrowsIllegalArgumentException_ShouldRedirectWithError() throws Exception {
        String errorMessage = "Invalid name";
        doThrow(new IllegalArgumentException(errorMessage))
                .when(profileService).completeProfile(eq(TEST_USERNAME), any());

        mockMvc.perform(post("/profile/edit")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/edit"))
                .andExpect(flash().attribute("errorMessage", errorMessage))
                .andExpect(flash().attributeExists("profileDTO"));

        verify(profileService).completeProfile(eq(TEST_USERNAME), any());
    }
}