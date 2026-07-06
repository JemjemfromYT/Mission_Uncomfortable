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
 *                                  Uses Image composable (R.drawable.rank_badge_placeholder).
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
 *   In MainActivity (or a NavHost), show this composable after the user has
 *   completed the WelcomeScreen onboarding:
 *
 *       if (showDashboard) {
 *           DashboardScreen()
 *       }
 *
 * ─── FUTURE WORK ──────────────────────────────────────────────────────────────
 *
 *   - Replace placeholder data in DashboardViewModel with Room DAO calls.
 *   - Add swipe-to-refresh (PullRefreshIndicator) once real networking is in place.
 *   - Add real rank badge illustrations to res/drawable and update RankBadgeSection:
 *       replace R.drawable.rank_badge_placeholder with rank.badgeResId ?: placeholder.
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
 */

package com.example.missionuncomfortable.ui.dashboard

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
// Jetpack Compose UI building blocks
import androidx.compose.animation.core.animateFloatAsState        // Smoothly animates a float value
import androidx.compose.animation.core.tween                       // Animation timing spec
import androidx.compose.foundation.Image                           // Image composable for rank badge
import androidx.compose.foundation.background                      // Sets the background colour of a composable
import androidx.compose.foundation.clickable                       // Makes a composable respond to taps
import androidx.compose.foundation.layout.*                        // Box, Column, Row, Spacer, padding, fillMaxSize, etc.
import androidx.compose.foundation.rememberScrollState             // Remembers scroll position of a scrollable column
import androidx.compose.foundation.shape.CircleShape               // Circular shape for difficulty dots
import androidx.compose.foundation.shape.RoundedCornerShape        // Rounded corners for cards and progress bar
import androidx.compose.foundation.verticalScroll                  // Makes a Column scrollable vertically
import androidx.compose.material3.*                                // Material 3 components: Text, Slider, CircularProgressIndicator, etc.
import androidx.compose.runtime.*                                  // remember, LaunchedEffect, getValue, etc.
import androidx.compose.ui.Alignment                               // Alignment constants (center, start, end, etc.)
import androidx.compose.ui.Modifier                                // Modifier — the "how does it look and behave" chain
import androidx.compose.ui.draw.clip                               // Clips a composable to a specific shape
import androidx.compose.ui.graphics.Color                          // Colour class — use hex values like Color(0xFF...)
import androidx.compose.ui.layout.ContentScale                     // Controls how Image content is scaled inside bounds
import androidx.compose.ui.res.painterResource                     // Loads a drawable resource for use in Image()
import androidx.compose.ui.text.font.FontWeight                    // Bold, normal, etc.
import androidx.compose.ui.text.style.TextAlign                    // Centre, start, end alignment for text
import androidx.compose.ui.unit.dp                                 // Density-independent pixels for spacing/sizing
import androidx.compose.ui.unit.sp                                 // Scale-independent pixels for font sizes
import androidx.compose.runtime.livedata.observeAsState            // Bridges LiveData → Compose State
import androidx.lifecycle.viewmodel.compose.viewModel              // Gets or creates a ViewModel scoped to this composable
import com.example.missionuncomfortable.R                          // App resource references (R.drawable.rank_badge_placeholder)
import kotlin.math.roundToInt                                      // Accurate Float-to-Int rounding for the discomfort slider

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
 * @param viewModel        The ViewModel that manages this screen's state.
 *                         Provided automatically by `viewModel()` — you don't pass one manually.
 * @param onMissionClicked Called when the user taps the mission card area (not the action buttons).
 *                         Use this to navigate to the Mission Detail screen in the future.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),   // Auto-creates or retrieves existing ViewModel
    onMissionClicked: () -> Unit = {}             // Default to no-op so Previews don't crash
) {
    // Observe the ViewModel's LiveData as a Compose State.
    // The `?: DashboardUiState()` provides a safe default while the first value arrives.
    val uiState by viewModel.uiState.observeAsState(DashboardUiState())

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
            uiState.isDailyComplete -> {
                DailyCompleteScreen(
                    xpProgress = uiState.xpProgress  // May be null if somehow reached without data
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
                        onDiscomfortRatingChanged = { rating -> viewModel.onDiscomfortRatingChanged(rating) },
                        onSubmitReflection = { viewModel.onSubmitReflection() },
                        onViewCompletion = { viewModel.onViewCompletion() }  // v3: new callback
                    )
                }
            }
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
 *
 * @param xpProgress               The XP and rank progress data to display.
 * @param todaysMission            The mission assigned to the user today.
 * @param showReflectionSlider     True when the discomfort slider should be shown in the card.
 * @param currentDiscomfortRating  The live slider position (1–10) before submission.
 * @param onMissionClicked         Called when the user taps the card area (for future navigation).
 * @param onAcceptMission          Called when the user taps "ACCEPT THE MISSION".
 * @param onSwapMission            Called when the user taps "SWAP MISSION".
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
    onDiscomfortRatingChanged: (Int) -> Unit,
    onSubmitReflection: () -> Unit,
    onViewCompletion: () -> Unit              // v3: new parameter
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
        RankBadgeSection(rank = xpProgress.currentRank)

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
 * Uses an Image composable that loads from R.drawable.rank_badge_placeholder.
 *
 * SETUP REQUIRED:
 *   Before this will compile, add a drawable file called "rank_badge_placeholder" to your
 *   res/drawable folder. Any PNG or vector will work as a temporary stand-in.
 *   When real per-rank badge art is ready, change the painter to:
 *       painterResource(id = rank.badgeResId ?: R.drawable.rank_badge_placeholder)
 *
 * @param rank  The user's current Rank object (level, title, description).
 */
