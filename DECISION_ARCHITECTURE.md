# DSS-Core: Decision Support System Architecture

## Overview

DSS-Core has evolved from a passive data aggregator into an active Decision Support System with:
- **Persistence Layer**: PostgreSQL with JPA/Hibernate and Envers auditing
- **Decision Engine**: Pluggable rule-based system using Specification Pattern
- **Action Tracking**: User interventions (approve, override, archive) with audit logs
- **Insights Dashboard**: Swiss-style flat UI for decision management

---

## 1. PERSISTENCE LAYER

### Database Schema

```
NORMALIZED_RECORDS (with Envers audit trail)
├── id (PK)
├── source_id
├── record_id (unique)
├── timestamp
├── data (JSONB)
├── status (valid/warning/error)
├── metadata (JSONB)
├── created_at, updated_at (audit)
└── version (optimistic lock)

DECISION_INSIGHTS
├── id (PK)
├── record_id (FK)
├── rule_name
├── insight_type (THRESHOLD/ANOMALY/TREND/CUSTOM)
├── severity (INFO/WARNING/CRITICAL)
├── message
├── metadata (JSONB - rule context)
├── status (OPEN/APPROVED/ARCHIVED)
└── created_at, updated_at

ACTION_LOGS
├── id (PK)
├── insight_id (FK)
├── action_type (APPROVE/OVERRIDE/ARCHIVE)
├── user_id
├── override_data (JSONB)
├── reason
└── created_at
```

### Entity Classes

- **NormalizedRecordEntity**: Stores ingested records with full audit history
- **DecisionInsightEntity**: Stores rule-triggered findings
- **ActionLogEntity**: Tracks user actions on insights

### Repositories

Spring Data JPA repositories provide query methods:
- `NormalizedRecordRepository`: Find by sourceId, recordId, timestamp range, status
- `DecisionInsightRepository`: Find by recordId, status, severity, ruleName
- `ActionLogRepository`: Find by insightId, actionType, timestamp range

---

## 2. DECISION RULES ENGINE

### Architecture Pattern: Specification Pattern

Each rule is a self-contained specification that:
1. Evaluates a record independently
2. Returns an optional `DecisionInsight` if triggered
3. Can be enabled/disabled dynamically
4. Requires no core engine changes to add new rules

### Core Interfaces

```java
public interface Rule {
    Optional<DecisionInsightEntity> evaluate(NormalizedRecordEntity record);
    String getRuleName();
    DecisionInsightEntity.InsightType getRuleType();
    boolean isEnabled();
}

public interface RuleEngine {
    void registerRule(Rule rule);
    void unregisterRule(String ruleName);
    List<DecisionInsightEntity> executeRules(NormalizedRecordEntity record);
    List<Rule> getRegisteredRules();
}
```

### Implementation: RuleEngineImpl

- Thread-safe rule registry (ConcurrentHashMap)
- Executes all enabled rules against each record
- Handles rule exceptions gracefully
- Returns list of triggered insights

### Example Rule: ThresholdRule

```java
new ThresholdRule("temperature", 100.0, ThresholdType.GREATER_THAN)
// Triggers when temperature > 100
```

### Adding New Rules

1. Implement `Rule` interface
2. Define evaluation logic
3. Register in `RuleConfiguration` or dynamically via API
4. No core code changes required

Example: Create `AnomalyRule`, `TrendRule`, etc. following the same pattern.

---

## 3. PROCESSING PIPELINE

### DecisionService: Orchestration

```
Ingest → Normalize → Persist → Execute Rules → Persist Insights
```

**Flow:**
1. `processRecord(NormalizedRecord)` receives normalized data
2. Converts DTO to JPA entity
3. Saves to database (transactional)
4. Executes all active rules via `RuleEngine`
5. Persists generated insights
6. Maintains transactional integrity

**Key Methods:**
- `processRecord(NormalizedRecord)`: Single record processing
- `processRecords(List<NormalizedRecord>)`: Batch processing
- `getOpenInsights()`: Retrieve unreviewed insights
- `getInsightsForRecord(Long recordId)`: Record-specific insights

### InsightActionService: User Interactions

Handles three user actions:

1. **Approve**: Mark insight as reviewed (status → APPROVED)
2. **Override**: Approve + store manual override data
3. **Archive**: Hide insight (status → ARCHIVED)

Each action creates an `ActionLogEntity` for audit trail.

---

## 4. API ENDPOINTS

### Insights Management

```
GET  /api/insights                    # Get all open insights
GET  /api/insights/{recordId}         # Get insights for specific record
POST /api/insights/{insightId}/approve    # Approve insight
POST /api/insights/{insightId}/override   # Override with manual data
POST /api/insights/{insightId}/archive    # Archive insight
```

