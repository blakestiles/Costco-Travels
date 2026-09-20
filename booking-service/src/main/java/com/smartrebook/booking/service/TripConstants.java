package com.smartrebook.booking.service;

/**
 * The core demo scenario fixes destination, room type, and car class - only dates change
 * (see plan §3/§8: "Keep destination and travelers fixed for the core demo"). Real supplier
 * contracts would carry these per-booking; hardcoding them here is a deliberate prototype
 * simplification since there is only one seeded itinerary.
 */
public final class TripConstants {

    public static final String ROOM_TYPE = "Ocean View King";
    public static final String HOTEL_NAME = "Wailea Beach Resort";
    public static final String CAR_CLASS = "Standard SUV";

    private TripConstants() {
    }
}
