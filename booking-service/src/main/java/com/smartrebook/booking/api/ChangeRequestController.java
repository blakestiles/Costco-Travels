package com.smartrebook.booking.api;

import com.smartrebook.booking.dto.BookingEventDto;
import com.smartrebook.booking.dto.ChangeRequestResponse;
import com.smartrebook.booking.dto.IncidentResponse;
import com.smartrebook.booking.orchestration.ReconciliationService;
import com.smartrebook.booking.repository.BookingEventRepository;
import com.smartrebook.booking.service.IncidentService;
import com.smartrebook.booking.service.OpsQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/change-requests")
public class ChangeRequestController {

    private final OpsQueryService opsQueryService;
    private final BookingEventRepository bookingEventRepository;
    private final ReconciliationService reconciliationService;
    private final IncidentService incidentService;

    public ChangeRequestController(OpsQueryService opsQueryService, BookingEventRepository bookingEventRepository,
                                    ReconciliationService reconciliationService, IncidentService incidentService) {
        this.opsQueryService = opsQueryService;
        this.bookingEventRepository = bookingEventRepository;
        this.reconciliationService = reconciliationService;
        this.incidentService = incidentService;
    }

    @GetMapping("/{id}")
    public ChangeRequestResponse getChangeRequest(@PathVariable Long id) {
        return opsQueryService.getByChangeRequestId(id);
    }

    @GetMapping("/{id}/events")
    public List<BookingEventDto> getEvents(@PathVariable Long id) {
        return bookingEventRepository.findByChangeRequestIdOrderByCreatedAtAsc(id).stream()
                .map(e -> new BookingEventDto(e.getEventType(), e.getSupplier(), e.getStatus(), e.getMessage(),
                        e.getLatencyMs(), e.getCreatedAt()))
                .toList();
    }

    @PostMapping("/{id}/reconcile")
    public ChangeRequestResponse reconcile(@PathVariable Long id) {
        return reconciliationService.reconcile(id);
    }

    @PostMapping("/{id}/incident")
    public IncidentResponse createIncident(@PathVariable Long id) {
        return incidentService.createIncidentForChangeRequest(id);
    }
}
