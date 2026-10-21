ALTER TABLE users ADD COLUMN IF NOT EXISTS instagram_handle VARCHAR(128);
ALTER TABLE users ADD COLUMN IF NOT EXISTS upi_id VARCHAR(128);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bank_account_name VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bank_account_number VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bank_ifsc VARCHAR(32);
ALTER TABLE users ADD COLUMN IF NOT EXISTS payment_qr_data TEXT;

ALTER TABLE money_requests ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(16);
ALTER TABLE money_requests ADD COLUMN IF NOT EXISTS payment_instructions TEXT;
ALTER TABLE money_requests ADD COLUMN IF NOT EXISTS payment_qr_data TEXT;
ALTER TABLE money_requests ADD COLUMN IF NOT EXISTS settlement_transaction_id VARCHAR(128);
ALTER TABLE money_requests ADD COLUMN IF NOT EXISTS settlement_proof_data BYTEA;
ALTER TABLE money_requests ADD COLUMN IF NOT EXISTS settlement_proof_mime_type VARCHAR(96);
ALTER TABLE money_requests ADD COLUMN IF NOT EXISTS settlement_submitted_at TIMESTAMPTZ;
ALTER TABLE money_requests ADD COLUMN IF NOT EXISTS settlement_verified_at TIMESTAMPTZ;

ALTER TABLE payment_reminders ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(16);
ALTER TABLE payment_reminders ADD COLUMN IF NOT EXISTS payment_instructions TEXT;
