ALTER TABLE outbox ADD COLUMN claim_token uuid;
ALTER TABLE outbox ADD COLUMN locked_until timestamptz;
CREATE INDEX outbox_claimable_idx ON outbox (locked_until, created_at) WHERE published_at IS NULL;
