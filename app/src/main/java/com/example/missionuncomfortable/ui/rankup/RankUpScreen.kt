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
 *   DashboardScreen.kt     — Main UI. v5: SWAP MISSION button shows a popup, does NOT swap.
 *   HistoryModels.kt       — CompletedMissionEntry data class
 *   HistoryViewModel.kt    — Reads history from SharedPreferences JSON
 *   HistoryScreen.kt       — Scrollable mission history UI
 *   RankUpScreen.kt        — Full-screen rank-up celebration with spring animation (this file)
 *   StatsViewModel.kt      — Computes stats (streaks, category breakdown, avg discomfort)
 *   StatsScreen.kt         — Bar charts: category breakdown + discomfort by day of week
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
 *        v3 — Short description of what changed and why.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these rules, the developer will have to   ║
 * ║  manually fix every file you touch. Please respect these conventions.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * RankUpScreen.kt
 *
 * Full-screen rank-up celebration — shown when the user earns enough XP to advance.
 *
 * ─── WHAT THIS FILE CONTAINS ────────────────────────────────────────────────
 *
 *   RankUpScreen()  — Full-screen composable. Shown by DashboardScreen when
 *                     uiState.rankUpEvent is non-null (i.e. a rank-up just occurred).
 *                     Displays the new rank badge, title, and description with
 *                     staggered animations. Has a single CONTINUE button.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/rankup/RankUpScreen.kt
 *
 * ─── HOW IT IS TRIGGERED ─────────────────────────────────────────────────────
 *
 *   1. User submits discomfort rating → DashboardViewModel.onSubmitReflection()
 *   2. ViewModel compares old rank level vs new rank level.
 *   3. If newRank.level > oldRank.level → sets rankUpEvent = newRank in UiState.
 *   4. DashboardScreen sees rankUpEvent != null → renders RankUpScreen instead of dashboard.
 *   5. User taps CONTINUE → DashboardViewModel.onRankUpCelebrationComplete()
 *      → rankUpEvent = null, isDailyComplete = true → DailyCompleteScreen shown.
 *
 * ─── ANIMATION DESIGN ────────────────────────────────────────────────────────
 *
 *   This is a stoic app. The animation must feel EARNED, not celebratory.
 *   It should feel like a promotion ceremony, not a game reward screen.
 *
 *   Badge:              Scale 0 → 1 with a medium spring (some bounce — it lands with weight).
 *                       v2: The spring now applies to the full glow container, so badge + glow
 *                       scale in as one unit.
 *   "YOU ARE NOW" label: Fade in, 400ms delay.
 *   Rank title:          Fade in, 400ms delay (same as label).
 *   Description:         Fade in, 700ms delay.
 *   CONTINUE:            Slides up from 40dp below, 500ms delay.
 *
 * ─── DESIGN AESTHETIC ────────────────────────────────────────────────────────
 *
 *   Fullscreen near-black background — same as the rest of the app.
 *   Gold accent for the CONTINUE button — same as ACCEPT THE MISSION.
 *   No confetti. No sound. No fireworks.
 *   Just the badge, the title, and a single sentence that means something.
 *
 * ─── FUTURE WORK ─────────────────────────────────────────────────────────────
 *
 *   - [DONE v2] Add a gold pulse/glow behind the badge — implemented via
 *     Brush.radialGradient + drawBehind, with per-rank intensity.
 *   - Randomise the rank description between 2-3 variants to keep it fresh.
 *   - Add haptic feedback (VibrationEffect.createOneShot) when the badge lands.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial version. Introduced for Phase 7 (Rank-Up Celebration Screen).
 *
 *   v2 — RANK-SPECIFIC GLOW on the celebration screen.
 *          Badge is now rendered inside a 200dp container Box.
 *          The container draws a radial gold glow via Brush.radialGradient + drawBehind.
 *          graphicsLayer (spring scale) is applied to the container so the glow and
 *          badge scale in together as a single unit — they land with the same weight.
 *          Per-rank glow intensity:
 *            Initiate  (2): faint static halo
 *            Challenger(3): slow breathing pulse (2200ms half-cycle)
 *            Conqueror (4): strong pulsing glow (1600ms half-cycle)
 *            Sovereign (5): rapid full shimmer (1100ms half-cycle) — the apex
 *          New imports: drawBehind, Brush.
 *
 *   v3 — GLOW FIX (RING GRADIENT):
 *          Problem: the v2 gradient went gold-at-centre → transparent-at-edge, but the
 *          badge Image is drawn ON TOP of the glow and hides the centre, so only the
 *          already-faded outer rim of the gradient fell in the visible halo — the glow
 *          was nearly invisible, matching the same bug found on the dashboard badge.
 *          Fix: switched to Brush.radialGradient(colorStops = ...) — a RING gradient
 *          that stays transparent under the badge and peaks in gold just OUTSIDE the
 *          badge's edge (container 200dp / badge 160dp → badge edge at colorStop 0.80,
 *          ring peaks at 0.82), fading to transparent by the container edge.
 *          Alpha values raised substantially so the celebration glow reads clearly:
 *            Initiate  (2): 0.08 → 0.50 static halo.
 *            Challenger(3): 0.12-0.32 → 0.45-0.80 breathing pulse.
 *            Conqueror (4): 0.24-0.54 → 0.65-0.95 pulsing glow.
 *            Sovereign (5): 0.38-0.76 → 0.80-1.00 rapid shimmer.
 *
 *   v4 — PRESTIGE TIER SYSTEM (shared with DashboardScreen):
 *          Problem: even after the v3 ring fix, every rank still got the SAME visual
 *          treatment — one ring, just brighter/wider per rank. Sovereign looked like
 *          "Initiate but brighter," not like the apex rank it's meant to represent.
 *          Fix: the glow is now drawn by the shared RankBadgeGlow composable (see
 *          ui/dashboard/RankBadgeGlow.kt), which escalates genuinely per rank:
 *            Initiate  (2): single soft static ring.
 *            Challenger(3): dual-layer ring, breathing pulse.
 *            Conqueror (4): dual-layer ring in a hotter gold + a rotating shimmer sweep.
 *            Sovereign (5): triple-layer molten ring + twin shimmer sweep + 8 rotating
 *                           radiant light rays + a slow "living" breathing scale on the
 *                           whole badge+glow unit.
 *          All local glow-animation code (alpha tuples, infiniteTransition, radialGradient
 *          drawBehind) was removed from this file — RankUpScreen now just calls
 *          RankBadgeGlow(rank = newRank, badgeSize = 160.dp, modifier = spring-scale) { Image(...) }.
 */

