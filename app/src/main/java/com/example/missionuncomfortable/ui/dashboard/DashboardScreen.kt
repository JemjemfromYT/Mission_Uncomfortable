/**
 * DashboardScreen.kt
 *
 * This is the PRIMARY screen of Mission: Uncomfortable.
 * It is the first screen the user sees after completing the Welcome intro.
 *
 * ─── WHAT THIS FILE CONTAINS ────────────────────────────────────────────────
 *
 *   DashboardScreen()            — The root composable. Connects to the ViewModel, observes state,
 *                                  and decides what to show (loading, error, daily-complete, or content).
 *
 *   DashboardContent()           — The main layout once data is loaded. Holds all three sections:
 *                                    1. RankBadgeSection   — rank badge + rank name
 *                                    2. XpProgressSection  — XP progress bar + labels
 *                                    3. MissionCard        — Today's active/completed mission card
 *
 *   RankBadgeSection()           — Composable for the visual rank badge at the top of the screen.
 *                                  v4:  Now reads rank.badgeResId to display rank-specific badge art.
 *                                  v11: Badge enlarged (140dp image / 170dp glow container).
 *                                       Per-rank glow added — each rank has a unique intensity.
 *
 *   XpProgressSection()          — Composable for the XP progress bar and labels.
 *                                  v3: The bar animates automatically when xpProgress changes
 *                                  because animateFloatAsState() reacts to any change in
 *                                  xpProgress.progressFraction — including after Submit Rating.
 *
 *   MissionCard()                — Composable for the large interactive mission card.
 *                                  v3: VIEW COMPLETION button now calls onViewCompletion()
 *                                  instead of onCardClicked(), triggering the daily-complete state.
 *
 *   DailyCompleteScreen()        — NEW (v3). Shown after the user taps "VIEW COMPLETION".
 *                                  Displays the updated XP bar + "Come back tomorrow" message.
 *                                  This gives the user a satisfying final look at their progress
 *                                  before the app goes quiet for the day.
 *
 *   MilitaryBriefingSection()    — Renders the mission brief in OBJECTIVE + RULES format.
 *
 *   DiscomfortSliderSection()    — 1–10 slider shown after the user accepts the mission.
 *
 *   DifficultyIndicator()        — Row of difficulty dots on the mission card.
 *   LoadingScreen()              — Shown while data is loading.
 *   ErrorScreen()                — Shown if something went wrong loading data.
 *
 * ─── DESIGN AESTHETIC ────────────────────────────────────────────────────────
 *
 *   Dark, minimal, stoic.
 *   Background: near-black (#0D0D0D)
 *   Surface/Card:  dark grey (#1A1A1A)
 *   Accent:        off-white (#E0E0E0) for primary text
 *   XP bar accent: muted gold (#C8A84B) — ties into the "rank/achievement" theme
 *   Difficulty dots: active = white, inactive = dark grey
 *
 * ─── HOW TO USE ──────────────────────────────────────────────────────────────
 *
 *   In NavGraph.kt (Phase 8+), call this composable from the "dashboard" route:
 *
 *       DashboardScreen(
 *           onNavigateToRankUp = { navController.navigate(Routes.RANKUP) }
 *       )
 *
 *   onNavigateToRankUp is called automatically by a LaunchedEffect when the
 *   ViewModel fires a rank-up event (uiState.rankUpEvent != null).
 *
 * ─── FUTURE WORK ──────────────────────────────────────────────────────────────
 *
 *   - Replace placeholder data in DashboardViewModel with Room DAO calls.
 *   - Add swipe-to-refresh (PullRefreshIndicator) once real networking is in place.
 *   - Implement onSwapMission() in the ViewModel with real Supabase alternative-mission fetch.
 *   - Enforce a daily swap limit (one swap per day) in the ViewModel.
 *   - Replace DailyCompleteScreen with a dedicated MissionCompleteScreen (NavController route)
 *     showing streak count, discomfort history chart, and next-day preview.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v2 — Applied four UI/UX updates:
 *          1. RankBadgeSection: replaced text-circle placeholder with Image composable.
 *          2. MissionCard: replaced paragraph with MilitaryBriefingSection (OBJECTIVE + RULES).
 *          3. MissionCard: added "SWAP MISSION" secondary button for location-dependent missions.
 *          4. MissionCard: added DiscomfortSliderSection (1–10 slider post-accept).
 *
 *   v3 — LIVE ENGINE integration:
 *          1. DashboardScreen: added isDailyComplete check.
 *             When true, renders DailyCompleteScreen instead of DashboardContent.
 *             DailyCompleteScreen shows the updated XP bar (so the user sees their new total)
 *             and a "Come back tomorrow" message.
 *          2. MissionCard: VIEW COMPLETION button now calls onViewCompletion() (new callback)
 *             instead of the old onCardClicked() generic handler.
 *             onViewCompletion() → ViewModel.onViewCompletion() → isDailyComplete = true.
 *          3. XpProgressSection: no code change needed — the existing animateFloatAsState()
 *             already reacts to progressFraction changes. When onSubmitReflection() updates
 *             xpProgress in the ViewModel, the bar animates automatically.
 *          4. DailyCompleteScreen composable added (new in v3).
 *
 *   v4 — RANK BADGE ART:
 *          1. RankBadgeSection now reads rank.badgeResId to load rank-specific badge art.
 *             The painterResource line changed from the hardcoded placeholder to:
 *               painterResource(id = rank.badgeResId ?: R.drawable.rank_badge_placeholder)
 *             Each rank in DashboardModels.ALL_RANKS now has its own unique drawable ID.
 *             Level 1 Observer  → badge_observer  (open eye)
 *             Level 2 Initiate  → badge_initiate  (single chevron)
 *             Level 3 Challenger→ badge_challenger (shield + sword)
 *             Level 4 Conqueror → badge_conqueror  (eagle wings)
 *             Level 5 Sovereign → badge_sovereign  (military crown)
 *
 *   v5 — SWAP MISSION BLOCKED (motivational popup):
 *          1. The "SWAP MISSION" button no longer swaps the mission.
 *             Instead, tapping it shows a motivational AlertDialog that tells the user
 *             skipping is forbidden and they must face the challenge.
 *             This reinforces the app's core philosophy: discomfort must be confronted,
 *             not escaped.
 *          2. SwapBlockedDialog composable added (new in v5).
 *             It is a standalone, reusable private composable so it can be evolved later
 *             (e.g., randomising from a pool of motivational quotes) without touching MissionCard.
 *          3. MissionCard: added showSwapBlockedDialog local state (mutableStateOf false).
 *             The SWAP MISSION button sets showSwapBlockedDialog = true.
 *             Dismissing the dialog sets it back to false.
 *          4. The onSwapMission parameter is kept in all composable signatures for
 *             future use (e.g., if you later want to allow swaps after a penalty,
 *             or after watching an ad), but it is NOT called from the button.
 *
 *   v10 — Phase 8 (Navigation): added onNavigateToRankUp parameter and LaunchedEffect.
 *          When the ViewModel fires a rank-up event (uiState.rankUpEvent != null),
 *          the LaunchedEffect calls onNavigateToRankUp() so NavGraph can navigate to
 *          the RankUpScreen route. This decouples DashboardScreen from NavController —
 *          the Screen just notifies its caller, and the caller decides how to navigate.
 *
 *   v11 — RANK BADGE VISUAL UPGRADE:
 *          1. RankBadgeSection: badge image enlarged from 120dp to 140dp.
 *             A 170dp container Box now wraps the badge, giving 15dp of glow halo space
 *             on each side without clipping the badge art.
 *          2. Added rank-specific glow animation drawn behind the badge via
 *             Brush.radialGradient + drawBehind. No external library required.
 *             Observer  (1): no glow — nothing earned yet.
 *             Initiate  (2): faint static halo — barely visible, first sign of progress.
 *             Challenger(3): slow breathing pulse (2200ms half-cycle).
 *             Conqueror (4): strong pulsing glow (1600ms half-cycle).
 *             Sovereign (5): rapid full shimmer (1100ms half-cycle).
 *          3. New imports: FastOutSlowInEasing, RepeatMode, animateFloat,
 *             infiniteRepeatable, rememberInfiniteTransition, drawBehind, Brush.
 */

