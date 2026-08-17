CREATE TABLE calendar (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(50) NOT NULL UNIQUE,
    name_th varchar(150) NOT NULL,
    name_en varchar(150),
    country_code char(2) NOT NULL DEFAULT 'TH',
    status varchar(30) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE holiday (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    holiday_code varchar(80) NOT NULL UNIQUE,
    substitute_for_id uuid REFERENCES holiday(id),
    record_status varchar(30) NOT NULL DEFAULT 'ACTIVE'
        CHECK (record_status IN ('ACTIVE', 'CANCELLED')),
    current_published_revision_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES app_user(id),
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid NOT NULL REFERENCES app_user(id),
    version bigint NOT NULL DEFAULT 0,
    CHECK (substitute_for_id IS NULL OR substitute_for_id <> id)
);

CREATE TABLE holiday_revision (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    holiday_id uuid NOT NULL REFERENCES holiday(id),
    revision_no integer NOT NULL CHECK (revision_no > 0),
    holiday_date date NOT NULL,
    name_th varchar(250) NOT NULL,
    name_en varchar(250),
    holiday_type varchar(30) NOT NULL
        CHECK (holiday_type IN ('REGULAR', 'SPECIAL', 'SUBSTITUTE')),
    source_reference_no varchar(150),
    source_url text,
    change_reason text,
    workflow_status varchar(40) NOT NULL DEFAULT 'DRAFT'
        CHECK (workflow_status IN (
            'DRAFT', 'PENDING_LEVEL_1', 'PENDING_LEVEL_2', 'APPROVED',
            'PUBLISHED', 'REJECTED', 'CANCEL_PENDING', 'CANCELLED'
        )),
    published_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES app_user(id),
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid NOT NULL REFERENCES app_user(id),
    version bigint NOT NULL DEFAULT 0,
    UNIQUE (holiday_id, revision_no),
    UNIQUE (holiday_id, id),
    CHECK (source_url IS NULL OR source_url ~* '^https://'),
    CHECK (revision_no = 1 OR change_reason IS NOT NULL)
);

ALTER TABLE holiday
    ADD CONSTRAINT fk_holiday_current_published_revision
    FOREIGN KEY (id, current_published_revision_id)
    REFERENCES holiday_revision(holiday_id, id);

CREATE UNIQUE INDEX uq_holiday_one_current_revision
    ON holiday (current_published_revision_id)
    WHERE current_published_revision_id IS NOT NULL;

CREATE INDEX ix_holiday_revision_date_status
    ON holiday_revision (holiday_date, workflow_status);
CREATE INDEX ix_holiday_revision_holiday_created
    ON holiday_revision (holiday_id, created_at DESC);
CREATE INDEX ix_holiday_revision_published
    ON holiday_revision (holiday_date, published_at)
    WHERE workflow_status = 'PUBLISHED';

CREATE TABLE holiday_revision_calendar (
    holiday_revision_id uuid NOT NULL REFERENCES holiday_revision(id) ON DELETE CASCADE,
    calendar_id uuid NOT NULL REFERENCES calendar(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (holiday_revision_id, calendar_id)
);

CREATE INDEX ix_holiday_revision_calendar_calendar
    ON holiday_revision_calendar (calendar_id, holiday_revision_id);

