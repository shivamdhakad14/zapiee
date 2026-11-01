package com.zapiee.menu_service.repository;

import com.zapiee.menu_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByRestaurantIdAndNameIgnoreCaseAndIsDeletedFalse(Long restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCaseAndIsDeletedFalseAndIdNot(
            Long restaurantId, String name, Long excludeId);

    long countByRestaurantIdAndIsDeletedFalse(Long restaurantId);
    long countByRestaurantIdAndIsDeletedFalseAndActiveTrue(Long restaurantId);
    List<Category> findByRestaurantIdAndIsDeletedFalseAndActiveTrueOrderByDisplayOrderAsc(Long restaurantId);
    Optional<Category> findByIdAndRestaurantIdAndIsDeletedFalse(Long id, Long restaurantId);

    @Query("""
        SELECT c.id as id, COUNT(m.id) as total,
               SUM(CASE WHEN m.isAvailable = true THEN 1 ELSE 0 END) as available
        FROM Category c LEFT JOIN MenuItem m ON m.category.id = c.id AND m.isDeleted = false
        WHERE c.id = :categoryId
        GROUP BY c.id
    """)
    CategoryItemCounts getItemCounts(@Param("categoryId") Long categoryId);

    Optional<Category> findByRestaurantIdAndNameIgnoreCaseAndIsDeletedFalse(Long restaurantId, String categoryName);

    interface CategoryItemCounts {
        Long getId();
        Long getTotal();
        Long getAvailable();
    }

}
