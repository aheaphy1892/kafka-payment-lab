CREATE TABLE processed_events (id uuid PRIMARY KEY);
CREATE TABLE payment_decisions (payment_id uuid PRIMARY KEY, status varchar(20) NOT NULL);
CREATE TABLE outbox (id uuid PRIMARY KEY, aggregate_id uuid NOT NULL, topic text NOT NULL, payload jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), published_at timestamptz);
CREATE INDEX outbox_unpublished_idx ON outbox(created_at) WHERE published_at IS NULL;
