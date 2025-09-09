# Real Estate POC

A real estate listing system built with Scala, Akka HTTP, PostgreSQL, and Apache Kafka using event-driven architecture.

## Features

- **Listings Management**: Create and update property listings with property types
- **Property Types**: Support for Residential, Commercial, Industrial, Land, Mixed-Use, and Investment properties
- **Event Sourcing**: All changes are captured as events using the outbox pattern
- **Notification System**: Subscribe to property alerts based on location and price
- **Search Index**: Real-time search projection from events with property type filtering
- **RESTful API**: Clean HTTP endpoints for all operations

## Architecture

- **Event-Driven**: Uses Apache Kafka for event streaming
- **Outbox Pattern**: Ensures reliable event publishing with database transactions
- **CQRS**: Separate read and write models
- **Microservices Ready**: Modular design for easy scaling

## Prerequisites

- Docker and Docker Compose
- Java 8 or higher
- SBT (Scala Build Tool)

## Quick Start

1. **Start Infrastructure**:
   ```bash
   docker-compose up -d
   ```

2. **Initialize Database**:
   ```bash
   ./init-db.sh
   ```

3. **Run the Application**:
   ```bash
   sbt run
   ```

The application will start on `http://localhost:8080`

## API Endpoints

### Listings

- `POST /listings` - Create a new listing
  ```json
  {
    "address": "123 Main St, City, State",
    "price": 500000,
    "propertyType": "RESIDENTIAL"
  }
  ```

- `GET /listings` - Get all listings

- `GET /listings/{id}` - Get a specific listing

- `PUT /listings/{id}/price` - Update listing price
  ```json
  {
    "price": 450000
  }
  ```

- `PUT /listings/{id}/propertyType` - Update listing property type
  ```json
  {
    "propertyType": "COMMERCIAL"
  }
  ```

- `GET /listings/types` - Get all available property types

- `GET /listings/type/{propertyType}` - Get listings by property type

### Subscriptions

- `POST /subscriptions` - Create a subscription
  ```json
  {
    "userId": "user123",
    "address": "Main St",
    "price": 600000
  }
  ```

- `GET /subscriptions` - Get all subscriptions

- `GET /subscriptions/{id}` - Get a specific subscription

- `DELETE /subscriptions/{id}` - Delete a subscription

- `GET /subscriptions/user/{userId}` - Get subscriptions for a user

### Health Check

- `GET /health` - Service health status

## Property Types

The system supports the following property types:

- **RESIDENTIAL**: Houses, apartments, condos for living
- **COMMERCIAL**: Office buildings, retail spaces, restaurants
- **INDUSTRIAL**: Factories, warehouses, manufacturing facilities
- **LAND**: Vacant land, plots, agricultural land
- **MIXED_USE**: Combined residential and commercial properties
- **INVESTMENT**: Properties for investment purposes

## Event Flow

1. **Listing Creation**: When a listing is created, a `ListingCreated` event is stored in the outbox
2. **Price Update**: When a price is updated, a `ListingPriceChanged` event is stored in the outbox
3. **Property Type Update**: When a property type is updated, a `ListingPropertyTypeChanged` event is stored in the outbox
4. **Event Publishing**: The `OutboxPublisher` periodically publishes events to Kafka
5. **Event Processing**: 
   - `SearchProjector` updates the search index with property type information
   - `NotificationService` checks subscriptions and sends notifications with property type details

## Configuration

Configuration is in `src/main/resources/application.conf`:

```hocon
db {
  url = "jdbc:postgresql://localhost:5432/realestate"
  user = "realestate"
  password = "realestate"
  driver = "org.postgresql.Driver"
  numThreads = 5
  maxConnections = 5
}

kafka {
  bootstrapServers = "localhost:9092"
  listingsTopic = "listings.events"
  consumerGroupId = "real-estate-poc"
}

server {
  host = "0.0.0.0"
  port = 8080
}
```

## Development

### Project Structure

```
src/main/scala/com/realestate/
├── api/                    # HTTP routes
├── consumer/              # Kafka consumers
├── db/                    # Database layer
├── domain/                # Domain models (including PropertyType)
├── events/                # Event definitions
├── producer/              # Kafka producers
├── service/               # Business logic
└── Main.scala            # Application entry point
```

### Key Components

- **PropertyType**: Enum for different property categories
- **OutboxPublisher**: Publishes events from database outbox to Kafka
- **SearchProjector**: Builds search index from listing events with property type support
- **NotificationService**: Processes events to send notifications with property type information
- **ListingRepository**: Manages listing data with event sourcing and property type filtering
- **NotificationRepository**: Manages subscription data

## Testing

```bash
sbt test
```

## Troubleshooting

1. **Database Connection Issues**: Ensure PostgreSQL is running and accessible
2. **Kafka Connection Issues**: Check if Kafka and Zookeeper are running
3. **Event Processing Issues**: Check logs for parsing errors or missing event types
4. **Property Type Issues**: Ensure property type values match the enum (case-sensitive)

## Improvements Made

- ✅ Proper database schema with indexes
- ✅ Complete JSON API with proper request/response models
- ✅ Error handling throughout the application
- ✅ Outbox pattern with message deletion after publishing
- ✅ Proper event parsing in consumers
- ✅ Database-backed subscriptions (replaced in-memory storage)
- ✅ Health check endpoint
- ✅ Better configuration management
- ✅ Improved logging and error reporting
- ✅ Property type enum with comprehensive support
- ✅ Property type filtering and search capabilities
- ✅ Enhanced event schema with property type information
