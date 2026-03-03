# DSS-Core Architecture Guide

## Project Structure

```
src/main/java/com/dss/core/
├── DssApplication.java                 # Spring Boot entry point
├── api/
│   └── DssController.java              # REST endpoints
├── ingestion/
│   ├── DataIngestor.java               # Core interface
│   ├── NormalizedRecord.java           # Unified data format
│   └── impl/
│       ├── CsvDataIngestor.java        # CSV implementation
│       └── ApiDataIngestor.java        # API implementation
└── processing/
    ├── DataProcessingService.java      # Orchestration service
    └── ProcessingResult.java           # Result DTO

src/main/resources/
├── application.properties              # Spring config
└── static/
    └── index.html                      # Dashboard UI
```

## Core Architecture

### 1. DataIngestor Interface

The `DataIngestor` interface is the foundation of the modular design:

```java
public interface DataIngestor {
    CompletableFuture<List<NormalizedRecord>> ingest();
    String getSourceId();
    String getSourceName();
    boolean validateConfig();
}
```

**Key Design Decisions:**
- **Async-first**: Uses `CompletableFuture` for non-blocking I/O
- **Pluggable**: New sources (Webhooks, Kafka, etc.) can be added without modifying existing code
- **Validation**: Each ingestor validates its own configuration

### 2. Data Normalization Flow

All data sources (CSV, API, future webhooks) are normalized into a uniform `NormalizedRecord`:

```
CSV File → CsvDataIngestor → NormalizedRecord
                                    ↓
API Endpoint → ApiDataIngestor → NormalizedRecord
                                    ↓
                        Processing Pipeline
                                    ↓
                        DSS Engine / Analysis
```

**NormalizedRecord Structure:**
```java
{
    sourceId: "csv-upload-uuid",
    recordId: "csv-upload-uuid-0",
    timestamp: LocalDateTime.now(),
    data: { "column1": "value1", "column2": "value2" },
    status: "valid|warning|error",
    metadata: "{...}"
}
```

### 3. Processing Pipeline

`DataProcessingService` orchestrates the ingestion and processing:

- **Async Processing**: All ingestors run in parallel via `CompletableFuture.allOf()`
- **Source Registration**: Ingestors are registered/unregistered dynamically
- **Data Aggregation**: Processed data is stored in-memory (can be extended to database)

### 4. REST API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/dashboard` | GET | Dashboard status (sources, record count) |
| `/api/upload-csv` | POST | Upload and process CSV file |
| `/api/configure-api` | POST | Configure API endpoint |
| `/api/process-api` | POST | Trigger API data ingestion |
| `/api/process-all` | POST | Process all registered sources |
| `/api/data` | GET | Retrieve processed data (optional sourceId filter) |

## Data Normalization Strategy

### CSV Ingestion
1. Parse CSV with Apache Commons CSV
2. Convert each row to a Map<String, Object>
3. Wrap in NormalizedRecord with metadata

### API Ingestion
1. Call external API endpoint
2. Parse JSON response (handles both array and object responses)
3. Convert each item to NormalizedRecord

### Extensibility
To add a new source (e.g., Webhook):

```java
@Component
public class WebhookDataIngestor implements DataIngestor {
    @Override
    public CompletableFuture<List<NormalizedRecord>> ingest() {
        // Webhook-specific logic
    }
    // ... implement other methods
}
```

Then register it in the controller:
```java
processingService.registerIngestor(webhookIngestor);
```

## Frontend Design Philosophy

**Swiss-Style Flat Design:**
- No gradients, shadows, or decorative elements
- Monochromatic palette (whites, grays, blacks)
- Accent colors only for status (green=valid, orange=warning, red=error)
- Grid-based layout with clear borders
- High information density with card-based UI
- Focus on readability and data visualization

**Key Components:**
- Dashboard cards: KPIs (total records, active sources, last update)
- Sources list: Real-time status of each data source
- Data table: Preview of processed records (first 10)
- Modals: CSV upload and API configuration

## Running the Application

```bash
mvn clean install
mvn spring-boot:run
```

Access dashboard at: `http://localhost:8080`

## Future Enhancements

1. **Database Persistence**: Replace in-memory storage with JPA entities
2. **Webhook Support**: Add webhook ingestor for real-time data
3. **Data Validation Rules**: Implement pluggable validation pipeline
4. **DSS Engine**: Add decision logic and rule evaluation
5. **Authentication**: Secure endpoints with Spring Security
6. **Monitoring**: Add metrics and health checks
