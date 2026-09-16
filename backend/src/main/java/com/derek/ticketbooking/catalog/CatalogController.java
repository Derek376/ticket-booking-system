package com.derek.ticketbooking.catalog;

import com.derek.ticketbooking.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

	private final CatalogService service;

	public CatalogController(CatalogService service) {
		this.service = service;
	}

	@GetMapping("/venues")
	public PageResponse<Venue> venues(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return service.listVenues(page, size);
	}

	@GetMapping("/venues/{id}")
	public Venue venue(@PathVariable @Positive long id) {
		return service.getVenue(id);
	}

	@GetMapping("/venues/{venueId}/seats")
	public PageResponse<Seat> seats(@PathVariable @Positive long venueId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return service.listSeats(venueId, page, size);
	}

	@GetMapping("/events")
	public PageResponse<Event> events(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return service.listEvents(page, size);
	}

	@GetMapping("/events/{id}")
	public Event event(@PathVariable @Positive long id) {
		return service.getEvent(id);
	}

	@GetMapping("/events/{eventId}/performances")
	public PageResponse<Performance> performances(@PathVariable @Positive long eventId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return service.listPerformances(eventId, page, size);
	}

	@GetMapping("/performances/{id}")
	public Performance performance(@PathVariable @Positive long id) {
		return service.getPerformance(id);
	}

}
