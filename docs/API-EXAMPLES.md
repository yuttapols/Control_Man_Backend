# API Examples — Control M Backend

ตัวอย่าง JSON request/response ของ API ที่ implement แล้ว อ้างอิงจากโค้ดจริงใน `src/main/java`
(contract เป็นเจ้าของโดย BE — ดู `docs/13-API-DESIGN.md` ใน document repo เป็น source of truth)

> สถานะ: **BE Phase 1** — มีเฉพาะ module `auth` (4 endpoints) ใต้ `/api/v1/portal/auth`
> กลุ่ม `/api/v1/holidays/...` (X-API-Key) และ Portal CRUD อื่น ๆ มาในเฟส BE2+
> ดูสดได้ที่ Swagger UI `http://localhost:8080/swagger-ui.html` (OpenAPI: `/v3/api-docs`)

---

## Envelope ร่วม (ทุก endpoint)

### Success — `ApiResponse<T>`

```json
{
  "data": { },
  "meta": {
    "apiVersion": "v1",
    "requestId": "9b1c3d2e-...-uuid",
    "generatedAt": "2026-08-16T00:20:00Z"
  }
}
```

- `meta.page` จะปรากฏเฉพาะ list response (ยังไม่มีในเฟสนี้)
- field ที่เป็น `null` ใน `meta` จะถูกตัดออก (`@JsonInclude(NON_NULL)`)

### Error — Problem Details (RFC 7807) + field เสริม

```json
{
  "type": "https://errors.control-m/validation_error",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "code": "VALIDATION_ERROR",
  "requestId": "9b1c3d2e-...-uuid",
  "errors": [
    { "field": "username", "code": "NOT_BLANK", "message": "must not be blank" }
  ]
}
```

- ห้ามมี stack trace / SQL / secret ใน error ทุกกรณี
- `code` คือสัญญาที่ FE ใช้ผูก message (อย่าผูกกับ HTTP status หรือข้อความ)

#### error code → HTTP status

| code | status |
|---|---|
| `VALIDATION_ERROR` | 400 |
| `UNAUTHENTICATED` | 401 |
| `ACCESS_DENIED` | 403 |
| `NOT_FOUND` | 404 |
| `STATE_CONFLICT` / `OPTIMISTIC_LOCK_CONFLICT` / `DUPLICATE_RESOURCE` | 409 |
| `BUSINESS_RULE_VIOLATION` | 422 |
| `RATE_LIMITED` | 429 |
| `INTERNAL_ERROR` | 500 |
| `DEPENDENCY_UNAVAILABLE` | 503 |

---

## Auth flow model

Portal ใช้ pattern **access token ใน body + refresh token ใน HttpOnly cookie + CSRF double-submit**:

- `accessToken` (JWT RS256, อายุ 15 นาที) — FE เก็บใน memory แล้วแนบเป็น `Authorization: Bearer ...`
- `csrfToken` — คืนใน **response body** ของ login/refresh; FE เก็บใน memory แล้วแนบเป็น header `X-CSRF-Token` ตอนเรียก refresh/logout
- `control_m_refresh` — HttpOnly cookie, FE เข้าถึงไม่ได้ ส่งอัตโนมัติเฉพาะ path `/api/v1/portal/auth`
- `control_m_csrf` — cookie ที่ browser ส่งกลับอัตโนมัติ ใช้เป็นฝั่ง server ของ double-submit (server เทียบ header `X-CSRF-Token` กับ cookie นี้) โดย FE ไม่ต้องอ่านค่ามันเอง เพราะได้ `csrfToken` จาก body แล้ว
- refresh/logout ยังตรวจ `Origin` ว่าอยู่ใน allowed-origins ด้วย
- `permissions[]` (รหัส `module.resource.action`) แนบไปกับ user ทุก endpoint (login/refresh/me) ให้ FE กรองเมนูได้ — การบังคับสิทธิ์จริงอยู่ที่ backend (deny by default)

---

## 1) POST `/api/v1/portal/auth/login`

เข้าสู่ระบบด้วย username/password

### Request

```
POST /api/v1/portal/auth/login
Content-Type: application/json
```

```json
{ "username": "admin", "password": "Admin@1234" }
```

Validation: `username` ไม่ว่าง ≤100, `password` ไม่ว่าง ≤200

### Response `200 OK`

Set-Cookie: `control_m_refresh` (HttpOnly) + `control_m_csrf`

```json
{
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "csrfToken": "d3Ad...random",
    "user": {
      "id": "f73f202c-35e7-4de4-b05c-77631e2007fc",
      "username": "admin",
      "displayName": "System Administrator",
      "permissions": ["holiday.revision.submit", "holiday.revision.view"]
    }
  },
  "meta": { "apiVersion": "v1", "requestId": "...", "generatedAt": "2026-08-16T00:20:00Z" }
}
```

