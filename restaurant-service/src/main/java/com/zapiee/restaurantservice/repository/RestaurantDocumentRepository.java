package com.zapiee.restaurantservice.repository;

import com.zapiee.restaurantservice.enums.DocumentType;
import com.zapiee.restaurantservice.model.RestaurantDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantDocumentRepository extends JpaRepository<RestaurantDocument, Long> {

    List<RestaurantDocument> findByRestaurantId(Long restaurantId);

    Optional<RestaurantDocument> findByRestaurantIdAndDocumentType(
            Long restaurantId,
            DocumentType documentType
    );

    Optional<RestaurantDocument> findByRestaurant_Id(Long restaurantId);

    boolean existsByRestaurantIdAndDocumentType(
            Long restaurantId,
            DocumentType documentType
    );
}