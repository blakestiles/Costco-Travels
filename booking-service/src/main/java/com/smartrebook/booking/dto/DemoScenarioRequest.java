package com.smartrebook.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record DemoScenarioRequest(@NotBlank String scenario) {
}
