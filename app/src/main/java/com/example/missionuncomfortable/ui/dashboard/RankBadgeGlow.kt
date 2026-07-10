/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        ⚠  AI ASSISTANT — READ THIS BEFORE EDITING THIS FILE  ⚠         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PROJECT: Mission: Uncomfortable — Android app (Kotlin + Jetpack Compose)
 *          Daily social-discomfort challenges. User accepts a mission → goes
 *          and does it → returns → rates discomfort (1–10) → earns XP → ranks up.
 *
 * ── COMMENTING RULES — NEVER break these ────────────────────────────────────
 *
 *   1. DO NOT remove any existing comment. If a comment is outdated, UPDATE it.
 *   2. ADD comments to every new block of code you write.
 *   3. COMMENT STYLE used across this entire codebase (stay consistent):
 *        • Section dividers  →  // ── SECTION NAME ─────────────────────────────
 *        • KDoc blocks       →  /** ... */ above every function, class, data class
 *        • Inline reasoning  →  // Why this decision was made (not just "what it does")
 *   4. CHANGELOG: every file has a  ─── CHANGELOG ───  block in its file header.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these rules, the developer will have to   ║
 * ║  manually fix every file you touch. Please respect these conventions.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * RankBadgeGlow.kt
 *
 * Shared "prestige glow" system drawn behind the rank badge — used on both the
 * Dashboard (RankBadgeSection) and the Rank-Up celebration screen (RankUpScreen).
 *
 * ─── WHY THIS FILE EXISTS ────────────────────────────────────────────────────
 *
 *   Previously, DashboardScreen.kt and RankUpScreen.kt each had their own
 *   duplicated, near-identical glow-drawing code, and every rank got the SAME
 *   visual treatment — just a bigger/brighter version of the same single ring.
 *   That reads as cheap: Sovereign should not look like "Initiate but brighter."
 *   It should look like a completely different tier of presentation.
 *
 *   This file factors the glow into ONE shared composable — RankBadgeGlow —
 *   with a genuinely escalating "prestige tier" system per rank level:
 *
 *     Level 1 — Observer:    no glow at all. Nothing has been earned.
 *     Level 2 — Initiate:    single soft ring, static. The first sign of light.
 *     Level 3 — Challenger:  dual-layer ring (soft + a brighter inner edge),
 *                            breathing pulse. Visibly more substantial.
 *     Level 4 — Conqueror:   dual-layer ring in a warmer/hotter gold, PLUS a
 *                            rotating shimmer sweep gliding around the ring —
 *                            the glow now visibly *moves*, not just breathes.
 *     Level 5 — Sovereign:   triple-layer ring (soft outer / hot inner / molten
 *                            near-white core), a faster twin shimmer sweep,
 *                            8 rotating radiant light rays reaching to the
 *                            container edge, AND a slow "living" breathing
 *                            scale on the whole badge+glow unit. This is the
 *                            flagship rank — it must look unmistakably regal.
 *
 *   Container size also scales with rank (more halo room at higher ranks), so
 *   the badge's presence on screen grows, not just its brightness.
 *
 * ─── HOW TO USE ──────────────────────────────────────────────────────────────
 *
 *   RankBadgeGlow(rank = someRank, badgeSize = 140.dp) {
 *       Image(...)   // the badge art, centred automatically inside the glow
 *   }
 *
 *   Pass any extra modifiers (clickable, graphicsLayer spring scale, etc.) via
 *   the `modifier` parameter — they compose with the internal size/glow/breathe
 *   modifiers exactly like a normal Box.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Extracted from DashboardScreen.kt v12 / RankUpScreen.kt v3's duplicated
 *        ring-gradient glow code into this shared file, and replaced the flat
 *        "same ring, different alpha" system with genuinely different per-rank
 *        visual tiers (multi-layer rings, rotating shimmer, radiant rays,
 *        breathing scale) as described above.
 *
 *   v2 — BUILD FIX: two import errors caused every InfiniteTransition.animateFloat()
 *        call and every StrokeCap reference to fail to compile.
 *          1. androidx.compose.animation.core.animateFloat (the InfiniteTransition
 *             extension function actually used at the call sites) was never imported —
 *             only the unrelated top-level `animateFloat` symbols were in scope, so the
 *             compiler couldn't resolve the call or infer its type parameter.
 *          2. StrokeCap was imported from androidx.compose.ui.graphics.drawscope, but it
 *             actually lives in androidx.compose.ui.graphics — the wrong package meant
 *             "Unresolved reference" everywhere StrokeCap.Round was used.
 *        No drawing/animation logic changed — imports only.
 *
 *   v3 — BUILD FIX: `val x by infiniteTransition.animateFloat(...)` failed with
 *        "State<Float> has no method getValue" because androidx.compose.runtime.getValue
 *        (the extension that makes `by` work on a Compose State) was never imported.
 *        Added the missing import. No drawing/animation logic changed.
 *
 *   v4 — VISUAL FIX: the Conqueror/Sovereign "shimmer sweep" was a rotating
 *        stroked arc with round caps — which looks exactly like Android's own
 *        indeterminate loading spinner, not a prestige effect (confirmed via a
 *        real on-device screenshot showing a "yellow snake" circling the badge).
 *        Replaced the arc-stroke shimmer with a small travelling radial "glint" —
 *        a soft glowing dot that orbits the ring like a gleam of light catching
 *        polished metal. See drawShimmerSweep's KDoc for full reasoning. Removed
 *        the now-unused Stroke import (StrokeCap is still used by the radiant rays).
 *
 *   v5 — BUILD FIX: the v4 rename of drawShimmerSweep's parameter (wedgeCount →
 *        glintCount) wasn't propagated to its call site in RankBadgeGlow's
 *        drawBehind block, causing "No parameter with name 'wedgeCount'". Updated
 *        the call site to pass glintCount. No behavior change.
 */

