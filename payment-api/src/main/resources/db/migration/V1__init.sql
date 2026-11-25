CREATE TABLE payments (id uuid PRIMARY KEY, amount numeric(19,2) NOT NULL, currency varchar(3) NOT NULL, status varchar(20) NOT NULL);
CREATE TABLE outbox (id uuid PRIMARY KEY, aggregate_id uuid NOT NULL, topic text NOT NULL, payload jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), published_at timestamptz);
CREATE INDEX outbox_unpublished_idx ON outbox(created_at) WHERE published_at IS NULL;
CREATE TABLE processed_events (id uuid PRIMARY KEY);
