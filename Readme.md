<div align="center">

# 🌿 ScrapSetu

### B2B Industrial Waste Marketplace

**Connecting industrial waste suppliers directly with recyclers.**  
No middlemen. Transparent pricing. AI-powered matching.

[![Download APK](https://img.shields.io/badge/Download%20APK-FinalVersion-1B4332?style=for-the-badge&logo=android&logoColor=white)](https://github.com/ADHDcoder0/Hack4IMPACTTrack2-MARKK-CORP/releases/download/FinalVersion/app-debug.apk)
[![Live Website](https://img.shields.io/badge/Live%20Website-Vercel-40916C?style=for-the-badge&logo=vercel&logoColor=white)](https://hack4-imapct-markk-corp.vercel.app/)

**Team MARKKCORP** — Hack4IMPACT Track 2 · Green Infrastructure & Smart Cities

---

## Related Repos

| Repo | Link |
|---|---|
| 🌐 Web Frontend | [Hack4Imapct_MARKK-CORP-WEB](https://github.com/krishna02405/Hack4Imapct_MARKK-CORP-WEB) |
| 🌿 Live Website | [hack4-imapct-markk-corp.vercel.app](https://hack4-imapct-markk-corp.vercel.app/) |

---

Kushagra (2406037) · Krishna Agarwal (2406035) · Manglam Agarwal (2406130) · Rakshit Trivedi (2428039)

</div>

---

## 📲 Download & Install

| | |
|---|---|
| **APK** | [Download app-debug.apk](https://github.com/ADHDcoder0/Hack4IMPACTTrack2-MARKK-CORP/releases/download/FinalVersion/app-debug.apk) |
| **Min Android** | 7.0 Nougat (API 24+) |
| **Storage** | ~20 MB |
| **Internet** | Required (Supabase + Groq AI) |

**Steps:**
1. Download the APK from the link above
2. Transfer to Android device (or open from mobile browser)
3. Tap to install — enable **Install from Unknown Sources** if prompted
   *(Settings → Security → Unknown Sources)*

> ⚠️ Sideloaded APK — not from Play Store. Unknown sources must be enabled.

---

## Overview

ScrapSetu is a B2B waste marketplace Android app built for Hack4IMPACT Track 2 (Green Infrastructure & Smart Cities). It connects industrial waste **suppliers** with **recyclers/buyers**, eliminating middlemen and using Groq AI to intelligently match materials with the right buyers.

**The problem:** Industrial waste disposal is fragmented, opaque, and dominated by brokers who inflate costs and obscure pricing.

**The solution:** A direct marketplace with role-based flows for suppliers and buyers, transparent listing prices, and AI-driven match suggestions.

---

## Features

| Feature | Status |
|---|---|
| Auth — register, login, logout | ✅ |
| Role-based navigation (supplier / buyer) | ✅ |
| Input validation across all forms | ✅ |
| Supplier dashboard — create & view listings | ✅ |
| Buyer dashboard — browse & search listings | ✅ |
| Request Match | ✅ |
| Smart Match — Groq AI suggestion | ✅ |
| Match status screen (buyer) | ✅ |
| Supplier match management — confirm / reject | ✅ |
| Pull-to-refresh on both dashboards | ✅ |
| ScrapSetu color theme | ✅ |
| BuildConfig secrets management | ✅ |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.00) |
| Dependency Injection | Hilt 2.52 |
| Backend | Supabase 3.0.0 (PostgREST + Auth) |
| HTTP Client | Ktor 3.1.2 |
| AI Matching | Groq API — `llama-3.3-70b-versatile` |
| Navigation | Jetpack Navigation Compose 2.7.7 |
| Build | AGP 8.9.1 / KSP 2.0.21-1.0.28 |
| Min / Target SDK | 24 / 36 |

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

## Configuration

Secrets in `local.properties` (not committed), exposed via `BuildConfig`:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your_publishable_key
GROQ_API_KEY=gsk_...
```

Enable `buildConfig = true` in `buildFeatures` inside `app/build.gradle.kts`.

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



<div align="center">

Built with ♻️ for Hack4IMPACT · Team MARKKCORP · KIIT 2025

</div>
