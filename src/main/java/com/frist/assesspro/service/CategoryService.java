package com.frist.assesspro.service;

import com.frist.assesspro.dto.category.CategoryCreateDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.category.CategoryUpdateDTO;
import com.frist.assesspro.entity.Category;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.CategoryRepository;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    /**
     * Создание новой категории
     */
    @Transactional
    @CacheEvict(value = {"categories", "activeCategories"}, allEntries = true)
    public Category createCategory(CategoryCreateDTO categoryCreateDTO, String username) {
        validateCategoryName(categoryCreateDTO.getName());

        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверка уникальности названия
        if (categoryRepository.existsByName(categoryCreateDTO.getName().trim())) {
            throw new IllegalArgumentException("Категория с таким названием уже существует");
        }

        Category category = new Category();
        category.setName(categoryCreateDTO.getName().trim());

        String description = categoryCreateDTO.getDescription();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAX_DESCRIPTION_LENGTH);
            log.warn("Описание категории обрезано до {} символов", MAX_DESCRIPTION_LENGTH);
        }
        category.setDescription(description);
        category.setCreatedBy(creator);
        category.setIsActive(true);

        Category savedCategory = categoryRepository.save(category);
        log.info("Создана новая категория '{}' (ID: {}) пользователем {}",
                savedCategory.getName(), savedCategory.getId(), username);

        return savedCategory;
    }
    /**
     * Получение категории по ID с проверкой прав
     */
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId);
    }

    /**
     * Получение категории по ID (без проверки прав, для публичного доступа)
     */

    /**
     * Получение DTO списка категорий создателя
     */
    @Transactional(readOnly = true)
    public Page<CategoryDTO> getAllCategories(Pageable pageable) {
        return categoryRepository.findAllActiveCategoryDTOs(pageable);
    }

    /**
     * Получение всех активных категорий для тестера
     */
    @Cacheable(value = "activeCategories", unless = "#result == null or #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllActiveCategories() {
        return categoryRepository.findAllActiveCategoryDTOs();
    }

    /**
     * Обновление категории
     */
    @Transactional
    @CacheEvict(value = {"categories", "activeCategories"}, allEntries = true)
    public Category updateCategory(Long categoryId, CategoryUpdateDTO updateDTO, String username) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));

        if (!userService.isAdmin(username) && !category.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав на редактирование этой категории");
        }

        // Проверка уникальности названия (исключая текущую категорию)
        if (!category.getName().equalsIgnoreCase(updateDTO.getName().trim()) &&
                categoryRepository.existsByNameAndIdNot(updateDTO.getName().trim(), categoryId)) {
            throw new IllegalArgumentException("Категория с таким названием уже существует");
        }

        category.setName(updateDTO.getName().trim());

        String description = updateDTO.getDescription();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAX_DESCRIPTION_LENGTH);
        }
        category.setDescription(description);

        if (updateDTO.getIsActive() != null) {
            category.setIsActive(updateDTO.getIsActive());
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Обновлена категория '{}' (ID: {})", updatedCategory.getName(), categoryId);

        return updatedCategory;
    }

    /**
     * Удаление категории (мягкое удаление)
     */
    @Transactional
    @CacheEvict(value = {"categories", "activeCategories"}, allEntries = true)
    public void deleteCategory(Long categoryId, String username) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));

        if (!userService.isAdmin(username) && !category.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Нет прав на удаление этой категории");
        }

        // Проверяем, есть ли тесты в категории
        if (category.getTests() != null && !category.getTests().isEmpty()) {
            // Мягкое удаление - просто деактивируем
            category.setIsActive(false);
            categoryRepository.save(category);
            log.info("Категория '{}' (ID: {}) деактивирована, содержит {} тестов",
                    category.getName(), categoryId, category.getTests().size());
        } else {
            // Если тестов нет, удаляем полностью
            categoryRepository.delete(category);
            log.info("Категория '{}' (ID: {}) полностью удалена", category.getName(), categoryId);
        }
    }

    /**
     * Валидация названия категории
     */
    private void validateCategoryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название категории обязательно");
        }

        String trimmedName = name.trim();
        if (trimmedName.length() < 2 || trimmedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Название категории должно быть от 2 до %d символов", MAX_NAME_LENGTH));
        }
    }

}
