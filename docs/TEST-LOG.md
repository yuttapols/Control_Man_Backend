# Test Log

บันทึกว่าเทสอะไรไปบ้าง และแต่ละ test ครอบคลุม use case ไหน
อัปเดตทุกครั้งที่เขียนหรือแก้ test ก่อนปิดงาน — ห้ามค้างไว้ท้ายเฟส

Type: `unit` | `integration` | `security` | `concurrency`
Result: ผลจริงจากการรันครั้งล่าสุด (`PASS` / `FAIL` / `SKIPPED` พร้อมเหตุผล)

## BE Phase 1 — Foundation, Database & Authentication

รันล่าสุด: 2026-08-15
· `./mvnw verify` → **Tests run: 27, Failures: 0, Errors: 0 — BUILD SUCCESS**
· `./mvnw verify -Pdb` → **Tests run: 31, Failures: 0, Errors: 0 — BUILD SUCCESS** (PostgreSQL local พร้อมแล้วที่ `[::1]:5432/control_m`)

Test ที่ต้องใช้ PostgreSQL ถูก tag ว่า `db` และถูกกันออกจากการรันปกติ
รันชุดเต็มที่ต่อ DB จริงด้วย:

```text
./mvnw verify -Pdb
```

| Task | Test | Type | Use case | Result |
|---|---|---|---|---|
| BE1-01 | `ControlMBackendApplicationTests#contextLoads` | integration | application context ขึ้นได้ด้วย profile `local` และ bean ทั้งหมด wire สำเร็จ | PASS |
| BE1-02 | `SecurityConfigTest#healthProbeIsPublic` | security | health probe เรียกได้โดยไม่ต้อง authenticate เพื่อให้ platform ตรวจสถานะได้ | PASS |
| BE1-02 | `SecurityConfigTest#nonHealthActuatorEndpointsAreNotPublic` | security | actuator endpoint อื่นนอกจาก health ไม่ถูกเปิดสาธารณะ (`/actuator/env` ต้องไม่ได้ 200) | PASS |
| BE1-02 | `SecurityConfigTest#openApiDocumentIsAvailableAndDeclaresBothSecuritySchemes` | integration | OpenAPI document เปิดอ่านได้ และประกาศ security scheme ทั้ง Portal JWT และ consumer API key | PASS |
| BE1-03 | `SchemaMigrationTest#v1IsRecordedAsSuccessfullyApplied` | integration | Flyway บันทึกว่า V1 apply สำเร็จ และไม่มี migration ที่ล้มเหลวค้างอยู่ | PASS |
| BE1-03 | `SchemaMigrationTest#allV1TablesExist` | integration | ตารางทั้ง 9 ของ identity/RBAC schema ถูกสร้างครบตาม V1 | PASS |
| BE1-03 | `SchemaMigrationTest#pgcryptoExtensionIsInstalled` | integration | `pgcrypto` ติดตั้งแล้ว ทำให้ `gen_random_uuid()` ใช้เป็น default ของ PK ได้ | PASS |
| BE1-03 | `SchemaMigrationTest#appUserHasCaseInsensitiveUniqueIndexes` | integration | username/email ของ `app_user` ซ้ำแบบไม่สนตัวพิมพ์ไม่ได้ ตามกฎ normalize ในเอกสาร | PASS |
| BE1-03 | `SchemaMigrationTest#lockableTablesHaveVersionColumn` | integration | ตารางที่ต้องรองรับ optimistic locking มีคอลัมน์ `version` | PASS |
| BE1-03 | `SchemaMigrationTest#timestampColumnsAreTimezoneAware` | integration | คอลัมน์เวลาของ schema เราเป็น `timestamptz` ทั้งหมด (ไม่รวมตาราง `flyway_schema_history` ของ Flyway) | PASS |
| BE1-04 | `IdentityPersistenceMappingTest#userLevelPersistsAndBumpsVersionOnUpdate` | integration | `user_level` persist ได้ id/created_at ที่ DB เติมให้ และคอลัมน์ `version` เพิ่มขึ้นเมื่อแก้ไข (optimistic locking ทำงานจริง) | PASS |
| BE1-04 | `IdentityPersistenceMappingTest#appUserRoundTripsMappedColumns` | integration | `app_user` map ครบทุกคอลัมน์รวม enum `status` และ FK `user_level_id` โหลดกลับได้ตรง | PASS |
| BE1-04 | `IdentityPersistenceMappingTest#appUserLookupIsCaseInsensitive` | integration | ค้น `app_user` ด้วย username แบบไม่สนตัวพิมพ์เจอ record เดียวกัน ตามดัชนี `uq_app_user_username_ci` | PASS |
| BE1-04 | `IdentityPersistenceMappingTest#compositeKeyAssignmentsPersistAndLoad` | integration | ตาราง assignment แบบ composite key (`user_role`, `role_permission`) โหลดกลับด้วย embedded id ได้ | PASS |
| BE1-04 | `IdentityPersistenceMappingTest#authSessionStoresHashAndLooksUpByHash` | integration | `auth_session` เก็บเฉพาะ hash ของ refresh token และค้นด้วย hash เจอ ไม่เก็บ token ดิบ | PASS |
| BE1-05 | `LockoutPolicyTest#belowThresholdDoesNotLock` | unit | ยังไม่ล็อกบัญชีเมื่อจำนวนครั้งที่ล้มยังไม่ถึงเพดาน (5) | PASS |
| BE1-05 | `LockoutPolicyTest#atThresholdLocks` | unit | ล็อกบัญชีเมื่อจำนวนครั้งที่ล้มถึงเพดาน (5) | PASS |
| BE1-05 | `AuthenticateCredentialsUseCaseImplTest#correctPasswordReturnsIdentityAndResetsFailures` | unit | รหัสผ่านถูก (เทียบ hash) คืน identity, รีเซ็ตตัวนับ failed login และบันทึก last_login_at | PASS |
| BE1-05 | `AuthenticateCredentialsUseCaseImplTest#wrongPasswordThrowsGenericErrorAndCountsFailure` | security | รหัสผ่านผิดคืน 401 generic และเพิ่มตัวนับ failed login | PASS |
| BE1-05 | `AuthenticateCredentialsUseCaseImplTest#unknownUsernameFailsIdenticallyToWrongPassword` | security | username ที่ไม่มีจริงคืน error ตัวเดียวกับรหัสผิด ไม่เปิดเผยว่า user มีอยู่ (BR-SEC) | PASS |
| BE1-05 | `AuthenticateCredentialsUseCaseImplTest#inactiveAccountCannotAuthenticate` | security | บัญชีที่ไม่ ACTIVE (DISABLED) ล็อกอินไม่ได้แม้รหัสผ่านถูก | PASS |
| BE1-05 | `AuthenticateCredentialsUseCaseImplTest#lockedAccountRejectedWithoutCheckingPassword` | security | บัญชีที่ยังติด locked_until ถูกปฏิเสธก่อนตรวจรหัส ตัวนับไม่ขยับ | PASS |
| BE1-05 | `AuthenticateCredentialsUseCaseImplTest#reachingThresholdLocksTheAccount` | unit | การล้มครั้งที่ถึงเพดานตั้ง locked_until ให้บัญชีอัตโนมัติ | PASS |
| BE1-06 | `JwtAccessTokenServiceTest#issuedTokenHasRequiredClaimsAndValidSignature` | security | ออก access token แบบ RS256 ที่ตรวจลายเซ็นได้ มี issuer, subject, audience, เวลา, jti, session ID และ authorization version ครบ โดยไม่ใส่ข้อมูลผู้ใช้ที่ไม่จำเป็น | PASS |
| BE1-06 | `JwtAccessTokenServiceTest#wrongAudienceIsRejected` | security | access token ที่ออกให้ audience อื่นถูกปฏิเสธ แม้ลายเซ็นถูกต้อง | PASS |
| BE1-06 | `JwtAccessTokenServiceTest#expiredTokenIsRejected` | security | access token ที่หมดอายุถูกปฏิเสธ | PASS |
| BE1-06 | `JwtAccessTokenServiceTest#nonPositiveTtlIsRejected` | unit | config ปฏิเสธอายุ access token ที่เป็นศูนย์หรือติดลบ | PASS |
| BE1-09 | `SecurityConfigTest#unauthenticatedRequestIsRejectedWith401` | security | คำขอที่ไม่มี identity ไปยัง path ที่ไม่ได้เปิดสาธารณะ ได้ 401 ไม่ใช่ redirect ไปหน้า login | PASS |
| BE1-10 | `RequestIdFilterTest#generatesAndReturnsRequestId` | unit | ทุก request ได้ request id และส่งกลับใน response header ให้ผู้ใช้อ้างอิงตอนแจ้งปัญหา | PASS |
| BE1-10 | `RequestIdFilterTest#reusesCallerSuppliedRequestId` | unit | request id ที่ client ส่งมาถูกใช้ต่อ เพื่อ trace ข้ามระบบได้ | PASS |
| BE1-10 | `RequestIdFilterTest#rejectsUnsafeRequestIdFromClient` | security | request id ที่มีอักขระอันตราย (ขึ้นบรรทัดใหม่) ถูกทิ้งและสร้างใหม่ ป้องกัน log injection | PASS |
| BE1-10 | `RequestIdFilterTest#rejectsOverlongRequestId` | security | request id ยาวเกิน 64 ตัวถูกปฏิเสธ ไม่ให้ client ยัดข้อมูลขนาดใหญ่เข้า log | PASS |
| BE1-10 | `RequestIdFilterTest#clearsRequestIdAfterRequest` | unit | request id ถูกล้างจาก MDC เมื่อจบ request ไม่ปนกับ request ถัดไปของ thread เดิม | PASS |
| BE1-10 | `RequestIdFilterTest#clearsRequestIdEvenWhenChainThrows` | unit | request id ถูกล้างแม้ request ล้มเหลวกลางทาง | PASS |
| BE1-10 | `GlobalExceptionHandlerTest#businessRuleViolationReturns422` | unit | business rule ที่ถูกละเมิดคืน 422 พร้อม `code` ที่ FE ใช้เลือกข้อความ | PASS |
| BE1-10 | `GlobalExceptionHandlerTest#stateConflictReturns409` | unit | การกระทำที่ขัดกับ state ปัจจุบันคืน 409 ไม่ใช่ 400 | PASS |
| BE1-10 | `GlobalExceptionHandlerTest#optimisticLockReturns409` | unit | แก้ข้อมูลที่ถูกคนอื่นแก้ไปแล้วคืน 409 optimistic lock พร้อมบอกให้โหลดใหม่ | PASS |
| BE1-10 | `GlobalExceptionHandlerTest#notFoundReturns404` | security | resource ที่ไม่มีและที่ต้องซ่อนคืน 404 เหมือนกัน ไม่บอกใบ้ว่ามีอยู่จริง | PASS |
| BE1-10 | `GlobalExceptionHandlerTest#accessDeniedReturns403` | security | ผู้ใช้ที่ยืนยันตัวตนแล้วแต่ไม่มีสิทธิ์คืน 403 แยกจาก 401 | PASS |
| BE1-10 | `GlobalExceptionHandlerTest#authenticationFailureReturnsGeneric401` | security | authentication ล้มเหลวคืน 401 ข้อความกลาง ไม่เปิดเผยว่า username มีอยู่จริง (BR-SEC) | PASS |
| BE1-10 | `GlobalExceptionHandlerTest#dataIntegrityViolationDoesNotLeakDatabaseDetail` | security | error จาก DB ไม่รั่วชื่อ constraint หรือ SQL ออกไปหา client | PASS |
| BE1-10 | `GlobalExceptionHandlerTest#unexpectedExceptionIsSanitised` | security | exception ที่ไม่คาดคิดคืน 500 โดยไม่มี stack trace, ชื่อ class ภายใน หรือ connection string หลุด | PASS |
| BE1-10 | `GlobalExceptionHandlerTest#everyErrorCarriesTheRequestId` | unit | ทุก error response แนบ `requestId` ตัวเดียวกับที่อยู่ใน response header | PASS |

