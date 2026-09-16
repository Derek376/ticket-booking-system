package com.derek.ticketbooking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
class CatalogApiTests {

	private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final BasicJsonTester json = new BasicJsonTester(getClass());
	private final JsonMapper mapper = JsonMapper.builder().build();

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void clearCatalog() {
		jdbc.update("DELETE FROM performances");
		jdbc.update("DELETE FROM seats");
		jdbc.update("DELETE FROM events");
		jdbc.update("DELETE FROM venues");
	}

	@AfterAll
	static void closeClient() {
		client.close();
	}

	@Test
	void createsAndReadsTheEntireCatalog() throws Exception {
		var venue = post("/api/admin/venues", Map.of("name", " Riverside Hall ", "address", "12 River Road", "city", "Dublin"));
		assertThat(venue.statusCode()).isEqualTo(201);
		long venueId = id(venue);
		assertThat(venue.headers().firstValue("Location")).contains("/api/venues/" + venueId);
		assertThat(json.from(get("/api/venues/" + venueId).body())).extractingJsonPathStringValue("$.name")
				.isEqualTo("Riverside Hall");

		var seat = post("/api/admin/venues/" + venueId + "/seats", Map.of("rowLabel", " a ", "seatNumber", 1));
		assertThat(seat.statusCode()).isEqualTo(201);
		assertThat(json.from(get("/api/venues/" + venueId + "/seats").body()))
				.extractingJsonPathStringValue("$.items[0].rowLabel").isEqualTo("A");

		long eventId = createEvent("Acoustic Evening");
		assertThat(json.from(get("/api/events/" + eventId).body())).extractingJsonPathStringValue("$.title")
				.isEqualTo("Acoustic Evening");
		OffsetDateTime startsAt = OffsetDateTime.now().plusDays(30).withNano(0);
		var performance = post("/api/admin/events/" + eventId + "/performances", performance(venueId, startsAt));
		assertThat(performance.statusCode()).isEqualTo(201);
		assertThat(performance.headers().firstValue("Location")).contains("/api/performances/" + id(performance));
		var details = get("/api/performances/" + id(performance));
		assertThat(details.statusCode()).isEqualTo(200);
		assertThat(number(details, "$.venueId")).isEqualTo(venueId);
		assertThat(number(details, "$.eventId")).isEqualTo(eventId);
		String returnedStart = JsonPath.read(details.body(), "$.startsAt");
		assertThat(OffsetDateTime.parse(returnedStart).toInstant()).isEqualTo(startsAt.toInstant());
		assertThat(number(get("/api/events/" + eventId + "/performances"), "$.totalItems")).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM performances", Long.class)).isEqualTo(1);
	}

	@Test
	void paginatesVenuesAndEventsInStableOrder() throws Exception {
		long firstVenue = createVenue("First Hall");
		createVenue("Second Hall");
		long lastVenue = createVenue("Third Hall");
		long firstEvent = createEvent("First Event");
		createEvent("Second Event");
		long lastEvent = createEvent("Third Event");

		for (var resource : Map.of("venues", new long[] { firstVenue, lastVenue },
				"events", new long[] { firstEvent, lastEvent }).entrySet()) {
			var first = get("/api/" + resource.getKey() + "?size=2");
			assertThat(number(first, "$.totalItems")).isEqualTo(3);
			assertThat(number(first, "$.items.length()")).isEqualTo(2);
			assertThat(number(first, "$.items[0].id")).isEqualTo(resource.getValue()[0]);
			var second = get("/api/" + resource.getKey() + "?page=1&size=2");
			assertThat(number(second, "$.page")).isEqualTo(1);
			assertThat(number(second, "$.items.length()")).isEqualTo(1);
			assertThat(number(second, "$.items[0].id")).isEqualTo(resource.getValue()[1]);
			assertThat(number(get("/api/" + resource.getKey() + "?page=2147483647&size=100"), "$.items.length()"))
					.isZero();
		}
	}

