package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "CREATOR")
@DisplayName("Тесты контроллера списка тестировщиков (TesterListController)")
class TesterListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private User tester1;
    private User tester2;

    @BeforeEach
    void setUp() {
        tester1 = new User();
        tester1.setId(1L);
        tester1.setUsername("tester1");
        tester1.setFirstName("John");
        tester1.setLastName("Doe");
        tester1.setRole("ROLE_TESTER");

        tester2 = new User();
        tester2.setId(2L);
        tester2.setUsername("tester2");
        tester2.setFirstName("Jane");
        tester2.setLastName("Smith");
        tester2.setRole("ROLE_TESTER");
    }

    @Test
    @DisplayName("GET /creator/testers без поиска: должен вернуть view со списком тестировщиков")
    void getAllTesters_WithoutSearch_ShouldReturnView() throws Exception {
        int page = 0;
        int size = 10;
        String sort = "createdAt";
        Page<User> pageResult = new PageImpl<>(
                List.of(tester1, tester2),
                PageRequest.of(page, size, Sort.by(sort).descending()),
                2
        );

        when(userService.findAllTesters(isNull(), any(PageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get("/creator/testers")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/testers-list"))
                .andExpect(model().attribute("testers", pageResult.getContent()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("pageSize", size))
                .andExpect(model().attributeDoesNotExist("search"))
                .andExpect(model().attribute("sort", sort))
                // Проверяем атрибуты, добавленные PaginationUtils (предположительно)
                .andExpect(model().attributeExists("startPage", "endPage", "baseUrl", "queryString"));
    }

    @Test
    @DisplayName("GET /creator/testers с поиском: должен вернуть view с отфильтрованным списком")
    void getAllTesters_WithSearch_ShouldReturnView() throws Exception {
        int page = 0;
        int size = 10;
        String sort = "createdAt";
        String search = "john";
        Page<User> pageResult = new PageImpl<>(
                List.of(tester1),
                PageRequest.of(page, size, Sort.by(sort).descending()),
                1
        );

        when(userService.findAllTesters(eq(search), any(PageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get("/creator/testers")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sort", sort)
                        .param("search", search))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/testers-list"))
                .andExpect(model().attribute("testers", pageResult.getContent()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("pageSize", size))
                .andExpect(model().attribute("search", search))
                .andExpect(model().attribute("sort", sort))
                .andExpect(model().attributeExists("startPage", "endPage", "baseUrl", "queryString"));
    }

    @Test
    @DisplayName("GET /creator/testers: параметры пагинации по умолчанию")
    void getAllTesters_DefaultParams_ShouldUseDefaults() throws Exception {
        int defaultPage = 0;
        int defaultSize = 10;
        String defaultSort = "createdAt";
        Page<User> pageResult = new PageImpl<>(
                List.of(tester1),
                PageRequest.of(defaultPage, defaultSize, Sort.by(defaultSort).descending()),
                1
        );

        when(userService.findAllTesters(isNull(), any(PageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get("/creator/testers"))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/testers-list"))
                .andExpect(model().attribute("currentPage", defaultPage))
                .andExpect(model().attribute("pageSize", defaultSize))
                .andExpect(model().attribute("sort", defaultSort));
    }
}