-- V1__initial_schema.sql

-- 1. USERS
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    initials VARCHAR(10),
    avatar_url VARCHAR(512),
    color VARCHAR(32) DEFAULT '#6366F1',
    phone VARCHAR(32) UNIQUE,
    default_currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    onboarding_step VARCHAR(32) NOT NULL DEFAULT 'welcome',
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 2. USER FEDERATED IDENTITIES (MULTI-OAUTH)
CREATE TABLE user_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id)
);
CREATE INDEX idx_user_identities_user ON user_identities(user_id);

-- 3. CONTACTS (MSISDN PHONE-BASED)
CREATE TABLE contacts (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_owner_msisdn UNIQUE (owner_id, msisdn)
);
CREATE INDEX idx_contacts_owner ON contacts(owner_id);
CREATE INDEX idx_contacts_msisdn ON contacts(msisdn);

-- 4. GROUPS
CREATE TABLE groups (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    kind VARCHAR(20) NOT NULL DEFAULT 'other',
    emoji VARCHAR(16) DEFAULT '💰',
    simplify_debts BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 5. GROUP MEMBERS
CREATE TABLE group_members (
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id)
);
CREATE INDEX idx_group_members_user ON group_members(user_id);

-- 6. EXPENSES
CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    notes TEXT,
    category VARCHAR(64) NOT NULL DEFAULT 'General',
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_expense_idempotency UNIQUE (group_id, idempotency_key)
);
CREATE INDEX idx_expenses_group ON expenses(group_id, occurred_at DESC);

-- 7. EXPENSE ALLOCATIONS
CREATE TABLE expense_allocations (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(10) NOT NULL CHECK (type IN ('PAYER', 'SHARER')),
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    CONSTRAINT uk_allocation UNIQUE (expense_id, user_id, type)
);
CREATE INDEX idx_allocations_user ON expense_allocations(user_id);
CREATE INDEX idx_allocations_expense ON expense_allocations(expense_id);

-- 8. PAYMENTS
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    from_user_id UUID NOT NULL REFERENCES users(id),
    to_user_id UUID NOT NULL REFERENCES users(id),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    note VARCHAR(255),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_different_users CHECK (from_user_id <> to_user_id)
);
CREATE INDEX idx_payments_group ON payments(group_id, occurred_at DESC);

-- 9. UPCOMING BILLS
CREATE TABLE upcoming_bills (
    id UUID PRIMARY KEY,
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    due_date DATE NOT NULL,
    recurrence VARCHAR(20) NOT NULL DEFAULT 'once',
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_bills_due ON upcoming_bills(due_date, status);

-- 10. BILL ASSIGNEES
CREATE TABLE bill_assignees (
    bill_id UUID NOT NULL REFERENCES upcoming_bills(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (bill_id, user_id)
);

-- 11. MONEY REQUESTS
CREATE TABLE money_requests (
    id UUID PRIMARY KEY,
    group_id UUID REFERENCES groups(id) ON DELETE SET NULL,
    from_user_id UUID NOT NULL REFERENCES users(id),
    to_user_id UUID NOT NULL REFERENCES users(id),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    description VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'open',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_request_users CHECK (from_user_id <> to_user_id)
);
CREATE INDEX idx_requests_to ON money_requests(to_user_id, status);
CREATE INDEX idx_requests_expiry ON money_requests(expires_at);

-- 12. ACTIVITY EVENTS (AUDIT TRAIL)
CREATE TABLE activity_events (
    id UUID PRIMARY KEY,
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id UUID,
    payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_activity_group ON activity_events(group_id, created_at DESC);
CREATE INDEX idx_activity_actor ON activity_events(actor_id, created_at DESC);
