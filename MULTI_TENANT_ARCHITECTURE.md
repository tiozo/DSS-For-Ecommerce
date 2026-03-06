# Multi-Tenant SaaS Architecture for DSS-For-Ecommerce

## Overview
This document outlines the multi-tenant SaaS refactoring of the DSS-Core project, enabling isolated data and rules per tenant.

## 1. MULTI-TENANCY IMPLEMENTATION

### TenantContext (ThreadLocal-based)
- **Location**: `com.dss.core.tenant.TenantContext`
- **Purpose**: Thread-safe tenant isolation using ThreadLocal
- **Usage**: 
  ```java
  TenantContext.setTenantId("tenant-123");
  String tenantId = TenantContext.getTenantId();
  TenantContext.clear();
  ```

### TenantFilter
- **Location**: `com.dss.core.tenant.TenantFilter`
- **Purpose**: Extracts `X-Tenant-ID` header from HTTP requests and sets TenantContext
- **Behavior**: Automatically clears context after request processing

### TenantAspect
- **Location**: `com.dss.core.tenant.TenantAspect`
- **Purpose**: AOP-based automatic tenant_id injection into repository queries
- **Scope**: All repository `find*` methods

### Database Schema Changes
All entities now include `tenant_id` column:
- `NormalizedRecordEntity`: Added `tenant_id` (non-null, indexed)
- `DecisionInsightEntity`: Added `tenant_id` (non-null, indexed)
- `DecisionActionEntity`: Added `tenant_id` (non-null, indexed)
- `DynamicRuleEntity`: Added `tenant_id` (non-null, indexed)

**Composite Indexes**:
- `idx_tenant_record`: (tenant_id, record_id) on normalized_records
- `idx_action_tenant_id`: (tenant_id) on decision_actions
- `idx_insight_tenant_id`: (tenant_id) on decision_insights
- `idx_rule_tenant_id`: (tenant_id) on dynamic_rules

## 2. DYNAMIC RULES (JSON-BASED)

### DynamicRuleEntity
- **Location**: `com.dss.core.persistence.entity.DynamicRuleEntity`
- **Fields**:
  - `name`: Rule identifier
  - `expression`: SpEL expression (e.g., `#QUANTITYORDERED > 50`)
  - `description`: Human-readable description
  - `enabled`: Boolean flag for rule activation
  - `actionPayload`: JSON payload for triggered actions

### SpELRuleEvaluator
- **Location**: `com.dss.core.decision.rule.SpELRuleEvaluator`
- **Purpose**: Evaluates SpEL expressions against normalized record data
- **Example**:
  ```java
  boolean matches = evaluator.evaluate(rule, record);
  ```

### RuleController
- **Location**: `com.dss.core.api.RuleController`
- **Endpoints**:
  - `POST /api/rules`: Create rule
  - `GET /api/rules`: List all rules for tenant
  - `GET /api/rules/enabled`: List enabled rules
  - `GET /api/rules/{id}`: Get specific rule
  - `PUT /api/rules/{id}`: Update rule
  - `DELETE /api/rules/{id}`: Delete rule

**Example Rule Creation**:
```json
{
  "name": "High Volume Alert",
  "expression": "#QUANTITYORDERED > 50 && #SALES > 5000",
  "description": "Alert when quantity ordered exceeds 50 and sales exceed 5000",
  "enabled": true,
  "actionPayload": "{\"action\": \"UP_PRICE\", \"percentage\": 10}"
}
```

## 3. ACTION WORKFLOWS

### DecisionActionEntity
- **Location**: `com.dss.core.persistence.entity.DecisionActionEntity`
- **Fields**:
  - `actionType`: UP_PRICE, LOGISTICS_OVERLOAD, CUSTOM
  - `status`: PENDING, APPROVED, REJECTED, EXECUTED
  - `payload`: JSON action parameters
  - `reason`: User-provided justification

### ActionController
- **Location**: `com.dss.core.api.ActionController`
- **Endpoints**:
  - `POST /api/actions`: Create action (status=PENDING)
  - `GET /api/actions/pending`: List pending actions
  - `GET /api/actions/record/{recordId}`: Get actions for record
  - `PUT /api/actions/{id}/approve`: Approve action
  - `PUT /api/actions/{id}/reject`: Reject action

**Example Action Creation**:
```json
{
  "recordId": 123,
  "actionType": "UP_PRICE",
  "payload": "{\"percentage\": 15}",
  "reason": "High demand detected"
}
```

## 4. REQUEST FLOW WITH MULTI-TENANCY

1. **Client Request**: Includes `X-Tenant-ID` header
2. **TenantFilter**: Extracts tenant_id and sets TenantContext
3. **Controller**: Processes request (tenant_id available via TenantContext)
4. **Service Layer**: Uses TenantContext for business logic
5. **Repository Layer**: TenantAspect injects tenant_id into queries
6. **Database**: Returns only tenant-scoped data
7. **Response**: TenantContext cleared after request

## 5. USAGE EXAMPLES

### Creating a Rule
```bash
curl -X POST http://localhost:8080/api/rules \
  -H "X-Tenant-ID: tenant-123" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Price Surge Rule",
    "expression": "#SALES > 10000",
    "enabled": true
  }'
```

### Creating an Action
```bash
curl -X POST http://localhost:8080/api/actions \
  -H "X-Tenant-ID: tenant-123" \
  -H "Content-Type: application/json" \
  -d '{
    "recordId": 456,
    "actionType": "UP_PRICE",
    "reason": "Inventory low"
  }'
```

### Uploading CSV (with tenant isolation)
```bash
curl -X POST http://localhost:8080/api/upload-csv \
  -H "X-Tenant-ID: tenant-123" \
  -F "file=@sales.csv"
```

## 6. SECURITY CONSIDERATIONS

- **Tenant Isolation**: All queries automatically scoped to tenant_id
- **Header Validation**: Implement authentication middleware to validate X-Tenant-ID
- **Row-Level Security**: Database constraints ensure tenant_id cannot be bypassed
- **Audit Trail**: ActionLogEntity tracks all user decisions per tenant

## 7. MIGRATION NOTES

- Existing records require `tenant_id` population (backfill migration)
- Update all repository calls to include tenant_id parameter
- Remove unique constraint on `recordId` (now composite: tenant_id + recordId)
- Add database migration scripts for schema changes

## 8. FUTURE ENHANCEMENTS

- [ ] Tenant provisioning API
- [ ] Usage metrics per tenant
- [ ] Custom rule templates per industry
- [ ] Webhook support for action execution
- [ ] Rule versioning and rollback
- [ ] Batch action execution
