package com.derek.ticketbooking.catalog;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class CatalogRequests {

	private CatalogRequests() {
	}

	public record CreateVenue(
			@NotBlank @Size(max = 200) String name,
			@NotBlank @Size(max = 300) String address,
			@NotBlank @Size(max = 120) String city) {
	}

	public record CreateSeat(
			@NotBlank @Size(max = 20) String rowLabel,
			@NotNull @Min(1) @Max(10000) Integer seatNumber) {
	}

	public record CreateEvent(
			@NotBlank @Size(max = 200) String title,
			@NotNull @Size(max = 5000) String description) {
	}

	public record CreatePerformance(
			@NotNull @Positive Long venueId,
			@NotNull @Future OffsetDateTime startsAt,
			@NotNull OffsetDateTime endsAt) {
	}

}
