# ScrapSetu AI Context

Last updated: 2026-03-20

## Purpose
This document is the working context for AI agents editing this repository.
Use it as the first reference before making code changes.

For UI-specific implementation details, also read UI_CONTEXT.md.

## Project Snapshot
- App type: Android B2B waste marketplace
- Goal: connect suppliers and buyers/recyclers, enable listing and match flow
- Main app module: app
- Entry point: app/src/main/java/com/example/scrapsetu/MainActivity.kt
- Application class: app/src/main/java/com/example/scrapsetu/scrapSetuApp.kt

## Core Stack
- Language: Kotlin
- UI: Jetpack Compose + Material3
- Dependency injection: Hilt
- Backend: Supabase (Auth, Postgrest, Storage)
- AI integration: Groq chat completions via Ktor
- Build: Gradle Kotlin DSL
- Min SDK: 24
- Target and compile SDK: 36

## Build and Run
Run from repository root:

```powershell
.\gradlew.bat assembleDebug
```

Useful checks:

```powershell
.\gradlew.bat test
.\gradlew.bat lintDebug
```

Supabase rollout and verification directions:
- supabase/ROLL_OUT_DIRECTIONS.md

## Required Local Configuration
The app expects these values in local.properties and injects them through BuildConfig fields in app/build.gradle.kts:
- SUPABASE_URL
- SUPABASE_KEY
- GROQ_API_KEY

Do not hardcode secrets in source files.

## High-Value Code Map
- Navigation graph and routes:
  - app/src/main/java/com/example/scrapsetu/view/navigation/NavGraph.kt
- Data models:
  - app/src/main/java/com/example/scrapsetu/data/model/user.kt
  - app/src/main/java/com/example/scrapsetu/data/model/Listings.kt
  - app/src/main/java/com/example/scrapsetu/data/model/Match.kt
- Supabase client provider:
  - app/src/main/java/com/example/scrapsetu/data/remote/SupabaseClient.kt
- Repositories:
  - app/src/main/java/com/example/scrapsetu/data/repo/AuthRepository.kt
  - app/src/main/java/com/example/scrapsetu/data/repo/ListingRepository.kt
  - app/src/main/java/com/example/scrapsetu/data/repo/MatchRepository.kt
  - app/src/main/java/com/example/scrapsetu/data/repo/StorageRepository.kt
  - app/src/main/java/com/example/scrapsetu/data/repo/GroqRepository.kt
- ViewModels:
  - app/src/main/java/com/example/scrapsetu/vm/AuthViewModel.kt
  - app/src/main/java/com/example/scrapsetu/vm/ListingViewModel.kt
  - app/src/main/java/com/example/scrapsetu/vm/MatchViewModel.kt
  - app/src/main/java/com/example/scrapsetu/vm/GroqViewModel.kt

## Functional Flow Summary
1. User signs in or signs up through AuthRepository.
2. Role is loaded from users table and UI routes to supplier or buyer dashboard.
3. Supplier creates listings, optionally uploads image to Supabase Storage bucket images.
4. Buyer browses active listings and creates match requests.
5. Supplier views incoming matches and updates match status.
6. Groq endpoint is used for short buyer/recycler suggestion text for a listing.

## Analytics Runtime Flow (Groq + Fallback)
The analytics pipeline is intentionally two-stage: try live Groq first, then deterministic fallback from marketplace data.

Entry points (screen-level triggers):
- Buyer dashboard and smart-match dialog:
  - app/src/main/java/com/example/scrapsetu/view/screens/buyer/BuyerDashboardScreen.kt
- Supplier dashboard and add-listing panel:
  - app/src/main/java/com/example/scrapsetu/view/screens/supplier/SupplierDashboardScreen.kt
- Supplier match screen:
  - app/src/main/java/com/example/scrapsetu/view/screens/supplier/SupplierMatchScreen.kt
- Profile trust view:
  - app/src/main/java/com/example/scrapsetu/view/screens/auth/ProfileScreen.kt

Primary data/state path:
1. Screen calls GroqAnalyticsViewModel.load(userId, listingId?)
2. ViewModel calls GroqAnalyticsRepository.fetchAnalytics(...)
3. Repository fetches context from Supabase:
   - users (current user)
   - listings (supplier listings)
   - matches (recent by listing ids)
4. Repository builds a strict JSON prompt and calls Groq chat completions API.
5. Repository parses response with resilient extraction.
6. If parsing succeeds:
   - return AnalyticsFetchResult(source=GROQ, data=AnalyticsResponse)
7. If Groq is empty/malformed/unusable:
   - generate deterministic fallback metrics from listings + matches + user stats
   - return AnalyticsFetchResult(source=FALLBACK, data=AnalyticsResponse)
8. ViewModel publishes AnalyticsUiState.Success(data, source)
9. AnalyticsShell renders source badge and downstream cards.

Source-of-truth indicator in UI:
- Shared analytics component renders runtime source badge:
  - app/src/main/java/com/example/scrapsetu/view/components/AnalyticsComponents.kt
- Badge values:
  - AI Source: Groq
  - AI Source: Fallback

Key implementation files:
- Repository and Groq/fallback orchestration:
  - app/src/main/java/com/example/scrapsetu/data/repo/GroqAnalyticsRepository.kt
- Analytics state source metadata:
  - app/src/main/java/com/example/scrapsetu/vm/GroqAnalyticsViewModel.kt
- Shared analytics shell and source badge:
  - app/src/main/java/com/example/scrapsetu/view/components/AnalyticsComponents.kt

Operational behavior guarantees:
- UI widgets should remain populated even when Groq returns empty content.
- Error state should only surface when both Groq and local fallback construction fail.
- In-flight request dedupe remains key-based at ViewModel level (userId:listingId).

