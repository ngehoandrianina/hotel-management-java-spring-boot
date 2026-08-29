CREATE TABLE reservations (
    id        BIGSERIAL PRIMARY KEY,
    room_id   BIGINT       NOT NULL REFERENCES rooms(id),
    client_id BIGINT       NOT NULL REFERENCES clients(id),
    check_in  DATE         NOT NULL,
    check_out DATE         NOT NULL,
    status    VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMEE',
    CONSTRAINT chk_dates CHECK (check_out > check_in)
);

CREATE INDEX idx_reservations_room_id ON reservations(room_id);
CREATE INDEX idx_reservations_client_id ON reservations(client_id);