> `csrfToken` มีค่าเท่ากับ cookie `control_m_csrf` (double-submit) — FE เก็บ `csrfToken` จาก body ไว้ใน memory
> `permissions[]` จะว่าง `[]` ถ้า user ยังไม่ถูกผูก role/permission (เช่น admin ใน BE1 ที่ยังไม่ seed permission)

### Error

- `401 UNAUTHENTICATED` — username หรือ password ผิด (ข้อความเดียวกันเสมอ ไม่เปิดเผยว่า user มีจริง)
- `400 VALIDATION_ERROR` — field ไม่ผ่าน validation

---

## 2) POST `/api/v1/portal/auth/refresh`

ต่ออายุ session แบบ rotate (ออก access token ใหม่ + หมุน refresh/csrf cookie ใหม่)

### Request — ไม่มี body

```
POST /api/v1/portal/auth/refresh
Cookie: control_m_refresh=<refresh>; control_m_csrf=<csrf>
Origin: http://localhost:4200
X-CSRF-Token: <csrfToken จาก body ของ login/refresh ครั้งก่อน>
```

### Response `200 OK`

รูปเดียวกับ login (`AuthResponse`) และ rotate cookie + `csrfToken` ใหม่

```json
{
  "data": {
    "accessToken": "eyJ...ใหม่",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "csrfToken": "n3W...random",
    "user": {
      "id": "f73f202c-35e7-4de4-b05c-77631e2007fc",
      "username": "admin",
      "displayName": "System Administrator",
      "permissions": ["holiday.revision.submit", "holiday.revision.view"]
    }
  },
  "meta": { "apiVersion": "v1", "requestId": "...", "generatedAt": "..." }
}
```

> refresh หมุน csrf ใหม่ทุกครั้ง — FE ต้องอัปเดต `csrfToken` ใน memory จาก response นี้

### Error

- `403` — `Origin` ไม่อยู่ใน allowed-origins หรือ `X-CSRF-Token` ไม่ตรง cookie (ตอบจาก filter ก่อนถึง controller)
- `401 UNAUTHENTICATED` — refresh token หมดอายุ / ถูก revoke / ถูกใช้ซ้ำ (reuse detection)

---

## 3) POST `/api/v1/portal/auth/logout`

ยกเลิก session ปัจจุบันและเคลียร์ cookie

### Request — ไม่มี body

```
POST /api/v1/portal/auth/logout
Cookie: control_m_refresh=<refresh>; control_m_csrf=<csrf>
Origin: http://localhost:4200
X-CSRF-Token: <csrfToken จาก body ของ login/refresh ครั้งก่อน>
```

### Response `200 OK`

`data` เป็น `null` และ Set-Cookie เคลียร์ `control_m_refresh` + `control_m_csrf`

```json
{
  "data": null,
  "meta": { "apiVersion": "v1", "requestId": "...", "generatedAt": "..." }
}
```

### Error

- `403` — CSRF/Origin ไม่ผ่าน

---

## 4) GET `/api/v1/portal/auth/me`

ข้อมูล user ของ access token ปัจจุบัน

### Request — ไม่มี body

```
GET /api/v1/portal/auth/me
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

### Response `200 OK`

```json
{
  "data": {
    "id": "f73f202c-35e7-4de4-b05c-77631e2007fc",
    "username": "admin",
    "displayName": "System Administrator",
    "permissions": ["holiday.revision.submit", "holiday.revision.view"]
  },
  "meta": { "apiVersion": "v1", "requestId": "...", "generatedAt": "..." }
}
```

> `permissions[]` โหลดสด ณ เวลาที่เรียก (นับ role assignment ที่ยัง valid) — FE ใช้กรองเมนู

### Error

- `401 UNAUTHENTICATED` — ไม่มี token / token หมดอายุ / ลายเซ็นไม่ถูกต้อง

---

## ตัวอย่าง cURL (local)

```bash
# login (เก็บ cookie ลงไฟล์)
curl -s -c cookies.txt -X POST http://localhost:8080/api/v1/portal/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@1234"}'

# me (ใช้ accessToken จาก response ด้านบน)
curl -s http://localhost:8080/api/v1/portal/auth/me \
  -H "Authorization: Bearer <accessToken>"

# refresh (แนบ csrfToken ที่ได้จาก body ของ login + Origin; cookie ส่งอัตโนมัติจาก -b)
curl -s -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/v1/portal/auth/refresh \
  -H 'Origin: http://localhost:4200' \
  -H 'X-CSRF-Token: <csrfToken จาก response login>'

# logout
curl -s -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/v1/portal/auth/logout \
  -H 'Origin: http://localhost:4200' \
  -H 'X-CSRF-Token: <csrfToken จาก response login/refresh>'
```
