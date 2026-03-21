# Team MARKKCORP

# ScrapSetu — Complete Project Description and Architechture 

---

## Team Members:- 
| Kushagra | 2406037 |
| Krishna Agarwal | 2406035 |
| Manglam Agarwal | 2406130 |
| Rakshit Trivedi | 2428039 |

---

## Overview
B2B waste marketplace Android app connecting industrial waste suppliers with recyclers.
Eliminates middlemen, provides transparent pricing, smart AI-based matching via Groq.

---

## Tech Stack
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| DI | Hilt 2.52 |
| Backend | Supabase 3.0.0 |
| HTTP Client | Ktor 3.1.2 |
| AI Matching | Groq API (llama-3.3-70b-versatile) |
| Navigation | Jetpack Navigation Compose 2.7.7 |
| Build | AGP 8.9.1 |
| Kotlin Version | 2.0.21 |
| KSP | 2.0.21-1.0.28 |
| Min SDK | 24 |
| Target/Compile SDK | 36 |

---

## Supabase
- **Project URL:** `https://zriijjrfd:::::::::gsow.supabase.co`
- **Publishable Key:** `sb_publish::::::::::erEx_mmwzJ1qSA_Au8Myd3n`

### Tables

#### users
| Column | Type |
|---|---|
| id | String (UUID) |
| email | String |
| role | String ("supplier" / "buyer") |
| name | String |
| location | String |
| created_at | String |

#### listings
| Column | Type |
|---|---|
| id | String (UUID) |
| supplier_id | String (FK → users.id) |
| waste_type | String |
| quantity_kg | Double |
| price_per_kg | Double |
| location | String |
| status | String ("active" / "matched" / "closed") |
| description | String |
| created_at | String |

#### matches
| Column | Type |
|---|---|
| id | String (UUID) |
| listing_id | String (FK → listings.id) |
| buyer_id | String (FK → users.id) |
| status | String ("pending" / "confirmed" / "rejected") |
| created_at | String |

---

## Package Structure
```
com.example.scrapsetu
├── data
│   ├── model
│   │   ├── User.kt
│   │   ├── Listing.kt
│   │   └── Match.kt
│   ├── remote
│   │   └── SupabaseClientProvider.kt
│   └── repo
│       ├── AuthRepository.kt
│       ├── ListingRepository.kt
│       ├── MatchRepository.kt
│       └── GroqRepository.kt
├── di
│   └── AppModule.kt
├── ui
│   └── theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── view
│   ├── navigation
│   │   └── NavGraph.kt
│   └── screens
│       ├── auth
│       │   ├── LoginScreen.kt
│       │   └── RegisterScreen.kt
│       ├── supplier
│       │   ├── SupplierDashboardScreen.kt
│       │   └── SupplierMatchScreen.kt
│       └── buyer
│           ├── BuyerDashboardScreen.kt
│           └── MatchStatusScreen.kt
├── vm
│   ├── AuthViewModel.kt       (+ AuthState sealed class)
│   ├── ListingViewModel.kt    (+ ListingState sealed class)
│   ├── MatchViewModel.kt      (+ MatchState sealed class)
│   └── GroqViewModel.kt       (+ GroqState sealed class)
├── MainActivity.kt
└── ScrapSetuApp.kt
```

---

## Navigation Routes
| Route | Screen |
|---|---|
| `login` | LoginScreen |
| `register` | RegisterScreen |
| `supplier_dashboard` | SupplierDashboardScreen |
| `buyer_dashboard` | BuyerDashboardScreen |
| `match_status` | MatchStatusScreen |
| `supplier_matches` | SupplierMatchScreen |

**Role-based routing:** After login, `AuthViewModel.loadUserRole()` fetches user role from Supabase → navigates to supplier or buyer dashboard accordingly.

---

## Classes & Responsibilities

### Data Models (`data/model/`)

#### User.kt
```kotlin
@Serializable
data class User(
    val id: String,
    val email: String,
    val role: String,
    val name: String,
    val location: String,
    @SerialName("created_at") val createdAt: String
)
```

