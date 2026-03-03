# DSS-Core: Architecture Diagrams & Component Interactions

## 1. SYSTEM ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND LAYER                                  │
├─────────────────────────────────────────────────────────────────────────┤
│  Dashboard (index.html)          │  Insights Dashboard (insights.html)  │
│  - Data sources                  │  - Open insights table               │
│  - Processed records             │  - Action buttons (approve/override) │
│  - Upload/Configure              │  - User action modal                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                         API LAYER (REST)                                │
├─────────────────────────────────────────────────────────────────────────┤
│  DssController                                                          │
│  ├── /api/insights (GET)                                               │
│  ├── /api/insights/{recordId} (GET)                                    │
│  ├── /api/insights/{insightId}/approve (POST)                          │
│  ├── /api/insights/{insightId}/override (POST)                         │
│  └── /api/insights/{insightId}/archive (POST)                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER (Business Logic)                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐                   │
│  │  DecisionService     │  │ InsightActionService │                   │
│  ├──────────────────────┤  ├──────────────────────┤                   │
│  │ processRecord()      │  │ approveInsight()     │                   │
│  │ processRecords()     │  │ overrideInsight()    │                   │
│  │ getOpenInsights()    │  │ archiveInsight()     │                   │
│  │ getInsightsForRecord │  └──────────────────────┘                   │
│  └──────────────────────┘                                              │
│           ↓                                                             │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │              RuleEngine (Decision Logic)                         │  │
│  ├──────────────────────────────────────────────────────────────────┤  │
│  │  RuleEngineImpl                                                   │  │
│  │  ├── registerRule(Rule)                                          │  │
│  │  ├── executeRules(NormalizedRecordEntity)                        │  │
│  │  └── getRegisteredRules()                                        │  │
│  │                                                                  │  │
│  │  Rules (Specification Pattern):                                 │  │
│  │  ├── ThresholdRule                                              │  │
│  │  ├── AnomalyRule                                                │  │
│  │  └── [Custom Rules]                                             │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    PERSISTENCE LAYER (Data Access)                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐                   │
│  │ NormalizedRecord     │  │ DecisionInsight      │                   │
│  │ Repository           │  │ Repository           │                   │
│  └──────────────────────┘  └──────────────────────┘                   │
│           ↓                           ↓                                │
│  ┌──────────────────────┐  ┌──────────────────────┐                   │
│  │ NormalizedRecord     │  │ DecisionInsight      │                   │
│  │ Entity               │  │ Entity               │                   │
│  │ (JPA + Envers)       │  │ (JPA + Envers)       │                   │
│  └──────────────────────┘  └──────────────────────┘                   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  ActionLogRepository → ActionLogEntity                           │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                      DATABASE LAYER (PostgreSQL)                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐                   │
│  │ normalized_records   │  │ decision_insights    │                   │
│  │ (+ audit table)      │  │ (+ audit table)      │                   │
│  └──────────────────────┘  └──────────────────────┘                   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  action_logs                                                     │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. DATA FLOW: INGEST → PROCESS → DECIDE → ACT

```
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 1: INGEST                                                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  CSV Upload / API Poll / Webhook                                       │
│         ↓                                                               │
│  DataIngestor.ingest()                                                 │
│         ↓                                                               │
│  List<NormalizedRecord> (DTO)                                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 2: NORMALIZE & PERSIST                                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  DecisionService.processRecord(NormalizedRecord)                       │
│         ↓                                                               │
│  Convert DTO → NormalizedRecordEntity                                  │
│         ↓                                                               │
│  recordRepository.save(entity)  [TRANSACTIONAL]                        │
│         ↓                                                               │
│  NormalizedRecordEntity persisted to DB                                │
│  (Envers creates audit entry)                                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 3: EXECUTE RULES                                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ruleEngine.executeRules(savedRecord)                                  │
│         ↓                                                               │
│  For each enabled Rule:                                                │
│    rule.evaluate(record)                                               │
│         ↓                                                               │
│  Optional<DecisionInsightEntity> returned                              │
│         ↓                                                               │
│  List<DecisionInsightEntity> insights collected                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 4: PERSIST INSIGHTS                                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  insightRepository.saveAll(insights)  [TRANSACTIONAL]                  │
│         ↓                                                               │
│  DecisionInsightEntity records persisted                               │
│  (Envers creates audit entries)                                        │
│         ↓                                                               │
│  Status: OPEN (awaiting user action)                                   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 5: USER ACTION                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  UI: Insights Dashboard displays open insights                         │
│         ↓                                                               │
│  User clicks: Approve / Override / Archive                             │
│         ↓                                                               │
│  POST /api/insights/{id}/[action]                                      │
│         ↓                                                               │
│  InsightActionService.[action]Insight()                                │
│         ↓                                                               │
│  Update insight.status                                                 │
│  Create ActionLogEntity                                                │
│  Both persisted [TRANSACTIONAL]                                        │
│         ↓                                                               │
│  Audit trail complete                                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. RULE ENGINE: SPECIFICATION PATTERN

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         RuleEngine Interface                            │
├─────────────────────────────────────────────────────────────────────────┤
│  registerRule(Rule)                                                     │
│  unregisterRule(String)                                                 │
│  executeRules(NormalizedRecordEntity)                                   │
│  getRegisteredRules()                                                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                      RuleEngineImpl (Concrete)                           │
├─────────────────────────────────────────────────────────────────────────┤
│  rules: ConcurrentHashMap<String, Rule>                                │
│                                                                         │
│  executeRules(record):                                                  │
│    for each rule in rules.values():                                    │
│      if rule.isEnabled():                                              │
│        Optional<Insight> = rule.evaluate(record)                       │
│        if present: add to insights list                                │
│    return insights                                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
        ┌───────────────────────────┼───────────────────────────┐
        ↓                           ↓                           ↓
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│  ThresholdRule   │      │  AnomalyRule     │      │  CustomRule      │
├──────────────────┤      ├──────────────────┤      ├──────────────────┤
│ fieldName        │      │ fieldName        │      │ [Your logic]     │
│ threshold        │      │ expectedMin/Max  │      │                  │
│ thresholdType    │      │                  │      │                  │
│                  │      │                  │      │                  │
│ evaluate():      │      │ evaluate():      │      │ evaluate():      │
│  if value >      │      │  if value <      │      │  [Your logic]    │
│  threshold:      │      │  min or >        │      │  return Insight  │
│  return Insight  │      │  max:            │      │                  │
│                  │      │  return Insight  │      │                  │
└──────────────────┘      └──────────────────┘      └──────────────────┘
        ↓                           ↓                           ↓
        └───────────────────────────┼───────────────────────────┘
                                    ↓
                    All return Optional<DecisionInsightEntity>
                    (empty if rule doesn't trigger)
```

