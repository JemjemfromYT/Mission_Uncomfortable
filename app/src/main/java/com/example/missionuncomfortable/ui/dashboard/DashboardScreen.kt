/**
 * DashboardScreen.kt
 *
 * This is the PRIMARY screen of Mission: Uncomfortable.
 * It is the first screen the user sees after logging in.
 *
 * ─── WHAT THIS FILE CONTAINS ────────────────────────────────────────────────
 *
 *   DashboardScreen()        — The root composable. Connects to the ViewModel, observes state,
 *                              and decides what to show (loading, error, or actual content).
 *
 *   DashboardContent()       — The main layout once data is loaded. Holds all three sections:
 *                                1. RankBadgeSection   — rank badge + rank name
 *                                2. XpProgressSection  — XP progress bar + labels
 *                                3. MissionCard        — Today's active mission card
 *
 *   RankBadgeSection()       — Composable for the visual rank badge at the top of the screen.
 *   XpProgressSection()      — Composable for the XP progress bar and labels.
 *   MissionCard()            — Composable for the large interactive mission card.
 *   DifficultyIndicator()    — Composable for the row of difficulty dots on the mission card.
 *   LoadingScreen()          — Composable shown while data is loading.
 *   ErrorScreen()            — Composable shown if something went wrong loading data.
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
 *   - Add rank badge illustrations to res/drawable and wire up RankBadgeSection
 *   - Animate the XP bar filling up when the screen first loads (AnimatedContent)
 */

package com.example.missionuncomfortable.ui.dashboard

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
// Jetpack Compose UI building blocks
import androidx.compose.animation.core.animateFloatAsState        // Smoothly animates a float value
import androidx.compose.animation.core.tween                       // Animation timing spec
import androidx.compose.foundation.background                      // Sets the background colour of a composable
import androidx.compose.foundation.clickable                       // Makes a composable respond to taps
import androidx.compose.foundation.layout.*                        // Box, Column, Row, Spacer, padding, fillMaxSize, etc.
import androidx.compose.foundation.rememberScrollState             // Remembers scroll position of a scrollable column
import androidx.compose.foundation.shape.CircleShape               // Circular shape for the rank badge placeholder
import androidx.compose.foundation.shape.RoundedCornerShape        // Rounded corners for cards and progress bar
import androidx.compose.foundation.verticalScroll                  // Makes a Column scrollable vertically
import androidx.compose.material3.*                                // Material 3 components: Text, CircularProgressIndicator, etc.
import androidx.compose.runtime.*                                  // remember, LaunchedEffect, getValue, etc.
import androidx.compose.ui.Alignment                               // Alignment constants (center, start, end, etc.)
import androidx.compose.ui.Modifier                                // Modifier — the "how does it look and behave" chain
import androidx.compose.ui.draw.clip                               // Clips a composable to a specific shape
import androidx.compose.ui.graphics.Color                          // Colour class — use hex values like Color(0xFF...)
import androidx.compose.ui.text.font.FontWeight                    // Bold, normal, etc.
import androidx.compose.ui.text.style.TextAlign                    // Centre, start, end alignment for text
import androidx.compose.ui.unit.dp                                 // Density-independent pixels for spacing/sizing
import androidx.compose.ui.unit.sp                                 // Scale-independent pixels for font sizes
import androidx.compose.runtime.livedata.observeAsState            // Bridges LiveData → Compose State (requires lifecycle-livedata-compose dependency)
import androidx.lifecycle.viewmodel.compose.viewModel              // Gets or creates a ViewModel scoped to this composable

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
 * @param onMissionClicked Called when the user taps the mission card.
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
                        xpProgress = uiState.xpProgress!!,      // XP + rank data
                        todaysMission = uiState.todaysMission!!, // Today's mission
                        onMissionClicked = {
                            viewModel.onMissionCardClicked()     // Notify ViewModel user tapped the card
                            onMissionClicked()                   // Trigger navigation from the caller
                        }
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
 * @param xpProgress       The XP and rank progress data to display.
 * @param todaysMission    The mission assigned to the user today.
 * @param onMissionClicked Called when the user taps the mission card.
 */
