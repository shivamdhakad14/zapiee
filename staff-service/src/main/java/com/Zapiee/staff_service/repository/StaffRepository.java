package com.Zapiee.staff_service.repository;

import com.Zapiee.staff_service.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findStaffByName(String name);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    Optional<Staff> findByEmail(String email);
    Optional<Staff> findByPinResetToken(String token);
    Optional<Staff> findByMobile(String mobile);



    List<Staff> findByRestaurantId(Long id);

    Optional<Staff> findByMobileOrEmail(String mobile , String email);
}
