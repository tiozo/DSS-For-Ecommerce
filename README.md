# DSS-Core: Decision Support System

A production-ready, admin-driven Decision Support System for e-commerce analytics with dynamic rule engine, real-time dashboards, and PostgreSQL persistence. Built with Spring Boot 3.2 and Swiss-style flat design principles.

## Executive Summary

DSS-Core is a high-performance Decision Support System (DSS) designed for e-commerce analytics. It features a modular data ingestion pipeline, a dynamic rule engine based on SpEL, and a modern analytics dashboard. Administrators can define business rules via CSV, which are then applied to incoming sales data to generate real-time metrics and decision insights.

## System Architecture

### Dual-Layer Ingestion
The system employs a dual-layer persistence strategy for maximum flexibility and performance:

1.  **Domain Persistence**: Raw records are saved to domain-specific tables (e.g., `sales_records`) for historical reporting and BI.
2.  **Normalized Persistence**: Records are simultaneously normalized into a flat `NormalizedRecordEntity` structure (JSON data blobs) to provide a uniform surface for the Rule Engine scanning.

### Dynamic Rules Engine
SpEL-powered engine with:
- **Hot-Reloadable Rules**: Upload `rules.csv` to update business logic instantly.
- **Unified Scanning**: The engine scans the normalized table, allowing one engine to process data from multiple diverse sources.
- **Insight Lifecycle**: Triggered rules generate `DecisionInsightEntity` records, which support explicit review actions (Approve, Override, Snooze, False Positive) with full audit logging.

### Data Flow

```
CSV Upload → SalesCsvProcessor → [SalesRecord (Domain DB)]
                           ↘
                             [NormalizedRecord (JSON DB)] → SalesRuleEngine → DecisionInsight → Dashboard
```

## SaaS Capability

### Admin-Driven Features

**Data Ingestion Pipeline**
- **Sequential Workflow**: Rules must be active/uploaded before Sales Data can be processed (enforced by UI locks).
- **Dynamic CSV Mapping**: Automatic parsing of `sales.csv` and `rules.csv`.
- **Validation**: Strict type checking during ingestion.

**Analytics & Insights Management**
- **Real-time Dashboard**: Integrated Charts.js visualizations (Revenue Trends, Category Splits, Status Distribution).
- **Advanced Grid**: Paginated, searchable, and sortable insights management table.
- **Decision Workflow**: Audit-backed review system (Approve, Override, Snooze, False Positive).
- **System Clock & Heartbeat**: Real-time system status indicators.
- **Audit Logs**: Every manual decision is tracked in `ActionLogEntity`.

### Multi-Tenant Support

Built-in tenant isolation via `TenantContext` and `TenantFilter`:
- Tenant ID in HTTP headers (`X-Tenant-ID`)
- Automatic tenant filtering in queries
- Separate data spaces per tenant

## Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **PostgreSQL 14+**

### Database Setup

```bash
# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Create database
sudo -u postgres psql
CREATE DATABASE dss_core;
GRANT ALL PRIVILEGES ON DATABASE dss_core TO postgres;
\q
```

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/dss_core
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Server
server.port=8080
```

### Build & Run

**Option 1: Quick Start Script**
```bash
./start.sh
```

**Option 2: Manual**
```bash
mvn clean install
mvn spring-boot:run
```

Access dashboard at: **http://localhost:8080/dashboard.html**

### Usage Workflow

1. **Upload Rules Config**
   - Navigate to the "Data Ingestion Pipeline" section.
   - Select and upload `rules.csv`.
   - **Note**: This unlocks the Sales Data upload button.

2. **Upload Sales Data**
   - Select `sales.csv`.
   - Click "Upload Sales".
   - System performs dual-persistence (Sales + Normalized) and triggers the Rule Engine.

3. **Manage Insights**
   - View the "Insights Management" table below the stats.
   - Use search and sorting to find specific triggered rules.
   - Click **Review** on any insight to open the resolution modal and log a decision.

4. **Advanced Analytics**
   - Expand the "View Advanced Analytics" section to see trend charts.
   - Charts refresh automatically when new data is uploaded.

## API Reference

### Admin Endpoints

**Upload Sales Data**
```http
POST /api/admin/upload-sales
Content-Type: multipart/form-data

