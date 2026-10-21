ALTER TABLE contacts ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE contacts ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX idx_contacts_owner_user ON contacts(owner_id, contact_user_id);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT,
    message_type VARCHAR(16) NOT NULL DEFAULT 'TEXT',
    sticker_code VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_chat_users CHECK (sender_id <> recipient_id)
);
CREATE INDEX idx_chat_conversation ON chat_messages(sender_id, recipient_id, created_at DESC);
CREATE INDEX idx_chat_recipient_unread ON chat_messages(recipient_id, read_at, created_at DESC);

ALTER TABLE money_requests ALTER COLUMN to_user_id DROP NOT NULL;
ALTER TABLE money_requests ADD COLUMN contact_id UUID REFERENCES contacts(id) ON DELETE SET NULL;
ALTER TABLE money_requests ADD COLUMN payer_name VARCHAR(100);
ALTER TABLE money_requests ADD COLUMN payer_msisdn VARCHAR(20);
ALTER TABLE money_requests ADD COLUMN expense_id UUID;
ALTER TABLE money_requests ADD COLUMN reminder_interval_days INTEGER;
ALTER TABLE money_requests ADD CONSTRAINT chk_request_recipient CHECK (to_user_id IS NOT NULL OR payer_msisdn IS NOT NULL);
ALTER TABLE money_requests ADD CONSTRAINT chk_request_reminder_interval CHECK (reminder_interval_days IS NULL OR reminder_interval_days IN (7, 10, 15, 30));

CREATE TABLE payment_reminders (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_type VARCHAR(16) NOT NULL,
    source_id UUID NOT NULL,
    expense_description VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    payer_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    payer_name VARCHAR(100) NOT NULL,
    payer_msisdn VARCHAR(20),
    interval_days INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    next_send_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_sent_at TIMESTAMP WITH TIME ZONE,
    last_delivery_status VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reminder_interval CHECK (interval_days IN (7, 10, 15, 30))
);
CREATE INDEX idx_reminders_due ON payment_reminders(status, next_send_at);
CREATE INDEX idx_reminders_source ON payment_reminders(owner_id, source_type, source_id);

CREATE TABLE reminder_deliveries (
    id UUID PRIMARY KEY,
    reminder_id UUID NOT NULL REFERENCES payment_reminders(id) ON DELETE CASCADE,
    channel VARCHAR(24) NOT NULL,
    destination VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(255),
    error_message VARCHAR(512),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_reminder_delivery_reminder ON reminder_deliveries(reminder_id, attempted_at DESC);
