package com.smartrebook.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BookingServiceApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
        printStartupBanner();
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(BookingServiceApplication.class);
    }

    private static void printStartupBanner() {
        System.out.println("""

                ================================================================
                 Costco Travel Smart Rebook Demo
                ================================================================
                 Member Portal:   http://localhost:8080
                 Operations:      http://localhost:8080/ops
                 Demo booking:    CT-DEMO-78291
                ================================================================
                """);
    }
}
