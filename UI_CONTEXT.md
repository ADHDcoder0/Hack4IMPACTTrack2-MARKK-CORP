# ScrapSetu UI Context

Last updated: 2026-03-20

## Purpose
This document is a UI-only context for AI agents working on Jetpack Compose screens, theme, and navigation behavior.

## UI Architecture
- UI toolkit: Jetpack Compose with Material3 components
- App-level theme wrapper: app/src/main/java/com/example/scrapsetu/ui/theme/Theme.kt
- Navigation host: app/src/main/java/com/example/scrapsetu/view/navigation/NavGraph.kt
- State source: ViewModels in app/src/main/java/com/example/scrapsetu/vm, collected with collectAsState in composables

## Theme and Visual Tokens
- Color source: app/src/main/java/com/example/scrapsetu/ui/theme/Color.kt
- Typography source: app/src/main/java/com/example/scrapsetu/ui/theme/Type.kt
- Theme setup: app/src/main/java/com/example/scrapsetu/ui/theme/Theme.kt

Current palette direction:
- Primary dark green, secondary green, light green background, orange tertiary accent
- Surface and on-surface are set for light theme usage
- Status bar color is explicitly set to primary dark green in theme side effect

Typography notes:
- Uses default Material Typography with mostly default definitions
- Body large is explicitly set in Type.kt

## Navigation and Screen Map
Defined in app/src/main/java/com/example/scrapsetu/view/navigation/NavGraph.kt.

Routes:
- login
- register
- supplier_dashboard
- buyer_dashboard
- match_status
- supplier_matches

Role flow:
- Login screen triggers sign-in
- On auth success, AuthViewModel loads user role
- Role routes user to supplier or buyer dashboard
- Login no longer allows direct buyer/supplier entry without successful auth

## Screen Behavior Details

### Login
File: app/src/main/java/com/example/scrapsetu/view/screens/auth/LoginScreen.kt

Layout and interactions:
- Centered column with brand title, subtitle, email and password fields
- Client-side validation for email format and minimum password length
- Shows loading state in primary button
- Shows auth error text from AuthState.Error
- Register text action navigates to register screen
- Dashboard navigation is gated: only after AuthState.Success and role fetch
- Quick role bypass buttons were removed to enforce authentication order

State dependencies:
- AuthViewModel.authState
- AuthViewModel.userRole
- Uses a combined effect keyed by authState and userRole to enforce auth-first routing

### Register
File: app/src/main/java/com/example/scrapsetu/view/screens/auth/RegisterScreen.kt

Layout and interactions:
- Input fields: full name, email, password, location
- Role selection via FilterChip for supplier or buyer
- Client-side form validation
- Loading indicator inside Register button
- Login text action returns to login

State dependencies:
- AuthViewModel.authState
- LaunchedEffect navigates on AuthState.Success

### Buyer Dashboard
File: app/src/main/java/com/example/scrapsetu/view/screens/buyer/BuyerDashboardScreen.kt

Layout and interactions:
- Top app bar with icon-only actions (notifications, sign out)
- Glass-style icon bottom nav with Home active state and My Matches action
- Search field filters by wasteType and location
- Pull-to-refresh reloads active listings
- Listing cards show image, quantity, price, location, description
- Two actions per listing:
  - Request Match
  - Smart Match (opens dialog and calls Groq)
- Snackbar feedback for match success and error
- Top and bottom navigation actions are callback-driven from NavGraph

Dialog behavior:
- Smart match dialog uses a rounded card style and renders loading, success text, or error text based on GroqState
- Request Match action in dialog submits and closes dialog

### Buyer Match Status
File: app/src/main/java/com/example/scrapsetu/view/screens/buyer/MatchStatusScreen.kt

Layout and interactions:
- Top app bar with Back action
- Loads user matches on first composition
- Shows loading indicator, empty state, or lazy list of match cards
- Bottom nav actions and sign-out are routed through NavGraph callbacks
- Match badge color reflects status:
  - confirmed: secondary color
  - rejected: errorContainer with onErrorContainer text
  - pending: softened tertiary container

### Supplier Dashboard
File: app/src/main/java/com/example/scrapsetu/view/screens/supplier/SupplierDashboardScreen.kt

Layout and interactions:
- Top app bar with icon actions (match requests, sign out)
- Floating action button opens add listing dialog
- Pull-to-refresh loads supplier listings
- Listing cards show image (if present), pricing, location, status, description
- Top and bottom navigation actions are callback-driven from NavGraph

