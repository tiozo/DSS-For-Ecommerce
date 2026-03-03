# DSS-Core: Decision Support System

A high-performance, modular Decision Support System built with Spring Boot and a Swiss-style flat design frontend.

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

Access the dashboard at: **http://localhost:8080**

## Architecture Overview

### Modular Ingestion Layer

The system uses a pluggable `DataIngestor` interface to support multiple data sources:

- **CSV Upload**: Direct file uploads with automatic parsing
- **API Polling**: External API endpoints with JSON/array support
- **Extensible**: Add webhooks, Kafka, databases, etc. without modifying core code

### Data Normalization

All sources normalize to a unified `NormalizedRecord` format:

```json
{
  "sourceId": "csv-upload-uuid",
  "recordId": "csv-upload-uuid-0",
  "timestamp": "2024-01-15T10:30:00",
  "data": {
    "column1": "value1",
    "column2": "value2"
  },
  "status": "valid",
  "metadata": "{...}"
}
```

### Processing Pipeline

1. **Ingestion**: Data sources are polled/triggered asynchronously
2. **Normalization**: Raw data converted to unified format
3. **Aggregation**: All records collected in-memory (extensible to database)
4. **Analysis**: DSS engine processes normalized data

## API Endpoints

### Dashboard
```
GET /api/dashboard
```
Returns active sources and record count.

### CSV Upload
```
POST /api/upload-csv
Content-Type: multipart/form-data

file: <csv-file>
```

### API Configuration
```
POST /api/configure-api
Content-Type: application/json

{
  "endpoint": "https://api.example.com/data",
  "method": "GET"
}
```

### Process Data
```
POST /api/process-all          # Process all sources
POST /api/process-api          # Process API source only
```

### Retrieve Data
```
GET /api/data                  # All records
GET /api/data?sourceId=<id>    # Records from specific source
```

## Frontend Design

**Swiss-Style Flat Design Philosophy:**
- No gradients, shadows, or decorative elements
- Monochromatic palette (whites, grays, blacks)
- Accent colors for status indicators only:
  - Green: Valid data
  - Orange: Warning
  - Red: Error
- Grid-based layout with clear borders
- High information density

**Key Components:**
- **Dashboard Cards**: KPIs (total records, active sources, last update)
- **Sources List**: Real-time status of each data source
- **Data Table**: Preview of processed records
- **Modals**: CSV upload and API configuration forms

## Adding a New Data Source

### Step 1: Create Ingestor Implementation

```java
@Component
public class WebhookDataIngestor implements DataIngestor {
    
    @Override
    public CompletableFuture<List<NormalizedRecord>> ingest() {
        return CompletableFuture.supplyAsync(() -> {
            // Your webhook logic here
            List<NormalizedRecord> records = new ArrayList<>();
            // ... populate records
            return records;
        });
    }
    
    @Override
    public String getSourceId() { return "webhook-1"; }
    
    @Override
    public String getSourceName() { return "Webhook Source"; }
    
    @Override
    public boolean validateConfig() { return true; }
}
```

### Step 2: Register in Controller

```java
@Autowired
private WebhookDataIngestor webhookIngestor;

@PostConstruct
public void init() {
    processingService.registerIngestor(webhookIngestor);
}
```

## Project Structure

```
src/main/java/com/dss/core/
├── DssApplication.java
├── api/
│   └── DssController.java
├── ingestion/
│   ├── DataIngestor.java
│   ├── NormalizedRecord.java
│   └── impl/
│       ├── CsvDataIngestor.java
│       └── ApiDataIngestor.java
└── processing/
    ├── DataProcessingService.java
    └── ProcessingResult.java

src/main/resources/
├── application.properties
└── static/
    └── index.html
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
spring.application.name=dss-core
server.port=8080
spring.datasource.url=jdbc:h2:mem:testdb
logging.level.com.dss=DEBUG
```

## Future Enhancements

- [ ] Database persistence (JPA entities)
- [ ] Webhook support for real-time ingestion
- [ ] Data validation rules engine
- [ ] DSS decision logic and rule evaluation
- [ ] Spring Security authentication
- [ ] Metrics and health checks
- [ ] Batch processing for large datasets
- [ ] Data export (CSV, JSON)

## License

Internal project for DSS301
