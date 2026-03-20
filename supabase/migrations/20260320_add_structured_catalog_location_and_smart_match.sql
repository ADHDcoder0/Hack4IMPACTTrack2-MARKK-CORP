-- Structured categories, India location hierarchy, and smart-match persistence
-- Safe to re-run with IF NOT EXISTS guards where possible.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Waste category master table
CREATE TABLE IF NOT EXISTS waste_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug TEXT NOT NULL UNIQUE,
    label TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO waste_categories (slug, label, sort_order)
VALUES
    ('e_waste', 'E-Waste', 10),
    ('metal_scrap', 'Metal Scrap', 20),
    ('plastic_pallets', 'Plastic Pallets', 30),
    ('paper_cardboard', 'Paper/Cardboard', 40),
    ('glass', 'Glass', 50),
    ('rubber', 'Rubber', 60),
    ('textile', 'Textile', 70),
    ('organic', 'Organic', 80),
    ('mixed_waste', 'Mixed Waste', 90)
ON CONFLICT (slug) DO NOTHING;

-- 2) Structured India location tables
CREATE TABLE IF NOT EXISTS india_states (
    state_code TEXT PRIMARY KEY,
    state_name TEXT NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS india_districts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state_code TEXT NOT NULL,
    district_name TEXT NOT NULL,
    district_code TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (state_code, district_name)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_india_districts_state_code'
    ) THEN
        ALTER TABLE india_districts
        ADD CONSTRAINT fk_india_districts_state_code
        FOREIGN KEY (state_code) REFERENCES india_states(state_code);
    END IF;
END $$;

-- India states + UTs seed (36)
INSERT INTO india_states (state_code, state_name)
VALUES
    ('AN', 'Andaman and Nicobar Islands'),
    ('AP', 'Andhra Pradesh'),
    ('AR', 'Arunachal Pradesh'),
    ('AS', 'Assam'),
    ('BR', 'Bihar'),
    ('CH', 'Chandigarh'),
    ('CT', 'Chhattisgarh'),
    ('DN', 'Dadra and Nagar Haveli and Daman and Diu'),
    ('DL', 'Delhi'),
    ('GA', 'Goa'),
    ('GJ', 'Gujarat'),
    ('HR', 'Haryana'),
    ('HP', 'Himachal Pradesh'),
    ('JK', 'Jammu and Kashmir'),
    ('JH', 'Jharkhand'),
    ('KA', 'Karnataka'),
    ('KL', 'Kerala'),
    ('LA', 'Ladakh'),
    ('LD', 'Lakshadweep'),
    ('MP', 'Madhya Pradesh'),
    ('MH', 'Maharashtra'),
    ('MN', 'Manipur'),
    ('ML', 'Meghalaya'),
    ('MZ', 'Mizoram'),
    ('NL', 'Nagaland'),
    ('OR', 'Odisha'),
    ('PY', 'Puducherry'),
    ('PB', 'Punjab'),
    ('RJ', 'Rajasthan'),
    ('SK', 'Sikkim'),
    ('TN', 'Tamil Nadu'),
    ('TS', 'Telangana'),
    ('TR', 'Tripura'),
    ('UP', 'Uttar Pradesh'),
    ('UT', 'Uttarakhand'),
    ('WB', 'West Bengal')
ON CONFLICT (state_code) DO NOTHING;

-- 3) Add listing references to category and structured location
ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS waste_category_id UUID,
    ADD COLUMN IF NOT EXISTS state_code TEXT,
    ADD COLUMN IF NOT EXISTS district_id UUID,
    ADD COLUMN IF NOT EXISTS town_city TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_listings_waste_category_id'
    ) THEN
        ALTER TABLE listings
        ADD CONSTRAINT fk_listings_waste_category_id
        FOREIGN KEY (waste_category_id) REFERENCES waste_categories(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_listings_state_code'
    ) THEN
        ALTER TABLE listings
        ADD CONSTRAINT fk_listings_state_code
        FOREIGN KEY (state_code) REFERENCES india_states(state_code);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_listings_district_id'
    ) THEN
        ALTER TABLE listings
        ADD CONSTRAINT fk_listings_district_id
        FOREIGN KEY (district_id) REFERENCES india_districts(id);
    END IF;