package com.example.missionuncomfortable.ui.rankup

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionuncomfortable.ui.dashboard.Rank
import com.example.missionuncomfortable.ui.dashboard.RankBadgeGlow   // v4: shared prestige-tier glow composable

// ─── COLOUR CONSTANTS ────────────────────────────────────────────────────────

private val ColorBackground    = Color(0xFF0D0D0D)   // Near-black background
private val ColorTextPrimary   = Color(0xFFE0E0E0)   // Off-white — rank title
private val ColorTextSecondary = Color(0xFF8A8A8A)   // Muted grey — "YOU ARE NOW", description
private val ColorAccentGold    = Color(0xFFC8A84B)   // Muted gold — CONTINUE button + glow colour

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE — RankUpScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * RankUpScreen — full-screen rank-up celebration composable.
 *
 * Shown when the user earns enough XP to advance to the next rank.
 * All animation state is local — no ViewModel involvement needed for the
 * animations themselves. The ViewModel is only involved in triggering this
 * screen (via rankUpEvent) and dismissing it (via onRankUpCelebrationComplete).
 *
 * v2: Added rank-specific glow animation behind the badge.
 *     Badge is now wrapped in a 200dp container that draws the glow via drawBehind.
 *     The spring scale (badgeScale) is applied to the container so badge + glow
 *     animate in together as one unit.
 * v3: Glow switched from a filled centre→transparent gradient to a RING gradient
 *     (colorStops) that peaks just outside the badge's edge instead of at its centre,
 *     which the badge image was hiding. See file header CHANGELOG v3 for details.
 * v4: Glow drawing (and its container sizing) moved into the shared RankBadgeGlow
 *     composable (see ui/dashboard/RankBadgeGlow.kt) — this screen no longer draws
 *     the glow itself. badgeScale is still applied here, passed into RankBadgeGlow's
 *     `modifier` param so the spring entrance still animates badge + glow as one unit.
 *     See file header CHANGELOG v4 for the full per-rank prestige-tier breakdown.
 *
 * @param newRank    The rank the user just achieved. Used for badge image,
 *                   title text, and description text.
 * @param onContinue Called when the user taps CONTINUE. The caller
 *                   (DashboardScreen) passes { viewModel.onRankUpCelebrationComplete() }.
 */
