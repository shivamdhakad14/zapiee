package com.Zapiee.staff_service.repository;

import com.Zapiee.staff_service.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken , Long> {

    Optional<RefreshToken> findByToken(String token);
    void deleteByStaffId(Long staffId);



}