Verification checklist for analytics source:
1. Trigger any analytics card in app.
2. Observe source badge in analytics shell.
3. Confirm "AI Source: Groq" during healthy API response.
4. Simulate API/key/network issue and confirm "AI Source: Fallback" renders with usable metrics.
5. Ensure no "AI unavailable" message appears for normal fallback scenarios.

Auth ordering note:
- Login routing must only happen after AuthState.Success and resolved user role.
- Direct UI bypass from login to buyer or supplier dashboard is disallowed.

## Data and Migration Notes
- Current migration folder: supabase/migrations
- Existing migration adds image_url to listings:
  - supabase/migrations/20260319_add_image_url_to_listings.sql

Current Listing model includes imageUrl mapped to image_url in Supabase.

## Practical Guardrails For AI Changes
- Avoid editing generated or build output directories:
  - app/build
  - build
- Keep repository and ViewModel APIs stable unless task explicitly requires breaking change.
- Prefer minimal, focused edits over broad refactors.
- When changing schema-related code, verify model serial names align with table columns.
- Preserve user-scoped image storage path pattern in StorageRepository:
  - supplierId/uuid.extension
  This is important for common Supabase RLS storage policies.
- Keep navigation callback wiring centralized in NavGraph and pass callbacks into screens, instead of local route calls inside composables.
- Use top-level navigation with launchSingleTop and state restore semantics for bottom/top navigation actions.

## Known Technical Debt and Risks
- Inconsistent file naming style exists (example: user.kt, Listings.kt); keep naming consistent within touched feature area.
- Some repository methods fetch broadly then filter in memory (supplier match lookup path); watch for scale impact.
- README has broad context but may lag implementation details. Prefer source-of-truth from current Kotlin files.
- Some non-routing UI actions are still placeholders (for example filter/report/chat/download style actions); add explicit behavior or route before release.
- Request fan-out can happen if effects are keyed too broadly; prefer stable keys (like listing IDs) plus ViewModel in-flight guards.

## User-Reported Product Gaps (2026-03-20)
- Seller-side listing action button labeled as analytics was not wired to a meaningful flow.
- Some bottom navigation actions were routed to the same screen and felt non-functional.
- Profile icon flow was miswired to login/sign-out path instead of opening a profile page.
- Match request cards were over-expanded and surfaced raw IDs instead of useful business context.
- Buyer/seller identity should come from users table data and appear in listing and match cards.
- Buyer dashboard contained static demo values and weak trust signals (for example fixed smart match accuracy).
- Route language should reflect role intent:
  - Buyer-facing primary surface should be dashboard-oriented.
  - Seller-facing primary surface should be marketplace-oriented.
- AI-driven features need stronger real-data grounding and less placeholder copy.
- Smart Match dialog still shows hardcoded reliability and ETA values instead of model or historical outputs.
- Listing creation uses free-text waste type; it should use a controlled category list (for example e-waste, metal scrap, plastic pallets, paper, glass, rubber, textile, organic, mixed).
- Location input is too loose; input should support structured hierarchy (state, district, town/city).
- Multiple buyer requests for the same listing should be surfaced with a visible request count and grouped request view.
- Profile screen should display real user details from users table (at minimum name, role, email, location) instead of role-only summary.

## Data and Tables Needed For Pending Features
- Waste category master source:
  - Option A: new table (recommended) with id, slug, label, active flag, sort order.
  - Option B: confirmed fixed enum list in product spec.
- Structured location source:
  - Option A: normalized tables (states, districts, towns) with foreign keys.
  - Option B: static JSON hierarchy with code + label for state/district/town.
- Smart Match metrics source:
  - Clarify whether reliability and ETA come from Groq response, deterministic scoring service, or historical match analytics table.
- Multi-request aggregation source:
  - Confirm if aggregation is based only on matches table by listing_id and status, or if separate request summary materialized view/table exists.

## Product Decisions Confirmed (2026-03-20)
- Decision 1 (waste categories): use a database-backed master table approach for better scale and shared use across Android app and website.
  - Recommended table: waste_categories(id, slug, label, active, sort_order, created_at)
  - Listing rows should reference category by key/slug instead of free-text type.
- Decision 2 (location): India-centered hierarchy is required.
  - Use normalized location tables for india_states and india_districts.
  - Town/city can be phase-2 via india_cities table or API-backed lookup.
  - users table can keep user-selected location references (state_code, district_code, city_name) for profile and filtering.
- Decision 3 (smart match metrics): reliability and ETA will come from a custom Python pipeline.
  - Build a custom ipynb/python service layer that combines historical data + APIs.
  - App should consume this via API contract rather than hardcoded UI constants.
- Decision 4 (multi-request aggregation rule): deferred by user for now.
  - Keep as open design item until rule is finalized.

Implementation direction recorded for future edits:
- Prioritize replacing placeholder actions with real routes, server-backed operations, or explicit short-term feedback.
- Keep role-based routing strict and avoid profile/logout coupling bugs.
- Reduce card visual density and remove opaque IDs from primary UI surfaces.
- Build KPI cards from live repository data; avoid hardcoded business metrics.
- Replace free-text waste type with selector UI bound to controlled data source.
- Convert location capture to structured state/district/town flow with validation.
- Add per-listing request aggregation and visual count chips in seller views.
- Bind profile UI to users table fields and keep auth/session role as fallback only.

## Expected AI Workflow
1. Read this file first.
2. Read target feature files from the code map above.
3. Apply minimal patch.
4. Run relevant Gradle checks for touched area.
5. Summarize changes, risks, and test status.

## Definition of Done For Most Changes
- Code compiles for debug build.
- No new lint or test break caused by the change set in touched scope.
- Behavior aligns with role-based routing and Supabase data model.