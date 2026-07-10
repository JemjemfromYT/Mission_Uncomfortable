/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        ⚠  AI ASSISTANT — READ THIS BEFORE EDITING THIS FILE  ⚠         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PROJECT: Mission: Uncomfortable — Android app (Kotlin + Jetpack Compose)
 *          Daily social-discomfort challenges. User accepts a mission → goes
 *          and does it → returns → rates discomfort (1–10) → earns XP → ranks up.
 *
 * KEY FILES for this feature:
 *   AscensionLore.kt        — Story content + colour theme per rank (read this first).
 *   AscensionBookScreen.kt  — this file. The full-screen book UI.
 *   DashboardViewModel.kt   — fires ascensionEvent: Rank? when a book should be shown.
 *   NavGraph.kt             — routes "ascension" → AscensionBookScreen.
 *
 * ── COMMENTING RULES — NEVER break these ────────────────────────────────────
 *   1. DO NOT remove any existing comment. If a comment is outdated, UPDATE it.
 *   2. ADD comments to every new block of code you write.
 *   3. COMMENT STYLE: section dividers, KDoc blocks, inline reasoning.
 *   4. CHANGELOG block in the file header, updated on every significant change.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these rules, the developer will have to   ║
 * ║  manually fix every file you touch. Please respect these conventions.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * AscensionBookScreen.kt
 *
 * "The Ascension Book" — a full-screen storybook reveal shown exactly once per
 * rank promotion (including the very first day, when the user becomes an Observer).
 *
 * ─── EXPERIENCE FLOW ─────────────────────────────────────────────────────────
 *
 *   CLOSED   → A closed book sits in darkness, breathing with a slow ambient glow
 *              in the rank's colour. Ember particles drift upward around it.
 *              The book is rendered as three strips: a dark spine on the left,
 *              the cover face in the centre, and a thick fore-edge strip on the
 *              right made of many visible cream/gold page lines — giving a clear
 *              sense of physical book thickness before it is opened.
 *              Tapping the book (or the "OPEN" hint beneath it) begins OPENING.
 *
 *   OPENING  → The cover swings open hinged on its LEFT (spine) edge — exactly
 *              how a real book opens — using graphicsLayer rotationY with
 *              TransformOrigin(0f, 0.5f). A bright flash sweeps the screen at the
 *              moment of maximum opening — the "the light was always inside" beat.
 *
 *   READING  → A HorizontalPager presents the story: a title page, each lore
 *              paragraph as its own page, and a final page with the closing line
 *              and a "CLOSE THE BOOK" button. Pages use a true page-curl effect:
 *              the horizontal scroll translation is cancelled in graphicsLayer and
 *              replaced with a rotationY pivot on the correct edge (right edge for
 *              the leaving page, left edge for the arriving page), so each swipe
 *              looks like physically turning a book leaf rather than sliding a card.
 *              Small dot indicators show progress.
 *
 *   CLOSING  → Tapping "CLOSE THE BOOK" fades the whole scene to black, then
 *              calls onFinish() so the caller (NavGraph) can pop back to Dashboard.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/ascension/AscensionBookScreen.kt
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial implementation. Closed book → hinge-open animation → paged
 *        story reader → fade-out close, themed per rank via AscensionLore.kt.
 *   v2 — Realistic book visual (spine + fore-edge), Y-axis spine-hinge opening,
 *        3D page-turn graphicsLayer effect.
 *   v3 — Thicker fore-edge with visible page lines, page-background for legibility,
 *        proper page-curl by cancelling pager translation and pivoting on the
 *        correct edge (right for leaving page, left for arriving page).
 */

package com.example.missionuncomfortable.ui.ascension

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionuncomfortable.ui.dashboard.Rank
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// SHARED COLOUR CONSTANTS — the "60%" of the 60/30/10 palette used in this file
// ─────────────────────────────────────────────────────────────────────────────

// Near-black void — the base of every book scene, regardless of rank.
// This is intentionally darker than the app's own ColorBackground (#0D0D0D)
// so the rank's accent colour (the 30%) reads as dramatically brighter by contrast.
private val ColorVoid = Color(0xFF060606)

// Off-white used for body text on every page — kept neutral so the rank's
// accent colour (not the text) carries the "10% highlight" weight.
private val ColorPageText = Color(0xFFE7E2D8)

// Aged-paper background for the book's interior pages — warm off-white so text
// reads clearly and the scene feels like parchment, not a screen.
private val ColorPageBackground = Color(0xFF1A1610)

