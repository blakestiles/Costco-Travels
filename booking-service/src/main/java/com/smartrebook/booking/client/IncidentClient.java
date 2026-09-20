package com.smartrebook.booking.client;

/**
 * External boundary for incident creation. MockServiceNowIncidentClient is the only
 * implementation active in this prototype (no access to a real Costco ServiceNow instance) - a
 * real REST adapter would implement this same interface and could be swapped in when
 * SERVICENOW_BASE_URL/credentials are configured. See README: ServiceNow.
 */
public interface IncidentClient {
    String createIncident(String correlationId, String title, String description);
}
