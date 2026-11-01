package com.Zapiee.staff_service.repository;

import com.Zapiee.staff_service.model.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<StaffRole, Long> {

    Optional<StaffRole> findByName(String name);
}
