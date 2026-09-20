package com.smartrebook.booking.repository;

import com.smartrebook.booking.domain.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByConfirmationNumber(String confirmationNumber);

    List<Booking> findByMember_MemberReferenceOrderByCreatedAtDesc(String memberReference);

    /**
     * The booking-detail and change-availability screens always need the line items too.
     * Fetched explicitly via @EntityGraph in one round trip rather than left EAGER globally
     * (which would slow down every plain booking lookup) or lazy-loaded implicitly (which would
     * N+1 the moment a caller iterates items). See README: SQL/Hibernate/JDBC Decisions.
     */
    @EntityGraph(attributePaths = {"items", "member"})
    @Query("select b from Booking b where b.confirmationNumber = :confirmationNumber")
    Optional<Booking> findWithItemsByConfirmationNumber(@Param("confirmationNumber") String confirmationNumber);
}
