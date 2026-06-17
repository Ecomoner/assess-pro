package com.frist.assesspro.repository;

import com.frist.assesspro.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @EntityGraph(attributePaths = {"manager"})
    Page<Project> findAll(Pageable pageable);
    List<Project> findByManagerId(Long managerId);
    List<Project> findByActiveTrue();
    boolean existsByName(String name);

    @EntityGraph(attributePaths = {"manager"})
    Page<Project> findByNameContainingIgnoreCase(String trim, Pageable pageable);

}