package com.example.missionuncomfortable.ui.dashboard

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
// Jetpack Compose UI building blocks
import androidx.compose.animation.core.FastOutSlowInEasing          // Easing curve for the glow pulse animation
import androidx.compose.animation.core.RepeatMode                   // Reverse mode — glow pulse goes back and forth
import androidx.compose.animation.core.animateFloat                 // Float animation on an InfiniteTransition
import androidx.compose.animation.core.animateFloatAsState          // Smoothly animates a float value
import androidx.compose.animation.core.infiniteRepeatable           // Loops the glow animation forever
import androidx.compose.animation.core.rememberInfiniteTransition   // Creates the infinite animation driver for the glow
import androidx.compose.animation.core.tween                        // Animation timing spec
import androidx.compose.foundation.Image                            // Image composable for rank badge
import androidx.compose.foundation.background                       // Sets the background colour of a composable
import androidx.compose.foundation.clickable                        // Makes a composable respond to taps
import androidx.compose.foundation.layout.*                         // Box, Column, Row, Spacer, padding, fillMaxSize, etc.
import androidx.compose.foundation.rememberScrollState              // Remembers scroll position of a scrollable column
import androidx.compose.foundation.shape.CircleShape                // Circular shape for difficulty dots
import androidx.compose.foundation.shape.RoundedCornerShape         // Rounded corners for cards and progress bar
import androidx.compose.foundation.verticalScroll                   // Makes a Column scrollable vertically
import androidx.compose.material3.*                                 // Material 3 components: Text, Slider, AlertDialog, CircularProgressIndicator, etc.
import androidx.compose.runtime.*                                   // remember, LaunchedEffect, getValue, mutableStateOf, etc.
import androidx.compose.ui.Alignment                                // Alignment constants (center, start, end, etc.)
import androidx.compose.ui.Modifier                                 // Modifier — the "how does it look and behave" chain
import androidx.compose.ui.draw.clip                                // Clips a composable to a specific shape
import androidx.compose.ui.draw.drawBehind                          // Draws the radial glow circle behind the badge container
import androidx.compose.ui.graphics.Brush                           // Brush.radialGradient — used to shape the glow
import androidx.compose.ui.graphics.Color                           // Colour class — use hex values like Color(0xFF...)
import androidx.compose.ui.layout.ContentScale                      // Controls how Image content is scaled inside bounds
import androidx.compose.ui.res.painterResource                      // Loads a drawable resource for use in Image()
import androidx.compose.ui.text.font.FontWeight                     // Bold, normal, etc.
import androidx.compose.ui.text.style.TextAlign                     // Centre, start, end alignment for text
import androidx.compose.ui.unit.dp                                  // Density-independent pixels for spacing/sizing
import androidx.compose.ui.unit.sp                                  // Scale-independent pixels for font sizes
import androidx.compose.runtime.livedata.observeAsState             // Bridges LiveData → Compose State
import androidx.lifecycle.viewmodel.compose.viewModel               // Gets or creates a ViewModel scoped to this composable
import kotlin.math.roundToInt                                       // Accurate Float-to-Int rounding for the discomfort slider
// Admin panel — secret testing overlay (v7: new).
// Activation: tap the rank badge 7 times rapidly on the Dashboard.
import com.example.missionuncomfortable.ui.admin.AdminPanel

// ─── COLOUR CONSTANTS ────────────────────────────────────────────────────────
// Defined here so if we ever want to change the design system colours, we change them in ONE place.

