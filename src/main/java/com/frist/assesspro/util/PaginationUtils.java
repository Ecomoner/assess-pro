package com.frist.assesspro.util;

import org.springframework.data.domain.Page;
import org.springframework.ui.Model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PaginationUtils {

    public static void addPaginationAttributes(Model model, Page<?> page, String baseUrl) {
        addPaginationAttributes(model, page, baseUrl, new HashMap<>());
    }

    public static void addPaginationAttributes(Model model, Page<?> page, String baseUrl,
                                               Map<String, String> params) {
        int currentPage = page.getNumber();
        int totalPages = page.getTotalPages();

        // Расчет диапазона отображаемых страниц
        int startPage = Math.max(0, currentPage - 2);
        int endPage = Math.min(totalPages - 1, currentPage + 2);

        // Если страниц мало, расширяем диапазон
        if (endPage - startPage < 4 && totalPages > 5) {
            if (currentPage < 3) {
                endPage = Math.min(4, totalPages - 1);
            } else if (currentPage > totalPages - 3) {
                startPage = Math.max(totalPages - 5, 0);
            }
        }

        // Строим строку запроса из параметров
        String queryString = params.entrySet().stream()
                .map(entry -> {
                    try {
                        return entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                    } catch (Exception e) {
                        return ""; // Should not happen with UTF-8
                    }
                })
                .collect(Collectors.joining("&"));

        model.addAttribute("items", page.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("pageSize", page.getSize());
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("queryString", queryString.isEmpty() ? "" : "&" + queryString);
    }
}