package com.example.missionuncomfortable.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat            // FIX: InfiniteTransition.animateFloat extension — was missing, caused "Unresolved reference" + type-inference errors on every animateFloat call
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue          // FIX: `by` delegate operator for State<Float> — without this, `val x by animateFloat(...)` fails with "has no method getValue"
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap             // FIX: StrokeCap lives in androidx.compose.ui.graphics, NOT .drawscope — wrong package was causing "Unresolved reference"
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default brand gold used across the whole glow system unless overridden. */
val RankGlowBaseColor = Color(0xFFC8A84B)

// ─── PRESTIGE TIER CONFIG ───────────────────────────────────────────────────

/**
 * RankGlowTier — the full set of visual parameters that define how dramatic a
 * given rank's glow looks. Each field maps to a specific visual layer drawn
 * inside RankBadgeGlow's drawBehind block below.
 *
 * @param haloPadding        Extra radius (each side) added around the badge to
 *                            form the glow container. Higher ranks get more —
 *                            the badge's "presence" on screen grows with rank.
 * @param alphaMin/alphaMax  Pulse range for the primary ring's opacity.
 * @param pulseDurationMs    Half-cycle duration of the breathing pulse.
 * @param pulses             False for levels 1-2 (static/none) — avoids wasting
 *                            animation frames on a value that never changes.
 * @param layerCount         How many concentric ring layers are drawn (1-3).
 *                            More layers = a denser, richer-looking halo.
 * @param hotColor           A warmer/brighter gold used for the inner layer(s)
 *                            on tiers 4-5, so the glow doesn't just get bigger —
 *                            it gets visibly hotter/richer in colour too.
 * @param sweepCount         Number of rotating shimmer wedges (0 = none).
 * @param sweepDurationMs    Full-revolution duration of the shimmer rotation.
 * @param rayCount           Number of radiant light rays (0 = none). Sovereign only.
 * @param breathes           Whether the whole badge+glow unit slowly scales
 *                            up/down (a "living" effect). Sovereign only.
 */
private data class RankGlowTier(
    val haloPadding: Dp,
    val alphaMin: Float,
    val alphaMax: Float,
    val pulseDurationMs: Int,
    val pulses: Boolean,
    val layerCount: Int,
    val hotColor: Color,
    val sweepCount: Int,
    val sweepDurationMs: Int,
    val rayCount: Int,
    val breathes: Boolean
)

/**
 * Returns the prestige tier config for a given rank level (1-5).
 * This is the SINGLE place that defines how each rank's glow escalates —
 * change intensities here, not inside the drawing code below.
 */
