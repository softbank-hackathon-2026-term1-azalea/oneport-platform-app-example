CREATE TABLE notes (
    id         BIGINT GENERATED ALWAYS AS IDENTITY,
    title      VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_notes PRIMARY KEY (id),
    CONSTRAINT ck_notes_title_not_blank CHECK (btrim(title) <> '')
);

CREATE INDEX ix_notes_created_at ON notes (created_at);