@Composable
private fun DashboardContent(
    xpProgress: XpProgress,           // Contains currentXp, rank, progressFraction, etc.
    todaysMission: Mission,           // Contains title, description, xpReward, status, etc.
    onMissionClicked: () -> Unit      // Tap handler for the mission card
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
        // Displays a large circular badge image + the rank name below it.
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
            onClick = onMissionClicked
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 1 — Rank Badge
// ─────────────────────────────────────────────────────────────────────────────

/**
 * RankBadgeSection — displays the user's current rank badge and title.
 *
 * For now it shows a placeholder circle. When you have real badge illustrations,
 * replace the Box placeholder with an Image(painter = painterResource(...)) composable.
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

        // ── RANK BADGE PLACEHOLDER ─────────────────────────────────────────
        // This is a circular dark-grey placeholder where the actual badge illustration will go.
        // Replace this Box with your Image composable when badge assets are ready.
        Box(
            contentAlignment = Alignment.Center,    // Centre the "?" text inside the circle
            modifier = Modifier
                .size(120.dp)                       // 120dp diameter circle
                .clip(CircleShape)                  // Clip to a perfect circle
                .background(ColorSurface)           // Dark grey fill
        ) {
            // Placeholder text — the rank's numeric level in a large font
            // Replace this with: Image(painter = painterResource(id = rank.badgeResId ?: R.drawable.badge_placeholder))
            Text(
                text = rank.level.toString(),       // Shows "2" for Level 2, etc.
                color = ColorAccentGold,            // Gold colour to suggest the achievement feel
                fontSize = 48.sp,                  // Very large font — it's the hero visual
                fontWeight = FontWeight.Bold        // Bold so it has visual weight
            )
        }

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
 *   - The mission description (readable paragraph text)
 *   - A footer: difficulty dots on the left, XP reward on the right
 *   - A full-width action button: "ACCEPT THE MISSION" (ACTIVE) or "VIEW COMPLETED" (COMPLETED)
 *
 * @param mission  The Mission object to display.
 * @param onClick  Called when the user taps anywhere on the card or the action button.
 */
@Composable
private fun MissionCard(
    mission: Mission,
    onClick: () -> Unit
) {
    // The card is a Box so we can control its shape and background precisely.
    // Using a Box rather than Card gives us full control over the dark theme.
    Box(
        modifier = Modifier
            .fillMaxWidth()                                     // Card spans full screen width
            .clip(RoundedCornerShape(16.dp))                   // Rounded corners — sleek, not sharp
            .background(ColorSurface)                          // Dark grey card background
            .clickable(onClick = onClick)                      // The whole card is tappable
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
                    MissionStatus.COMPLETED -> Color(0xFF4CAF50)     // Green = done!
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

            Spacer(modifier = Modifier.height(12.dp))  // Gap between title and description

            // ── MISSION DESCRIPTION ───────────────────────────────────────
            // The full mission instructions — readable paragraph style
            Text(
                text = mission.description,                        // The full mission text
                color = ColorTextSecondary,                       // Muted — supporting content
                fontSize = 14.sp,                                 // Standard readable body text size
                lineHeight = 21.sp                                // 1.5x line height for readability
            )

            Spacer(modifier = Modifier.height(20.dp))  // Gap between description and footer

            // ── SUBTLE DIVIDER ────────────────────────────────────────────
            // A hairline horizontal line separating the description from the card footer
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
                    // Small "+" prefix in gold to suggest gain
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

            Spacer(modifier = Modifier.height(20.dp))  // Gap between footer and action button

            // ── ACTION BUTTON ─────────────────────────────────────────────
            // Full-width button — text and style changes based on mission status
            val buttonText = when (mission.status) {
                MissionStatus.ACTIVE -> "ACCEPT THE MISSION"        // CTA for an active mission
                MissionStatus.COMPLETED -> "VIEW COMPLETION"        // View proof/reflection
                MissionStatus.FAILED -> "MISSION FAILED"           // Non-interactive (no navigation yet)
                MissionStatus.LOCKED -> "UNLOCK AT NEXT RANK"      // Non-interactive
            }

            // Whether the button should respond to taps
            val isButtonEnabled = mission.status == MissionStatus.ACTIVE ||
                    mission.status == MissionStatus.COMPLETED

            // The button itself — a gold-outlined full-width pill
            Box(
                contentAlignment = Alignment.Center,              // Centre the button text
                modifier = Modifier
                    .fillMaxWidth()                               // Full width of the card
                    .clip(RoundedCornerShape(10.dp))             // Slightly rounded button corners
                    .background(
                        if (isButtonEnabled) ColorAccentGold     // Gold fill when enabled
                        else ColorSurface                        // Matches card when disabled (invisible)
                    )
                    .clickable(
                        enabled = isButtonEnabled,               // Only clickable if ACTIVE or COMPLETED
                        onClick = onClick                        // Triggers navigation on tap
                    )
                    .padding(vertical = 14.dp)                   // Comfortable tap target height
            ) {
                Text(
                    text = buttonText,
                    // Text is dark (near-black) on the gold button so it's readable
                    // Text is muted on disabled state
                    color = if (isButtonEnabled) Color(0xFF0D0D0D) else ColorTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp                       // Wide spacing for a strong, confident feel
                )
            }
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
 * Example: difficulty=2 → ●●○○○
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
            // A dot is "filled" (active) if its index + 1 is ≤ the difficulty level.
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
            // Error icon — using a simple "!" text placeholder
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