private fun rankGlowTier(level: Int): RankGlowTier = when (level) {
    1 -> RankGlowTier(   // Observer — no glow. Nothing earned yet.
        haloPadding = 15.dp, alphaMin = 0f, alphaMax = 0f, pulseDurationMs = 3000,
        pulses = false, layerCount = 0, hotColor = RankGlowBaseColor,
        sweepCount = 0, sweepDurationMs = 24000, rayCount = 0, breathes = false
    )
    2 -> RankGlowTier(   // Initiate — single soft static halo. First sign of light.
        haloPadding = 25.dp, alphaMin = 0.40f, alphaMax = 0.40f, pulseDurationMs = 3000,
        pulses = false, layerCount = 1, hotColor = RankGlowBaseColor,
        sweepCount = 0, sweepDurationMs = 24000, rayCount = 0, breathes = false
    )
    3 -> RankGlowTier(   // Challenger — dual-layer ring, slow breathing pulse.
        haloPadding = 35.dp, alphaMin = 0.35f, alphaMax = 0.70f, pulseDurationMs = 2200,
        pulses = true, layerCount = 2, hotColor = Color(0xFFDCB65C),
        sweepCount = 0, sweepDurationMs = 24000, rayCount = 0, breathes = false
    )
    4 -> RankGlowTier(   // Conqueror — dual-layer ring in hotter gold + rotating shimmer.
        haloPadding = 48.dp, alphaMin = 0.55f, alphaMax = 0.88f, pulseDurationMs = 1500,
        pulses = true, layerCount = 2, hotColor = Color(0xFFE8B84B),
        sweepCount = 1, sweepDurationMs = 5200, rayCount = 0, breathes = false
    )
    else -> RankGlowTier( // Sovereign — triple-layer molten ring, twin shimmer, 8 radiant rays, breathing.
        haloPadding = 70.dp, alphaMin = 0.72f, alphaMax = 1.00f, pulseDurationMs = 1050,
        pulses = true, layerCount = 3, hotColor = Color(0xFFFFDE85),
        sweepCount = 2, sweepDurationMs = 3400, rayCount = 8, breathes = true
    )
}

// ─── SHARED COMPOSABLE ───────────────────────────────────────────────────────

/**
 * RankBadgeGlow — wraps [content] (the badge Image) in a container that draws
 * the rank-appropriate prestige glow behind it via drawBehind.
 *
 * The container is centred on [content] and sized as (badgeSize + tier.haloPadding * 2),
 * so the glow always has room to extend beyond the badge without being clipped —
 * and that room grows with rank, so higher ranks visually command more space.
 *
 * @param rank      The user's current (or newly achieved) Rank — drives every
 *                  visual parameter via rankGlowTier(rank.level).
 * @param badgeSize The size of the badge Image that will be placed inside.
 *                  Callers pass their own value (140dp on the dashboard,
 *                  160dp on the celebration screen) — the glow geometry is
 *                  computed relative to this, so both callers automatically
 *                  get correctly-proportioned rings.
 * @param modifier  Extra modifiers applied to the outer Box — e.g. a caller's
 *                  own .clickable{} (dashboard secret-tap zone) or .graphicsLayer
 *                  spring-scale (rank-up entrance animation). These compose
 *                  naturally with this composable's own size/breathe/drawBehind.
 * @param content   The badge Image (or placeholder), centred inside the glow.
 */
