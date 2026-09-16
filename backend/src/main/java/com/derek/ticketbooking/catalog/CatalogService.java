package com.derek.ticketbooking.catalog;

import java.util.Locale;

import com.derek.ticketbooking.api.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CatalogService {

	private final CatalogRepository repository;

	public CatalogService(CatalogRepository repository) {
		this.repository = repository;
	}

	public Venue getVenue(long id) {
		return repository.findVenue(id).orElseThrow(() -> new CatalogNotFoundException("Venue", id));
	}

	public Event getEvent(long id) {
		return repository.findEvent(id).orElseThrow(() -> new CatalogNotFoundException("Event", id));
	}

	public Performance getPerformance(long id) {
		return repository.findPerformance(id).orElseThrow(() -> new CatalogNotFoundException("Performance", id));
	}

	public PageResponse<Venue> listVenues(int page, int size) {
		return new PageResponse<>(repository.listVenues(size, (long) page * size), page, size,
				repository.countVenues());
	}

	public PageResponse<Event> listEvents(int page, int size) {
		return new PageResponse<>(repository.listEvents(size, (long) page * size), page, size,
				repository.countEvents());
	}

	public PageResponse<Seat> listSeats(long venueId, int page, int size) {
		getVenue(venueId);
		return new PageResponse<>(repository.listSeats(venueId, size, (long) page * size), page, size,
				repository.countSeats(venueId));
	}

	public PageResponse<Performance> listPerformances(long eventId, int page, int size) {
		getEvent(eventId);
		return new PageResponse<>(repository.listPerformances(eventId, size, (long) page * size), page, size,
				repository.countPerformances(eventId));
	}

	@Transactional
	public Venue createVenue(CatalogRequests.CreateVenue request) {
		return repository.createVenue(request);
	}

	@Transactional
	public Seat createSeat(long venueId, CatalogRequests.CreateSeat request) {
		getVenue(venueId);
		String rowLabel = request.rowLabel().strip().toUpperCase(Locale.ROOT);
		if (rowLabel.length() > 20) {
			throw new InvalidCatalogRequestException("The normalized row label must not exceed 20 characters.");
		}
		return repository.createSeat(venueId, rowLabel, request.seatNumber());
	}

	@Transactional
	public Event createEvent(CatalogRequests.CreateEvent request) {
		return repository.createEvent(request);
	}

	@Transactional
	public Performance createPerformance(long eventId, CatalogRequests.CreatePerformance request) {
		if (!request.endsAt().isAfter(request.startsAt())) {
			throw new InvalidCatalogRequestException("The performance must end after it starts.");
		}
		getEvent(eventId);
		getVenue(request.venueId());
		return repository.createPerformance(eventId, request);
	}

}
