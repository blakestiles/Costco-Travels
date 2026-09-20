package com.smartrebook.booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    /** Every non-demo travel nav link routes here rather than a dead anchor or a broken page. */
    @GetMapping("/not-included")
    public String notIncluded() {
        return "not-included";
    }
}
