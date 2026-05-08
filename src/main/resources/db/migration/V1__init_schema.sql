CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
CREATE TYPE magazine_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid  VARCHAR(128) UNIQUE NOT NULL,
    email         VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100),
    role          user_role    NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id          SERIAL       PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,
    description TEXT
);

CREATE TABLE magazines (
    id           UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255)    NOT NULL,
    publisher    VARCHAR(255),
    year_founded INT,
    category_id  INT             REFERENCES categories(id),
    description  TEXT,
    cover_path   VARCHAR(500),
    uploaded_by  UUID            REFERENCES users(id),
    status       magazine_status NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE issues (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    magazine_id      UUID         NOT NULL REFERENCES magazines(id) ON DELETE CASCADE,
    issue_number     VARCHAR(50)  NOT NULL,
    publication_date DATE,
    pdf_path         VARCHAR(500) NOT NULL,
    pages_count      INT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE reviews (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    magazine_id UUID      NOT NULL REFERENCES magazines(id) ON DELETE CASCADE,
    user_id     UUID      NOT NULL REFERENCES users(id),
    rating      INT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (magazine_id, user_id)
);

CREATE TABLE favorites (
    user_id     UUID      NOT NULL REFERENCES users(id),
    magazine_id UUID      NOT NULL REFERENCES magazines(id) ON DELETE CASCADE,
    added_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, magazine_id)
);

CREATE INDEX idx_magazines_status      ON magazines(status);
CREATE INDEX idx_magazines_category_id ON magazines(category_id);
CREATE INDEX idx_reviews_magazine_id   ON reviews(magazine_id);
CREATE INDEX idx_favorites_user_id     ON favorites(user_id);
