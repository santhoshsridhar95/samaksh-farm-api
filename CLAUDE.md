# Samaksh Farms Workspace - Developer & Agent Guide

This repository contains the backend REST API (`samkash-farm-api`) and is paired with the mobile/web frontend (`samaksh-farms-app`).

---

## Workspace Overview

- **Backend API Project**: `samkash-farm-api` (`c:\San Docs\workspace\samkash-farm-api`)
  - **Framework**: Spring Boot (Java 21, Spring Security, Spring Data JPA, JWT)
  - **Database**: PostgreSQL (Neon Cloud / Local) with HikariCP connection pooling
  - **API Documentation**: SpringDoc OpenAPI (`/swagger-ui.html`, `/v3/api-docs`)
  - **Timezone Standard**: `Asia/Kolkata` across JPA and Jackson configurations

- **Frontend Project**: `samaksh-farms-app` (`c:\San Docs\workspace\samaksh\samaksh-farms-app`)
  - **Framework**: React 19 + TypeScript + Vite + Capacitor 8 (Android build support)
  - **Routing & State**: React Router v7, custom API services, role-based protection
  - **UI/Animation**: Framer Motion, Lucide React icons, Responsive CSS

---

## Domain & Core Business Modules

1. **Production & Harvest**: Mushroom substrate preparation, batch lifecycle tracking (`ProductionBatch`), room transfers (`ProductionMovement`), harvesting records (`Harvest`), yield analytics.
2. **Inventory & Finished Goods**: Raw material tracking (`Inventory`), converted yield (`FinishedGoodsInventory`), stock alerts, auto-balancing.
3. **Sales & Cash Handover**: Direct customer sales (`Sale`), order management (`CustomerOrder`), payment tracking, cash handover receipts (`CashHandover`), cash ledger summaries.
4. **Employee Management**: Employee profiles, daily work contributions, work-based salary calculations.
5. **Security & User Management**: JWT authentication (`JwtService`), Google OAuth verification, user approval workflow (`ApprovalStatus`), granular entitlements (`Entitlement`), audit logging (`AuditLog`).
6. **Public Content & Reviews**: Public landing page dynamic content management (`publiccontent`), customer review submissions (`CustomerReview`) and admin moderation.
7. **Server Maintenance**: Keep-awake pinging service (`KeepAwakeService`, `/api/server/keep-awake`).

---

## Common Development Commands

### Backend (`samkash-farm-api`)
- **Build & Package**: `mvn clean package -DskipTests`
- **Run Tests**: `mvn test`
- **Run API locally**: `mvn spring-boot:run` (or run `SamkashFarmApiApplication.java` directly in IDE)

### Frontend (`samaksh-farms-app`)
- **Dev Server**: `npm run dev`
- **Build Web App**: `npm run build`
- **Build Android APK**: `npm run android:debug` or `npm run android:release`
- **Sync Capacitor**: `npm run cap:sync`

---

## Code & Architectural Standards

- **Java Package Structure**: Domain-driven subpackages under `com.samaksh.farms.<domain>` (e.g., `controller`, `dto`, `entity`, `repo`, `service`).
- **DTO Conversions**: Explicit DTO mappings for standard request/response validation.
- **Security**: Endpoint endpoints guarded via Spring Security and JWT headers (`Authorization: Bearer <token>`).
- **Commits**: End git commit messages with:
  `Co-Authored-By: Claude Code <noreply@anthropic.com>`

---

## Guidelines for Custom Agents & Skills

When building subagents (`.claude/agents/*.md`) or custom skills (`.claude/skills/`) for this workspace:
- **Backend Agent (`backend-dev`)**: Focuses on `src/main/java/com/samaksh/farms/`, ensuring Spring Data JPA queries, DTO validations, security checks, and transaction management adhere to domain patterns.
- **Frontend Agent (`frontend-dev`)**: Focuses on `c:\San Docs\workspace\samaksh\samaksh-farms-app\src\`, handling React components, Vite build scripts, API service wrappers, and mobile Capacitor sync.
- **Fullstack / Audit Agent (`farm-auditor`)**: Cross-verifies database schemas, DTO structures, REST endpoints, and UI consumption across both projects.
