package com.derek.ticketbooking;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
class DatabaseIntegrationTests {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private Flyway flyway;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void migrationsRunOnStartupAndAreNotRepeated() {
		flyway.validate();
		assertThat(flyway.info().pending()).isEmpty();
		assertThat(flyway.migrate().migrationsExecuted).isZero();
		assertThat(jdbc.queryForObject("""
				SELECT count(*) FROM flyway_schema_history
				WHERE version = '1' AND success = true
				""", Integer.class)).isEqualTo(1);
	}

	@Test
	@Transactional
	void venuesCanBeSavedAndRead() {
		Long id = jdbc.queryForObject("""
				INSERT INTO venues (name, address, city) VALUES (?, ?, ?) RETURNING id
				""", Long.class, "Riverside Hall", "12 River Road", "Dublin");

		var venue = jdbc.queryForMap("SELECT * FROM venues WHERE id = ?", id);

		assertThat(id).isPositive();
		assertThat(venue).containsEntry("name", "Riverside Hall")
				.containsEntry("address", "12 River Road")
				.containsEntry("city", "Dublin");
		assertThat(venue.get("created_at")).isNotNull();
	}

	@Test
	@Transactional
	void databaseRejectsBlankVenueNames() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO venues (name, address, city) VALUES (?, ?, ?)
				""", "   ", "12 River Road", "Dublin"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("venues_name_not_blank");
	}

	@Test
	void rolledBackChangesAreNotSaved() {
		var transaction = new TransactionTemplate(transactionManager);
		Long id = transaction.execute(status -> {
			Long insertedId = jdbc.queryForObject("""
					INSERT INTO venues (name, address, city) VALUES (?, ?, ?) RETURNING id
					""", Long.class, "Temporary Hall", "1 Test Street", "Cork");
			status.setRollbackOnly();
			return insertedId;
		});

		assertThat(id).isNotNull();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM venues WHERE id = ?", Integer.class, id))
				.isZero();
	}

}
