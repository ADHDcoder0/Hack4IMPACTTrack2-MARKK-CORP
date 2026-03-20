# Supabase Rollout Directions

Last updated: 2026-03-20

## Goal
Apply backend changes required by the new app behavior:
- Supplier confirm/reject match requests
- Rejected requests removed from supplier queue
- Buyer can revert pending request
- Supplier can edit own listings
- Supplier-owned storage path enforcement for images

## Migrations To Apply
Run in this order:
1. supabase/migrations/20260319_add_image_url_to_listings.sql
2. supabase/migrations/20260320_add_structured_catalog_location_and_smart_match.sql
3. supabase/migrations/20260320_seed_india_districts_template.sql
4. supabase/migrations/20260320_enforce_match_listing_rls_and_flow_constraints.sql
5. supabase/migrations/20260320_enforce_storage_rls_for_listing_images.sql

## How To Apply
### Option A: Supabase CLI
1. Ensure project is linked.
2. Run:
   - supabase db push
3. Confirm no migration errors.

### Option B: SQL Editor (manual)
1. Open Supabase SQL Editor.
2. Execute each migration file in the same order listed above.
3. Confirm each script completes successfully.

## Post-Apply Verification Queries
Run these checks in SQL Editor.

### 1) Confirm match status constraint exists
SELECT conname
FROM pg_constraint
WHERE conname = 'matches_status_allowed';

### 2) Confirm RLS enabled
SELECT relname, relrowsecurity
FROM pg_class
WHERE relname IN ('matches', 'listings');

### 3) Confirm table policies
SELECT schemaname, tablename, policyname, cmd
FROM pg_policies
WHERE tablename IN ('matches', 'listings')
ORDER BY tablename, policyname;

### 4) Confirm storage policies
SELECT schemaname, tablename, policyname, cmd
FROM pg_policies
WHERE schemaname = 'storage' AND tablename = 'objects'
ORDER BY policyname;

### 5) Confirm indexes
SELECT indexname, tablename
FROM pg_indexes
WHERE tablename IN ('matches', 'listings')
ORDER BY tablename, indexname;

## App Behavior Verification
Use two users: one buyer and one supplier.

### A) Supplier confirm flow
1. Buyer creates request on a supplier listing.
2. Supplier opens Match Requests.
3. Tap Confirm.
4. Expected:
   - Button row disappears for that card (status no longer pending).
   - Card status shows confirmed.
   - Buyer sees confirmed in My Matches.

### B) Supplier reject flow
1. Buyer creates request.
2. Supplier taps Reject.
3. Expected:
   - Request is removed from supplier request list.
   - Buyer sees rejected status in My Matches (until user archives/reverts logic applies).

### C) Buyer revert flow
1. Buyer has a pending request in My Matches.
2. Tap Revert Request.
3. Expected:
   - Request row disappears from buyer list.
   - Supplier no longer sees that pending request.

### D) Supplier listing edit flow
1. Supplier opens dashboard listing card.
2. Tap Edit Details.
3. Dialog opens with prefilled values.
4. Update values and Save Changes.
5. Expected:
   - Listing card reflects updated values after refresh.
   - New image uploads if changed; old image remains if unchanged.

## Common Failure Causes
1. RLS policy missing on matches/listings.
2. Storage object path not prefixed with supplier auth uid.
3. Bucket name mismatch (must be images unless app code changed).
4. Buyer trying to revert non-pending request (blocked by policy by design).

## Rollback Guidance
If a migration fails in production:
1. Do not partially rerun random sections.
2. Fix failing SQL and rerun that migration transactionally.
3. Re-check policies with verification queries before app smoke tests.
