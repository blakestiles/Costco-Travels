package com.smartrebook.booking.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Optional: only activates when SPLUNK_HEC_URL and SPLUNK_HEC_TOKEN are set. Otherwise this
 * appender is inert and the application runs normally on console/file JSON logs. No paid Splunk
 * account is required to run the demo. See README: Observability.
 */
public class SplunkHecAppender extends AppenderBase<ILoggingEvent> {

    private String hecUrl;
    private String hecToken;
    private HttpClient httpClient;

    @Override
    public void start() {
        hecUrl = System.getenv("SPLUNK_HEC_URL");
        hecToken = System.getenv("SPLUNK_HEC_TOKEN");
        if (hecUrl == null || hecUrl.isBlank() || hecToken == null || hecToken.isBlank()) {
            // Deliberately not started: no config present, appender is a no-op.
            return;
        }
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (httpClient == null) {
            return;
        }
        String payload = "{\"event\":" + jsonEscape(event.getFormattedMessage()) + "}";
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(hecUrl))
                .header("Authorization", "Splunk " + hecToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding());
    }

    private String jsonEscape(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
