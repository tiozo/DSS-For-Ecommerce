# DSS-Core: End-to-End Example

## Scenario: E-Commerce Order Monitoring

We want to monitor orders and trigger insights when:
1. Order value exceeds $10,000 (threshold rule)
2. Order quantity is outside normal range (anomaly rule)

---

## Step 1: Define Rules

### Rule 1: High-Value Order Alert

```java
// In RuleConfiguration.java
@PostConstruct
public void initializeRules() {
    // Alert when order value > $10,000
    ruleEngine.registerRule(
        new ThresholdRule("order_value", 10000.0, ThresholdRule.ThresholdType.GREATER_THAN)
    );
    
    // Alert when quantity outside normal range (1-100)
    ruleEngine.registerRule(
        new AnomalyRule("quantity", 1.0, 100.0)
    );
}
```

---

## Step 2: Prepare Test Data

### CSV File: `orders.csv`

```csv
order_id,customer_id,order_value,quantity,timestamp
ORD-001,CUST-123,5000.00,10,2024-01-15T10:30:00
ORD-002,CUST-456,15000.00,5,2024-01-15T10:35:00
ORD-003,CUST-789,3000.00,150,2024-01-15T10:40:00
ORD-004,CUST-101,8000.00,20,2024-01-15T10:45:00
```

---

## Step 3: Upload and Process Data

### Upload CSV

```bash
curl -X POST \
  -F "file=@orders.csv" \
  http://localhost:8080/api/upload-csv
```

**Response:**
```json
{
  "status": "success",
  "recordsProcessed": 4,
  "insightsGenerated": 2
}
```

### Process All Data

```bash
curl -X POST http://localhost:8080/api/process-all
```

---

## Step 4: View Generated Insights

### Get All Open Insights

```bash
curl http://localhost:8080/api/insights | jq
```

**Response:**
```json
[
  {
    "id": 1,
    "recordId": 2,
    "ruleName": "THRESHOLD_ORDER_VALUE_GREATER_THAN_10000",
    "insightType": "THRESHOLD",
    "severity": "WARNING",
    "message": "Field 'order_value' value 15000.00 exceeds threshold 10000.00",
    "metadata": "{\"field\":\"order_value\",\"value\":15000.00,\"threshold\":10000.00}",
    "status": "OPEN",
    "createdAt": "2024-01-15T10:35:00",
    "updatedAt": "2024-01-15T10:35:00"
  },
  {
    "id": 2,
    "recordId": 3,
    "ruleName": "ANOMALY_QUANTITY_[1-100]",
    "insightType": "ANOMALY",
    "severity": "WARNING",
    "message": "Anomaly detected: 'quantity' value 150.00 outside expected range [1.00, 100.00]",
    "metadata": "{\"field\":\"quantity\",\"value\":150.00,\"min\":1.00,\"max\":100.00}",
    "status": "OPEN",
    "createdAt": "2024-01-15T10:40:00",
    "updatedAt": "2024-01-15T10:40:00"
  }
]
```

---

## Step 5: User Actions on Insights

### Scenario A: Approve High-Value Order

The order value of $15,000 is legitimate (bulk customer).

```bash
curl -X POST http://localhost:8080/api/insights/1/approve \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "analyst@company.com",
    "reason": "Verified bulk order from premium customer"
  }'
```

**Response:**
```json
{
  "status": "approved",
  "insightId": "1"
}
```

**Database Changes:**
- `decision_insights.status` → APPROVED
- `action_logs` → New entry with APPROVE action

---

### Scenario B: Override Anomalous Quantity

The quantity of 150 is actually correct (special order), but we want to update the system.

```bash
curl -X POST http://localhost:8080/api/insights/2/override \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "analyst@company.com",
    "reason": "Special order - customer requested 150 units",
    "overrideData": "{\"quantity\": 150, \"order_type\": \"bulk_special\"}"
  }'
```

**Response:**
```json
{
  "status": "overridden",
  "insightId": "2"
}
```

**Database Changes:**
- `decision_insights.status` → APPROVED
- `action_logs` → New entry with OVERRIDE action and override_data

---

### Scenario C: Archive False Positive

If an insight is a false positive, archive it.

```bash
curl -X POST http://localhost:8080/api/insights/3/archive \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "analyst@company.com",
    "reason": "False positive - system misconfiguration"
  }'
```

**Response:**
```json
{
  "status": "archived",
  "insightId": "3"
}
```

---

## Step 6: Query Audit Trail

### View Insight History (Envers)

```sql
-- Query audit history for insight #1
SELECT * FROM decision_insights_audit 
WHERE id = 1 
ORDER BY rev DESC;
```

**Result:**
```
rev | revtype | id | status    | updated_at
3   | 1       | 1  | APPROVED  | 2024-01-15T10:50:00
1   | 0       | 1  | OPEN      | 2024-01-15T10:35:00
```

### View Action Log

```sql
-- Query all actions on insight #1
SELECT * FROM action_logs 
WHERE insight_id = 1 
ORDER BY created_at DESC;
```

**Result:**
```
id | insight_id | action_type | user_id              | reason
1  | 1          | APPROVE     | analyst@company.com  | Verified bulk order...
```

