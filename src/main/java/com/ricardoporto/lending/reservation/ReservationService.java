package com.ricardoporto.lending.reservation;

import com.ricardoporto.lending.audit.AuditService;
import com.ricardoporto.lending.outbox.OutboxService;
import com.ricardoporto.lending.resource.Resource;
import com.ricardoporto.lending.resource.ResourceItem;
import com.ricardoporto.lending.resource.ResourceItemRepository;
import com.ricardoporto.lending.resource.ResourceItemStatus;
import com.ricardoporto.lending.resource.ResourceRepository;
import com.ricardoporto.lending.shared.exception.ApiException;
import com.ricardoporto.lending.shared.exception.ResourceNotFoundException;
import com.ricardoporto.lending.shared.security.AuthorizationService;
import com.ricardoporto.lending.user.Usuario;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
  private static final EnumSet<ReservationStatus> OPEN_STATUSES =
      EnumSet.of(ReservationStatus.WAITING, ReservationStatus.READY);

  private final ReservationRepository reservationRepository;
  private final ResourceRepository resourceRepository;
  private final ResourceItemRepository itemRepository;
  private final AuthorizationService authorizationService;
  private final AuditService auditService;
  private final OutboxService outboxService;
  private final Duration readyWindow;

  public ReservationService(
      ReservationRepository reservationRepository,
      ResourceRepository resourceRepository,
      ResourceItemRepository itemRepository,
      AuthorizationService authorizationService,
      AuditService auditService,
      OutboxService outboxService,
      @Value("${api.reservation.ready-window}") Duration readyWindow) {
    this.reservationRepository = reservationRepository;
    this.resourceRepository = resourceRepository;
    this.itemRepository = itemRepository;
    this.authorizationService = authorizationService;
    this.auditService = auditService;
    this.outboxService = outboxService;
    this.readyWindow = readyWindow;
  }

  @Transactional
  public ReservationResponse reserve(UUID resourceId) {
    var user = authorizationService.currentUser();
    var resource = requireResource(resourceId);
    if (reservationRepository.existsByUserIdAndResourceIdAndStatusIn(
        user.getId(), resourceId, OPEN_STATUSES)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "RESERVATION_ALREADY_EXISTS",
          "The user already has an active reservation for this resource.");
    }

    var reservation = new Reservation();
    reservation.setId(UUID.randomUUID());
    reservation.setUser(user);
    reservation.setResource(resource);
    reservation.setCreatedAt(Instant.now());
    var available =
        itemRepository.findFirstByResourceIdAndStatusOrderByAssetTag(
            resourceId, ResourceItemStatus.AVAILABLE);
    if (available.isPresent()) {
      makeReady(reservation, available.get(), Instant.now());
    } else {
      reservation.setStatus(ReservationStatus.WAITING);
    }
    var saved = reservationRepository.save(reservation);
    auditService.record(
        "RESERVATION_CREATED", "Reservation", saved.getId(), "status=" + saved.getStatus());
    if (saved.getStatus() == ReservationStatus.READY) publishReady(saved);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<ReservationResponse> findAll() {
    var user = authorizationService.currentUser();
    var reservations =
        authorizationService.isStaffOrAdmin()
            ? reservationRepository.findAll()
            : reservationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    return reservations.stream().map(this::toResponse).toList();
  }

  @Transactional
  public void cancel(UUID id) {
    var reservation = requireReservation(id);
    var current = authorizationService.currentUser();
    if (!authorizationService.isStaffOrAdmin()
        && !reservation.getUser().getId().equals(current.getId())) {
      throw new AccessDeniedException("Reservation does not belong to the authenticated user.");
    }
    if (!OPEN_STATUSES.contains(reservation.getStatus())) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "INVALID_RESERVATION_TRANSITION",
          "Only waiting or ready reservations can be cancelled.");
    }
    var readyItem = reservation.getReadyItem();
    reservation.setStatus(ReservationStatus.CANCELLED);
    reservation.setReadyItem(null);
    auditService.record(
        "RESERVATION_CANCELLED", "Reservation", reservation.getId(), "status=CANCELLED");
    if (readyItem != null) {
      var lockedItem = requireItemForUpdate(readyItem.getId());
      lockedItem.setStatus(ResourceItemStatus.AVAILABLE);
      lockedItem.setUpdatedAt(Instant.now());
      promoteNext(lockedItem, false);
    }
  }

  @Transactional
  public boolean claimReadyReservation(Usuario user, ResourceItem lockedItem) {
    var reservation =
        reservationRepository.findByUserIdAndReadyItemIdAndStatus(
            user.getId(), lockedItem.getId(), ReservationStatus.READY);
    if (reservation.isEmpty()) return false;
    var claimed = reservation.get();
    claimed.setStatus(ReservationStatus.FULFILLED);
    claimed.setReadyItem(null);
    lockedItem.setStatus(ResourceItemStatus.AVAILABLE);
    lockedItem.setUpdatedAt(Instant.now());
    auditService.record(
        "RESERVATION_FULFILLED", "Reservation", claimed.getId(), "status=FULFILLED");
    return true;
  }

  @Transactional
  public void promoteNext(ResourceItem lockedItem) {
    promoteNext(lockedItem, true);
  }

  @Scheduled(fixedDelayString = "${api.reservation.expiry-scan-ms:60000}")
  @Transactional
  public void expireReadyReservations() {
    var now = Instant.now();
    reservationRepository
        .findAllByStatusAndExpiresAtBefore(ReservationStatus.READY, now)
        .forEach(
            reservation -> {
              var item = requireItemForUpdate(reservation.getReadyItem().getId());
              reservation.setStatus(ReservationStatus.EXPIRED);
              reservation.setReadyItem(null);
              item.setStatus(ResourceItemStatus.AVAILABLE);
              item.setUpdatedAt(now);
              auditService.recordSystem(
                  "RESERVATION_EXPIRED", "Reservation", reservation.getId(), "status=EXPIRED");
              promoteNext(item, false);
            });
  }

  private void promoteNext(ResourceItem item, boolean recordActor) {
    var next =
        reservationRepository.findFirstByResourceIdAndStatusOrderByCreatedAt(
            item.getResource().getId(), ReservationStatus.WAITING);
    if (next.isEmpty()) return;
    var reservation = next.get();
    makeReady(reservation, item, Instant.now());
    if (recordActor) {
      auditService.record("RESERVATION_READY", "Reservation", reservation.getId(), "status=READY");
    } else {
      auditService.recordSystem(
          "RESERVATION_READY", "Reservation", reservation.getId(), "status=READY");
    }
    publishReady(reservation);
  }

  private void publishReady(Reservation reservation) {
    outboxService.publish(
        "RESERVATION_READY",
        "Reservation",
        reservation.getId(),
        Map.of(
            "reservationId", reservation.getId(),
            "userEmail", reservation.getUser().getEmail(),
            "resourceId", reservation.getResource().getId(),
            "resourceItemId", reservation.getReadyItem().getId(),
            "expiresAt", reservation.getExpiresAt()),
        "RESERVATION_READY:" + reservation.getId());
  }

  private void makeReady(Reservation reservation, ResourceItem item, Instant now) {
    reservation.setStatus(ReservationStatus.READY);
    reservation.setReadyItem(item);
    reservation.setReadyAt(now);
    reservation.setExpiresAt(now.plus(readyWindow));
    item.setStatus(ResourceItemStatus.RESERVED);
    item.setUpdatedAt(now);
    itemRepository.save(item);
  }

  private ReservationResponse toResponse(Reservation reservation) {
    long position =
        reservation.getStatus() == ReservationStatus.WAITING
            ? reservationRepository.countByResourceIdAndStatusAndCreatedAtLessThanEqual(
                reservation.getResource().getId(),
                ReservationStatus.WAITING,
                reservation.getCreatedAt())
            : 0;
    return new ReservationResponse(
        reservation.getId(),
        reservation.getUser().getId(),
        reservation.getResource().getId(),
        reservation.getResource().getName(),
        reservation.getReadyItem() == null ? null : reservation.getReadyItem().getId(),
        reservation.getStatus(),
        position,
        reservation.getCreatedAt(),
        reservation.getReadyAt(),
        reservation.getExpiresAt());
  }

  private Resource requireResource(UUID id) {
    return resourceRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
  }

  private Reservation requireReservation(UUID id) {
    return reservationRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
  }

  private ResourceItem requireItemForUpdate(UUID id) {
    return itemRepository
        .findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("Resource item", id));
  }
}