### Request/Response Format

**Approve/Override/Archive:**
```json
POST /api/insights/123/approve
{
  "userId": "user@example.com",
  "reason": "Verified and acceptable",
  "overrideData": null  // Only for override action
}

Response:
{
  "status": "approved",
  "insightId": "123"
}
```

---

## 5. UI/UX: INSIGHTS DASHBOARD

### Design Philosophy: Swiss-Style Flat

- **No gradients, shadows, or decorative elements**
- **Monochromatic palette** (whites, grays, blacks)
- **Accent colors for status only:**
  - Blue (#3b82f6): Info/Open
  - Green (#4a9d6f): Approved
  - Orange (#f59e0b): Warning/Override
  - Red (#ef4444): Critical
  - Gray (#999): Archived

### Components

**Insights Table:**
- Rule name, type, severity, message, timestamp
- Status badges (OPEN/APPROVED/ARCHIVED)
- Action buttons: Approve, Override, Archive

**Action Modal:**
- User ID input
- Reason textarea
- Override data (JSON) for override action
- Submit/Cancel buttons

**Navigation:**
- Tab: Open Insights (default)
- Tab: All Insights (future)

### File: `src/main/resources/static/insights.html`

---

## 6. CONFIGURATION & INITIALIZATION

### RuleConfiguration

Initializes rules on application startup:

```java
@PostConstruct
public void initializeRules() {
    ruleEngine.registerRule(
        new ThresholdRule("temperature", 100.0, GREATER_THAN)
    );
    ruleEngine.registerRule(
        new ThresholdRule("stock_level", 10.0, LESS_THAN)
    );
}
```

### Application Properties

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/dss_core
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.format_sql=true

# Envers Auditing
spring.jpa.properties.hibernate.envers.audit_table_suffix=_audit
```

---

## 7. TRANSACTIONAL INTEGRITY

### @Transactional Boundaries

- **DecisionService.processRecord()**: Atomic record + insights persistence
- **InsightActionService methods**: Atomic action + audit log creation

### Optimistic Locking

- `NormalizedRecordEntity.version` prevents concurrent modification conflicts
- `DecisionInsightEntity.version` ensures insight consistency

---

## 8. EXTENDING THE SYSTEM

### Adding a New Rule Type

```java
@Slf4j
public class AnomalyRule implements Rule {
    private final String fieldName;
    private final double stdDevThreshold;
    private boolean enabled = true;
    
    @Override
    public Optional<DecisionInsightEntity> evaluate(NormalizedRecordEntity record) {
        // Implement anomaly detection logic
        // Return Optional<DecisionInsightEntity> if triggered
    }
    
    @Override
    public String getRuleName() { return "ANOMALY_" + fieldName; }
    
    @Override
    public DecisionInsightEntity.InsightType getRuleType() { 
        return InsightType.ANOMALY; 
    }
    
    @Override
    public boolean isEnabled() { return enabled; }
}
```

Then register in `RuleConfiguration`:
```java
ruleEngine.registerRule(new AnomalyRule("price", 2.5));
```

### Adding a New Data Source

Existing `DataIngestor` interface remains unchanged. Implement and register:

```java
@Component
public class WebhookDataIngestor implements DataIngestor {
    @Override
    public CompletableFuture<List<NormalizedRecord>> ingest() { ... }
}
```

---

## 9. DEPLOYMENT CHECKLIST

- [ ] PostgreSQL database created and accessible
- [ ] `application.properties` configured with DB credentials
- [ ] Flyway/Liquibase migrations applied (or `ddl-auto=create`)
- [ ] Rules initialized in `RuleConfiguration`
- [ ] API endpoints tested
- [ ] Insights dashboard accessible at `/insights.html`
- [ ] Audit tables created by Envers

---

## 10. FUTURE ENHANCEMENTS

- [ ] Dynamic rule management API (CRUD rules without restart)
- [ ] Rule versioning and rollback
- [ ] Batch insight generation for historical data
- [ ] Webhook notifications on critical insights
- [ ] Machine learning-based anomaly detection
- [ ] Data export (CSV, JSON) with audit trail
- [ ] Role-based access control (RBAC)
- [ ] Metrics and performance dashboards
- [ ] Insight correlation and root cause analysis

---

## Summary

DSS-Core now provides:
1. **Persistent storage** with full audit trail
2. **Pluggable rule engine** for extensible decision logic
3. **Action tracking** for compliance and transparency
4. **Minimal UI** for insight management
5. **Transactional integrity** across the pipeline

The architecture supports adding new rules, data sources, and actions without modifying core code.