---

## 4. TRANSACTIONAL BOUNDARIES

```
┌─────────────────────────────────────────────────────────────────────────┐
│  @Transactional                                                         │
│  DecisionService.processRecord(NormalizedRecord)                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  BEGIN TRANSACTION                                                      │
│    ├─ recordRepository.save(entity)                                    │
│    ├─ ruleEngine.executeRules(savedRecord)                             │
│    └─ insightRepository.saveAll(insights)                              │
│  COMMIT TRANSACTION                                                     │
│                                                                         │
│  If any step fails: ROLLBACK (all-or-nothing)                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  @Transactional                                                         │
│  InsightActionService.approveInsight(...)                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  BEGIN TRANSACTION                                                      │
│    ├─ insight.setStatus(APPROVED)                                      │
│    ├─ insightRepository.save(insight)                                  │
│    └─ actionLogRepository.save(log)                                    │
│  COMMIT TRANSACTION                                                     │
│                                                                         │
│  Audit trail guaranteed to be created with insight update              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 5. ENTITY RELATIONSHIPS

```
NormalizedRecordEntity (1) ──────────────── (N) DecisionInsightEntity
                                                        │
                                                        │ (1)
                                                        │
                                                        └─── (N) ActionLogEntity

Audit Tables (Envers):
  normalized_records_audit
  decision_insights_audit
  (ActionLogEntity not audited - it IS the audit log)
```

---

## 6. API REQUEST/RESPONSE FLOW

```
┌─────────────────────────────────────────────────────────────────────────┐
│ CLIENT REQUEST                                                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  POST /api/insights/123/approve                                        │
│  Content-Type: application/json                                        │
│                                                                         │
│  {                                                                      │
│    "userId": "user@example.com",                                       │
│    "reason": "Verified and acceptable"                                 │
│  }                                                                      │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ CONTROLLER PROCESSING                                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  DssController.approveInsight(123, request)                            │
│         ↓                                                               │
│  insightActionService.approveInsight(123, userId, reason)              │
│         ↓                                                               │
│  [Service updates DB]                                                  │
│         ↓                                                               │
│  Return response                                                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ SERVER RESPONSE                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  HTTP 200 OK                                                            │
│  Content-Type: application/json                                        │
│                                                                         │
│  {                                                                      │
│    "status": "approved",                                               │
│    "insightId": "123"                                                  │
│  }                                                                      │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 7. DEPLOYMENT TOPOLOGY

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CLIENT BROWSER                                  │
│  (index.html, insights.html)                                            │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓ HTTP/REST
┌─────────────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT APPLICATION                              │
│  (Port 8080)                                                            │
│  ├── DssController                                                      │
│  ├── DecisionService                                                    │
│  ├── InsightActionService                                              │
│  ├── RuleEngineImpl                                                     │
│  └── Repositories (JPA)                                                │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓ JDBC
┌─────────────────────────────────────────────────────────────────────────┐
│                      POSTGRESQL DATABASE                                │
│  (localhost:5432 or remote)                                             │
│  ├── normalized_records                                                │
│  ├── normalized_records_audit (Envers)                                 │
│  ├── decision_insights                                                 │
│  ├── decision_insights_audit (Envers)                                  │
│  └── action_logs                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Summary

This architecture provides:
- **Separation of Concerns**: Each layer has clear responsibilities
- **Extensibility**: Add rules without modifying core code
- **Auditability**: Full history via Envers
- **Transactional Safety**: Atomic operations
- **Scalability**: Stateless services, database-backed state
- **Maintainability**: Clear interfaces and patterns