// ─────────────────────────────────────────────────────────────────────────────
// STAGE STATE MACHINE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * BookStage — the four states of the Ascension Book experience.
 *
 *   CLOSED  — The book sits shut, waiting for a tap.
 *   OPENING — The cover animates open (~900ms). No user input is accepted.
 *   READING — The HorizontalPager story is visible and swipeable.
 *   CLOSING — A fade-to-black plays, then onFinish() is invoked.
 */
private enum class BookStage {
    CLOSED,   // Waiting for the user to tap the book
    OPENING,  // Cover-open animation is playing
    READING,  // Story pager is visible
    CLOSING   // Fade-out before returning to Dashboard
}

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE — AscensionBookScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AscensionBookScreen — the full-screen storybook shown on a rank promotion.
 *
 * @param rank      The Rank the user just achieved (or, on first launch, Level 1
 *                   Observer). Its level selects the lore + theme from AscensionLore.kt.
 * @param onFinish  Called once the user taps "CLOSE THE BOOK" and the closing fade
 *                   completes. The caller (NavGraph) marks the book as seen and
 *                   pops back to Dashboard.
 */
@Composable
fun AscensionBookScreen(rank: Rank, onFinish: () -> Unit) {
    // Look up this rank's story + theme once. `rank` does not change during the
    // life of this screen (a new rank promotion always creates a new navigation
    // entry), so remember(rank.level) is safe and avoids recomputing on every
    // recomposition (e.g. every pager drag frame).
    val lore = remember(rank.level) { getLoreForRank(rank.level) }

    // ── STAGE STATE ───────────────────────────────────────────────────────────
    var stage by remember { mutableStateOf(BookStage.CLOSED) }

    // ── SCREEN FADE (used only for the CLOSING stage) ─────────────────────────
    // 1f = fully visible, 0f = fully black. Animates down to 0 when stage = CLOSING,
    // and onFinish() fires once the animation settles (see LaunchedEffect below).
    val screenAlpha by animateFloatAsState(
        targetValue = if (stage == BookStage.CLOSING) 0f else 1f,
        animationSpec = tween(durationMillis = 550),
        label = "AscensionScreenFade"
    )

    // When CLOSING begins, wait for the fade to finish, then hand control back
    // to the caller. 600ms > the 550ms fade duration, leaving a small safety margin.
    LaunchedEffect(stage) {
        if (stage == BookStage.CLOSING) {
            delay(600)
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha }   // Whole-screen fade for CLOSING
            .background(
                // Ambient background: a radial wash of the rank's mist colour
                // fading into the shared near-black void — the "60%" base with
                // the rank's colour breathing underneath it.
                Brush.radialGradient(
                    colors = listOf(lore.theme.mistColor, ColorVoid),
                    radius = 1200f
                )
            )
    ) {
        // ── EMBER PARTICLES — ambient drifting motes in the rank's hot colour ──
        // Drawn behind everything else. Present in every stage for continuity.
        EmberParticles(color = lore.theme.hotColor)

        when (stage) {
            BookStage.CLOSED, BookStage.OPENING -> {
                ClosedBookScene(
                    lore = lore,
                    isOpening = stage == BookStage.OPENING,
                    onOpenAnimationComplete = { stage = BookStage.READING },
                    onTap = {
                        if (stage == BookStage.CLOSED) stage = BookStage.OPENING
                    }
                )
            }

            BookStage.READING -> {
                StoryReader(
                    lore = lore,
                    rank = rank,
                    onCloseBook = { stage = BookStage.CLOSING }
                )
            }

            BookStage.CLOSING -> {
                // Nothing new to draw — the Box's graphicsLayer alpha fade (above)
                // handles the entire closing visual by fading the last-drawn frame
                // (the story reader) to black. Rendering StoryReader() again here
                // would restart its own internal state; instead we simply draw
                // nothing new and let the fade do the work.
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMBER PARTICLES — ambient drifting motes of light
// ─────────────────────────────────────────────────────────────────────────────

/**
 * EmberParticles — draws a field of soft, slowly rising light motes in [color].
 *
 * Purely decorative. Each particle has a randomised (but stable across
 * recompositions, via `remember`) starting position, drift speed, and size.
 * A single shared InfiniteTransition drives all particles so only one
 * animation clock is created rather than one per particle.
 *
 * @param color  The "hot" highlight colour for the current rank's theme.
 */
@Composable
private fun EmberParticles(color: Color) {
    // Number of particles — enough to feel alive without looking busy or
    // costing meaningful frame time on low-end devices.
    val particleCount = 22

    // Each particle: (horizontal position 0..1, phase offset 0..1, size in dp).
    // Random(seed) keeps the layout stable across recompositions within this
    // screen's lifetime — remember{} ensures it is computed only once.
    val particles = remember {
        val rng = Random(seed = 42)
        List(particleCount) {
            Triple(rng.nextFloat(), rng.nextFloat(), 1.5.dp + (rng.nextFloat() * 2.5).dp)
        }
    }

    // A single infinite transition drives a 0f..1f cycle over 14 seconds.
    // All particles read from this same progress value, offset by their own
    // phase, so they drift at staggered times rather than in lockstep.
    val infiniteTransition = rememberInfiniteTransition(label = "EmberParticlesTransition")
    val cycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EmberParticlesCycle"
    )

    val density = LocalDensity.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width

        particles.forEach { (xFraction, phase, sizeDp) ->
            // Each particle's local progress is the shared cycle offset by its
            // own phase, wrapped back into 0..1 — this staggers their timing.
            val local = (cycle + phase) % 1f

            // Rises from just below the bottom edge to just above the top edge.
            val y = h * (1f - local) + h * 0.15f
            // A gentle horizontal sway using sin(), so motes don't travel in
            // perfectly straight vertical lines.
            val x = w * xFraction + sin(local * Math.PI.toFloat() * 2f) * 14f

            // Fades in over the first 15% of its journey and out over the last
            // 25%, so particles never "pop" abruptly into or out of existence.
            val alpha = when {
                local < 0.15f -> local / 0.15f
                local > 0.75f -> (1f - local) / 0.25f
                else -> 1f
            }.coerceIn(0f, 1f)

            drawCircle(
                color = color.copy(alpha = alpha * 0.55f),
                radius = with(density) { sizeDp.toPx() },
                center = Offset(x, y)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CLOSED BOOK SCENE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ClosedBookScene — shows the closed book with a breathing glow, and (when
 * [isOpening] is true) plays the spine-hinge open animation before calling
 * [onOpenAnimationComplete].
 *
 * The book is composed of three horizontal strips in a Row:
 *   • Spine     — a narrow (18 dp) dark gradient strip on the left with a rounded
 *                 left edge, representing the bound spine.
 *   • Cover     — the main face (200 dp wide): sigil, breathing glow halo, title.
 *   • Fore-edge — a thick (28 dp) Canvas on the right drawing 80 horizontal lines
 *                 in alternating light/dark shades to simulate the visible stacked
 *                 page leaves. The lines are prominent enough to read as book-depth
 *                 even at a glance.
 *
 * Opening animation: rotationY 0 → -90 degrees, TransformOrigin(0f, 0.5f) so the
 * cover pivots on its left (spine) edge — exactly like a real book opening. At -90
 * the cover is edge-on (invisible), triggering the transition to READING.
 *
 * @param lore                       This rank's lore + theme.
 * @param isOpening                  True once the user has tapped the book.
 * @param onOpenAnimationComplete    Called once the open animation finishes.
 * @param onTap                      Called when the user taps the book while closed.
 */
@Composable
private fun ClosedBookScene(
    lore: AscensionLore,
    isOpening: Boolean,
    onOpenAnimationComplete: () -> Unit,
    onTap: () -> Unit
) {
    // ── BREATHING GLOW ────────────────────────────────────────────────────────
    // A slow pulse between two alpha values, giving the impression the book is
    // "alive" and waiting, before the user has interacted with it at all.
    val infiniteTransition = rememberInfiniteTransition(label = "ClosedBookGlow")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ClosedBookGlowAlpha"
    )

    // ── SPINE-HINGE ROTATION ───────────────────────────────────────────────────
    // rotationY from 0 (flat, facing the viewer) to -90 degrees (edge-on, fully
    // open). TransformOrigin(0f, 0.5f) means the LEFT edge of the entire Row
    // stays fixed while the right side swings away — exactly like a real book.
    val openRotation by animateFloatAsState(
        targetValue = if (isOpening) -90f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "BookOpenRotation",
        finishedListener = {
            if (isOpening) onOpenAnimationComplete()
        }
    )

    // openProgress: 0 = fully closed, 1 = fully open (edge-on at -90 degrees).
    // Used to drive the flash overlay and to fade the hint text.
    val openProgress = (openRotation / -90f).coerceIn(0f, 1f)

    // ── OPENING FLASH ──────────────────────────────────────────────────────────
    // A brief bright flash timed to peak roughly when the cover has swung past
    // the halfway point — sells the "light was inside all along" beat.
    val flashAlpha = when {
        openProgress < 0.35f -> 0f
        openProgress < 0.65f -> (openProgress - 0.35f) / 0.30f          // ramp up
        openProgress < 1f    -> (1f - (openProgress - 0.65f) / 0.35f)   // ramp down
        else -> 0f
    }.coerceIn(0f, 1f)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ── BOOK BODY (spine | cover | fore-edge) ─────────────────────────
            // The entire Row rotates together as one rigid object, pivoting on
            // the LEFT edge (TransformOrigin x=0f) to mimic a spine hinge.
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        rotationY = openRotation
                        // Increased camera distance prevents perspective clipping at
                        // steep angles during the open swing.
                        cameraDistance = 14 * density
                        // LEFT-edge pivot = spine hinge.
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        // Fade the cover out as it approaches edge-on so it
                        // disappears cleanly before READING takes over.
                        alpha = 1f - (openProgress * 0.85f)
                    }
                    .height(300.dp)
                    .clickable(enabled = !isOpening, onClick = onTap),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ── SPINE STRIP ────────────────────────────────────────────────
                // Narrow dark gradient, rounded only on the left edge.
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .fillMaxHeight()
                        .clip(
                            RoundedCornerShape(
                                topStart = 10.dp,
                                bottomStart = 10.dp,
                                topEnd = 0.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF020202),       // darkest at the very left edge
                                    Color(0xFF181818)        // lightens slightly toward the cover
                                )
                            )
                        )
                )

                // ── COVER FACE ─────────────────────────────────────────────────
                // Main visible face: no left/right rounding (flush with spine
                // and fore-edge strips), rounded top and bottom right corners.
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    lore.theme.mistColor,
                                    Color(0xFF0A0A0A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow halo behind the sigil — breathes while closed.
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        lore.theme.accentColor.copy(alpha = breathingAlpha * 0.5f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // ── SIGIL ──────────────────────────────────────────────
                        Text(
                            text = lore.theme.sigil,
                            fontSize = 44.sp,
                            color = lore.theme.accentColor
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // ── BOOK TITLE (engraved on the cover) ────────────────
                        Text(
                            text = lore.bookTitle,
                            color = lore.theme.hotColor,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // ── FORE-EDGE STRIP ────────────────────────────────────────────
                // A thick Canvas strip representing the visible stacked page leaves
                // on the right side of a closed book. 80 horizontal lines drawn in
                // alternating light and slightly-darker shades give the impression
                // of individual pages with real depth. The strip is deliberately
                // wider (28 dp) than the spine so the page-bulk reads clearly.
                Canvas(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight()
                        .clip(
                            RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = 8.dp,
                                bottomEnd = 8.dp
                            )
                        )
                        .background(Color(0xFF0E0E0E))
                ) {
                    val lineCount = 80
                    val spacing = size.height / lineCount

                    for (i in 0 until lineCount) {
                        val y = i * spacing + spacing * 0.5f

                        // Alternate between two shades of cream/gold so individual
                        // pages are visually distinct — this is the key detail that
                        // makes the fore-edge read as stacked leaves rather than a
                        // flat stripe. Every 3rd line is slightly brighter to
                        // simulate the irregular tonal variation of real paper edges.
                        val lineAlpha = when {
                            i % 3 == 0 -> 0.65f   // brighter "top-facing" page edge
                            i % 2 == 0 -> 0.40f   // mid shade
                            else       -> 0.22f   // darker gap between pages
                        }

                        drawLine(
                            color = lore.theme.accentColor.copy(alpha = lineAlpha),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = if (i % 3 == 0) 1.5f else 0.8f
                        )
                    }
                }
            }

            // ── SOFT DROP SHADOW ──────────────────────────────────────────────
            // A subtle radial smear beneath the book that pulses with the
            // breathing glow — grounds the book visually in the scene.
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(20.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                lore.theme.accentColor.copy(alpha = breathingAlpha * 0.28f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── "TAP TO OPEN" HINT ────────────────────────────────────────────
            // Fades away once opening begins so it never overlaps the flash.
            Text(
                text = "TAP THE BOOK TO BEGIN",
                color = ColorPageText.copy(alpha = 0.55f * (1f - openProgress)),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )
        }

        // ── OPENING FLASH OVERLAY ─────────────────────────────────────────────
        // A full-screen radial burst of the rank's hot colour, timed to the
        // midpoint of the cover's rotation. Drawn above the book itself.
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                lore.theme.hotColor.copy(alpha = flashAlpha * 0.85f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STORY READER — paged lore content with true page-curl effect
// ─────────────────────────────────────────────────────────────────────────────

/**
 * StoryReader — the swipeable book interior: a title page, one page per lore
 * paragraph, and a final closing page with the "CLOSE THE BOOK" button.
 *
 * Page-curl implementation:
 *   HorizontalPager physically positions pages at horizontal offsets (page N+1
 *   starts one screen-width to the right of page N). To produce a true page-curl
 *   instead of a flat slide, we cancel that translation inside graphicsLayer and
 *   replace it with a rotationY pivot on the correct edge:
 *     • Leaving page (offset < 0): pivot on the RIGHT edge, swing 0° → -90°.
 *     • Arriving page (offset > 0): pivot on the LEFT edge, swing +90° → 0°.
 *   This means each page appears to stay in place while curling away (or into)
 *   position, matching the motion of physically turning a book leaf.
 *
 * @param lore         This rank's lore + theme.
 * @param rank         The achieved Rank (used for the title page subtitle).
 * @param onCloseBook  Called when the user taps "CLOSE THE BOOK" on the final page.
 */
@Composable
private fun StoryReader(lore: AscensionLore, rank: Rank, onCloseBook: () -> Unit) {
    // Page 0            → title page (book title + opening line)
    // Pages 1..N         → one page per lore.pages entry
    // Final page (N + 1) → closing line + CLOSE THE BOOK button
    val totalPages = lore.pages.size + 2
    val pagerState = rememberPagerState(pageCount = { totalPages })

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->

                // ── PAGE CURL GRAPHICSLAYER ────────────────────────────────────
                // pageOffset: signed distance of this page from the currently
                // snapped page, including the live drag fraction.
                //   0f  = this is the current page (no rotation).
                //   +1f = this page is one page to the RIGHT of the current page.
                //   -1f = this page is one page to the LEFT of the current page.
                val pageOffset = (pageIndex - pagerState.currentPage).toFloat() -
                        pagerState.currentPageOffsetFraction
                val absOffset = abs(pageOffset)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Cancel the pager's natural horizontal scroll so the
                            // page appears to stay in the same screen position.
                            // Without this, the rotation is applied ON TOP of the
                            // slide, which looks like a twisted card, not a page turn.
                            translationX = -pageOffset * size.width

                            // Rotate around the correct edge depending on which
                            // side of the current page this page lives on.
                            if (pageOffset < 0f) {
                                // This page is leaving (was the current page, now
                                // sweeping to the left as the user swipes forward).
                                // Pivot on the RIGHT edge so it folds away to the left.
                                transformOrigin = TransformOrigin(1f, 0.5f)
                                // pageOffset goes from 0 to -1 as the page leaves.
                                // rotationY goes from 0° to +90° (swings right edge back).
                                rotationY = pageOffset * -90f
                            } else {
                                // This page is arriving (next page, coming in from the
                                // right as the user swipes forward).
                                // Pivot on the LEFT edge so it unfolds from the right.
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                // pageOffset goes from +1 to 0 as the page arrives.
                                // rotationY goes from +90° to 0° (unfolds into view).
                                rotationY = pageOffset * 90f
                            }

                            // Increased camera distance prevents the 3D fold from
                            // looking pinched or distorted at steep rotation angles.
                            cameraDistance = 24 * density

                            // Darken pages as they swing away — the page in mid-fold
                            // would catch less light; this reinforces the 3D read.
                            alpha = 1f - (absOffset * 0.3f).coerceIn(0f, 1f)
                        }
                        // Warm dark parchment background so each page reads as a
                        // physical object rather than floating text on the void.
                        .background(ColorPageBackground)
                        .padding(horizontal = 32.dp, vertical = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (pageIndex) {
                        // ── TITLE PAGE ─────────────────────────────────────────
                        0 -> TitlePage(lore = lore, rank = rank)

                        // ── CLOSING PAGE ───────────────────────────────────────
                        totalPages - 1 -> ClosingPage(
                            lore = lore,
                            onCloseBook = onCloseBook
                        )

                        // ── STORY PAGES ────────────────────────────────────────
                        else -> StoryPage(
                            lore = lore,
                            paragraph = lore.pages[pageIndex - 1],
                            pageNumber = pageIndex
                        )
                    }
                }
            }
        }

        // ── PAGE INDICATOR DOTS ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalArrangement = spacedByCenter(6.dp)
        ) {
            repeat(totalPages) { index ->
                val isActive = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isActive) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) lore.theme.accentColor
                            else lore.theme.accentColor.copy(alpha = 0.25f)
                        )
                )
            }
        }
    }
}

