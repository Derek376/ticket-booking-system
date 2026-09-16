package com.derek.ticketbooking.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {

	private final JdbcClient jdbc;

	public CatalogRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<Venue> findVenue(long id) {
		return jdbc.sql("SELECT id, name, address, city FROM venues WHERE id = :id")
				.param("id", id).query(Venue.class).optional();
	}

	public List<Venue> listVenues(int size, long offset) {
		return jdbc.sql("SELECT id, name, address, city FROM venues ORDER BY id LIMIT :size OFFSET :offset")
				.param("size", size).param("offset", offset).query(Venue.class).list();
	}

	public long countVenues() {
		return jdbc.sql("SELECT count(*) FROM venues").query(Long.class).single();
	}

	public Venue createVenue(CatalogRequests.CreateVenue request) {
		return jdbc.sql("""
				INSERT INTO venues (name, address, city) VALUES (:name, :address, :city)
				RETURNING id, name, address, city
				""").param("name", request.name().strip()).param("address", request.address().strip())
				.param("city", request.city().strip()).query(Venue.class).single();
	}

	public List<Seat> listSeats(long venueId, int size, long offset) {
		return jdbc.sql("""
				SELECT id, venue_id, row_label, seat_number FROM seats WHERE venue_id = :venueId
				ORDER BY row_label, seat_number, id LIMIT :size OFFSET :offset
				""").param("venueId", venueId).param("size", size).param("offset", offset)
				.query(Seat.class).list();
	}

	public long countSeats(long venueId) {
		return jdbc.sql("SELECT count(*) FROM seats WHERE venue_id = :venueId")
				.param("venueId", venueId).query(Long.class).single();
	}

	public Seat createSeat(long venueId, String rowLabel, int seatNumber) {
		return jdbc.sql("""
				INSERT INTO seats (venue_id, row_label, seat_number) VALUES (:venueId, :rowLabel, :seatNumber)
				RETURNING id, venue_id, row_label, seat_number
				""").param("venueId", venueId).param("rowLabel", rowLabel).param("seatNumber", seatNumber)
				.query(Seat.class).single();
	}

	public Optional<Event> findEvent(long id) {
		return jdbc.sql("SELECT id, title, description FROM events WHERE id = :id")
				.param("id", id).query(Event.class).optional();
	}

	public List<Event> listEvents(int size, long offset) {
		return jdbc.sql("SELECT id, title, description FROM events ORDER BY id LIMIT :size OFFSET :offset")
				.param("size", size).param("offset", offset).query(Event.class).list();
	}

	public long countEvents() {
		return jdbc.sql("SELECT count(*) FROM events").query(Long.class).single();
	}

	public Event createEvent(CatalogRequests.CreateEvent request) {
		return jdbc.sql("""
				INSERT INTO events (title, description) VALUES (:title, :description)
				RETURNING id, title, description
				""").param("title", request.title().strip()).param("description", request.description().strip())
				.query(Event.class).single();
	}

	public Optional<Performance> findPerformance(long id) {
		return jdbc.sql("SELECT id, event_id, venue_id, starts_at, ends_at FROM performances WHERE id = :id")
				.param("id", id).query(Performance.class).optional();
	}

	public List<Performance> listPerformances(long eventId, int size, long offset) {
		return jdbc.sql("""
				SELECT id, event_id, venue_id, starts_at, ends_at FROM performances WHERE event_id = :eventId
				ORDER BY starts_at, id LIMIT :size OFFSET :offset
				""").param("eventId", eventId).param("size", size).param("offset", offset)
				.query(Performance.class).list();
	}

	public long countPerformances(long eventId) {
		return jdbc.sql("SELECT count(*) FROM performances WHERE event_id = :eventId")
				.param("eventId", eventId).query(Long.class).single();
	}

	public Performance createPerformance(long eventId, CatalogRequests.CreatePerformance request) {
		return jdbc.sql("""
				INSERT INTO performances (event_id, venue_id, starts_at, ends_at)
				VALUES (:eventId, :venueId, :startsAt, :endsAt)
				RETURNING id, event_id, venue_id, starts_at, ends_at
				""").param("eventId", eventId).param("venueId", request.venueId())
				.param("startsAt", request.startsAt()).param("endsAt", request.endsAt())
				.query(Performance.class).single();
	}

}
