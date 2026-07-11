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
 *   wrapped in remember(backStackEntry) — the Navigation Compose API requires any call to
 *   getBackStackEntry() during composition to be inside remember() keyed on a
 *   NavBackStackEntry. The remember() lambda is NOT @Composable, so try/catch inside it
 *   is legal and used to handle the case where "dashboard" is no longer on the stack
 *   (returning null instead of throwing).
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
 *
 *   v7 — Hoisted DashboardAudioEffect() from DashboardScreen to MissionNavGraph.
 *        Root cause of BGM dropping on History/Stats tab: DashboardScreen leaves the
 *        composition tree on tab switch, firing DisposableEffect.onDispose() and
 *        releasing the MediaPlayer. Fix: call DashboardAudioEffect() at NavGraph
 *        level where it is never removed during tab navigation. An `active` flag
 *        (= showBottomNav) silences the player on welcome/rankup/ascension without
 *        destroying it. rankForBgm (mutableStateOf) is updated by a LaunchedEffect
 *        inside the dashboard composable block whenever the rank changes.
 *        Null-guards in rankup and ascension use LaunchedEffect for popBackStack()
 *        instead of calling it directly in the composable body (which could misfire
 *        during the exit animation and pop the wrong entry).
 *
 *   v4 — FAILED ATTEMPT — documented so it is never retried:
 *        Tried wrapping remember() and LaunchedEffect in try/catch.
 *        Error: "Try catch is not supported around composable function invocations."
 *        Tried navController.currentBackStack.value pre-check.
 *        Errors: "currentBackStack can only be accessed from within the same library
 *        group" and "StateFlow.value should not be called within composition."
 *
 *   v5 — FAILED ATTEMPT — documented so it is never retried:
 *        Tried calling getBackStackEntry() outside remember() in a try/catch, then
 *        using remember() only for the confirmed-present case.
 *        Error: "Calling getBackStackEntry during composition without using remember
 *        with a NavBackStackEntry key."
 *        The Navigation Compose lint rule requires getBackStackEntry() to ALWAYS be
 *        inside remember() keyed on a NavBackStackEntry — even a probe/check call.
 *
 *   v6 — DEFINITIVE FIX for:
 *        BUG 1 — crash "No destination with route dashboard on back stack"
 *        BUG 2 — blank Mission tab after closing the Ascension Book
 *
 *        ROOT CAUSE OF BOTH BUGS:
 *          The 300 ms fadeOut exit animation keeps rankup/ascension composables alive
 *          in the composition tree after popBackStack() has already fired. A LiveData
 *          emission during that window triggers a recompose. In the recompose:
 *          Bug 1 — the remember lambda re-ran getBackStackEntry() → dashboard was
 *                  already gone → IllegalArgumentException → crash.
 *          Bug 2 — a direct navController.popBackStack() in the composable body
 *                  fired again → popped dashboard instead of rankup/ascension →
 *                  Mission tab went blank until user switched tabs.
 *
 *        THE FIX — try/catch INSIDE the remember lambda:
 *          The remember() lambda is regular Kotlin, NOT @Composable. Therefore,
 *          try/catch inside the lambda is completely legal — it does not violate
 *          the "try/catch not supported around composable invocations" rule, because
 *          there are no @Composable calls inside the lambda at all.
 *
 *          Pattern used in both rankup and ascension routes:
 *
 *            val dashboardEntry = remember(backStackEntry) {
 *                try {
 *                    navController.getBackStackEntry(Routes.DASHBOARD)
 *                } catch (e: IllegalArgumentException) {
 *                    null   // dashboard gone — handled safely below
 *                }
 *            }
 *            // LaunchedEffect is @Composable — it lives OUTSIDE the remember lambda
 *            if (dashboardEntry == null) {
 *                LaunchedEffect(backStackEntry) {
 *                    navController.popBackStack(route, inclusive = true)
 *                }
 *                return@composable
 *            }
 *
 *          This satisfies ALL compiler and lint constraints:
 *            ✓ getBackStackEntry() is inside remember(backStackEntry) — lint satisfied
 *            ✓ try/catch is around non-composable code only — Compose compiler satisfied
 *            ✓ LaunchedEffect is outside remember and try/catch — legal
 *            ✓ No restricted APIs (currentBackStack is internal; not used here)
 *            ✓ No StateFlow.value read in composition
 *
 *          Null-guards also use LaunchedEffect + targeted idempotent pop so that
 *          navController calls never fire as raw composition side effects (Bug 2 fix).
 *          popBackStack(route, inclusive=true) is idempotent: if the route is already
 *          gone it returns false harmlessly — dashboard is never accidentally popped.
 */

