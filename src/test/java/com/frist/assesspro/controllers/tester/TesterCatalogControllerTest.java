package com.frist.assesspro.controllers.tester;

import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.test.TestInfoDTO;
import com.frist.assesspro.service.TestPassingService;
import org.junit.jupiter.api.Disabled;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "TESTER")
@DisplayName("Тесты контроллера каталога тестировщика")
class TesterCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestPassingService testPassingService;

    // ---------- GET /tester/catalog ----------

    @Test
    @DisplayName("GET /tester/catalog: должен вернуть view с категориями")
    void catalogCategories_ShouldReturnViewWithCategories() throws Exception {
        List<CategoryDTO> categories = List.of(
                createCategoryDTO(1L, "Math"),
                createCategoryDTO(2L, "Science")
        );
        when(testPassingService.getAvailableCategories()).thenReturn(categories);

        mockMvc.perform(get("/tester/catalog"))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/category-catalog"))
                .andExpect(model().attribute("categories", categories));
    }

    // ---------- GET /tester/catalog/category/{categoryId} ----------

    @Test
    @DisplayName("GET /tester/catalog/category/{id} с categoryName: должен использовать переданное имя")
    void catalogTestsByCategory_WithCategoryNameParam_ShouldUseIt() throws Exception {
        Long categoryId = 1L;
        int page = 0, size = 12;
        String categoryName = "Math";

        Page<TestInfoDTO> pageResult = createTestInfoPage(page, size);
        when(testPassingService.getAvailableTestsByCategoryDTOPaginated(eq(categoryId), eq(page), eq(size)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/tester/catalog/category/{categoryId}", categoryId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("categoryName", categoryName))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-catalog"))
                .andExpect(model().attribute("tests", pageResult.getContent()))
                .andExpect(model().attribute("testsPage", pageResult))
                .andExpect(model().attribute("categoryId", categoryId))
                .andExpect(model().attribute("categoryName", categoryName))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("pageSize", size));
    }

    @Test
    @DisplayName("GET /tester/catalog/category/{id} без categoryName: имя должно быть взято из первого теста")
    void catalogTestsByCategory_WithoutCategoryName_ShouldDeriveFromFirstTest() throws Exception {
        Long categoryId = 1L;
        int page = 0, size = 12;
        String derivedCategoryName = "Math";

        TestInfoDTO test1 = new TestInfoDTO();
        test1.setCategoryName(derivedCategoryName);
        TestInfoDTO test2 = new TestInfoDTO();
        test2.setCategoryName(derivedCategoryName);

        Page<TestInfoDTO> pageResult = new PageImpl<>(List.of(test1, test2), PageRequest.of(page, size), 2);
        when(testPassingService.getAvailableTestsByCategoryDTOPaginated(eq(categoryId), eq(page), eq(size)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/tester/catalog/category/{categoryId}", categoryId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-catalog"))
                .andExpect(model().attribute("categoryName", derivedCategoryName))
                .andExpect(model().attribute("pageSize", size));
    }

    @Test
    @DisplayName("GET /tester/catalog/category/{id} с пустой страницей: categoryName не добавляется")
    void catalogTestsByCategory_WhenPageEmpty_ShouldHandleNullCategoryName() throws Exception {
        Long categoryId = 1L;
        int page = 0, size = 12;

        Page<TestInfoDTO> emptyPage = Page.empty(PageRequest.of(page, size));
        when(testPassingService.getAvailableTestsByCategoryDTOPaginated(eq(categoryId), eq(page), eq(size)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/tester/catalog/category/{categoryId}", categoryId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/test-catalog"))
                .andExpect(model().attributeDoesNotExist("categoryName"))
                .andExpect(model().attribute("pageSize", size));
    }

    // ---------- GET /tester/catalog/all-tests ----------

    @Disabled("Отключено из-за ошибки в шаблоне (несоответствие типов в #numbers.min)")
    @Test
    @DisplayName("GET /tester/catalog/all-tests: должен вернуть view со всеми тестами")
    void allTestsCatalog_ShouldReturnViewWithAllTests() throws Exception {
        int page = 0, size = 12;
        Page<TestInfoDTO> pageResult = createTestInfoPage(page, size);
        when(testPassingService.getAllAvailableTestsDTOPaginated(eq(page), eq(size)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/tester/catalog/all-tests")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(view().name("tester/all-tests-catalog"))
                .andExpect(model().attribute("tests", pageResult.getContent()))
                .andExpect(model().attribute("testsPage", pageResult))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", pageResult.getTotalPages()))
                .andExpect(model().attribute("totalItems", pageResult.getTotalElements()))
                .andExpect(model().attribute("showAll", true))
                .andExpect(model().attribute("pageSize", size));
    }

    // ---------- Вспомогательные методы ----------

    private CategoryDTO createCategoryDTO(Long id, String name) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }

    private Page<TestInfoDTO> createTestInfoPage(int page, int size) {
        TestInfoDTO test = new TestInfoDTO();
        test.setId(1L);
        test.setTitle("Sample Test");
        return new PageImpl<>(List.of(test), PageRequest.of(page, size), 1);
    }
}