END $$;

-- 4) Add structured location fields to users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS state_code TEXT,
    ADD COLUMN IF NOT EXISTS district_id UUID,
    ADD COLUMN IF NOT EXISTS town_city TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_state_code'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT fk_users_state_code
        FOREIGN KEY (state_code) REFERENCES india_states(state_code);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_district_id'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT fk_users_district_id
        FOREIGN KEY (district_id) REFERENCES india_districts(id);
    END IF;
END $$;

-- 5) Persisted smart-match insights for reliability/ETA
CREATE TABLE IF NOT EXISTS smart_match_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL,
    buyer_id UUID,
    reliability_score NUMERIC(5,2) NOT NULL,
    eta_days INT NOT NULL,
    confidence_note TEXT,
    model_version TEXT,
    source TEXT NOT NULL DEFAULT 'python_pipeline',
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_smi_listing_id'
    ) THEN
        ALTER TABLE smart_match_insights
        ADD CONSTRAINT fk_smi_listing_id
        FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_smi_buyer_id'
    ) THEN
        ALTER TABLE smart_match_insights
        ADD CONSTRAINT fk_smi_buyer_id
        FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 6) Backfill listing category using existing free-text waste_type
UPDATE listings l
SET waste_category_id = wc.id
FROM waste_categories wc
WHERE l.waste_category_id IS NULL
  AND (
    (LOWER(l.waste_type) LIKE '%e-waste%' OR LOWER(l.waste_type) LIKE '%ewaste%') AND wc.slug = 'e_waste'
    OR (LOWER(l.waste_type) LIKE '%metal%') AND wc.slug = 'metal_scrap'
    OR (LOWER(l.waste_type) LIKE '%plastic%' OR LOWER(l.waste_type) LIKE '%pallet%') AND wc.slug = 'plastic_pallets'
    OR (LOWER(l.waste_type) LIKE '%paper%' OR LOWER(l.waste_type) LIKE '%cardboard%') AND wc.slug = 'paper_cardboard'
    OR (LOWER(l.waste_type) LIKE '%glass%') AND wc.slug = 'glass'
    OR (LOWER(l.waste_type) LIKE '%rubber%') AND wc.slug = 'rubber'
    OR (LOWER(l.waste_type) LIKE '%textile%' OR LOWER(l.waste_type) LIKE '%fabric%') AND wc.slug = 'textile'
    OR (LOWER(l.waste_type) LIKE '%organic%' OR LOWER(l.waste_type) LIKE '%bio%') AND wc.slug = 'organic'
  );

-- Default remaining unmapped values to mixed waste
UPDATE listings l
SET waste_category_id = wc.id
FROM waste_categories wc
WHERE l.waste_category_id IS NULL
  AND wc.slug = 'mixed_waste';

-- 7) Indexes for scale
CREATE INDEX IF NOT EXISTS idx_listings_waste_category_id ON listings(waste_category_id);
CREATE INDEX IF NOT EXISTS idx_listings_state_district ON listings(state_code, district_id);
CREATE INDEX IF NOT EXISTS idx_users_state_district ON users(state_code, district_id);
CREATE INDEX IF NOT EXISTS idx_india_districts_state_name ON india_districts(state_code, district_name);
CREATE INDEX IF NOT EXISTS idx_smi_listing_generated_at ON smart_match_insights(listing_id, generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_matches_listing_status ON matches(listing_id, status);

-- NOTE: Full India district seed is intentionally separate due size and update frequency.
-- Next step: load complete district dataset via dedicated seed script from an authoritative source.