#### Listing.kt
```kotlin
@Serializable
data class Listing(
    val id: String,
    @SerialName("supplier_id") val supplierId: String,
    @SerialName("waste_type") val wasteType: String,
    @SerialName("quantity_kg") val quantityKg: Double,
    @SerialName("price_per_kg") val pricePerKg: Double,
    val location: String,
    val status: String,
    val description: String,
    @SerialName("created_at") val createdAt: String
)
```

#### Match.kt
```kotlin
@Serializable
data class Match(
    val id: String,
    @SerialName("listing_id") val listingId: String,
    @SerialName("buyer_id") val buyerId: String,
    val status: String,
    @SerialName("created_at") val createdAt: String
)
```

---

### Remote (`data/remote/`)

#### SupabaseClientProvider.kt
- Singleton object
- Installs `Postgrest` and `Auth` plugins
- Package: `com.example.scrapsetu.data.remote`

---

### Repositories (`data/repo/`)

#### AuthRepository.kt
- `signUp(email, password, name, role, location)` — creates auth user + inserts into users table
- `signIn(email, password)` — Supabase email auth
- `signOut()` — signs out current session
- `currentUserId(): String?` — returns current auth user ID
- `getCurrentUser(): User?` — fetches user record from users table

#### ListingRepository.kt
- `createListing(listing)` — inserts listing
- `getActiveListings(): List<Listing>` — fetches all status=active, ordered by created_at DESC
- `getSupplierListings(supplierId): List<Listing>` — fetches supplier's own listings
- `updateListingStatus(listingId, status)` — updates listing status

#### MatchRepository.kt
- `createMatch(listingId, buyerId)` — inserts match with status=pending
- `getBuyerMatches(buyerId): List<Match>` — fetches buyer's matches
- `updateMatchStatus(matchId, status)` — confirms or rejects match
- `getMatchesForSupplier(supplierListingIds): List<Match>` — fetches all matches for supplier's listings

#### GroqRepository.kt
- `getSmartMatchSuggestion(listing): String` — calls Groq API
- Model: `llama-3.3-70b-versatile`
- Endpoint: `https://api.groq.com/openai/v1/chat/completions`
- Sends listing details (wasteType, quantity, price, location, description) as prompt
- Returns buyer/recycler suggestion under 100 words
- Uses Ktor `HttpClient(Android)` with `ContentNegotiation` plugin

---

### DI (`di/`)

#### AppModule.kt
- `@InstallIn(SingletonComponent::class)`
- Provides: `AuthRepository`, `ListingRepository`, `MatchRepository`, `GroqRepository`
- All `@Singleton`

---

### ViewModels (`vm/`)

#### AuthViewModel.kt
- States: `AuthState` (Idle, Loading, Success, Error)
- `signIn(email, password)`
- `signUp(email, password, name, role, location)`
- `signOut()`
- `loadUserRole()` — fetches role, exposes via `userRole: StateFlow<String?>`

#### ListingViewModel.kt
- States: `ListingState` (Idle, Loading, Success, Error) — defined BEFORE class
- `loadActiveListings()` — for buyer dashboard
- `loadSupplierListings()` — for supplier dashboard
- `createListing(wasteType, qty, price, location, desc)`

#### MatchViewModel.kt
- States: `MatchState` (Idle, Loading, Success, Error) — defined BEFORE class
- `requestMatch(listingId)` — buyer requests match
- `loadMyMatches()` — buyer's match history
- `loadSupplierMatches(listingIds)` — supplier's incoming requests
- `updateMatch(matchId, status)` — confirm or reject, updates local state optimistically

#### GroqViewModel.kt
- States: `GroqState` (Idle, Loading, Success(suggestion), Error)
- `getSuggestion(listing)` — calls GroqRepository
- `reset()` — resets state to Idle

---

### Screens

