/**
 * WelcomeScreen.kt  — v6
 *
 * ─── WHAT CHANGED IN v4 ──────────────────────────────────────────────────────
 *
 *   Problems with v3:
 *     1. The emblem was still a circular badge drawn from emblem_welcome.xml —
 *        visually indistinguishable from any rank badge. Observer, Initiate, etc.
 *        all use circular badges. The welcome screen CANNOT use the same shape.
 *     2. Too much text — felt auto-generated and lifeless.
 *
 *   What v4 does instead:
 *     • EMBLEM — completely removed emblem_welcome.xml.
 *       The welcome screen now renders a bespoke animated SIGIL drawn 100% in
 *       Compose Canvas — no drawable resource at all. The sigil is a diamond
 *       geometry (rotated square), NOT a circle, so it reads nothing like any
 *       rank badge. Components:
 *         – Outer diamond stroked in gold, slowly rotating clockwise.
 *         – Inner diamond, slightly smaller, rotating counter-clockwise.
 *         – 8 thin light rays emanating from the centre, slowly rotating.
 *         – A pulsing radial gradient orb at the exact centre.
 *         – Two concentric faint glow circles behind everything.
 *       The entire sigil fills ~260dp of vertical space and commands the screen.
 *     • TEXT — cut to the absolute minimum:
 *         – "UNCOMFORTABLE" (the brand word, 34sp, full brightness).
 *         – "Discomfort is the curriculum." (one line, very faint, 11sp).
 *         – "BEGIN YOUR JOURNEY" button.
 *       "WELCOME TO THE" is gone. The tagline card is gone.
 *     • ENTRANCE — same staggered reveal (sigil → title → button) but faster.
 *     • BUTTON — same gold shimmer sweep as v3.
 *     • BACKGROUND — same faint warm radial spotlight.
 *
 * ─── WHAT CHANGED IN v5 ──────────────────────────────────────────────────────
 *
 *   Problems with v4:
 *     1. The spinning diamond sigil was overacting compared to the Dashboard's
 *        rank badge animations. Two counter-rotating diamonds plus 8 rotating
 *        light rays made the welcome screen visually louder than even the
 *        Sovereign rank's glow system — which undermines the sense of escalation
 *        the rank system is built around. The welcome screen should feel like an
 *        invitation, not a climax.
 *     2. "UNCOMFORTABLE" shown alone as a single word confused new users. Without
 *        context, it reads as a philosophical statement, not an app name. Users
 *        who have never seen the app before have no way to know what they opened.
 *
 *   What v5 does instead:
 *     • EMBLEM — reverted from canvas-drawn diamond back to emblem_welcome.xml,
 *       BUT the XML itself was redesigned. The old reticle (concentric circles,
 *       crosshair lines, diagonal ticks) still looked too much like a rank badge.
 *       The new emblem_welcome.xml is a gold FLAME — a tall, vertical shape with
 *       NO circular background and NO rings at all. It looks nothing like any of
 *       the five rank badges (Observer eye, Initiate triangle, Challenger shield,
 *       Conqueror crossbow, Sovereign crown). The flame represents:
 *         – Heat — the discomfort the app is built around.
 *         – A starting point — fire begins every journey.
 *         – Energy — raw starting potential, not a rank achievement.
 *     • ANIMATION — the spinning diamonds, counter-rotating geometry, and 8-ray
 *       system are gone. Replaced with a single soft gold glow ring that breathes
 *       slowly behind the flame (alpha 0.25 → 0.55 over 3 200 ms). Nothing
 *       rotates. The button shimmer sweep is preserved unchanged.
 *     • TEXT — replaced lone word "UNCOMFORTABLE" with the full app name as a
 *       two-line label:
 *         – "MISSION" (11sp, all-caps, gold, 5sp tracking — the category label)
 *         – "Uncomfortable" (28sp, bold, off-white — the app name itself)
 *       This mirrors the RANK LABEL → RANK NAME pattern used on the Dashboard
 *       (e.g. the small "RANK" label above "The Observer"). A new user can now
 *       read the screen and immediately understand what app they opened, with
 *       zero ambiguity.
 *     • TAGLINE — "Discomfort is the curriculum." removed entirely.
 *       The flame icon and the app name are enough. Every extra word is noise.
 *     • GOLD SEPARATOR — the thin horizontal accent line between the name and
 *       the button is kept. It provides visual breathing room and reinforces the
 *       gold palette without adding text.
 *     • clip(CircleShape) removed from the flame Image — a circular crop was
 *       cutting off the tip and the base of the flame silhouette. The Image now
 *       displays at its natural bounds (150dp) inside the 220dp glow container.
 *       Import for CircleShape removed as it is no longer referenced.
 *
 * ─── WHAT CHANGED IN v6 ──────────────────────────────────────────────────────
 *
 *   Problems carried over from the v5 delivery:
 *     1. ColorTextSecondary (0xFF8A8A8A) was still declared but never used —
 *        the tagline Text that referenced it was removed in v5, but the constant
 *        was accidentally left behind. Android Studio reported:
 *          Warning (102, 13): Property "ColorTextSecondary" is never used.
 *     2. The v5 code delivered as a paste-in replacement stripped out a large
 *        portion of the existing comments, violating the project commenting rules
 *        established in RankBadgeGlow.kt:
 *          "DO NOT remove any existing comment. If a comment is outdated, UPDATE it."
 *        This v6 file is built directly from the v4 source, with every comment
 *        preserved and updated rather than deleted.
 *
 *   Fixes in v6:
 *     • ColorTextSecondary constant removed. The removal is noted in the colour
 *       constants block below so a future reader understands why it is absent.
 *     • All canvas-drawing imports removed with comments explaining the reason —
 *       they are no longer needed because the canvas-drawn diamond is gone.
 *     • All comments from v4 are preserved and updated to reflect the v5/v6 state.
 *     • New code (flame emblem section, updated text section) written with the same
 *       comment density as the rest of the file.
 *
 * ─── FILE LOCATION ───────────────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/WelcomeScreen.kt
 *
 * ─── PACKAGE ─────────────────────────────────────────────────────────────────
 */

