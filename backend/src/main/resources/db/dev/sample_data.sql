-- Optional local fixtures; this file is not a Flyway migration.
-- From the repository root, with the default development database running:
-- docker compose exec -T postgres psql -U ticket_booking -d ticket_booking < backend/src/main/resources/db/dev/sample_data.sql
DO $$
DECLARE
    sample_venue_id BIGINT;
    sample_event_id BIGINT;
BEGIN
    SELECT id INTO sample_venue_id FROM venues
    WHERE name = 'Riverside Hall' AND address = '12 River Road' AND city = 'Dublin'
    ORDER BY id LIMIT 1;

    IF sample_venue_id IS NULL THEN
        INSERT INTO venues (name, address, city)
        VALUES ('Riverside Hall', '12 River Road', 'Dublin')
        RETURNING id INTO sample_venue_id;
    END IF;

    INSERT INTO seats (venue_id, row_label, seat_number)
    SELECT sample_venue_id, row_label, seat_number
    FROM (VALUES ('A'), ('B')) AS rows(row_label)
    CROSS JOIN generate_series(1, 10) AS numbers(seat_number)
    ON CONFLICT (venue_id, row_label, seat_number) DO NOTHING;

    SELECT id INTO sample_event_id FROM events
    WHERE title = 'Acoustic Evening' AND description = 'A small live music performance.'
    ORDER BY id LIMIT 1;

    IF sample_event_id IS NULL THEN
        INSERT INTO events (title, description)
        VALUES ('Acoustic Evening', 'A small live music performance.')
        RETURNING id INTO sample_event_id;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM performances WHERE event_id = sample_event_id AND venue_id = sample_venue_id) THEN
        INSERT INTO performances (event_id, venue_id, starts_at, ends_at)
        VALUES (sample_event_id, sample_venue_id,
                CURRENT_TIMESTAMP + INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '30 days 2 hours');
    END IF;
END;
$$;