// A tiny local helper — Arrangement.spacedBy() alone does not centre the row,
// and Arrangement has no built-in "spacedBy + centred" variant. Rather than
// wrap every call site in an extra Box, this composes the two behaviours.
// NOTE: this is a plain top-level function, NOT an extension on Arrangement.Companion —
// Arrangement is a Kotlin `object`, not a class, so it has no Companion to extend
// (that mistake caused "Unresolved reference 'Companion'" at build time).
private fun spacedByCenter(space: Dp): Arrangement.Horizontal =
    Arrangement.spacedBy(space, Alignment.CenterHorizontally)

/**
 * TitlePage — the first page of the book: title, ornamental divider, opening line.
 */
@Composable
private fun TitlePage(lore: AscensionLore, rank: Rank) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = lore.theme.sigil,
            fontSize = 40.sp,
            color = lore.theme.accentColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = lore.bookTitle,
            color = lore.theme.hotColor,
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "A CHAPTER FOR ${rank.title.uppercase()}",
            color = lore.theme.accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        OrnamentalDivider(color = lore.theme.accentColor)

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = lore.openingLine,
            color = ColorPageText,
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "swipe to continue",
            color = ColorPageText.copy(alpha = 0.4f),
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )
    }
}

/**
 * StoryPage — a single page of lore body text, with a drop-cap first letter.
 *
 * @param lore        This rank's theme (for the drop-cap and page-number colour).
 * @param paragraph   The page's text. May contain "\n\n" for internal sub-paragraphs.
 * @param pageNumber  1-based index into lore.pages, shown as a small folio number.
 */
