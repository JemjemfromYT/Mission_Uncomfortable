/**
 * MainActivity.kt
 *
 * The single Activity for Mission: Uncomfortable.
 * Acts as the app's entry point and hands off all screen routing to MissionNavGraph.
 *
 * ─── ARCHITECTURE ─────────────────────────────────────────────────────────────
 *
 *   v1–v4 used a simple `var showDashboard by mutableStateOf(...)` to switch between
 *   WelcomeScreen and DashboardScreen. That approach was the right call for two screens —
 *   NavController adds unnecessary boilerplate below three destinations.
 *
 *   v5 (Phase 8) migrates to Jetpack Compose Navigation (NavController + NavHost),
 *   implemented in NavGraph.kt. This change was made because we now have five destinations:
 *     "welcome"   → WelcomeScreen
 *     "dashboard" → DashboardScreen
 *     "history"   → HistoryScreen
 *     "stats"     → StatsScreen
 *     "rankup"    → RankUpScreen
 *   Five destinations exceed the threshold where NavController's back-stack management,
 *   state restoration, and transition animations pay for their added complexity.
 *
 *   MainActivity now only:
 *     1. Calls enableEdgeToEdge() for the full-bleed dark look.
 *     2. Reads the "has_seen_welcome" flag to determine the start destination.
 *     3. Writes "has_seen_welcome = true" on first launch.
 *     4. Sets the Material 3 dark theme.
 *     5. Calls MissionNavGraph(startDestination) — everything else happens there.
 *
 * ─── FIRST-TIME USER DETECTION ────────────────────────────────────────────────
 *
 *   We use SharedPreferences with the key "has_seen_welcome" (Boolean, default false).
 *   The prefs file is named "mission_uncomfortable_prefs".
 *
 *   On first launch:
 *     1. SharedPreferences returns false for has_seen_welcome.
 *     2. startDestination = Routes.WELCOME → NavGraph shows WelcomeScreen.
 *     3. User taps "BEGIN YOUR JOURNEY" → MainActivity wrote has_seen_welcome = true
 *        before setContent (see v5 note below) → NavGraph navigates to dashboard.
 *
 *   On every subsequent launch:
 *     1. SharedPreferences returns true for has_seen_welcome.
 *     2. startDestination = Routes.DASHBOARD → NavGraph skips WelcomeScreen entirely.
 *     3. WelcomeScreen is never instantiated, never shown.
 *
 *   v5 change in has_seen_welcome write timing:
 *     In v1–v4, the KTX `prefs.edit { }` lambda wrote the flag inside the
 *     WelcomeScreen's onStartJourney callback. In v5, the NavGraph's welcome route
 *     handles navigation, and the flag is written HERE (in MainActivity, before
 *     setContent) so the start destination can be determined synchronously.
 *     The WelcomeScreen's onStartJourney callback now triggers NavGraph navigation,
 *     not a SharedPreferences write.
 *
 *   Future migration note:
 *     Once Room is set up, move the first-run flag to a UserProfile row in the database
 *     (or to AndroidX DataStore for a coroutine-native, type-safe alternative to SharedPreferences).
 *     SharedPreferences is used here because it requires zero extra dependencies.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial implementation. Launched DashboardScreen directly.
 *
 *   v2 — Added first-time user detection via SharedPreferences.
 *        Added WelcomeScreen composable as the intro destination.
 *        DashboardScreen is now the second screen, reached after onboarding.
 *
 *   v3 — Fixed Android Studio warning: "Use the KTX extension function
 *        SharedPreferences.edit instead?"
 *        Replaced the old chained form:
 *          prefs.edit().putBoolean(KEY_HAS_SEEN_WELCOME, true).apply()
 *        with the KTX lambda form:
 *          prefs.edit { putBoolean(KEY_HAS_SEEN_WELCOME, true) }
 *        Added import androidx.core.content.edit to support the KTX lambda.
 *        No logic changes — behaviour is identical.
 *
 *   v4 — Fixed Android Studio warning: "Remove redundant qualifier name"
 *        at the getSharedPreferences() call.
 *        Inside an Activity (which IS a Context), writing Context.MODE_PRIVATE
 *        is redundant — MODE_PRIVATE is already in scope directly.
 *        Changed: getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
 *            to: getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
 *        Removed the now-unused `import android.content.Context` line.
 *        No logic changes — behaviour is identical.
 *
 *   v5 — Phase 8 (Navigation): replaced the simple mutableStateOf routing with
 *        MissionNavGraph(). Added navigation-compose dependency to build.gradle.
 *        MainActivity now reads the "has_seen_welcome" flag and writes it on first
 *        launch before handing off to NavGraph. The KTX `prefs.edit { }` lambda
 *        and MODE_PRIVATE fixes from v3/v4 are preserved.
 */

