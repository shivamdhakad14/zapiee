package com.zapiee.tableservice.repository;

import com.zapiee.tableservice.enums.QrStatus;
import com.zapiee.tableservice.model.TableQr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableQrRepository extends JpaRepository<TableQr, Long> {

    Optional<TableQr> findByRestaurantTableId(Long tableId);

    Optional<TableQr> findByQrToken(String qrToken);

    Optional<TableQr> findByQrTokenAndStatus(
            String qrToken,
            QrStatus status
    );

    boolean existsByQrToken(String qrToken);

    boolean existsByRestaurantTableId(Long tableId);

    boolean existsByRestaurantTableIdAndStatus(
            Long tableId,
            QrStatus status
    );

    Page<TableQr> findAllByRestaurantTableRestaurantId(
            Long restaurantId,
            Pageable pageable
    );

    Page<TableQr> findAllByRestaurantTableRestaurantIdAndStatus(
            Long restaurantId,
            QrStatus status,
            Pageable pageable
    );

    void deleteByRestaurantTableId(Long tableId);
}

