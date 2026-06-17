package com.frist.assesspro.controllers.manager;

import com.frist.assesspro.entity.Project;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final ProjectService projectService;


    @GetMapping("/dashboard")
    public String dashboard(Model model,@AuthenticationPrincipal User user) {

        List<Project> projects = projectService.getProjectsByManager(user.getId());
        model.addAttribute("projects",projects);
        return "manager/dashboard";
    }


}
