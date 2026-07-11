/**
 * WelcomeScreen.kt  — v4  VISUAL OVERHAUL
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
 * ─── FILE LOCATION ───────────────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/WelcomeScreen.kt
 *
 * ─── PACKAGE ─────────────────────────────────────────────────────────────────
 */

package com.example.missionuncomfortable.ui.welcome

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// ─── COLOURS ─────────────────────────────────────────────────────────────────

private val ColorBackground    = Color(0xFF0D0D0D)
private val ColorTextPrimary   = Color(0xFFE0E0E0)
private val ColorTextSecondary = Color(0xFF8A8A8A)
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
    var showSigil  by remember { mutableStateOf(false) }
    var showTitle  by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showSigil  = true
        delay(350L); showTitle  = true
        delay(280L); showButton = true
    }

    val sigilAlpha  by animateFloatAsState(
        targetValue = if (showSigil)  1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing), label = "sigilAlpha"
    )
    val sigilSlide  by animateFloatAsState(
        targetValue = if (showSigil)  0f else 50f,
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

    // Outer diamond: slow clockwise rotation — full revolution in 20 s
    val outerDiamondAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "OuterDiamondAngle"
    )

    // Inner diamond: slow counter-clockwise rotation — full revolution in 14 s
    val innerDiamondAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "InnerDiamondAngle"
    )

    // Light rays: rotate with the outer diamond but at their own speed (8 s)
    val rayAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "RayAngle"
    )

    // Centre orb: breathing pulse — alpha oscillates between 0.55 and 1.0
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "OrbAlpha"
    )

    // Outer glow ring: slower breathing — 0.25 → 0.55
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "GlowAlpha"
    )

    // CTA button shimmer sweep
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

        // Atmospheric background: warm faint spotlight behind the sigil
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

            // ── ANIMATED SIGIL ────────────────────────────────────────────
            // Drawn 100% in Compose Canvas — no drawable XML at all.
            // Shape: diamond geometry (rotated squares), NOT a circle.
            // This cannot be confused with any rank badge.
            //
            // Visual layers (bottom to top):
            //   1. Wide soft glow circle behind everything
            //   2. 8 thin light rays rotating outward from centre
            //   3. Outer diamond (square rotated 45°), slow CW rotation
            //   4. Tick marks at the midpoints of each outer diamond side
            //   5. Inner diamond, slightly smaller, slow CCW rotation
            //   6. Four short cardinal lines from centre (static crosshair)
            //   7. Pulsing radial orb at the exact centre
            val sigilSize = 280.dp

            Box(
                modifier = Modifier
                    .size(sigilSize)
                    .graphicsLayer {
                        alpha = sigilAlpha
                        translationY = sigilSlide
                    }
                    .drawBehind {
                        val cx = center.x
                        val cy = center.y
                        val r  = size.minDimension / 2f

                        // ── 1. OUTER ATMOSPHERIC GLOW ──────────────────────
                        // A wide, very soft radial gradient — the sigil feels
                        // like it emanates light, not just sits in darkness.
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

                        // ── 2. RADIANT LIGHT RAYS ───────────────────────────
                        // 8 thin beams radiating outward from a inner ring,
                        // fading to transparent at the container edge.
                        // Rotated slowly so they feel alive.
                        rotate(degrees = rayAngle, pivot = center) {
                            val rayCount  = 8
                            val rayInner  = r * 0.30f   // Rays start here (just outside centre orb)
                            val rayOuter  = r * 0.82f   // Rays end here (near container edge)
                            val rayWidth  = r * 0.013f  // Thin — these are light, not bars
                            for (i in 0 until rayCount) {
                                val angleRad = Math.toRadians((i * (360.0 / rayCount))).toFloat()
                                val startX = cx + rayInner * cos(angleRad)
                                val startY = cy + rayInner * sin(angleRad)
                                val endX   = cx + rayOuter * cos(angleRad)
                                val endY   = cy + rayOuter * sin(angleRad)
                                drawLine(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            ColorAccentGold.copy(alpha = glowAlpha * 0.90f),
                                            Color.Transparent
                                        ),
                                        start = Offset(startX, startY),
                                        end   = Offset(endX, endY)
                                    ),
                                    start = Offset(startX, startY),
                                    end   = Offset(endX,   endY),
                                    strokeWidth = rayWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // ── 3. OUTER DIAMOND ─────────────────────────────────
                        // A square rotated 45° → diamond shape.
                        // Slowly rotating clockwise — organic, not mechanical.
                        // NOT a circle — visually distinct from every rank badge.
                        rotate(degrees = outerDiamondAngle + 45f, pivot = center) {
                            val half = r * 0.72f   // Half the diamond's diagonal
                            val path = Path().apply {
                                moveTo(cx, cy - half)       // Top
                                lineTo(cx + half, cy)       // Right
                                lineTo(cx, cy + half)       // Bottom
                                lineTo(cx - half, cy)       // Left
                                close()
                            }
                            drawPath(
                                path = path,
                                color = ColorAccentGold.copy(alpha = 0.88f),
                                style = Stroke(width = r * 0.022f)
                            )
                            // Corner accent dots — small filled circles at each corner
                            val dotRadius = r * 0.028f
                            listOf(
                                Offset(cx, cy - half),
                                Offset(cx + half, cy),
                                Offset(cx, cy + half),
                                Offset(cx - half, cy)
                            ).forEach { pt ->
                                drawCircle(
                                    color = ColorHotGold.copy(alpha = 0.95f),
                                    radius = dotRadius,
                                    center = pt
                                )
                            }
                        }

                        // ── 4. OUTER DIAMOND MIDPOINT TICKS ─────────────────
                        // Short perpendicular tick lines at the midpoint of each
                        // outer diamond side — gives the diamond a refined,
                        // measured look (like a jeweller's cut mark).
                        rotate(degrees = outerDiamondAngle + 45f, pivot = center) {
                            val half   = r * 0.72f
                            val tickLen = r * 0.06f
                            // Midpoints of each side are at the diagonals
                            val mids = listOf(
                                Offset(cx + half * 0.5f, cy - half * 0.5f),  // Top-right side midpoint
                                Offset(cx + half * 0.5f, cy + half * 0.5f),  // Bottom-right side midpoint
                                Offset(cx - half * 0.5f, cy + half * 0.5f),  // Bottom-left side midpoint
                                Offset(cx - half * 0.5f, cy - half * 0.5f)   // Top-left side midpoint
                            )
                            // Tick direction is perpendicular to each side (45° rotated)
                            val dirs = listOf(
                                Offset(1f, 1f),   // NE side → tick goes NW-SE
                                Offset(-1f, 1f),  // SE side → tick goes NE-SW
                                Offset(-1f, -1f), // SW side → tick goes SE-NW
                                Offset(1f, -1f)   // NW side → tick goes SW-NE
                            )
                            for (i in mids.indices) {
                                val m   = mids[i]
                                val dir = dirs[i]
                                val norm = 0.7071f   // 1/sqrt(2) to normalize diagonal
                                val dx  = dir.x * norm * tickLen
                                val dy  = dir.y * norm * tickLen
                                drawLine(
                                    color = ColorAccentGold.copy(alpha = 0.65f),
                                    start = Offset(m.x - dx, m.y - dy),
                                    end   = Offset(m.x + dx, m.y + dy),
                                    strokeWidth = r * 0.016f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // ── 5. INNER DIAMOND ─────────────────────────────────
                        // Smaller diamond, rotating counter-clockwise.
                        // The counter-rotation against the outer diamond creates
                        // a sense of internal tension and depth.
                        rotate(degrees = innerDiamondAngle + 45f, pivot = center) {
                            val half = r * 0.44f
                            val path = Path().apply {
                                moveTo(cx, cy - half)
                                lineTo(cx + half, cy)
                                lineTo(cx, cy + half)
                                lineTo(cx - half, cy)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = ColorHotGold.copy(alpha = 0.72f),
                                style = Stroke(width = r * 0.016f)
                            )
                        }

                        // ── 6. STATIC CARDINAL CROSSHAIR LINES ───────────────
                        // Four short lines from just outside the inner orb to
                        // just inside the inner diamond — a static anchor in the
                        // middle of all the rotating geometry.
                        val crossLen   = r * 0.20f
                        val crossInner = r * 0.12f
                        listOf(
                            // Top
                            Offset(cx, cy - crossInner) to Offset(cx, cy - crossInner - crossLen),
                            // Bottom
                            Offset(cx, cy + crossInner) to Offset(cx, cy + crossInner + crossLen),
                            // Left
                            Offset(cx - crossInner, cy) to Offset(cx - crossInner - crossLen, cy),
                            // Right
                            Offset(cx + crossInner, cy) to Offset(cx + crossInner + crossLen, cy)
                        ).forEach { (s, e) ->
                            drawLine(
                                color = ColorAccentGold.copy(alpha = 0.80f),
                                start = s,
                                end   = e,
                                strokeWidth = r * 0.018f,
                                cap = StrokeCap.Round
                            )
                        }

                        // ── 7. CENTRE ORB ─────────────────────────────────────
                        // A small pulsing radial glow at the exact centre —
                        // the "locked target" point of the sigil.
                        // Three layers: outer soft halo, mid glow, solid bright core.
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ColorHotGold.copy(alpha = orbAlpha * 0.45f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = r * 0.18f
                            ),
                            radius = r * 0.18f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ColorHotGold.copy(alpha = orbAlpha * 0.80f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = r * 0.09f
                            ),
                            radius = r * 0.09f
                        )
                        drawCircle(
                            color = Color(0xFFFFF4CC).copy(alpha = (orbAlpha * 0.95f).coerceAtMost(1f)),
                            radius = r * 0.035f,
                            center = center
                        )
                    }
            )

            Spacer(modifier = Modifier.height(44.dp))

            // ── BRAND WORD ────────────────────────────────────────────────
            // One word. Maximum weight. That's it.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha        = titleAlpha
                    translationY = titleSlide
                }
            ) {
                Text(
                    text = "UNCOMFORTABLE",
                    color = ColorTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gold separator — thin horizontal accent line
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .graphicsLayer { alpha = buttonAlpha }
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorAccentGold)
                    .drawWithContent {
                        drawContent()
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
                    text = "BEGIN YOUR JOURNEY",
                    color = Color(0xFF0D0D0D),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── SINGLE FAINT TAGLINE ──────────────────────────────────────
            // One line. Very faint. Almost invisible. Sets the tone without
            // cluttering the screen with explanation.
            Text(
                text = "Discomfort is the curriculum.",
                color = ColorTextSecondary.copy(alpha = 0.40f),
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = buttonAlpha }
            )
        }
    }
}
