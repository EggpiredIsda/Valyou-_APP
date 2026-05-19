Valyou
Smart Property Valuation · Philippines

A native Android application that gives buyers, sellers, and appraisers instant market valuation estimates for properties in Cebu City — built as a frontend mockup with clean backend-ready architecture.


Overview
Valyou lets users explore an interactive heat map of Cebu City's real estate market, tap into any neighbourhood, and instantly see estimated property values, price-per-sqm ranges, 30-day market trends, and a list of comparable nearby listings — all in one screen.
The current version runs on mock data structured to mirror the ONEProperty PH API response shape. Swapping in the real backend requires changing only the stub functions marked // TODO in MainActivity.kt.

Screenshots
LoginMap ViewValuation PanelDark themed sign-in with animated entranceOSMDroid map of Cebu with colour-coded heat clustersBottom sheet with live count-up value animation

Features
Authentication

Email + password sign-in with inline validation
Staggered entrance animation on load
Forgot password and sign-up links (backend hooks ready)

Interactive Map

Full OSMDroid map (OpenStreetMap tiles — no API key required)
Heat clusters colour-coded by market tier:

🔴 Red — High value (₱150k–260k / sqm)
🟠 Orange — Mid value (₱60k–145k / sqm)
🟡 Amber — Lower value (₱35k–80k / sqm)


Dual-ring clusters (outer glow + inner solid) with haversine-based tap detection
White selection ring highlights the active area
Smooth zoom-to-area animation on selection
Custom zoom buttons (OSMDroid built-in buttons hidden)

Search

Live-filter search with autocomplete dropdown
Matches any part of an area name
Selecting a suggestion flies the map to that area and opens the panel

Valuation Panel (Bottom Sheet)

Slides up from the map at 56% screen height (draggable to full screen)
Animated count-up on the estimated value (PHP X.XXM)
Valuation range (low–high)
Price per sqm range
30-day market trend with directional indicator
Nearby listings count within 3 km radius
Staggered property cards (House / Condo / Lot) with price and distance


Covered Areas (Mock Data)
AreaEst. Value (mid)Price / sqmTrendIT Park, Cebu CityPHP 14.0M150k – 210k+3.8%Ayala Center, CebuPHP 17.0M180k – 260k+4.2%Lahug, Cebu CityPHP 9.0M80k – 130k+2.1%Banilad, Cebu CityPHP 10.0M90k – 140k+1.9%Mabolo, Cebu CityPHP 10.5M95k – 145k+2.5%Guadalupe, Cebu CityPHP 8.0M70k – 110k+1.5%Talamban, Cebu CityPHP 5.2M45k – 80k+1.2%Mandaue CityPHP 6.5M60k – 100k+2.0%Lapu-Lapu / MactanPHP 7.0M55k – 120k+3.1%Talisay CityPHP 4.0M35k – 65k+0.8%Pit-os / Busay, CebuPHP 4.7M40k – 75k+0.5%
Data sourced from ONEProperty PH listing structure. Values are indicative estimates for mockup purposes.

Tech Stack
ComponentLibrary / ToolLanguageKotlinUIAndroid Views + ViewBindingDesign systemMaterial Components 3 (Dark theme)MapOSMDroid 6.1.17 (OpenStreetMap)Build systemGradle 8.6Android Gradle Plugin8.3.2Kotlin1.9.22Min SDK24 (Android 7.0)Target SDK34 (Android 14)

Project Structure
app/src/main/
├── java/com/estimaph/app/
│   ├── LoginActivity.kt       ← Sign-in screen + animations
│   └── MainActivity.kt        ← Map, search, valuation panel
├── res/
│   ├── layout/
│   │   ├── activity_login.xml
│   │   └── activity_main.xml  ← Map + bottom sheet
│   ├── drawable/              ← Vector icons + shape backgrounds
│   └── values/
│       ├── colors.xml
│       └── themes.xml
gradle/
├── wrapper/
│   └── gradle-wrapper.properties
gradle.properties
settings.gradle

Getting Started
Requirements

Android Studio Hedgehog (2023.1.1) or later
Android SDK 34
JDK 11 (bundled with Android Studio)
Internet connection (first run downloads OSMDroid map tiles)

Run
bash# 1. Open the project
File → Open → select the "ValYou - Stable Release" folder

# 2. Wait for Gradle sync to complete

# 3. Create an emulator (if needed)
Tools → Device Manager → Create Device → Pixel 6 → API 34 (Android 14)

# 4. Run
Shift + F10  (or press the green ▶ Play button)
Login
Any valid email + password of 6+ characters will sign in (mock auth). No real credentials needed.

Backend Integration
The app is architected so the real ONEProperty PH API can be dropped in with minimal changes.
What to replace
MainActivity.kt — two stub functions:
kotlin// 1. Area search
private fun searchAreas(q: String): List<Area> {
    // TODO: Replace with:
    // ApiService.searchAreas(q, callback = ...)
    return AREAS.filter { it.name.lowercase().contains(q.lowercase()) }
}

// 2. Area valuation (called when user taps a map cluster)
// Currently resolved via nearest-neighbour on the mock AREAS list.
// Replace with:
// ApiService.getValuation(lat, lon) { area -> showPanel(area) }
LoginActivity.kt:
kotlin// Replace mock delay with:
// authRepo.login(email, pass, onSuccess = ::navigateToMain, onError = ::showError)
API Response Shape
The Area data class in MainActivity.kt defines the expected API shape:
kotlindata class Area(
    val name: String,
    val lat: Double,  val lon: Double,
    val estMid: Double,               // PHP millions — midpoint estimate
    val estLow: Double,               // PHP millions — low end
    val estHigh: Double,              // PHP millions — high end
    val sqmLow: Int,                  // PHP / sqm — low
    val sqmHigh: Int,                 // PHP / sqm — high
    val trend: Double,                // % change over 30 days
    val listings: Int,                // count within 3 km
    val houseM: Double,               // avg house price (PHP millions)
    val condoM: Double,               // avg condo price (PHP millions)
    val lotSqm: Int,                  // avg lot price (PHP / sqm)
    val heat: Int                     // 0 = low · 1 = mid · 2 = high
)

Color Scheme
SwatchHexUsage🔴 Brand Red#C3110FPrimary actions, prices, heat clusters (high)⚫ Brand Dark#242420Backgrounds, surfaces⚪ Brand Gray#D9D9D9Secondary text, borders

Known Limitations (Mockup)

Auth is not real — any valid-format credentials are accepted
Map data is static — 11 hardcoded Cebu areas; no live scraping yet
Valuation is estimated — based on median comparable pricing, not a certified appraisal
No saved pins / history — planned for backend integration phase
Cebu City only — nationwide PH coverage planned post-backend
