package com.frist.assesspro.util;

import org.springframework.data.domain.Page;
import org.springframework.ui.Model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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

        model.addAttribute("items", page.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("pageSize", page.getSize());
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("params", buildParamsString(params));
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevious", page.hasPrevious());
        model.addAttribute("hasNext", page.hasNext());
    }

    /**
     * Создает строку параметров для URL
     */
    private static String buildParamsString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));  // ← КОДИРУЕМ!
        }

        return sb.toString();
    }

    /**
     * Создает Pageable объект с сортировкой по умолчанию
     */
    public static org.springframework.data.domain.Pageable createPageRequest(int page, int size, String sortBy, String direction) {
        org.springframework.data.domain.Sort sort = direction.equalsIgnoreCase("desc")
                ? org.springframework.data.domain.Sort.by(sortBy).descending()
                : org.springframework.data.domain.Sort.by(sortBy).ascending();

        return org.springframework.data.domain.PageRequest.of(page, size, sort);
    }
    /**
     * Получает параметры фильтрации из запроса
     */
    public static Map<String, String> extractFilterParams(Map<String, String[]> requestParams,
                                                          String... excludeParams) {
        Map<String, String> filterParams = new HashMap<>();

        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            // Пропускаем параметры пагинации
            if (key.equals("page") || key.equals("size") || key.equals("sort")) {
                continue;
            }

            // Пропускаем исключенные параметры
            boolean excluded = false;
            for (String excludeParam : excludeParams) {
                if (key.equals(excludeParam)) {
                    excluded = true;
                    break;
                }
            }

            if (!excluded && values != null && values.length > 0 &&
                    values[0] != null && !values[0].isEmpty()) {
                // Декодируем при извлечении
                filterParams.put(key, decodeValue(values[0]));
            }
        }

        return filterParams;
    }
    public static String decodeValue(String value) {
        try {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
