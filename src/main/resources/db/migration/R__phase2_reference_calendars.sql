INSERT INTO calendar (code, name_th, name_en, country_code, status)
VALUES
    ('TH_GOVERNMENT', 'วันหยุดราชการ', 'Thailand Government Holiday', 'TH', 'ACTIVE'),
    ('TH_BANK', 'วันหยุดธนาคาร', 'Thailand Bank Holiday', 'TH', 'ACTIVE')
ON CONFLICT (code) DO UPDATE
SET name_th = EXCLUDED.name_th,
    name_en = EXCLUDED.name_en,
    country_code = EXCLUDED.country_code,
    status = EXCLUDED.status,
    updated_at = now();