@Composable
private fun StoryPage(lore: AscensionLore, paragraph: String, pageNumber: Int) {
    val firstChar = paragraph.firstOrNull()?.toString() ?: ""
    val rest = if (paragraph.isNotEmpty()) paragraph.substring(1) else ""

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // ── DROP CAP + BODY ────────────────────────────────────────────────────
        Row {
            Text(
                text = firstChar,
                color = lore.theme.hotColor,
                fontSize = 46.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = rest,
                color = ColorPageText,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                lineHeight = 25.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── FOLIO NUMBER ───────────────────────────────────────────────────────
        Text(
            text = "— $pageNumber —",
            color = lore.theme.accentColor.copy(alpha = 0.6f),
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )
    }
}

/**
 * ClosingPage — the final page: closing line, ornamental divider, and the
 * "CLOSE THE BOOK" button that ends the experience.
 *
 * @param lore         This rank's theme.
 * @param onCloseBook  Called when the button is tapped.
 */
@Composable
private fun ClosingPage(lore: AscensionLore, onCloseBook: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = lore.theme.sigil,
            fontSize = 40.sp,
            color = lore.theme.hotColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = lore.closingLine,
            color = ColorPageText,
            fontSize = 17.sp,
            fontFamily = FontFamily.Serif,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        OrnamentalDivider(color = lore.theme.accentColor)

        Spacer(modifier = Modifier.height(36.dp))

        // ── CLOSE THE BOOK BUTTON ──────────────────────────────────────────────
        // Uses the rank's own accent colour rather than the app's global gold,
        // so each rank's ending feels distinct — this is the last thing the
        // user sees before returning to the dashboard.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(lore.theme.accentColor)
                .clickable(onClick = onCloseBook)
                .padding(vertical = 14.dp)
        ) {
            Text(
                text = "CLOSE THE BOOK",
                color = Color(0xFF0A0A0A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

/**
 * OrnamentalDivider — a small horizontal line with a centred diamond, used to
 * separate sections on the title and closing pages. Purely decorative.
 *
 * @param color  The line/diamond colour — always the rank's accent colour.
 */
@Composable
private fun OrnamentalDivider(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(1.dp)
                .background(color.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer { rotationZ = 45f }
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(1.dp)
                .background(color.copy(alpha = 0.5f))
        )
    }
}
