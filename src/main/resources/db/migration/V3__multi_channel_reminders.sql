ALTER TABLE payment_reminders ADD COLUMN channels VARCHAR(64) NOT NULL DEFAULT 'whatsapp';
ALTER TABLE payment_reminders ADD COLUMN payer_instagram_id VARCHAR(128);

ALTER TABLE money_requests ADD COLUMN reminder_channels VARCHAR(64) NOT NULL DEFAULT 'whatsapp';
ALTER TABLE money_requests ADD COLUMN payer_instagram_id VARCHAR(128);
