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
 * ║  If you are an AI and you skip these rules, the developer will have to  ║
 * ║  manually fix every file you touch. Please respect these conventions.   ║
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
 *   "welcome"   → WelcomeScreen        — shown only on first launch
 *   "dashboard" → DashboardScreen      — main daily mission screen (start destination)
 *   "history"   → HistoryScreen        — scrollable list of completed missions
 *   "stats"     → StatsScreen          — bar charts: category + discomfort by day
 *   "rankup"    → RankUpScreen         — full-screen celebration, triggered by rankUpEvent
 *   "ascension" → AscensionBookScreen  — full-screen storybook, triggered by ascensionEvent
 *
 * ─── VIEWMODEL SHARING NOTE ───────────────────────────────────────────────────
 *
 *   DashboardViewModel, HistoryViewModel, and StatsViewModel all extend
 *   AndroidViewModel(application). Inside a NavHost, viewModel() scopes a ViewModel
 *   to the current NavBackStackEntry — meaning each destination gets its own instance.
 *
 *   The RankUpScreen and AscensionBookScreen routes need the SAME DashboardViewModel
 *   instance as the "dashboard" route. To achieve this, those composables retrieve the
 *   ViewModel from the "dashboard" back stack entry via getBackStackEntry(Routes.DASHBOARD),
 *   wrapped in remember(backStackEntry) — the Navigation Compose API requires a
 *   NavBackStackEntry key on any getBackStackEntry() call during composition.
 *
 * ─── BOTTOM NAV BAR ──────────────────────────────────────────────────────────
 *
 *   Shown on: "dashboard", "history", "stats"
 *   Hidden on: "welcome", "rankup", "ascension"
 *
 *   Design spec:
 *     - Three tabs: Mission (shield icon), History (clock icon), Stats (chart icon)
 *     - Active tab:   ColorAccentGold (#C8A84B)
 *     - Inactive tab: ColorTextSecondary (#8A8A8A)
 *     - Background:   ColorSurface (#1A1A1A)
 *     - No pill/indicator highlight (indicatorColor = Transparent)
 *
 * ─── SCREEN TRANSITIONS ──────────────────────────────────────────────────────
 *
 *   welcome  → dashboard : fade
 *   dashboard ↔ history  : horizontal slide (history from the right)
 *   dashboard ↔ stats    : horizontal slide (stats from the right)
 *   rankup               : fade in/out
 *   ascension            : fade in/out
 *
 *   WHY slideInHorizontally instead of slideIntoContainer:
 *     slideIntoContainer / slideOutOfContainer require Compose Animation 1.5+.
 *     slideInHorizontally / slideOutHorizontally work since Compose 1.0.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial implementation. Phase 8 + Phase 9 navigation wiring.
 *        Bottom nav bar (Mission / History / Stats), welcome-screen first-launch
 *        gate, rank-up route with shared ViewModel, screen slide transitions.
 *
 *   v2 — Fixed three compile errors reported after first build attempt:
 *        (1) "Unresolved reference: slideIntoContainer" + slideOutOfContainer.
 *            Replaced with slideInHorizontally / slideOutHorizontally (Compose 1.0+).
 *        (2) "Calling getBackStackEntry during composition without using remember
 *             with a NavBackStackEntry key."
 *            Changed remember(navController) → remember(backStackEntry).
 *
 *   v3 — Added ascension route for AscensionBookScreen.
 *        Null-guards in rankup and ascension use LaunchedEffect for popBackStack()
 *        instead of calling it directly in the composable body (which could misfire
 *        during the exit animation and pop the wrong entry).
 *
 *   v4 — FAILED attempt: wrapped remember() and LaunchedEffect in try/catch to guard
 *        getBackStackEntry(). Caused compile error:
 *          "Try catch is not supported around composable function invocations."
 *        Then replaced with navController.currentBackStack.value pre-check. Caused:
 *          "NavController.currentBackStack can only be accessed from within the same
 *           library group"  and  "StateFlow.value should not be called within composition."
 *        Both approaches were invalid. See v5 for the definitive fix.
 *
 *   v5 — DEFINITIVE FIX for two bugs:
 *
 *        BUG 1 (CRASH — "No destination with route dashboard is on the back stack"):
 *          getBackStackEntry(Routes.DASHBOARD) throws IllegalArgumentException when
 *          "dashboard" is not on the back stack. This occurs during the 300 ms Compose
 *          exit animation — after the explicit popBackStack() in onContinue/onFinish
 *          fires, the fadeOut transition keeps this composable alive. A LiveData
 *          recompose in that window re-runs the remember lambda and crashes.
 *
 *          FIX: call getBackStackEntry() directly (NOT inside remember) in a plain
 *          try/catch to detect whether "dashboard" is present. getBackStackEntry() is
 *          a regular NavController function — NOT a @Composable — so try/catch around
 *          it is legal. The result is stored in a Boolean. Then, OUTSIDE the try/catch,
 *          composables (LaunchedEffect, remember) are used normally. This separates the
 *          non-composable safety check from the composable side-effect handling.
 *
 *          TWO FAILED APPROACHES DOCUMENTED (so they are never re-tried):
 *            (a) try/catch around remember() or LaunchedEffect: compile error —
 *                "Try catch is not supported around composable function invocations."
 *            (b) navController.currentBackStack.value: two errors —
 *                "currentBackStack can only be accessed from within the same library group"
 *                "StateFlow.value should not be called within composition."
 *
 *        BUG 2 (BLANK MISSION TAB after closing the book):
 *          The null-guards in rankup and ascension called navController.popBackStack()
 *          directly in the composable body. During the exit animation this fired again,
 *          popping DASHBOARD instead of the current route. Mission tab went blank.
 *          FIX: null-guards use LaunchedEffect (already introduced in v3). The v5
 *          isDashboardOnStack pre-check also stops the guard from running in the
 *          exit-animation window — doubly safe.
 */

package com.example.missionuncomfortable.ui.navigation

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
import androidx.compose.animation.core.tween                            // Duration spec for transitions
import androidx.compose.animation.fadeIn                                // Fade-in transition
import androidx.compose.animation.fadeOut                               // Fade-out transition
import androidx.compose.animation.slideInHorizontally                   // Horizontal slide-in (Compose 1.0+)
import androidx.compose.animation.slideOutHorizontally                  // Horizontal slide-out (Compose 1.0+)
// NOTE: slideIntoContainer / slideOutOfContainer are NOT imported — they require
// Compose Animation 1.5+ and cause "Unresolved reference" on older BOMs.
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
 *   ic_menu_today          → shield-like calendar icon → today's mission
 *   ic_menu_recent_history → clock icon                → mission history
 *   ic_menu_sort_by_size   → bar-chart-like icon       → stats
 */
private val bottomNavItems = listOf(
    BottomNavItem(
        route   = Routes.DASHBOARD,
        label   = "Mission",
        iconRes = android.R.drawable.ic_menu_today
    ),
    BottomNavItem(
        route   = Routes.HISTORY,
        label   = "History",
        iconRes = android.R.drawable.ic_menu_recent_history
    ),
    BottomNavItem(
        route   = Routes.STATS,
        label   = "Stats",
        iconRes = android.R.drawable.ic_menu_sort_by_size
    )
)

// ─── TRANSITION DURATION ──────────────────────────────────────────────────────

// 300ms feels snappy but not jarring. Rank-up uses 400ms — more deliberate.
private const val SLIDE_DURATION_MS = 300
private const val FADE_DURATION_MS  = 400

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE — MissionNavGraph
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MissionNavGraph — the root composable that hosts the entire app navigation.
 *
 * Called from MainActivity. Owns the NavController and decides which screen to show.
 *
 * @param startDestination  The first route to display. Passed by MainActivity based on
 *                          the "has_seen_welcome" SharedPreferences flag.
 */
@Composable
fun MissionNavGraph(startDestination: String) {

    // ── NAV CONTROLLER ─────────────────────────────────────────────────────────
    val navController = rememberNavController()

    // ── OBSERVE CURRENT ROUTE ──────────────────────────────────────────────────
    // currentBackStackEntryAsState() returns the current entry as Compose State.
    // Used to decide which tab is active and whether to show the bottom nav.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ── BOTTOM NAV VISIBILITY RULE ─────────────────────────────────────────────
    // Show only on the three main tabs. Hide on welcome, rankup, ascension —
    // those are full-screen experiences where a nav bar breaks immersion.
    val showBottomNav = currentRoute in listOf(
        Routes.DASHBOARD,
        Routes.HISTORY,
        Routes.STATS
    )

    Scaffold(
        containerColor = Color(0xFF0D0D0D),
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = ColorSurface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = navBackStackEntry?.destination
                            ?.hierarchy
                            ?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = isSelected,
                            onClick  = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
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
                                // No gold pill behind the selected tab — tint is enough.
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ── ROUTE: WELCOME ─────────────────────────────────────────────────
            // Shown only on first launch. Fades in so the app "opens like a door".
            // Navigates to dashboard and pops itself so Back exits the app, not
            // returns to welcome.
            composable(
                route           = Routes.WELCOME,
                enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
                exitTransition  = { fadeOut(animationSpec = tween(FADE_DURATION_MS)) }
            ) {
                WelcomeScreen(
                    onStartJourney = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                )
            }

            // ── ROUTE: DASHBOARD ───────────────────────────────────────────────
            // Main screen: today's mission, rank badge, XP bar.
            // Dashboard is the LEFTMOST tab — slides in from left, exits to left.
            //
            // onNavigateToRankUp   → called when uiState.rankUpEvent != null
            // onNavigateToAscension→ called when uiState.ascensionEvent != null
            composable(
                route           = Routes.DASHBOARD,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { -it }
                },
                exitTransition  = {
                    slideOutHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { -it }
                }
            ) {
                DashboardScreen(
                    onNavigateToRankUp = {
                        navController.navigate(Routes.RANKUP) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAscension = {
                        navController.navigate(Routes.ASCENSION) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ── ROUTE: HISTORY ─────────────────────────────────────────────────
            // Scrollable list of all completed missions, most recent first.
            // History is RIGHT of Dashboard in the tab order → slides in from right.
            composable(
                route           = Routes.HISTORY,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { it }
                },
                exitTransition  = {
                    slideOutHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { it }
                }
            ) {
                HistoryScreen()
            }

            // ── ROUTE: STATS ───────────────────────────────────────────────────
            // Bar charts: mission count by category + avg discomfort by day.
            // Stats is the RIGHTMOST tab → slides in from right.
            composable(
                route           = Routes.STATS,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { it }
                },
                exitTransition  = {
                    slideOutHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { it }
                }
            ) {
                StatsScreen()
            }

            // ── ROUTE: RANKUP ──────────────────────────────────────────────────
            // Full-screen rank-up celebration. Navigated to automatically when
            // DashboardScreen detects uiState.rankUpEvent != null.
            //
            // ── SHARED VIEWMODEL ──
            //   Needs the SAME DashboardViewModel as the "dashboard" route.
            //   Retrieved via getBackStackEntry(Routes.DASHBOARD), wrapped in
            //   remember(backStackEntry) as required by Navigation Compose API.
            //
            // ── CRASH FIX (v5) ──
            //   getBackStackEntry() throws IllegalArgumentException when "dashboard"
            //   is not on the back stack. This happens during the Compose exit
            //   animation: the 300 ms fadeOut keeps this composable alive AFTER
            //   the explicit popBackStack() in onContinue has already fired.
            //   A LiveData recompose in that window re-runs the remember lambda → crash.
            //
            //   DEFINITIVE FIX: call getBackStackEntry() directly (NOT inside remember)
            //   in a plain try/catch to produce a Boolean. getBackStackEntry() is a
            //   regular NavController function — NOT @Composable — so try/catch around
            //   it alone is legal. Then OUTSIDE the try/catch, use LaunchedEffect and
            //   remember normally. This fully separates the non-composable safety check
            //   from the composable side-effect and caching logic.
            //
            //   TWO APPROACHES THAT DO NOT WORK (documented to prevent re-trying):
            //     (a) try/catch around remember() or LaunchedEffect:
            //         → compile error: "Try catch is not supported around composable
            //           function invocations." @Composable calls cannot be inside
            //           catch blocks — enforced by the Compose compiler.
            //     (b) navController.currentBackStack.value:
            //         → "currentBackStack can only be accessed from within the same
            //           library group" (restricted internal API)
            //         → "StateFlow.value should not be called within composition"
            //
            // ── BLANK TAB FIX (v5) ──
            //   Null-guards use LaunchedEffect + targeted idempotent pop so NavController
            //   calls never fire as raw composition side effects. A direct popBackStack()
            //   in the composable body fires again during exit-animation recomposition
            //   and pops DASHBOARD instead of this route — blanking the Mission tab.
            //   popBackStack(Routes.RANKUP, inclusive=true) is idempotent: if "rankup"
            //   is already gone from the stack it returns false and does nothing.
            composable(
                route           = Routes.RANKUP,
                enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
                exitTransition  = { fadeOut(animationSpec = tween(300)) }
            ) { backStackEntry ->

                // ── STEP 1: BACK STACK SAFETY CHECK ───────────────────────────
                // getBackStackEntry() is NOT @Composable — try/catch around it is legal.
                // We use the result ONLY to set a Boolean; composables come after.
                val isDashboardOnStack: Boolean = try {
                    navController.getBackStackEntry(Routes.DASHBOARD)
                    true   // No exception → "dashboard" is on the back stack
                } catch (e: IllegalArgumentException) {
                    false  // Threw → "dashboard" is absent (exit-animation window)
                }

                // ── STEP 2: BAIL IF DASHBOARD IS GONE ─────────────────────────
                // LaunchedEffect IS @Composable — it is OUTSIDE the try/catch (legal).
                // Uses a targeted idempotent pop: if "rankup" is already gone, returns
                // false harmlessly — can never accidentally pop "dashboard".
                if (!isDashboardOnStack) {
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.RANKUP, inclusive = true)
                    }
                    return@composable
                }

                // ── STEP 3: RETRIEVE SHARED VIEWMODEL ─────────────────────────
                // "dashboard" is confirmed on the stack — safe to call getBackStackEntry().
                // remember(backStackEntry) satisfies the Navigation Compose requirement
                // that getBackStackEntry() calls during composition be keyed on a
                // NavBackStackEntry (not navController alone).
                val dashboardEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.DASHBOARD)
                }
                val dashboardViewModel: DashboardViewModel = viewModel(dashboardEntry)

                // ── STEP 4: NULL GUARD ─────────────────────────────────────────
                // If rankUpEvent is null (e.g. config change cleared it before we composed),
                // pop back via LaunchedEffect — never as a direct composition side effect.
                val uiState = dashboardViewModel.uiState.value
                if (uiState?.rankUpEvent == null) {
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.RANKUP, inclusive = true)
                    }
                    return@composable
                }

                // ── STEP 5: SHOW THE SCREEN ────────────────────────────────────
                RankUpScreen(
                    newRank    = uiState.rankUpEvent,
                    onContinue = {
                        // Clear rankUpEvent (and possibly set ascensionEvent) in ViewModel,
                        // then pop back to dashboard. DashboardScreen's LaunchedEffect will
                        // then navigate to ASCENSION if ascensionEvent is non-null.
                        dashboardViewModel.onRankUpCelebrationComplete()
                        navController.popBackStack()
                    }
                )
            }

            // ── ROUTE: ASCENSION ───────────────────────────────────────────────
            // Full-screen Ascension Book storybook. Navigated to when:
            //   (a) First ever dashboard load (Observer book, set in loadInitialData()), or
            //   (b) Right after RANKUP pops back to dashboard and ascensionEvent is set
            //       (see DashboardViewModel.onRankUpCelebrationComplete).
            //
            // ── SHARED VIEWMODEL ──
            //   Same pattern as the "rankup" route — see its comment for full details.
            //
            // ── CRASH FIX (v5) ──
            //   Identical pattern to "rankup": try/catch around the non-composable
            //   getBackStackEntry() call produces a Boolean, then LaunchedEffect and
            //   remember are used outside the try/catch. See "rankup" comments above
            //   for the full explanation and the two failed approaches that must not
            //   be re-tried.
            //
            // ── BLANK TAB FIX (v5) ──
            //   Same as "rankup": null-guard uses LaunchedEffect + idempotent targeted
            //   pop so dashboard is never accidentally removed during exit animation.
            composable(
                route           = Routes.ASCENSION,
                enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
                exitTransition  = { fadeOut(animationSpec = tween(300)) }
            ) { backStackEntry ->

                // ── STEP 1: BACK STACK SAFETY CHECK ───────────────────────────
                // Same pattern as "rankup" — getBackStackEntry() is NOT @Composable,
                // so try/catch around it alone is legal. Composables come after.
                val isDashboardOnStack: Boolean = try {
                    navController.getBackStackEntry(Routes.DASHBOARD)
                    true   // No exception → "dashboard" is on the back stack
                } catch (e: IllegalArgumentException) {
                    false  // Threw → "dashboard" is absent (exit-animation window)
                }

                // ── STEP 2: BAIL IF DASHBOARD IS GONE ─────────────────────────
                if (!isDashboardOnStack) {
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.ASCENSION, inclusive = true)
                    }
                    return@composable
                }

                // ── STEP 3: RETRIEVE SHARED VIEWMODEL ─────────────────────────
                val dashboardEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.DASHBOARD)
                }
                val dashboardViewModel: DashboardViewModel = viewModel(dashboardEntry)

                // ── STEP 4: NULL GUARD ─────────────────────────────────────────
                val uiState = dashboardViewModel.uiState.value
                if (uiState?.ascensionEvent == null) {
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.ASCENSION, inclusive = true)
                    }
                    return@composable
                }

                // ── STEP 5: SHOW THE SCREEN ────────────────────────────────────
                AscensionBookScreen(
                    rank     = uiState.ascensionEvent,
                    onFinish = {
                        // Mark this rank's book as seen, clear ascensionEvent, pop back.
                        dashboardViewModel.onAscensionBookDismissed()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
