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
 *   CLOSED   → A closed book sits centered in darkness. Three strips: dark spine
 *              on the left, sigil/title cover in the middle, thick fore-edge of
 *              visible page lines on the right. Ember particles drift upward.
 *              Tapping the book begins OPENING.
 *
 *   OPENING  → The cover swings open on its spine (left-edge Y-axis hinge). A
 *              bright flash marks the moment it falls fully open.
 *
 *   READING  → The book stays as a visible PHYSICAL OBJECT on the dark background —
 *              same size and position as the closed book, now showing its open
 *              interior. A spine strip remains on the left. Pages are warm parchment.
 *              Swiping triggers a true page-curl: the leaving page folds back around
 *              its right edge, the arriving page unfolds from its left edge, both
 *              rotating in place (pager translation cancelled) so it looks like
 *              physically turning a leaf, not sliding a card.
 *
 *   CLOSING  → Tapping "CLOSE THE BOOK" fades the whole scene to black, then
 *              calls onFinish().
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
 *   v3 — Thicker fore-edge, page backgrounds, page-curl with translation cancel.
 *   v4 — Reading state is now a constrained book object on the dark background
 *        (same visual weight as the closed book). Pages are warm aged parchment.
 *        Page-curl fixed: each page rotates in-place around the correct edge with
 *        a drop-shadow between pages to reinforce depth. Book spine stays visible
 *        throughout reading.
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
import androidx.compose.ui.draw.shadow
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
// SHARED COLOUR CONSTANTS
// ─────────────────────────────────────────────────────────────────────────────

// Near-black void — the base of every book scene, regardless of rank.
private val ColorVoid = Color(0xFF060606)

// Off-white used for body text on every page.
private val ColorPageText = Color(0xFFE7E2D8)

// Warm aged-parchment background for open book pages — dark enough to feel
// atmospheric but clearly distinct from the surrounding void so each page
// reads as a physical object, not floating text on blackness.
private val ColorPageParchment = Color(0xFF1E1A14)

// ─────────────────────────────────────────────────────────────────────────────
// STAGE STATE MACHINE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * BookStage — the four states of the Ascension Book experience.
 */
private enum class BookStage {
    CLOSED,   // Waiting for the user to tap the book
    OPENING,  // Cover-open animation is playing
    READING,  // Story pager is visible
    CLOSING   // Fade-out before returning to Dashboard
}

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AscensionBookScreen — the full-screen storybook shown on a rank promotion.
 *
 * @param rank      The Rank the user just achieved.
 * @param onFinish  Called once the closing fade completes.
 */
