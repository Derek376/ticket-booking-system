package com.derek.ticketbooking.catalog;

public record Seat(long id, long venueId, String rowLabel, int seatNumber) {
}