@Composable
private fun RankBadgeSection(rank: Rank) {
    // Centre everything in this section horizontally
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()     // Take the full width so centring works correctly
    ) {

        // ── RANK BADGE IMAGE ───────────────────────────────────────────────
        // ContentScale.Fit: scales the image to fill the 120×120dp frame while maintaining
        // its aspect ratio. This prevents stretching or cropping.
        // clip(CircleShape): masks the image to a circle, matching the old placeholder shape.
        //
        // TODO (when rank-specific art is ready):
        //   Change the painter argument to:
        //     painterResource(id = rank.badgeResId ?: R.drawable.rank_badge_placeholder)
        Image(
            painter = painterResource(id = R.drawable.rank_badge_placeholder),
            contentDescription = "Rank badge for ${rank.title}",   // Accessibility label
            contentScale = ContentScale.Fit,                         // Scale to fit within bounds
            modifier = Modifier
                .size(120.dp)                // 120dp diameter
                .clip(CircleShape)           // Clip the image to a circle
        )

        Spacer(modifier = Modifier.height(16.dp))  // Gap between badge and rank title text

        // ── RANK LABEL ────────────────────────────────────────────────────
        // "RANK" in small caps above the rank title — acts as a label/category
        Text(
            text = "RANK",
            color = ColorTextSecondary,             // Muted grey — this is supporting info
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp                   // Wide letter spacing for a sleek, "stamp" feel
        )

        Spacer(modifier = Modifier.height(4.dp))   // Tiny gap between label and title

        // ── RANK TITLE ────────────────────────────────────────────────────
        // The rank's name, e.g. "The Initiate", in large white text
        Text(
            text = rank.title,
            color = ColorTextPrimary,              // Off-white — this is the most important text here
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))   // Small gap before description

        // ── RANK DESCRIPTION ──────────────────────────────────────────────
        // Short flavour text describing the rank — less prominent than the title
        Text(
            text = rank.description,
            color = ColorTextSecondary,            // Muted grey — supporting flavour, not critical
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)  // Extra padding so long text wraps nicely
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 2 — XP Progress Bar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * XpProgressSection — displays the XP progress bar and surrounding labels.
 *
 * Includes:
 *   - A label row: "TOTAL XP" on the left, "next rank at X XP" on the right
 *   - The animated progress bar
 *   - A "X XP until next rank" line below the bar
 *
 * v3 NOTE — No code changes were needed here.
 *   The animateFloatAsState() call already reacts to any change in xpProgress.progressFraction.
 *   When DashboardViewModel.onSubmitReflection() increments XP and pushes a new XpProgress
 *   into state, this composable re-composes with the new progressFraction, and the bar
 *   smoothly animates from the old value to the new one over 1000ms.
 *
 * @param xpProgress  The XpProgress object with currentXp, progressFraction, etc.
 */
