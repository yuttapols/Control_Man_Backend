<#
.SYNOPSIS
  เพิ่ม/รีเซ็ต admin user (role SUPER_ADMIN) ลงฐานข้อมูล Control M — สำหรับ local/dev

.DESCRIPTION
  ห่อ scripts/create-admin.sql อีกที ถ้าไม่ส่ง -Password จะถามแบบไม่โชว์บนจอ
  ไม่มีการเก็บรหัสผ่านลงไฟล์/repo ตามกฎ security ของโปรเจกต์

.EXAMPLE
  ./scripts/create-admin.ps1
  ./scripts/create-admin.ps1 -Username somchai -Email somchai@control-m.local
#>
param(
    [string]$Username    = "admin",
    [string]$Email       = "admin@control-m.local",
    [string]$DisplayName = "System Administrator",
    [string]$Password,
    [string]$DbHost      = "[::1]",      # local Postgres เป็น IPv6-only
    [int]   $Port        = 5432,
    [string]$Database    = "control_m",
    [string]$DbUser      = "postgres"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw "หา psql ไม่เจอใน PATH — ติดตั้ง PostgreSQL client ก่อน"
}

# รหัสผ่านของ app user (ที่จะเอาไป login) — prompt ถ้าไม่ได้ส่งมา
if ([string]::IsNullOrEmpty($Password)) {
    $secure  = Read-Host "รหัสผ่านสำหรับ user '$Username'" -AsSecureString
    $bstr    = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    if ([string]::IsNullOrEmpty($Password)) { throw "ยกเลิก: ไม่ได้ใส่รหัสผ่าน" }
}

# รหัสผ่านของ DB (postgres) — ใช้จาก $env:PGPASSWORD ถ้ามี ไม่งั้น psql จะถามเอง
$sqlFile = Join-Path $PSScriptRoot "create-admin.sql"

& psql -h $DbHost -p $Port -U $DbUser -d $Database `
    -v ON_ERROR_STOP=1 `
    -v username=$Username `
    -v email=$Email `
    -v display_name=$DisplayName `
    -v password=$Password `
    -f $sqlFile

if ($LASTEXITCODE -ne 0) { throw "psql ล้มเหลว (exit $LASTEXITCODE)" }
Write-Host "`nสร้าง/อัปเดต admin '$Username' เรียบร้อย" -ForegroundColor Green
