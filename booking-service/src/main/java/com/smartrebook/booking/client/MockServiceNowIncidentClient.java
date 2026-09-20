package com.smartrebook.booking.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Active whenever SERVICENOW_BASE_URL is not configured - i.e. always, in this prototype.
 * Intentionally mocked: this prototype has no access to a real ServiceNow
 * environment. The IncidentClient interface isolates the provider so a real REST implementation
 * could replace this mock without touching IncidentService or the ops UI.
 */
@Component
public class MockServiceNowIncidentClient implements IncidentClient {

    private final AtomicInteger sequence = new AtomicInteger(1000);

    @Value("${smartrebook.servicenow.base-url:}")
    private String serviceNowBaseUrl;

    @Override
    public String createIncident(String correlationId, String title, String description) {
        return "INC-DEMO-" + sequence.incrementAndGet();
    }
}
