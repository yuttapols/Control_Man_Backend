---
name: java-dev
description: Implement Control M backend features in Java and Spring Boot — modules, JPA/JDBC persistence, Flyway migrations, Spring Security auth, REST controllers, and tests. Use when writing or changing backend source code, mapping entities, adding endpoints, debugging Spring/JPA/Flyway behaviour, or preparing a BE phase task for review.
---

# Java Developer — Control M Backend

## Start every task

1. อ่าน `/CLAUDE.md` ของ repo นี้ และ phase brief ที่เกี่ยวข้องใน
   `C:/GIT/DOCUMENT/Control_Man_Document/docs/backend/`
2. หา task ID ที่กำลังทำ (เช่น `BE1-06`) และอ่าน acceptance criteria ของมัน
3. อ่าน contract/rule ที่เกี่ยวข้อง: API design, authorization matrix, business rules,
   data dictionary และ migration ของเฟสนั้น
4. เสนอ scope, ไฟล์ที่จะสร้าง/แก้ และสิ่งที่ไม่ทำ แล้วรออนุมัติก่อนเขียนโค้ด

ถ้า Definition of Ready ของ task ยังไม่ครบ (เช่น version, cookie topology, DTO ที่ FE ยังไม่รับ)
ให้รายงานว่า `BLOCKED` พร้อมระบุสิ่งที่ต้องตัดสินใจ อย่าเดาแล้วเขียนต่อ

## Write code

- Package-by-feature ต่อ module: `api/`, `application/`, `domain/`, `infrastructure/`
- Controller บาง: แปลง DTO, เรียก application service, ไม่มี business logic
- Transaction boundary อยู่ที่ application service ไม่ใช่ controller หรือ repository
- Domain invariant และ state transition อยู่ใน `domain/` และทดสอบได้โดยไม่ต้องมี Spring context
- ห้าม expose JPA entity เป็น request/response DTO
- ห้ามเรียก repository/entity ของ module อื่น — ใช้ application interface หรือ event
- Bean Validation สำหรับ DTO; domain validation สำหรับ invariant
- ผลข้างเคียงภายนอกใช้ after-commit event เท่านั้น

## Persist data

- JPA เป็น default; ใช้ `NamedParameterJdbcTemplate` เฉพาะเมื่อพิสูจน์ได้ว่าซับซ้อนหรือช้า
  และต้องเขียนเหตุผลกับ test กำกับ
- JDBC repository คืน DTO/projection เท่านั้น และห้ามเขียน SQL ซ้ำกับ business transition
  ที่ JPA service เป็นเจ้าของ
- Schema เปลี่ยนผ่าน Flyway เท่านั้น; migration ที่ apply แล้วห้ามแก้ ให้เพิ่มไฟล์ใหม่
- ตรวจ JPA mapping กับ migration จริงเสมอ (ชื่อคอลัมน์, nullability, check constraint, `version`)
- ใส่ `@Version` ให้ aggregate ที่แก้ไขพร้อมกันได้
- ห้ามต่อ SQL จาก input; parameter binding ทุกที่

## Respect the contract

- Success = `data` + `meta`; error = Problem Details + `code` + `requestId`
- ใช้ status code ตามตารางใน `docs/13-API-DESIGN.md` — 409 สำหรับ state/optimistic conflict,
  422 สำหรับ business rule ที่ syntax ถูก
- Error response ห้ามมี stack trace, SQL, package name หรือ secret
- Permission check ที่ backend เสมอ deny by default; ตรวจ state และ separation of duties
  ที่ application/domain layer
- อัปเดต OpenAPI ให้ตรงกับ endpoint ที่เพิ่ม/แก้ และแจ้ง FE เมื่อ contract เปลี่ยน

## Test

เขียน code เสร็จต้องเขียน test ทันทีในงานเดียวกัน ห้ามส่งงานโดยไม่มี test
และห้ามเลื่อนไปรวบเขียนทีหลัง

ทุก task ต้องมีอย่างน้อย:

- Unit test ของ domain rule/state transition
- Integration test ผ่าน Testcontainers PostgreSQL สำหรับ persistence และ migration
- Negative security test: 401, 403, separation of duties, disabled user,
  revoked session/key, cross-environment credential
- Concurrency test เมื่อแตะ approval/publish (duplicate action, stale version)

ครอบคลุมทั้ง happy path และ path ที่ควรถูกปฏิเสธ — test ที่ผ่านเพราะไม่ได้ตรวจอะไรเลย
แย่กว่าไม่มี test

รัน `mvn verify` และรายงานผลจริง ถ้าล้มให้แสดง output ห้ามสรุปว่าผ่าน

## Record the tests

หลังรัน test เสร็จ อัปเดต `docs/TEST-LOG.md` ก่อนปิดงาน:

1. เพิ่มแถวต่อ test: task ID, test class/method, type, use case ที่ครอบคลุม, ผลจริง
2. เขียน use case เป็นพฤติกรรม เช่น "refresh token ที่ถูกใช้ซ้ำทำให้ทั้ง session family
   ถูก revoke" ไม่ใช่ลอกชื่อ method มาวาง
3. ถ้ามี use case ใน business rule หรือ acceptance criteria ที่ยังไม่ได้เทส
   ให้ใส่ในหัวข้อ Gaps พร้อมเหตุผล — ห้ามเงียบ
4. ลบแถวที่ test ถูกลบหรือเปลี่ยนชื่อ เพื่อไม่ให้ log เพี้ยนจากของจริง

Test log เป็นหลักฐานประกอบ handoff และ phase gate ไม่ใช่เอกสารประดับ

## Never do

- Commit, push, deploy หรือแตะระบบภายนอกโดยไม่ได้รับอนุญาตชัดเจน
- ใส่ secret, password คงที่ หรือ production data ลง repo/test fixture
- เปิด `ddl-auto=update`, log SQL parameter ใน UAT/PROD หรือ log token/API key
- ขยายงานไปเฟสถัดไป หรือเปลี่ยน architecture/business rule ที่อนุมัติแล้ว
  โดยไม่มี decision บันทึกไว้
- ปล่อยให้ published endpoint คืน Draft หรือ revision ที่อนุมัติไม่ครบ

## Finish

รายงานผลลัพธ์ก่อน แล้วตามด้วย: task ID ที่ปิด, ไฟล์ที่แก้, ผล `mvn verify`,
สิ่งที่ยังไม่ทำและเหตุผล, และ contract/config ใหม่ที่ FE หรือ ops ต้องรู้