## BE Phase 2 — Holiday Domain & Published API

| Task | Test | Type | Use case | Result |
|---|---|---|---|---|
| _(ยังไม่เริ่ม)_ | | | | |

## BE Phase 3 — Approval & Emergency Workflow

| Task | Test | Type | Use case | Result |
|---|---|---|---|---|
| _(ยังไม่เริ่ม)_ | | | | |

## BE Phase 4 — API Consumer, Audit & Production Readiness

| Task | Test | Type | Use case | Result |
|---|---|---|---|---|
| _(ยังไม่เริ่ม)_ | | | | |

## Gaps

Use case จาก business rule หรือ acceptance criteria ที่ยังไม่มี test ครอบคลุม
พร้อมเหตุผลและแผนที่จะปิด

| Task | Use case ที่ยังไม่ครอบคลุม | เหตุผล | แผนปิด |
|---|---|---|---|
| BE1-07/08 | refresh rotation + login/refresh/logout/me endpoints | refresh cookie topology ยังต้อง match FE deployment | ตัดสิน cookie topology กับ FE ก่อนเริ่ม endpoint |
| BE1-09 · 11 · 12 | permission authorization, health/logging, security negative tests | ยังทำไม่ครบตาม acceptance ของแต่ละ task | ทำต่อตามลำดับ task |
| ทุก task | Testcontainers isolation | ไม่มี Docker ในเครื่อง จึงยังใช้ DB จริงเป็น test database | รอการตัดสินใจเรื่อง Docker |
| — | Reference seed `R__reference_data.sql` / `R__standard_approval_workflow.sql` | ทั้งสองไฟล์ insert ลงตารางของ V2/V3 จึงรันที่ Phase 1 ไม่ได้ | ต้องแยก seed ตามเฟสใน document repo ก่อนใช้ |

## หมายเหตุการเขียน

เขียน use case เป็นพฤติกรรมที่อ่านแล้วเข้าใจ ไม่ใช่ลอกชื่อ method มาวาง
และอัปเดต Result ให้ตรงกับผลรันล่าสุดจริงเสมอ
