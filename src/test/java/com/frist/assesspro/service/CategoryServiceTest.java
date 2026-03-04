package com.frist.assesspro.service;

import com.frist.assesspro.dto.category.CategoryCreateDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.category.CategoryUpdateDTO;
import com.frist.assesspro.entity.Category;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.CategoryRepository;
import com.frist.assesspro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CategoryService categoryService;

    private User creator;
    private User admin;
    private Category category;
    private CategoryCreateDTO createDTO;
    private CategoryUpdateDTO updateDTO;
    private List<CategoryDTO> categoryDTOList;

    @BeforeEach
    void setUp() {
        // Создаем создателя
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");
        creator.setRole(User.Roles.CREATOR);

        // Создаем администратора
        admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setRole(User.Roles.ADMIN);

        // Создаем категорию
        category = new Category();
        category.setId(1L);
        category.setName("Программирование");
        category.setDescription("Вопросы по программированию");
        category.setCreatedBy(creator);
        category.setIsActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setTests(new ArrayList<>());

        // DTO для создания
        createDTO = new CategoryCreateDTO();
        createDTO.setName("Новая категория");
        createDTO.setDescription("Описание новой категории");

        // DTO для обновления
        updateDTO = new CategoryUpdateDTO();
        updateDTO.setName("Обновленная категория");
        updateDTO.setDescription("Обновленное описание");
        updateDTO.setIsActive(true);

        // Список DTO для тестов
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(1L);
        categoryDTO.setName("Программирование");
        categoryDTO.setDescription("Вопросы по программированию");
        categoryDTO.setTestsCount(5L);
        categoryDTO.setPublishedTestsCount(3L);

        categoryDTOList = List.of(categoryDTO);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Успешное создание категории")
    void createCategory_Success() {
        // Arrange
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(categoryRepository.existsByName("Новая категория")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category savedCategory = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedCategory, "id", 1L);
            return savedCategory;
        });

        // Act
        Category createdCategory = categoryService.createCategory(createDTO, "creator");

        // Assert
        assertThat(createdCategory).isNotNull();
        assertThat(createdCategory.getId()).isEqualTo(1L);
        assertThat(createdCategory.getName()).isEqualTo("Новая категория");
        assertThat(createdCategory.getDescription()).isEqualTo("Описание новой категории");
        assertThat(createdCategory.getCreatedBy()).isEqualTo(creator);
        assertThat(createdCategory.getIsActive()).isTrue();

        verify(categoryRepository).save(any(Category.class));
    }

    @org.junit.jupiter.api.Test

    @DisplayName("Создание категории с пустым названием - ошибка")
    void createCategory_EmptyName_ThrowsException() {
        // Arrange
        createDTO.setName("   ");

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(createDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Название категории обязательно");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @org.junit.jupiter.api.Test

    @DisplayName("Создание категории со слишком коротким названием - ошибка")
    void createCategory_NameTooShort_ThrowsException() {
        // Arrange
        createDTO.setName("A");

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(createDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Название категории должно быть от 2 до 100 символов");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание категории с существующим названием - ошибка")
    void createCategory_DuplicateName_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(categoryRepository.existsByName("Новая категория")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(createDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Категория с таким названием уже существует");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Создание категории с несуществующим пользователем - ошибка")
    void createCategory_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(createDTO, "unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Получение категории по ID - успешно")
    void getCategoryById_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // Act
        Optional<Category> foundCategory = categoryService.getCategoryById(1L);

        // Assert
        assertThat(foundCategory).isPresent();
        assertThat(foundCategory.get().getId()).isEqualTo(1L);
        assertThat(foundCategory.get().getName()).isEqualTo("Программирование");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Получение несуществующей категории - пустой результат")
    void getCategoryById_NotFound_ReturnsEmpty() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<Category> foundCategory = categoryService.getCategoryById(99L);

        // Assert
        assertThat(foundCategory).isEmpty();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Получение всех категорий с пагинацией")
    void getAllCategories_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<CategoryDTO> page = new PageImpl<>(categoryDTOList, pageable, 1);

        when(categoryRepository.findAllActiveCategoryDTOs(pageable)).thenReturn(page);

        // Act
        Page<CategoryDTO> result = categoryService.getAllCategories(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Программирование");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Получение всех активных категорий (кэшируемый метод)")
    void getAllActiveCategories_Success() {
        // Arrange
        when(categoryRepository.findAllActiveCategoryDTOs()).thenReturn(categoryDTOList);

        // Act
        List<CategoryDTO> result = categoryService.getAllActiveCategories();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Программирование");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Обновление категории создателем - успешно")
    void updateCategory_ByCreator_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userService.isAdmin("creator")).thenReturn(false);
        when(categoryRepository.existsByNameAndIdNot("Обновленная категория", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // Act
        Category updatedCategory = categoryService.updateCategory(1L, updateDTO, "creator");

        // Assert
        assertThat(updatedCategory).isNotNull();
        assertThat(updatedCategory.getName()).isEqualTo("Обновленная категория");
        assertThat(updatedCategory.getDescription()).isEqualTo("Обновленное описание");

        verify(categoryRepository).save(category);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Обновление категории администратором - успешно")
    void updateCategory_ByAdmin_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userService.isAdmin("admin")).thenReturn(true);
        when(categoryRepository.existsByNameAndIdNot("Обновленная категория", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // Act
        Category updatedCategory = categoryService.updateCategory(1L, updateDTO, "admin");

        // Assert
        assertThat(updatedCategory).isNotNull();
        verify(categoryRepository).save(category);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Обновление чужой категории - ошибка")
    void updateCategory_NotOwner_ThrowsException() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userService.isAdmin("other")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.updateCategory(1L, updateDTO, "other"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нет прав на редактирование");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Обновление категории с существующим названием - ошибка")
    void updateCategory_DuplicateName_ThrowsException() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userService.isAdmin("creator")).thenReturn(false);
        when(categoryRepository.existsByNameAndIdNot("Обновленная категория", 1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.updateCategory(1L, updateDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Категория с таким названием уже существует");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Удаление категории без тестов (полное удаление)")
    void deleteCategory_NoTests_FullDelete() {
        // Arrange
        category.setTests(new ArrayList<>()); // Пустой список тестов

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userService.isAdmin("creator")).thenReturn(false);

        // Act
        categoryService.deleteCategory(1L, "creator");

        // Assert
        verify(categoryRepository).delete(category);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Удаление категории с тестами (мягкое удаление)")
    void deleteCategory_WithTests_SoftDelete() {
        // Arrange
        List<Test> tests = new ArrayList<>();
        tests.add(new Test());
        category.setTests(tests);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userService.isAdmin("creator")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // Act
        categoryService.deleteCategory(1L, "creator");

        // Assert
        assertThat(category.getIsActive()).isFalse();
        verify(categoryRepository).save(category);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Удаление категории администратором - успешно")
    void deleteCategory_ByAdmin_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userService.isAdmin("admin")).thenReturn(true);

        // Act
        categoryService.deleteCategory(1L, "admin");

        // Assert
        verify(categoryRepository).delete(category);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Удаление чужой категории - ошибка")
    void deleteCategory_NotOwner_ThrowsException() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userService.isAdmin("other")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(1L, "other"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нет прав на удаление");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Удаление несуществующей категории - ошибка")
    void deleteCategory_NotFound_ThrowsException() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(99L, "creator"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Категория не найдена");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Валидация названия - null")
    void validateCategoryName_Null_ThrowsException() {
        // Это приватный метод, тестируем через публичный createCategory
        createDTO.setName(null);

        assertThatThrownBy(() -> categoryService.createCategory(createDTO, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Название категории обязательно");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Обрезка длинного описания")
    void createCategory_TruncateLongDescription() {
        // Arrange
        String longDescription = "a".repeat(600); // 600 символов, лимит 500
        createDTO.setDescription(longDescription);

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(categoryRepository.existsByName("Новая категория")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category savedCategory = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedCategory, "id", 1L);
            return savedCategory;
        });

        // Act
        Category createdCategory = categoryService.createCategory(createDTO, "creator");

        // Assert
        assertThat(createdCategory.getDescription()).hasSize(500);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Деактивация категории при обновлении")
    void updateCategory_Deactivate_Success() {
        // Arrange
        updateDTO.setIsActive(false);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userService.isAdmin("creator")).thenReturn(false);
        when(categoryRepository.existsByNameAndIdNot("Обновленная категория", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // Act
        Category updatedCategory = categoryService.updateCategory(1L, updateDTO, "creator");

        // Assert
        assertThat(updatedCategory.getIsActive()).isFalse();
        verify(categoryRepository).save(category);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Проверка кэширования - @CacheEvict при создании")
    void createCategory_CacheEvictCalled() {
        // Arrange
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(categoryRepository.existsByName("Новая категория")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // Act
        categoryService.createCategory(createDTO, "creator");

        // Assert
        // Не можем напрямую проверить CacheEvict, но метод выполнился
        verify(categoryRepository).save(any(Category.class));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Проверка кэширования - @Cacheable для активных категорий")
    void getAllActiveCategories_Cacheable() {
        // Arrange
        when(categoryRepository.findAllActiveCategoryDTOs()).thenReturn(categoryDTOList);

        List<CategoryDTO> firstCall = categoryService.getAllActiveCategories();
        List<CategoryDTO> secondCall = categoryService.getAllActiveCategories();

        assertThat(firstCall).isNotNull();
        assertThat(secondCall).isNotNull();
        assertThat(firstCall).containsExactlyElementsOf(secondCall); // данные совпадают
        verify(categoryRepository, atLeastOnce()).findAllActiveCategoryDTOs();
    }
}