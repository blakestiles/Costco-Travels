package com.smartrebook.car.controller;

import com.smartrebook.car.repository.CarReservationStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cars/demo")
public class DemoController {

    private final CarReservationStore store;

    public DemoController(CarReservationStore store) {
        this.store = store;
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        store.clear();
        return ResponseEntity.noContent().build();
    }
}
