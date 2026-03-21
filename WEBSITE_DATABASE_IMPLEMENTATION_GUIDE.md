# ScrapSetu Website Database Implementation Guide

Last updated: 2026-03-21

## 1) Goal
Build website feature parity with the Android app for all flows that read/write Supabase.

This guide is implementation-focused and includes:
- Actual schema and migration-backed constraints
- End-to-end feature flows (auth, listings, matches, chat, master data, smart-match insights)
- RLS and storage requirements
- API recommendations for a web app architecture
- Known pitfalls to avoid during implementation

## 2) Required Supabase Migrations
Apply in this order:
1. supabase/migrations/20260319_add_image_url_to_listings.sql
2. supabase/migrations/20260320_add_structured_catalog_location_and_smart_match.sql
3. supabase/migrations/20260320_seed_india_districts_template.sql
4. supabase/migrations/20260320_enforce_match_listing_rls_and_flow_constraints.sql
5. supabase/migrations/20260320_enforce_storage_rls_for_listing_images.sql
6. supabase/migrations/20260320_add_messages.sql

## 3) Database Objects Used By Features

### 3.1 Core tables
- users
- listings
- matches
- messages

### 3.2 Master/lookup tables
- waste_categories
- india_states
- india_districts

### 3.3 AI/persisted output table
- smart_match_insights

### 3.4 Storage
- Bucket: images
- Owner path convention: <auth.uid()>/<uuid>.<ext>

## 4) Canonical Status Rules

### 4.1 matches.status allowed values
- pending
- confirmed
- rejected

The migration enforces this with a check constraint.
Do not write other values.

### 4.2 listings.status
Current flows use active/matched/closed semantics in UI, with active used for buyer listing fetch.
Keep website list APIs aligned to active for buyer-facing browse.

## 5) Feature Flows That Interact With Database

## 5.1 Auth + profile bootstrap

### User journey
1. User signs up or signs in via Supabase Auth.
2. Website loads users row by auth user id.
3. If no row exists after sign up, insert profile row.
4. Route by role:
   - supplier -> supplier dashboard
   - buyer -> buyer dashboard

### DB operations
- Insert or upsert users row after signup.
- Select own users row on app load.
- Update own users row on profile edit.

### Required fields used by current app model
- id, email, role, name, location
- business_name, phone, state, business_type, waste_categories, monthly_volume
- state_code, district_id, town_city

### Security expectation
- User can select/update/insert only own row.

## 5.2 Master data (categories + India location)

### User journey
1. Supplier opens listing form.
2. Website fetches active waste categories and states.
3. When state changes, website fetches active districts for that state.

### DB operations
- select * from waste_categories where active = true order by sort_order asc
- select * from india_states where active = true order by state_name asc
- select * from india_districts where active = true and state_code = ? order by district_name asc

### Security expectation
- Authenticated read access for lookup tables.
- No browser-side write access for lookup tables.

## 5.3 Supplier create listing with image

### User journey
1. Supplier fills listing form.
2. Website uploads image to storage bucket images.
3. Website inserts listings row using uploaded image URL/path.
4. Supplier listing list refreshes.

### DB + storage operations
- storage.objects insert into images bucket at path <supplierId>/<uuid>.<ext>
- insert into listings:
  - supplier_id (must equal auth.uid)
  - waste_type
  - waste_category_id
  - quantity_kg
  - price_per_kg
  - state_code
  - district_id
  - town_city
  - location
  - description
  - image_url
  - status default active

### Security expectation
- Listings insert policy requires supplier_id == auth.uid.
- Storage insert policy requires first path segment == auth.uid.

## 5.4 Supplier edit listing

### User journey
1. Supplier opens own listing.
2. Supplier edits details and optionally image.
3. If image changed, upload new object first.
4. Update listings row.
5. Refresh supplier listings.

### DB operations
- Optional new image upload to images/<supplierId>/<uuid>.<ext>
- update listings where id = listingId and supplier_id = auth.uid

### Important business rule
Current app behavior attempts to cancel pending matches when listing is edited.

