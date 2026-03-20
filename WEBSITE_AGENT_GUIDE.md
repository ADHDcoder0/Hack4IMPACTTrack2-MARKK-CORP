# ScrapSetu Website Agent Guide

Last updated: 2026-03-20

## Purpose
This document maps app-side behavior to website implementation so an engineering agent can deliver feature parity with the Android app, including schema connection logic, flow rules, AI prompt structure, and safety constraints.

## Scope
- Authentication and profile onboarding
- Supplier listing create/edit with image upload
- Buyer listing browse/filter/match request
- Match lifecycle (pending, confirmed, rejected, reverted)
- Chat enablement after match events
- AI smart-match and price-estimation integration
- Analytics summaries from transactional data
- Supabase schema + RLS + storage policy alignment

## Architecture (Web)
- Frontend: Any web stack (React/Vue/Svelte/plain TS).
- Backend: Supabase (Auth, Postgres, Realtime, Storage).
- AI Layer: server-side endpoint that calls model provider and applies prompt safety guards.
- State:
  - Session state from Supabase auth token.
  - Feature state per module (listings, matches, chat, analytics).
- Error model:
  - Domain-safe errors from backend.
  - Retry on transient network issues.
  - Deterministic fallback when AI returns empty/invalid output.

## Required Database Objects
Assumes existing/updated migrations from `supabase/migrations` have been applied.

### Tables
1. `users`
- `id` UUID PK (matches auth user id)
- `name` text
- `phone` text
- `email` text
- `role` text (`buyer` or `supplier`)
- `location` text
- additional onboarding fields as needed
- timestamps

2. `listings`
- `id` UUID PK
- `supplier_id` UUID FK -> `users.id`
- `waste_type` text
- `quantity_kg` numeric
- `price_per_kg` numeric (or equivalent)
- `location` text
- `image_url` text
- `status` text
- timestamps

3. `matches`
- `id` UUID PK
- `listing_id` UUID FK -> `listings.id`
- `buyer_id` UUID FK -> `users.id`
- `status` text (`pending`, `confirmed`, `rejected`)
- timestamps

4. `messages` (if chat is enabled)
- `id` UUID PK
- `match_id` UUID FK -> `matches.id`
- `sender_id` UUID FK -> `users.id`
- `content` text
- timestamps

### Storage
- Bucket: `images`
- Object key convention: `<supplierId>/<uuid>.<ext>`
- Always set content-type on upload.

## RLS and Security Rules
Implement policies equivalent to app behavior.

1. `users`
- User can read/update own profile.

2. `listings`
- Supplier can insert/update/delete own listings.
- Buyers can read active listings.

3. `matches`
- Buyer can create match request only for valid listing.
- Buyer can revert only when status is `pending`.
- Supplier can confirm/reject only for matches tied to supplier-owned listing.
- Rejected rows can be hidden from supplier queue at query level.

4. `messages`
- Only participants of related match can read/write messages.

5. `storage.objects` for bucket `images`
- Supplier can write only under path prefix matching auth uid.
- Public/private read behavior as required by product decision.

## Canonical Feature Flows

### 1) Auth + onboarding
1. User signs up/signs in via Supabase Auth.
2. On session establish, fetch `users` row by auth uid.
3. If profile missing/incomplete, route to onboarding steps.
4. Persist completed profile; route by role:
- supplier -> supplier dashboard
- buyer -> buyer dashboard

### 2) Supplier listing create
1. Validate form fields.
2. Upload image to `images/<supplierId>/<uuid>.<ext>`.
3. Get URL/path and insert listing row.
4. Refresh supplier list.

### 3) Supplier listing edit
1. Load existing listing values in edit dialog.
2. If image changed: upload new object and update `image_url`.
3. Update listing row.
4. Cancel pending matches tied to edited listing if business rule requires this (already aligned in app-side logic).

### 4) Buyer browse + filter
1. Query active listings.
2. Apply filters (waste type, location, price range, quantity range).
3. Show empty state when no results.

### 5) Buyer request match
1. Buyer creates `matches` row with status `pending`.
2. Supplier sees in match queue.

### 6) Supplier confirm/reject
1. Supplier updates match status from `pending` to `confirmed` or `rejected`.
2. UI updates for both users.
3. Rejected requests should no longer appear in active supplier queue.

### 7) Buyer revert pending request
1. Buyer action allowed only when status is `pending`.
2. Delete or state-transition per policy/migration design.
3. Supplier queue reflects removal.

### 8) Chat flow
1. Chat enabled when business condition is met (usually confirmed match).
2. Subscribe to realtime channel for match messages.
3. Persist each message in `messages`.

### 9) Analytics cards
Compute from DB aggregates, e.g.:
- active matches
- pending approvals
- conversion rate
Use server-side aggregation or SQL views for consistency.

## AI Integration Design
AI calls should be server-side only. Never call provider directly from browser with secret keys.

### Endpoints
1. `POST /api/ai/smart-match`
- Input: listing + supplier context + optional regional hints.
- Output: ranked buyer or listing suggestions with reasons.

2. `POST /api/ai/price-estimate`
- Input: waste type, quantity, location, quality factors.
- Output: estimated range + confidence + rationale.

### Prompt Structure (Template)
Use explicit delimiters and strict output schema.

System instructions:
- You are a marketplace assistant.
- Return JSON only.
- If uncertain, mark low confidence and provide conservative range.

User payload block:
- listing_data
- market_context
- constraints

Expected JSON schema:
- `summary`
- `recommendations[]`
- `confidence`
- `risks[]`

### Prompt Injection Protection
1. Treat all user text as untrusted data.
2. Wrap user content in quoted/escaped fields; never concatenate into instruction layer.
3. Keep system and developer instructions immutable server-side.
4. Enforce JSON schema validation after model output.
5. Reject outputs containing policy-breaking content or invalid structure.
6. Apply timeout + retry + deterministic fallback when model fails.

## API Contract Suggestions

### Listings
- `GET /api/listings`
- `POST /api/listings`
- `PATCH /api/listings/:id`
- `DELETE /api/listings/:id`

### Matches
- `GET /api/matches?role=buyer|supplier`
- `POST /api/matches`
- `PATCH /api/matches/:id/status`
- `POST /api/matches/:id/revert`

### Chat
- `GET /api/matches/:id/messages`
- `POST /api/matches/:id/messages`

## Query and State Rules
- Use auth uid from session token, never from client-supplied user id.
- Apply optimistic UI only for reversible actions.
- Refresh on mutation success to avoid stale cards.
- Keep in-flight guards to prevent duplicate API calls.

## Validation Rules
- quantity and price must be positive.
- role must be buyer/supplier.
- match status transitions must be legal.
- location fields normalized before filtering.

## Error and Fallback Strategy
- Network timeout: retry with capped backoff.
- AI timeout/empty response: show fallback text and continue app flow.
- Storage upload fail: block submit and allow retry.
- Partial mutation fail: rollback UI state and show actionable error.

## Website Delivery Checklist
- Auth and role routing implemented.
- Onboarding/profile completion wired to `users`.
- Supplier listing create/edit with image upload and secure path.
- Buyer filtering + empty states.
- Match lifecycle actions with proper permissions.
- Chat realtime channel and persistence.
- AI endpoints with schema validation and injection protections.
- Analytics cards from real aggregates.
- RLS/policy verification completed in Supabase.

## Deployment Notes
- Keep Supabase URL/keys in environment config.
- Use service-role key only on server.
- Add request logs for match/status transitions and AI errors.
- Smoke test with two accounts (buyer + supplier) before release.
