/**
 * WelcomeScreen.kt
 *
 * The first screen a brand-new user sees when they open Mission: Uncomfortable.
 *
 * ─── WHEN IS THIS SHOWN? ──────────────────────────────────────────────────────
 *
 *   MainActivity checks SharedPreferences for the key "has_seen_welcome" on every launch.
 *   If the key is missing (first install) or false, this screen is shown.
 *   When the user taps "BEGIN YOUR JOURNEY", MainActivity writes true to that key
 *   and switches to DashboardScreen. On every subsequent launch, this screen is skipped.
 *
 * ─── DESIGN AESTHETIC ─────────────────────────────────────────────────────────
 *
 *   Matches the Dashboard exactly:
 *     Background: near-black (#0D0D0D)
 *     Primary text: off-white (#E0E0E0)
 *     Secondary text: muted grey (#8A8A8A)
 *     Accent: muted gold (#C8A84B)
 *   No emojis. No colour outside the established palette.
 *
 * ─── FUTURE WORK ──────────────────────────────────────────────────────────────
 *
 *   - Replace the static screen with a short 2–3 step onboarding flow (Pager composable).
 *   - Add a name / username input so the ViewModel can personalise the Dashboard greeting.
 *   - Persist the onboarding flag to Room / DataStore instead of SharedPreferences once
 *     the database layer is set up.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial implementation.
 *        Single-screen welcome with badge image, title, tagline, and "BEGIN YOUR JOURNEY" button.
 *        Wired to MainActivity via the onStartJourney callback.
 *
 *   v2 — Replaced the rank_badge_placeholder with the dedicated emblem_welcome drawable.
 *        The welcome screen now has its own identity mark — a tactical targeting reticle —
 *        distinct from any rank badge. The reticle represents precision and purpose,
 *        not a user's current level.
 *
 *   v3 — Full visual overhaul: larger emblem, animated glow rings, staggered entrance,
 *        atmospheric background, button shimmer. Too much text removed.
 *
 *   v4 — Replaced emblem_welcome.xml with a custom canvas-drawn spinning diamond sigil.
 *        Removed "WELCOME TO THE" label. Cut text to minimum.
 *
 *   v5 — Reverted the spinning diamond back to emblem_welcome.xml.
 *        The rotating geometry was too aggressive compared to rank badge animations
 *        and distracted from the rest of the app's visual language.
 *        Animation is now calm: one soft gold glow ring that breathes slowly behind
 *        the emblem, nothing rotates or spins.
 *        Text simplified: replaced lone word "UNCOMFORTABLE" (confusing without context)
 *        with the full app name "Mission Uncomfortable" shown as a two-line label,
 *        matching the same LABEL → NAME pattern used for rank names on the Dashboard.
 *        Removed "Discomfort is the curriculum." tagline entirely — less is more.
 *        Removed all canvas drawing imports that are no longer needed (Path, Stroke,
 *        DrawScope, rotate, cos, sin).
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionuncomfortable.R
import kotlinx.coroutines.delay

// ─── COLOUR CONSTANTS ─────────────────────────────────────────────────────────
// Duplicated from DashboardScreen so WelcomeScreen is self-contained and portable.
// When a shared design-system file (e.g., Theme.kt or AppColors.kt) exists, move these there.

private val ColorBackground    = Color(0xFF0D0D0D)   // Near-black — main screen background
private val ColorTextPrimary   = Color(0xFFE0E0E0)   // Off-white — headings and primary text
private val ColorTextSecondary = Color(0xFF8A8A8A)   // Muted grey — supporting text
private val ColorAccentGold    = Color(0xFFC8A84B)   // Muted gold — CTA button + glow accent
private val ColorHotGold       = Color(0xFFDCB65C)   // Warmer gold — inner glow ring

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE — WelcomeScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * WelcomeScreen — the intro screen shown on first launch.
 *
 * Stateless: all it needs is a callback to call when the user taps "BEGIN YOUR JOURNEY".
 * MainActivity owns the SharedPreferences write + the navigation decision.
 *
 * @param onStartJourney  Called when the user taps the CTA button.
 *                        MainActivity should: (1) write has_seen_welcome=true to SharedPreferences,
 *                        (2) switch showDashboard state to true so DashboardScreen is shown.
 */
