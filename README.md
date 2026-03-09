# DSS-Core: Decision Support System

A production-ready, admin-driven Decision Support System for e-commerce analytics with dynamic rule engine, real-time dashboards, and PostgreSQL persistence. Built with Spring Boot 3.2 and Swiss-style flat design principles.

## Executive Summary

DSS-Core is a SaaS platform that transforms raw sales data into actionable business insights through dynamic CSV ingestion, configurable business rules, and real-time analytics dashboards. Administrators upload sales and rules data via a web interface, triggering automatic rule evaluation that generates insights with visual indicators—eliminating manual data processing and enabling data-driven decision making at scale.

## System Architecture

### Modular Ingestion Layer

The system uses a pluggable `DataIngestor` interface supporting multiple data sources:

- **CSV Upload**: Direct file uploads with automatic parsing (sales.csv with 26 columns)
- **API Polling**: External API endpoints with JSON support
- **Extensible**: Add webhooks, Kafka, databases without modifying core code

All sources normalize to a unified `NormalizedRecord` format before processing.

### Dynamic Rules Engine

Specification Pattern-based rule engine with:

- **Pluggable Rules**: Add/remove rules without code changes
- **Multiple Rule Types**: Threshold, Anomaly, Trend, Custom
- **Automatic Evaluation**: Rules execute on data upload
- **Insight Generation**: Triggered rules create `DecisionInsightEntity` records
- **Action Tracking**: Approve, override, or archive insights with full audit trail

### Data Flow

```
CSV Upload → SalesCsvProcessor → SalesRecordEntity (PostgreSQL)
                                         ↓
                                  SalesRuleEngine
                                         ↓
                              DecisionInsightEntity
                                         ↓
                              Dashboard (Charts + Badge)
```

## SaaS Capability

### Admin-Driven Features

**Dynamic CSV Import**
- Upload `sales.csv` (26 columns: orderNumber, sales, productLine, status, etc.)
- Upload `rules.csv` (ruleName, ruleType, condition, threshold, action)
- Automatic parsing, validation, and database persistence
- Real-time status feedback

**Sales Analytics Dashboard**
- **Line Chart**: Total sales over time (monthly aggregation)
- **Bar Chart**: Sales by product line
- **Pie Chart**: Order status distribution
- **KPI Cards**: Total records, insights count
- **Insight Badge**: Green indicator when rules trigger
- Swiss-style flat design (monochrome, high-density, no gradients)

**Dynamic Rules Engine**
- CSV-based rule configuration
- Threshold rules (e.g., "High Value Sale > $5000")
- Automatic evaluation on data upload
- Insight management (approve, override, archive)
- Full audit trail with `ActionLogEntity`

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

1. **Upload Sales Data**
   - Navigate to "Data Management" tab
   - Select `sales.csv` (sample provided)
   - Click "Upload Sales CSV"
   - System parses → saves → runs rules → generates insights

2. **Upload Rules** (Optional)
   - Select `rules.csv` (sample provided)
   - Click "Upload Rules CSV"
   - Rules stored in `rule_definitions` table

3. **View Analytics**
   - Switch to "Dashboard" tab
   - View 3 charts with real-time data
   - Green badge appears if insights generated

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
GET /api/insights
```

**Approve Insight**
```http
POST /api/insights/{insightId}/approve
Content-Type: application/json

{
  "userId": "admin@example.com",
  "reason": "Verified and acceptable"
}
```

**Override Insight**
```http
POST /api/insights/{insightId}/override
Content-Type: application/json

{
  "userId": "admin@example.com",
  "reason": "Manual correction applied",
  "overrideData": {"correctedValue": 1000}
}
```

**Archive Insight**
```http
POST /api/insights/{insightId}/archive
Content-Type: application/json

{
  "userId": "admin@example.com",
  "reason": "No longer relevant"
}
```

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

**normalized_records** (Legacy)
- `source_id`, `record_id`, `timestamp`, `data`, `status`, `metadata`

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

**Version**: 1.0.0  
**Last Updated**: 2024  
**Status**: P.O.C Ready
