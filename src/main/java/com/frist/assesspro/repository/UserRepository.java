package com.frist.assesspro.repository;

import com.frist.assesspro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {

    User findByUsername(String username);
    boolean existsByUsername(String username);



}
