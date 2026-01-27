package com.frist.assesspro.repository;

import com.frist.assesspro.model.Question;
import com.frist.assesspro.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    User findByUsername(String username);
    boolean existsByUsername(String username);



}
