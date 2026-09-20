package com.smartrebook.booking.service;

import com.smartrebook.booking.client.IncidentClient;
import com.smartrebook.booking.domain.ChangeRequest;
import com.smartrebook.booking.domain.Incident;
import com.smartrebook.booking.domain.IncidentStatus;
import com.smartrebook.booking.dto.IncidentResponse;
import com.smartrebook.booking.exception.InvalidBookingStateException;
import com.smartrebook.booking.repository.ChangeRequestRepository;
import com.smartrebook.booking.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    private final IncidentClient incidentClient;
    private final IncidentRepository incidentRepository;
    private final ChangeRequestRepository changeRequestRepository;

    public IncidentService(IncidentClient incidentClient, IncidentRepository incidentRepository,
                            ChangeRequestRepository changeRequestRepository) {
        this.incidentClient = incidentClient;
        this.incidentRepository = incidentRepository;
        this.changeRequestRepository = changeRequestRepository;
    }

    @Transactional
    public IncidentResponse createIncidentForChangeRequest(Long changeRequestId) {
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new InvalidBookingStateException("Change request not found: " + changeRequestId));

        String title = "Reservation change reconciliation - " + changeRequest.getCorrelationId();
        String description = "Change request " + changeRequestId + " (correlation " + changeRequest.getCorrelationId()
                + ") requires operational follow-up. Status: " + changeRequest.getStatus()
                + ", reconciliation: " + changeRequest.getReconciliationStatus();

        String externalReference = incidentClient.createIncident(changeRequest.getCorrelationId(), title, description);

        Incident incident = new Incident(changeRequest.getCorrelationId(), externalReference, title, description,
                IncidentStatus.OPEN);
        incidentRepository.save(incident);

        return new IncidentResponse(incident.getExternalReference(), incident.getTitle(), incident.getStatus().name());
    }
}
