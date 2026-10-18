package com.zapiee.mediaservice.repository;

import com.zapiee.mediaservice.model.MediaIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface MediaIdempotencyKeyRepository extends JpaRepository<MediaIdempotencyKey, String> {

    Optional<MediaIdempotencyKey> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("delete from MediaIdempotencyKey k where k.expiresAt < :now")
    int deleteExpired(Instant now);
}