file: sales.csv

Response:
{
  "success": true,
  "recordsProcessed": 50,
  "insightsGenerated": 3
}
```

**Upload Rules**
```http
POST /api/admin/upload-rules
Content-Type: multipart/form-data

file: rules.csv

Response:
{
  "success": true,
  "rulesProcessed": 3
}
```

**Dashboard Statistics**
```http
GET /api/admin/dashboard-stats

Response:
{
  "salesByMonth": [{"label": "2003-01", "value": 12345.67}],
  "salesByProductLine": [{"label": "Motorcycles", "value": 45678.90}],
  "statusDistribution": [{"label": "Shipped", "value": 45}],
  "totalRecords": 50,
  "insightCount": 3,
  "hasInsights": true
}
```

### Insights Management

**Get Open Insights**
```http
GET /api/insights/open
```

**Approve / Override / Archive Insight**
```http
POST /api/insights/{insightId}/{action}
Content-Type: application/json

{
  "userId": "admin",
  "reason": "Detailed audit note required for overrides"
}
```
*Valid actions: APPROVE, OVERRIDE, SNOOZE, FALSE_POSITIVE, ARCHIVE*

### Legacy Endpoints

**Dashboard Status**
```http
GET /api/dashboard
```

**Process All Sources**
```http
POST /api/process-all
```

**Retrieve Data**
```http
GET /api/data
GET /api/data?sourceId={sourceId}
```

## Database Schema

### Core Tables

**sales_records** (26 columns)
- `order_number`, `quantity_ordered`, `price_each`, `sales`
- `order_date`, `status`, `product_line`, `customer_name`
- Indexes: `order_date`, `product_line`, `status`

**rule_definitions**
- `rule_name`, `rule_type`, `condition`, `threshold`, `action`, `active`

**decision_insights**
- `rule_id`, `rule_name`, `severity`, `message`, `record_id`, `status`

**action_logs** (Audit Trail)
- `insight_id`, `action_type`, `user_id`, `override_data`, `reason`

**normalized_records** (JSON Data Store)
- `record_id`, `source_id`, `timestamp`, `data` (JSON blob of original row), `status`
- Used by Rule Engine for uniform scanning across different source types.

## Project Structure

```
src/main/java/com/dss/core/
├── DssApplication.java
├── api/
│   ├── AdminController.java          # Admin CSV uploads + dashboard
│   ├── ActionController.java         # Insight actions
│   ├── DssController.java            # Legacy endpoints
│   └── RuleController.java           # Rule management
├── decision/
│   ├── rule/
│   │   ├── Rule.java                 # Rule interface
│   │   ├── RuleEngine.java           # Engine interface
│   │   ├── RuleEngineImpl.java       # Engine implementation
│   │   ├── ThresholdRule.java        # Threshold rule type
│   │   └── AnomalyRule.java          # Anomaly detection
│   └── RuleConfiguration.java        # Rule initialization
├── ingestion/
│   ├── DataIngestor.java             # Ingestor interface
│   ├── NormalizedRecord.java         # Unified data format
│   └── impl/
│       ├── CsvDataIngestor.java      # CSV ingestion
│       └── ApiDataIngestor.java      # API ingestion
├── persistence/
│   ├── entity/
│   │   ├── SalesRecordEntity.java    # Sales data model
│   │   ├── RuleDefinitionEntity.java # Rule storage
│   │   ├── DecisionInsightEntity.java # Insights
│   │   ├── ActionLogEntity.java      # Audit logs
│   │   └── NormalizedRecordEntity.java # Legacy records
│   └── repository/
│       ├── SalesRecordRepository.java # Sales queries + aggregations
│       ├── RuleDefinitionRepository.java
│       ├── DecisionInsightRepository.java
│       └── ActionLogRepository.java
├── processing/
│   ├── SalesCsvProcessor.java        # Parse sales.csv
│   ├── RuleCsvProcessor.java         # Parse rules.csv
│   ├── SalesRuleEngine.java          # Sales-specific rules
│   ├── DecisionService.java          # Rule orchestration
│   ├── InsightActionService.java     # Action handling
│   └── DataProcessingService.java    # Legacy processing
└── tenant/
    ├── TenantContext.java            # Tenant state
    ├── TenantFilter.java             # HTTP filter
    └── TenantAspect.java             # AOP tenant injection

