package com.zapiee.menu_service.repository;

import com.zapiee.menu_service.entity.MenuImportJobPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuImportJobPageRepository extends JpaRepository<MenuImportJobPage, Long> {
    List<MenuImportJobPage> findByImportJobId(Long importJobId);
}
