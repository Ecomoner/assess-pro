package com.frist.assesspro.controllers;

import com.frist.assesspro.entity.Test;
import com.frist.assesspro.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/tester")
@PreAuthorize("hasRole('TESTER')")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TesterController {

    private final TestRepository testRepository;

    @GetMapping("/dashboard")
    public String testerDashboard(Model model){
        model.addAttribute("message","Добро пожаловать в каталог тестов");
        return "tester/dashboard";
    }

    @GetMapping("/tests")
    public String testCatalog(Model model){
        List<Test> tests = testRepository.findByIsPublishedTrue();
        model.addAttribute("tests",tests);
        return "/tester/test-catalog";
    }

    @GetMapping("/test/{id}")
    public String startTest(@PathVariable Long id,Model model){
        Test test = testRepository.findByIdAndIsPublishedTrue(id)
                .orElseThrow();
        model.addAttribute("test",test);
        return "tester/test-start";
    }


}
