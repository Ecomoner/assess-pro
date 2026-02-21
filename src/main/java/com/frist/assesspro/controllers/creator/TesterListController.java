package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.TesterStatisticsService;
import com.frist.assesspro.service.UserService;
import com.frist.assesspro.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/creator")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Список тестеров",description = "API для создателей")
public class TesterListController {

    private final UserService userService;

    @Operation(summary = "Получение списка всех тестировщиков")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/testers")
    public String getAllTesters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sort,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        log.info("Запрос списка тестировщиков: страница={}, размер={}, поиск='{}'", page, size, search);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());

        // Получаем тестировщиков с пагинацией и поиском
        Page<User> testersPage = userService.findAllTesters(search, pageable);

        // Добавляем атрибуты в модель
        model.addAttribute("testers", testersPage.getContent());
        model.addAttribute("totalItems", testersPage.getTotalElements());
        model.addAttribute("currentPage", testersPage.getNumber());
        model.addAttribute("totalPages", testersPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);

        // Параметры для пагинации (чтобы сохранять поиск при переключении страниц)
        Map<String, String> params = new HashMap<>();
        params.put("size", String.valueOf(size));
        params.put("sort", sort);
        if (search != null && !search.isEmpty()) {
            params.put("search", search);
        }

        PaginationUtils.addPaginationAttributes(model, testersPage, "/creator/testers", params);

        return "creator/testers-list";
    }
}