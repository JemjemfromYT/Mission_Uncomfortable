/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        ⚠  AI ASSISTANT — READ THIS BEFORE EDITING THIS FILE  ⚠         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PROJECT: Mission: Uncomfortable — Android app (Kotlin + Jetpack Compose)
 *          Daily social-discomfort challenges. User accepts a mission → goes
 *          and does it → returns → rates discomfort (1–10) → earns XP → ranks up.
 *
 * KEY FILES (read these for full context before making changes):
 *   DashboardModels.kt     — All enums + data classes (Mission, Rank, XpProgress, MissionCategory)
 *   MissionRepository.kt   — Full mission library: 25 missions, 5 XP tiers, 6 categories
 *   DashboardViewModel.kt  — All game logic: XP, streaks, rank-ups, history saving, core loop
 *   DashboardScreen.kt     — Main UI. v5: SWAP MISSION shows popup. v10: onNavigateToRankUp added.
 *   HistoryModels.kt       — CompletedMissionEntry data class
 *   HistoryViewModel.kt    — Reads history from SharedPreferences JSON
 *   HistoryScreen.kt       — Scrollable mission history UI
 *   RankUpScreen.kt        — Full-screen rank-up celebration with spring animation
 *   StatsViewModel.kt      — Computes stats (streaks, category breakdown, avg discomfort)
 *   StatsScreen.kt         — Bar charts: category breakdown + discomfort by day of week
 *   NavGraph.kt            — Navigation graph: all routes + bottom nav bar (this file)
 *
 * ── COMMENTING RULES — NEVER break these ────────────────────────────────────
 *
 *   1. DO NOT remove any existing comment. If a comment is outdated, UPDATE it.
 *      The developer relies on comments to understand decisions made earlier.
 *      Deleting a comment is not allowed, even if the code seems obvious.
 *
 *   2. ADD comments to every new block of code you write:
 *        • Every function         → KDoc block /** ... */ with @param / @return
 *        • Every data class       → KDoc block on the class AND on every field
 *        • Every enum value       → inline comment explaining what state it represents
 *        • Every composable       → KDoc block + ── SECTION NAME ── dividers inside
 *        • Every non-obvious line → inline comment explaining WHY, not just what
 *
 *   3. COMMENT STYLE used across this entire codebase (stay consistent):
 *        • Section dividers  →  // ── SECTION NAME ─────────────────────────────
 *        • KDoc blocks       →  /** ... */ above every function, class, data class
 *        • Inline reasoning  →  // Why this decision was made (not just "what it does")
 *
 *   4. CHANGELOG: every file has a  ─── CHANGELOG ───  block in its file header.
 *      When you make a significant change, add a line:
 *        v2 — Short description of what changed and why.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these file, the developer will have to   ║
 * ║  manually fix every file you touch. Please respect these conventions.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * NavGraph.kt
 *
 * Defines the full navigation graph for Mission: Uncomfortable.
 * This is the single source of truth for all screen routing.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/navigation/NavGraph.kt
 *
 * ─── ROUTES ──────────────────────────────────────────────────────────────────
 *
 *   "welcome"   → WelcomeScreen  — shown only on first launch (has_seen_welcome == false)
 *   "dashboard" → DashboardScreen — the main daily mission screen (start destination)
 *   "history"   → HistoryScreen  — scrollable list of completed missions
 *   "stats"     → StatsScreen    — bar charts: category + discomfort by day
 *   "rankup"    → RankUpScreen   — full-screen celebration, triggered by rankUpEvent
 *   "ascension" → AscensionBookScreen — full-screen storybook, triggered by ascensionEvent
 *
 * ─── VIEWMODEL SHARING NOTE ───────────────────────────────────────────────────
 *
 *   DashboardViewModel, HistoryViewModel, and StatsViewModel all extend
 *   AndroidViewModel(application). Inside a NavHost, viewModel() scopes a ViewModel
 *   to the current NavBackStackEntry — meaning each destination gets its own instance.
 *
 *   The RankUpScreen and AscensionBookScreen routes need the SAME DashboardViewModel
 *   instance as the "dashboard" route (so they can call ViewModel methods on the correct
 *   object). To achieve this, those composables retrieve the ViewModel from the "dashboard"
 *   back stack entry via navController.getBackStackEntry(Routes.DASHBOARD).
 *   The result is wrapped in remember(backStackEntry) — using the route's own
 *   NavBackStackEntry as the key. This is required by the Navigation Compose API:
 *   getBackStackEntry() called during composition must be keyed on a NavBackStackEntry,
 *   not on navController alone (which triggers a compile/runtime error).
 *
 * ─── BOTTOM NAV BAR ──────────────────────────────────────────────────────────
 *
 *   Shown on: "dashboard", "history", "stats"
 *   Hidden on: "welcome", "rankup", "ascension"
 *
 *   Design spec (from handoff doc):
 *     - Three tabs: Mission (shield icon), History (clock icon), Stats (chart icon)
 *     - Active tab:   ColorAccentGold (#C8A84B)
 *     - Inactive tab: ColorTextSecondary (#8A8A8A)
 *     - Background:   ColorSurface (#1A1A1A) — dark grey, no elevation / shadow
 *     - No pill/indicator highlight behind selected tab (indicatorColor = Transparent)
 *
 * ─── SCREEN TRANSITIONS ──────────────────────────────────────────────────────
 *
 *   welcome  → dashboard : fade (no slide — the journey "begins")
 *   dashboard ↔ history  : horizontal slide (history slides in from the right)
 *   dashboard ↔ stats    : horizontal slide (stats slides in from the right)
 *   rankup               : fade in/out (feels like a ceremony, not a page turn)
 *   ascension            : fade in/out (storybook feels ceremonial)
 *
 *   WHY slideInHorizontally instead of slideIntoContainer:
 *     slideIntoContainer / slideOutOfContainer were introduced in Compose Animation 1.5
 *     (August 2023). Projects on an older Compose BOM get "Unresolved reference" compile
 *     errors because those functions simply do not exist in earlier versions.
 *     slideInHorizontally / slideOutHorizontally have been available since Compose 1.0
 *     and produce an identical visual result — the lambda offset { it } or { -it }
 *     controls which direction the screen enters/exits.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial implementation. Phase 8 + Phase 9 navigation wiring.
 *        Bottom nav bar (Mission / History / Stats), welcome-screen first-launch
 *        gate, rank-up route with shared ViewModel, screen slide transitions.
 *   v2 — Fixed three compile errors reported after first build attempt:
 *        (1) "Unresolved reference: slideIntoContainer" + slideOutOfContainer.
 *            Replaced both with slideInHorizontally / slideOutHorizontally (Compose 1.0+).
 *            Removed AnimatedContentTransitionScope import — no longer needed.
 *        (2) "Calling getBackStackEntry during composition without using remember
 *             with a NavBackStackEntry key."
 *            Changed remember(navController) → remember(backStackEntry) in the rankup
 *            composable. backStackEntry is the lambda parameter that Compose passes into
 *            every composable { backStackEntry -> } block, and it IS a NavBackStackEntry,
 *            which satisfies the Navigation Compose API requirement.
 *   v3 — Added ascension route for AscensionBookScreen. Updated VIEWMODEL SHARING NOTE
 *        and BOTTOM NAV VISIBILITY to include "ascension". Null-guards in both rankup
 *        and ascension routes use LaunchedEffect so popBackStack() is a proper side effect,
 *        not a direct composable body call (which could fire again during exit animation
 *        and pop the wrong entry).
 *   v4 — Fixed two bugs that caused a crash and a blank Mission tab:
 *
 *        BUG 1 (CRASH — "No destination with route dashboard is on the back stack"):
 *          Root cause: getBackStackEntry(Routes.DASHBOARD) was called inside
 *          remember(backStackEntry) { ... }. During the 300 ms Compose exit animation
 *          (after the explicit popBackStack() in onContinue/onFinish had already fired),
 *          a LiveData recompose re-ran the remember lambda. By that point, "dashboard"
 *          was already gone from the back stack, so getBackStackEntry() threw
 *          IllegalArgumentException and the app crashed.
 *
 *          Previous "fix" (v3 attempt): wrapped the remember block and LaunchedEffect
 *          in a try/catch. This caused a NEW compile error:
 *            "Try catch is not supported around composable function invocations."
 *          Composables (LaunchedEffect, remember) cannot appear inside catch blocks —
 *          the Compose compiler enforces this at build time.
 *
 *          Correct fix (v4): check navController.currentBackStack.value BEFORE calling
 *          getBackStackEntry(). currentBackStack is a StateFlow — reading .value is a
 *          plain Kotlin read, not a composable invocation, so it is safe anywhere in
 *          the composable body. If "dashboard" is absent, we emit a LaunchedEffect to
 *          pop safely and return early. Only if "dashboard" IS on the stack do we
 *          call remember(backStackEntry) { getBackStackEntry(...) } — which is then
 *          guaranteed not to throw.
 *
 *        BUG 2 (BLANK MISSION TAB after closing the book):
 *          Root cause: null-guards in the rankup and ascension routes previously called
 *          navController.popBackStack() directly inside the composable body. Composable
 *          bodies are not the right place for NavController side effects — Compose may
 *          re-execute them during the 300 ms exit animation, after the real pop has
 *          already happened. In that window, the direct popBackStack() would fire again
 *          and pop DASHBOARD (the entry below rankup/ascension) instead of the route
 *          itself. With dashboard gone, the Mission tab showed a blank screen until
 *          the user switched tabs and back.
 *          Fix: null-guards already used LaunchedEffect(backStackEntry) in v3 for the
 *          pop call (correct), but the back-stack pre-check in v4 also prevents the
 *          guard from running at all in the exit-animation window — doubly safe.
 */

package com.example.missionuncomfortable.ui.navigation

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
import androidx.compose.animation.core.tween                            // Duration spec for transitions
import androidx.compose.animation.fadeIn                                // Fade-in transition
import androidx.compose.animation.fadeOut                               // Fade-out transition
import androidx.compose.animation.slideInHorizontally                   // Horizontal slide-in (available since Compose 1.0)
import androidx.compose.animation.slideOutHorizontally                  // Horizontal slide-out (available since Compose 1.0)
// NOTE: slideIntoContainer / slideOutOfContainer are NOT imported here — they require
// Compose Animation 1.5+ and cause "Unresolved reference" errors on older Compose BOMs.
// slideInHorizontally { ±it } is the equivalent that works on all supported versions.
import androidx.compose.foundation.layout.fillMaxSize                   // Fill all available space
import androidx.compose.foundation.layout.padding                       // Inner padding modifier
import androidx.compose.foundation.layout.size                          // Fixed size modifier
import androidx.compose.material3.Icon                                  // Icon composable for nav bar
import androidx.compose.material3.NavigationBar                         // Bottom navigation bar container
import androidx.compose.material3.NavigationBarItem                     // Single tab in the nav bar
import androidx.compose.material3.NavigationBarItemDefaults             // Colours for nav bar items
import androidx.compose.material3.Scaffold                              // Layout with top/bottom bars
import androidx.compose.material3.Text                                  // Text composable
import androidx.compose.runtime.Composable                              // Marks a composable function
import androidx.compose.runtime.LaunchedEffect                          // Side-effect that runs outside composition
import androidx.compose.runtime.getValue                                // Property delegate for State
import androidx.compose.runtime.remember                                // Keeps value across recompositions
import androidx.compose.ui.Modifier                                     // Modifier chain
import androidx.compose.ui.graphics.Color                               // Colour class
import androidx.compose.ui.res.painterResource                          // Loads a drawable resource
import androidx.compose.ui.text.font.FontWeight                         // Bold, SemiBold, etc.
import androidx.compose.ui.unit.dp                                      // Density-independent pixels
import androidx.compose.ui.unit.sp                                      // Scale-independent pixels
import androidx.lifecycle.viewmodel.compose.viewModel                   // Creates/retrieves a ViewModel
import androidx.navigation.NavDestination.Companion.hierarchy           // Checks if destination is in hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination       // Finds start destination for popUpTo
import androidx.navigation.compose.NavHost                              // Hosts the navigation graph
import androidx.navigation.compose.composable                           // Registers a route in the NavHost
import androidx.navigation.compose.currentBackStackEntryAsState         // Observes current destination as State
import androidx.navigation.compose.rememberNavController                // Creates and remembers a NavController
import com.example.missionuncomfortable.ui.ascension.AscensionBookScreen // Full-screen ascension storybook
import com.example.missionuncomfortable.ui.dashboard.DashboardScreen   // Mission dashboard screen
import com.example.missionuncomfortable.ui.dashboard.DashboardViewModel // ViewModel for dashboard + rank-up
import com.example.missionuncomfortable.ui.history.HistoryScreen        // Mission history screen
import com.example.missionuncomfortable.ui.rankup.RankUpScreen          // Full-screen rank-up celebration
import com.example.missionuncomfortable.ui.stats.StatsScreen            // Stats bar-chart screen
import com.example.missionuncomfortable.ui.welcome.WelcomeScreen        // First-launch intro screen

// ─── COLOUR CONSTANTS ────────────────────────────────────────────────────────
// Mirrored from DashboardScreen.kt. Do not redefine with different values.
// When a shared AppColors.kt file exists, replace these with imports from there.

private val ColorSurface        = Color(0xFF1A1A1A)   // Dark grey — bottom nav background
private val ColorAccentGold     = Color(0xFFC8A84B)   // Muted gold — active tab colour
private val ColorTextSecondary  = Color(0xFF8A8A8A)   // Muted grey — inactive tab colour

// ─── ROUTE CONSTANTS ──────────────────────────────────────────────────────────

/**
 * All navigation route strings used in the NavHost.
 *
 * Defined as an object so any file can reference Routes.DASHBOARD etc.
 * without magic string literals scattered through the codebase.
 */
object Routes {
    const val WELCOME    = "welcome"    // First-launch intro screen
    const val DASHBOARD  = "dashboard"  // Main daily mission screen (start destination)
    const val HISTORY    = "history"    // Completed mission history list
    const val STATS      = "stats"      // Category + discomfort bar charts
    const val RANKUP     = "rankup"     // Full-screen rank-up celebration overlay
    const val ASCENSION  = "ascension"  // Full-screen Ascension Book storybook overlay
}

// ─── BOTTOM NAV ITEM MODEL ────────────────────────────────────────────────────

/**
 * BottomNavItem — represents one tab in the bottom navigation bar.
 *
 * @param route    The nav route this tab navigates to when tapped.
 * @param label    Display label shown below the icon.
 * @param iconRes  Drawable resource ID for the tab icon.
 */
private data class BottomNavItem(
    val route:   String,
    val label:   String,
    val iconRes: Int
)

/**
 * bottomNavItems — the three visible tabs in the bottom navigation bar.
 *
 * Uses built-in Android vector drawables to avoid adding new drawable assets.
 * Replace with custom drawables if the design system evolves.
 *   ic_menu_today          → shield-like calendar icon → represents today's mission
 *   ic_menu_recent_history → clock icon                → represents mission history
 *   ic_menu_sort_by_size   → bar-chart-like icon       → represents stats
 */
private val bottomNavItems = listOf(
    BottomNavItem(
        route   = Routes.DASHBOARD,
        label   = "Mission",
        iconRes = android.R.drawable.ic_menu_today           // Today's mission tab
    ),
    BottomNavItem(
        route   = Routes.HISTORY,
        label   = "History",
        iconRes = android.R.drawable.ic_menu_recent_history  // History tab
    ),
    BottomNavItem(
        route   = Routes.STATS,
        label   = "Stats",
        iconRes = android.R.drawable.ic_menu_sort_by_size    // Stats tab
    )
)

// ─── TRANSITION DURATION ──────────────────────────────────────────────────────

// 300ms feels snappy but not jarring. Matches the stoic, disciplined tone of the app.
// The rank-up screen uses 400ms for its entrance — it should feel more deliberate.
private const val SLIDE_DURATION_MS = 300
private const val FADE_DURATION_MS  = 400

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE — MissionNavGraph
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MissionNavGraph — the root composable that hosts the entire app navigation.
 *
 * Called from MainActivity instead of DashboardScreen() or WelcomeScreen() directly.
 * Owns the NavController and decides which screen to show based on the current route.
 *
 * ── First-launch logic ──
 *   The start destination is passed in as [startDestination].
 *   MainActivity reads "has_seen_welcome" from SharedPreferences and passes:
 *     - Routes.WELCOME   if this is the user's first launch
 *     - Routes.DASHBOARD if the user has completed onboarding
 *
 * ── Bottom nav visibility ──
 *   The Scaffold's bottomBar is only shown when the current route is one of the
 *   three main tabs (dashboard, history, stats). It is hidden on the welcome,
 *   rankup, and ascension routes so they appear as full-screen experiences.
 *
 * ── Rank-up flow ──
 *   DashboardScreen detects uiState.rankUpEvent != null via a LaunchedEffect
 *   and calls onNavigateToRankUp() → navController navigates to Routes.RANKUP.
 *   RankUpScreen calls onContinue() → ViewModel.onRankUpCelebrationComplete()
 *   → navController.popBackStack() returns to dashboard.
 *   The DashboardViewModel is retrieved from the "dashboard" back stack entry in
 *   the rankup route so both routes share the same ViewModel instance.
 *
 * ── Ascension flow ──
 *   After the rank-up celebration completes, onRankUpCelebrationComplete() may set
 *   ascensionEvent in the ViewModel. DashboardScreen's LaunchedEffect detects this
 *   and navigates to Routes.ASCENSION. AscensionBookScreen calls onFinish() →
 *   ViewModel.onAscensionBookDismissed() → navController.popBackStack() → dashboard.
 *
 * @param startDestination  The first route to display. Passed by MainActivity
 *                          based on the "has_seen_welcome" SharedPreferences flag.
 */
@Composable
fun MissionNavGraph(startDestination: String) {

    // ── NAV CONTROLLER ─────────────────────────────────────────────────────────
    // rememberNavController() creates a NavController and keeps it alive across
    // recompositions. The NavController drives all navigation in this composable tree.
    val navController = rememberNavController()

    // ── OBSERVE CURRENT ROUTE ──────────────────────────────────────────────────
    // currentBackStackEntryAsState() returns the current back stack entry as
    // Compose State so the UI recomposes when the destination changes.
    // Used to decide which tab is active and whether to show the bottom nav.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ── BOTTOM NAV VISIBILITY RULE ─────────────────────────────────────────────
    // Show the bottom nav only when the user is on one of the three main tabs.
    // The welcome screen, rank-up screen, and ascension screen are full-screen
    // experiences — showing a nav bar on them would break the immersion.
    val showBottomNav = currentRoute in listOf(
        Routes.DASHBOARD,
        Routes.HISTORY,
        Routes.STATS
    )

    // ── SCAFFOLD ───────────────────────────────────────────────────────────────
    // Scaffold provides the slot for the bottom bar. The NavHost is placed in the
    // content slot so it respects the bottom bar's height (innerPadding).
    Scaffold(
        containerColor = Color(0xFF0D0D0D),  // Near-black — same as ColorBackground across the app
        bottomBar = {

            // ── BOTTOM NAVIGATION BAR ──────────────────────────────────────────
            // Only rendered when showBottomNav is true (see rule above).
            // Three tabs: Mission | History | Stats
            //   Active tab:  ColorAccentGold (muted gold)
            //   Inactive:    ColorTextSecondary (muted grey)
            //   Background:  ColorSurface (dark grey)
            //   Elevation:   0dp — no shadow, per design spec
            if (showBottomNav) {
                NavigationBar(
                    containerColor = ColorSurface,
                    tonalElevation = 0.dp  // No shadow — matches the flat dark aesthetic
                ) {
                    bottomNavItems.forEach { item ->

                        // Check if this tab's route is anywhere in the current destination
                        // hierarchy. hierarchy includes parent routes, so this correctly
                        // highlights a tab even when on a nested destination.
                        val isSelected = navBackStackEntry?.destination
                            ?.hierarchy
                            ?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = isSelected,
                            onClick  = {
                                navController.navigate(item.route) {
                                    // Pop back to the graph's start destination before navigating.
                                    // This prevents a growing back stack when the user switches tabs.
                                    // saveState/restoreState preserves scroll position and ViewModel
                                    // state when returning to a previously visited tab.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true  // Avoid duplicate destinations on re-tap
                                    restoreState    = true  // Restore scroll / VM state on tab return
                                }
                            },
                            icon  = {
                                Icon(
                                    painter            = painterResource(id = item.iconRes),
                                    contentDescription = item.label,
                                    modifier           = Modifier.size(22.dp),
                                    tint               = if (isSelected) ColorAccentGold
                                    else ColorTextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text       = item.label,
                                    color      = if (isSelected) ColorAccentGold
                                    else ColorTextSecondary,
                                    fontSize   = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold
                                    else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                // Transparent indicator = no gold pill behind the selected tab.
                                // The gold tint on the icon + label is enough visual feedback.
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        // ── NAV HOST ──────────────────────────────────────────────────────────
        // NavHost renders the composable for the current route.
        // innerPadding accounts for the bottom nav bar height so content isn't hidden behind it.
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ── ROUTE: WELCOME ─────────────────────────────────────────────────
            // Shown only on first launch. Fades in so the app "opens like a door".
            // When the user taps "BEGIN YOUR JOURNEY":
            //   1. MainActivity already wrote has_seen_welcome = true before setContent.
            //   2. WelcomeScreen calls onStartJourney().
            //   3. We navigate to dashboard and pop the welcome route off the back stack
            //      so the user cannot press Back to return to it.
            composable(
                route           = Routes.WELCOME,
                enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
                exitTransition  = { fadeOut(animationSpec = tween(FADE_DURATION_MS)) }
            ) {
                WelcomeScreen(
                    onStartJourney = {
                        // Navigate to dashboard and clear the welcome screen from the back stack.
                        // inclusive = true removes the "welcome" destination itself from the stack.
                        // This means pressing Back from Dashboard exits the app, not returns to Welcome.
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                )
            }

            // ── ROUTE: DASHBOARD ───────────────────────────────────────────────
            // The main screen — shows today's mission, rank badge, and XP bar.
            // Slides in from the left when navigating back from History or Stats.
            //
            // Transition direction reasoning:
            //   Dashboard is the LEFTMOST tab. When returning to it, it slides in from
            //   the left ( { -it } = initial offset is -fullWidth = off-screen left ).
            //   When leaving, it exits to the left to make room for the right-hand tabs.
            //
            // onNavigateToRankUp is called by DashboardScreen's LaunchedEffect when
            // uiState.rankUpEvent is non-null (i.e. a rank-up just occurred).
            //
            // onNavigateToAscension is called by DashboardScreen's LaunchedEffect when
            // uiState.ascensionEvent is non-null (i.e. the Ascension Book should show).
            composable(
                route           = Routes.DASHBOARD,
                enterTransition = {
                    // Slide in from LEFT — Dashboard is the leftmost tab in the bar.
                    // { -it } → initial X offset = -fullWidth (completely off-screen left).
                    slideInHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { -it }
                },
                exitTransition  = {
                    // Slide out to LEFT — moves aside as History/Stats enter from the right.
                    slideOutHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { -it }
                }
            ) {
                DashboardScreen(
                    onNavigateToRankUp = {
                        // The ViewModel set rankUpEvent — navigate to the celebration screen.
                        // launchSingleTop prevents stacking multiple rankup screens if the
                        // LaunchedEffect fires more than once before navigation settles.
                        navController.navigate(Routes.RANKUP) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAscension = {
                        // The ViewModel set ascensionEvent — navigate to the storybook screen.
                        // See ROUTE: ASCENSION below for why this fires separately from
                        // (and after) the rank-up celebration route.
                        navController.navigate(Routes.ASCENSION) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ── ROUTE: HISTORY ─────────────────────────────────────────────────
            // Scrollable list of all completed missions, most recent first.
            // Slides in from the RIGHT when the user taps the History tab —
            // History sits to the right of Dashboard in the tab order.
            //
            //   Enter: { it }  → initial X offset = +fullWidth (off-screen right)
            //   Exit:  { it }  → final   X offset = +fullWidth (exits right)
            composable(
                route           = Routes.HISTORY,
                enterTransition = {
                    // Slide in from RIGHT — History is right of Mission in the tab bar.
                    // { it } → initial X offset = +fullWidth (completely off-screen right).
                    slideInHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { it }
                },
                exitTransition  = {
                    // Slide out to RIGHT — moves aside when returning to Dashboard.
                    slideOutHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { it }
                }
            ) {
                // viewModel() here creates a HistoryViewModel scoped to this back stack entry.
                // HistoryViewModel extends AndroidViewModel — it reads SharedPreferences on init.
                // A fresh read on each visit is correct — new history may have been saved since last visit.
                HistoryScreen()
            }

            // ── ROUTE: STATS ───────────────────────────────────────────────────
            // Bar charts: mission count by category + average discomfort by day of week.
            // Slides in from the RIGHT — Stats is the rightmost tab in the tab order.
            //
            //   Enter: { it }  → initial X offset = +fullWidth (off-screen right)
            //   Exit:  { it }  → final   X offset = +fullWidth (exits right)
            composable(
                route           = Routes.STATS,
                enterTransition = {
                    // Slide in from RIGHT — Stats is the rightmost tab in the bar.
                    slideInHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { it }
                },
                exitTransition  = {
                    // Slide out to RIGHT — moves aside when returning to Dashboard.
                    slideOutHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { it }
                }
            ) {
                // viewModel() here creates a StatsViewModel scoped to this back stack entry.
                // StatsViewModel extends AndroidViewModel — reads SharedPreferences on init.
                StatsScreen()
            }

            // ── ROUTE: RANKUP ──────────────────────────────────────────────────
            // Full-screen rank-up celebration. Navigated to automatically when
            // DashboardScreen detects uiState.rankUpEvent != null.
            //
            // SHARED VIEWMODEL NOTE:
            //   This route needs the SAME DashboardViewModel instance that the "dashboard"
            //   route is using — not a new one. If we called viewModel() here directly, we
            //   would get a fresh instance with no rankUpEvent set, and onRankUpCelebrationComplete()
            //   would update the wrong object.
            //
            //   Solution: retrieve the "dashboard" NavBackStackEntry and use viewModel(dashboardEntry)
            //   to get the existing ViewModel from that entry's ViewModelStore.
            //   This is the standard Compose Navigation pattern for sharing ViewModels across routes.
            //   See: https://developer.android.com/jetpack/compose/navigation#viewmodels
            //
            // REMEMBER KEY NOTE — WHY backStackEntry, NOT navController:
            //   Navigation Compose requires that any call to getBackStackEntry() during
            //   composition be wrapped in remember() with a NavBackStackEntry as the key.
            //   Using remember(navController) causes this compile/runtime error:
            //     "Calling getBackStackEntry during composition without using remember
            //      with a NavBackStackEntry key"
            //   Fix: use remember(backStackEntry) where backStackEntry is the lambda parameter
            //   that Compose automatically passes into every composable { backStackEntry -> } block.
            //   It IS a NavBackStackEntry (specifically, the "rankup" route's entry), so it
            //   satisfies the API requirement. The remember cache is invalidated if the rankup
            //   entry itself changes — which is the correct re-computation trigger.
            //
            // SAFETY NOTE — WHY WE PRE-CHECK THE BACK STACK (v4 fix):
            //   getBackStackEntry() throws IllegalArgumentException if "dashboard" is not
            //   on the back stack. This can happen during the composable's Compose exit
            //   animation: the fadeOut transition keeps this composable in the composition
            //   tree for 300 ms after the explicit popBackStack() in onContinue has fired.
            //   During that window, a LiveData emission (e.g. from the ViewModel) can
            //   trigger a recompose, re-running the remember lambda and crashing.
            //
            //   WRONG FIX (try/catch): wrapping remember() and LaunchedEffect in try/catch
            //   causes a compile error: "Try catch is not supported around composable function
            //   invocations." Composables cannot appear inside catch blocks — the Compose
            //   compiler enforces this at build time.
            //
            //   CORRECT FIX (v4): read navController.currentBackStack.value to check
            //   whether "dashboard" is on the stack. currentBackStack is a plain StateFlow
            //   — reading .value is a Kotlin property access, NOT a composable invocation,
            //   so it is legal anywhere in the composable body. If "dashboard" is absent,
            //   we emit a LaunchedEffect to pop safely and return early. Only when "dashboard"
            //   IS confirmed present do we call remember { getBackStackEntry(...) }, which
            //   is then guaranteed not to throw.
            composable(
                route           = Routes.RANKUP,
                enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
                exitTransition  = { fadeOut(animationSpec = tween(300)) }
            ) { backStackEntry ->   // backStackEntry = NavBackStackEntry for the "rankup" route

                // ── BACK STACK SAFETY CHECK (v4) ──────────────────────────────
                // Check whether "dashboard" is still on the back stack before calling
                // getBackStackEntry(). This is a plain StateFlow value read — NOT a
                // composable call — so it is safe to use anywhere in the composable body.
                //
                // WHY this check is needed: during the 300 ms Compose exit animation
                // (after the explicit popBackStack() in onContinue fires), this composable
                // stays alive in the composition tree. A LiveData recompose in that window
                // would re-run the remember lambda below and crash if "dashboard" is gone.
                //
                // WHY NOT try/catch: "Try catch is not supported around composable function
                // invocations" — the Compose compiler rejects it at build time.
                val isDashboardOnStack = navController.currentBackStack.value
                    .any { it.destination.route == Routes.DASHBOARD }

                if (!isDashboardOnStack) {
                    // Dashboard is gone — this route should not be visible.
                    // Pop safely via LaunchedEffect (a proper side effect, runs once,
                    // outside of the composition phase).
                    //
                    // WHY popBackStack(Routes.RANKUP, inclusive=true) and NOT plain popBackStack():
                    //   Targeted pop is idempotent. If "rankup" has already been removed from
                    //   the stack (e.g. by the onContinue pop above), it returns false and does
                    //   nothing — it can never accidentally pop "dashboard" by mistake.
                    //
                    // WHY LaunchedEffect(backStackEntry) and NOT a direct call:
                    //   Direct NavController calls inside a composable body are side effects.
                    //   Compose may execute them at unexpected times (including during exit
                    //   animations). LaunchedEffect(backStackEntry) runs exactly once per
                    //   back stack entry instance, safely outside of the composition phase.
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.RANKUP, inclusive = true)
                    }
                    return@composable
                }

                // "dashboard" is confirmed on the back stack — safe to call getBackStackEntry().
                // remember(backStackEntry) satisfies the Navigation Compose API requirement
                // that getBackStackEntry() calls during composition be keyed on a NavBackStackEntry.
                val dashboardEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.DASHBOARD)
                }

                // Retrieve the SAME DashboardViewModel instance that DashboardScreen is using.
                // viewModel(dashboardEntry) scopes the lookup to the dashboard entry's ViewModelStore,
                // returning the existing instance — not a new one. This ensures that
                // onRankUpCelebrationComplete() clears rankUpEvent on the correct object.
                val dashboardViewModel: DashboardViewModel = viewModel(dashboardEntry)

                // uiState holds rankUpEvent — the Rank object the user just achieved.
                // We read .value directly (not observeAsState) because we only need the
                // snapshot at the moment this composable runs. The LaunchedEffect in
                // DashboardScreen already guaranteed rankUpEvent is non-null before navigating here.
                val uiState = dashboardViewModel.uiState.value

                // Guard: if somehow we arrive here with no rankUpEvent, pop back immediately.
                // This prevents a blank RankUpScreen if the event was already cleared
                // (e.g. config change between navigate() and composition).
                //
                // IMPORTANT: use LaunchedEffect with a targeted route pop, NOT a direct
                // navController.popBackStack() call here in composition.
                //
                //   Why LaunchedEffect: NavController calls inside a composable body are side
                //   effects — Compose may execute them at unexpected times, including during the
                //   300 ms exit animation after the real pop has already happened. In that window
                //   a plain popBackStack() would pop the WRONG entry (dashboard instead of rankup),
                //   stripping dashboard from the back stack and blanking the Mission tab.
                //
                //   Why targeted route: popBackStack(Routes.RANKUP, inclusive=true) is idempotent.
                //   If rankup is already gone from the stack it returns false and does nothing,
                //   so dashboard can never be popped by mistake.
                if (uiState?.rankUpEvent == null) {
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.RANKUP, inclusive = true)
                    }
                    return@composable
                }

                RankUpScreen(
                    newRank    = uiState.rankUpEvent,
                    onContinue = {
                        // User tapped CONTINUE on the celebration screen.
                        //   1. Clear rankUpEvent + set isDailyComplete = true in the ViewModel.
                        //      This may ALSO set ascensionEvent (see DashboardViewModel
                        //      .onRankUpCelebrationComplete's KDoc) if this rank's Ascension
                        //      Book hasn't been shown yet.
                        //   2. Pop the rankup route → returns to dashboard.
                        //   3. Back on "dashboard", DashboardScreen's LaunchedEffect notices
                        //      ascensionEvent is non-null and navigates to Routes.ASCENSION —
                        //      so the storybook opens right after the celebration, not during it.
                        dashboardViewModel.onRankUpCelebrationComplete()
                        navController.popBackStack()
                    }
                )
            }

            // ── ROUTE: ASCENSION ───────────────────────────────────────────────
            // Full-screen "Ascension Book" storybook. Navigated to automatically
            // when DashboardScreen detects uiState.ascensionEvent != null — either
            // on the user's very first ever dashboard load (the Observer's book),
            // or right after the "rankup" route pops back to dashboard following
            // a promotion (see DashboardViewModel.onRankUpCelebrationComplete).
            //
            // SHARED VIEWMODEL NOTE — identical reasoning to the "rankup" route above:
            //   This route needs the SAME DashboardViewModel instance as "dashboard"
            //   so onAscensionBookDismissed() marks the book as seen on the correct
            //   object. Retrieved via navController.getBackStackEntry(Routes.DASHBOARD),
            //   wrapped in remember(backStackEntry) for the same Navigation Compose
            //   API reason documented on the "rankup" route.
            //
            // SAFETY NOTE — identical reasoning to the "rankup" route (v4 fix):
            //   getBackStackEntry() throws if "dashboard" is not on the back stack.
            //   We pre-check navController.currentBackStack.value — a plain StateFlow
            //   read, not a composable — before calling getBackStackEntry(). If dashboard
            //   is absent, we bail with a LaunchedEffect pop. This avoids both the
            //   crash AND the "try/catch around composable" compile error.
            //
            // BLANK TAB FIX (v4):
            //   The null guard below uses LaunchedEffect + targeted idempotent pop so
            //   that navController calls never fire as raw composition side effects.
            //   This prevents the exit-animation recompose from accidentally popping
            //   "dashboard" instead of "ascension", which was what blanked the Mission tab.
            composable(
                route           = Routes.ASCENSION,
                enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
                exitTransition  = { fadeOut(animationSpec = tween(300)) }
            ) { backStackEntry ->   // backStackEntry = NavBackStackEntry for the "ascension" route

                // ── BACK STACK SAFETY CHECK (v4) ──────────────────────────────
                // Same pattern as the "rankup" route — see its SAFETY NOTE for the
                // full explanation. Short version: we must confirm "dashboard" is still
                // on the back stack before calling getBackStackEntry(), because during
                // the 300 ms exit animation this composable can be recomposed AFTER the
                // real popBackStack() has already fired. Reading currentBackStack.value
                // is a plain Kotlin property access, not a composable call — legal here.
                val isDashboardOnStack = navController.currentBackStack.value
                    .any { it.destination.route == Routes.DASHBOARD }

                if (!isDashboardOnStack) {
                    // Dashboard is not on the back stack — ascension should not be visible.
                    // Pop via LaunchedEffect (safe side effect, runs once per entry instance).
                    // popBackStack(Routes.ASCENSION, inclusive=true) is idempotent:
                    // if ascension is already gone it returns false without touching dashboard.
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.ASCENSION, inclusive = true)
                    }
                    return@composable
                }

                // "dashboard" is confirmed on the back stack — safe to call getBackStackEntry().
                val dashboardEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.DASHBOARD)
                }

                val dashboardViewModel: DashboardViewModel = viewModel(dashboardEntry)
                val uiState = dashboardViewModel.uiState.value

                // Guard: if somehow we arrive here with no ascensionEvent, pop back.
                // Prevents a blank AscensionBookScreen if the event was already cleared
                // (e.g. config change between navigate() and composition).
                //
                // IMPORTANT: use LaunchedEffect with a targeted route pop, NOT a direct
                // navController.popBackStack() call here. A direct pop fires during the
                // exit-animation recompose and removes DASHBOARD instead of this ascension
                // route — that is what caused the blank Mission tab bug (v4 fix).
                //
                // popBackStack(Routes.ASCENSION, inclusive=true) is idempotent: if the
                // ascension route has already been removed (e.g. by the explicit pop in
                // onFinish) it returns false and does nothing, keeping dashboard intact.
                if (uiState?.ascensionEvent == null) {
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.ASCENSION, inclusive = true)
                    }
                    return@composable
                }

                AscensionBookScreen(
                    rank     = uiState.ascensionEvent,
                    onFinish = {
                        // User tapped CLOSE THE BOOK and the closing fade finished.
                        //   1. Mark this rank's book as seen + clear ascensionEvent.
                        //   2. Pop the ascension route → returns to dashboard.
                        dashboardViewModel.onAscensionBookDismissed()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
