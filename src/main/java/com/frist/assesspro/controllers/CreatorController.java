package com.frist.assesspro.controllers;


import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.service.TestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/creator")
@RequiredArgsConstructor
@Slf4j
public class CreatorController {

    private final TestService testService;

    @GetMapping("/tests")
    @Transactional(readOnly = true)
    public String getAllTests(Model model, @AuthenticationPrincipal UserDetails userDetails){
        List<TestDTO> tests = testService.getTestsByCreator(userDetails.getUsername());
        model.addAttribute("tests",tests);
        return "creator/test-list";
    }

    @GetMapping("/tests/new")
    public String showCreateTestForm(Model model){
        model.addAttribute("test",new Test());
        model.addAttribute("action","create");
        return "creator/test-form";
    }

    @PostMapping("/tests/new")
    public String createNewTest(
            @ModelAttribute Test test,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes){
        try {
            Test createdTest = testService.createdTest(test, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест '" + createdTest.getTitle() + "' успешно создан!");
            return "redirect:/creator/tests";
        }catch (Exception e){
            log.error("Ошибка при создании теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при создании теста: " + e.getMessage());
            return "redirect:/creator/tests/new";
        }
    }

    @GetMapping("tests/edit/{id}")
    public String showEditTestForm(@PathVariable Long id,Model model,
                                   @AuthenticationPrincipal UserDetails userDetails){
        return testService.getTestById(id, userDetails.getUsername())
                .map(test -> {
                    model.addAttribute("test", test);
                    model.addAttribute("action", "edit");
                    return "creator/test-form";
                })
                .orElse("redirect:/creator/tests?error=not_found");
    }

    @PostMapping("tests/update/{id}")
    public String updateTest(@PathVariable Long id,
                             @ModelAttribute Test test,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes){
        try {
            Test updateTest = testService.updateTest(id,test,userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест '" + updateTest.getTitle() + "' успешно обновлен!");
            return "redirect:/creator/tests";
        } catch (Exception e) {
            log.error("Ошибка при обновлении теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при обновлении теста: " + e.getMessage());
            return "redirect:/creator/tests/edit/" + id;
        }
    }

    @PostMapping("/tests/publish/{id}")
    public String publishTest(@PathVariable Long id,
                              @RequestParam boolean publish,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes){
        try {
            Test test = testService.switchPublishStatus(id,userDetails.getUsername(),publish);
            String message = publish ? "опубликован" : "снят с публикации";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест '" + test.getTitle() + "' успешно " + message + "!");
        } catch (Exception e) {
            log.error("Ошибка при изменении статуса теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка: " + e.getMessage());
        }
        return "redirect:/creator/tests";
    }

    @PostMapping("/tests/delete/{id}")
    public String deleteTest(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes){
        try {
            testService.deleteTest(id,userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тест  успешно удален!");
            return "redirect:/creator/tests";
        } catch (Exception e) {
            log.error("Ошибка при удалении теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при удалении: " + e.getMessage());
            return "redirect:/creator/tests";
        }
    }

}