package com.example.missionuncomfortable.ui.welcome

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image                          // v5: added — needed for the flame Image composable (was not needed in v4 which drew on Canvas)
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// androidx.compose.foundation.shape.CircleShape — REMOVED in v5.
//   The flame Image is no longer clipped to a circle. A circular crop was cutting
//   off the tip and base of the flame silhouette, ruining the shape.
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
// androidx.compose.ui.graphics.Path — REMOVED in v5.
//   Was needed to build the diamond shape on Canvas. The canvas-drawn diamond
//   is gone; emblem_welcome.xml renders the shape instead.
// androidx.compose.ui.graphics.StrokeCap — REMOVED in v5.
//   Was needed for the rounded line caps on the 8 radiant rays drawn in Canvas.
//   No custom line drawing happens in this file any more.
// androidx.compose.ui.graphics.drawscope.DrawScope — REMOVED in v5.
//   The helper function drawDiamondPath that used DrawScope as a receiver was
//   removed along with the canvas-drawn sigil. No DrawScope extension functions
//   remain in this file.
// androidx.compose.ui.graphics.drawscope.Stroke — REMOVED in v5.
//   Was the stroke style for the diamond paths. No stroked canvas paths remain.
// androidx.compose.ui.graphics.drawscope.rotate — REMOVED in v5.
//   Was used to rotate the outer/inner diamonds and the light rays on Canvas.
//   Nothing rotates in v5 — the glow behind the flame is a static radial shape
//   that only breathes in alpha.
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale                   // v5: added — needed to specify ContentScale.Fit for the flame Image
import androidx.compose.ui.res.painterResource                   // v5: added — loads emblem_welcome.xml as a Painter for the Image composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionuncomfortable.R                        // v5: added — needed to reference R.drawable.emblem_welcome
import kotlinx.coroutines.delay
// kotlin.math.cos — REMOVED in v5. Was used to compute the x-components of
//   the 8 light ray endpoint positions on the canvas. No trigonometry needed
//   now that the canvas-drawn sigil is gone.
// kotlin.math.sin — REMOVED in v5. Same reason as cos above.

// ─── COLOURS ─────────────────────────────────────────────────────────────────