package com.example.missionuncomfortable

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
import android.os.Bundle                                    // Standard Activity lifecycle parameter
import androidx.activity.ComponentActivity                  // Compose-compatible base Activity class
import androidx.activity.compose.setContent                 // Sets the Compose UI as the Activity's content view
import androidx.activity.enableEdgeToEdge                   // Lets content draw under system bars
import androidx.compose.material3.MaterialTheme             // Applies the app's Material 3 theme to all children
import androidx.compose.material3.darkColorScheme           // Builds a Material 3 dark colour palette
import androidx.compose.ui.graphics.Color                   // Colour class for the theme palette
import androidx.core.content.edit                           // v3: KTX extension — enables `edit { }` lambda form
import com.example.missionuncomfortable.ui.navigation.MissionNavGraph  // Phase 8: root nav composable
import com.example.missionuncomfortable.ui.navigation.Routes           // Phase 8: route string constants

// ─── THEME ────────────────────────────────────────────────────────────────────

// The app's dark colour palette for Material 3.
// Applied at the Activity level so every screen and component inherits these colours automatically.
// To change the app's colour system, only edit here — the rest of the UI adapts.
private val MissionDarkColors = darkColorScheme(
    primary            = Color(0xFFFFD700),   // Bright gold — Material primary (used by some M3 components)
    onPrimary          = Color(0xFF000000),   // Black text on gold surfaces
    background         = Color(0xFF0A0A0A),   // Very dark — true near-black background
    onBackground       = Color(0xFFFFFFFF),   // White text on background
    surface            = Color(0xFF1A1A1A),   // Dark grey — card/surface colour
    onSurface          = Color(0xFFFFFFFF),   // White text on surfaces
    surfaceVariant     = Color(0xFF2A2A2A),   // Slightly lighter grey — secondary surfaces
    onSurfaceVariant   = Color(0xFFAAAAAA),   // Light grey text on variant surfaces
    error              = Color(0xFFCF6679),   // Soft red — error states
)

// ─── ACTIVITY ─────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    // SharedPreferences constants — defined as companion-object constants so we avoid
    // typo-prone magic strings scattered across the codebase.
    companion object {
        // Name of the SharedPreferences file — scoped to this app.
        // NOTE: DashboardViewModel, HistoryViewModel, and StatsViewModel also open this
        // same file to read/write their respective data. Keeping one file name means all
        // prefs are in one place and easy to inspect during debugging.
        private const val PREFS_NAME = "mission_uncomfortable_prefs"

        // Key for the "has the user seen the welcome screen?" flag.
        // Type: Boolean. Default: false (absent = never shown).
        private const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw content behind the system status and navigation bars for a full-bleed dark look.
        enableEdgeToEdge()

        // ── READ FIRST-RUN FLAG ────────────────────────────────────────────────
        // Open (or create) the SharedPreferences file in private mode —
        // only this app can read or write it.
        // v4: Changed Context.MODE_PRIVATE → MODE_PRIVATE to remove the
        // "redundant qualifier name" warning. Activity IS a Context so
        // MODE_PRIVATE is already in scope without the qualifier.
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Check whether the user has already completed onboarding.
        // getBoolean returns the default (false) if the key has never been written.
        val hasSeenWelcome = prefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)

        // ── FIRST-LAUNCH WRITE ────────────────────────────────────────────────
        // v5 (Phase 8): In v1–v4, the flag was written inside WelcomeScreen's
        // onStartJourney callback. In v5 it is written HERE, before setContent,
        // so that NavGraph can receive a fixed startDestination synchronously.
        // If the user has never seen the welcome screen, write the flag now so
        // that if they background the app mid-onboarding and return, we still
        // show the Dashboard (not the Welcome screen again).
        //
        // v3: Using the KTX `edit { }` lambda form — identical behaviour to the old
        // .edit().putBoolean(...).apply() chain, but removes the Android Studio
        // warning: "Use the KTX extension function SharedPreferences.edit".
        // Applies changes asynchronously in the background (non-blocking).
        if (!hasSeenWelcome) {
            prefs.edit {
                putBoolean(KEY_HAS_SEEN_WELCOME, true)
            }
        }

        // ── DETERMINE START DESTINATION ───────────────────────────────────────
        // Pass the correct start route to NavGraph so it knows which screen to show first.
        //   hasSeenWelcome == false → Routes.WELCOME  (first launch: show intro)
        //   hasSeenWelcome == true  → Routes.DASHBOARD (returning user: skip intro)
        val startDestination = if (hasSeenWelcome) Routes.DASHBOARD else Routes.WELCOME

        setContent {
            // Apply the dark Material 3 theme to the entire composable tree.
            // All child composables (DashboardScreen, WelcomeScreen, etc.) inherit these colours.
            MaterialTheme(colorScheme = MissionDarkColors) {

                // ── NAVIGATION GRAPH ───────────────────────────────────────────
                // Phase 8: replaced the old `if (showDashboard) DashboardScreen() else WelcomeScreen()`
                // with MissionNavGraph(). All routing, back-stack management, bottom nav,
                // and screen transitions now live in NavGraph.kt.
                MissionNavGraph(startDestination = startDestination)
            }
        }
    }
}
