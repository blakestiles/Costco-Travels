package com.smartrebook.booking.config;

import org.springframework.stereotype.Component;

/**
 * Authentication is explicitly out of scope for this prototype - every request resolves to this
 * one pre-seeded mock member. See README: Security.
 */
@Component
public class DemoMemberContext {
    public static final String MEMBER_REFERENCE = "DEMO-EXEC-001";

    public String currentMemberReference() {
        return MEMBER_REFERENCE;
    }
}
