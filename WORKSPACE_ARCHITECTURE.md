# Workspace Architecture & Blueprint Guide

This document provides a full reference model of the Samaksh Farms management system workspace, covering both backend and frontend architectures, domain entities, REST API structure, build workflows, and blueprints for building custom Claude Code Agents and Skills.

---

## 1. System Architecture Overview

```
                      +-----------------------------------+
                      |   Public Website & Mobile App     |
                      |      (samaksh-farms-app)          |
                      |  React 19 + TypeScript + Vite     |
                      |  Capacitor 8 (Android Build)      |
                      +-----------------+-----------------+
                                        |
                                   REST / JSON
                                        |
                      +-----------------v-----------------+
                      |          Backend API              |
                      |     (samkash-farm-api)            |
                      |  Spring Boot 3.4 / Java 21        |
                      +-----------------+-----------------+
                                        |
                            JPA / Hibernate / PostgreSQL
                                        |
                      +-----------------v-----------------+
                      |       PostgreSQL Database         |
                      |   (Neon Cloud / Local Instance)   |
                      +-----------------------------------+
```

---

## 2. Backend Architecture (`samkash-farm-api`)

### Tech Stack
- **Language**: Java 21 (Lombok, Jackson)
- **Framework**: Spring Boot 3.4 (WebMVC, Data JPA, Security, Mail, Validation)
- **Database Driver**: PostgreSQL (HikariCP pool management, Asia/Kolkata timezone)
- **Authentication**: JWT (`jjwt 0.12.5`), Google OAuth token verifier (`GoogleTokenVerifier`)
- **API Spec**: SpringDoc OpenAPI (`v2.8.9`)

### Module Breakdown (`com.samaksh.farms.*`)
1. **`analytics`**: Batch yield calculation, inventory replenishment alerts (`InventoryAlertController`, `BatchAnalyticsService`).
2. **`audit`**: Centralized audit logging (`AuditLogRepository`, `AuditController`).
3. **`auth`**: User registration, email verification, login, JWT token generation/validation, password reset.
4. **`cashhandover`**: Operator-to-Manager daily cash handovers, ledger balancing, collection summaries.
5. **`customer`**: Customer directory & order association.
6. **`dashboard`**: Consolidated farm KPIs, inventory balance summaries, daily revenues.
7. **`employee`**: Employee profiles, daily work contributions, work unit logging, automated salary processing.
8. **`entitlement`**: Feature flags & permission matrix (`EntitlementController`, `EntitlementRepository`).
9. **`finishedgoods`**: Post-harvest inventory tracking by product type and batch.
10. **`harvest`**: Harvest weight logging per room/batch, quality grading.
11. **`inventory`**: Raw material stock management, stock adjustments, minimum threshold tracking.
12. **`order`**: Customer order management, status updates (`OrderStatus`).
13. **`production`**: Production batch lifecycle (Preparation -> Incubation -> Fruiting -> Completed).
14. **`productionmovement`**: Physical movement of substrate/batches across rooms.
15. **`products`**: Farm product catalog (Fresh Mushrooms, Value-added products, Spawns) & pricing.
16. **`publiccontent`**: Dynamic landing page configuration (Hero, Products, Contact info).
17. **`review`**: Customer review submissions and moderation workflows.
18. **`sale`**: Direct sales transactions, invoice generation, payment status updates.
19. **`server`**: Keep-alive ping mechanism to maintain cloud backend availability.
20. **`user`**: User roles (`SUPER_ADMIN`, `ADMIN`, `MANAGER`, `OPERATOR`, `VIEWER`), account approval (`PENDING`, `APPROVED`, `REJECTED`).

---

## 3. Frontend Architecture (`samaksh-farms-app`)

### Tech Stack
- **Framework**: React 19 + TypeScript + Vite 8
- **Mobile Runtime**: Capacitor 8 for Android compilation (`gradlew.bat assembleDebug / bundleRelease`)
- **Key Packages**: `axios`, `framer-motion`, `lucide-react`, `react-router-dom` v7

### Page & Component Hierarchy
- **Public Site**: `Homepage.tsx`, `Hero.tsx`, `Products.tsx`, `Reviews.tsx`, `Contact.tsx`, `OrderForm.tsx`, `ReviewSubmission.tsx`
- **Admin Portal**:
  - `AdminDashboard.tsx` & `DashBoardPage.tsx`: KPI cards, revenue summaries, alerts.
  - `ProductionPage.tsx` & `RoomTransferPage.tsx`: Substrate & batch lifecycle tracking.
  - `HarvestPage.tsx` & `InventoryPage.tsx`: Daily yield inputs, finished goods & raw materials.
  - `SalesPage.tsx` & `DirectSalesPage.tsx`: Sales recording, forecasting, simulation.
  - `MoneyPage.tsx`: Cash handover logs, ledger reconciliation, sourcing profit analysis.
  - `EmployeesPage.tsx`: Employee work logs and salary calculations.
  - `UserManagementPage.tsx` & `AuditTrailPage.tsx`: Account approvals, role updates, audit logs.
  - `ServerKeepAlivePage.tsx`: Server heartbeat monitoring & configuration.

---

## 4. Blueprint for Agents & Skills

This workspace structure provides the basis for building custom agents (`.claude/agents/*.md`) and skills (`.claude/skills/`).

### Agent Blueprint 1: Backend Developer Subagent (`.claude/agents/backend-developer.md`)
- **Scope**: Java 21, Spring Boot REST controllers, Services, Repositories, JPA Entities, Security.
- **Responsibilities**: Feature additions, DTO updates, controller endpoints, unit tests (`src/test/java`).

### Agent Blueprint 2: Frontend Developer Subagent (`.claude/agents/frontend-developer.md`)
- **Scope**: React 19, TypeScript, Vite, Tailwind/CSS, Capacitor Android sync.
- **Responsibilities**: UI component updates, state management, API service integrations (`src/admin/services/api.ts`).

### Agent Blueprint 3: Fullstack QA & Audit Subagent (`.claude/agents/fullstack-auditor.md`)
- **Scope**: Cross-system endpoint validation, API contract compliance, permission enforcement, entity synchronization.

### Skill Blueprint: Farm Module Generator (`.claude/skills/create-farm-module`)
- **Purpose**: Generates boilerplate Spring Boot domain package (Entity, DTOs, Repo, Service, Controller) and matching React Admin Page component.

---

## 5. Quick Verification Commands

```bash
# Build backend without running tests
mvn clean package -DskipTests

# Run unit test suite
mvn test

# Run frontend build
cd ../samaksh/samaksh-farms-app
npm run build
```
