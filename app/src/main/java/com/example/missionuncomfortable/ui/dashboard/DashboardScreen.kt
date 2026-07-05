/**
 * DashboardScreen.kt
 *
 * This is the PRIMARY screen of Mission: Uncomfortable.
 * It is the first screen the user sees after logging in.
 *
 * ─── WHAT THIS FILE CONTAINS ────────────────────────────────────────────────
 *
 *   DashboardScreen()            — The root composable. Connects to the ViewModel, observes state,
 *                                  and decides what to show (loading, error, or actual content).
 *
 *   DashboardContent()           — The main layout once data is loaded. Holds all three sections:
 *                                    1. RankBadgeSection   — rank badge + rank name
 *                                    2. XpProgressSection  — XP progress bar + labels
 *                                    3. MissionCard        — Today's active mission card
 *
 *   RankBadgeSection()           — Composable for the visual rank badge at the top of the screen.
 *                                  v2: Now uses Image composable (R.drawable.rank_badge_placeholder)
 *                                  instead of a plain text circle.
 *
 *   XpProgressSection()          — Composable for the XP progress bar and labels.
 *
 *   MissionCard()                — Composable for the large interactive mission card.
 *                                  v2: Shows mission in military briefing format (OBJECTIVE / RULES),
 *                                  adds a "Swap Mission" button for location-dependent tasks,
 *                                  and shows the DiscomfortSlider after the user accepts.
 *
 *   MilitaryBriefingSection()    — NEW (v2). Renders the mission brief in a structured
 *                                  OBJECTIVE + RULES format instead of a blocky paragraph.
 *
 *   DiscomfortSliderSection()    — NEW (v2). A 1–10 slider that replaces typed text reflections.
 *                                  Shown after the user accepts the mission so they can rate
 *                                  how uncomfortable the experience was before submitting.
 *
 *   DifficultyIndicator()        — Composable for the row of difficulty dots on the mission card.
 *   LoadingScreen()              — Composable shown while data is loading.
 *   ErrorScreen()                — Composable shown if something went wrong loading data.
 *
 * ─── DESIGN AESTHETIC ────────────────────────────────────────────────────────
 *
 *   Dark, minimal, stoic.
 *   Background: near-black (#0D0D0D)
 *   Surface/Card:  dark grey (#1A1A1A)
 *   Accent:        off-white (#E0E0E0) for primary text
 *   XP bar accent: a muted gold (#C8A84B) — ties into the "rank/achievement" theme
 *   Difficulty dots: active = white, inactive = dark grey
 *
 * ─── HOW TO USE ──────────────────────────────────────────────────────────────
 *
 *   In your NavGraph or Activity, navigate to DashboardScreen like this:
 *
 *       composable("dashboard") {
 *           DashboardScreen(
 *               onMissionClicked = { navController.navigate("mission_detail") }
 *           )
 *       }
 *
 * ─── FUTURE WORK ──────────────────────────────────────────────────────────────
 *
 *   - Replace placeholder data in DashboardViewModel with Room DAO calls
 *   - Add swipe-to-refresh (PullRefreshIndicator) once real networking is in place
 *   - Add real rank badge illustrations to res/drawable and update RankBadgeSection:
 *       replace R.drawable.rank_badge_placeholder with the rank-specific drawable.
 *   - Animate the XP bar filling up when the screen first loads (AnimatedContent)
 *   - Implement onSwapMission() in the ViewModel with real Supabase alternative-mission fetch
 *   - Enforce a daily swap limit (one swap per day) in the ViewModel
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v2 — Applied four UI/UX updates:
 *          1. RankBadgeSection: replaced text-circle placeholder with Image composable.
 *             Uses R.drawable.rank_badge_placeholder. Add this drawable to your
 *             res/drawable folder — any 120×120dp image works as a stand-in.
 *          2. MissionCard: replaced blocky paragraph description with MilitaryBriefingSection
 *             (OBJECTIVE block + numbered RULES list).
 *          3. MissionCard: added "SWAP MISSION" secondary button below "ACCEPT THE MISSION"
 *             for missions where Mission.isLocationDependent == true.
 *          4. MissionCard: added DiscomfortSliderSection (1–10 slider) that appears after
 *             the user accepts the mission. Replaces the old typed-reflection text field.
 *             No emojis used anywhere in this file.
 */

