package com.frist.assesspro.controllers.admin;

import com.frist.assesspro.dto.admin.ProjectViewDTO;
import com.frist.assesspro.dto.manager.ProjectDTO;
import com.frist.assesspro.entity.Project;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import com.frist.assesspro.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/projects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    @GetMapping()
    public String projectList(@RequestParam (defaultValue = "0") int page,
                              @RequestParam (defaultValue = "10") int size,
                              @RequestParam(required = false) String search,
                              Model model){
        Pageable pageable = PageRequest.of(page,size, Sort.by("name").ascending());
        Page<ProjectDTO> projectsPage = projectService.getAllProjects(search, pageable);

        model.addAttribute("projects", projectsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", projectsPage.getTotalPages());
        model.addAttribute("totalItems", projectsPage.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("pageSize", size);
        return "admin/project-list";
    }

    @GetMapping("/new")
    public String showCreateProjectForm(Model model){
        List<User> managers = userRepository.findByRole("MANAGER");
        model.addAttribute("project", new Project());
        model.addAttribute("managers", managers);
        model.addAttribute("formAction", "/admin/projects/new");
        return "admin/project-form";
    }

    @PostMapping("/new")
    public String createProject(@RequestParam String name,
                                @RequestParam(required = false) Long managerId,
                                @RequestParam(defaultValue = "true") boolean active,
                                RedirectAttributes redirectAttributes){

        try {
            projectService.createProject(name, managerId,active);
            redirectAttributes.addFlashAttribute("message", "Проект создан");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/projects";
    }

    @GetMapping("/edit/{id}")
    public String showEditProjectForm(@PathVariable Long id, Model model,
                                      RedirectAttributes redirectAttributes){
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        List<User> managers = userRepository.findByRole(User.Roles.MANAGER);

        model.addAttribute("project", project);
        model.addAttribute("managers", managers);
        model.addAttribute("formAction", "/admin/projects/update/" + id);
        return "admin/project-form";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) Long managerId,
                         @RequestParam(required = false) boolean active,
                         RedirectAttributes redirectAttributes){
    try {
        projectService.updateProject(id, name, managerId, active);
        redirectAttributes.addFlashAttribute("successMessage","Проект обновлен");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/admin/projects";
    }

    @PostMapping("/delete/{id}")
    public String deleteProject(@PathVariable Long id,RedirectAttributes redirectAttributes){
        try {
            projectService.deleteProject(id);
            redirectAttributes.addFlashAttribute("successMessage","Проект удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/projects";
    }

    @GetMapping("/view/{id}")
    public String viewProject(@PathVariable Long id, Model model){
        ProjectViewDTO projectViewDTO = projectService.getProjectView(id);
        model.addAttribute("project", projectViewDTO);
        List<User> managers = userRepository.findByRole(User.Roles.MANAGER);
        List<User> testers = userRepository.findByRole(User.Roles.TESTER);
        model.addAttribute("managers", managers);
        model.addAttribute("testers", testers);
        return "admin/project-view";
    }

    @PostMapping("view/{id}/assign-manager")
    public String assignManagerProject(@PathVariable Long id,
                                       @RequestParam Long managerId,
                                       RedirectAttributes redirectAttributes){
        try {
            projectService.assignManager(id, managerId);
            redirectAttributes.addFlashAttribute("successMessage","Менеджер назначен");
        }catch (Exception e){
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/projects/view/" + id;
    }

    @PostMapping("view/{id}/add-testers")
    public String addTestersProject(@PathVariable Long id,
                                    @RequestParam List<Long> testersId,
                                    RedirectAttributes redirectAttributes){
        try {
            projectService.addTesterToProject(id, testersId);
            redirectAttributes.addFlashAttribute("successMessage","Тестеры добавлены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/projects/view/" + id;
    }

    @PostMapping("view/{id}/remove-tester")
    public String removeTesterProject(@PathVariable Long id,
                                      @RequestParam Long testerId,
                                      RedirectAttributes redirectAttributes){
        try {
            projectService.removeTesterFromProject(id, testerId);
            redirectAttributes.addFlashAttribute("successMessage","Тестер удален");
        }catch (Exception e){
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/projects/view/" + id;
    }
}
