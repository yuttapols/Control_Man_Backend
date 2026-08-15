CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE user_level (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(50) NOT NULL UNIQUE,
    name_th varchar(150) NOT NULL,
    rank_order integer NOT NULL CHECK (rank_order >= 0),
    status varchar(30) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE app_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username varchar(100) NOT NULL,
    email varchar(254) NOT NULL,
    display_name varchar(200) NOT NULL,
    password_hash varchar(255) NOT NULL,
    user_level_id uuid REFERENCES user_level(id),
    status varchar(30) NOT NULL DEFAULT 'INVITED'
        CHECK (status IN ('INVITED', 'ACTIVE', 'LOCKED', 'SUSPENDED', 'DISABLED')),
    failed_login_count integer NOT NULL DEFAULT 0 CHECK (failed_login_count >= 0),
    locked_until timestamptz,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid REFERENCES app_user(id),
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid REFERENCES app_user(id),
    version bigint NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_app_user_username_ci ON app_user (lower(username));
CREATE UNIQUE INDEX uq_app_user_email_ci ON app_user (lower(email));
CREATE INDEX ix_app_user_status ON app_user (status);
CREATE INDEX ix_app_user_user_level ON app_user (user_level_id);

CREATE TABLE role (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(80) NOT NULL UNIQUE,
    name_th varchar(150) NOT NULL,
    description text,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE permission (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(120) NOT NULL UNIQUE,
    module varchar(50) NOT NULL,
    description text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_role (
    user_id uuid NOT NULL REFERENCES app_user(id),
    role_id uuid NOT NULL REFERENCES role(id),
    valid_from timestamptz NOT NULL DEFAULT now(),
    valid_until timestamptz,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    assigned_by uuid REFERENCES app_user(id),
    PRIMARY KEY (user_id, role_id),
    CHECK (valid_until IS NULL OR valid_until > valid_from)
);

CREATE INDEX ix_user_role_role ON user_role (role_id);

CREATE TABLE role_permission (
    role_id uuid NOT NULL REFERENCES role(id),
    permission_id uuid NOT NULL REFERENCES permission(id),
    granted_at timestamptz NOT NULL DEFAULT now(),
    granted_by uuid REFERENCES app_user(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX ix_role_permission_permission ON role_permission (permission_id);

CREATE TABLE auth_session (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_user(id),
    refresh_token_hash varchar(255) NOT NULL UNIQUE,
    token_family_id uuid NOT NULL,
    issued_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    last_used_at timestamptz,
    revoked_at timestamptz,
    revoke_reason varchar(255),
    client_fingerprint varchar(255),
    replaced_by_session_id uuid REFERENCES auth_session(id),
    CHECK (expires_at > issued_at),
    CHECK (revoked_at IS NULL OR revoked_at >= issued_at)
);

CREATE INDEX ix_auth_session_user_active
    ON auth_session (user_id, expires_at)
    WHERE revoked_at IS NULL;
CREATE INDEX ix_auth_session_family ON auth_session (token_family_id);

CREATE TABLE system_setting (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key varchar(150) NOT NULL UNIQUE,
    setting_value jsonb NOT NULL,
    description text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid REFERENCES app_user(id),
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid REFERENCES app_user(id),
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE audit_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    actor_type varchar(20) NOT NULL
        CHECK (actor_type IN ('USER', 'SYSTEM', 'API_CONSUMER')),
    actor_user_id uuid REFERENCES app_user(id),
    module varchar(50) NOT NULL,
    action varchar(80) NOT NULL,
    target_type varchar(80) NOT NULL,
    target_id varchar(100) NOT NULL,
    correlation_id varchar(100),
    before_data jsonb,
    after_data jsonb,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX ix_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX ix_audit_log_actor ON audit_log (actor_user_id, occurred_at DESC);
CREATE INDEX ix_audit_log_target ON audit_log (target_type, target_id, occurred_at DESC);
CREATE INDEX ix_audit_log_module_action ON audit_log (module, action, occurred_at DESC);

