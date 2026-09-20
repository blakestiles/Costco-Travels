package com.smartrebook.booking.api;

import com.smartrebook.booking.demo.DemoResetService;
import com.smartrebook.booking.demo.DemoScenarioState;
import com.smartrebook.booking.dto.DemoScenarioRequest;
import com.smartrebook.contracts.DemoScenario;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoScenarioState demoScenarioState;
    private final DemoResetService demoResetService;

    public DemoController(DemoScenarioState demoScenarioState, DemoResetService demoResetService) {
        this.demoScenarioState = demoScenarioState;
        this.demoResetService = demoResetService;
    }

    @PostMapping("/scenario")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setScenario(@RequestBody DemoScenarioRequest request) {
        demoScenarioState.set(DemoScenario.valueOf(request.scenario().trim().toUpperCase()));
    }

    @GetMapping("/scenario")
    public DemoScenarioRequest getScenario() {
        return new DemoScenarioRequest(demoScenarioState.get().name());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        demoResetService.reset();
    }
}