Add listing dialog behavior:
- Collects waste type, quantity, price, location, description
- Allows image pick from gallery via ActivityResult GetContent(image/*)
- Reads image bytes and mime type from content resolver
- Shows selected image preview
- Submit passes values to createListingWithImage

### Supplier Match Requests
File: app/src/main/java/com/example/scrapsetu/view/screens/supplier/SupplierMatchScreen.kt

Layout and interactions:
- Top app bar with Back action
- Loads supplier listings, then loads matches for those listing ids
- Shows loading indicator, empty state, or list of match request cards
- Pending cards expose Confirm and Reject actions
- Confirm and Reject call updateMatch with confirmed or rejected
- Status chips follow same confirmed/rejected/pending token mapping as buyer match screen
- Bottom nav actions and sign-out are routed through NavGraph callbacks

## Shared UI Patterns
- Most pages use Scaffold with TopAppBar
- Lists use LazyColumn with card rows and spacing
- Empty and loading states are shown inline in screen body
- Actions use Button or OutlinedButton depending on emphasis
- Async images loaded with Coil AsyncImage

## Dependencies Used by UI Layer
- Compose Material3
- Compose Material pull refresh APIs
- Navigation Compose
- Hilt navigation compose
- Coil compose

## UI Risks and Implementation Notes
- Login and register forms are centered; on very small screens, keyboard overlap can reduce usability because there is no vertical scroll container.
- Add listing dialog currently allows empty or zero numeric values; validation can be tightened in UI before submit.
- Match list cards display shortened ids rather than contextual names, which reduces readability for end users.
- Pull refresh relies on Loading state; ensure future state changes preserve refresh semantics.
- Several non-routing actions remain placeholders (for example filter/report/chat/download/review style actions) and should be explicitly implemented before release.

## Open UI Feature Gaps (2026-03-20)
- Smart Match dialog currently renders hardcoded values for reliability and ETA instead of live AI/data-backed outputs.
- Add Listing flow uses plain text for waste type; replace with selectable category options.
- Category options should cover common streams (example baseline):
  - e-waste
  - metal scrap
  - plastic pallets
  - paper/cardboard
  - glass
  - rubber
  - textile
  - organic
  - mixed waste
- Location input should be structured and supervised as state -> district -> town/city instead of one free-text field.
- Supplier listing/match views should clearly show multiple requests against the same listing (count badge + grouped request details).
- Profile page should show users table data (name, role, email, location) and not only route/role hint values.

## UI Data Dependencies For Above Gaps
- Waste category source (table or fixed spec list) for dropdown/chips.
- State/district/town dataset (table-backed or static hierarchy JSON) for cascading selectors.
- Smart Match metric contract that returns:
  - reliability score
  - ETA match window
  - optional confidence note/source
- Match aggregation logic for per-listing request count from matches table.

## Confirmed Direction (2026-03-20)
- Waste type selector must be backed by a central database category source so both app and website use one canonical list.
- Location selector must be India-first with structured hierarchy:
  - state
  - district
  - town/city (phase-2 can be API-fed if full static dataset is too heavy)
- Smart Match reliability and ETA must come from a custom Python notebook/script pipeline exposed through APIs.
- Multiple-request display requirement remains in backlog; aggregation rule is intentionally deferred for now.

## UI Notes For Upcoming Build
- Replace free-text waste type input with dropdown/chips sourced from waste_categories.
- Replace single location text field with cascading selectors for india_states -> india_districts -> city/town input/select.
- Profile view should render users table identity fields and selected India location fields when available.

## UI Definition of Done Additions
- No hardcoded business metrics in Smart Match cards/dialogs where live data is expected.
- Listing form uses controlled selectors for waste category and structured location fields.
- Supplier can identify all requests per listing without reading raw IDs.
- Profile screen always renders user name when available from users table.

## AI Editing Guardrails for UI Work
- Keep route names stable unless navigation graph and all call sites are updated together.
- Prefer extracting reusable composables only when repeated in at least two screens.
- Avoid introducing hardcoded colors in screen files; use MaterialTheme colorScheme.
- Keep loading, empty, and error states explicit when changing list screens.
- Preserve role-based navigation behavior from login.

## Fast UI Change Checklist
1. Update composable and preview behavior if present.
2. Confirm state flow transitions for loading, success, and error.
3. Verify navigation actions still pop or route correctly.
4. Run assembleDebug.
5. Manually smoke test login, register, supplier add listing, buyer request match.