private val ColorBackground = Color(0xFF0D0D0D)        // Near-black — the main screen background
private val ColorSurface = Color(0xFF1A1A1A)           // Dark grey — card/surface backgrounds
private val ColorSurfaceVariant = Color(0xFF242424)    // Slightly lighter grey — for secondary surfaces
private val ColorTextPrimary = Color(0xFFE0E0E0)       // Off-white — main text colour
private val ColorTextSecondary = Color(0xFF8A8A8A)     // Muted grey — labels, hints, secondary text
private val ColorAccentGold = Color(0xFFC8A84B)        // Muted gold — XP bar fill and accent highlights
private val ColorDivider = Color(0xFF2E2E2E)           // Very subtle divider line colour

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE — DashboardScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DashboardScreen — the root composable for the Dashboard.
 *
 * This is the ONLY composable that knows about the ViewModel. All child composables
 * receive plain data (XpProgress, Mission, etc.) rather than the ViewModel itself.
 * This makes child composables easy to test and preview in isolation.
 *
 * v3: Added isDailyComplete routing.
 *   - When isDailyComplete is false (normal operation): shows DashboardContent.
 *   - When isDailyComplete is true (after VIEW COMPLETION): shows DailyCompleteScreen.
 *     DailyCompleteScreen still receives xpProgress so it can show the updated XP bar.
 *
 * v10 (Phase 8): Added onNavigateToRankUp parameter and LaunchedEffect.
 *   - When uiState.rankUpEvent is non-null, the LaunchedEffect fires onNavigateToRankUp().
 *   - The caller (NavGraph.kt) passes { navController.navigate(Routes.RANKUP) }.
 *   - DashboardScreen itself has NO NavController dependency — it only calls the callback.
 *     This keeps the composable decoupled from navigation infrastructure.
 *
 * @param viewModel           The ViewModel that manages this screen's state.
 *                            Provided automatically by `viewModel()` — you don't pass one manually.
 * @param onMissionClicked    Called when the user taps the mission card area (not the action buttons).
 *                            Use this to navigate to the Mission Detail screen in the future.
 * @param onNavigateToRankUp  v10: Called when uiState.rankUpEvent becomes non-null.
 *                            The caller is responsible for navigating to RankUpScreen.
 *                            Defaults to a no-op so Previews and tests don't crash.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),   // Auto-creates or retrieves existing ViewModel
    onMissionClicked: () -> Unit = {},             // Default to no-op so Previews don't crash
    onNavigateToRankUp: () -> Unit = {}            // v10: called when a rank-up event is detected
) {
    // Observe the ViewModel's LiveData as a Compose State.
    // The `?: DashboardUiState()` provides a safe default while the first value arrives.
    val uiState by viewModel.uiState.observeAsState(DashboardUiState())

    // ── v10: RANK-UP NAVIGATION ────────────────────────────────────────────────
    // When the ViewModel fires a rank-up event (rankUpEvent != null),
    // call onNavigateToRankUp() to trigger NavController navigation to RankUpScreen.
    // LaunchedEffect key = uiState.rankUpEvent so this only fires when rankUpEvent
    // changes value — not on every recomposition. A null→non-null transition is what
    // triggers the navigation; a non-null→null transition (after celebration completes)
    // does nothing (the if-guard below prevents a spurious second navigation).
    LaunchedEffect(uiState.rankUpEvent) {
        if (uiState.rankUpEvent != null) {
            onNavigateToRankUp()
        }
    }

    // ── v7: ADMIN PANEL STATE ─────────────────────────────────────────────────
    // Controls whether the secret admin overlay is visible.
    // Activated by tapping the rank badge 7 times rapidly (handled in RankBadgeSection).
    // False by default — the panel is invisible to regular users.
    var showAdminPanel by remember { mutableStateOf(false) }

    // The outer Box fills the entire screen with the dark background colour.
    // Everything else is drawn on top of this Box.
    Box(
        modifier = Modifier
            .fillMaxSize()                          // Take up the whole screen
            .background(ColorBackground)            // Paint the background near-black
    ) {
        // Switch on the current state to decide what to show the user
        when {
            // ── CASE 1: Still loading data ────────────────────────────────────
            uiState.isLoading -> {
                // Show a centred loading spinner while we wait for data
                LoadingScreen()
            }

            // ── CASE 2: An error occurred ─────────────────────────────────────
            uiState.errorMessage != null -> {
                // Show an error message with a retry button
                ErrorScreen(
                    message = uiState.errorMessage!!,  // !! safe here — we checked != null above
                    onRetry = { viewModel.onRefresh() } // Retry calls the ViewModel's refresh function
                )
            }

            // ── CASE 3: User has completed today's mission ────────────────────
            // v3: isDailyComplete is set to true by ViewModel.onViewCompletion().
            // Show the "Come back tomorrow" screen instead of the mission card.
            // We still pass xpProgress so the user can see their updated XP total.
            // v7: Pass onAdminTriggered so admin is reachable from this screen too.
            //     Secret tap target: the "MISSION COMPLETE" gold text at the top.
            uiState.isDailyComplete -> {
                DailyCompleteScreen(
                    xpProgress       = uiState.xpProgress,  // May be null if somehow reached without data
                    onAdminTriggered = { showAdminPanel = true }
                )
            }

            // ── CASE 4: Data is ready — show the full dashboard ───────────────
            else -> {
                // Both xpProgress and todaysMission should be non-null at this point.
                // We use safe-calls with ?: just to be defensive.
                if (uiState.xpProgress != null && uiState.todaysMission != null) {
                    DashboardContent(
                        xpProgress = uiState.xpProgress!!,
                        todaysMission = uiState.todaysMission!!,
                        showReflectionSlider = uiState.showReflectionSlider,
                        currentDiscomfortRating = uiState.currentDiscomfortRating,
                        onMissionClicked = {
                            viewModel.onMissionCardClicked()     // Notify ViewModel user tapped the card
                            onMissionClicked()                   // Trigger navigation from the caller
                        },
                        onAcceptMission = { viewModel.onAcceptMission() },
                        onSwapMission = { viewModel.onSwapMission() },
                        onMissionDone = { viewModel.onMissionDone() },           // v6: "I DID IT"
                        onMissionAbandoned = { viewModel.onMissionAbandoned() }, // v6: "I COULDN'T DO IT"
                        onDiscomfortRatingChanged = { rating -> viewModel.onDiscomfortRatingChanged(rating) },
                        onSubmitReflection = { viewModel.onSubmitReflection() },
                        onViewCompletion = { viewModel.onViewCompletion() },     // v3: new callback
                        // v7: callback fired when the secret tap sequence completes in RankBadgeSection.
                        // Sets showAdminPanel = true, which renders AdminPanel as an overlay above all content.
                        onAdminTriggered = { showAdminPanel = true }
                    )
                }
            }
        }

        // ── v7: ADMIN PANEL OVERLAY ───────────────────────────────────────────
        // Rendered as a child of the root Box so it floats above ALL content
        // (including DailyCompleteScreen, LoadingScreen, and DashboardContent).
        // Only visible after the secret 7-tap gesture on the rank badge.
        if (showAdminPanel) {
            AdminPanel(
                onDismiss = { showAdminPanel = false },
                onRefresh = { viewModel.onRefresh() }   // Reload ViewModel state from updated prefs
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DASHBOARD CONTENT — the main layout shown when data is loaded
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DashboardContent — lays out all three main sections of the dashboard.
 *
 * Uses a vertically scrollable Column so the screen works on small devices too.
 *
 * v3: Added onViewCompletion parameter and passes it down to MissionCard.
 * v6: Added onMissionDone and onMissionAbandoned for the IN_PROGRESS return flow.
 *
 * @param xpProgress               The XP and rank progress data to display.
 * @param todaysMission            The mission assigned to the user today.
 * @param showReflectionSlider     True when the discomfort slider should be shown in the card.
 * @param currentDiscomfortRating  The live slider position (1–10) before submission.
 * @param onMissionClicked         Called when the user taps the card area (for future navigation).
 * @param onAcceptMission          Called when the user taps "ACCEPT THE MISSION".
 * @param onSwapMission            Called when the user taps "SWAP MISSION".
 *                                 NOTE (v5): The SWAP MISSION button no longer calls this directly.
 *                                 Instead, it shows a motivational popup (SwapBlockedDialog).
 *                                 This parameter is kept for future use (e.g., allow swap after a penalty).
 * @param onMissionDone            v6: Called when the user taps "I DID IT".
 * @param onMissionAbandoned       v6: Called when the user taps "I COULDN'T DO IT".
 * @param onDiscomfortRatingChanged Called continuously as the user drags the slider.
 * @param onSubmitReflection       Called when the user taps "SUBMIT RATING".
 * @param onViewCompletion         v3: Called when the user taps "VIEW COMPLETION" on a COMPLETED mission.
 *                                 Triggers the daily-complete state → DailyCompleteScreen is shown.
 */
@Composable
private fun DashboardContent(
    xpProgress: XpProgress,
    todaysMission: Mission,
    showReflectionSlider: Boolean,
    currentDiscomfortRating: Int,
    onMissionClicked: () -> Unit,
    onAcceptMission: () -> Unit,
    onSwapMission: () -> Unit,
    onMissionDone: () -> Unit,               // v6: "I DID IT" → shows discomfort slider
    onMissionAbandoned: () -> Unit,          // v6: "I COULDN'T DO IT" → resets to ACTIVE
    onDiscomfortRatingChanged: (Int) -> Unit,
    onSubmitReflection: () -> Unit,
    onViewCompletion: () -> Unit,            // v3: new parameter
    onAdminTriggered: () -> Unit             // v7: called when secret 7-tap gesture completes
) {
    // rememberScrollState() tracks scroll position so it survives recompositions
    val scrollState = rememberScrollState()

    // A vertical Column is the main container for the screen.
    // It stacks the three sections top-to-bottom.
    Column(
        modifier = Modifier
            .fillMaxSize()                     // Fill the whole screen
            .verticalScroll(scrollState)       // Make the whole screen scrollable
            .padding(horizontal = 20.dp)       // 20dp left/right padding on all sections
            .padding(
                top = 48.dp,                   // Extra top padding for status bar clearance
                bottom = 32.dp                 // Extra bottom padding so content doesn't get clipped
            ),
        horizontalAlignment = Alignment.CenterHorizontally  // Centre children horizontally
    ) {

        // ── SECTION 1: Rank Badge ─────────────────────────────────────────────
        // Displays a large badge image + the rank name below it.
        // v7:  onAdminTriggered is passed through so RankBadgeSection can fire it
        //      after detecting the secret 7-tap gesture on the badge image.
        // v11: Badge is now 140dp with a 170dp glow container and per-rank glow animation.
        RankBadgeSection(rank = xpProgress.currentRank, onSecretTap = onAdminTriggered)

        Spacer(modifier = Modifier.height(28.dp))  // Vertical gap between badge and XP bar

        // ── SECTION 2: XP Progress Bar ───────────────────────────────────────
        // Shows how close the user is to ranking up.
        // v3: The bar animates automatically when xpProgress.progressFraction changes
        // (i.e., after Submit Rating adds XP in the ViewModel).
        XpProgressSection(xpProgress = xpProgress)

        Spacer(modifier = Modifier.height(36.dp))  // Vertical gap between XP bar and mission card

        // ── SECTION 3: Today's Mission Card ──────────────────────────────────
        // The large interactive card displaying the active daily mission.
        MissionCard(
            mission = todaysMission,
            showReflectionSlider = showReflectionSlider,
            currentDiscomfortRating = currentDiscomfortRating,
            onCardClicked = onMissionClicked,
            onAcceptMission = onAcceptMission,
            onSwapMission = onSwapMission,
            onMissionDone = onMissionDone,               // v6: passed through to "I DID IT" button
            onMissionAbandoned = onMissionAbandoned,     // v6: passed through to "I COULDN'T DO IT" button
            onDiscomfortRatingChanged = onDiscomfortRatingChanged,
            onSubmitReflection = onSubmitReflection,
            onViewCompletion = onViewCompletion    // v3: passed through to the button handler
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 1 — Rank Badge
// ─────────────────────────────────────────────────────────────────────────────

/**
 * RankBadgeSection — displays the user's current rank badge and title.
 *
 * v4:  Now uses rank.badgeResId to display rank-specific badge art.
 *      Each of the 5 ranks (Observer → Sovereign) has its own unique vector drawable.
 *      The null-safe branch is retained as a safety net in case a rank is added to
 *      ALL_RANKS without a badge being created yet.
 *
 * v7:  Added onSecretTap parameter and 7-tap secret detection.
 *      Tapping the badge area 7 times within 3 seconds fires onSecretTap(),
 *      which DashboardScreen uses to show the AdminPanel overlay.
 *
 * v11: Badge image enlarged from 120dp to 140dp.
 *      A 170dp container Box now wraps the badge so the radial glow can extend
 *      15dp beyond the badge circle on each side without clipping.
 *      Per-rank glow animation added via Brush.radialGradient drawn with drawBehind:
 *        Observer  (1): no glow — nothing has been earned yet.
 *        Initiate  (2): faint static halo — the first signal of something earned.
 *        Challenger(3): slow breathing pulse (2200ms half-cycle).
 *        Conqueror (4): strong pulsing glow (1600ms half-cycle).
 *        Sovereign (5): rapid full shimmer (1100ms half-cycle) — the apex.
 *
 * @param rank         The user's current Rank object (level, title, description, badgeResId).
 * @param onSecretTap  v7: Called when the secret 7-tap gesture completes. Used to show AdminPanel.
 */
@Composable
private fun RankBadgeSection(rank: Rank, onSecretTap: () -> Unit) {

    // ── v7: SECRET TAP DETECTION STATE ───────────────────────────────────────
    // Tracks how many times the badge area has been tapped and when the first tap happened.
    // If 7 taps occur within 3 seconds of the first tap, onSecretTap() is fired.
    // If more than 3 seconds pass between the first and a subsequent tap, the count resets.
    //
    // Why 7 taps / 3 seconds?
    //   - 7 taps is too many to happen accidentally during normal use.
    //   - 3 seconds is short enough that only deliberate rapid tapping triggers it.
    //   - The gesture is memorable but completely invisible — no visual indicator.
    var tapCount     by remember { mutableStateOf(0) }
    var firstTapTime by remember { mutableStateOf(0L) }

    // ── v11: RANK-SPECIFIC GLOW ANIMATION ─────────────────────────────────────
    // Glow colour matches the gold accent used throughout this file.
    // The glow is drawn behind the badge via Brush.radialGradient + drawBehind.
    // No external library is required — this is pure Compose Canvas.
    val glowColor = ColorAccentGold

    // Per-rank animation parameters — Triple(minAlpha, maxAlpha, halfCycleDurationMs).
    //   Levels 1-2: minAlpha == maxAlpha → glow is static, no visible pulse.
    //   Levels 3-5: minAlpha < maxAlpha  → alpha oscillates, badge breathes.
    //
    // Why these values?
    //   - Observer starts at zero: you have earned nothing yet, the badge is cold.
    //   - Each rank adds more light: the badge visually reflects accumulated effort.
    //   - Speed also increases: Sovereign's shimmer is noticeably faster than Challenger's pulse.
    val (glowAlphaMin, glowAlphaMax, glowDurationMs) = when (rank.level) {
        1    -> Triple(0.00f, 0.00f, 3000)   // Observer:    no glow at all
        2    -> Triple(0.08f, 0.08f, 3000)   // Initiate:    faint static halo, barely visible
        3    -> Triple(0.10f, 0.28f, 2200)   // Challenger:  slow breathing pulse
        4    -> Triple(0.20f, 0.48f, 1600)   // Conqueror:   strong pulsing glow
        else -> Triple(0.32f, 0.68f, 1100)   // Sovereign:   rapid shimmer, visibly alive
    }

    val infiniteTransition = rememberInfiniteTransition(label = "BadgeGlow")

    val animatedGlowAlpha by infiniteTransition.animateFloat(
        initialValue  = glowAlphaMin,
        targetValue   = glowAlphaMax,
        animationSpec = infiniteRepeatable(
            // FastOutSlowInEasing gives the pulse a natural deceleration at each peak —
            // more organic than a linear oscillation.
            animation  = tween(durationMillis = glowDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse   // Animate forward then backward — a smooth breath
        ),
        label = "BadgeGlowAlpha"
    )

    // Levels 1-2 use the fixed static minimum (min == max, so the animation
    // produces a constant value — no CPU wasted on invisible updates).
    // Levels 3-5 use the live animated value so the pulse runs continuously.
    val effectiveGlowAlpha = if (rank.level >= 3) animatedGlowAlpha else glowAlphaMin

    // Centre everything in this section horizontally
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()     // Take the full width so centring works correctly
    ) {

        // ── GLOW CONTAINER + BADGE IMAGE + SECRET TAP ZONE ────────────────────
        // The 170dp outer Box serves three purposes:
        //   1. drawBehind: draws the radial glow in this larger space so it extends
        //      15dp beyond the 140dp badge circle on each side without being clipped.
        //   2. clickable: captures the 7-tap secret gesture across the full badge area.
        //   3. Alignment.Center: keeps the 140dp badge image perfectly centred inside.
        //
        // v4: reads rank.badgeResId — each rank has its own distinct drawable.
        //   Level 1 Observer   → badge_observer   (open eye)
        //   Level 2 Initiate   → badge_initiate   (single chevron)
        //   Level 3 Challenger → badge_challenger  (shield + sword)
        //   Level 4 Conqueror  → badge_conqueror   (eagle wings)
        //   Level 5 Sovereign  → badge_sovereign   (military crown)
        //
        // v6 FIX: replaced checkNotNull() with a null-safe branch (no crash on null badgeResId).
        // v7: clickable captures secret 7-tap gesture (fires onSecretTap after 7 taps in 3s).
        // v11: container enlarged to 170dp; badge image enlarged to 140dp; glow added.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(170.dp)
                .drawBehind {
                    // Radial gradient centred on the container. Fades from gold to transparent.
                    // radius = half the container diameter so the glow fills the full circle.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = effectiveGlowAlpha),
                                Color.Transparent
                            ),
                            radius = size.minDimension / 2f
                        )
                    )
                }
                .clickable {
                    // ── SECRET TAP LOGIC (v7) ─────────────────────────────────
                    // 7 taps within 3 seconds fires onSecretTap() and opens AdminPanel.
                    val now = System.currentTimeMillis()
                    if (tapCount == 0 || now - firstTapTime > 3_000L) {
                        // First tap or the previous sequence timed out — start fresh.
                        tapCount     = 1
                        firstTapTime = now
                    } else {
                        tapCount++
                        if (tapCount >= 7) {
                            // Sequence complete — fire callback and reset the counter.
                            tapCount     = 0
                            firstTapTime = 0L
                            onSecretTap()
                        }
                    }
                }
        ) {
            if (rank.badgeResId != null) {
                // Real badge art — ContentScale.Fit keeps the SVG aspect ratio inside 140dp.
                // CircleShape masks to a circle, matching the badge drawables' circular design.
                // The 30dp margin between container (170dp) and image (140dp) is the glow halo zone.
                Image(
                    painter            = painterResource(id = rank.badgeResId),
                    contentDescription = "Rank badge for ${rank.title}",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                )
            } else {
                // Placeholder circle — shown when no badge drawable has been added yet.
                // Displays the rank's initial letter in gold on a dark circle.
                // To replace: add badge drawables to res/drawable/ and set badgeResId
                // in DashboardModels.ALL_RANKS for each rank.
                // firstOrNull() + ?: "?" guards against a blank rank title string.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(ColorSurfaceVariant)
                ) {
                    Text(
                        text       = rank.title.firstOrNull()?.uppercase() ?: "?",
                        color      = ColorAccentGold,
                        fontSize   = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))  // Gap between badge container and rank title text

        // ── RANK LABEL ────────────────────────────────────────────────────
        // "RANK" in small caps above the rank title — acts as a label/category
        Text(
            text          = "RANK",
            color         = ColorTextSecondary,    // Muted grey — this is supporting info
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 3.sp                  // Wide letter spacing for a sleek, "stamp" feel
        )

        Spacer(modifier = Modifier.height(4.dp))   // Tiny gap between label and title

        // ── RANK TITLE ────────────────────────────────────────────────────
        // The rank's name, e.g. "The Initiate", in large white text
        Text(
            text       = rank.title,
            color      = ColorTextPrimary,         // Off-white — this is the most important text here
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))   // Small gap before description

        // ── RANK DESCRIPTION ──────────────────────────────────────────────
        // Short flavour text describing the rank — less prominent than the title
        Text(
            text      = rank.description,
            color     = ColorTextSecondary,        // Muted grey — supporting flavour, not critical
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 16.dp)  // Extra horizontal padding for narrower text
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 2 — XP Progress Bar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * XpProgressSection — displays the XP progress bar and surrounding labels.
 *
 * The animated progress bar uses animateFloatAsState() to smoothly transition
 * from the old XP fraction to the new one whenever xpProgress changes.
 * This happens automatically when ViewModel.onSubmitReflection() adds XP —
 * no special animation trigger code is needed here.
 *
 * @param xpProgress  The XP and rank data to display.
 */
@Composable
private fun XpProgressSection(xpProgress: XpProgress) {

    // Animate the progress bar fill fraction — smooth transition on XP changes.
    // tween(600): the animation takes 600ms with a default ease-in-out curve.
    val animatedFraction by animateFloatAsState(
        targetValue = xpProgress.progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "XpBarAnimation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── TOP ROW: "TOTAL XP" label + "next rank at X XP" label ────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "TOTAL XP",
                color = ColorTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )

            // Show "next rank at X XP" unless the user is at max rank
            if (xpProgress.xpUntilNextRank > 0) {
                Text(
                    text = "next rank at ${xpProgress.xpForNextRank} XP",
                    color = ColorTextSecondary,
                    fontSize = 10.sp
                )
            } else {
                // User is at max rank — show a "MAX" label instead
                Text(
                    text = "MAX RANK",
                    color = ColorAccentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── CURRENT XP NUMBER ────────────────────────────────────────────
        Text(
            text = "${xpProgress.currentXp} XP",
            color = ColorTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ── PROGRESS BAR ─────────────────────────────────────────────────
        // A Box containing two overlapping Boxes:
        //   1. The full-width dark background track.
        //   2. The gold fill, whose width is animatedFraction * total width.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ColorSurfaceVariant)  // Dark grey track
        ) {
            // Gold fill — width is a fraction of the parent Box's width
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedFraction)  // Animated fill fraction
                    .fillMaxHeight()
                    .background(ColorAccentGold)               // Muted gold fill
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── BOTTOM LABEL: "X XP until The Next Rank" ─────────────────────
        if (xpProgress.xpUntilNextRank > 0) {
            // Find the next rank's title for the label
            val nextRankTitle = ALL_RANKS.find { it.level == xpProgress.currentRank.level + 1 }?.title
                ?: "next rank"

            Text(
                text = "${xpProgress.xpUntilNextRank} XP until $nextRankTitle",
                color = ColorTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 3 — Mission Card
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MissionCard — the primary interactive card showing today's mission.
 *
 * Displays the mission in "military briefing" format: STATUS badge, TITLE,
 * OBJECTIVE block, RULES list, DIFFICULTY dots, XP reward, and action buttons.
 *
 * The card has three visual states driven by mission.status:
 *   ACTIVE    → shows ACCEPT THE MISSION + (if location-dependent) SWAP MISSION buttons.
 *               If showReflectionSlider is true (user tapped ACCEPT), shows the slider below.
 *   COMPLETED → shows VIEW COMPLETION button.
 *   Other     → no action buttons shown.
 *
 * v3: VIEW COMPLETION now calls onViewCompletion() instead of onCardClicked().
 *
 * v5: SWAP MISSION button no longer calls onSwapMission() directly.
 *     Instead, it sets showSwapBlockedDialog = true, which triggers SwapBlockedDialog —
 *     a motivational popup telling the user that skipping is forbidden.
 *     onSwapMission is kept as a parameter for potential future use (e.g., swap-after-penalty).
 *
 * v6: Added onMissionDone and onMissionAbandoned for the IN_PROGRESS → return flow.
 *     Added MissionStatus.IN_PROGRESS branch in the when() block.
 *     Moved showReflectionSlider from ACTIVE branch to IN_PROGRESS branch where it belongs.
 *
 * @param mission                  The mission data to display.
 * @param showReflectionSlider     True when the discomfort slider should be shown (after "I DID IT").
 * @param currentDiscomfortRating  The live slider value (1–10).
 * @param onCardClicked            Called when the user taps the card body (future navigation).
 * @param onAcceptMission          Called when the user taps "ACCEPT THE MISSION".
 * @param onSwapMission            Kept for future use. NOT called by the SWAP MISSION button in v5.
 * @param onMissionDone            v6: Called when the user taps "I DID IT" (shows discomfort slider).
 * @param onMissionAbandoned       v6: Called when the user taps "I COULDN'T DO IT" (resets to ACTIVE).
 * @param onDiscomfortRatingChanged Called as the slider moves.
 * @param onSubmitReflection       Called when the user taps "SUBMIT RATING".
 * @param onViewCompletion         v3: Called when the user taps "VIEW COMPLETION".
 */
@Composable
private fun MissionCard(
    mission: Mission,
    showReflectionSlider: Boolean,
    currentDiscomfortRating: Int,
    onCardClicked: () -> Unit,
    onAcceptMission: () -> Unit,
    onSwapMission: () -> Unit,
    onMissionDone: () -> Unit,               // v6: "I DID IT" — shows discomfort slider
    onMissionAbandoned: () -> Unit,          // v6: "I COULDN'T DO IT" — resets to ACTIVE
    onDiscomfortRatingChanged: (Int) -> Unit,
    onSubmitReflection: () -> Unit,
    onViewCompletion: () -> Unit              // v3: new parameter
) {
    // ── v5: LOCAL DIALOG STATE ────────────────────────────────────────────
    // Controls whether the "swap is forbidden" motivational dialog is visible.
    // Starts as false (dialog hidden). Set to true when user taps SWAP MISSION.
    // Set back to false when the user dismisses the dialog.
    var showSwapBlockedDialog by remember { mutableStateOf(false) }

    // ── v5: SHOW THE MOTIVATIONAL DIALOG IF STATE IS TRUE ────────────────
    // SwapBlockedDialog is placed here (above the card Box) so it can appear
    // as an overlay over the entire screen — not clipped inside the card.
    if (showSwapBlockedDialog) {
        SwapBlockedDialog(
            onDismiss = { showSwapBlockedDialog = false }  // Tapping OK or outside hides the dialog
        )
    }

    // The card is a dark-grey rounded Box that holds all the card content in a Column.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))   // Rounded corners on the card
            .background(ColorSurface)          // Dark grey card background
            .clickable(onClick = onCardClicked) // Whole card is tappable (future: navigate to detail)
            .padding(20.dp)                    // Inner padding on all sides
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── CARD HEADER: "TODAY'S MISSION" + STATUS BADGE ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S MISSION",
                    color = ColorTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )

                // Status badge — gold box with status text
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (mission.status) {
                                MissionStatus.COMPLETED -> Color(0xFF2A2A1A)  // Slightly warm dark
                                else -> Color(0xFF2A200A)                     // Dark gold-tinted
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = mission.status.name,
                        color = ColorAccentGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── MISSION TITLE ─────────────────────────────────────────────────
            Text(
                text = mission.title,
                color = ColorTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── MILITARY BRIEFING SECTION: OBJECTIVE + RULES ──────────────────
            MilitaryBriefingSection(
                objective = mission.objective,
                rules = mission.rules
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── CARD DIVIDER ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorDivider)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── CARD FOOTER: DIFFICULTY + XP REWARD ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DifficultyIndicator(difficulty = mission.difficulty)

                Text(
                    text = "+${mission.xpReward} XP",
                    color = ColorAccentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── ACTION BUTTONS — conditional on mission status ────────────────
            when (mission.status) {

                MissionStatus.ACTIVE -> {
                    // "ACCEPT THE MISSION" — primary gold button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ColorAccentGold)
                            .clickable(onClick = onAcceptMission)
                            .padding(vertical = 14.dp)
                    ) {
                        Text(
                            text = "ACCEPT THE MISSION",
                            color = Color(0xFF0D0D0D),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    // ── SWAP MISSION BUTTON (location-dependent missions only) ────
                    // v5: Tapping this no longer calls onSwapMission().
                    //     Instead, it shows SwapBlockedDialog — a motivational popup
                    //     telling the user that skipping is forbidden.
                    //     The core philosophy of this app is: face the discomfort, don't avoid it.
                    if (mission.isLocationDependent) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ColorSurfaceVariant)
                                // v5: Set showSwapBlockedDialog = true instead of calling onSwapMission()
                                .clickable { showSwapBlockedDialog = true }
                                .padding(vertical = 14.dp)
                        ) {
                            Text(
                                text = "SWAP MISSION",
                                color = ColorTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                }

                // ── IN_PROGRESS: user accepted and went to do the mission ────────
                // v6: This branch was missing — the app showed "IN_PROGRESS" badge
                // but no buttons, leaving the user stuck. Now shows two buttons:
                //
                //   "I DID IT"           → gold primary button → onMissionDone()
                //     ViewModel sets showReflectionSlider = true, slider appears below.
                //
                //   "I COULDN'T DO IT"   → grey secondary button → onMissionAbandoned()
                //     ViewModel resets status to ACTIVE, user can try again today.
                //
                // If showReflectionSlider is already true (user tapped "I DID IT"),
                // the two buttons are replaced by the discomfort slider so the user
                // can rate how hard the mission was before submitting.
                MissionStatus.IN_PROGRESS -> {
                    if (!showReflectionSlider) {
                        // ── "I DID IT" — primary gold button ─────────────────────
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ColorAccentGold)
                                .clickable(onClick = onMissionDone)
                                .padding(vertical = 14.dp)
                        ) {
                            Text(
                                text = "I DID IT",
                                color = Color(0xFF0D0D0D),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ── "I COULDN'T DO IT" — grey secondary button ───────────
                        // Tapping this resets the mission to ACTIVE so the user can
                        // try again today. No XP is awarded. Not punitive — just
                        // gives the user another chance if the moment wasn't right.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ColorSurfaceVariant)
                                .clickable(onClick = onMissionAbandoned)
                                .padding(vertical = 14.dp)
                        ) {
                            Text(
                                text = "I COULDN'T DO IT",
                                color = ColorTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    } else {
                        // ── DISCOMFORT SLIDER — shown after "I DID IT" is tapped ──
                        // The user tapped "I DID IT" (ViewModel set showReflectionSlider = true).
                        // Now they rate how uncomfortable the mission felt on a 1–10 scale.
                        // Tapping SUBMIT RATING calls onSubmitReflection() which awards XP,
                        // saves the history entry, and transitions to COMPLETED status.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ColorDivider)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        DiscomfortSliderSection(
                            currentRating = currentDiscomfortRating,
                            onRatingChanged = onDiscomfortRatingChanged,
                            onSubmit = onSubmitReflection
                        )
                    }
                }

                MissionStatus.COMPLETED -> {
                    // "VIEW COMPLETION" — shown after the discomfort rating is submitted.
                    // v3: calls onViewCompletion() instead of onCardClicked().
                    // Tapping this transitions to DailyCompleteScreen.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ColorSurfaceVariant)
                            .clickable(onClick = onViewCompletion)  // v3: was onCardClicked
                            .padding(vertical = 14.dp)
                    ) {
                        Text(
                            text = "VIEW COMPLETION",
                            color = ColorAccentGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                else -> {
                    // FAILED / LOCKED — no action button, mission is locked or expired
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// v5 NEW COMPOSABLE — Swap Blocked Dialog (motivational popup)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * SwapBlockedDialog — a motivational AlertDialog shown when the user taps "SWAP MISSION".
 *
 * NEW in v5. The core philosophy of this app is that discomfort must be confronted,
 * not escaped. Rather than letting the user silently swap to an easier mission,
 * we intercept the attempt and remind them why they are here.
 *
 * Design decisions:
 *   - The title is short and declarative — it immediately sets the tone.
 *   - The body message is motivational, not punishing. It acknowledges that the
 *     mission is hard, then pushes the user forward anyway.
 *   - There is only ONE button ("I WILL FACE IT") — there is no "skip anyway" escape hatch.
 *     Pressing it dismisses the dialog and returns the user to their original mission.
 *   - The dialog uses Material 3 AlertDialog so it inherits the dark theme
 *     automatically from MaterialTheme (set in MainActivity).
 *
 * Future ideas:
 *   - Randomise the body message from a pool of motivational quotes so it feels
 *     fresh even if the user taps SWAP MISSION repeatedly.
 *   - Add a "Why is this important?" expandable section with a short paragraph on
 *     the science of voluntary discomfort exposure.
 *   - After 3 taps in a session, unlock a genuine swap (with a streak-risk warning).
 *
 * @param onDismiss  Called when the user taps "I WILL FACE IT" or taps outside the dialog.
 *                   The caller (MissionCard) uses this to set showSwapBlockedDialog = false.
 */
@Composable
private fun SwapBlockedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        // onDismissRequest fires when the user taps outside the dialog area.
        // We treat that the same as tapping "I WILL FACE IT" — dialog closes.
        onDismissRequest = onDismiss,

        // ── DIALOG TITLE ─────────────────────────────────────────────────────
        // Short, declarative, stoic. No question marks — this is a statement.
        title = {
            Text(
                text = "Skipping is forbidden.",
                color = ColorTextPrimary,            // Off-white — primary text colour
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },

        // ── DIALOG BODY MESSAGE ───────────────────────────────────────────────
        // Motivational — acknowledges difficulty, then redirects the user forward.
        // This is the heart of the app's philosophy: you face it, even when it's hard.
        text = {
            Text(
                text = "Sometimes it will be hard.\nSometimes it will feel impossible.\n\nThat is exactly why you must face it.\n\nThe discomfort you feel right now is the mission working. Stay.",
                color = ColorTextSecondary,          // Muted grey — body text, not headline
                fontSize = 14.sp,
                lineHeight = 22.sp                   // Comfortable line height for multi-line text
            )
        },

        // ── CONFIRM BUTTON ────────────────────────────────────────────────────
        // Only one button — no escape hatch. "I WILL FACE IT" is the only option.
        // This is intentional: the user committed to this challenge. Hold them to it.
        confirmButton = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorAccentGold)     // Gold button — same as "ACCEPT THE MISSION"
                    .clickable(onClick = onDismiss)   // Dismiss the dialog and return to the mission
                    .padding(vertical = 14.dp)
            ) {
                Text(
                    text = "I WILL FACE IT",
                    color = Color(0xFF0D0D0D),        // Near-black text on gold — high contrast
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        },

        // ── DIALOG CONTAINER COLOUR ───────────────────────────────────────────
        // Set the dialog background to the dark surface colour so it matches the app's aesthetic.
        // Without this, AlertDialog would use Material 3's default light-ish container colour.
        containerColor = ColorSurface
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DAILY COMPLETE SCREEN
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DailyCompleteScreen — shown after the user taps "VIEW COMPLETION".
 *
 * NEW in v3. Replaces the main DashboardContent for the rest of the day.
 *
 * Displays:
 *   - The updated XP progress bar (so the user sees their new total after earning XP).
 *   - A "Come back tomorrow" message.
 *   - A subtle confirmation that today's mission is done.
 *
 * Why show the XP bar here?
 *   After submitting a rating, the user's XP has just gone up. Showing the updated bar
 *   here gives them a satisfying moment of seeing their progress before the app goes quiet.
 *
 * v7: Added onAdminTriggered parameter. The secret 7-tap gesture is attached to
 *     the "MISSION COMPLETE" label at the top so admin is reachable even after
 *     today's mission is done (when the badge is not visible on screen).
 *
 * @param xpProgress       The updated XP data (after mission XP was added). Null if somehow
 *                         reached without data (defensive — shouldn't happen in practice).
 * @param onAdminTriggered v7: Called when the secret 7-tap gesture completes.
 *                         Fires from tapping "MISSION COMPLETE" text 7× within 3 seconds.
 */
@Composable
private fun DailyCompleteScreen(
    xpProgress: XpProgress?,
    onAdminTriggered: () -> Unit          // v7: secret admin tap target on this screen
) {
    // ── v7: SECRET TAP STATE ──────────────────────────────────────────────────
    // Same logic as RankBadgeSection: 7 taps within 3 seconds fires onAdminTriggered().
    // Attached to the "MISSION COMPLETE" gold label — natural tap target, invisible trigger.
    var tapCount     by remember { mutableStateOf(0) }
    var firstTapTime by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 80.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── COMPLETION HEADER ─────────────────────────────────────────────
        // v7: This Text is the secret admin tap target on this screen.
        // Tapping it 7 times within 3 seconds opens the admin panel.
        // No visual feedback is shown — completely invisible to regular users.
        Text(
            text = "MISSION COMPLETE",
            color = ColorAccentGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
            modifier = Modifier.clickable {
                val now = System.currentTimeMillis()
                if (tapCount == 0 || now - firstTapTime > 3_000L) {
                    tapCount     = 1
                    firstTapTime = now
                } else {
                    tapCount++
                    if (tapCount >= 7) {
                        tapCount     = 0
                        firstTapTime = 0L
                        onAdminTriggered()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Come back\ntomorrow.",
            color = ColorTextPrimary,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 44.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You've done your part for today.",
            color = ColorTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ── UPDATED XP BAR ────────────────────────────────────────────────
        // Show the XP progress bar so the user can see their updated total.
        // If xpProgress is null (should not happen), show nothing.
        if (xpProgress != null) {
            XpProgressSection(xpProgress = xpProgress)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── CLOSING MESSAGE ───────────────────────────────────────────────
        Text(
            text = "Discomfort endured today\nbuilds the strength you need tomorrow.",
            color = ColorTextSecondary.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER COMPOSABLE — Military Briefing Section
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MilitaryBriefingSection — renders the mission brief in OBJECTIVE + RULES format.
 *
 * Shown inside the MissionCard for both ACTIVE and COMPLETED missions.
 * Uses small-caps labels and numbered rules to reinforce the stoic, military tone.
 *
 * @param objective  Single-sentence statement of what the user must do.
 * @param rules      Ordered list of specific constraints for the mission.
 */
@Composable
private fun MilitaryBriefingSection(
    objective: String,
    rules: List<String>
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // ── OBJECTIVE BLOCK ───────────────────────────────────────────────
        Text(
            text = "OBJECTIVE",
            color = ColorTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = objective,
            color = ColorTextPrimary,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── RULES BLOCK ───────────────────────────────────────────────────
        Text(
            text = "RULES",
            color = ColorTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Numbered list of rules — each on its own row
        rules.forEachIndexed { index, rule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),  // Gap between rules
                verticalAlignment = Alignment.Top
            ) {
                // Rule number — gold accent to make the list scannable
                Text(
                    text = "${index + 1}.",
                    color = ColorAccentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(24.dp)  // Fixed width so rule text aligns
                )

                // Rule text — off-white, same size as objective body text
                Text(
                    text = rule,
                    color = ColorTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)  // Takes remaining width after the number
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER COMPOSABLE — Discomfort Slider Section
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DiscomfortSliderSection — the post-acceptance 1–10 rating slider.
 *
 * Shown inside MissionCard after the user taps "ACCEPT THE MISSION".
 * The user drags the slider to rate how uncomfortable the experience was,
 * then taps "SUBMIT RATING" to record the result and mark the mission complete.
 *
 * Tapping SUBMIT RATING:
 *   → ViewModel.onSubmitReflection()
 *   → XP is added, rank is recalculated, mission is COMPLETED
 *   → XP bar animates to new value
 *   → Slider hides
 *   → "VIEW COMPLETION" button appears
 *
 * @param currentRating     The live slider position (1–10). Updated by the ViewModel.
 * @param onRatingChanged   Called with the new Int value whenever the slider moves.
 * @param onSubmit          Called when the user taps "SUBMIT RATING".
 */
@Composable
private fun DiscomfortSliderSection(
    currentRating: Int,
    onRatingChanged: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // ── SECTION HEADER ────────────────────────────────────────────────
        Text(
            text = "HOW UNCOMFORTABLE WAS THAT?",
            color = ColorTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── CURRENT RATING DISPLAY ────────────────────────────────────────
        // Large centred number — updates in real time as the slider moves.
        Text(
            text = "$currentRating / 10",
            color = ColorAccentGold,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── SLIDER + RANGE LABELS ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "1", color = ColorTextSecondary, fontSize = 12.sp)

            // Material 3 Slider.
            // valueRange: 1f..10f so the thumb snaps between 1 and 10.
            // steps = 8 creates 10 discrete positions (8 intermediate + 2 endpoints).
            // roundToInt() rather than toInt() avoids off-by-one from floating-point imprecision.
            Slider(
                value = currentRating.toFloat(),
                onValueChange = { newValue ->
                    onRatingChanged(newValue.roundToInt().coerceIn(1, 10))
                },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = ColorAccentGold,
                    activeTrackColor = ColorAccentGold,
                    inactiveTrackColor = ColorSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )

            Text(text = "10", color = ColorTextSecondary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── SUBMIT RATING BUTTON ──────────────────────────────────────────
        // Tapping this calls onSubmit() → ViewModel.onSubmitReflection()
        // → XP added → bar animates → mission COMPLETED → slider hides → VIEW COMPLETION shown.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ColorAccentGold)
                .clickable(onClick = onSubmit)
                .padding(vertical = 14.dp)
        ) {
            Text(
                text = "SUBMIT RATING",
                color = Color(0xFF0D0D0D),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER COMPOSABLE — Difficulty Indicator
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DifficultyIndicator — renders 5 small dots representing difficulty level.
 *
 * Filled dots (white) = active difficulty level.
 * Dark dots = remaining difficulty tiers.
 *
 * Example: difficulty=2 → filled filled empty empty empty
 *
 * @param difficulty  A number from 1 to 5 indicating the mission's difficulty.
 */
@Composable
private fun DifficultyIndicator(difficulty: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "DIFFICULTY",
            color = ColorTextSecondary,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(end = 4.dp)
        )

        repeat(5) { index ->
            val isFilled = index + 1 <= difficulty
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) ColorTextPrimary
                        else ColorSurfaceVariant
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOADING STATE COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * LoadingScreen — shown while DashboardViewModel is loading data.
 *
 * Displays a centred circular progress spinner + a "Loading..." label.
 */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = ColorAccentGold,
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Loading...",
                color = ColorTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ERROR STATE COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ErrorScreen — shown if DashboardViewModel encountered an error loading data.
 *
 * Displays the error message and a RETRY button that calls ViewModel.onRefresh().
 *
 * @param message  The human-readable error message to display.
 * @param onRetry  Called when the user taps the Retry button.
 */
@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "!",
                color = Color(0xFFD32F2F),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = message,
                color = ColorTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorSurface)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "RETRY",
                    color = ColorTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
