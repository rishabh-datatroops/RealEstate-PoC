-- Database schema for Real Estate POC

-- Create listings table
CREATE TABLE IF NOT EXISTS listings (
    id UUID PRIMARY KEY,
    address VARCHAR(500) NOT NULL,
    price BIGINT NOT NULL,
    property_type VARCHAR(50) NOT NULL DEFAULT 'RESIDENTIAL'
);

-- Create index on property type for efficient filtering
CREATE INDEX IF NOT EXISTS idx_listings_property_type ON listings(property_type);

-- Create outbox table for event sourcing
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Create index on outbox for efficient processing
CREATE INDEX IF NOT EXISTS idx_outbox_topic_created ON outbox(topic, created_at);

-- Create subscriptions table (replacing in-memory storage)
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    address VARCHAR(500) NOT NULL,
    price BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on subscriptions for efficient querying
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_address ON subscriptions(address);
