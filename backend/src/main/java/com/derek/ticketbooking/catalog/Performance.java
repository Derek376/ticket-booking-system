package com.derek.ticketbooking.catalog;

public record Performance(long id, long eventId, long venueId, java.time.OffsetDateTime startsAt, java.time.OffsetDateTime endsAt) {
}
