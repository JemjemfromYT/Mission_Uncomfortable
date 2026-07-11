/**
 * WelcomeScreen.kt  — v7
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
 *       The new emblem_welcome.xml was a gold FLAME.
 *     • ANIMATION — the spinning diamonds, counter-rotating geometry, and 8-ray
 *       system are gone. Replaced with a single soft gold glow ring that breathes
 *       slowly behind the flame (alpha 0.25 → 0.55 over 3 200 ms). Nothing
 *       rotates. The button shimmer sweep is preserved unchanged.
 *     • TEXT — replaced lone word "UNCOMFORTABLE" with the full app name as a
 *       two-line label:
 *         – "MISSION" (11sp, all-caps, gold, 5sp tracking — the category label)
 *         – "Uncomfortable" (28sp, bold, off-white — the app name itself)
 *     • TAGLINE — "Discomfort is the curriculum." removed entirely.
 *     • clip(CircleShape) removed from the flame Image.
 *
 * ─── WHAT CHANGED IN v6 ──────────────────────────────────────────────────────
 *
 *   Problems carried over from v5:
 *     1. ColorTextSecondary was declared but never used — caused Android Studio
 *        warning. Removed in v6.
 *     2. v5 delivery stripped comments, violating project commenting rules.
 *        v6 rebuilt from v4 source with every comment preserved.
 *
 * ─── WHAT CHANGED IN v7 ──────────────────────────────────────────────────────
 *
 *   Problems with v6:
 *     1. The breathing glow loop (rememberInfiniteTransition, glowAlpha) ran
 *        indefinitely — a slow alpha pulse cycling from 0.25 → 0.55 → 0.25 on
 *        a 3 200 ms repeat. The user asked for a "cinematic welcome", meaning
 *        things should REVEAL ONCE and then sit still. A perpetual loop is the
 *        opposite of cinematic — it feels like a loading screen or a game lobby.
 *     2. The button shimmer sweep (buttonShimmer, drawWithContent) also looped
 *        indefinitely — a bright streak cycling across the gold surface every
 *        3 000 ms. Same problem: it looks busy and restless, not calm and premium.
 *     3. emblem_welcome.xml v2 (the gold flame) was flagged as anatomically
 *        ambiguous. The teardrop silhouette reads as something other than a flame
 *        without supporting context. Replaced in v3 of that file.
 *
 *   What v7 does:
 *     • ALL LOOPING ANIMATIONS REMOVED:
 *         – rememberInfiniteTransition removed entirely.
 *         – glowAlpha removed. The glow behind the emblem is now a static circle
 *           at a fixed alpha (0.38f) — always present, never moving.
 *         – buttonShimmer removed. drawWithContent removed from the button.
 *           The button is a plain solid gold rectangle. No sweep, no loop.
 *     • ENTRANCE ANIMATIONS KEPT AND SLIGHTLY EXTENDED for cinematic weight:
 *         – Emblem: 1 200 ms fade-in + scale-up from 0.90 → 1.00 (was 800 ms,
 *           no scale). The scale-up gives a "materialise" feel — the gem appears
 *           to grow out of nothing.
 *         – Title: 900 ms fade-in + slide-up (was 600 ms). Heavier, more
 *           deliberate. Delay increased to 450 ms after emblem starts.
 *         – Button: 700 ms fade-in (was 500 ms). Delay 850 ms after title starts.
 *     • EMBLEM — emblem_welcome.xml updated to v3 (nested gold diamond/gem).
 *       The flame teardrop is gone. Three concentric rotated squares:
 *         1. Outer diamond: bold gold outline, 10% alpha fill.
 *         2. Middle diamond: thinner, slightly warmer gold — gem-cut bevel effect.
 *         3. Centre diamond: small solid gold focal point ("the stone").
 *       A diamond forms under pressure — thematically perfect for an app about
 *       voluntary discomfort. Cannot be misread. Distinct from all rank badges.
 *     • STATIC GLOW stays — a soft radial gradient behind the emblem that is
 *       drawn once and holds. It gives the gem a sense of being lit from within
 *       without any movement.
 *     • BUTTON stays gold solid — no shimmer, but the entrance fade-in is enough
 *       to make it feel dynamic when it first appears.
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// androidx.compose.foundation.shape.CircleShape — REMOVED in v5.
//   The emblem Image is no longer clipped to a circle.
// androidx.compose.animation.core.LinearEasing — REMOVED in v7.
//   Was used by buttonShimmer (infiniteRepeatable, LinearEasing). The shimmer
//   loop is gone in v7 — the button is a plain solid gold rectangle.
// androidx.compose.animation.core.RepeatMode — REMOVED in v7.
//   Was only used by the two infinite animations (glowAlpha, buttonShimmer).
//   Both are gone. No InfiniteTransition remains in this file.
// androidx.compose.animation.core.animateFloat — REMOVED in v7.
//   Was the animateFloat delegate used inside rememberInfiniteTransition for
//   glowAlpha and buttonShimmer. No infinite animations remain.
// androidx.compose.animation.core.infiniteRepeatable — REMOVED in v7.
//   Same reason — no infinite animations.
// androidx.compose.animation.core.rememberInfiniteTransition — REMOVED in v7.
//   The entire InfiniteTransition block is gone. Welcome screen now has
//   zero looping animations. Entrance plays once; everything holds still.
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
// androidx.compose.ui.draw.drawWithContent — REMOVED in v7.
//   Was used to draw the buttonShimmer streak on top of the button's gold
//   background. The shimmer loop is gone, so drawWithContent is no longer needed.
//   The button now uses a simple .background(ColorAccentGold) with no overlay.
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
// androidx.compose.ui.graphics.Path — REMOVED in v5.
//   Was needed to build the diamond shape on Canvas. Gone with canvas sigil.
// androidx.compose.ui.graphics.StrokeCap — REMOVED in v5. Same reason.
// androidx.compose.ui.graphics.drawscope.DrawScope — REMOVED in v5. Same reason.
// androidx.compose.ui.graphics.drawscope.Stroke — REMOVED in v5. Same reason.
// androidx.compose.ui.graphics.drawscope.rotate — REMOVED in v5. Same reason.
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionuncomfortable.R
import kotlinx.coroutines.delay
// kotlin.math.cos — REMOVED in v5. Was used for canvas ray positions.
// kotlin.math.sin — REMOVED in v5. Same reason.

// ─── COLOURS ─────────────────────────────────────────────────────────────────

private val ColorBackground    = Color(0xFF0D0D0D)
private val ColorTextPrimary   = Color(0xFFE0E0E0)
// ColorTextSecondary (0xFF8A8A8A) — REMOVED in v6.
//   Was used for the tagline "Discomfort is the curriculum." which was removed
//   in v5. The constant was accidentally left behind in v5, causing:
//     Warning (102, 13): Property "ColorTextSecondary" is never used.
//   Removed in v6 to clear the warning.
private val ColorAccentGold    = Color(0xFFC8A84B)

// ColorHotGold — REMOVED in v7.
//   Was used as the inner ring colour in the breathing glow (drawBehind on the
//   emblem Box). The glow ring used ColorHotGold at glowAlpha * 0.70f for the
//   peak of the radial gradient. Since glowAlpha (and the entire
//   rememberInfiniteTransition block) is gone in v7, and the static glow only
//   uses ColorAccentGold at fixed alphas, ColorHotGold is no longer referenced.

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
    // diamond which is gone. "Emblem" is the correct term for the image loaded
    // from emblem_welcome.xml. Logic is identical — only the name changed.
    var showEmblem by remember { mutableStateOf(false) }
    var showTitle  by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    // v7: delays slightly extended for cinematic weight.
    //   Emblem starts immediately on mount (unchanged).
    //   Title: 450 ms after mount (was 350 ms).
    //   Button: 850 ms after title starts (was 280 ms).
    // The entrance is the only animation on this screen. It plays once and stops.
    // Nothing loops. Nothing pulses. The screen "arrives" and then holds still.
    LaunchedEffect(Unit) {
        showEmblem = true
        delay(450L); showTitle  = true
        delay(850L); showButton = true
    }

    // ── ENTRANCE FLOAT STATES ────────────────────────────────────────────────
    // animateFloatAsState converts each boolean flip into a smooth eased
    // transition. These fire ONCE when the booleans above flip to true.
    // They do NOT loop or repeat.

    // v7: sigilAlpha duration extended to 1 200 ms (was 800 ms) so the gem
    // takes longer to fully materialise — weight matches the cinematic intent.
    val sigilAlpha  by animateFloatAsState(
        targetValue    = if (showEmblem) 1f else 0f,
        animationSpec  = tween(1200, easing = FastOutSlowInEasing),
        label          = "sigilAlpha"
    )
    // v5: "sigilSlide" kept as a name even though it now drives the emblem Image
    // instead of the canvas diamond. Slide-up from 50px offset, same as before.
    val sigilSlide  by animateFloatAsState(
        targetValue    = if (showEmblem) 0f else 50f,
        animationSpec  = tween(1200, easing = FastOutSlowInEasing),
        label          = "sigilSlide"
    )

    // v7: sigilScale added. Animates from 0.90 → 1.00 as the emblem appears.
    // WHY: A plain fade-in makes the gem "pop in" which feels abrupt at 1 200 ms.
    // A simultaneous scale-up from 90% makes it feel like the gem is materialising
    // out of darkness — growing into existence — which reads as cinematic.
    // 0.90 as the start value is subtle enough not to look like a "zoom in"
    // effect; it just removes the jarring appearance of a sudden full-size object.
    val sigilScale  by animateFloatAsState(
        targetValue    = if (showEmblem) 1f else 0.90f,
        animationSpec  = tween(1200, easing = FastOutSlowInEasing),
        label          = "sigilScale"
    )

    // v7: titleAlpha duration extended to 900 ms (was 600 ms).
    val titleAlpha  by animateFloatAsState(
        targetValue    = if (showTitle) 1f else 0f,
        animationSpec  = tween(900, easing = FastOutSlowInEasing),
        label          = "titleAlpha"
    )
    val titleSlide  by animateFloatAsState(
        targetValue    = if (showTitle) 0f else 28f,
        animationSpec  = tween(900, easing = FastOutSlowInEasing),
        label          = "titleSlide"
    )

    // v7: buttonAlpha duration extended to 700 ms (was 500 ms).
    val buttonAlpha by animateFloatAsState(
        targetValue    = if (showButton) 1f else 0f,
        animationSpec  = tween(700, easing = FastOutSlowInEasing),
        label          = "buttonAlpha"
    )

    // v7 REMOVED — rememberInfiniteTransition ("WelcomeLoop"):
    //   The entire InfiniteTransition block and both of its child animations
    //   are gone. The welcome screen now has ZERO looping animations.
    //
    //   WHY removed:
    //     The user asked for a "cinematic welcome". In a film, a title card
    //     appears, settles, and holds. It does not pulse or shimmer indefinitely.
    //     A perpetual glow loop or shimmer sweep makes the screen feel like a
    //     game lobby or a loading screen — the opposite of cinematic.
    //     Cinematic = one dramatic entrance, then stillness. The entrance
    //     animations above handle the drama. After they complete, everything rests.
    //
    // v7 REMOVED — glowAlpha (0.25 → 0.55, 3 200 ms repeat, Reverse):
    //   Was the breathing pulse of the glow ring behind the flame emblem.
    //   Replaced by a static glow at a fixed alpha of 0.38f — present from
    //   the moment the emblem fades in, never moving. See the drawBehind block
    //   below for the static glow implementation.
    //
    // v7 REMOVED — buttonShimmer (-0.5 → 1.5, 3 000 ms repeat, Restart):
    //   Was the bright streak that swept left→right across the gold button
    //   surface on a 3 000 ms loop. Replaced by a plain solid gold button.
    //   The button's entrance fade-in (buttonAlpha, 700 ms) provides all the
    //   visual interest needed when the button first appears. After that, it rests.

    // ── LAYOUT ──────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center
    ) {

        // Atmospheric background: warm faint spotlight behind the emblem.
        // A radial gradient centred on the upper half of the screen (38% from
        // top) so the background feels like a hidden light source is illuminating
        // the gem from within — not just a flat black void.
        // Unchanged from v5/v6.
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

            // ── GEM EMBLEM ───────────────────────────────────────────────────
            // v7: emblem_welcome.xml redesigned to v3 — a nested-diamond gem
            // motif (three concentric rotated squares). See emblem_welcome.xml
            // for full design reasoning.
            //
            // WHY a Box wrapping the Image:
            //   The drawBehind modifier on the Box draws the static glow behind
            //   the gem on the CONTAINER's background layer, giving the glow room
            //   to breathe outside the Image's bounds without clipping. The Image
            //   (150dp) is centred inside the container (220dp), leaving 35dp of
            //   glow room on each side.
            //
            // WHY 220dp container / 150dp Image:
            //   220dp provides enough radius for the glow to peak just outside
            //   the gem's outermost point and fade to nothing at the edge.
            //   150dp for the Image sizes the gem to fill most of the container
            //   while still leaving visible glow room around it.
            //
            // WHY no clip modifier:
            //   The diamond/gem shape is tall AND wide. Clipping to any shape
            //   (circle, rounded rect) would crop the four pointed corners of the
            //   outer diamond, making it look like a blob. No clip is applied.
            //
            // v7: graphicsLayer now includes scaleX and scaleY (sigilScale)
            //   in addition to alpha and translationY. This gives the gem the
            //   "materialise" entrance described in the CHANGELOG above.
            //
            // Visual layers (bottom to top):
            //   1. Static glow halo — soft gold radial gradient, fixed alpha,
            //      no animation. Present once the emblem fades in; never moves.
            //   2. The gem Image itself, centred on top of the glow.
            val containerSize = 220.dp
            val imageSize     = 150.dp

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(containerSize)
                    .graphicsLayer {
                        alpha        = sigilAlpha    // Entrance: fade in from 0
                        translationY = sigilSlide    // Entrance: slide up from 50px
                        scaleX       = sigilScale    // v7: materialise from 90%
                        scaleY       = sigilScale    // v7: uniform scale on both axes
                    }
                    .drawBehind {
                        val r = size.minDimension / 2f

                        // ── STATIC OUTER ATMOSPHERIC GLOW ───────────────────
                        // v7: replaces the animated glowAlpha gradient.
                        // Fixed alpha of 0.22f on the inner stop, 0.08f mid.
                        // The glow is always at this brightness — it does not
                        // breathe or pulse. It reads as the gem casting a soft
                        // warm light onto the dark background, like a lit jewel
                        // in a dark room. Static light is more cinematic than
                        // pulsing light.
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ColorAccentGold.copy(alpha = 0.22f),
                                    ColorAccentGold.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = r * 0.90f
                            ),
                            radius = r * 0.90f
                        )

                        // ── STATIC INNER RIM GLOW ────────────────────────────
                        // v7: replaces the animated inner ring (ColorHotGold,
                        // glowAlpha * 0.70f). Now a fixed alpha 0.28f ring.
                        // Peaks at ~60% of the container radius, which lines up
                        // with the outer diamond's edge in emblem_welcome.xml.
                        // The ring fades to transparent before and after its peak
                        // so it reads as a rim of light around the gem's face,
                        // not a drawn circle.
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.48f to Color.Transparent,
                                    0.60f to ColorAccentGold.copy(alpha = 0.28f),
                                    1.00f to Color.Transparent
                                ),
                                center = center,
                                radius = r
                            ),
                            radius = r
                        )
                    }
            ) {
                // ── GEM IMAGE ────────────────────────────────────────────────
                // Loads emblem_welcome.xml v3 (nested diamond gem) as a Compose
                // Image. ContentScale.Fit preserves the shape's aspect ratio.
                // No clip modifier — see WHY above.
                Image(
                    painter            = painterResource(id = R.drawable.emblem_welcome),
                    contentDescription = "Mission Uncomfortable gem emblem",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.size(imageSize)
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // ── APP NAME ──────────────────────────────────────────────────────
            // v5: replaced lone word "UNCOMFORTABLE" with the full app name as a
            // two-line label — same pattern as RANK LABEL → RANK NAME on Dashboard.
            // Unchanged from v5/v6.
            //
            // WHY "UNCOMFORTABLE" alone was wrong:
            //   A brand-new user has zero context. It reads as a philosophical
            //   statement, not an app name.
            //
            // WHY the two-line pattern works:
            //   "MISSION" (small gold label, heavy tracking) → type/category
            //   "Uncomfortable" (large off-white, bold) → the specific name
            //   Together: "This is Mission Uncomfortable." Zero ambiguity.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha        = titleAlpha
                    translationY = titleSlide
                }
            ) {
                // "MISSION" — the small gold category label.
                // 5sp letter-spacing reads as a stamped/engraved label,
                // matching the Dashboard's rank section headers.
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
                // 0.5sp tracking prevents letters feeling cramped at 28sp.
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
                // v5: kept from v4. Breathing room between name and button.
                // Reinforces gold palette without adding text.
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

            // ── CTA BUTTON ────────────────────────────────────────────────────
            // v7: the button is now a PLAIN SOLID GOLD rectangle.
            //   drawWithContent and the buttonShimmer sweep are gone.
            //   The shimmer loop made the button look restless — like a UI
            //   element waiting for interaction rather than a calm invitation.
            //   The button's entrance fade-in (buttonAlpha, 700 ms) provides all
            //   the visual impact it needs at first appearance. After the fade
            //   completes, the button holds still. Clean. Deliberate. Cinematic.
            //
            // The .clickable modifier and onStartJourney callback are unchanged.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .graphicsLayer { alpha = buttonAlpha }
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorAccentGold)
                    .clickable(onClick = onStartJourney)
            ) {
                Text(
                    text          = "BEGIN YOUR JOURNEY",
                    color         = Color(0xFF0D0D0D),   // Near-black on gold — max contrast
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            // v5 REMOVED — the single faint tagline Text:
            //   text  = "Discomfort is the curriculum."
            //   color = ColorTextSecondary.copy(alpha = 0.40f)
            //
            // WHY removed:
            //   The tagline was already "one line, very faint, almost invisible"
            //   (per the v4 comment). If it's that close to invisible, it adds
            //   no information — it's just noise. The gem emblem and the app
            //   name communicate the same idea better and without words.
            //   ColorTextSecondary was also removed in v6 since this was its
            //   only remaining usage.
        }
    }
}
