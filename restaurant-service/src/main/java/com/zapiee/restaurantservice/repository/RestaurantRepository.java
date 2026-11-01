package com.zapiee.restaurantservice.repository;

import com.zapiee.restaurantservice.enums.RestaurantStatus;
import com.zapiee.restaurantservice.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant,Long> {
    Optional<Restaurant> findByOwnerId(Long ownerId);
    boolean existsByOwnerId(Long ownerId);
    boolean existsByBusinessContact(String businessContact);
    boolean existsByBusinessEmail(String businessEmail);
    Optional<Restaurant> findById(Long restaurantId);

    Optional<Restaurant> findByBusinessEmail(String businessEmail);
    Optional<Restaurant> findByBusinessContact(String businessContact);

    List<Restaurant> findByIsActiveTrue();
    List<Restaurant> findByIsActiveFalse();
    List<Restaurant> findByRestaurantStatus(RestaurantStatus restaurantStatus);


}
