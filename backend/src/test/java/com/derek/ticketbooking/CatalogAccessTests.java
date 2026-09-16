package com.derek.ticketbooking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
class CatalogAccessTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	void catalogReadsAreAvailableButWritesAreDisabledByDefault() throws Exception {
		try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
			for (String path : new String[] { "/api/venues", "/api/events" }) {
				var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
						.timeout(Duration.ofSeconds(10)).GET().build();
				assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
			}
			for (String path : new String[] { "/api/admin/venues", "/api/admin/events",
					"/api/admin/venues/1/seats", "/api/admin/events/1/performances" }) {
				var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
						.timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString("{}")).build();
				var response = client.send(request, HttpResponse.BodyHandlers.ofString());
				assertThat(response.statusCode()).as(path + ": " + response.body()).isEqualTo(404);
			}
		}
	}

}
