CREATE TABLE seats (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id BIGINT NOT NULL REFERENCES venues (id),
    row_label VARCHAR(20) NOT NULL CHECK (length(trim(row_label)) > 0),
    seat_number INTEGER NOT NULL CHECK (seat_number BETWEEN 1 AND 10000),
    CONSTRAINT seats_venue_position_unique UNIQUE (venue_id, row_label, seat_number)
);

CREATE TABLE events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
    description VARCHAR(5000) NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE performances (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events (id),
    venue_id BIGINT NOT NULL REFERENCES venues (id),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT performances_valid_time_range CHECK (ends_at > starts_at),
    CONSTRAINT performances_event_venue_start_unique UNIQUE (event_id, venue_id, starts_at)
);

CREATE INDEX performances_event_schedule_idx ON performances (event_id, starts_at, id);
CREATE INDEX performances_venue_schedule_idx ON performances (venue_id, starts_at);
