CREATE TABLE rooms (
    id              BIGSERIAL PRIMARY KEY,
    room_number     VARCHAR(20)     NOT NULL UNIQUE,
    type            VARCHAR(20)     NOT NULL,
    price_per_night NUMERIC(10, 2)  NOT NULL CHECK (price_per_night > 0),
    status          VARCHAR(20)     NOT NULL DEFAULT 'DISPONIBLE',
    capacity        INTEGER,
    floor           INTEGER
);
