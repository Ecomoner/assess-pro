package com.frist.assesspro.controllers.admin;

import com.frist.assesspro.dto.admin.AppStatisticsDTO;
import com.frist.assesspro.dto.admin.UserManagementDTO;
import com.frist.assesspro.service.AdminService;
import com.frist.assesspro.service.export.AdminExportService;
import com.frist.assesspro.service.metrics.MetricsService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Администратор", description = "API для администратора")
public class AdminController {

    private final AdminService adminService;
    private final AdminExportService adminExportService;
    private final MetricsService metricsService;

    // ============= DASHBOARD =============
    @Operation(summary = "Получить дашборд администратора")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Дашборд успешно получен"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AppStatisticsDTO stats = adminService.getAppStatistics();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    // ============= УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ =============
    @Operation(summary = "Получить список пользователей")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список успешно получен"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/users")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserManagementDTO> usersPage = adminService.getAllUsers(role, search, active, pageable);

        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("usersPage", usersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalItems", usersPage.getTotalElements());
        model.addAttribute("role", role);
        model.addAttribute("search", search);
        model.addAttribute("active", active);
        model.addAttribute("pageSize", size);

        return "admin/user-list";
    }
    @Operation(summary = "Детальные данные пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные успешно получены"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        UserManagementDTO user = adminService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        model.addAttribute("user", user);
        return "admin/user-view";
    }

    @Operation(summary = "Открыть форму пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/users/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new UserManagementDTO());
        model.addAttribute("action", "create");
        return "admin/user-form";
    }

    @Operation(summary = "Создать нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/users/new")
    public String createUser(
            @Valid @ModelAttribute("user") UserManagementDTO userDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails adminDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "admin/user-form";
        }

        try {
            adminService.createUser(userDTO, adminDetails.getUsername());
            metricsService.incrementUsersRegistered();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Пользователь успешно создан");
            return "redirect:/admin/users";
        } catch (Exception e) {
            log.error("Ошибка при создании пользователя", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @Operation(summary = "Открыть форму редактирования пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/users/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        UserManagementDTO user = adminService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        model.addAttribute("user", user);
        model.addAttribute("action", "edit");
        return "admin/user-form";
    }
    @Operation(summary = "Обновление данных пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/users/update/{id}")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute("user") UserManagementDTO userDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails adminDetails,
            RedirectAttributes redirectAttributes) {

        if (id == null && userDTO.getId() != null) {
            id = userDTO.getId();
        }

        if (id == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "ID пользователя не указан");
            return "redirect:/admin/users";
        }

        if (bindingResult.hasErrors()) {
            return "admin/user-form";
        }

        try {
            adminService.updateUser(id, userDTO, adminDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Пользователь успешно обновлен");
            return "redirect:/admin/users/" + id;
        } catch (Exception e) {
            log.error("Ошибка при обновлении пользователя", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/edit/" + id;
        }
    }

    @Operation(summary = "Смена статуса пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails adminDetails,
            RedirectAttributes redirectAttributes) {

        try {
            adminService.toggleUserStatus(id, active, adminDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Статус пользователя изменен");
        } catch (Exception e) {
            log.error("Ошибка при изменении статуса", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @Operation(summary = "Удаление пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/users/delete/{id}")
    public String deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminDetails,
            RedirectAttributes redirectAttributes) {

        try {
            adminService.deleteUser(id, adminDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Пользователь удален");
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ============= СТАТИСТИКА =============

    @Operation(summary = "Получение статистики")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/statistics")
    public String statistics(Model model) {
        AppStatisticsDTO stats = adminService.getAppStatistics();
        model.addAttribute("stats", stats);
        return "admin/statistics";
    }

    @Operation(summary = "Экспорт статистики в PDF файл")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/statistics/export")
    public ResponseEntity<byte[]> exportStatistics() {

        log.info("Экспорт общей статистики приложения");

        try {
            byte[] pdfContent = adminExportService.generateAppStatisticsPDF();

            String filename = "app_statistics_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                    ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(pdfContent);

        } catch (Exception e) {
            log.error("Ошибка при экспорте статистики", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Экспорт статистики пользователя в PDF файл")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active) {

        log.info("Экспорт списка пользователей, роль: {}, активные: {}", role, active);

        try {
            // Здесь можно реализовать экспорт пользователей
            byte[] pdfContent = adminExportService.generateUsersListPDF(role, active);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"users_list.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);

        } catch (Exception e) {
            log.error("Ошибка при экспорте пользователей", e);
            return ResponseEntity.badRequest().build();
        }
    }
}