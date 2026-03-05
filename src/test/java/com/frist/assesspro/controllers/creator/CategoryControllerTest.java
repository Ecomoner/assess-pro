package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.category.CategoryCreateDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.category.CategoryUpdateDTO;
import com.frist.assesspro.entity.Category;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.CategoryService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "CREATOR")
@DisplayName("Тесты контроллера категорий (CategoryController)")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private UserService userService; // требуется для WebConfig

    private final Long CATEGORY_ID = 1L;
    private final String CATEGORY_NAME = "Math";
    private final String CATEGORY_DESC = "Math category";

    private Category category;
    private CategoryDTO categoryDTO;

    @BeforeEach
    void setUp() {
        User creator = new User();
        creator.setUsername("user");

        category = new Category();
        category.setId(CATEGORY_ID);
        category.setName(CATEGORY_NAME);
        category.setDescription(CATEGORY_DESC);
        category.setIsActive(true);
        category.setCreatedBy(creator);

        categoryDTO = new CategoryDTO();
        categoryDTO.setId(CATEGORY_ID);
        categoryDTO.setName(CATEGORY_NAME);
        categoryDTO.setDescription(CATEGORY_DESC);
    }

    // ---------- GET /creator/categories ----------

    @Test
    @DisplayName("GET /creator/categories: должен вернуть view со списком категорий")
    void getAllCategories_ShouldReturnView() throws Exception {
        int page = 0, size = 10;
        String sort = "name";
        Page<CategoryDTO> pageResult = new PageImpl<>(List.of(categoryDTO), PageRequest.of(page, size), 1);

        when(categoryService.getAllCategories(any(PageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get("/creator/categories")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/category-list"))
                .andExpect(model().attribute("categories", pageResult.getContent()))
                .andExpect(model().attribute("sort", sort))
                .andExpect(model().attribute("pageSize", size))
                // Атрибуты, добавляемые PaginationUtils (из реального ответа)
                .andExpect(model().attributeExists("startPage", "endPage", "baseUrl", "queryString"));
    }

    // ---------- GET /creator/categories/new ----------

    @Test
    @DisplayName("GET /creator/categories/new: должен вернуть форму создания")
    void showCreateForm_ShouldReturnView() throws Exception {
        mockMvc.perform(get("/creator/categories/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/category-form"))
                .andExpect(model().attributeExists("categoryDTO"))
                .andExpect(model().attribute("action", "create"));
    }

    // ---------- POST /creator/categories/new ----------

    @Test
    @DisplayName("POST /creator/categories/new: успешное создание, редирект на список")
    void createCategory_Success_ShouldRedirect() throws Exception {
        when(categoryService.createCategory(any(CategoryCreateDTO.class), anyString()))
                .thenReturn(category);

        mockMvc.perform(post("/creator/categories/new")
                        .param("name", CATEGORY_NAME)
                        .param("description", CATEGORY_DESC))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/categories"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(categoryService).createCategory(any(CategoryCreateDTO.class), eq("user"));
    }

    @Test
    @DisplayName("POST /creator/categories/new: ошибка валидации, редирект с ошибкой")
    void createCategory_ValidationError_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/creator/categories/new")
                        .param("name", "") // пустое имя
                        .param("description", CATEGORY_DESC))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/categories/new"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("categoryDTO"));

        verify(categoryService, never()).createCategory(any(), any());
    }

    @Test
    @DisplayName("POST /creator/categories/new: исключение сервиса, редирект с ошибкой")
    void createCategory_ServiceException_ShouldRedirectWithError() throws Exception {
        when(categoryService.createCategory(any(CategoryCreateDTO.class), anyString()))
                .thenThrow(new RuntimeException("Creation failed"));

        mockMvc.perform(post("/creator/categories/new")
                        .param("name", CATEGORY_NAME)
                        .param("description", CATEGORY_DESC))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/categories/new"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("categoryDTO"));
    }

    // ---------- GET /creator/categories/edit/{id} ----------

    @Test
    @DisplayName("GET /creator/categories/edit/{id}: успешный просмотр формы редактирования")
    void showEditForm_Success_ShouldReturnView() throws Exception {
        when(categoryService.getCategoryById(CATEGORY_ID)).thenReturn(Optional.of(category));

        mockMvc.perform(get("/creator/categories/edit/{id}", CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/category-form"))
                .andExpect(model().attributeExists("categoryDTO"))
                .andExpect(model().attribute("categoryId", CATEGORY_ID))
                .andExpect(model().attribute("formAction", "/creator/categories/update/" + CATEGORY_ID))
                .andExpect(model().attribute("action", "edit"));
    }

    @Test
    @DisplayName("GET /creator/categories/edit/{id}: категория не найдена, редирект")
    void showEditForm_NotFound_ShouldRedirect() throws Exception {
        when(categoryService.getCategoryById(CATEGORY_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/creator/categories/edit/{id}", CATEGORY_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/categories?error=not_found"));
    }

    // ---------- POST /creator/categories/update/{id} ----------

    @Test
    @DisplayName("POST /creator/categories/update/{id}: успешное обновление, редирект на список")
    void updateCategory_Success_ShouldRedirect() throws Exception {
        when(categoryService.updateCategory(eq(CATEGORY_ID), any(CategoryUpdateDTO.class), anyString()))
                .thenReturn(category);

        mockMvc.perform(post("/creator/categories/update/{id}", CATEGORY_ID)
                        .param("name", "Updated Name")
                        .param("description", "Updated Desc")
                        .param("isActive", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/categories"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(categoryService).updateCategory(eq(CATEGORY_ID), any(CategoryUpdateDTO.class), eq("user"));
    }

    @Test
    @DisplayName("POST /creator/categories/update/{id}: ошибка валидации, редирект с ошибкой")
    void updateCategory_ValidationError_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/creator/categories/update/{id}", CATEGORY_ID)
                        .param("name", "") // пустое имя
                        .param("description", "Desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/categories/edit/" + CATEGORY_ID))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("categoryDTO"));

        verify(categoryService, never()).updateCategory(any(), any(), any());
    }

    @Test
    @DisplayName("POST /creator/categories/update/{id}: исключение сервиса, редирект с ошибкой")
    void updateCategory_ServiceException_ShouldRedirectWithError() throws Exception {
        when(categoryService.updateCategory(eq(CATEGORY_ID), any(CategoryUpdateDTO.class), anyString()))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(post("/creator/categories/update/{id}", CATEGORY_ID)
                        .param("name", "Updated Name")
                        .param("description", "Desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/categories/edit/" + CATEGORY_ID))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("categoryDTO"));
    }

    // ---------- POST /creator/categories/delete/{id} ----------

    @Test
    @DisplayName("POST /creator/categories/delete/{id}: успешное удаление, редирект с сообщением")
    void deleteCategory_Success_ShouldRedirect() throws Exception {
        doNothing().when(categoryService).deleteCategory(eq(CATEGORY_ID), anyString());

        mockMvc.perform(post("/creator/categories/delete/{id}", CATEGORY_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/categories"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(categoryService).deleteCategory(eq(CATEGORY_ID), eq("user"));
    }

    @Test
    @DisplayName("POST /creator/categories/delete/{id}: исключение сервиса, редирект с ошибкой")
    void deleteCategory_ServiceException_ShouldRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Delete failed"))
                .when(categoryService).deleteCategory(eq(CATEGORY_ID), anyString());

        mockMvc.perform(post("/creator/categories/delete/{id}", CATEGORY_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/categories"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}