@Composable
fun RankBadgeGlow(
    rank: Rank,
    badgeSize: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tier = rankGlowTier(rank.level)
    val containerSize = badgeSize + tier.haloPadding * 2

    // ── SINGLE SHARED infiniteTransition drives all animations for this badge ──
    // (breathing pulse, rotating shimmer sweep, rotating radiant rays). Using one
    // transition instead of three keeps the animation clock in sync and is cheaper.
    val infiniteTransition = rememberInfiniteTransition(label = "RankBadgeGlow")

    // Breathing pulse — alpha oscillates for tiers that pulse (levels 3-5);
    // tiers that don't pulse (levels 1-2) get a constant value with zero cost.
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = tier.alphaMin,
        targetValue = tier.alphaMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = tier.pulseDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RankBadgeGlowAlpha"
    )
    val effectiveAlpha = if (tier.pulses) animatedAlpha else tier.alphaMin

    // Rotating shimmer sweep angle (Conqueror + Sovereign). Restart (not Reverse) —
    // a shimmer should travel continuously in one direction, like light passing over metal.
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = tier.sweepDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RankBadgeGlowSweep"
    )

    // Rotating radiant rays angle (Sovereign only) — deliberately slower and
    // independent of the shimmer sweep, so the two motions don't look synced/mechanical.
    val rayAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RankBadgeGlowRays"
    )

    // "Living" breathing scale on the whole badge+glow unit (Sovereign only) —
    // a slow, subtle 1.0 → 1.035 → 1.0 scale so the apex rank feels alive, not static.
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (tier.breathes) 1.035f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RankBadgeGlowBreathe"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(containerSize)
            .graphicsLayer {
                // Breathing scale applies to the container (glow + badge together) —
                // for non-breathing tiers this is a no-op 1.0 scale.
                scaleX = breathingScale
                scaleY = breathingScale
            }
            .drawBehind {
                if (tier.layerCount == 0) return@drawBehind   // Observer: no glow at all

                // Fraction of the container radius occupied by the badge itself.
                // This is where the ring's peak brightness must sit — just outside
                // this value — so the glow is never hidden under the badge image.
                val edgeStop = (badgeSize / containerSize).coerceIn(0.1f, 0.95f)

                // ── LAYER 1 — soft, wide outer ring (always present when glowing) ──
                drawRingGlow(edgeStop = edgeStop, peakOffset = 0.05f, spread = 0.16f, color = RankGlowBaseColor, alpha = effectiveAlpha)

                // ── LAYER 2 — tighter, hotter inner ring (Challenger and above) ──
                if (tier.layerCount >= 2) {
                    drawRingGlow(edgeStop = edgeStop, peakOffset = 0.02f, spread = 0.07f, color = tier.hotColor, alpha = (effectiveAlpha * 1.05f).coerceAtMost(1f))
                }

                // ── LAYER 3 — molten near-white core ring (Sovereign only) ──
                if (tier.layerCount >= 3) {
                    drawRingGlow(edgeStop = edgeStop, peakOffset = 0.006f, spread = 0.035f, color = Color(0xFFFFF6E0), alpha = (effectiveAlpha * 0.95f).coerceAtMost(1f))
                }

                // ── ROTATING SHIMMER SWEEP (Conqueror: 1 wedge, Sovereign: 2 opposite wedges) ──
                // A bright gleam travelling around the ring — the glow now visibly MOVES,
                // not just breathes. This is what separates Conqueror/Sovereign from the
                // lower ranks' static-feeling halos.
                if (tier.sweepCount > 0) {
                    drawShimmerSweep(edgeStop = edgeStop, rotationDeg = sweepAngle, glintCount = tier.sweepCount, color = tier.hotColor, alpha = effectiveAlpha)
                }

                // ── ROTATING RADIANT RAYS (Sovereign only) ──
                // 8 thin beams of light reaching from just outside the ring to the
                // container's edge, slowly rotating — the "crown" of the whole system.
                if (tier.rayCount > 0) {
                    drawRadiantRays(edgeStop = edgeStop, rotationDeg = rayAngle, rayCount = tier.rayCount, color = RankGlowBaseColor, alpha = effectiveAlpha)
                }
            },
        content = content
    )
}

// ─── DRAWING HELPERS ─────────────────────────────────────────────────────────

/**
 * Draws a single RING gradient layer — NOT a filled circle.
 *
 * Why a ring and not a simple centre→transparent fade: the badge Image is
 * drawn ON TOP of this glow and physically covers the centre of the container.
 * A gradient that peaks at the centre wastes all its brightness where nobody
 * can see it, leaving only the already-faded outer rim visible (this was the
 * original bug in DashboardScreen.kt v11 / RankUpScreen.kt v2). Instead, this
 * draws a ring whose peak brightness sits just OUTSIDE the badge's edge —
 * the only place the glow can actually be seen — then fades back to
 * transparent by the container's outer edge.
 *
 * @param edgeStop    Fraction of the container radius where the badge's own
 *                    edge sits (badgeSize / containerSize). The ring's peak
 *                    is placed just past this point.
 * @param peakOffset  How far past edgeStop the peak sits. Smaller = a tighter,
 *                    sharper ring hugging the badge; larger = a softer, more
 *                    spread-out glow.
 * @param spread      How far past the peak the ring fades back to transparent.
 *                    Wider layers (spread) look softer/hazier; narrower layers
 *                    look sharper/hotter — used to build up depth across layers.
 */