	@Test
	void seatPagesAreScopedToTheVenueAndSortedByPosition() throws Exception {
		long venueId = createVenue("Main Hall");
		long otherId = createVenue("Other Hall");
		post("/api/admin/venues/" + otherId + "/seats", Map.of("rowLabel", "A", "seatNumber", 1));
		for (int number : new int[] { 3, 1, 2 }) {
			assertThat(post("/api/admin/venues/" + venueId + "/seats",
					Map.of("rowLabel", "A", "seatNumber", number)).statusCode()).isEqualTo(201);
		}
		var response = get("/api/venues/" + venueId + "/seats?page=1&size=2");
		assertThat(number(response, "$.totalItems")).isEqualTo(3);
		assertThat(number(response, "$.items[0].seatNumber")).isEqualTo(3);
	}

	@Test
	void performancePagesAreScopedToTheEventAndSortedByTime() throws Exception {
		long venueId = createVenue("Main Hall");
		long eventId = createEvent("Main Event");
		long otherId = createEvent("Other Event");
		OffsetDateTime first = OffsetDateTime.now().plusDays(10).withNano(0);
		post("/api/admin/events/" + otherId + "/performances", performance(venueId, first));
		for (int day : new int[] { 3, 1, 2 }) {
			assertThat(post("/api/admin/events/" + eventId + "/performances", performance(venueId, first.plusDays(day)))
					.statusCode()).isEqualTo(201);
		}
		var response = get("/api/events/" + eventId + "/performances?page=1&size=2");
		assertThat(number(response, "$.totalItems")).isEqualTo(3);
		String start = JsonPath.read(response.body(), "$.items[0].startsAt");
		assertThat(OffsetDateTime.parse(start).toInstant()).isEqualTo(first.plusDays(3).toInstant());
	}

