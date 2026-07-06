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
 *                                  v4: Now reads rank.badgeResId to display rank-specific badge art.
 *                                  Each of the 5 ranks has its own unique drawable.
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
 * v4: Now uses rank.badgeResId to display rank-specific badge art.
 * Each of the 5 ranks (Observer → Sovereign) has its own unique vector drawable.
 * The ?: fallback to rank_badge_placeholder is retained as a safety net in case
 * a rank is added to ALL_RANKS without a badge being created yet.
 *
 * @param rank  The user's current Rank object (level, title, description, badgeResId).
 */
@Composable
private fun RankBadgeSection(rank: Rank) {
    // Centre everything in this section horizontally
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()     // Take the full width so centring works correctly
    ) {

        // ── RANK BADGE IMAGE ───────────────────────────────────────────────
        // v4: reads rank.badgeResId — each rank now has its own distinct drawable.
        //   Level 1 Observer   → badge_observer  (open eye)
        //   Level 2 Initiate   → badge_initiate  (single chevron)
        //   Level 3 Challenger → badge_challenger (shield + sword)
        //   Level 4 Conqueror  → badge_conqueror  (eagle wings)
        //   Level 5 Sovereign  → badge_sovereign  (military crown)
        //
        // checkNotNull() crashes loudly in testing if a future rank is added to ALL_RANKS
        // without a badgeResId — better than silently showing a wrong image.
        // ContentScale.Fit: scales the image to fill the 120×120dp frame while maintaining
        // its aspect ratio. clip(CircleShape): masks the image to a circle.
        Image(
            painter = painterResource(id = checkNotNull(rank.badgeResId) { "Rank '${rank.title}' has no badgeResId set" }),
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
            modifier = Modifier.padding(horizontal = 16.dp)  // Extra horizontal padding for narrower text
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
 * @param mission                  The mission data to display.
 * @param showReflectionSlider     True when the discomfort slider should be shown.
 * @param currentDiscomfortRating  The live slider value (1–10).
 * @param onCardClicked            Called when the user taps the card body (future navigation).
 * @param onAcceptMission          Called when the user taps "ACCEPT THE MISSION".
 * @param onSwapMission            Called when the user taps "SWAP MISSION".
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
    onDiscomfortRatingChanged: (Int) -> Unit,
    onSubmitReflection: () -> Unit,
    onViewCompletion: () -> Unit              // v3: new parameter
) {
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

                    // "SWAP MISSION" — only shown for location-dependent missions
                    if (mission.isLocationDependent) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ColorSurfaceVariant)
                                .clickable(onClick = onSwapMission)
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

                    // ── DISCOMFORT SLIDER ─────────────────────────────────────
                    // Shown after the user taps ACCEPT THE MISSION.
                    // Hidden by default (showReflectionSlider starts false).
                    if (showReflectionSlider) {
                        Spacer(modifier = Modifier.height(18.dp))

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
 * @param xpProgress  The updated XP data (after mission XP was added). Null if somehow
 *                    reached without data (defensive — shouldn't happen in practice).
 */
@Composable
private fun DailyCompleteScreen(xpProgress: XpProgress?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 80.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── COMPLETION HEADER ─────────────────────────────────────────────
        Text(
            text = "MISSION COMPLETE",
            color = ColorAccentGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp
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
