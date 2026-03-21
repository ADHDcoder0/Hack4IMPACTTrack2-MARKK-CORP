# 🌿 ScrapSetu — B2B Waste Marketplace

> Connecting industrial waste suppliers directly with recyclers. No middlemen. Transparent pricing. AI-powered matching.

**Team MARKKCORP** | Kushagra (2406037) · Krishna Agarwal (2406035) · Manglam Agarwal (2406130) · Rakshit Trivedi (2428039)

---

## 📦 Download the App

The compiled release APK is available in the **[Releases](../../releases)** section of this repository.

### How to install

1. Go to the **[Releases](../../releases)** tab on the right sidebar of this repo (or click the link).
2. Click the **latest release** (tagged `v1.0` or higher).
3. Under **Assets**, download **`scrapsetu-release.apk`**.
4. Transfer the APK to your Android device (or open directly from mobile browser).
5. Tap the file to install. If prompted, enable **Install from Unknown Sources** in Settings → Security.

> ⚠️ **Note:** This is a sideloaded APK (not from Play Store). You must allow installation from unknown sources before installing.

### Requirements

| Requirement | Details |
|---|---|
| Android Version | 7.0 (Nougat) or higher — API 24+ |
| Internet | Required (Supabase backend + Groq AI) |
| Storage | ~20 MB free space |

---

## Overview

ScrapSetu is a B2B waste marketplace Android app that connects industrial waste **suppliers** with **recyclers/buyers**. It eliminates middlemen, provides transparent pricing, and uses Groq AI smart matching to pair the right materials with the right buyers.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.00) |
| Dependency Injection | Hilt 2.52 |
| Backend | Supabase 3.0.0 (Postgrest + Auth) |
| HTTP Client | Ktor 3.1.2 |
| AI Matching | Groq API — `llama-3.3-70b-versatile` |
| Navigation | Jetpack Navigation Compose 2.7.7 |
| Build | AGP 8.9.1 / KSP 2.0.21-1.0.28 |
| Min / Target SDK | 24 / 36 |

---

## Features

- [x] Auth — register, login, logout
- [x] Role-based navigation (supplier / buyer)
- [x] Input validation across all forms
- [x] Supplier dashboard — create and view listings
- [x] Buyer dashboard — browse and search listings
- [x] Request Match
- [x] Smart Match — Groq AI suggestion
- [x] Match status screen (buyer)
- [x] Supplier match management — confirm / reject
- [x] ScrapSetu color theme
- [x] Pull-to-refresh on both dashboards
- [x] BuildConfig secrets management
- [ ] Fix: sign out does not clear userRole state
- [ ] Listing validation in AddListingDialog
- [ ] App icon + splash screen
- [ ] Play Store prep

---

## Database Schema (Supabase)

<details>
<summary><strong>users</strong></summary>

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| email | String | Unique auth email |
| role | String | `"supplier"` or `"buyer"` |
| name | String | Display name |
| location | String | City / region |
| created_at | String | ISO timestamp |

</details>

<details>
<summary><strong>listings</strong></summary>

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| supplier_id | String (FK) | → users.id |
| waste_type | String | e.g. Plastic, Metal, Paper |
| quantity_kg | Double | Weight in kilograms |
| price_per_kg | Double | Price in INR per kg |
| location | String | Pickup location |
| status | String | `"active"` / `"matched"` / `"closed"` |
| description | String | Additional details |
| created_at | String | ISO timestamp |

</details>

<details>
<summary><strong>matches</strong></summary>

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| listing_id | String (FK) | → listings.id |
| buyer_id | String (FK) | → users.id |
| status | String | `"pending"` / `"confirmed"` / `"rejected"` |
| created_at | String | ISO timestamp |

</details>

---

## Architecture

MVVM with clean layered separation — data → repository → ViewModel → Compose UI.

```
com.example.scrapsetu
├── data
│   ├── model         # User.kt, Listing.kt, Match.kt
│   ├── remote        # SupabaseClientProvider.kt
│   └── repo          # AuthRepository, ListingRepository, MatchRepository, GroqRepository
├── di                # AppModule.kt (Hilt)
├── ui/theme          # Color.kt, Theme.kt, Type.kt
├── view
│   ├── navigation    # NavGraph.kt
│   └── screens
│       ├── auth      # LoginScreen, RegisterScreen
│       ├── supplier  # SupplierDashboardScreen, SupplierMatchScreen
│       └── buyer     # BuyerDashboardScreen, MatchStatusScreen
├── vm                # AuthViewModel, ListingViewModel, MatchViewModel, GroqViewModel
├── MainActivity.kt
└── ScrapSetuApp.kt
```

### Navigation Routes

| Route | Screen |
|---|---|
| `login` | LoginScreen |
| `register` | RegisterScreen |
| `supplier_dashboard` | SupplierDashboardScreen |
| `buyer_dashboard` | BuyerDashboardScreen |
| `match_status` | MatchStatusScreen |
| `supplier_matches` | SupplierMatchScreen |

After login, `AuthViewModel.loadUserRole()` fetches the user's role from Supabase and routes to the appropriate dashboard.

---

## Key Dependencies

```kotlin
// Ktor
ktor-client-core:3.1.2
ktor-client-android:3.1.2
ktor-client-content-negotiation:3.1.2
ktor-serialization-kotlinx-json:3.1.2

// Supabase
supabase-kt:3.0.0
postgrest-kt:3.0.0
auth-kt:3.0.0

// Hilt
hilt-android:2.52
hilt-compiler:2.52 (ksp)
hilt-navigation-compose:1.2.0

// Compose BOM: 2024.09.00
// Navigation Compose: 2.7.7
// ViewModel Compose: 2.7.0
// Coroutines Android: 1.7.3
// Serialization JSON: 1.6.3
// Material (pull-to-refresh): 1.7.0
```

---

## Configuration

Secrets are stored in `local.properties` (not committed to version control) and exposed via `BuildConfig`:

```
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your_publishable_key
GROQ_API_KEY=gsk_...
```

Enable `buildConfig = true` in `buildFeatures` inside `app/build.gradle.kts`.