@Composable
private fun XpProgressSection(xpProgress: XpProgress) {

    // ── ANIMATE THE PROGRESS FRACTION ─────────────────────────────────────
    // `animateFloatAsState` smoothly animates the bar filling up when xpProgress changes.
    // tween(durationMillis = 1000) means the animation takes 1 second.
    // On first composition, the bar animates from 0 to the real value — satisfying.
    // On XP change (after Submit Rating), it animates from old fraction to new fraction.
    val animatedProgress by animateFloatAsState(
        targetValue = xpProgress.progressFraction,       // The real progress value from our model
        animationSpec = tween(durationMillis = 1000),    // 1-second smooth animation
        label = "xpProgressAnimation"                   // Label for debugging/tooling
    )

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── LABEL ROW ─────────────────────────────────────────────────────
        // "TOTAL XP" on the left, "next rank at 500 XP" on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOTAL XP",
                color = ColorTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )

            Text(
                text = "next rank at ${xpProgress.xpForNextRank} XP",
                color = ColorTextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── CURRENT XP ────────────────────────────────────────────────────
        // Large bold number showing the user's total earned XP.
        // v3: This updates immediately when the ViewModel pushes new xpProgress state.
        Text(
            text = "${xpProgress.currentXp} XP",
            color = ColorTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── PROGRESS BAR ──────────────────────────────────────────────────
        // A custom progress bar — Material's LinearProgressIndicator doesn't give enough
        // design control, so we build ours from a Box (background track) with an inner Box (fill).
        Box(
            modifier = Modifier
                .fillMaxWidth()                                // Bar spans the full screen width
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ColorSurface)                     // Dark grey track (unfilled portion)
        ) {
            // The filled portion — its width is controlled by animatedProgress (0f to 1f).
            // animatedProgress smoothly interpolates to the new fraction whenever xpProgress changes.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)            // Only fill X% of the width
                    .clip(RoundedCornerShape(4.dp))
                    .background(ColorAccentGold)              // Muted gold fill — achievement feel
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── XP UNTIL NEXT RANK ────────────────────────────────────────────
        Text(
            text = "${xpProgress.xpUntilNextRank} XP until ${
                // Look up the NEXT rank's title from ALL_RANKS — e.g. "The Challenger"
                ALL_RANKS.find { it.level == xpProgress.currentRank.level + 1 }?.title ?: "Max Rank"
            }",
            color = ColorTextSecondary,
            fontSize = 12.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 3 — Today's Mission Card
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MissionCard — a large, interactive card showing today's active or completed mission.
 *
 * The card is divided into:
 *   - A header bar: "TODAY'S MISSION" label + status badge (ACTIVE / COMPLETED)
 *   - The mission title (large, bold)
 *   - MilitaryBriefingSection: OBJECTIVE block + numbered RULES list
 *   - A footer: difficulty dots on the left, XP reward on the right
 *   - Primary action button (ACCEPT THE MISSION / VIEW COMPLETION / etc.)
 *   - "SWAP MISSION" secondary button (only shown when mission.isLocationDependent == true)
 *   - DiscomfortSliderSection (shown after the user accepts the mission)
 *
 * v3 CHANGES:
 *   - Added `onViewCompletion` parameter.
 *   - The VIEW COMPLETION button (COMPLETED status) now calls onViewCompletion() directly
 *     instead of the old generic onCardClicked(). This is the correct separation of concerns:
 *     "view completion" is a distinct user intent from "tap to navigate to detail".
 *
 * @param mission                 The Mission object to display.
 * @param showReflectionSlider    True = show the discomfort slider below the action buttons.
 * @param currentDiscomfortRating The live slider position (1–10).
 * @param onCardClicked           Called when the user taps the card area (for future navigation).
 * @param onAcceptMission         Called when the user taps "ACCEPT THE MISSION".
 * @param onSwapMission           Called when the user taps "SWAP MISSION".
 * @param onDiscomfortRatingChanged Called as the slider moves.
 * @param onSubmitReflection      Called when the user taps "SUBMIT RATING".
 * @param onViewCompletion        v3: Called when the user taps "VIEW COMPLETION" on a COMPLETED mission.
 */
@Composable
private fun MissionCard(
    mission: Mission,
    showReflectionSlider: Boolean,
    currentDiscomfortRating: Int,
    onCardClicked: () -> Unit,
    onAcceptMission: () -> Unit,
    onSwapMission: () -> Unit,
    onDiscomfortRatingChanged: (Int) -> Unit,
    onSubmitReflection: () -> Unit,
    onViewCompletion: () -> Unit              // v3: new parameter
) {
    // The card is a Box so we can control its shape and background precisely.
    //
    // Conditional clickable (v2):
    //   When showReflectionSlider is true, the user is actively interacting with the slider
    //   inside the card. In that state we suppress the outer card-level tap-to-navigate behaviour
    //   so accidental taps on the card body don't trigger navigation mid-rating.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorSurface)
            .then(
                if (!showReflectionSlider) {
                    Modifier.clickable(onClick = onCardClicked)
                } else {
                    Modifier  // No-op modifier — card is not tappable during slider interaction
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            // ── CARD HEADER ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S MISSION",
                    color = ColorTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )

                // Status badge — colour changes based on mission status
                val statusColor = when (mission.status) {
                    MissionStatus.ACTIVE    -> ColorAccentGold          // Gold = active/in progress
                    MissionStatus.COMPLETED -> Color(0xFF4CAF50)        // Green = done
                    MissionStatus.FAILED    -> Color(0xFFD32F2F)        // Red = failed
                    MissionStatus.LOCKED    -> ColorTextSecondary       // Grey = locked
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = mission.status.name,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── MISSION TITLE ─────────────────────────────────────────────
            Text(
                text = mission.title,
                color = ColorTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── MISSION BRIEFING (MILITARY FORMAT) ───────────────────────
            // Structured OBJECTIVE block + numbered RULES list.
            MilitaryBriefingSection(
                objective = mission.objective,
                rules = mission.rules
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── SUBTLE DIVIDER ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorDivider)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── CARD FOOTER ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DifficultyIndicator(difficulty = mission.difficulty)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+",
                        color = ColorAccentGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${mission.xpReward} XP",
                        color = ColorAccentGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── PRIMARY ACTION BUTTON ─────────────────────────────────────
            // Label and enabled state differ by mission status.
            val buttonText = when (mission.status) {
                MissionStatus.ACTIVE    -> "ACCEPT THE MISSION"
                MissionStatus.COMPLETED -> "VIEW COMPLETION"
                MissionStatus.FAILED    -> "MISSION FAILED"
                MissionStatus.LOCKED    -> "UNLOCK AT NEXT RANK"
            }

            val isPrimaryButtonEnabled = mission.status == MissionStatus.ACTIVE ||
                    mission.status == MissionStatus.COMPLETED

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isPrimaryButtonEnabled) ColorAccentGold
                        else ColorSurface
                    )
                    .clickable(
                        enabled = isPrimaryButtonEnabled,
                        onClick = {
                            when (mission.status) {
                                // ACTIVE: show the slider so the user can rate discomfort.
                                MissionStatus.ACTIVE -> onAcceptMission()

                                // COMPLETED: v3 — call onViewCompletion() to transition to
                                // the "Come back tomorrow" daily-complete screen.
                                // Previously this called onCardClicked() which did nothing.
                                MissionStatus.COMPLETED -> onViewCompletion()

                                // FAILED / LOCKED: button is disabled, this branch never fires.
                                else -> {}
                            }
                        }
                    )
                    .padding(vertical = 14.dp)
            ) {
                Text(
                    text = buttonText,
                    color = if (isPrimaryButtonEnabled) Color(0xFF0D0D0D) else ColorTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            // ── SWAP MISSION BUTTON (LOCATION-DEPENDENT MISSIONS ONLY) ────
            // Only shown when Mission.isLocationDependent == true and mission is still ACTIVE.
            // Lets the user swap to a non-location-dependent alternative if they can't travel.
            if (mission.isLocationDependent && mission.status == MissionStatus.ACTIVE) {

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ColorSurfaceVariant)
                        .clickable(onClick = onSwapMission)
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "SWAP MISSION",
                        color = ColorTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // ── DISCOMFORT SLIDER SECTION ─────────────────────────────────
            // Shown after the user taps "ACCEPT THE MISSION".
            // showReflectionSlider is set to true by ViewModel.onAcceptMission().
            if (showReflectionSlider) {

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorDivider)
                )

                Spacer(modifier = Modifier.height(20.dp))

                DiscomfortSliderSection(
                    currentRating = currentDiscomfortRating,
                    onRatingChanged = onDiscomfortRatingChanged,
                    onSubmit = onSubmitReflection
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DAILY COMPLETE SCREEN — shown after the user taps "VIEW COMPLETION"
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DailyCompleteScreen — the end-of-day state shown after the user views their completion.
 *
 * v3 NEW COMPOSABLE.
 *
 * Triggered by: ViewModel.onViewCompletion() → isDailyComplete = true → DashboardScreen routes here.
 *
 * What it shows:
 *   - The updated XP progress bar (so the user can see their new total and bar fill level).
 *   - A "DAILY MISSION COMPLETE" headline.
 *   - A "Come back tomorrow for your next mission." subline.
 *   - A subtle "Rest. You earned it." flavour line at the bottom.
 *
 * Why show XP here?
 *   The XP animation plays when onSubmitReflection() runs (on the mission card).
 *   By the time the user taps VIEW COMPLETION, the bar has already animated.
 *   Showing the bar here gives one final, satisfying look at their progress before
 *   the screen goes quiet for the day.
 *
 * Future:
 *   Replace with a full MissionCompleteScreen (separate NavController destination) that shows:
 *   streak count, discomfort history, weekly progress chart, and a next-mission preview.
 *
 * @param xpProgress  The user's current XP data (after the mission XP was added).
 *                    May be null if the screen is reached in an unexpected state — handled safely.
 */
@Composable
private fun DailyCompleteScreen(xpProgress: XpProgress?) {

    // Vertically scrollable so it works on all screen sizes
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── COMPLETION HEADLINE ───────────────────────────────────────────
        // "DAILY MISSION" in small caps — the label
        Text(
            text = "DAILY MISSION",
            color = ColorTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // "COMPLETE" in large gold — the hero word for this screen
        Text(
            text = "COMPLETE",
            color = ColorAccentGold,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Divider line under the headline — gives it a "stamp" quality
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
                .background(ColorAccentGold.copy(alpha = 0.5f))
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ── UPDATED XP SECTION ────────────────────────────────────────────
        // Show the XP bar so the user can see their final standing after the mission reward.
        // Only rendered if xpProgress is non-null (safety guard).
        if (xpProgress != null) {
            XpProgressSection(xpProgress = xpProgress)
            Spacer(modifier = Modifier.height(48.dp))
        }

        // ── COME BACK TOMORROW MESSAGE ────────────────────────────────────
        // Inside a Card so it has the same surface treatment as the mission card —
        // visual consistency with the rest of the Dashboard.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ColorSurface)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // "NEXT MISSION" label — same style as "TODAY'S MISSION" on the card header
                Text(
                    text = "NEXT MISSION",
                    color = ColorTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Main message — direct, stoic, no fluff
                Text(
                    text = "Come back tomorrow for your next mission.",
                    color = ColorTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary flavour line — quiet encouragement
                Text(
                    text = "Each day you show up, you change.",
                    color = ColorTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── STOIC SIGN-OFF ────────────────────────────────────────────────
        // Very faint — almost a watermark. The user should almost miss it.
        Text(
            text = "Rest. You earned it.",
            color = ColorTextSecondary.copy(alpha = 0.4f),
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MILITARY BRIEFING SECTION — structured OBJECTIVE + RULES format
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MilitaryBriefingSection — renders the mission instructions in a structured format.
 *
 * Layout:
 *   OBJECTIVE
 *   [objective text]
 *
 *   RULES
 *   1. [rule one]
 *   2. [rule two]
 *   ...
 *
 * @param objective  A single clear sentence describing the mission's primary goal.
 * @param rules      An ordered list of rules/constraints the user must follow.
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
            fontSize = 14.sp,
            lineHeight = 21.sp
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

        rules.forEachIndexed { index, rule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Rule number — gold, fixed width so text left-aligns neatly
                Text(
                    text = "${index + 1}.",
                    color = ColorAccentGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(24.dp)
                )

                // Rule text — muted, supporting content
                Text(
                    text = rule,
                    color = ColorTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DISCOMFORT SLIDER SECTION — post-mission 1–10 rating
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DiscomfortSliderSection — a 1–10 slider for rating post-mission discomfort.
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