package com.example.missionuncomfortable.ui.dashboard

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
// Jetpack Compose UI building blocks
import androidx.compose.animation.core.animateFloatAsState        // Smoothly animates a float value
import androidx.compose.animation.core.tween                       // Animation timing spec
import androidx.compose.foundation.Image                           // v2: Image composable for rank badge
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
import androidx.compose.ui.layout.ContentScale                     // v2: Controls how Image content is scaled inside bounds
import androidx.compose.ui.res.painterResource                     // v2: Loads a drawable resource for use in Image()
import androidx.compose.ui.text.font.FontWeight                    // Bold, normal, etc.
import androidx.compose.ui.text.style.TextAlign                    // Centre, start, end alignment for text
import androidx.compose.ui.unit.dp                                 // Density-independent pixels for spacing/sizing
import androidx.compose.ui.unit.sp                                 // Scale-independent pixels for font sizes
import androidx.compose.runtime.livedata.observeAsState            // Bridges LiveData → Compose State (requires lifecycle-livedata-compose dependency)
import androidx.lifecycle.viewmodel.compose.viewModel              // Gets or creates a ViewModel scoped to this composable
import com.example.missionuncomfortable.R                          // v2: App resource references (R.drawable.rank_badge_placeholder)
import kotlin.math.roundToInt                                      // v2: Accurate Float-to-Int rounding for the discomfort slider

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
 * @param viewModel        The ViewModel that manages this screen's state.
 *                         Provided automatically by `viewModel()` — you don't pass one manually.
 * @param onMissionClicked Called when the user taps the mission card area (not the action buttons).
 *                         Use this to navigate to the Mission Detail screen.
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

            // ── CASE 3: Data is ready — show the full dashboard ───────────────
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
                        onSubmitReflection = { viewModel.onSubmitReflection() }
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
 * v2: Now accepts new slider-related parameters and passes them down to MissionCard.
 *
 * @param xpProgress               The XP and rank progress data to display.
 * @param todaysMission            The mission assigned to the user today.
 * @param showReflectionSlider     True when the discomfort slider should be shown in the card.
 * @param currentDiscomfortRating  The live slider position (1–10) before submission.
 * @param onMissionClicked         Called when the user taps the card area (for navigation).
 * @param onAcceptMission          Called when the user taps "ACCEPT THE MISSION".
 * @param onSwapMission            Called when the user taps "SWAP MISSION".
 * @param onDiscomfortRatingChanged Called continuously as the user drags the slider.
 * @param onSubmitReflection       Called when the user taps "SUBMIT RATING".
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
    onSubmitReflection: () -> Unit
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
            .padding(top = 48.dp,              // Extra top padding for status bar clearance
                bottom = 32.dp),          // Extra bottom padding so content doesn't get clipped
        horizontalAlignment = Alignment.CenterHorizontally  // Centre children horizontally
    ) {

        // ── SECTION 1: Rank Badge ─────────────────────────────────────────────
        // Displays a large badge image + the rank name below it.
        RankBadgeSection(rank = xpProgress.currentRank)

        Spacer(modifier = Modifier.height(28.dp))  // Vertical gap between badge and XP bar

        // ── SECTION 2: XP Progress Bar ───────────────────────────────────────
        // Shows how close the user is to ranking up.
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
            onSubmitReflection = onSubmitReflection
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 1 — Rank Badge
// ─────────────────────────────────────────────────────────────────────────────

/**
 * RankBadgeSection — displays the user's current rank badge and title.
 *
 * v2 CHANGE: The old text-circle placeholder (a Box containing the rank level as a number)
 * has been replaced with an Image composable that loads from R.drawable.rank_badge_placeholder.
 *
 * SETUP REQUIRED:
 *   Before this will compile, add a drawable file called "rank_badge_placeholder" to your
 *   res/drawable folder. Any PNG or vector will work — even a solid circle with the same
 *   gold colour (#C8A84B) as a temporary stand-in. Then swap in rank-specific drawables
 *   (badge_observer, badge_initiate, etc.) when your badge art is ready.
 *
 *   To show different badges per rank, replace `R.drawable.rank_badge_placeholder` with
 *   `rank.badgeResId ?: R.drawable.rank_badge_placeholder` — this uses the rank-specific
 *   asset if it exists, or falls back to the placeholder if it doesn't.
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
        // v2: Replaced the Box+Text placeholder with a proper Image composable.
        //
        // ContentScale.Fit: scales the image to fill the 120×120dp frame while maintaining
        // its aspect ratio. This prevents stretching or cropping.
        //
        // clip(CircleShape): masks the image to a circle, matching the old placeholder shape.
        //
        // TODO (when rank-specific art is ready):
        //   Change the painter argument to:
        //     painterResource(id = rank.badgeResId ?: R.drawable.rank_badge_placeholder)
        //   This will automatically show the correct badge for each rank level.
        Image(
            painter = painterResource(id = R.drawable.rank_badge_placeholder),
            contentDescription = "Rank badge for ${rank.title}",   // Accessibility label
            contentScale = ContentScale.Fit,                         // Scale to fit within bounds
            modifier = Modifier
                .size(120.dp)                // 120dp diameter — same size as the old circle placeholder
                .clip(CircleShape)           // Clip the image to a circle
        )

        Spacer(modifier = Modifier.height(16.dp))  // Gap between badge and rank title text

        // ── RANK LABEL ────────────────────────────────────────────────────
        // "RANK" in small caps above the rank title — acts as a label/category
        Text(
            text = "RANK",                          // Static label text
            color = ColorTextSecondary,             // Muted grey — this is supporting info
            fontSize = 11.sp,                       // Tiny — just a label
            fontWeight = FontWeight.SemiBold,       // Slightly bold so it reads cleanly at small size
            letterSpacing = 3.sp                   // Wide letter spacing for a sleek, "stamp" feel
        )

        Spacer(modifier = Modifier.height(4.dp))   // Tiny gap between label and title

        // ── RANK TITLE ────────────────────────────────────────────────────
        // The rank's name, e.g. "The Initiate", in large white text
        Text(
            text = rank.title,                      // e.g. "The Initiate"
            color = ColorTextPrimary,              // Off-white — this is the most important text here
            fontSize = 24.sp,                      // Prominent size — this is the hero text
            fontWeight = FontWeight.Bold           // Bold for authority/presence
        )

        Spacer(modifier = Modifier.height(6.dp))   // Small gap before description

        // ── RANK DESCRIPTION ──────────────────────────────────────────────
        // Short flavour text describing the rank — less prominent than the title
        Text(
            text = rank.description,               // e.g. "You've taken your first uncomfortable step."
            color = ColorTextSecondary,            // Muted grey — supporting flavour, not critical
            fontSize = 13.sp,                     // Small but readable
            textAlign = TextAlign.Center,         // Centre-aligned to match the column alignment
            modifier = Modifier.padding(horizontal = 24.dp)  // Extra horizontal padding so long text wraps nicely
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
 * @param xpProgress  The XpProgress object with currentXp, progressFraction, etc.
 */
@Composable
private fun XpProgressSection(xpProgress: XpProgress) {

    // ── ANIMATE THE PROGRESS FRACTION ─────────────────────────────────────
    // `animateFloatAsState` smoothly animates the bar filling up when xpProgress changes.
    // tween(durationMillis = 1000) means the animation takes 1 second.
    // On first composition, the bar will animate from 0 to the real value — a satisfying effect.
    val animatedProgress by animateFloatAsState(
        targetValue = xpProgress.progressFraction,       // The real progress value from our model
        animationSpec = tween(durationMillis = 1000),    // 1-second animation
        label = "xpProgressAnimation"                   // Label for debugging/tooling
    )

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── LABEL ROW ─────────────────────────────────────────────────────
        // "TOTAL XP" on the left, "next rank at 500 XP" on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,  // Push labels to opposite ends
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left label: "TOTAL XP" — the section header
            Text(
                text = "TOTAL XP",
                color = ColorTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp                           // Wide spacing for sleek look
            )

            // Right label: "next rank at X XP" — tells the user their target
            Text(
                text = "next rank at ${xpProgress.xpForNextRank} XP",
                color = ColorTextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))  // Gap between labels and current XP number

        // ── CURRENT XP ────────────────────────────────────────────────────
        // Large bold number showing the user's total earned XP
        Text(
            text = "${xpProgress.currentXp} XP",    // e.g. "350 XP"
            color = ColorTextPrimary,               // Off-white — this is important info
            fontSize = 28.sp,                       // Large and prominent
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))  // Gap between XP number and progress bar

        // ── PROGRESS BAR ──────────────────────────────────────────────────
        // A custom progress bar — Material's LinearProgressIndicator doesn't give enough design control,
        // so we build ours from a Box (background track) with an inner Box (fill).
        Box(
            modifier = Modifier
                .fillMaxWidth()                                // Bar spans the full screen width
                .height(8.dp)                                  // 8dp tall — substantial but not chunky
                .clip(RoundedCornerShape(4.dp))               // Rounded ends on the track
                .background(ColorSurface)                     // Dark grey track (unfilled portion)
        ) {
            // The filled portion of the bar — its width is controlled by animatedProgress (0f to 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()                           // Full height of the track
                    .fillMaxWidth(animatedProgress)            // Only fill X% of the width
                    .clip(RoundedCornerShape(4.dp))           // Rounded ends on the fill too
                    .background(ColorAccentGold)              // Muted gold fill — feels like an achievement
            )
        }

        Spacer(modifier = Modifier.height(8.dp))  // Gap between bar and "X XP until next rank" text

        // ── XP UNTIL NEXT RANK ────────────────────────────────────────────
        // Shows how many more XP the user needs to rank up
        Text(
            text = "${xpProgress.xpUntilNextRank} XP until ${
                // Look up the NEXT rank's title from ALL_RANKS — e.g. "The Challenger"
                // We find the rank with level = currentRank.level + 1
                // If the user is at max rank, fall back to "Max Rank"
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
 * MissionCard — a large, interactive card showing today's active mission.
 *
 * The card is divided into:
 *   - A header bar: "TODAY'S MISSION" label + status badge (ACTIVE / DONE)
 *   - The mission title (large, bold)
 *   - MilitaryBriefingSection: OBJECTIVE block + numbered RULES list (v2 — replaces paragraph)
 *   - A footer: difficulty dots on the left, XP reward on the right
 *   - "ACCEPT THE MISSION" action button
 *   - "SWAP MISSION" secondary button (v2 — only shown when mission.isLocationDependent == true)
 *   - DiscomfortSliderSection (v2 — shown after the user accepts the mission)
 *
 * v2 CHANGES:
 *   - `description` paragraph replaced with MilitaryBriefingSection (OBJECTIVE + RULES format).
 *   - "SWAP MISSION" button added below the accept button (conditional).
 *   - DiscomfortSliderSection added at the bottom of the card (conditional on showReflectionSlider).
 *   - Card no longer calls onClick for the whole card area, to avoid accidental navigation
 *     while the user interacts with the slider. The card area click is preserved via onCardClicked
 *     but the individual buttons have their own handlers.
 *
 * @param mission                 The Mission object to display.
 * @param showReflectionSlider    True = show the discomfort slider below the action buttons.
 * @param currentDiscomfortRating The live slider position (1–10).
 * @param onCardClicked           Called when the user taps the card area (for navigation).
 * @param onAcceptMission         Called when the user taps "ACCEPT THE MISSION".
 * @param onSwapMission           Called when the user taps "SWAP MISSION".
 * @param onDiscomfortRatingChanged Called as the slider moves.
 * @param onSubmitReflection      Called when the user taps "SUBMIT RATING".
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
    onSubmitReflection: () -> Unit
) {
    // The card is a Box so we can control its shape and background precisely.
    // Using a Box rather than Card gives us full control over the dark theme.
    //
    // v2 NOTE — Conditional clickable:
    //   When showReflectionSlider is true, the user is actively interacting with the slider
    //   inside the card. In that state we suppress the outer card-level tap-to-navigate behaviour
    //   so accidental taps on the card body don't trigger navigation mid-rating.
    //   When the slider is hidden (normal browsing mode), the card remains fully tappable.
    Box(
        modifier = Modifier
            .fillMaxWidth()                                     // Card spans full screen width
            .clip(RoundedCornerShape(16.dp))                   // Rounded corners — sleek, not sharp
            .background(ColorSurface)                          // Dark grey card background
            .then(
                // Only attach the card-level click handler when the slider is NOT visible.
                // This prevents accidental navigation while the user is interacting with the slider.
                if (!showReflectionSlider) {
                    Modifier.clickable(onClick = onCardClicked)
                } else {
                    Modifier  // No-op modifier — card is not tappable during slider interaction
                }
            )
    ) {
        // Main content column inside the card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)  // Comfortable padding inside the card on all sides
        ) {

            // ── CARD HEADER ───────────────────────────────────────────────
            // "TODAY'S MISSION" label on the left, status badge on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,  // Push items to opposite ends
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "TODAY'S MISSION" label — acts as the card category header
                Text(
                    text = "TODAY'S MISSION",
                    color = ColorTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp                           // Wide spacing for sleek label look
                )

                // Status badge — shows "ACTIVE" in gold or "COMPLETED" in green
                // The background colour changes based on mission status
                val statusColor = when (mission.status) {
                    MissionStatus.ACTIVE -> ColorAccentGold           // Gold = active/in progress
                    MissionStatus.COMPLETED -> Color(0xFF4CAF50)     // Green = done
                    MissionStatus.FAILED -> Color(0xFFD32F2F)        // Red = failed
                    MissionStatus.LOCKED -> ColorTextSecondary       // Grey = locked
                }

                // The status badge is a small pill-shaped Box with coloured text inside
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))             // Slightly rounded pill
                        .background(statusColor.copy(alpha = 0.15f)) // Very faint background tint
                        .padding(horizontal = 8.dp, vertical = 4.dp) // Compact padding
                ) {
                    Text(
                        text = mission.status.name,                  // e.g. "ACTIVE", "COMPLETED"
                        color = statusColor,                         // Matches the background tint
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))  // Gap between header and mission title

            // ── MISSION TITLE ─────────────────────────────────────────────
            // Large, prominent mission name — this is the first thing the eye should land on
            Text(
                text = mission.title,                               // e.g. "The Silent Stranger"
                color = ColorTextPrimary,                          // Off-white — max contrast
                fontSize = 22.sp,                                  // Large — this is the hero text on the card
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp                                 // Comfortable line height for multi-line titles
            )

            Spacer(modifier = Modifier.height(16.dp))  // Gap between title and briefing section

            // ── MISSION BRIEFING (MILITARY FORMAT) ───────────────────────
            // v2: Replaced the flat paragraph `description` text with a structured
            // MilitaryBriefingSection that shows an OBJECTIVE block + numbered RULES list.
            // This makes mission instructions clearer and more scannable.
            MilitaryBriefingSection(
                objective = mission.objective,
                rules = mission.rules
            )

            Spacer(modifier = Modifier.height(20.dp))  // Gap between briefing and divider

            // ── SUBTLE DIVIDER ────────────────────────────────────────────
            // A hairline horizontal line separating the briefing from the card footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)                                  // 1dp = hairline thickness
                    .background(ColorDivider)                      // Very subtle dark line
            )

            Spacer(modifier = Modifier.height(16.dp))  // Gap between divider and footer

            // ── CARD FOOTER ───────────────────────────────────────────────
            // Difficulty dots on the left, XP reward on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: difficulty indicator (5 dots, filled up to the difficulty level)
                DifficultyIndicator(difficulty = mission.difficulty)

                // Right: XP reward — how much XP the user earns for completing this
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "+" prefix in gold to suggest gain
                    Text(
                        text = "+",
                        color = ColorAccentGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${mission.xpReward} XP",           // e.g. "+75 XP"
                        color = ColorAccentGold,                   // Gold to match the XP bar colour
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))  // Gap between footer and action buttons

            // ── PRIMARY ACTION BUTTON ─────────────────────────────────────
            // "ACCEPT THE MISSION" for ACTIVE, "VIEW COMPLETION" for COMPLETED, etc.
            // v2: ACTIVE state now calls onAcceptMission() instead of onCardClicked()
            // so the ViewModel can independently track acceptance + reveal the slider.
            val buttonText = when (mission.status) {
                MissionStatus.ACTIVE -> "ACCEPT THE MISSION"        // CTA for an active mission
                MissionStatus.COMPLETED -> "VIEW COMPLETION"        // View proof/reflection
                MissionStatus.FAILED -> "MISSION FAILED"           // Non-interactive
                MissionStatus.LOCKED -> "UNLOCK AT NEXT RANK"      // Non-interactive
            }

            // Whether the primary button should respond to taps
            val isPrimaryButtonEnabled = mission.status == MissionStatus.ACTIVE ||
                    mission.status == MissionStatus.COMPLETED

            // The primary action button — gold fill when enabled
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
                            // v2: For an ACTIVE mission, tap "ACCEPT THE MISSION" triggers
                            // onAcceptMission() (which shows the slider) rather than navigating away.
                            // For COMPLETED, it navigates via onCardClicked.
                            if (mission.status == MissionStatus.ACTIVE) {
                                onAcceptMission()
                            } else {
                                onCardClicked()
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
            // v2: NEW BUTTON. Only shown when Mission.isLocationDependent == true.
            //
            // Some missions require the user to travel to a specific type of place.
            // If the user cannot reach that location (e.g., stuck at home, unwell, no transport),
            // this button lets them swap to an alternative mission that does NOT require travel.
            //
            // Design rationale for secondary styling (outline instead of fill):
            //   The swap is a fallback — we don't want to incentivise using it.
            //   The subtle outline treatment makes it available but not prominent.
            //   It sits BELOW the primary action so the eye lands on "Accept" first.
            if (mission.isLocationDependent && mission.status == MissionStatus.ACTIVE) {

                Spacer(modifier = Modifier.height(10.dp))  // Small gap between primary and secondary buttons

                // Secondary button — outlined style (no fill, border stroke)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ColorSurfaceVariant)       // Slightly lighter than the card — subtle but distinct
                        .clickable(onClick = onSwapMission)    // Calls ViewModel.onSwapMission()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "SWAP MISSION",                 // No emojis — clean, direct label
                        color = ColorTextSecondary,            // Muted text — de-emphasised vs. the gold button
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // ── DISCOMFORT SLIDER SECTION ─────────────────────────────────
            // v2: NEW SECTION. Shown after the user taps "ACCEPT THE MISSION".
            // Replaces the old typed-reflection text field.
            //
            // The slider lets the user rate how uncomfortable the experience was on a 1–10 scale.
            // Tapping "SUBMIT RATING" records the value, marks the mission COMPLETED, and hides the slider.
            //
            // showReflectionSlider is controlled by DashboardUiState (set to true in onAcceptMission()).
            if (showReflectionSlider) {

                Spacer(modifier = Modifier.height(20.dp))  // Gap between buttons and slider section

                // A hairline divider to visually separate the action buttons from the slider section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorDivider)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // The slider composable — handles all internal layout and labels
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
// MILITARY BRIEFING SECTION — structured OBJECTIVE + RULES format
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MilitaryBriefingSection — renders the mission instructions in a structured format.
 *
 * v2 NEW COMPOSABLE. Replaces the old flat paragraph mission description.
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
 * Design decisions:
 *   - "OBJECTIVE" and "RULES" are rendered as small-caps labels (like the "RANK" and "TOTAL XP"
 *     labels elsewhere in the design) — wide letter spacing, muted grey, semibold.
 *   - The objective text uses primary text colour (off-white) for clarity.
 *   - Rules are numbered and displayed in secondary text colour (muted grey), slightly smaller.
 *   - No bullet points — numbered rules feel more like a field brief and are easier to track.
 *   - No emojis anywhere in this composable.
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

        // "OBJECTIVE" label — styled like other section labels in the design
        Text(
            text = "OBJECTIVE",
            color = ColorTextSecondary,            // Muted grey — this is a label, not content
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp                  // Wide spacing matches the label aesthetic elsewhere
        )

        Spacer(modifier = Modifier.height(6.dp))  // Tight gap between label and content

        // The objective text — single sentence, prominent colour
        Text(
            text = objective,
            color = ColorTextPrimary,             // Off-white — the objective is the most important content
            fontSize = 14.sp,
            lineHeight = 21.sp                    // 1.5x line height for comfortable reading
        )

        Spacer(modifier = Modifier.height(16.dp)) // Gap between OBJECTIVE block and RULES block

        // ── RULES BLOCK ───────────────────────────────────────────────────

        // "RULES" label — same style as "OBJECTIVE" label above
        Text(
            text = "RULES",
            color = ColorTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))  // Gap between label and first rule

        // Render each rule as a numbered line.
        // `forEachIndexed` gives us both the index (for numbering) and the string (the rule text).
        rules.forEachIndexed { index, rule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),          // 6dp gap below each rule row
                verticalAlignment = Alignment.Top     // Align number and text at the top for multi-line rules
            ) {
                // Rule number — right-aligned in a fixed-width column so multi-digit numbers don't shift the text
                Text(
                    text = "${index + 1}.",             // "1.", "2.", "3.", etc.
                    color = ColorAccentGold,            // Gold numbering ties into the achievement theme
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(24.dp)   // Fixed width keeps all rule texts left-aligned
                )

                // Rule text — slightly smaller than the objective, muted colour
                Text(
                    text = rule,
                    color = ColorTextSecondary,        // Muted — rules are supporting content
                    fontSize = 13.sp,
                    lineHeight = 20.sp                 // Comfortable line height for multi-line rules
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
 * v2 NEW COMPOSABLE. Replaces typed text reflections.
 *
 * Shown inside MissionCard after the user taps "ACCEPT THE MISSION".
 * The user drags the slider to rate how uncomfortable the experience was,
 * then taps "SUBMIT RATING" to record the result and mark the mission complete.
 *
 * Design decisions:
 *   - The slider uses Material 3's `Slider` composable with a gold thumb and track.
 *   - The current rating is displayed as a large number above the slider so the user
 *     can clearly see their selected value while dragging.
 *   - Range labels "1" and "10" are shown at either end of the slider for orientation.
 *   - "SUBMIT RATING" is styled like the primary action button (gold fill, dark text).
 *   - No emojis used anywhere.
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
        // Brief prompt — tells the user what the slider is for
        Text(
            text = "HOW UNCOMFORTABLE WAS THAT?",
            color = ColorTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp                  // Wide spacing — matches other label style
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── CURRENT RATING DISPLAY ────────────────────────────────────────
        // Large, centred number showing the user's current slider position.
        // Updates in real time as the slider thumb moves.
        Text(
            text = "$currentRating / 10",           // e.g. "7 / 10"
            color = ColorAccentGold,               // Gold — ties into the achievement/XP colour system
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── SLIDER + RANGE LABELS ROW ─────────────────────────────────────
        // The "1" and "10" labels flank the slider so the user knows the scale.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left range label — "1" = least uncomfortable
            Text(
                text = "1",
                color = ColorTextSecondary,
                fontSize = 12.sp
            )

            // Material 3 Slider — the core interactive element.
            //
            // valueRange: 1f..10f so the thumb snaps between 1 and 10.
            // steps: 8 creates 10 discrete positions (steps+2 total positions for a range of size 10).
            //   steps = (max - min - 1) = 10 - 1 - 1 = 8.
            // value: must be a Float for Slider, so we cast currentRating to Float.
            // onValueChange: converts the incoming Float back to Int before calling onRatingChanged.
            //
            // Colors.sliderColors() customises the thumb and active track to gold.
            // All inactive/inactive parts use a darker variant of the surface colour.
            Slider(
                value = currentRating.toFloat(),              // Slider works in Float internally
                onValueChange = { newValue ->
                    // roundToInt() rather than toInt() (truncation) avoids off-by-one errors
                    // caused by floating-point imprecision near integer boundaries.
                    // e.g. a thumb snapped to "7" might internally be 6.9999... — truncation
                    // would give 6, but rounding correctly gives 7. Then clamp to [1,10] for safety.
                    onRatingChanged(newValue.roundToInt().coerceIn(1, 10))
                },
                valueRange = 1f..10f,                        // The valid range of the slider
                steps = 8,                                   // 8 intermediate steps → 10 discrete positions
                colors = SliderDefaults.colors(
                    thumbColor = ColorAccentGold,             // Gold thumb — matches the XP bar accent
                    activeTrackColor = ColorAccentGold,       // Gold filled track (left of thumb)
                    inactiveTrackColor = ColorSurfaceVariant  // Dark grey empty track (right of thumb)
                ),
                modifier = Modifier.weight(1f)               // Slider takes all remaining horizontal space
            )

            // Right range label — "10" = most uncomfortable
            Text(
                text = "10",
                color = ColorTextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))  // Gap between slider and submit button

        // ── SUBMIT RATING BUTTON ──────────────────────────────────────────
        // Styled identically to "ACCEPT THE MISSION" (gold fill, dark text, same corner radius).
        // Tapping this calls onSubmit() → ViewModel.onSubmitReflection() → marks mission COMPLETE.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ColorAccentGold)           // Gold fill — this is a primary action
                .clickable(onClick = onSubmit)         // Calls ViewModel.onSubmitReflection()
                .padding(vertical = 14.dp)
        ) {
            Text(
                text = "SUBMIT RATING",                // Direct, action-oriented label
                color = Color(0xFF0D0D0D),             // Near-black text on gold — high contrast
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
 * Hollow/dark dots = remaining difficulty tiers.
 *
 * Example: difficulty=2 → filled filled empty empty empty
 *
 * @param difficulty  A number from 1 to 5 indicating the mission's difficulty.
 */
@Composable
private fun DifficultyIndicator(difficulty: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)  // 5dp gap between each dot
    ) {
        // Difficulty label
        Text(
            text = "DIFFICULTY",
            color = ColorTextSecondary,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(end = 4.dp)         // Small gap between label and dots
        )

        // Draw exactly 5 dots — one per difficulty tier
        repeat(5) { index ->
            // `index` goes from 0 to 4.
            // A dot is "filled" (active) if its index + 1 is less than or equal to the difficulty level.
            // e.g., difficulty=2 → indices 0 and 1 are filled, 2–4 are empty.
            val isFilled = index + 1 <= difficulty

            Box(
                modifier = Modifier
                    .size(7.dp)                                        // 7dp dot size — subtle but visible
                    .clip(CircleShape)                                 // Perfect circle
                    .background(
                        if (isFilled) ColorTextPrimary               // White dot = active difficulty tier
                        else ColorSurfaceVariant                     // Dark dot = unused tier
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
 * Simple and unobtrusive — the user should see this for only a brief moment.
 */
@Composable
private fun LoadingScreen() {
    // Centre everything both horizontally and vertically in the screen
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Material 3 circular spinner — styled to the gold accent colour
            CircularProgressIndicator(
                color = ColorAccentGold,              // Gold spinner matches the XP bar accent
                modifier = Modifier.size(48.dp),      // 48dp is a comfortable spinner size
                strokeWidth = 3.dp                   // Slightly thin stroke for a refined look
            )

            Spacer(modifier = Modifier.height(16.dp))  // Gap between spinner and text

            // Loading label — unobtrusive, muted colour
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
 * Displays:
 *   - The error message from the ViewModel
 *   - A "RETRY" button that calls the ViewModel's onRefresh()
 *
 * @param message  The human-readable error message to display.
 * @param onRetry  Called when the user taps the Retry button.
 */
@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    // Centre everything both horizontally and vertically in the screen
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)  // 16dp gap between all children
        ) {
            // Error indicator — a plain "!" text marker
            // Replace with an actual Icon composable from Material Icons Extended if desired
            Text(
                text = "!",
                color = Color(0xFFD32F2F),    // Red — signals an error state
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )

            // Error message from the ViewModel
            Text(
                text = message,
                color = ColorTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center  // Centre the text so it wraps nicely
            )

            // Retry button — gives the user a way to recover without force-quitting the app
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorSurface)
                    .clickable(onClick = onRetry)             // Calls ViewModel.onRefresh()
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
