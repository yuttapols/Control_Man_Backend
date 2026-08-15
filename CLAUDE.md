# CLAUDE.md — Control M Backend

Backend repository ของระบบ **Thai Holiday Control** (ศูนย์กลางวันหยุดราชการ/ธนาคารไทย
เผยแพร่ให้ระบบอื่นผ่าน Web Service)

## Repository state

- ยังไม่มี source code ใน repo นี้ (ไม่มี commit, ไม่มี `pom.xml`)
- งานทั้งหมดเริ่มจาก **BE Phase 1** ซึ่งสถานะปัจจุบันคือ `NOT_STARTED`
- Design ทั้ง 3 เฟสปิดแล้ว/รอตรวจรับ — ห้ามออกแบบใหม่ทับของเดิมโดยไม่บันทึก decision

## Source of truth

เอกสารทั้งหมดอยู่คนละ repo: `C:/GIT/DOCUMENT/Control_Man_Document`
อ่านจากที่นั่นเสมอ อย่าคัดลอกเนื้อหามาไว้ในไฟล์นี้ให้เกิดสองแหล่งความจริง

| ต้องการ | ไฟล์ |
|---|---|
| กติกาการทำงานร่วมกัน | `AGENTS.md` |
| สถานะล่าสุด / decision | `docs/CONTEXT-SUMMARY.md`, `docs/DECISION-LOG.md` |
| งานของเฟสที่ทำอยู่ | `docs/backend/BE-PHASE-{1..4}-*.md` |
| API contract / error contract | `docs/13-API-DESIGN.md` |
| Auth / RBAC / security | `docs/14-AUTHENTICATION.md`, `docs/15-AUTHORIZATION.md`, `docs/16-SECURITY-DESIGN.md` |
| Module boundary / architecture | `docs/17-SYSTEM-ARCHITECTURE.md` |
| Business rules (BR-*) | `docs/05-BUSINESS-RULES.md` |
| Schema / JPA-JDBC boundary | `docs/11-DATABASE-DESIGN.md`, `docs/12-DATA-DICTIONARY.md` |
| Flyway SQL | `database/migration/V1..V4`, `database/seed/`, `docs/23-DATABASE-MIGRATION-PLAN.md` |
| ข้อตกลงกับ FE | `docs/22-INTEGRATION-MATRIX.md` |
| Config keys / deploy / rollback | `docs/18-ENVIRONMENT-DEPLOYMENT.md` |
| Performance/retention target | `docs/19-NON-FUNCTIONAL-REQUIREMENTS.md` |

## Working agreement

ตาม `AGENTS.md` ของโครงการ — ทุกงานใหม่:

1. เสนอแนวทาง scope, assumptions, ไฟล์ที่กระทบ และสิ่งที่ไม่ทำ
2. รอ user อนุมัติ **ก่อน** แก้ไฟล์
3. ทำเฉพาะ scope ที่อนุมัติ ห้ามล้ำไปเฟสถัดไป
4. ปิดงานด้วยสรุปสั้น และอัปเดต `docs/CONTEXT-SUMMARY.md` / `docs/DECISION-LOG.md`
   ใน document repo เมื่อมี decision ที่เปลี่ยนสาระ

Status values: `NOT_STARTED`, `READY`, `IN_PROGRESS`, `BLOCKED`, `READY_FOR_REVIEW`, `DONE`

## Tech stack (approved direction)

- Java + Spring Boot, Maven, modular monolith เดียวต่อ environment
- Spring Data JPA เป็น default; `NamedParameterJdbcTemplate` เฉพาะ read projection/report
  ที่มีเหตุผลและมี test
- PostgreSQL + Flyway (`pgcrypto`, `gen_random_uuid()`, `timestamptz`, holiday date = `date`)
- Spring Security + JWT access token อายุสั้น (10–15 นาที) + opaque rotating refresh token
- Springdoc OpenAPI
- Testcontainers PostgreSQL สำหรับ integration test

Java/Spring Boot version ที่แน่นอนเป็น Definition of Ready ของ BE1 ที่ยังไม่ตัดสินใจ —
ต้องถาม user ก่อน generate `pom.xml`

## Module structure

Package-by-feature ต่อ module:

```text
holiday/
  api/             HTTP DTO/controller
  application/     use cases and transactions
  domain/          rules and state transitions
  infrastructure/  JPA/JDBC adapters
```

Modules: `auth`, `iam`, `holiday`, `approval`, `emergency`, `consumer`, `audit`, `config`

- Module อื่นเรียกผ่าน application interface/event ที่ประกาศไว้เท่านั้น
- ห้ามเข้าถึง repository/entity ภายใน module อื่นโดยตรง
- JPA entity ห้ามถูกส่งออกเป็น API DTO
- `shared` มีเฉพาะ cross-cutting primitives ที่เสถียร ไม่ใช่ถังขยะ utility

## API contract rules

Base paths:

```text
/api/v1/portal/...     JWT Bearer (Angular Portal)
/api/v1/holidays/...   X-API-Key (consumers, published only)
/actuator/health       restricted
```

- Success: `data` + `meta` (list เพิ่ม `page` ใน `meta`)
- Error: Problem Details + `code`, `requestId`, `errors[]` (ห้ามมี stack trace/SQL/secret)
- Status: 400 validation, 401 no/invalid identity, 403 policy denied, 404 not found/hidden,
  409 state conflict/optimistic lock, 422 business rule, 429 rate limit, 503 dependency down