Recommended website handling under current DB constraints:
- Either delete pending matches for listing, or
- Update pending -> rejected

Do not update to cancelled because matches.status constraint does not allow it.

## 5.5 Buyer browse listings + filters

### User journey
1. Buyer opens dashboard.
2. Website fetches active listings.
3. Client applies selected filters (material/state now, range filters optional server-side).

### DB operations
- select from listings where status = active order by created_at desc

### Security expectation
- Buyers can only read active listings (or own listings if they are supplier).

## 5.6 Buyer request match

### User journey
1. Buyer clicks Request Match on listing.
2. Website inserts a new matches row with pending status.
3. Supplier sees request in supplier queue.

### DB operations
- insert into matches (listing_id, buyer_id, status=pending)

### Security expectation
- matches insert check enforces buyer_id == auth.uid and status == pending.

## 5.7 Supplier match queue + confirm/reject

### User journey
1. Supplier opens match requests screen.
2. Website loads matches linked to supplier-owned listings.
3. Supplier confirms or rejects pending requests.
4. Rejected requests are removed from supplier queue UI.

### DB operations
- select matches where listing is owned by supplier (through RLS)
- update matches set status = confirmed|rejected where id = ? and current status = pending

### Security expectation
- Only listing owner can perform update.
- Transition is pending -> confirmed or pending -> rejected.

## 5.8 Buyer match status + revert request

### User journey
1. Buyer opens My Matches.
2. Website fetches buyer-owned matches.
3. Buyer can revert only pending request.

### DB operations
- select matches where buyer_id = auth.uid order by created_at desc
- delete from matches where id = ? and buyer_id = auth.uid and status = pending

### Security expectation
- Buyer cannot revert confirmed/rejected.

## 5.9 Chat (messages table + realtime)

### User journey
1. User opens chat for a match.
2. Website loads last 24h messages (ascending by created_at).
3. Website subscribes to realtime inserts.
4. User sends text/image.
5. Recipient marks messages as read.

### DB operations
- select from messages where match_id = ? and created_at >= now - 24h
- insert text message: match_id, sender_id, content
- insert image message: match_id, sender_id, image_url
- update messages set read_at = now where match_id = ? and sender_id != current_user
- optional delete old messages (< 24h retention cutoff) if you keep same policy

### Realtime
- messages table is added to supabase_realtime publication.
- Subscribe to INSERT for table messages and filter in client by match_id.

### Security expectation
- Only match participants can read/insert/delete.
- Non-sender participant can mark read.

### Message constraints from app behavior
- Retention window: 24 hours
- Max in-memory/chat window size: 50 messages

## 5.10 Smart match insight read flow

### User journey
1. Buyer opens smart-match dialog for a listing.
2. Website fetches latest persisted smart_match_insights row for listing.
3. If none, show empty-state copy and continue normal request flow.

### DB operations
- select * from smart_match_insights where listing_id = ? order by generated_at desc limit 1

### Notes
- Writing insights is expected from your Python pipeline/service, not browser client.
- Website should consume latest insight as read-only UI data.

## 5.11 AI image detection + pricing fallback chain

### Required behavior
1. Try Gemini image classification first.
2. If Gemini key is missing or model call fails, use deterministic local detection fallback.
3. Try Groq pricing using detection result.
4. If Groq key is missing or pricing call fails, use deterministic local pricing fallback.
5. Always return a usable result to UI unless image decoding itself fails.

### Canonical category slug mapping (must be normalized)
Normalize model output to these DB-aligned slugs:
- e_waste
- metal_scrap
- plastic_pallets
- paper_cardboard
- glass
- rubber
- textile
- organic
- mixed_waste

Any unknown slug must map to mixed_waste.

### Why this matters
- Model outputs can return non-canonical labels (for example plastic or paper).
- Without normalization, downstream category selectors and analytics can drift from master tables.

## 5.12 AI analytics generation chain