---

## Step 7: Dashboard Views

### Insights Dashboard (`/insights.html`)

**Before Actions:**
```
┌─────────────────────────────────────────────────────────────────┐
│ OPEN INSIGHTS                                                   │
├─────────────────────────────────────────────────────────────────┤
│ Rule                    │ Type      │ Severity │ Message        │
├─────────────────────────────────────────────────────────────────┤
│ THRESHOLD_ORDER_VALUE   │ THRESHOLD │ WARNING  │ Field 'order.. │
│ [Approve] [Override] [Archive]                                  │
├─────────────────────────────────────────────────────────────────┤
│ ANOMALY_QUANTITY        │ ANOMALY   │ WARNING  │ Anomaly detec..│
│ [Approve] [Override] [Archive]                                  │
└─────────────────────────────────────────────────────────────────┘
```

**After Actions:**
```
┌─────────────────────────────────────────────────────────────────┐
│ OPEN INSIGHTS                                                   │
├─────────────────────────────────────────────────────────────────┤
│ No insights                                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Step 8: Add Custom Rule

### Create a Fraud Detection Rule

```java
@Slf4j
public class FraudDetectionRule implements Rule {
    
    private final double suspiciousValueThreshold = 50000.0;
    private final int suspiciousQuantityThreshold = 1000;
    private boolean enabled = true;
    
    @Override
    public Optional<DecisionInsightEntity> evaluate(NormalizedRecordEntity record) {
        try {
            Map<String, Object> data = objectMapper.readValue(record.getData(), Map.class);
            
            double orderValue = Double.parseDouble(data.get("order_value").toString());
            int quantity = Integer.parseInt(data.get("quantity").toString());
            
            // Suspicious: high value + high quantity
            if (orderValue > suspiciousValueThreshold && quantity > suspiciousQuantityThreshold) {
                DecisionInsightEntity insight = DecisionInsightEntity.builder()
                    .recordId(record.getId())
                    .ruleName(getRuleName())
                    .insightType(getRuleType())
                    .severity(DecisionInsightEntity.Severity.CRITICAL)
                    .message("Potential fraud: High value + high quantity combination")
                    .metadata(String.format(
                        "{\"value\":%.2f,\"quantity\":%d,\"risk\":\"high\"}",
                        orderValue, quantity
                    ))
                    .status(DecisionInsightEntity.InsightStatus.OPEN)
                    .build();
                
                return Optional.of(insight);
            }
            
        } catch (Exception e) {
            log.error("Error evaluating fraud rule", e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public String getRuleName() { return "FRAUD_DETECTION"; }
    
    @Override
    public DecisionInsightEntity.InsightType getRuleType() { 
        return DecisionInsightEntity.InsightType.CUSTOM; 
    }
    
    @Override
    public boolean isEnabled() { return enabled; }
}
```

### Register the Rule

```java
// In RuleConfiguration.java
@PostConstruct
public void initializeRules() {
    // ... existing rules ...
    
    // Add fraud detection
    ruleEngine.registerRule(new FraudDetectionRule());
}
```

### Restart Application

```bash
mvn spring-boot:run
```

Now the fraud detection rule will run on all new records!

---

## Step 9: Monitor System Health

### Check Dashboard

```bash
curl http://localhost:8080/api/dashboard | jq
```

**Response:**
```json
{
  "sources": {
    "csv-upload-uuid": {
      "name": "CSV Upload",
      "valid": true,
      "recordCount": 4
    }
  },
  "recordCount": 4,
  "timestamp": 1705318200000
}
```

### Check Insights Summary

```bash
curl http://localhost:8080/api/insights | jq 'length'
```

**Response:**
```
0  # All insights have been actioned
```

---

## Complete Data Flow Visualization

```
CSV File (orders.csv)
    ↓
Upload to /api/upload-csv
    ↓
CsvDataIngestor.ingest()
    ↓
List<NormalizedRecord> (4 records)
    ↓
DecisionService.processRecords()
    ├─ Save record to DB
    ├─ Execute rules:
    │  ├─ ThresholdRule → Insight #1 (order_value > 10000)
    │  ├─ AnomalyRule → Insight #2 (quantity outside range)
    │  └─ FraudDetectionRule → No trigger
    └─ Save insights to DB
    ↓
Insights Dashboard (/insights.html)
    ├─ Display Insight #1 (OPEN)
    └─ Display Insight #2 (OPEN)
    ↓
User Actions
    ├─ Approve Insight #1 → Status: APPROVED, ActionLog created
    └─ Override Insight #2 → Status: APPROVED, ActionLog created
    ↓
Audit Trail
    ├─ normalized_records_audit (4 entries)
    ├─ decision_insights_audit (2 entries)
    └─ action_logs (2 entries)
```

---

## Summary

This example demonstrates:
1. ✅ Rule definition and registration
2. ✅ Data ingestion and processing
3. ✅ Insight generation
4. ✅ User actions (approve/override/archive)
5. ✅ Audit trail tracking
6. ✅ Custom rule implementation
7. ✅ System monitoring

The system is **extensible, auditable, and production-ready**!
