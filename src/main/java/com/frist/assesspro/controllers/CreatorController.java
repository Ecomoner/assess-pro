package com.frist.assesspro.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class CreatorController {

    @GetMapping("/creator/tests")
    public String getAllTests(){

    }

    @PostMapping("/creator/tests/new")
    public String createNewTest(){

    }

    @PostMapping("/creator/tests/")
    public String createTestUpdate(){

    }

    @DeleteMapping("/creator/tests")
    public String deleteTest(){

    }


}
