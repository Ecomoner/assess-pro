package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.category.CategoryCreateDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.category.CategoryUpdateDTO;
import com.frist.assesspro.entity.Category;
import com.frist.assesspro.service.CategoryService;
import com.frist.assesspro.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/creator/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Категории",description = "API для создателей")
public class CategoryController {

    private final CategoryService categoryService;

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @Operation(summary = "Список категорий")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping
    public String getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        Page<CategoryDTO> categoriesPage = categoryService.getAllCategories(pageable);

        Map<String, String> params = new HashMap<>();
        params.put("size", String.valueOf(size));
        params.put("sort", sort);

        PaginationUtils.addPaginationAttributes(model, categoriesPage, "/creator/categories", params);

        model.addAttribute("categories", categoriesPage.getContent());
        model.addAttribute("sort", sort);
        model.addAttribute("pageSize", size);

        return "creator/category-list";
    }

    @Operation(summary = "Форма создания категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("categoryDTO", new CategoryCreateDTO());
        model.addAttribute("action", "create");
        return "creator/category-form";
    }

    @Operation(summary = "Создание категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/new")
    public String createCategory(
            @Valid @ModelAttribute CategoryCreateDTO categoryCreateDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибки валидации: " + bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("categoryDTO", categoryCreateDTO);
            return "redirect:/creator/categories/new";
        }

        try {
            Category category = categoryService.createCategory(categoryCreateDTO, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Категория '" + category.getName() + "' успешно создана!");
            return "redirect:/creator/categories";
        } catch (Exception e) {
            log.error("Ошибка при создании категории", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при создании категории: " + e.getMessage());
            redirectAttributes.addFlashAttribute("categoryDTO", categoryCreateDTO);
            return "redirect:/creator/categories/new";
        }
    }

    @Operation(summary = "Форма редактирования категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               Model model) {
        return categoryService.getCategoryById(id)
                .map(category -> {
                    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO();
                    updateDTO.setName(category.getName());
                    updateDTO.setDescription(category.getDescription());
                    updateDTO.setIsActive(category.getIsActive());

                    model.addAttribute("categoryDTO", updateDTO);
                    model.addAttribute("categoryId", id);
                    model.addAttribute("formAction", "/creator/categories/update/" + id);
                    model.addAttribute("action", "edit");
                    return "creator/category-form";
                })
                .orElse("redirect:/creator/categories?error=not_found");
    }

    @Operation(summary = "Обновление категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/update/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute CategoryUpdateDTO updateDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибки валидации: " + bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("categoryDTO", updateDTO);
            return "redirect:/creator/categories/edit/" + id;
        }

        try {
            Category category = categoryService.updateCategory(id, updateDTO, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Категория '" + category.getName() + "' успешно обновлена!");
            return "redirect:/creator/categories";
        } catch (Exception e) {
            log.error("Ошибка при обновлении категории", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при обновлении категории: " + e.getMessage());
            redirectAttributes.addFlashAttribute("categoryDTO", updateDTO);
            return "redirect:/creator/categories/edit/" + id;
        }
    }

    @Operation(summary = "Удаление категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/delete/{id}")
    public String deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            categoryService.deleteCategory(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Категория успешно удалена!");
        } catch (Exception e) {
            log.error("Ошибка при удалении категории", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при удалении категории: " + e.getMessage());
        }
        return "redirect:/creator/categories";
    }
}