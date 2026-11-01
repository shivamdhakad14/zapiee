package com.zapiee.menu_service.repository;

import com.zapiee.menu_service.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    boolean existsByCategoryIdAndNameIgnoreCaseAndIsDeletedFalse(Long categoryId, String name);

    boolean existsByCategoryIdAndNameIgnoreCaseAndIsDeletedFalseAndIdNot(
            Long categoryId, String name, Long excludeId);

    long countByCategoryIdAndIsDeletedFalse(Long categoryId);

    long countByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    long countByRestaurantIdAndIsDeletedFalseAndIsAvailableTrue(Long restaurantId);

    long countByRestaurantIdAndIsDeletedFalseAndIsAvailableFalse(Long restaurantId);

    long countByRestaurantIdAndIsDeletedFalseAndStockCount(Long restaurantId, Integer stockCount);

    List<MenuItem> findByRestaurantIdAndIsDeletedFalseOrderByDisplayOrderAsc(Long restaurantId);

    List<MenuItem> findByRestaurantIdAndIsDeletedFalseAndIsAvailableTrueOrderByDisplayOrderAsc(Long restaurantId);

    @Query(value = """
        SELECT * FROM menu_items
        WHERE restaurant_id = :restaurantId
          AND is_deleted = false
          AND is_available = true
          AND search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC
        """, nativeQuery = true)
    List<MenuItem> searchPublicMenu(@Param("restaurantId") Long restaurantId, @Param("query") String query);

    List<MenuItem> findByCategoryIdAndIsDeletedFalseAndIsAvailableTrue(Long categoryId);
    Page<MenuItem> findByRestaurantIdAndCategoryIdAndIsDeletedFalse(Long restaurantId, Long categoryId, Pageable pageable);
    Optional<MenuItem> findByIdAndRestaurantIdAndIsDeletedFalse(Long id, Long restaurantId);

    Page<MenuItem> findAll(Specification<MenuItem> menuItemSpecification, Pageable pageable);

    Optional<MenuItem> findFirstByRestaurantIdAndNameIgnoreCaseAndIsDeletedFalse(
            Long restaurantId,
            String name
    );}
