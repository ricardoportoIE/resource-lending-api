package com.ricardoporto.lending.reservation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
  boolean existsByUserIdAndResourceIdAndStatusIn(
      Long userId, UUID resourceId, Collection<ReservationStatus> statuses);

  List<Reservation> findAllByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<Reservation> findFirstByResourceIdAndStatusOrderByCreatedAt(
      UUID resourceId, ReservationStatus status);

  Optional<Reservation> findByUserIdAndReadyItemIdAndStatus(
      Long userId, UUID itemId, ReservationStatus status);

  List<Reservation> findAllByStatusAndExpiresAtBefore(ReservationStatus status, Instant threshold);

  long countByResourceIdAndStatusAndCreatedAtLessThanEqual(
      UUID resourceId, ReservationStatus status, Instant createdAt);
}
