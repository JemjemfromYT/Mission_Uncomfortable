/**
 * MainActivity.kt
 *
 * The single Activity for Mission: Uncomfortable.
 * Acts as the app's entry point and handles top-level navigation between screens.
 *
 * ─── ARCHITECTURE ─────────────────────────────────────────────────────────────
 *
 *   This app uses a simple stateful-composable routing approach rather than a full
 *   NavController graph. For two screens this is the cleanest option — NavController
 *   adds boilerplate that isn't justified until there are three or more destinations.
 *
 *   The routing logic lives in a single `var showDashboard by mutableStateOf(...)` variable:
 *     - false → WelcomeScreen is shown (first-time user)
 *     - true  → DashboardScreen is shown (returning user or after onboarding)
 *
 *   When you add a third screen (e.g., MissionDetailScreen, ProfileScreen), migrate this
 *   to a proper NavHost + NavController with a sealed class / string-route destination set.
 *
 * ─── FIRST-TIME USER DETECTION ────────────────────────────────────────────────
 *
 *   We use SharedPreferences with the key "has_seen_welcome" (Boolean, default false).
 *   The prefs file is named "mission_uncomfortable_prefs".
 *
 *   On first launch:
 *     1. SharedPreferences returns false for has_seen_welcome.
 *     2. showDashboard starts as false → WelcomeScreen is shown.
 *     3. User taps "BEGIN YOUR JOURNEY" → onStartJourney() callback fires.
 *     4. onStartJourney() writes has_seen_welcome = true to SharedPreferences.
 *     5. showDashboard flips to true → DashboardScreen is shown.
 *
 *   On every subsequent launch:
 *     1. SharedPreferences returns true for has_seen_welcome.
 *     2. showDashboard starts as true → DashboardScreen is shown immediately.
 *     3. WelcomeScreen is never shown again.
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
 */

package com.example.missionuncomfortable

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
import android.content.Context                              // Needed to access SharedPreferences
import android.os.Bundle                                    // Standard Activity lifecycle parameter
import androidx.activity.ComponentActivity                  // Compose-compatible base Activity class
import androidx.activity.compose.setContent                 // Sets the Compose UI as the Activity's content view
import androidx.activity.enableEdgeToEdge                   // Lets content draw under system bars
import androidx.compose.material3.MaterialTheme             // Applies the app's Material 3 theme to all children
import androidx.compose.material3.darkColorScheme           // Builds a Material 3 dark colour palette
import androidx.compose.runtime.getValue                    // Property delegate for Compose State (used with 'by')
import androidx.compose.runtime.mutableStateOf              // Creates observable mutable state in Compose
import androidx.compose.runtime.remember                    // Keeps state alive across recompositions
import androidx.compose.runtime.setValue                    // Property delegate for Compose State (used with 'by')
import androidx.compose.ui.graphics.Color                   // Colour class for the theme palette
import androidx.core.content.edit                           // v3: KTX extension — enables `edit { }` lambda form
import com.example.missionuncomfortable.ui.dashboard.DashboardScreen  // The main dashboard composable
import com.example.missionuncomfortable.ui.welcome.WelcomeScreen      // The first-launch intro composable

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
        // NOTE: DashboardViewModel (v4) also opens this same file to read/write
        // total_xp and last_completed_date. Keeping one file name means all prefs
        // are in one place and easy to inspect during debugging.
        private const val PREFS_NAME = "mission_uncomfortable_prefs"

        // Key for the "has the user seen the welcome screen?" flag.
        // Type: Boolean. Default: false (absent = never shown).
        private const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw content behind the system status and navigation bars for a full-bleed dark look.
        enableEdgeToEdge()

        // ── READ FIRST-RUN FLAG ────────────────────────────────────────────
        // Open (or create) the SharedPreferences file in private mode —
        // only this app can read or write it.
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check whether the user has already completed onboarding.
        // getBoolean returns the default (false) if the key has never been written.
        val hasSeenWelcome = prefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)

        setContent {
            // Apply the dark Material 3 theme to the entire composable tree.
            // All child composables (DashboardScreen, WelcomeScreen, etc.) inherit these colours.
            MaterialTheme(colorScheme = MissionDarkColors) {

                // ── ROUTING STATE ──────────────────────────────────────────
                // `remember { mutableStateOf(...) }` creates state that survives recompositions
                // but resets on process death (which is fine — we persist the real flag in SharedPreferences).
                //
                // Initial value: true if the user has already seen the welcome screen
                //                (skip straight to the Dashboard), false otherwise.
                var showDashboard by remember { mutableStateOf(hasSeenWelcome) }

                // ── SCREEN ROUTING ─────────────────────────────────────────
                // Simple if/else — no NavController needed for two screens.
                // Compose will automatically recompose when showDashboard changes.
                if (showDashboard) {
                    // ── RETURNING USER PATH ───────────────────────────────
                    // The user has already completed onboarding.
                    // Jump straight to the Dashboard.
                    DashboardScreen()
                } else {
                    // ── FIRST-TIME USER PATH ──────────────────────────────
                    // Show the welcome/intro screen.
                    // When the user taps "BEGIN YOUR JOURNEY", the lambda below fires:
                    //   1. Write has_seen_welcome = true so this screen is never shown again.
                    //   2. Flip showDashboard to true — Compose re-routes to DashboardScreen.
                    WelcomeScreen(
                        onStartJourney = {
                            // v3: KTX `edit { }` lambda form — identical behaviour to the old
                            // .edit().putBoolean(...).apply() chain, but removes the Android
                            // Studio warning: "Use the KTX extension function SharedPreferences.edit".
                            // Applies changes asynchronously in the background (non-blocking).
                            prefs.edit {
                                putBoolean(KEY_HAS_SEEN_WELCOME, true)
                            }

                            // Flip the routing state — triggers recomposition → DashboardScreen shown.
                            showDashboard = true
                        }
                    )
                }
            }
        }
    }
}