@Composable
fun AscensionBookScreen(rank: Rank, onFinish: () -> Unit) {
    val lore = remember(rank.level) { getLoreForRank(rank.level) }

    var stage by remember { mutableStateOf(BookStage.CLOSED) }

    val screenAlpha by animateFloatAsState(
        targetValue = if (stage == BookStage.CLOSING) 0f else 1f,
        animationSpec = tween(durationMillis = 550),
        label = "AscensionScreenFade"
    )

    LaunchedEffect(stage) {
        if (stage == BookStage.CLOSING) {
            delay(600)
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha }
            .background(
                Brush.radialGradient(
                    colors = listOf(lore.theme.mistColor, ColorVoid),
                    radius = 1200f
                )
            )
    ) {
        EmberParticles(color = lore.theme.hotColor)

        when (stage) {
            BookStage.CLOSED, BookStage.OPENING -> {
                ClosedBookScene(
                    lore = lore,
                    isOpening = stage == BookStage.OPENING,
                    onOpenAnimationComplete = { stage = BookStage.READING },
                    onTap = { if (stage == BookStage.CLOSED) stage = BookStage.OPENING }
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
                // Let the screenAlpha fade handle the visual — nothing new to draw.
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMBER PARTICLES
// ─────────────────────────────────────────────────────────────────────────────

/**
 * EmberParticles — slowly rising light motes in [color]. Decorative only.
 */
@Composable
private fun EmberParticles(color: Color) {
    val particleCount = 22
    val particles = remember {
        val rng = Random(seed = 42)
        List(particleCount) {
            Triple(rng.nextFloat(), rng.nextFloat(), 1.5.dp + (rng.nextFloat() * 2.5).dp)
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "EmberParticlesTransition")
    val cycle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
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
            val local = (cycle + phase) % 1f
            val y = h * (1f - local) + h * 0.15f
            val x = w * xFraction + sin(local * Math.PI.toFloat() * 2f) * 14f
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
 * ClosedBookScene — the closed book with breathing glow and spine-hinge open animation.
 *
 * Three strips in a Row: spine (left, dark gradient) | cover face (sigil + title) |
 * fore-edge (Canvas of 80 lines simulating stacked page leaves, right side).
 * Rotation is rotationY with TransformOrigin(0f, 0.5f) — pivots on the left/spine edge.
 */
@Composable
private fun ClosedBookScene(
    lore: AscensionLore,
    isOpening: Boolean,
    onOpenAnimationComplete: () -> Unit,
    onTap: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ClosedBookGlow")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ClosedBookGlowAlpha"
    )

    // rotationY 0 → -90: cover swings away from the spine, left edge stays fixed.
    val openRotation by animateFloatAsState(
        targetValue = if (isOpening) -90f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "BookOpenRotation",
        finishedListener = { if (isOpening) onOpenAnimationComplete() }
    )

    val openProgress = (openRotation / -90f).coerceIn(0f, 1f)

    // Brief radial flash at the midpoint of the open swing.
    val flashAlpha = when {
        openProgress < 0.35f -> 0f
        openProgress < 0.65f -> (openProgress - 0.35f) / 0.30f
        openProgress < 1f    -> (1f - (openProgress - 0.65f) / 0.35f)
        else -> 0f
    }.coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ── THE BOOK ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        rotationY = openRotation
                        cameraDistance = 14 * density
                        transformOrigin = TransformOrigin(0f, 0.5f)   // spine hinge
                        alpha = 1f - (openProgress * 0.85f)
                    }
                    .height(300.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(10.dp))
                    .clickable(enabled = !isOpening, onClick = onTap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spine
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF020202), Color(0xFF181818))
                            )
                        )
                )

                // Cover face
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(lore.theme.mistColor, Color(0xFF0A0A0A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
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
                        Text(text = lore.theme.sigil, fontSize = 44.sp, color = lore.theme.accentColor)
                        Spacer(modifier = Modifier.height(18.dp))
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

                // Fore-edge — 80 lines in alternating shades to simulate page depth.
                Canvas(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        .background(Color(0xFF0E0E0E))
                ) {
                    val lineCount = 80
                    val spacing = size.height / lineCount
                    for (i in 0 until lineCount) {
                        val y = i * spacing + spacing * 0.5f
                        val lineAlpha = when {
                            i % 3 == 0 -> 0.65f
                            i % 2 == 0 -> 0.40f
                            else       -> 0.22f
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

            // Breathing drop shadow beneath the book.
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

            Text(
                text = "TAP THE BOOK TO BEGIN",
                color = ColorPageText.copy(alpha = 0.55f * (1f - openProgress)),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )
        }

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
// STORY READER — open book object with page-curl
// ─────────────────────────────────────────────────────────────────────────────

/**
 * StoryReader — the open book interior, shown as a PHYSICAL BOOK OBJECT centered
 * on the dark void background — same visual weight as the closed book.
 *
 * Layout:
 *   The book is a fixed Row: spine strip (left) + HorizontalPager (page area).
 *   The whole Row is the same height as the closed book (300 dp) and centered on screen.
 *   Below it: page-indicator dots.
 *
 * Page-curl:
 *   HorizontalPager naturally positions each page at `pageOffset * pageWidth` pixels
 *   from center. We cancel that translation with `translationX = -pageOffset * size.width`
 *   so every page occupies the same screen position. Then we rotate each page around
 *   the correct edge:
 *     • Leaving page (offset < 0): pivots on RIGHT edge, rotates 0° → +90°.
 *     • Arriving page (offset > 0): pivots on LEFT edge, rotates +90° → 0°.
 *   This is exactly how a physical book page turns — the leaf stays bounded by the
 *   spine, swings around its free edge, and lands flat on the other side.
 *
 * @param lore         This rank's lore + theme.
 * @param rank         The achieved Rank.
 * @param onCloseBook  Called when the user taps "CLOSE THE BOOK".
 */
@Composable
private fun StoryReader(lore: AscensionLore, rank: Rank, onCloseBook: () -> Unit) {
    val totalPages = lore.pages.size + 2
    val pagerState = rememberPagerState(pageCount = { totalPages })

    // Center the open book on the dark background, just like the closed book.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ── THE OPEN BOOK ─────────────────────────────────────────────────
            // Same 300 dp height as the closed book. Width: spine (18dp) + page area.
            Row(
                modifier = Modifier
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(10.dp))
                    .height(300.dp)
                    .width(280.dp),   // 18dp spine + 262dp page area
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── OPEN SPINE ────────────────────────────────────────────────
                // The spine stays visible throughout reading, anchoring the object
                // as a physical book rather than a floating page.
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF020202), Color(0xFF2A2218))
                            )
                        )
                )

                // ── PAGE AREA ─────────────────────────────────────────────────
                // HorizontalPager fills the remaining width. Each page is clipped
                // to this box so rotating pages don't bleed outside the book boundary.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->

                        // pageOffset: position of this page relative to the snapped
                        // current page, including live drag fraction.
                        //   0f  = this IS the current page.
                        //   +1f = one page to the RIGHT (arriving next).
                        //   -1f = one page to the LEFT (already passed).
                        val pageOffset = (pageIndex - pagerState.currentPage).toFloat() -
                                pagerState.currentPageOffsetFraction
                        val absOffset = abs(pageOffset)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // Cancel the pager's physical horizontal scroll so
                                    // this page stays fixed in the page-area bounds.
                                    // Without this the page would slide AND rotate,
                                    // which looks like a twisted card, not a book turn.
                                    translationX = -pageOffset * size.width

                                    if (pageOffset <= 0f) {
                                        // This page is the current page or is leaving.
                                        // Pivot on the RIGHT edge — the free (fore) edge
                                        // of a page as it turns away from the reader.
                                        transformOrigin = TransformOrigin(1f, 0.5f)
                                        // pageOffset goes 0 → -1 as the page leaves.
                                        // rotationY 0° → +90° (right edge swings back).
                                        rotationY = pageOffset * -90f
                                    } else {
                                        // This page is arriving from the right.
                                        // Pivot on the LEFT edge — the spine-side edge
                                        // of the incoming page as it unfolds into view.
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        // pageOffset goes +1 → 0 as the page arrives.
                                        // rotationY +90° → 0° (unfolds into flat view).
                                        rotationY = pageOffset * 90f
                                    }

                                    // Higher camera distance = less extreme perspective
                                    // distortion at steep angles mid-turn.
                                    cameraDistance = 24 * density

                                    // Pages darken as they rotate mid-turn — simulates
                                    // the reduced light hitting the angled leaf surface.
                                    alpha = 1f - (absOffset * 0.35f).coerceIn(0f, 1f)
                                }
                                // Warm aged-parchment background makes each page a
                                // clearly physical object against the surrounding void.
                                .background(ColorPageParchment),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (pageIndex) {
                                    0            -> TitlePage(lore = lore, rank = rank)
                                    totalPages-1 -> ClosingPage(lore = lore, onCloseBook = onCloseBook)
                                    else         -> StoryPage(
                                        lore = lore,
                                        paragraph = lore.pages[pageIndex - 1],
                                        pageNumber = pageIndex
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── PAGE INDICATOR DOTS ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
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
}

// Centres a spacedBy row — Arrangement has no built-in "spacedBy + centred" variant.
// Plain top-level function to avoid the Companion-extension compile error.
private fun spacedByCenter(space: Dp): Arrangement.Horizontal =
    Arrangement.spacedBy(space, Alignment.CenterHorizontally)

// ─────────────────────────────────────────────────────────────────────────────
// PAGE COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

/**
 * TitlePage — sigil, book title, rank subtitle, ornamental divider, opening line.
 */
@Composable
private fun TitlePage(lore: AscensionLore, rank: Rank) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = lore.theme.sigil, fontSize = 32.sp, color = lore.theme.accentColor)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = lore.bookTitle,
            color = lore.theme.hotColor,
            fontSize = 20.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "A CHAPTER FOR ${rank.title.uppercase()}",
            color = lore.theme.accentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        OrnamentalDivider(color = lore.theme.accentColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = lore.openingLine,
            color = ColorPageText,
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "swipe to continue",
            color = ColorPageText.copy(alpha = 0.4f),
            fontSize = 9.sp,
            letterSpacing = 2.sp
        )
    }
}

