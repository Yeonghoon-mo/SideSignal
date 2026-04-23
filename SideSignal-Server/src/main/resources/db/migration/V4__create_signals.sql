CREATE TABLE signals (
    id UUID PRIMARY KEY,
    pair_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
    departure_time TIMESTAMPTZ,
    message VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_signals_pair_user UNIQUE (pair_id, user_id),
    CONSTRAINT fk_signals_pair FOREIGN KEY (pair_id) REFERENCES pairs (id),
    CONSTRAINT fk_signals_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_signals_status CHECK (
        status IN (
            'FOCUSING',
            'IN_MEETING',
            'COFFEE_AVAILABLE',
            'LUNCH_AVAILABLE',
            'LEAVING_SOON',
            'OFFLINE'
        )
    ),
    CONSTRAINT ck_signals_message_not_blank CHECK (
        message IS NULL OR length(btrim(message)) > 0
    )
);

CREATE INDEX ix_signals_user_id
    ON signals (user_id);