### Required behavior
1. Attempt Groq analytics only when Groq key exists.
2. If Groq fails/returns unusable output, attempt Gemini.
3. Gemini call must support model discovery from /v1beta/models and try compatible generateContent models.
4. If Gemini also fails, return deterministic analytics fallback from listings + matches + user data.
5. UI should render source badge: GROQ, GEMINI, or FALLBACK.

## 6) Security And RLS Checklist

## 6.1 users
- select own
- insert own
- update own

## 6.2 listings
- select owner_or_active
- insert supplier_own
- update supplier_own

## 6.3 matches
- select buyer_or_supplier_owner
- insert buyer_own_pending
- update supplier_pending_to_confirmed_or_rejected
- delete buyer_pending_only

## 6.4 messages
- participants can read
- participants can send (sender_id must equal auth.uid)
- recipient can mark read
- participants can delete

## 6.5 storage.objects (images bucket)
- select authenticated
- insert/update/delete only when split_part(name, '/', 1) == auth.uid

## 7) Recommended Website API Surface
Use server routes for writes and any privileged operations. Example:

- POST /api/profile/upsert
- GET /api/master/categories
- GET /api/master/states
- GET /api/master/districts?stateCode=MH
- GET /api/listings?status=active
- POST /api/listings
- PATCH /api/listings/:id
- POST /api/matches
- GET /api/matches/buyer
- GET /api/matches/supplier
- PATCH /api/matches/:id/status
- DELETE /api/matches/:id/revert
- GET /api/chat/:matchId/messages
- POST /api/chat/:matchId/messages/text
- POST /api/chat/:matchId/messages/image
- PATCH /api/chat/:matchId/read
- GET /api/smart-match/insight/:listingId

## 8) Example Supabase JS Query Patterns

### Active listings
```ts
const { data, error } = await supabase
  .from('listings')
  .select('*')
  .eq('status', 'active')
  .order('created_at', { ascending: false });
```

### Request match
```ts
const { data: user } = await supabase.auth.getUser();
const { error } = await supabase.from('matches').insert({
  listing_id: listingId,
  buyer_id: user.user?.id,
  status: 'pending'
});
```

### Revert pending request
```ts
const { data: user } = await supabase.auth.getUser();
const { error } = await supabase
  .from('matches')
  .delete()
  .eq('id', matchId)
  .eq('buyer_id', user.user?.id)
  .eq('status', 'pending');
```

### Upload listing image with owner prefix
```ts
const { data: user } = await supabase.auth.getUser();
const path = `${user.user?.id}/${crypto.randomUUID()}.jpg`;
const { error } = await supabase.storage
  .from('images')
  .upload(path, file, { upsert: false, contentType: file.type });
```

## 9) Known Pitfalls To Avoid
1. Match status mismatch:
   - Current migration allows only pending/confirmed/rejected.
   - Do not introduce cancelled status writes.

2. Chat image path vs storage policy:
   - Storage policy enforces owner prefix auth.uid().
   - For chat images, also upload under auth.uid() prefix (for example <uid>/chat/<matchId>/<uuid>.jpg).

3. Blindly trusting client user id:
   - Always derive actor from Supabase auth context/token.

4. Missing district seed:
   - The district migration is a template.
   - Load full India district dataset before relying on district selectors in production.

5. Missing users policies:
   - Ensure users table RLS policies are created in target environment.

6. AI provider/model drift:
   - Do not hardcode a single Gemini model name for analytics.
   - Prefer discovered generateContent-capable models with fallback candidates.

7. Non-canonical AI category output:
   - Always normalize waste category slugs before saving or filtering.

## 10) End-To-End Website Delivery Checklist
1. Apply migrations and verify constraints/policies/indexes.
2. Confirm sign up/sign in creates and reads users row correctly.
3. Confirm supplier can create/edit own listing with image upload.
4. Confirm buyer sees only active listings.
5. Confirm match lifecycle: request, confirm/reject, revert pending.
6. Confirm chat load/send/read/realtime works for both participants.
7. Confirm smart match insight read path works with empty and success states.
8. Confirm all write actions fail correctly when user is unauthorized.
9. Smoke test with two accounts (supplier + buyer) and real image uploads.