@Composable
fun WelcomeScreen(onStartJourney: () -> Unit) {

    // ── ENTRANCE ANIMATION FLAGS ─────────────────────────────────────────────
    // Three stages staggered so each element arrives one at a time.
    // Simple fade + gentle upward slide — nothing dramatic.
    var showEmblem by remember { mutableStateOf(false) }
    var showTitle  by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showEmblem = true                    // Emblem appears immediately on mount
        delay(400L); showTitle  = true       // App name fades in 400 ms later
        delay(300L); showButton = true       // Button fades in 300 ms after title
    }

    // animateFloatAsState converts each boolean flip into a smooth eased float.
    // alpha:  0f (invisible) → 1f (fully visible)
    // slide:  positive dp value (shifted down) → 0f (in position)
    val emblemAlpha by animateFloatAsState(
        targetValue   = if (showEmblem) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label         = "emblemAlpha"
    )
    val emblemSlide by animateFloatAsState(
        targetValue   = if (showEmblem) 0f else 32f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label         = "emblemSlide"
    )
    val titleAlpha by animateFloatAsState(
        targetValue   = if (showTitle) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "titleAlpha"
    )
    val titleSlide by animateFloatAsState(
        targetValue   = if (showTitle) 0f else 20f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "titleSlide"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue   = if (showButton) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label         = "buttonAlpha"
    )

    // ── CONTINUOUS ANIMATIONS ────────────────────────────────────────────────
    // Only two loops run on this screen:
    //   1. Glow pulse — the soft gold ring behind the emblem breathes slowly.
    //      Nothing rotates. Nothing spins. The emblem just quietly glows.
    //   2. Button shimmer — a narrow bright streak sweeps across the gold button.
    // This matches the calm visual weight of the Observer rank (no glow at all on
    // the Dashboard) and stays well below the intensity of even Initiate's halo —
    // the welcome screen should feel like an introduction, not a rank achievement.
    val infiniteTransition = rememberInfiniteTransition(label = "WelcomeLoop")

    // Glow pulse: outer ring breathes between 0.25 and 0.55 alpha over 3 800 ms.
    // Slow enough to feel atmospheric, not urgent.
    val glowPulse by infiniteTransition.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // Button shimmer: a light glint that sweeps left→right across the gold button
    // on a 3 200 ms loop. Reads as polished metal, not a loading bar.
    val buttonShimmer by infiniteTransition.animateFloat(
        initialValue  = -0.5f,
        targetValue   = 1.5f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ButtonShimmer"
    )

    // ── LAYOUT ───────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center
    ) {

        // ── ATMOSPHERIC BACKGROUND ────────────────────────────────────────
        // A faint warm radial gradient centred where the emblem sits.
        // Makes the dark background feel like a spotlight is on the emblem
        // rather than the emblem just floating in an empty void.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val spotCenter = Offset(size.width / 2f, size.height * 0.36f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF181208), Color.Transparent),
                            center = spotCenter,
                            radius = size.width * 0.80f
                        ),
                        radius = size.width * 0.80f,
                        center = spotCenter
                    )
                }
        )

        // ── MAIN CONTENT COLUMN ───────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {

            // ── EMBLEM WITH GLOW ──────────────────────────────────────────
            // Uses emblem_welcome.xml — the tactical targeting reticle.
            // This is the correct asset for this screen and it stays.
            //
            // Behind it: a single soft radial glow that breathes slowly.
            // Two layers:
            //   • Wide outer halo — marks the emblem's presence on screen.
            //   • Tighter inner ring just outside the emblem edge — gives a
            //     subtle "lit from within" look without overpowering the art.
            //
            // Container is 220dp so the glow has breathing room around the
            // 140dp emblem image without being clipped.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        alpha        = emblemAlpha
                        translationY = emblemSlide
                    }
                    .drawBehind {
                        val r = size.minDimension / 2f

                        // Wide outer halo — very soft, fades to transparent at edges
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ColorAccentGold.copy(alpha = glowPulse * 0.45f),
                                    ColorAccentGold.copy(alpha = glowPulse * 0.10f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = r
                            ),
                            radius = r
                        )

                        // Tighter inner ring — peaks just outside the emblem edge,
                        // giving the reticle a crisp gold outline without obscuring the art
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.54f to Color.Transparent,
                                    0.62f to ColorHotGold.copy(alpha = glowPulse * 0.75f),
                                    1.00f to Color.Transparent
                                ),
                                center = center,
                                radius = r
                            ),
                            radius = r
                        )
                    }
            ) {
                // The targeting reticle image — clipped to a circle for clean containment.
                Image(
                    painter            = painterResource(id = R.drawable.emblem_welcome),
                    contentDescription = "Mission Uncomfortable emblem",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── APP NAME ──────────────────────────────────────────────────
            // Displays the full app name so the user immediately knows what
            // app they just opened — no abstract words, no philosophy.
            //
            // Layout mirrors how rank names are shown on the Dashboard:
            //   • Small all-caps gold label ("MISSION") — the category
            //   • Larger bright name below ("Uncomfortable") — the identity
            // This way the name reads as a title card, not a statement.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha        = titleAlpha
                    translationY = titleSlide
                }
            ) {
                // "MISSION" — small gold label, heavily tracked, like a category tag
                Text(
                    text       = "MISSION",
                    color      = ColorAccentGold.copy(alpha = 0.85f),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 5.sp,
                    textAlign  = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // "Uncomfortable" — the app name, full brightness, bold
                Text(
                    text       = "Uncomfortable",
                    color      = ColorTextPrimary,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign  = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gold separator line — thin accent stroke between the name and button.
                // Provides visual breathing room and reinforces the gold palette.
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ColorAccentGold.copy(alpha = 0.70f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(52.dp))

            // ── CTA BUTTON ────────────────────────────────────────────────
            // Gold fill with a slow shimmer sweep for a polished-metal feel.
            // Styled the same as "ACCEPT THE MISSION" on the Dashboard.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer { alpha = buttonAlpha }
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorAccentGold)
                    .drawWithContent {
                        drawContent()     // Draw button label first
                        // Shimmer overlay: narrow bright streak travels left → right
                        val sx = size.width * buttonShimmer
                        val sw = size.width * 0.36f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                startX = sx,
                                endX   = sx + sw
                            )
                        )
                    }
                    .clickable(onClick = onStartJourney)
            ) {
                Text(
                    text          = "BEGIN YOUR JOURNEY",
                    color         = Color(0xFF0D0D0D),    // Near-black on gold — high contrast
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