- Transition ใช้ action path เช่น `POST /portal/approval-requests/{id}/approve`
- Published GET รองรับ `ETag`/`If-None-Match`/`Last-Modified`; Portal update ใช้ `If-Match`
- Credential issue/rotate และ emergency publish รองรับ `Idempotency-Key`
- Date เป็น ISO-8601 `YYYY-MM-DD`, ค.ศ. เท่านั้นใน API (พ.ศ. เป็นเรื่อง display)
- `year` ใช้ร่วมกับ `from/to` ไม่ได้; `size` ต้องมีเพดาน
- BE เป็นเจ้าของ OpenAPI contract และ error code; breaking change หลัง FE เริ่มแล้ว
  ต้อง impact review + อนุมัติ

## Security rules (non-negotiable)

- Permission บังคับที่ backend เสมอ; FE guard เป็น UX เท่านั้น — deny by default
- Permission naming `module.resource.action` เช่น `holiday.revision.submit`
- Separation of duties: creator ≠ L1 actor ≠ L2 actor, และ emergency requester ≠ approver
  โดย Super Admin **ไม่มี** ข้อยกเว้น
- Published API ต้องไม่คืน Draft หรือ revision ที่อนุมัติไม่ครบ แม้เกิด partial failure
- Password ใช้ adaptive hash; refresh token และ API credential เก็บเฉพาะ hash
- API key ห้ามอยู่ใน query string; แยกตาม DEV/UAT/PROD และใช้ข้ามกันไม่ได้
- ห้าม secret ใน Git, `application-*.yml`, log หรือ audit — inject จาก environment
- ห้ามต่อ SQL จาก input; ใช้ parameter binding ทุกที่
- Redact `Authorization`, `Cookie`, `X-API-Key`, password ใน log; audit ต้อง redact ก่อนบันทึก
- ห้ามเปิดเผยว่า username มีอยู่จริงจาก error ของ login

## Persistence rules

- Schema เปลี่ยนผ่าน Flyway เท่านั้น; ห้าม `ddl-auto=update` ใน UAT/PROD
- Versioned migration ที่ apply แล้ว immutable — แก้ด้วย migration ใหม่
- Optimistic locking (`version`) กับ `app_user`, `holiday`, `holiday_revision`,
  `approval_request`, `api_environment_access`
- Approval state change + action record ต้องอยู่ transaction เดียวกัน
- Publish revision ต้อง atomic กับการเปลี่ยน current published pointer
- Side effect ภายนอก (notification, cache invalidation) เกิดหลัง commit เท่านั้น
- JDBC repository คืน DTO/projection เท่านั้น ห้ามคืน JPA entity
- Naming ตาม migration ที่มีอยู่: snake_case, UUID PK, `created_at`/`updated_at` เป็น `timestamptz`

## Observability

- Structured JSON log + environment + request/correlation ID ทุก request
- Health แยก liveness/readiness และไม่เปิด detail แก่ public
- Metrics: request count/latency/error, DB pool, auth failure, approval aging,
  consumer rate limit, emergency events
- เวลาเก็บเป็น UTC; แสดงผลเป็น `Asia/Bangkok`

## Verification

**เขียน code เสร็จต้องมี unit test เสมอ** ไม่นับว่างานเสร็จถ้ายังไม่มี test —
ห้ามเลื่อนไปเขียนทีหลังหรือรวบไว้ท้ายเฟส

ก่อนบอกว่างานเสร็จ:

```text
mvn verify
```

ต้องผ่าน unit test, integration test (Testcontainers PostgreSQL),
module-boundary test และ security negative test (401/403/SoD/cross-environment key)
รายงานผลจริงเสมอ ถ้า test ล้มให้แสดง output ไม่ใช่สรุปว่าผ่าน

## Test log

ทุกครั้งที่เขียน test ต้องบันทึกลง [docs/TEST-LOG.md](docs/TEST-LOG.md) ว่า
**เทสอะไรไป และครอบคลุม use case ไหนบ้าง** ในตารางเดียวกัน

| คอลัมน์ | ความหมาย |
|---|---|
| Task | task ID จาก phase brief เช่น `BE1-06` |
| Test | test class/method |
| Type | `unit`, `integration`, `security`, `concurrency` |
| Use case | สิ่งที่ทดสอบ เขียนเป็นพฤติกรรมที่เข้าใจได้ ไม่ใช่ชื่อ method |
| Result | ผลจริงจากการรัน |

พร้อมระบุ use case ที่ **ยังไม่ครอบคลุม** ไว้ในหัวข้อ Gaps ของไฟล์เดียวกัน
เพื่อไม่ให้ช่องว่างหายไปเงียบ ๆ

## Phase gates

| Phase | Scope | Migration |
|---|---|---|
| BE1 | Foundation, Flyway V1, JWT/refresh rotation, RBAC, error/audit foundation | V1 |
| BE2 | Holiday aggregate/revision, Portal CRUD, Published API, business-day | V2 |
| BE3 | Approval L1/L2, SoD, atomic publish, Emergency + 3-business-day post-review | V3 |
| BE4 | API consumer/credential lifecycle, admin/audit, hardening, runbooks | V4 |

ห้ามเริ่มเฟสถัดไปก่อน Definition of Done ของเฟสปัจจุบันผ่านและ user อนุมัติ

## Boundaries

- ห้าม commit, push, deploy, ออก production credential หรือแตะระบบภายนอกโดยไม่ได้รับอนุญาต
- ห้ามแก้เอกสารใน document repo นอกเหนือจาก Context Summary/Decision Log ที่ตกลงไว้
- ห้าม duplicate business rule ไปฝั่ง Angular — FE ใช้เพื่อ UX เท่านั้น
- ห้ามใช้ production data หรือ fixed password ใน repo/test fixture

## Skills

- `.claude/skills/java-dev` — implement backend ตาม phase brief
- `.claude/skills/lead-dev` — technical lead: contract, architecture review, phase gate
- `skills/control-m-pm` (document repo) — PM/phase coordination