package com.example.missionuncomfortable.ui.navigation

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
import androidx.compose.animation.core.tween                            // Duration spec for transitions
import androidx.compose.animation.fadeIn                                // Fade-in transition
import androidx.compose.animation.fadeOut                               // Fade-out transition
import androidx.compose.animation.slideInHorizontally                   // Horizontal slide-in (Compose 1.0+)
import androidx.compose.animation.slideOutHorizontally                  // Horizontal slide-out (Compose 1.0+)
// NOTE: slideIntoContainer / slideOutOfContainer are NOT imported — they require
// Compose Animation 1.5+ and cause "Unresolved reference" on older Compose BOMs.
// slideInHorizontally { ±it } is the Compose 1.0 equivalent.
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
import androidx.compose.runtime.mutableStateOf                          // Observable mutable state holder
import androidx.compose.runtime.remember                                // Keeps value across recompositions
import androidx.compose.runtime.setValue                                // Property delegate for mutable State
import androidx.compose.runtime.livedata.observeAsState                 // Bridges LiveData → Compose State
import androidx.compose.ui.Modifier                                     // Modifier chain
import androidx.compose.ui.graphics.Color                               // Colour class
import androidx.compose.ui.platform.LocalContext                        // Android Context inside a composable
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
import com.example.missionuncomfortable.ui.dashboard.DashboardUiState  // Default state for observeAsState
import com.example.missionuncomfortable.ui.dashboard.DashboardViewModel // ViewModel for dashboard + rank-up
import com.example.missionuncomfortable.ui.dashboard.DashboardAudioEffect // Ambient BGM composable
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

