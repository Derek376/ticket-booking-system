package com.derek.ticketbooking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
class TicketBookingApplicationTests {

	private final BasicJsonTester json = new BasicJsonTester(getClass());

	@Value("${local.server.port}")
	private int port;

	@Test
	void applicationReportsHealthyOverHttp() throws Exception {
		try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("http://localhost:" + port + "/actuator/health"))
					.timeout(Duration.ofSeconds(5))
					.GET()
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(json.from(response.body())).extractingJsonPathStringValue("$.status").isEqualTo("UP");
		}
	}

}
