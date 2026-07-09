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
 * ║  If you are an AI and you skip these rules, the developer will have to   ║
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
 *
 * ─── VIEWMODEL SHARING NOTE ───────────────────────────────────────────────────
 *
 *   DashboardViewModel, HistoryViewModel, and StatsViewModel all extend
 *   AndroidViewModel(application). Inside a NavHost, viewModel() scopes a ViewModel
 *   to the current NavBackStackEntry — meaning each destination gets its own instance.
 *
 *   The RankUpScreen route needs the SAME DashboardViewModel instance as the
 *   "dashboard" route (so it can call onRankUpCelebrationComplete() on the right object).
 *   To achieve this, the "rankup" composable retrieves the ViewModel from the
 *   "dashboard" back stack entry via navController.getBackStackEntry(Routes.DASHBOARD).
 *   This is the standard Compose Navigation pattern for shared ViewModels across routes.
 *
 * ─── BOTTOM NAV BAR ──────────────────────────────────────────────────────────
 *
 *   Shown on: "dashboard", "history", "stats"
 *   Hidden on: "welcome", "rankup"
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
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial implementation. Phase 8 + Phase 9 navigation wiring.
 *        Bottom nav bar (Mission / History / Stats), welcome-screen first-launch
 *        gate, rank-up route with shared ViewModel, screen slide transitions.
 */

package com.example.missionuncomfortable.ui.navigation

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
import androidx.compose.animation.AnimatedContentTransitionScope        // slideIntoContainer direction
import androidx.compose.animation.core.tween                            // Duration spec for transitions
import androidx.compose.animation.fadeIn                                // Fade-in transition
import androidx.compose.animation.fadeOut                               // Fade-out transition
import androidx.compose.animation.slideIntoContainer                    // Slide-in from a direction
import androidx.compose.animation.slideOutOfContainer                   // Slide-out to a direction
import androidx.compose.foundation.background                           // Background colour modifier
import androidx.compose.foundation.layout.Box                           // Layering composable
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
import com.example.missionuncomfortable.R                               // App resource references
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
    const val WELCOME   = "welcome"    // First-launch intro screen
    const val DASHBOARD = "dashboard"  // Main daily mission screen (start destination)
    const val HISTORY   = "history"    // Completed mission history list
    const val STATS     = "stats"      // Category + discomfort bar charts
    const val RANKUP    = "rankup"     // Full-screen rank-up celebration overlay
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
 *   three main tabs (dashboard, history, stats). It is hidden on the welcome and
 *   rankup routes so they appear as full-screen experiences.
 *
 * ── Rank-up flow ──
 *   DashboardScreen detects uiState.rankUpEvent != null via a LaunchedEffect
 *   and calls onNavigateToRankUp() → navController navigates to Routes.RANKUP.
 *   RankUpScreen calls onContinue() → ViewModel.onRankUpCelebrationComplete()
 *   → navController.popBackStack() returns to dashboard.
 *   The DashboardViewModel is retrieved from the "dashboard" back stack entry in
 *   the rankup route so both routes share the same ViewModel instance.
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
    // The welcome screen and the rank-up screen are full-screen experiences —
    // showing a nav bar on them would break the immersion.
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
            //   1. MainActivity already wrote has_seen_welcome = true before launching NavGraph.
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
            // Slides in from the right when navigating back from History or Stats.
            // onNavigateToRankUp is called by DashboardScreen's LaunchedEffect when
            // uiState.rankUpEvent is non-null (i.e. a rank-up just occurred).
            composable(
                route           = Routes.DASHBOARD,
                enterTransition = {
                    slideIntoContainer(
                        towards       = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(SLIDE_DURATION_MS)
                    )
                },
                exitTransition  = {
                    slideOutOfContainer(
                        towards       = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(SLIDE_DURATION_MS)
                    )
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
                    }
                )
            }

            // ── ROUTE: HISTORY ─────────────────────────────────────────────────
            // Scrollable list of all completed missions, most recent first.
            // Slides in from the left when the user taps the History tab.
            composable(
                route           = Routes.HISTORY,
                enterTransition = {
                    slideIntoContainer(
                        towards       = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(SLIDE_DURATION_MS)
                    )
                },
                exitTransition  = {
                    slideOutOfContainer(
                        towards       = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(SLIDE_DURATION_MS)
                    )
                }
            ) {
                // viewModel() here creates a HistoryViewModel scoped to this back stack entry.
                // HistoryViewModel extends AndroidViewModel — it reads SharedPreferences on init.
                // A fresh read on each visit is correct — new history may have been saved since last visit.
                HistoryScreen()
            }

            // ── ROUTE: STATS ───────────────────────────────────────────────────
            // Bar charts: mission count by category + average discomfort by day of week.
            // Slides in from the left when the user taps the Stats tab.
            composable(
                route           = Routes.STATS,
                enterTransition = {
                    slideIntoContainer(
                        towards       = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(SLIDE_DURATION_MS)
                    )
                },
                exitTransition  = {
                    slideOutOfContainer(
                        towards       = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(SLIDE_DURATION_MS)
                    )
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
            //   Fix: retrieve the "dashboard" NavBackStackEntry and get its ViewModel from that.
            //   This is the standard Compose Navigation pattern for sharing ViewModels across routes.
            //   See: https://developer.android.com/jetpack/compose/navigation#viewmodels
            composable(
                route           = Routes.RANKUP,
                enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
                exitTransition  = { fadeOut(animationSpec = tween(300)) }
            ) {
                // Get the dashboard back stack entry so we can retrieve its ViewModel.
                // remember() caches the result so it isn't recomputed on every recomposition.
                val dashboardEntry = remember(navController) {
                    navController.getBackStackEntry(Routes.DASHBOARD)
                }

                // Retrieve the SAME DashboardViewModel instance that DashboardScreen is using.
                // This ensures onRankUpCelebrationComplete() clears the event on the right object.
                val dashboardViewModel: DashboardViewModel = viewModel(dashboardEntry)

                // uiState holds rankUpEvent — the Rank object the user just achieved.
                // We observe it here to pass newRank to RankUpScreen.
                val uiState = dashboardViewModel.uiState.value

                // Guard: if somehow we arrive here with no rankUpEvent, pop back immediately.
                // This prevents a blank RankUpScreen if the event was already cleared.
                if (uiState?.rankUpEvent == null) {
                    navController.popBackStack()
                    return@composable
                }

                RankUpScreen(
                    newRank    = uiState.rankUpEvent,
                    onContinue = {
                        // User tapped CONTINUE on the celebration screen.
                        //   1. Clear rankUpEvent + set isDailyComplete = true in the ViewModel.
                        //   2. Pop the rankup route → returns to dashboard.
                        dashboardViewModel.onRankUpCelebrationComplete()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
