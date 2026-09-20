package com.smartrebook.booking.repository;

import com.smartrebook.booking.domain.ChangeRequest;
import com.smartrebook.booking.domain.ChangeRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    Optional<ChangeRequest> findByIdempotencyKey(String idempotencyKey);

    Optional<ChangeRequest> findByCorrelationId(String correlationId);

    /** In-progress-change guard: is there already a non-terminal change request for this booking? */
    List<ChangeRequest> findByBooking_IdAndStatusNotIn(Long bookingId, List<ChangeRequestStatus> terminalStatuses);

    /**
     * Ops transaction-detail page needs the change request plus all supplier reservations in one
     * round trip - fetched explicitly to avoid N+1 (see README: SQL/Hibernate/JDBC Decisions).
     */
    @EntityGraph(attributePaths = {"supplierReservations", "booking"})
    @Query("select c from ChangeRequest c where c.id = :id")
    Optional<ChangeRequest> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"booking", "booking.member"})
    @Query("select c from ChangeRequest c order by c.createdAt desc")
    List<ChangeRequest> findAllWithBookingOrderByCreatedAtDesc();
}
