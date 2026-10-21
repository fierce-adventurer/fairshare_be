ALTER TABLE chat_messages ADD COLUMN voice_data BYTEA;
ALTER TABLE chat_messages ADD COLUMN voice_mime_type VARCHAR(96);
ALTER TABLE chat_messages ADD COLUMN voice_duration_ms INTEGER;
ALTER TABLE chat_messages ADD CONSTRAINT chk_chat_voice_duration
    CHECK (voice_duration_ms IS NULL OR (voice_duration_ms >= 250 AND voice_duration_ms <= 60000));
