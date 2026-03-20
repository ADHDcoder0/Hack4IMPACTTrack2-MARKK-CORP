# ScrapSetu Demo Verification Checklist

Date: 2026-03-20

## Pre-demo cleanup
- Delete junk test listings from Supabase:
  - kkk
  - hey
  - kyshag
  - bg
- Create clean Supplier demo account with real business profile details.
- Create clean Buyer demo account with real business profile details.

## Mandatory Supabase SQL (run once before demo)
Run in Supabase SQL editor:

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY "users_select_own" ON users
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY "users_update_own" ON users
  FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "users_insert_own" ON users
  FOR INSERT WITH CHECK (auth.uid() = id);

ALTER TABLE users ADD COLUMN IF NOT EXISTS business_name text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS state text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS business_type text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS waste_categories text[];
ALTER TABLE users ADD COLUMN IF NOT EXISTS monthly_volume int;

## Feature flow checks
- Registration stepper:
  - Supplier: complete Step 1, Step 2, Step 3 and confirm redirect to supplier dashboard.
  - Buyer: confirm Step 3 shows buyer-specific labels.
  - Validation: verify Step 1 blocks invalid email or password shorter than 8.
- New listing image flow:
  - Upload photo and verify Gemini detection + pricing appears.
  - Turn on airplane mode, submit listing, verify Photo upload failed dialog appears.
  - Retry after network restored, verify listing is created.
- AI fallback chain:
  - Groq timeout/failure should fall back to Gemini, then to analytical fallback.
- Buyer dashboard filters:
  - Apply Textile material filter, verify only textile listings.
  - Apply state filter and verify combined filtering.
  - Clear filters and verify all listings return.
- Match behavior:
  - Confirm match as supplier.
  - Log in as buyer and verify matched listing card is greyed and non-interactive.
  - Edit listing with pending requests and verify pending matches become cancelled.

## Metrics and UI checks
- Buyer dashboard market trend card:
  - If active value or inventory is 0, card should be hidden.
- Buyer matches metrics:
  - Total savings should use confirmed matches.
  - Sustainable network tons should use confirmed quantities / 1000.
  - Match date should show formatted created_at as dd MMM yyyy.
- Empty states:
  - Supplier with no listings should see No listings yet card with Add New Listing action.
  - Buyer with no requests should see No match requests yet card with Browse Listings action.

## Network resilience checks
- Test on real device.
- Test on slow network (3G profile in developer options).
- Rapid-scroll listing cards and verify image placeholders/caching behavior.

## Sign-off
- No crash during end-to-end supplier to buyer flow.
- All critical tasks verified.
- Demo accounts and sample data clean.