#### LoginScreen.kt
- Email + password fields with validation
- Calls `viewModel.signIn()`
- Two `LaunchedEffect`: one on `authState` (triggers `loadUserRole`), one on `userRole` (navigates to supplier/buyer dashboard)
- Params: `onBuyerLogin`, `onSupplierLogin`, `onNavigateToRegister`

#### RegisterScreen.kt
- Name, email, password, location fields with validation
- Role selector: FilterChip (supplier/buyer)
- Calls `viewModel.signUp()`
- Params: `onRegisterSuccess`, `onNavigateToLogin`

#### SupplierDashboardScreen.kt
- Shows supplier's own listings in `LazyColumn`
- FAB → `AddListingDialog` (wasteType, quantity, price, location, description)
- Pull-to-refresh via `rememberPullRefreshState`
- TopAppBar: "Requests" button → `SupplierMatchScreen`, Sign Out
- Composables: `ListingCard`, `AddListingDialog`

#### SupplierMatchScreen.kt
- Shows incoming match requests for supplier's listings
- `LaunchedEffect(listings)` → loads supplier listings first, then loads matches
- Each `SupplierMatchCard` shows buyer ID, listing ID, status badge
- Pending matches show Confirm/Reject buttons
- Calls `matchViewModel.updateMatch()`

#### BuyerDashboardScreen.kt
- Browse all active listings with search filter (wasteType + location)
- Pull-to-refresh
- `BuyerListingCard`: two buttons — "Request Match" + "Smart Match"
- Smart Match → sets `selectedListing` + calls `groqViewModel.getSuggestion()`
- `AlertDialog` shows Groq suggestion with option to Request Match from dialog
- Snackbar on match success/error
- TopAppBar: "My Matches" → `MatchStatusScreen`, Sign Out

#### MatchStatusScreen.kt
- Shows buyer's match history
- `MatchCard`: shows match ID, listing ID, status badge (color-coded: confirmed=green, rejected=red, pending=orange)

---

### Theme (`ui/theme/`)

#### Colors
```
PrimaryDarkGreen = #1B4332
AccentGreen      = #40916C
LightBackground  = #D8F3DC
OrangeAccent     = #F4A261
```

#### Theme
- `lightColorScheme` with ScrapSetu colors
- Status bar color = PrimaryDarkGreen
- `ScrapSetuTheme` wraps all content in `MainActivity`

---

### Manifest & Config

#### AndroidManifest.xml
- `android:name=".ScrapSetuApp"` (Hilt application)
- `<uses-permission android:name="android.permission.INTERNET" />`

#### local.properties
```
SUPABASE_URL="https://zriijjrfdkefytkugsow.supabase.co"
SUPABASE_KEy="sb_publishable_ic_mt5ceerEx_mmwzJ1qSA_Au8Myd3n"
GROQ_API_KEY="gsk_..."
```

#### BuildConfig fields
- `SUPABASE_URL`, `SUPABASE_KEY`, `GROQ_API_KEY`
- `buildConfig = true` in `buildFeatures`

---

## Key Dependencies (app/build.gradle.kts)
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
// Serialization JSON: 1.6.3
// Coroutines Android: 1.7.3
// ViewModel Compose: 2.7.0
// Material (for pull-to-refresh): 1.7.0
```

---

## gradle.properties
```
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
```

---

## Features Completed
- [x] Auth — register, login, logout
- [x] Role-based navigation (supplier/buyer)
- [x] Input validation — all forms
- [x] Supplier dashboard — create + view listings
- [x] Buyer dashboard — browse + search listings
- [x] Request Match
- [x] Smart Match — Groq AI suggestion
- [x] Match status screen (buyer)
- [x] Supplier match management — confirm/reject
- [x] ScrapSetu color theme
- [x] Pull-to-refresh (both dashboards)
- [x] BuildConfig secrets management

## Remaining / Future
- [ ] Sign out clears userRole state bug
- [ ] Listing validation in AddListingDialog
- [ ] App icon + splash screen
- [ ] Play Store prep