	@ParameterizedTest
	@ValueSource(strings = { "{", "{}", "null", "{\"name\":\" \"}",
			"{\"name\":\"Hall\",\"address\":\"Road\",\"city\":\"\"}" })
	void invalidVenueBodiesReturnBadRequest(String body) throws Exception {
		assertProblem(send("POST", "/api/admin/venues", body), 400);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM venues", Long.class)).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/events?page=-1", "/api/events?size=0", "/api/venues?size=101",
			"/api/events?page=text", "/api/venues/0", "/api/events/not-a-number" })
	void invalidQueryParametersReturnBadRequest(String path) throws Exception {
		assertProblem(get(path), 400);
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/venues/9223372036854775807", "/api/events/9223372036854775807",
			"/api/performances/9223372036854775807", "/api/venues/9223372036854775807/seats",
			"/api/events/9223372036854775807/performances", "/api/unknown" })
	void missingResourcesReturnNotFound(String path) throws Exception {
		assertProblem(get(path), 404);
	}

	@Test
	void missingParentsDoNotCreateOrphanRecords() throws Exception {
		long venueId = createVenue("Hall");
		long eventId = createEvent("Event");
		var start = OffsetDateTime.now().plusDays(10);
		assertProblem(post("/api/admin/venues/9223372036854775807/seats", Map.of("rowLabel", "A", "seatNumber", 1)), 404);
		assertProblem(post("/api/admin/events/9223372036854775807/performances", performance(venueId, start)), 404);
		assertProblem(post("/api/admin/events/" + eventId + "/performances", performance(Long.MAX_VALUE, start)), 404);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM seats", Long.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM performances", Long.class)).isZero();
	}

	@Test
	void duplicateSeatsAndPerformancesReturnConflict() throws Exception {
		long venueId = createVenue("Hall");
		String seatsPath = "/api/admin/venues/" + venueId + "/seats";
		assertThat(post(seatsPath, Map.of("rowLabel", " a ", "seatNumber", 1)).statusCode()).isEqualTo(201);
		var duplicateSeat = post(seatsPath, Map.of("rowLabel", "A", "seatNumber", 1));
		assertProblem(duplicateSeat, 409);
		assertThat(duplicateSeat.body()).doesNotContain("INSERT", "seats_venue_position_unique");
		long eventId = createEvent("Event");
		var request = performance(venueId, OffsetDateTime.now().plusDays(10).withNano(0));
		String performancesPath = "/api/admin/events/" + eventId + "/performances";
		assertThat(post(performancesPath, request).statusCode()).isEqualTo(201);
		assertProblem(post(performancesPath, request), 409);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM seats", Long.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM performances", Long.class)).isEqualTo(1);
	}

	@Test
	void rejectsInvalidSeatsEventsAndSchedules() throws Exception {
		long venueId = createVenue("Hall");
		long eventId = createEvent("Event");
		String path = "/api/admin/events/" + eventId + "/performances";
		OffsetDateTime start = OffsetDateTime.now().plusDays(10);
		assertProblem(post(path, Map.of("venueId", venueId, "startsAt", start.toString(), "endsAt", start.toString())), 400);
		assertProblem(post(path, performance(venueId, OffsetDateTime.now().minusDays(1))), 400);
		assertProblem(post(path, Map.of("venueId", venueId, "startsAt", "tomorrow", "endsAt", "later")), 400);
		assertProblem(post(path, Map.of("startsAt", start.toString(), "endsAt", start.plusHours(2).toString())), 400);
		assertProblem(post("/api/admin/events", Map.of("title", " ", "description", "")), 400);
		assertProblem(post("/api/admin/events", Map.of("title", "x".repeat(201), "description", "")), 400);
		assertProblem(post("/api/admin/venues/" + venueId + "/seats", Map.of("rowLabel", "A", "seatNumber", 0)), 400);
		assertProblem(post("/api/admin/venues/" + venueId + "/seats", Map.of("rowLabel", " ", "seatNumber", 1)), 400);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM performances", Long.class)).isZero();
	}

	@Test
	void sqlMetacharactersAreStoredAsData() throws Exception {
		String title = "Evening'); DROP TABLE events; --";
		long eventId = createEvent(title);
		assertThat(json.from(get("/api/events/" + eventId).body())).extractingJsonPathStringValue("$.title")
				.isEqualTo(title);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM events", Long.class)).isEqualTo(1);
	}

	@Test
	void databaseEnforcesReferencesAndScheduleConstraints() throws Exception {
		long venueId = createVenue("Hall");
		long eventId = createEvent("Event");
		assertThatThrownBy(() -> jdbc.update("INSERT INTO seats (venue_id, row_label, seat_number) VALUES (?, 'A', 1)", Long.MAX_VALUE))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO performances (event_id, venue_id, starts_at, ends_at)
				VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 hour')
				""", eventId, venueId)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void sampleDataCanBeLoadedTwiceWithoutDuplicates() throws Exception {
		String script = new ClassPathResource("db/dev/sample_data.sql").getContentAsString(StandardCharsets.UTF_8);
		jdbc.execute(script);
		jdbc.execute(script);
		assertThat(number(get("/api/venues"), "$.totalItems")).isEqualTo(1);
		assertThat(number(get("/api/events"), "$.totalItems")).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM seats", Long.class)).isEqualTo(20);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM performances", Long.class)).isEqualTo(1);
	}

	private long createVenue(String name) throws Exception {
		var response = post("/api/admin/venues", Map.of("name", name, "address", "12 River Road", "city", "Dublin"));
		assertThat(response.statusCode()).isEqualTo(201);
		return id(response);
	}

	private long createEvent(String title) throws Exception {
		var response = post("/api/admin/events", Map.of("title", title, "description", "Live music"));
		assertThat(response.statusCode()).isEqualTo(201);
		return id(response);
	}

	private Map<String, Object> performance(long venueId, OffsetDateTime start) {
		return Map.of("venueId", venueId, "startsAt", start.toString(), "endsAt", start.plusHours(2).toString());
	}

	private HttpResponse<String> post(String path, Object body) throws Exception {
		return send("POST", path, mapper.writeValueAsString(body));
	}

	private HttpResponse<String> get(String path) throws Exception {
		return send("GET", path, "");
	}

	private HttpResponse<String> send(String method, String path, String body) throws Exception {
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
				.method(method, HttpRequest.BodyPublishers.ofString(body)).build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private void assertProblem(HttpResponse<String> response, int status) {
		assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
		assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
		assertThat(number(response, "$.status")).isEqualTo(status);
		assertThat(json.from(response.body())).hasJsonPathStringValue("$.title").hasJsonPathStringValue("$.detail");
	}

	private long id(HttpResponse<String> response) {
		return number(response, "$.id");
	}

	private long number(HttpResponse<String> response, String path) {
		return ((Number) JsonPath.read(response.body(), path)).longValue();
	}

}
