CREATE TABLE pair_invites (
    id UUID PRIMARY KEY,
    code_hash VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL,
    accepted_by UUID,
    pair_id UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pair_invites_code_hash UNIQUE (code_hash),
    CONSTRAINT fk_pair_invites_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_pair_invites_accepted_by FOREIGN KEY (accepted_by) REFERENCES users (id),
    CONSTRAINT fk_pair_invites_pair FOREIGN KEY (pair_id) REFERENCES pairs (id),
    CONSTRAINT ck_pair_invites_code_hash_not_blank CHECK (length(btrim(code_hash)) > 0),
    CONSTRAINT ck_pair_invites_acceptance_state CHECK (
        (accepted_by IS NULL AND accepted_at IS NULL AND pair_id IS NULL)
        OR (accepted_by IS NOT NULL AND accepted_at IS NOT NULL AND pair_id IS NOT NULL)
    ),
    CONSTRAINT ck_pair_invites_different_users CHECK (
        accepted_by IS NULL OR accepted_by <> created_by
    )
);

CREATE INDEX ix_pair_invites_created_by
    ON pair_invites (created_by);

CREATE INDEX ix_pair_invites_expires_at
    ON pair_invites (expires_at);
