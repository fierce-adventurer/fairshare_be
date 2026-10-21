ALTER TABLE users ADD COLUMN first_name VARCHAR(80);
ALTER TABLE users ADD COLUMN last_name VARCHAR(80);
ALTER TABLE users ADD COLUMN date_of_birth DATE;
ALTER TABLE users ADD COLUMN phone_verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN terms_accepted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN profile_reviewed_at TIMESTAMP WITH TIME ZONE;

UPDATE users
SET first_name = split_part(trim(name), ' ', 1),
    last_name = CASE
        WHEN position(' ' IN trim(name)) > 0 THEN substring(trim(name) FROM position(' ' IN trim(name)) + 1)
        ELSE ''
    END,
    onboarding_step = 'done',
    profile_reviewed_at = COALESCE(profile_reviewed_at, created_at),
    terms_accepted_at = COALESCE(terms_accepted_at, created_at),
    email_verified_at = COALESCE(email_verified_at, created_at),
    phone_verified_at = CASE WHEN phone IS NOT NULL THEN COALESCE(phone_verified_at, created_at) ELSE NULL END;

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verification_user ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_expiry ON email_verification_tokens(expires_at);
