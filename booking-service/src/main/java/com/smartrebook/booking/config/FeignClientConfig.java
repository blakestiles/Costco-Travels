package com.smartrebook.booking.config;

import com.smartrebook.booking.demo.DemoScenarioState;
import feign.Request;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignClientConfig {

    /**
     * Set explicitly rather than relying on feign.client.config.default.* YAML properties,
     * which were observed not to be honored by the client actually in use - this Options bean is
     * authoritative. The 3s read timeout is deliberately shorter than the CAR_TIMEOUT/CAR_AMBIGUOUS
     * supplier sleep (8s) so the client genuinely times out before the supplier responds.
     */
    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(2000, TimeUnit.MILLISECONDS, 3000, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public RequestInterceptor demoScenarioHeaderInterceptor(DemoScenarioState demoScenarioState) {
        return requestTemplate ->
                requestTemplate.header("X-Demo-Scenario", demoScenarioState.get().name());
    }

    @Bean
    public RequestInterceptor correlationIdPropagationInterceptor() {
        return requestTemplate -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId != null) {
                requestTemplate.header(CorrelationIdFilter.HEADER, correlationId);
            }
        };
    }
}
