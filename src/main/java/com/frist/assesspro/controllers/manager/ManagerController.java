package com.frist.assesspro.controllers.manager;

import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.dto.manager.ProjectDTO;
import com.frist.assesspro.dto.statistics.ProjectAggregatedStatsDTO;
import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.statistics.TesterProjectStatsDTO;
import com.frist.assesspro.dto.statistics.TesterProjectStatsProjection;
import com.frist.assesspro.entity.Project;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.EventService;
import com.frist.assesspro.service.ManagerService;
import com.frist.assesspro.service.ProjectService;
import com.frist.assesspro.service.export.AsyncPdfExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {


    private final ManagerService managerService;
    private final EventService eventService;
    private final AsyncPdfExportService  pdfExportService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        String username = principal.getName();
        List<ProjectDTO> projects = managerService.getManagerProjects(username);
        List<EventDTO> lastEvents = eventService.getLastEvents(5);
        List<TesterAttemptDTO> recentAttempts = managerService.getRecentAttemptsForManager(username, 5);

        model.addAttribute("projects", projects);
        model.addAttribute("lastEvents", lastEvents);
        model.addAttribute("recentAttempts", recentAttempts);
        return "manager/dashboard";
    }

    @GetMapping("/projects/{id}")
    public String projectStatistics(@PathVariable Long id, Model model, Principal principal) {
        // Проверка доступа: проект должен принадлежать текущему менеджеру
        List<ProjectDTO> projects = managerService.getManagerProjects(principal.getName());
        ProjectDTO currentProject = projects.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Проект не найден или доступ запрещен"));

        ProjectAggregatedStatsDTO stats = managerService.getProjectAggregatedStats(id);
        long testersCount = managerService.getTesterCount(id);
        List<TesterProjectStatsProjection> testers = managerService.getTesterStatsByProject(id);

        model.addAttribute("project", currentProject);
        model.addAttribute("testersCount", testersCount);
        model.addAttribute("completedAttempts", stats.getCompletedAttempts());
        model.addAttribute("averageScore", stats.getAverageScore());
        model.addAttribute("testers", testers);

        return "manager/project-statistics";
    }

    @GetMapping("/tester/{testerUsername}/full-statistics/export")
    @Transactional
    public ResponseEntity<Map<String, String>> exportTesterFullStatistics(
            @PathVariable String testerUsername,
            Principal principal) {

        User tester = managerService.getTesterWithAccessCheck(testerUsername, principal.getName());
        List<TesterAttemptDTO> attempts = managerService.getAllAttemptsByTesterForManager(testerUsername, principal.getName());

        if (attempts.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Нет попыток для экспорта"));
        }

        String requestId = UUID.randomUUID().toString();
        pdfExportService.generateTesterFullStatistics(tester, attempts, requestId);
        return ResponseEntity.ok(Map.of("requestId", requestId, "message", "Полный отчёт по тестировщику готовится..."));
    }

}