/**
 * StoryPage — lore body text with a drop-cap first letter and a folio number.
 *
 * @param lore        Rank theme (drop-cap colour, folio colour).
 * @param paragraph   Page text; may contain "\n\n" for sub-paragraphs.
 * @param pageNumber  1-based index shown as a small folio marker.
 */
@Composable
private fun StoryPage(lore: AscensionLore, paragraph: String, pageNumber: Int) {
    val firstChar = paragraph.firstOrNull()?.toString() ?: ""
    val rest = if (paragraph.isNotEmpty()) paragraph.substring(1) else ""

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text(
                text = firstChar,
                color = lore.theme.hotColor,
                fontSize = 36.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 3.dp)
            )
            Text(
                text = rest,
                color = ColorPageText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "— $pageNumber —",
            color = lore.theme.accentColor.copy(alpha = 0.6f),
            fontSize = 9.sp,
            letterSpacing = 2.sp
        )
    }
}

/**
 * ClosingPage — closing line, ornamental divider, and "CLOSE THE BOOK" button.
 *
 * @param lore         Rank theme.
 * @param onCloseBook  Called when the button is tapped.
 */
@Composable
private fun ClosingPage(lore: AscensionLore, onCloseBook: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = lore.theme.sigil, fontSize = 32.sp, color = lore.theme.hotColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = lore.closingLine,
            color = ColorPageText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        OrnamentalDivider(color = lore.theme.accentColor)
        Spacer(modifier = Modifier.height(20.dp))
        // CLOSE THE BOOK button — rank's accent colour, last thing the user sees.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(lore.theme.accentColor)
                .clickable(onClick = onCloseBook)
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = "CLOSE THE BOOK",
                color = Color(0xFF0A0A0A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

/**
 * OrnamentalDivider — a small decorative rule with a centred diamond.
 *
 * @param color  Always the rank's accent colour.
 */
@Composable
private fun OrnamentalDivider(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.width(40.dp).height(1.dp).background(color.copy(alpha = 0.5f)))
        Box(
            modifier = Modifier
                .size(5.dp)
                .graphicsLayer { rotationZ = 45f }
                .background(color)
        )
        Box(modifier = Modifier.width(40.dp).height(1.dp).background(color.copy(alpha = 0.5f)))
    }
}