private fun DrawScope.drawRingGlow(edgeStop: Float, peakOffset: Float, spread: Float, color: Color, alpha: Float) {
    val peak = (edgeStop + peakOffset).coerceIn(0.01f, 0.97f)
    val fadeEnd = (peak + spread).coerceIn(peak + 0.01f, 1f)
    // Stop just before the peak must stay transparent and strictly less than peak,
    // so the gradient is fully invisible under the badge and only blooms past its edge.
    val preStop = (peak - 0.001f).coerceAtLeast(0f)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color.Transparent,
                preStop to Color.Transparent,
                peak to color.copy(alpha = alpha),
                fadeEnd to Color.Transparent
            ),
            radius = size.minDimension / 2f
        )
    )
}

/**
 * Draws [glintCount] rotating bright "gleam" dots around the ring using
 * Brush.sweepGradient, rotated continuously by [rotationDeg]. This is what
 * makes Conqueror/Sovereign's glow feel alive and in motion rather than a
 * static painted halo — a highlight is always sliding around the ring, like
 * light catching polished metal.
 *
 * glintCount = 2 places the glints on opposite sides of the ring (180° apart)
 * for Sovereign's denser, busier shimmer.
 *
 * v4 FIX: this used to be a rotating STROKED ARC with round caps (Stroke +
 * drawArc). That shape — a thick rounded-cap band sweeping around a circle —
 * is visually IDENTICAL to Android's own indeterminate CircularProgressIndicator
 * ("loading spinner"), which is exactly the wrong association for a "very high
 * rank" badge (a screenshot showed it reading as a yellow snake/loading spinner,
 * not a prestige effect). Also, Brush.sweepGradient always maps its colour stops
 * across the FULL 360° regardless of the arc's own startAngle/sweepAngle window,
 * so the arc's visible colour didn't line up with its rotation the way intended —
 * it rendered as a flat, hard-edged pill instead of a soft gradient sweep.
 * Replaced with a small travelling radial "glint" — a soft glowing dot that
 * orbits the ring, like a gleam of light catching polished metal — which reads
 * as a specular highlight, not a loading indicator.
 */
private fun DrawScope.drawShimmerSweep(edgeStop: Float, rotationDeg: Float, glintCount: Int, color: Color, alpha: Float) {
    // Ring radius the glint orbits along — matches where the hot inner ring layer peaks.
    val ringRadius = size.minDimension / 2f * (edgeStop + 0.03f).coerceAtMost(0.95f)
    // Soft glow blob size — big enough to read as a gleam, small enough to stay a highlight,
    // not a second badge-sized blob.
    val glintRadius = size.minDimension * 0.10f
    val step = 360f / glintCount
    for (i in 0 until glintCount) {
        val angleRad = Math.toRadians((rotationDeg + i * step).toDouble())
        // Point on the ring at the current rotation angle — this IS the travelling gleam's position.
        val point = Offset(
            x = center.x + ringRadius * kotlin.math.cos(angleRad).toFloat(),
            y = center.y + ringRadius * kotlin.math.sin(angleRad).toFloat()
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    color.copy(alpha = alpha * 0.35f),
                    Color.Transparent
                ),
                center = point,
                radius = glintRadius
            ),
            radius = glintRadius,
            center = point
        )
    }
}

/**
 * Draws [rayCount] thin radiant light beams from just outside the badge's ring
 * out to the container's edge, evenly spaced and rotated by [rotationDeg].
 * Reserved for Sovereign — this is the single most distinctive visual marker
 * of the apex rank, evoking a crown radiating light rather than just glowing.
 */
private fun DrawScope.drawRadiantRays(edgeStop: Float, rotationDeg: Float, rayCount: Int, color: Color, alpha: Float) {
    val innerRadius = size.minDimension / 2f * (edgeStop + 0.09f).coerceAtMost(0.9f)
    val outerRadius = size.minDimension / 2f * 0.99f
    val rayWidth = size.minDimension * 0.012f
    rotate(degrees = rotationDeg, pivot = center) {
        for (i in 0 until rayCount) {
            rotate(degrees = i * (360f / rayCount), pivot = center) {
                val start = Offset(center.x, center.y - innerRadius)
                val end = Offset(center.x, center.y - outerRadius)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                        start = start,
                        end = end
                    ),
                    start = start,
                    end = end,
                    strokeWidth = rayWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
