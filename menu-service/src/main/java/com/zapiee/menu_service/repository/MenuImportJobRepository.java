package com.zapiee.menu_service.repository;

import com.zapiee.menu_service.entity.MenuImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuImportJobRepository extends JpaRepository<MenuImportJob, Long> {
}