src/main/resources/
├── application.properties            # Configuration
└── static/
    ├── dashboard.html                # Admin dashboard (Swiss-style)
    └── insights.html                 # Insights management UI
```

## Extending the System

### Adding a New Rule Type

```java
@Slf4j
public class CustomRule implements Rule {
    private final String fieldName;
    private boolean enabled = true;
    
    @Override
    public Optional<DecisionInsightEntity> evaluate(NormalizedRecordEntity record) {
        // Custom evaluation logic
        if (/* condition */) {
            return Optional.of(DecisionInsightEntity.builder()
                .ruleName(getRuleName())
                .severity("medium")
                .message("Custom rule triggered")
                .build());
        }
        return Optional.empty();
    }
    
    @Override
    public String getRuleName() { return "CUSTOM_" + fieldName; }
    
    @Override
    public DecisionInsightEntity.InsightType getRuleType() { 
        return InsightType.CUSTOM; 
    }
    
    @Override
    public boolean isEnabled() { return enabled; }
}
```

Register in `RuleConfiguration`:
```java
@PostConstruct
public void initializeRules() {
    ruleEngine.registerRule(new CustomRule("fieldName"));
}
```

### Adding a New Data Source

Implement `DataIngestor` interface:

```java
@Component
public class WebhookDataIngestor implements DataIngestor {
    
    @Override
    public CompletableFuture<List<NormalizedRecord>> ingest() {
        return CompletableFuture.supplyAsync(() -> {
            // Webhook logic
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

## Testing

### Verify Installation

```bash
# Check database
psql -h localhost -U postgres -d dss_core -c "\dt"

# Expected tables: sales_records, rule_definitions, decision_insights
```

### Test Upload

```bash
# Upload sales data
curl -X POST -F "file=@sales.csv" \
  http://localhost:8080/api/admin/upload-sales

# Expected: {"success":true,"recordsProcessed":50,"insightsGenerated":3}
```

### Verify Data

```sql
-- Check sales records
SELECT COUNT(*) FROM sales_records;

-- Check insights
SELECT * FROM decision_insights;
```

## Troubleshooting

**PostgreSQL Connection Error**
```bash
sudo systemctl status postgresql
sudo systemctl restart postgresql
```

**Port 8080 Already in Use**
```bash
sudo lsof -i :8080
# Change port in application.properties
```

**Charts Not Rendering**
- Check browser console (F12)
- Verify Chart.js CDN accessible
- Ensure data exists in database

## Configuration Options

### Database

```properties
# PostgreSQL (Production)
spring.datasource.url=jdbc:postgresql://localhost:5432/dss_core

# H2 (Development)
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true
```

### JPA

```properties
# Auto-create tables
spring.jpa.hibernate.ddl-auto=update

# Validate schema only
spring.jpa.hibernate.ddl-auto=validate
```

### Logging

```properties
logging.level.com.dss=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

## Sample Data

**sales.csv** (50 records provided)
- Columns: ORDERNUMBER, QUANTITYORDERED, PRICEEACH, SALES, ORDERDATE, STATUS, PRODUCTLINE, CUSTOMERNAME, etc.

**rules.csv** (3 rules provided)
- Format: ruleName, ruleType, condition, threshold, action
- Example: "High Value Sale, threshold, sales > threshold, 5000, alert"

## License

Internal project for DSS301

---

**Version**: 1.0.1  
**Last Updated**: March 2026  
**Status**: Production Ready (IS Core Integration)
