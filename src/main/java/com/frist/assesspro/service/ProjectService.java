package com.frist.assesspro.service;


import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.dto.admin.ProjectViewDTO;
import com.frist.assesspro.dto.manager.ProjectDTO;
import com.frist.assesspro.entity.Event;
import com.frist.assesspro.entity.Project;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.mapper.ProjectMapper;
import com.frist.assesspro.repository.ProjectRepository;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    /**
     * Получить все проекты с пагинацией и поиском по названию.
     */
    @Transactional(readOnly = true)
    public Page<ProjectDTO> getAllProjects(String search, Pageable pageable) {

        Page<Project> projectsPage = (search != null && !search.trim().isEmpty())
                ? projectRepository.findByNameContainingIgnoreCase(search.trim(), pageable)
                : projectRepository.findAll(pageable);

        return projectsPage.map(project -> {
            ProjectDTO dto = projectMapper.toDto(project);
            long count = userRepository.countUsersByProjectId(project.getId());
            dto.setTestersCount(count);
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllActiveProjects() {
        return projectRepository.findByActiveTrue().stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    @Transactional
    public Project createProject(String name, Long managerId,boolean active) {
        if (projectRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("Проект с таким названием уже существует");
        }

        Project project = new Project();
        project.setName(name.trim());
        project.setActive(active);

        if (managerId != null) {
            User manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new RuntimeException("Менеджер не найден"));
            project.setManager(manager);
        }

        Project saved = projectRepository.save(project);
        log.info("Создан проект '{}' (ID: {})", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public Project updateProject(Long id, String name, Long managerId, Boolean active) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        if (!project.getName().equalsIgnoreCase(name.trim()) &&
                projectRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("Проект с таким названием уже существует");
        }

        project.setName(name.trim());

        if (managerId != null) {
            User manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new RuntimeException("Менеджер не найден"));
            project.setManager(manager);
        } else {
            project.setManager(null);
        }

        if (active != null) {
            project.setActive(active);
        }

        Project updated = projectRepository.save(project);
        log.info("Обновлён проект '{}' (ID: {})", updated.getName(), updated.getId());
        return updated;
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        // Проверяем, есть ли пользователи, привязанные к проекту
        long userCount = userRepository.countByProjectId(id);
        if (userCount > 0) {
            throw new IllegalStateException(
                    "Нельзя удалить проект, к которому привязаны пользователи (" + userCount + " чел.)");
        }

        projectRepository.delete(project);
        log.info("Проект '{}' (ID: {}) удалён", project.getName(), id);
    }

    /**
     * Получить DTO для страницы просмотра проекта.
     */
    @Transactional(readOnly = true)
    public ProjectViewDTO getProjectView(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        ProjectViewDTO dto = new ProjectViewDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setActive(project.getActive());

        if (project.getManager() != null) {
            User m = project.getManager();
            dto.setManager(new ProjectViewDTO.ManagerInfo(m.getId(), m.getUsername(),  m.getFullName()));
        }

        List<User> testers = userRepository.findByProjectId(projectId);
        List<ProjectViewDTO.TesterInfo> testerInfos = testers.stream()
                .map(t -> new ProjectViewDTO.TesterInfo(t.getId(),t.getUsername(),t.getFullName()))
                .collect(Collectors.toList());
        dto.setTesters(testerInfos);
        return dto;
    }

    /**
     * Назначить менеджера проекта. Менеджер должен иметь роль MANAGER.
     */
    @Transactional()
    public void assignManager(Long projectId, Long managerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!manager.getRole().equals(User.Roles.MANAGER)){
            throw new IllegalArgumentException("Пользователь не является менеджером");
        }
        project.setManager(manager);
        projectRepository.save(project);
        log.info("Менеджер {} назначен на проект {}", manager.getUsername(), project.getName());
    }

    /**
     * Добавить тестеров к проекту (список ID). Все они должны иметь роль TESTER.
     * При добавлении пользователь автоматически покидает предыдущий проект.
     */

    @Transactional()
    public void addTesterToProject(Long projectId, List<Long> testerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        List<User> testers = userRepository.findAllById(testerId);
        for (User tester : testers) {
            if (!tester.getRole().equals(User.Roles.TESTER)){
                throw new IllegalArgumentException("Пользователь не является тестировщиком");
            }
            tester.setProject(project);
        }
        userRepository.saveAll(testers);
        log.info("Добавлены тестеры в проект {}: {}", project.getName(), testers.stream().map(User::getUsername).toList());


    }

    /**
     * Удалить тестера из проекта.
     */
    @Transactional
    public void removeTesterFromProject(Long projectId, Long userId) {
        User tester = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (tester.getProject() == null || !tester.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Пользователь не принадлежит этому проекту");
        }
        tester.setProject(null);
        userRepository.save(tester);
        log.info("Тестер {} удалён из проекта", tester.getUsername());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByManagerId(Long managerId) {
        return projectRepository.findByManagerId(managerId).stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());
    }


    public List<Project> getProjectsByManager(Long id) {
        return projectRepository.findByManagerId(id);
    }

}
