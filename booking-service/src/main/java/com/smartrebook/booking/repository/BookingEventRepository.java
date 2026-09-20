package com.smartrebook.booking.repository;

import com.smartrebook.booking.domain.BookingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingEventRepository extends JpaRepository<BookingEvent, Long> {

    List<BookingEvent> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    List<BookingEvent> findByChangeRequestIdOrderByCreatedAtAsc(Long changeRequestId);
}