@Composable
fun RankUpScreen(
    newRank: Rank,
    onContinue: () -> Unit
) {
    // ── ANIMATION STATE ───────────────────────────────────────────────────────
    // Single boolean flag — when true, all animations play to their target values.
    // Set to true on the first composition (LaunchedEffect(Unit)).
    // Using explicit MutableState (not `by` delegation) so the compiler can see that
    // animationsStarted.value is read by the animateXAsState targetValue expressions.
    // `by` delegation triggers a false "assigned value is never read" warning on the
    // `= true` assignment inside LaunchedEffect because the compiler only sees a setter
    // call and cannot trace Compose's recomposition reads across lambda boundaries.
    val animationsStarted = remember { mutableStateOf(false) }

    // ── BADGE SCALE ───────────────────────────────────────────────────────────
    // Springs from 0 (invisible) to 1 (full size).
    // Spring physics: MediumBouncy gives a single small overshoot —
    // the badge "lands" with weight rather than just fading in.
    // v2: Applied to the full glow container so badge + glow scale in together.
    val badgeScale by animateFloatAsState(
        targetValue = if (animationsStarted.value) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "BadgeScale"
    )

    // ── HEADER + TITLE ALPHA ──────────────────────────────────────────────────
    // "YOU ARE NOW" label and rank title fade in together, 400ms after animations start.
    val titleAlpha by animateFloatAsState(
        targetValue    = if (animationsStarted.value) 1f else 0f,
        animationSpec  = tween(durationMillis = 500, delayMillis = 400),
        label          = "TitleAlpha"
    )

    // ── DESCRIPTION ALPHA ─────────────────────────────────────────────────────
    // Rank description fades in 300ms after the title — staggered for drama.
    val descriptionAlpha by animateFloatAsState(
        targetValue    = if (animationsStarted.value) 1f else 0f,
        animationSpec  = tween(durationMillis = 500, delayMillis = 700),
        label          = "DescriptionAlpha"
    )

    // ── BUTTON OFFSET ─────────────────────────────────────────────────────────
    // CONTINUE button slides up from 40dp below its final position.
    // Starts at +40dp (below), animates to 0dp (final position).
    val buttonOffset by animateDpAsState(
        targetValue    = if (animationsStarted.value) 0.dp else 40.dp,
        animationSpec  = tween(durationMillis = 500, delayMillis = 500),
        label          = "ButtonOffset"
    )

    // v4: per-rank glow animation moved to the shared RankBadgeGlow composable
    // (see ui/dashboard/RankBadgeGlow.kt) — it now drives its own alpha/rotation/
    // breathing animations internally based on rank.level, so this screen no
    // longer needs its own glow alpha tuples or infiniteTransition.

    // ── START ANIMATIONS ──────────────────────────────────────────────────────
    // LaunchedEffect(Unit) runs once on first composition.
    // Setting animationsStarted.value = true triggers all animateXAsState values above.
    LaunchedEffect(Unit) {
        animationsStarted.value = true
    }

    // ── LAYOUT ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {

            // ── "YOU ARE NOW" LABEL ───────────────────────────────────────────
            // Small-caps, muted grey, letter-spaced — sets the tone before the badge appears.
            // Uses the same titleAlpha as the rank title so they fade in together.
            Text(
                text          = "YOU ARE NOW",
                color         = ColorTextSecondary,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                modifier      = Modifier.graphicsLayer { alpha = titleAlpha }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── RANK BADGE — animated scale-in + rank-specific prestige glow ──
            // v1: Badge springs from scale 0 → 1. The emotional centrepiece of the screen.
            // v2: The 200dp container Box now wraps both the glow and the badge image.
            //     graphicsLayer (spring scale) is on the container — the glow and badge
            //     scale in as one unit, landing with the same physical weight.
            // v4: Glow drawing now lives in the shared RankBadgeGlow composable (see
            //     ui/dashboard/RankBadgeGlow.kt). The spring-scale graphicsLayer below is
            //     passed in via `modifier` and composes with RankBadgeGlow's own container
            //     sizing (which now scales per-rank tier) and internal breathing scale
            //     (Sovereign only) — both apply to the same Box multiplicatively.
            //
            // checkNotNull() will crash loudly in testing if a rank has no badgeResId,
            // ensuring a missing drawable is caught immediately during development.
            RankBadgeGlow(
                rank = newRank,
                badgeSize = 160.dp,
                modifier = Modifier.graphicsLayer {
                    // Spring-animated scale applied to the full container.
                    // Both the glow (drawn inside RankBadgeGlow) and the badge image (inside)
                    // scale together — they arrive on screen as a single object.
                    scaleX = badgeScale
                    scaleY = badgeScale
                }
            ) {
                Image(
                    painter            = painterResource(
                        id = checkNotNull(newRank.badgeResId) {
                            "RankUpScreen: Rank '${newRank.title}' has no badgeResId set. " +
                                    "Add a drawable for this rank in DashboardModels.ALL_RANKS."
                        }
                    ),
                    contentDescription = "New rank badge: ${newRank.title}",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .size(160.dp)      // 160dp — same as v1. RankBadgeGlow sizes its own container around this.
                        .clip(CircleShape) // Circular clip — matches the badge drawables' circular design
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── NEW RANK TITLE ────────────────────────────────────────────────
            // e.g. "The Initiate" — large, bold, white.
            // Fades in with titleAlpha (same as the "YOU ARE NOW" label).
            Text(
                text       = newRank.title,
                color      = ColorTextPrimary,
                fontSize   = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.graphicsLayer { alpha = titleAlpha }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── RANK DESCRIPTION ──────────────────────────────────────────────
            // Short flavour text — e.g. "You've taken your first uncomfortable step."
            // Fades in after the title with a 300ms extra delay.
            Text(
                text       = newRank.description,
                color      = ColorTextSecondary,
                fontSize   = 14.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp,
                modifier   = Modifier.graphicsLayer { alpha = descriptionAlpha }
            )

            Spacer(modifier = Modifier.height(56.dp))

            // ── CONTINUE BUTTON — slides up from below ────────────────────────
            // The only interactive element on the screen.
            // Slides up from +40dp below its final position.
            // Gold background — same as ACCEPT THE MISSION on the dashboard.
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .offset(y = buttonOffset)        // Slides up from below
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorAccentGold)
                    .clickable(onClick = onContinue) // → DashboardViewModel.onRankUpCelebrationComplete()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text          = "CONTINUE",
                    color         = Color(0xFF0D0D0D),  // Near-black on gold — high contrast
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
