package com.zapiee.tableservice.repository;

import com.zapiee.tableservice.enums.TableStatus;
import com.zapiee.tableservice.model.RestaurantTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantTableRepository
        extends JpaRepository<RestaurantTable, Long> {

    boolean existsByRestaurantIdAndTableNumber(
            Long restaurantId,
            Long tableNumber
    );

    Optional<RestaurantTable> findByRestaurantIdAndTableNumber(
            Long restaurantId,
            Long tableNumber
    );

    Page<RestaurantTable> findAllByRestaurantId(
            Long restaurantId,
            Pageable pageable
    );

    Page<RestaurantTable> findAllByRestaurantIdAndActiveTrue(
            Long restaurantId,
            Pageable pageable
    );

    Page<RestaurantTable> findAllByRestaurantIdAndActive(
            Long restaurantId,
            Boolean active,
            Pageable pageable
    );

    Page<RestaurantTable> findAllByRestaurantIdAndStatus(
            Long restaurantId,
            TableStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(MAX(t.tableNumber), 0)
            FROM RestaurantTable t
            WHERE t.restaurantId = :restaurantId
            """)
    Long findMaxTableNumberByRestaurantId(
            @Param("restaurantId") Long restaurantId
    );

    long countByRestaurantId(Long restaurantId);

    long countByRestaurantIdAndActiveTrue(Long restaurantId);

    boolean existsByRestaurantId(Long restaurantId);
}

