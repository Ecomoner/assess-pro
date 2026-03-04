package com.frist.assesspro.repository;

import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.entity.Category;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestRepository testRepository;

    private User creator;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        categoryRepository.deleteAll();
        testRepository.deleteAll();

        creator = new User();
        creator.setUsername("creator");
        creator.setPassword("pass");
        creator.setRole(User.Roles.CREATOR);
        userRepository.save(creator);
    }

    @org.junit.jupiter.api.Test
    void saveAndFindById_ShouldWork() {
        Category cat = new Category();
        cat.setName("Математика");
        cat.setDescription("Тесты по математике");
        cat.setCreatedBy(creator);
        Category saved = categoryRepository.save(cat);

        Optional<Category> found = categoryRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Математика");
        assertThat(found.get().getCreatedBy().getUsername()).isEqualTo("creator");
    }

    @org.junit.jupiter.api.Test
    void existsByName_ShouldReturnTrueForExisting() {
        Category cat = new Category();
        cat.setName("Физика");
        cat.setCreatedBy(creator);
        categoryRepository.save(cat);

        assertThat(categoryRepository.existsByName("Физика")).isTrue();
        assertThat(categoryRepository.existsByName("Химия")).isFalse();
    }

    @org.junit.jupiter.api.Test
    void existsByNameAndIdNot_ShouldExcludeGivenId() {
        Category cat = new Category();
        cat.setName("Биология");
        cat.setCreatedBy(creator);
        Category saved = categoryRepository.save(cat);

        assertThat(categoryRepository.existsByNameAndIdNot("Биология", saved.getId())).isFalse();
        assertThat(categoryRepository.existsByNameAndIdNot("Биология", 999L)).isTrue();
    }

    @org.junit.jupiter.api.Test
    void countByCreatedBy_ShouldReturnCount() {
        Category cat1 = new Category();
        cat1.setName("Кат1");
        cat1.setCreatedBy(creator);
        categoryRepository.save(cat1);

        Category cat2 = new Category();
        cat2.setName("Кат2");
        cat2.setCreatedBy(creator);
        categoryRepository.save(cat2);

        assertThat(categoryRepository.countByCreatedBy(creator)).isEqualTo(2L);
    }

    @org.junit.jupiter.api.Test
    void findAllActiveCategoryDTOs_WithPageable_ShouldReturnDTOs() {
        Category cat = new Category();
        cat.setName("Программирование");
        cat.setCreatedBy(creator);
        cat.setIsActive(true);
        categoryRepository.save(cat);

        // добавим тест в категорию для проверки счётчиков
        Test test = new Test();
        test.setTitle("Тест");
        test.setCreatedBy(creator);
        test.setCategory(cat);
        test.setIsPublished(true);
        testRepository.save(test);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CategoryDTO> page = categoryRepository.findAllActiveCategoryDTOs(pageable);
        assertThat(page.getContent()).isNotEmpty();
        CategoryDTO dto = page.getContent().get(0);
        assertThat(dto.getName()).isEqualTo("Программирование");
        assertThat(dto.getTestsCount()).isEqualTo(1L);
        assertThat(dto.getPublishedTestsCount()).isEqualTo(1L);
    }

    @org.junit.jupiter.api.Test
    void findAllActiveCategoryDTOs_WithoutPageable_ShouldReturnList() {
        Category cat = new Category();
        cat.setName("Литература");
        cat.setCreatedBy(creator);
        cat.setIsActive(true);
        categoryRepository.save(cat);

        List<CategoryDTO> list = categoryRepository.findAllActiveCategoryDTOs();
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getName()).isEqualTo("Литература");
    }
}
