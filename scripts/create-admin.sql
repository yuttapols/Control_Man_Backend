-- create-admin.sql
-- เพิ่ม/รีเซ็ต admin user หนึ่งคน แล้วผูกกับ role SUPER_ADMIN ที่ได้ทุก permission
--
-- ใช้กับ local/dev เท่านั้น รันผ่าน psql พร้อมส่งตัวแปร (ห้าม hardcode รหัสผ่านลงไฟล์):
--
--   psql -h [::1] -U postgres -d control_m -v ON_ERROR_STOP=1 \
--     -v username=admin -v email=admin@control-m.local \
--     -v display_name='System Administrator' -v password='S3cret!' \
--     -f scripts/create-admin.sql
--
-- หรือใช้ตัวช่วย scripts/create-admin.ps1 ซึ่งจะ prompt รหัสผ่านให้แบบไม่โชว์บนจอ
--
-- ตัวแปรที่ต้องส่ง: username, email, display_name, password
-- password_hash เก็บเป็น {bcrypt}$2a$... ให้ตรงกับ DelegatingPasswordEncoder ของแอป

\set ON_ERROR_STOP on

BEGIN;

-- 1) มี role SUPER_ADMIN ไว้ (ถ้ายังไม่มี)
INSERT INTO role (code, name_th, description, status)
VALUES ('SUPER_ADMIN', 'ผู้ดูแลระบบสูงสุด',
        'Full-access role managed by scripts/create-admin.sql', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- 2) upsert user (match แบบ case-insensitive ตาม unique index uq_app_user_username_ci)
UPDATE app_user
SET email              = :'email',
    display_name       = :'display_name',
    password_hash      = '{bcrypt}' || crypt(:'password', gen_salt('bf', 12)),
    status             = 'ACTIVE',
    failed_login_count = 0,
    locked_until       = NULL,
    updated_at         = now()
WHERE lower(username) = lower(:'username');

INSERT INTO app_user (username, email, display_name, password_hash, status)
SELECT :'username', :'email', :'display_name',
       '{bcrypt}' || crypt(:'password', gen_salt('bf', 12)), 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE lower(username) = lower(:'username')
);

-- 3) ผูก user กับ SUPER_ADMIN
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
CROSS JOIN role r
WHERE lower(u.username) = lower(:'username')
  AND r.code = 'SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 4) ให้ SUPER_ADMIN ได้ทุก permission ที่มีอยู่ตอนนี้
--    (รันซ้ำได้เมื่อ phase ถัด ๆ ไป seed permission เพิ่ม)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

-- สรุปผล
SELECT u.username, u.email, u.status,
       (SELECT count(*) FROM user_role ur WHERE ur.user_id = u.id)          AS roles,
       (SELECT count(*) FROM role_permission rp
          JOIN role r ON r.id = rp.role_id
         WHERE r.code = 'SUPER_ADMIN')                                       AS super_admin_permissions
FROM app_user u
WHERE lower(u.username) = lower(:'username');
