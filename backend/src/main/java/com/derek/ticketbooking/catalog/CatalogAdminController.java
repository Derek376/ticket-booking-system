package com.derek.ticketbooking.catalog;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/api/admin")
public class CatalogAdminController {

	private final CatalogService service;

	public CatalogAdminController(CatalogService service) {
		this.service = service;
	}

	@PostMapping("/venues")
	public ResponseEntity<Venue> createVenue(@Valid @RequestBody CatalogRequests.CreateVenue request) {
		Venue venue = service.createVenue(request);
		return ResponseEntity.created(URI.create("/api/venues/" + venue.id())).body(venue);
	}

	@PostMapping("/venues/{venueId}/seats")
	public ResponseEntity<Seat> createSeat(@PathVariable @Positive long venueId,
			@Valid @RequestBody CatalogRequests.CreateSeat request) {
		Seat seat = service.createSeat(venueId, request);
		return ResponseEntity.status(201).body(seat);
	}

	@PostMapping("/events")
	public ResponseEntity<Event> createEvent(@Valid @RequestBody CatalogRequests.CreateEvent request) {
		Event event = service.createEvent(request);
		return ResponseEntity.created(URI.create("/api/events/" + event.id())).body(event);
	}

	@PostMapping("/events/{eventId}/performances")
	public ResponseEntity<Performance> createPerformance(@PathVariable @Positive long eventId,
			@Valid @RequestBody CatalogRequests.CreatePerformance request) {
		Performance performance = service.createPerformance(eventId, request);
		return ResponseEntity.created(URI.create("/api/performances/" + performance.id())).body(performance);
	}

}
