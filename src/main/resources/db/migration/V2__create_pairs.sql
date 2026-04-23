CREATE TABLE pairs (
    id UUID PRIMARY KEY,
    first_user_id UUID NOT NULL,
    second_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pairs_first_user FOREIGN KEY (first_user_id) REFERENCES users (id),
    CONSTRAINT fk_pairs_second_user FOREIGN KEY (second_user_id) REFERENCES users (id),
    CONSTRAINT ck_pairs_different_users CHECK (first_user_id <> second_user_id)
);

CREATE UNIQUE INDEX ux_pairs_member_set
    ON pairs (least(first_user_id, second_user_id), greatest(first_user_id, second_user_id));

CREATE INDEX ix_pairs_first_user_id
    ON pairs (first_user_id);

CREATE INDEX ix_pairs_second_user_id
    ON pairs (second_user_id);
