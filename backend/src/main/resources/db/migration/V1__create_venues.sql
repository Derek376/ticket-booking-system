CREATE TABLE venues (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(300) NOT NULL,
    city VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT venues_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT venues_address_not_blank CHECK (length(trim(address)) > 0),
    CONSTRAINT venues_city_not_blank CHECK (length(trim(city)) > 0)
);
