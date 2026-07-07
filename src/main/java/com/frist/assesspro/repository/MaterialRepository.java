package com.frist.assesspro.repository;


import com.frist.assesspro.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface MaterialRepository  extends JpaRepository<Material, Long> {

}
