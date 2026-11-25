ALTER TABLE payments ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
CREATE INDEX payments_created_at_idx ON payments(created_at DESC);
CREATE INDEX payments_status_created_at_idx ON payments(status,created_at DESC);
CREATE INDEX payments_currency_created_at_idx ON payments(currency,created_at DESC);
