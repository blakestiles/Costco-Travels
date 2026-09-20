package com.smartrebook.booking.demo;

import com.smartrebook.contracts.DemoScenario;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-wide current demo scenario, set via POST /api/demo/scenario. Read by
 * DemoScenarioFeignInterceptor and stamped onto every outgoing supplier call as
 * X-Demo-Scenario, so the mock suppliers genuinely change behavior - not a frontend illusion.
 */
@Component
public class DemoScenarioState {

    private final AtomicReference<DemoScenario> current = new AtomicReference<>(DemoScenario.NORMAL);

    public DemoScenario get() {
        return current.get();
    }

    public void set(DemoScenario scenario) {
        current.set(scenario);
    }
}
