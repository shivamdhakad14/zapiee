package com.zapiee.mediaservice.repository;

import com.zapiee.mediaservice.enums.MediaAssetStatus;
import com.zapiee.mediaservice.enums.MediaOwnerType;
import com.zapiee.mediaservice.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    @Query("select m from MediaAsset m where m.id = :id and m.restaurantId = :tenantId and m.status = 'ACTIVE'")
    Optional<MediaAsset> findActiveByIdAndRestaurant(@Param("id") Long id, @Param("restaurantId") Long restaurantId);

    @Query("""
           select m from MediaAsset m
           where m.restaurantId = :restaurantId
             and m.ownerType = :ownerType
             and m.ownerId = :ownerId
             and m.status = 'ACTIVE'
           order by m.createdAt asc
           """)
    List<MediaAsset> findActiveByOwner(@Param("restaurantId") Long restaurantId,
                                       @Param("ownerType") MediaOwnerType ownerType,
                                       @Param("ownerId") Long ownerId);

    @Query("""
           select count(m) from MediaAsset m
           where m.restaurantId = :restaurantId
             and m.ownerType = :ownerType
             and m.ownerId = :ownerId
             and m.status = 'ACTIVE'
           """)
    long countActiveByOwner(@Param("tenantId") Long restaurantId,
                            @Param("ownerType") MediaOwnerType ownerType,
                            @Param("ownerId") Long ownerId);

    Optional<MediaAsset> findByPublicId(String publicId);

    List<MediaAsset> findByStatusAndDeletedAtIsNull(MediaAssetStatus status);
}
