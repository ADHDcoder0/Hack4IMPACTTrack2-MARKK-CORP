-- Template migration: seed full India district list into india_districts
--
-- How to use:
-- 1) Prepare a CSV with headers: state_code,district_name,district_code
-- 2) Insert rows with this pattern (bulk values or generated SQL)
-- 3) Run this migration after 20260320_add_structured_catalog_location_and_smart_match.sql

-- Example rows (replace with full list):
INSERT INTO india_districts (state_code, district_name, district_code)
VALUES
    ('MH', 'Mumbai City', 'MH-MUM-CITY'),
    ('MH', 'Mumbai Suburban', 'MH-MUM-SUB'),
    ('MH', 'Pune', 'MH-PUNE'),
    ('DL', 'New Delhi', 'DL-NEW-DELHI'),
    ('KA', 'Bengaluru Urban', 'KA-BLR-URBAN'),
    ('TS', 'Hyderabad', 'TS-HYD')
ON CONFLICT (state_code, district_name) DO UPDATE
SET district_code = EXCLUDED.district_code,
    active = TRUE;

-- Optional cleanup if your source marks deprecated districts:
-- UPDATE india_districts SET active = FALSE WHERE ...;
