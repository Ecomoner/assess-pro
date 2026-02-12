package com.frist.assesspro.controllers;


import com.frist.assesspro.dto.profile.ProfileCompletionDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Страница заполнения профиля
     */
    @GetMapping("/complete")
    public String showProfileForm(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        // Проверяем, не заполнен ли уже профиль
        if (profileService.isProfileComplete(userDetails.getUsername())) {
            return "redirect:/dashboard";
        }

        if (!model.containsAttribute("profileDTO")) {
            model.addAttribute("profileDTO", new ProfileCompletionDTO());
        }

        return "profile/complete";
    }

    /**
     * Обработка заполнения профиля
     */
    @PostMapping("/complete")
    public String completeProfile(
            @Valid @ModelAttribute("profileDTO") ProfileCompletionDTO profileDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.info("Заполнение профиля пользователем: {}", userDetails.getUsername());

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.profileDTO",
                    bindingResult);
            redirectAttributes.addFlashAttribute("profileDTO", profileDTO);
            return "redirect:/profile/complete";
        }

        try {
            User user = profileService.completeProfile(userDetails.getUsername(), profileDTO);

            String role = user.getRole();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Профиль успешно заполнен! Добро пожаловать, " + user.getFullName());

            if (role.equals(User.Roles.CREATOR)) {
                return "redirect:/creator/dashboard";
            } else {
                return "redirect:/tester/dashboard";
            }

        } catch (IllegalArgumentException e) {
            log.error("Ошибка заполнения профиля: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("profileDTO", profileDTO);
            return "redirect:/profile/complete";
        }
    }

    /**
     * Просмотр/редактирование профиля
     */
    @GetMapping("/edit")
    public String editProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        ProfileCompletionDTO profileDTO = profileService.getProfile(userDetails.getUsername());
        model.addAttribute("profileDTO", profileDTO);
        return "profile/edit";
    }

    /**
     * Обновление профиля
     */
    @PostMapping("/edit")
    public String updateProfile(
            @Valid @ModelAttribute("profileDTO") ProfileCompletionDTO profileDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "profile/edit";
        }

        try {
            profileService.completeProfile(userDetails.getUsername(), profileDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Профиль успешно обновлен");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("profileDTO", profileDTO);
            return "redirect:/profile/edit";
        }
    }
}
