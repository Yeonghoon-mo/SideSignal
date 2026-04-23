CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_not_blank CHECK (length(btrim(email)) > 0),
    CONSTRAINT ck_users_password_hash_not_blank CHECK (length(btrim(password_hash)) > 0),
    CONSTRAINT ck_users_display_name_not_blank CHECK (length(btrim(display_name)) > 0)
);
