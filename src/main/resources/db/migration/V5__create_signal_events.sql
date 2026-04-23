CREATE TABLE signal_events (
    id UUID PRIMARY KEY,
    pair_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_signal_events_pair FOREIGN KEY (pair_id) REFERENCES pairs (id),
    CONSTRAINT fk_signal_events_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT ck_signal_events_event_type CHECK (
        event_type IN (
            'SIGNAL_UPDATED',
            'DEPARTURE_TIME_CLEARED'
        )
    )
);

CREATE INDEX ix_signal_events_pair_created_at
    ON signal_events (pair_id, created_at);

CREATE INDEX ix_signal_events_sender_id
    ON signal_events (sender_id);