private val ColorBackground    = Color(0xFF0D0D0D)
private val ColorTextPrimary   = Color(0xFFE0E0E0)
// ColorTextSecondary (0xFF8A8A8A) — REMOVED in v6.
//   This constant was introduced in v1 and used for the tagline text and the
//   "Discomfort is the curriculum." line. Both of those Text composables were
//   removed in v5 because the screen is simpler without them. The constant
//   was accidentally left behind in v5, which caused:
//     Warning (102, 13): Property "ColorTextSecondary" is never used.
//   Removed here in v6 to clear the warning. If a secondary text colour is
//   needed again in a future version, re-add it at that point.
private val ColorAccentGold    = Color(0xFFC8A84B)
private val ColorHotGold       = Color(0xFFDCB65C)

// ─── ROOT COMPOSABLE ─────────────────────────────────────────────────────────

/**
 * WelcomeScreen — shown once on first launch.
 *
 * @param onStartJourney  MainActivity callback: write has_seen_welcome=true,
 *                        then switch to DashboardScreen.
 */
@Composable
fun WelcomeScreen(onStartJourney: () -> Unit) {

    // ── ENTRANCE ANIMATION FLAGS ─────────────────────────────────────────────
    // v5: renamed showSigil → showEmblem. "Sigil" referred to the canvas-drawn
    // diamond which is gone. "Emblem" is the correct term for the flame Image
    // loaded from emblem_welcome.xml. Logic is identical — only the name changed.
    var showEmblem by remember { mutableStateOf(false) }
    var showTitle  by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showEmblem = true                          // Flame fades in immediately on mount
        delay(350L); showTitle  = true             // App name fades in 350 ms later
        delay(280L); showButton = true             // Button fades in 280 ms after title
    }

    // animateFloatAsState converts each boolean flip into a smooth eased transition.
    // v5: "sigilAlpha" and "sigilSlide" variable names kept intentionally — renaming
    // would not change behaviour and would diff-noise any future code review.
    // They now drive the flame Image's entrance, not the canvas diamond's entrance.
    val sigilAlpha  by animateFloatAsState(
        targetValue = if (showEmblem)  1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing), label = "sigilAlpha"
    )
    val sigilSlide  by animateFloatAsState(
        targetValue = if (showEmblem)  0f else 50f,
        animationSpec = tween(800, easing = FastOutSlowInEasing), label = "sigilSlide"
    )
    val titleAlpha  by animateFloatAsState(
        targetValue = if (showTitle)  1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing), label = "titleAlpha"
    )
    val titleSlide  by animateFloatAsState(
        targetValue = if (showTitle)  0f else 28f,
        animationSpec = tween(600, easing = FastOutSlowInEasing), label = "titleSlide"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (showButton) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing), label = "buttonAlpha"
    )

    // ── CONTINUOUS ANIMATIONS ────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "WelcomeLoop")

    // v5 REMOVED — outerDiamondAngle:
    //   Drove the clockwise rotation of the outer diamond (full revolution in 20 s).
    //   Gone because the canvas-drawn rotating diamond is gone.

    // v5 REMOVED — innerDiamondAngle:
    //   Drove the counter-clockwise rotation of the inner diamond (14 s).
    //   Gone for the same reason.

    // v5 REMOVED — rayAngle:
    //   Drove the slow rotation of the 8 radiant light rays (18 s).
    //   Gone because no light rays are drawn in v5.

    // v5 REMOVED — orbAlpha:
    //   Drove the pulsing brightness of the centre orb (1 800 ms half-cycle).
    //   Gone because there is no centre orb in v5 — the flame Image has its
    //   own internal bright core baked into the drawable.

    // Glow ring: the single soft gold halo behind the flame breathes slowly.
    // v5: "glowAlpha" was previously the outer glow circle behind the diamond sigil
    // (0.25 → 0.55, 3 200 ms). In v5, this same variable is repurposed as the
    // breathing pulse for the glow ring behind the flame. The range and duration
    // are kept identical — the welcome screen should feel calm, not urgent.
    // Nothing rotates. This is the ONLY continuous animation on the emblem.
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "GlowAlpha"
    )

    // CTA button shimmer sweep — unchanged from v4.
    // A narrow bright streak that sweeps left→right across the gold surface
    // on a 3 000 ms loop. Reads as polished metal catching light, not a loading bar.
    val buttonShimmer by infiniteTransition.animateFloat(
        initialValue = -0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ButtonShimmer"
    )

    // ── LAYOUT ──────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center
    ) {

        // Atmospheric background: warm faint spotlight behind the emblem.
        // A radial gradient centred on the upper half of the screen (38% from top)
        // so the background feels like a hidden light source is illuminating the
        // flame from within — not just a flat black void.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val spotCenter = Offset(size.width / 2f, size.height * 0.38f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1C1609), Color.Transparent),
                            center = spotCenter,
                            radius = size.width * 0.85f
                        ),
                        radius = size.width * 0.85f,
                        center = spotCenter
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {

            // ── FLAME EMBLEM ──────────────────────────────────────────────
            // v5: replaced the canvas-drawn diamond sigil with emblem_welcome.xml,
            // which was simultaneously redesigned from a targeting reticle (circles,
            // crosshair) to a gold FLAME (see emblem_welcome.xml header for full
            // design reasoning). The Image is loaded via painterResource.
            //
            // WHY a Box wrapping the Image instead of just a plain Image:
            //   The drawBehind modifier draws the glow ring on the CONTAINER's
            //   background — the 220dp Box — not on the Image itself. This gives
            //   the glow room to breathe outside the Image's bounds without being
            //   clipped. The Image (150dp) sits centred inside the container (220dp),
            //   leaving 35dp of glow room on each side.
            //
            // WHY 220dp container / 150dp Image:
            //   220dp provides enough radius for the glow ring to peak just outside
            //   the flame's widest point and fade to nothing at the container's edge.
            //   150dp for the Image is sized so the flame fills most of the container
            //   while still leaving glow room — 140dp (v4's value) was used when a
            //   clip(CircleShape) was cropping it; 150dp makes better use of the
            //   space now that the clip is gone.
            //
            // WHY clip(CircleShape) is REMOVED:
            //   The flame is a TALL vertical shape. Clipping it to a circle crops
            //   the tip at the top and the decorative base line at the bottom,
            //   completely destroying the silhouette that makes it read as a flame
            //   and not a circular badge. No clip is applied in v5+.
            //
            // Visual layers (bottom to top):
            //   1. Wide outer glow halo — very soft, breathes via glowAlpha
            //   2. Tighter inner glow ring — peaks at the flame's midsection width
            //   3. The flame Image itself, centred on top of the glow
            val containerSize = 220.dp
            val imageSize     = 150.dp   // v5: was 140dp with clip(CircleShape); 150dp without clip

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(containerSize)
                    .graphicsLayer {
                        alpha        = sigilAlpha    // Entrance: fade in from 0
                        translationY = sigilSlide    // Entrance: slide up from 50px offset
                    }
                    .drawBehind {
                        val r = size.minDimension / 2f

                        // ── 1. WIDE OUTER ATMOSPHERIC GLOW ──────────────────
                        // A soft radial gradient that fills the container — makes
                        // the flame feel like it is emanating light rather than just
                        // sitting in darkness. Outer edge fades completely to
                        // transparent so there is no hard boundary visible.
                        // Peak alpha is glowAlpha * 0.55f on the inner stop and
                        // glowAlpha * 0.18f on the mid stop — very low intentionally.
                        // This is atmosphere, not a spotlight.
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ColorAccentGold.copy(alpha = glowAlpha * 0.55f),
                                    ColorAccentGold.copy(alpha = glowAlpha * 0.18f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = r * 0.85f
                            ),
                            radius = r * 0.85f
                        )

                        // ── 2. TIGHTER INNER RING JUST OUTSIDE THE FLAME ────
                        // v5: this replaces all the canvas diamond geometry.
                        // A ring whose peak brightness sits at ~60% of the container
                        // radius — which corresponds to approximately the widest
                        // point of the flame silhouette in emblem_welcome.xml.
                        // The ring fades back to transparent before and after its
                        // peak, so it reads as a rim of warm light around the flame's
                        // body, not as a circle drawn on screen.
                        // ColorHotGold (warmer gold) is used here so the inner ring
                        // reads slightly hotter/brighter than the outer halo —
                        // the same layering principle used in RankBadgeGlow.kt.
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.50f to Color.Transparent,
                                    0.60f to ColorHotGold.copy(alpha = glowAlpha * 0.70f),
                                    1.00f to Color.Transparent
                                ),
                                center = center,
                                radius = r
                            ),
                            radius = r
                        )
                    }
            ) {
                // ── 3. FLAME IMAGE ────────────────────────────────────────
                // Loads emblem_welcome.xml (the gold flame drawable) as a
                // Compose Image. ContentScale.Fit preserves the flame's
                // aspect ratio so it is never stretched or squashed.
                // No clip modifier — see the WHY comment on clip(CircleShape)
                // above.
                Image(
                    painter            = painterResource(id = R.drawable.emblem_welcome),
                    contentDescription = "Mission Uncomfortable flame emblem",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.size(imageSize)
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // ── APP NAME ──────────────────────────────────────────────────
            // v5: replaced the single word "UNCOMFORTABLE" with the full app name
            // displayed as a two-line label — the same pattern used for rank names
            // on the Dashboard.
            //
            // WHY "UNCOMFORTABLE" alone was wrong:
            //   A brand-new user has zero context. The word alone reads as a
            //   philosophical statement or a description of a feeling, not as an
            //   app name. "What is this app? Why does it just say UNCOMFORTABLE?"
            //
            // WHY the two-line pattern fixes it:
            //   "MISSION" (small gold label, heavy tracking) → type/category
            //   "Uncomfortable" (large off-white, bold) → the specific name
            //   Together they read instantly as: "This is an app called Mission
            //   Uncomfortable." Same pattern as "RANK" → "The Observer" on the
            //   Dashboard. Familiar visual grammar for returning users; clear for
            //   new users.
            //
            // WHY mixed case "Uncomfortable" instead of "UNCOMFORTABLE":
            //   Mixed case reads as a proper name (which it is).
            //   All-caps reads as a shout or a warning. The v4 design was
            //   accidentally intimidating rather than welcoming.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha        = titleAlpha
                    translationY = titleSlide
                }
            ) {
                // "MISSION" — the small gold category label.
                // 5sp letter-spacing makes it read as a stamped/engraved label,
                // matching the aesthetic of the Dashboard's rank section headers.
                Text(
                    text          = "MISSION",
                    color         = ColorAccentGold.copy(alpha = 0.85f),
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 5.sp,
                    textAlign     = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // "Uncomfortable" — the app name, full brightness, bold.
                // Mixed case so it reads as a proper name, not a declaration.
                // 0.5sp tracking to prevent letters from feeling cramped at 28sp.
                Text(
                    text          = "Uncomfortable",
                    color         = ColorTextPrimary,
                    fontSize      = 28.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign     = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gold separator — thin horizontal accent line below the name.
                // v5: kept from v4. Provides visual breathing room between the
                // name and the button gap, and reinforces the gold accent palette
                // without adding any text.
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ColorAccentGold.copy(alpha = 0.75f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(52.dp))

            // ── CTA BUTTON ────────────────────────────────────────────────
            // Gold fill + sweeping shimmer streak = polished metal feel.
            // Unchanged from v4. The shimmer sweep travels left→right using the
            // buttonShimmer value, drawn as a drawWithContent overlay on top of
            // the gold background so it doesn't need a separate layer composable.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .graphicsLayer { alpha = buttonAlpha }
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorAccentGold)
                    .drawWithContent {
                        drawContent()   // Draw the "BEGIN YOUR JOURNEY" label first
                        // Shimmer overlay: a narrow bright streak at the current
                        // position drives by buttonShimmer (-0.5 → 1.5 of width).
                        // Starting before 0 and ending after 1 ensures the streak
                        // enters and exits smoothly from off-screen rather than
                        // popping into existence at the button's edge.
                        val shimmerStartX = size.width * buttonShimmer
                        val shimmerWidth  = size.width * 0.38f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                startX = shimmerStartX,
                                endX   = shimmerStartX + shimmerWidth
                            )
                        )
                    }
                    .clickable(onClick = onStartJourney)
            ) {
                Text(
                    text          = "BEGIN YOUR JOURNEY",
                    color         = Color(0xFF0D0D0D),   // Near-black on gold — maximum contrast
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            // v5 REMOVED — the single faint tagline Text:
            //   text = "Discomfort is the curriculum."
            //   color = ColorTextSecondary.copy(alpha = 0.40f)
            //
            // WHY removed:
            //   The tagline was already "one line, very faint, almost invisible"
            //   (per the v4 comment). If it's that close to invisible, it adds
            //   no information — it's just noise. The flame emblem and the app
            //   name communicate the same idea better and without words.
            //   ColorTextSecondary was also removed in v6 since this was its
            //   only remaining usage.
        }
    }
}
