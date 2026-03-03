# DSS-Core: Quick Setup Guide

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 12+

## Step 1: Database Setup

```bash
# Create database
createdb dss_core

# Or via psql:
psql -U postgres
CREATE DATABASE dss_core;
```

## Step 2: Configure Application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/dss_core
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=create  # First run: create, then validate
```

## Step 3: Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

Access:
- **Dashboard**: http://localhost:8080
- **Insights**: http://localhost:8080/insights.html

## Step 4: Test the Pipeline

### 1. Upload CSV Data

```bash
curl -X POST -F "file=@data.csv" http://localhost:8080/api/upload-csv
```

### 2. Process Data

```bash
curl -X POST http://localhost:8080/api/process-all
```

### 3. View Insights

```bash
curl http://localhost:8080/api/insights
```

### 4. Approve an Insight

```bash
curl -X POST http://localhost:8080/api/insights/1/approve \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user@example.com",
    "reason": "Verified"
  }'
```

## Step 5: Add Custom Rules

Edit `RuleConfiguration.java`:

```java
@PostConstruct
public void initializeRules() {
    // Add your rules here
    ruleEngine.registerRule(
        new ThresholdRule("your_field", 50.0, GREATER_THAN)
    );
}
```

Restart the application.

## Database Schema

Envers automatically creates audit tables:
- `normalized_records_audit`
- `decision_insights_audit`

Query audit history:
```sql
SELECT * FROM normalized_records_audit WHERE rev = 1;
```

## Troubleshooting

**Connection refused:**
- Ensure PostgreSQL is running: `pg_isready`

**Table not found:**
- Check `spring.jpa.hibernate.ddl-auto=create` is set on first run

**Rules not triggering:**
- Verify rules are registered in `RuleConfiguration`
- Check logs: `logging.level.com.dss=DEBUG`

## Next Steps

1. Implement custom rules (AnomalyRule, TrendRule, etc.)
2. Integrate with your data sources
3. Configure alert notifications
4. Set up monitoring and metrics
