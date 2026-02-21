package com.frist.assesspro.repository;

import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.entity.Category;
import com.frist.assesspro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(value = "Category.withCreatorAndTests", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Category> findById(Long id);

    @EntityGraph(value = "Category.withCreatorAndTests", type = EntityGraph.EntityGraphType.LOAD)
    List<Category> findByCreatedBy(User createdBy);

    @EntityGraph(value = "Category.withCreatorAndTests", type = EntityGraph.EntityGraphType.LOAD)
    Page<Category> findByCreatedBy(User createdBy, Pageable pageable);

    @EntityGraph(value = "Category.withCreatorAndTests", type = EntityGraph.EntityGraphType.LOAD)
    List<Category> findByIsActiveTrue();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.createdBy = :creator")
    long countByCreatedBy(@Param("creator") User creator);

    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.name")
    List<Category> findAllActiveOrderedByName();

    @Query("SELECT new com.frist.assesspro.dto.category.CategoryDTO(" +
            "c.id, c.name, c.description, c.createdAt, COUNT(t.id), " +
            "SUM(CASE WHEN t.isPublished = true THEN 1 ELSE 0 END)) " +
            "FROM Category c " +
            "LEFT JOIN c.tests t " +
            "WHERE c.isActive = true " +
            "GROUP BY c.id, c.name, c.description, c.createdAt " +
            "ORDER BY c.name")
    Page<CategoryDTO> findAllActiveCategoryDTOs(Pageable pageable);

    @Query("SELECT new com.frist.assesspro.dto.category.CategoryDTO(" +
            "c.id, c.name, c.description, c.createdAt, COUNT(t.id), " +
            "SUM(CASE WHEN t.isPublished = true THEN 1 ELSE 0 END)) " +
            "FROM Category c " +
            "LEFT JOIN c.tests t " +
            "WHERE c.isActive = true " +
            "GROUP BY c.id, c.name, c.description, c.createdAt " +
            "ORDER BY c.name")
    List<CategoryDTO> findAllActiveCategoryDTOs();

    @EntityGraph(value = "Category.withCreatorAndTests", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.createdBy = :creator AND c.isActive = true")
    Optional<Category> findActiveByIdAndCreatedBy(@Param("id") Long id, @Param("creator") User createdBy);
}