// 300ms feels snappy but not jarring. Rank-up/ascension use 400ms — more deliberate.
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
    // rememberNavController() creates a NavController and keeps it alive across
    // recompositions. The NavController drives all navigation in this composable tree.
    val navController = rememberNavController()

    // ── OBSERVE CURRENT ROUTE ──────────────────────────────────────────────────
    // currentBackStackEntryAsState() returns the current back stack entry as
    // Compose State so the UI recomposes when the destination changes.
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

    // ── AMBIENT BGM STATE ──────────────────────────────────────────────────────
    // rankForBgm persists at NavGraph level — it is updated by the dashboard
    // composable block below whenever the ViewModel reports a rank change.
    // It starts at 1 (Observer BGM) and reflects the user's actual rank as soon
    // as uiState loads. It never resets on tab switches because it lives here,
    // not inside DashboardScreen.
    val rankForBgm = remember { mutableStateOf(1) }

    // ── AMBIENT BGM PLAYER ─────────────────────────────────────────────────────
    // Called UNCONDITIONALLY — always in the composition tree regardless of tab.
    // This is what keeps the MediaPlayer alive when the user switches to History
    // or Stats: the composable never leaves the tree, so DisposableEffect never
    // fires onDispose, and the player never stops.
    //
    // active = showBottomNav means:
    //   • On Mission / History / Stats  → volume 0.45f  → audible
    //   • On welcome / rankup / ascension → volume 0f  → silenced
    // The player is NOT released when active becomes false — just muted. This
    // means audio resumes instantly on return with no prepareAsync() gap.
    DashboardAudioEffect(
        context   = LocalContext.current,
        rankLevel = rankForBgm.value,
        active    = showBottomNav
    )

    // ── SCAFFOLD ───────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = Color(0xFF0D0D0D),  // Near-black — same as ColorBackground across the app
        bottomBar = {

            // ── BOTTOM NAVIGATION BAR ──────────────────────────────────────────
            if (showBottomNav) {
                NavigationBar(
                    containerColor = ColorSurface,
                    tonalElevation = 0.dp  // No shadow — matches the flat dark aesthetic
                ) {
                    bottomNavItems.forEach { item ->

                        // Check if this tab's route is anywhere in the current destination
                        // hierarchy — correctly highlights a tab even on nested destinations.
                        val isSelected = navBackStackEntry?.destination
                            ?.hierarchy
                            ?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = isSelected,
                            onClick  = {
                                navController.navigate(item.route) {
                                    // Pop back to the graph's start destination before navigating.
                                    // Prevents a growing back stack when the user switches tabs.
                                    // saveState/restoreState preserves scroll position and VM state.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true  // Avoid duplicates on re-tap
                                    restoreState    = true  // Restore scroll / VM state on return
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
                                // The gold tint on icon + label is enough visual feedback.
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        // ── NAV HOST ──────────────────────────────────────────────────────────
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ── ROUTE: WELCOME ─────────────────────────────────────────────────
            // Shown only on first launch. Fades in so the app "opens like a door".
            // Navigates to dashboard and pops itself — Back from Dashboard exits app.
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
            // onNavigateToRankUp    → called by DashboardScreen when rankUpEvent != null
            // onNavigateToAscension → called by DashboardScreen when ascensionEvent != null
            composable(
                route           = Routes.DASHBOARD,
                enterTransition = {
                    // { -it } → initial X = -fullWidth → slides in from the LEFT
                    slideInHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { -it }
                },
                exitTransition  = {
                    // Exits to the LEFT as History/Stats slide in from the right
                    slideOutHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { -it }
                }
            ) {
                // ── RANK SYNC FOR BGM ──────────────────────────────────────────
                // The ambient BGM lives at NavGraph level (see DashboardAudioEffect
                // above) and needs to know the user's current rank. We read the
                // DashboardViewModel here — inside the composable block where its
                // ViewModelStoreOwner scope is the "dashboard" back stack entry —
                // and propagate any rank change up to rankForBgm via LaunchedEffect.
                //
                // viewModel() here returns the SAME instance as the one inside
                // DashboardScreen, because both calls share the same back stack
                // entry scope. No duplicate ViewModel is created.
                //
                // LaunchedEffect key = currentRank?.level: only fires when the
                // rank value actually changes (rank-up), not on every recomposition.
                val dashVm: DashboardViewModel = viewModel()
                val dashUiState by dashVm.uiState.observeAsState(DashboardUiState())
                LaunchedEffect(dashUiState.xpProgress?.currentRank?.level) {
                    val newLevel = dashUiState.xpProgress?.currentRank?.level
                    if (newLevel != null) rankForBgm.value = newLevel
                }

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
            // Scrollable list of completed missions. History is RIGHT of Dashboard
            // in the tab order → slides in from the right.
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
            // Bar charts: category breakdown + avg discomfort by day.
            // Stats is the RIGHTMOST tab → slides in from the right.
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
            //   Retrieved via getBackStackEntry(Routes.DASHBOARD) inside
            //   remember(backStackEntry) — required by Navigation Compose lint rule:
            //   "getBackStackEntry during composition must use remember with a
            //   NavBackStackEntry key."
            //
            // ── HOW THE FIX WORKS (v6) ──
            //   Problem: during the 300 ms fadeOut exit animation, this composable
            //   stays alive after popBackStack() fires. A LiveData recompose in that
            //   window re-runs the remember lambda. If "dashboard" is already gone,
            //   getBackStackEntry() throws IllegalArgumentException → crash.
            //
            //   The remember() lambda is regular Kotlin — NOT @Composable. Therefore,
            //   try/catch INSIDE the lambda is completely legal. It does NOT violate
            //   "try/catch not supported around composable function invocations" because
            //   there are zero @Composable calls inside the lambda.
            //
            //   We return null from the lambda on failure, then handle null with
            //   LaunchedEffect (which IS @Composable and lives OUTSIDE the lambda).
            //
            // ── APPROACHES THAT DO NOT WORK — never retry these ──
            //   (a) try/catch around remember() or LaunchedEffect:
            //       "Try catch is not supported around composable function invocations."
            //   (b) navController.currentBackStack.value:
            //       "currentBackStack can only be accessed from within the same library
            //        group" + "StateFlow.value should not be called within composition."
            //   (c) getBackStackEntry() outside remember() in a bare try/catch:
            //       "Calling getBackStackEntry during composition without using remember
            //        with a NavBackStackEntry key." — the lint rule requires it to always
            //        be inside remember(), even for a probe/check call.
            //
            // ── BLANK TAB FIX (v6) ──
            //   Null-guards use LaunchedEffect + targeted idempotent pop. A direct
            //   popBackStack() in the composable body fires again during exit-animation
            //   recomposition and pops DASHBOARD instead of RANKUP — blanking the tab.
            //   popBackStack(Routes.RANKUP, inclusive=true) is idempotent: already gone
            //   → returns false → dashboard is never accidentally removed.
            composable(
                route           = Routes.RANKUP,
                enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
                exitTransition  = { fadeOut(animationSpec = tween(300)) }
            ) { backStackEntry ->   // backStackEntry = NavBackStackEntry for the "rankup" route

                // ── STEP 1: GET DASHBOARD ENTRY — SAFELY ──────────────────────
                // getBackStackEntry() is inside remember(backStackEntry) → lint satisfied.
                // try/catch is INSIDE the lambda body. The lambda is NOT @Composable,
                // so try/catch here is legal — there are no @Composable calls inside it.
                // Returns null if "dashboard" is not on the stack (exit-animation window).
                val dashboardEntry = remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry(Routes.DASHBOARD)
                    } catch (_: IllegalArgumentException) {
                        // "dashboard" is not on the back stack — we are in the exit-animation
                        // window. Return null; the guard below will pop this route safely.
                        null
                    }
                }

                // ── STEP 2: BAIL IF DASHBOARD IS GONE ─────────────────────────
                // LaunchedEffect IS @Composable — it lives OUTSIDE remember. Legal.
                // Targeted idempotent pop: if RANKUP is already gone → returns false,
                // does nothing. Dashboard can never be accidentally popped this way.
                if (dashboardEntry == null) {
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.RANKUP, inclusive = true)
                    }
                    return@composable
                }

                // ── STEP 3: RETRIEVE SHARED VIEWMODEL ─────────────────────────
                // viewModel(dashboardEntry) scopes the lookup to the dashboard entry's
                // ViewModelStore, returning the EXISTING instance — not a new one.
                // This ensures onRankUpCelebrationComplete() clears rankUpEvent on the
                // correct object (the same one DashboardScreen is observing).
                val dashboardViewModel: DashboardViewModel = viewModel(dashboardEntry)

                // ── STEP 4: NULL GUARD ─────────────────────────────────────────
                // If rankUpEvent is null (e.g. config change cleared it before composition),
                // pop via LaunchedEffect — never as a direct composition side effect.
                // Direct navController calls in the composable body are side effects that
                // Compose may repeat during exit-animation recompositions — always use
                // LaunchedEffect for safety.
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
                        // 1. Clear rankUpEvent; may also set ascensionEvent if this rank's
                        //    book hasn't been shown yet (see DashboardViewModel KDoc).
                        // 2. Pop RANKUP → returns to dashboard.
                        // 3. DashboardScreen's LaunchedEffect detects ascensionEvent != null
                        //    and navigates to ASCENSION — storybook plays AFTER the ceremony.
                        dashboardViewModel.onRankUpCelebrationComplete()
                        navController.popBackStack()
                    }
                )
            }

            // ── ROUTE: ASCENSION ───────────────────────────────────────────────
            // Full-screen Ascension Book storybook. Navigated to when:
            //   (a) First ever dashboard load — Observer book triggered in loadInitialData()
            //   (b) After RANKUP celebration — ascensionEvent set in onRankUpCelebrationComplete()
            //
            // ── SHARED VIEWMODEL ──
            //   Same pattern as RANKUP. See that route's comments for full reasoning.
            //
            // ── HOW THE FIX WORKS (v6) ──
            //   Identical to RANKUP — try/catch INSIDE the remember lambda returns null
            //   if "dashboard" is gone, then LaunchedEffect (outside the lambda) handles
            //   the pop. See RANKUP comments for the full explanation and the list of
            //   approaches that do not work and must never be retried.
            //
            // ── BLANK TAB FIX (v6) ──
            //   Same as RANKUP: null-guard uses LaunchedEffect + idempotent targeted pop
            //   so dashboard is never accidentally removed during exit animation.
            composable(
                route           = Routes.ASCENSION,
                enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
                exitTransition  = { fadeOut(animationSpec = tween(300)) }
            ) { backStackEntry ->   // backStackEntry = NavBackStackEntry for the "ascension" route

                // ── STEP 1: GET DASHBOARD ENTRY — SAFELY ──────────────────────
                // Same pattern as RANKUP. try/catch inside the remember lambda —
                // the lambda is NOT @Composable, so this is legal.
                // Returns null if "dashboard" is not on the back stack.
                val dashboardEntry = remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry(Routes.DASHBOARD)
                    } catch (_: IllegalArgumentException) {
                        // "dashboard" is gone — exit-animation window. Return null.
                        null
                    }
                }

                // ── STEP 2: BAIL IF DASHBOARD IS GONE ─────────────────────────
                if (dashboardEntry == null) {
                    LaunchedEffect(backStackEntry) {
                        navController.popBackStack(Routes.ASCENSION, inclusive = true)
                    }
                    return@composable
                }

                // ── STEP 3: RETRIEVE SHARED VIEWMODEL ─────────────────────────
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
