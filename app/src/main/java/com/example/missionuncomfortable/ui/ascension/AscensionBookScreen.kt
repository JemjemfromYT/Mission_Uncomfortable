/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        ⚠  AI ASSISTANT — READ THIS BEFORE EDITING THIS FILE  ⚠         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PROJECT: Mission: Uncomfortable — Android app (Kotlin + Jetpack Compose)
 * Daily social-discomfort challenges. User accepts a mission → goes
 * and does it → returns → rates discomfort (1–10) → earns XP → ranks up.
 *
 * KEY FILES for this feature:
 * AscensionLore.kt        — Story content + colour theme per rank (read this first).
 * AscensionBookScreen.kt  — this file. The full-screen book UI.
 * DashboardViewModel.kt   — fires ascensionEvent: Rank? when a book should be shown.
 * NavGraph.kt             — routes "ascension" → AscensionBookScreen.
 *
 * ── COMMENTING RULES — NEVER break these ────────────────────────────────────
 * 1. DO NOT remove any existing comment. If a comment is outdated, UPDATE it.
 * 2. ADD comments to every new block of code you write.
 * 3. COMMENT STYLE: section dividers, KDoc blocks, inline reasoning.
 * 4. CHANGELOG block in the file header, updated on every significant change.
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
 * CLOSED   → A closed book sits centered in darkness. Three strips: dark spine
 *            on the left, sigil/title cover in the middle, thick fore-edge of
 *            visible page lines on the right. Ember particles drift upward.
 *            Tapping the book begins OPENING.
 *
 * OPENING  → The cover swings open on its spine (left-edge Y-axis hinge). A
 *            bright flash marks the moment it falls fully open.
 *
 * READING  → The book stays as a visible PHYSICAL OBJECT on the dark background —
 *            same size and position as the closed book, now showing its open
 *            interior. A spine strip remains on the left. Pages are warm parchment.
 *            Swiping triggers a true page-curl: the leaving page folds back around
 *            its right edge, the arriving page unfolds from its left edge, both
 *            rotating in place (pager translation cancelled) so it looks like
 *            physically turning a leaf, not sliding a card.
 *
 * CLOSING  → Tapping "CLOSE THE BOOK" fades the whole scene to black, then
 *            calls onFinish().
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 * app/src/main/java/com/example/missionuncomfortable/ui/ascension/AscensionBookScreen.kt
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 * v1 — Initial implementation. Closed book → hinge-open animation → paged
 *      story reader → fade-out close, themed per rank via AscensionLore.kt.
 * v2 — Realistic book visual (spine + fore-edge), Y-axis spine-hinge opening,
 *      3D page-turn graphicsLayer effect.
 * v3 — Thicker fore-edge, page backgrounds, page-curl with translation cancel.
 * v4 — Reading state is now a constrained book object on the dark background
 *      (same visual weight as the closed book). Pages are warm aged parchment.
 *      Page-curl fixed: each page rotates in-place around the correct edge with
 *      a drop-shadow between pages to reinforce depth. Book spine stays visible
 *      throughout reading.
 * v5 — Fixed StoryReader page curl logic (added left anchor, corrected left-edge
 *      transformOrigin, added zIndex for proper page stacking).
 * v6 — Flawless Cover Transition. The ClosedBookScene now perfectly matches
 *      the dimensions (300x300) and shadow of the StoryReader. Instead of rotating
 *      the entire book away, only the front cover swings open on a spine hinge,
 *      revealing the actual TitlePage waiting seamlessly underneath it. Z-index
 *      page stacking logic simplified and perfected.
 * v8 — Added PageIllustration: a Canvas drawing composable for every rank × page
 *      combination (5 ranks × 3 story pages = 15 illustrations). StoryPage now
 *      accepts rankLevel: Int and varies the illustration position per page:
 *        page 1 → illustration at the top, centred
 *        page 2 → illustration at the bottom, end-aligned
 *        page 3 → illustration at the top, start-aligned (left)
 *      Added import kotlin.math.cos (sin was already imported).
 * v9 — Added AscensionAudioState + rememberAscensionAudio: full audio system
 *      using MediaPlayer (looping BGM) and SoundPool (low-latency SFX).
 *      5 rank-specific BGM loops + 5 sound effects — see AUDIO SYSTEM section
 *      for the full file list. All audio degrades gracefully if files are absent.
 *      BGM starts immediately on screen entry; fades/stops on CLOSING.
 *      SFX triggers: book tap, book open, flash shimmer, page turn, book close.
 * v10 — Corrected BGM design. The 5 rank-specific BGM loops (bgm_observer, etc.)
 *      belong on the MAIN SCREEN, not here. The book screen now uses a single
 *      dedicated track: bgm_book.mp3. rememberAscensionAudio no longer accepts
 *      rankLevel; it always loads bgm_book regardless of rank.
 * v11 — Fixed silent BGM failure. Synchronous prepare() on the composition
 *      (main) thread caused MediaPlayerNative error (1, -2147483648) on device.
 *      Replaced with prepareAsync() + setOnPreparedListener { start() } so
 *      the player self-starts off the main thread. Removed the redundant
 *      LaunchedEffect(Unit) { startBgm() } — the listener handles it.
 * v12 — Fixed "pause called in state 0" crash on close. stopBgm() now calls
 *      setVolume(0f,0f) instead of pause() — safe to call in any MediaPlayer
 *      state including PREPARING. The player is fully released when the
 *      composable leaves the tree via DisposableEffect.
 */

package com.example.missionuncomfortable.ui.ascension

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.missionuncomfortable.R
import com.example.missionuncomfortable.ui.dashboard.Rank
import kotlinx.coroutines.delay
import kotlin.math.cos
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
// AUDIO SYSTEM
// ─────────────────────────────────────────────────────────────────────────────

/**
 * SfxEvent — all discrete sound effects that can fire during the book experience.
 *
 * Files must be placed in app/src/main/res/raw/ with exactly these names
 * (Android lowercases them automatically):
 *   sfx_ui_click.mp3   — instant tap feedback on every interactive element
 *   sfx_book_tap.mp3   — the user taps the closed book cover (follows sfx_ui_click)
 *   sfx_book_open.mp3  — the cover-swing animation starts
 *   sfx_flash.mp3      — the radiant light flash at the opening midpoint
 *   sfx_page_turn.mp3  — each page swipe completes
 *   sfx_book_close.mp3 — the book-close thud (follows sfx_ui_click on the button)
 */
private enum class SfxEvent {
    UI_CLICK,   // instant tap response — fires first on every button/tap
    BOOK_TAP,
    BOOK_OPEN,
    FLASH,
    PAGE_TURN,
    BOOK_CLOSE
}

/**
 * AscensionAudioState — wraps a looping MediaPlayer (BGM) and a SoundPool
 * (low-latency SFX) for the duration of one AscensionBookScreen session.
 *
 * BGM (one file, shared across all ranks, placed in res/raw/):
 *   bgm_book.mp3  — the book-reading atmosphere loop
 *
 * The 5 rank-specific tracks (bgm_observer, bgm_initiate, etc.) are for the
 * MAIN SCREEN, not this screen.
 *
 * If any audio file is missing at runtime the corresponding play call is
 * silently skipped — the experience degrades gracefully with no crash.
 */
private class AscensionAudioState(
    private val bgmPlayer: MediaPlayer?,
    private val pool: SoundPool,
    private val soundIds: Map<SfxEvent, Int>
) {
    /**
     * Silences the BGM when the book is closing.
     *
     * Uses setVolume(0,0) instead of pause() because pause() throws
     * "pause called in state 0" if prepareAsync() has not finished yet.
     * setVolume() is safe to call in ANY MediaPlayer state — it simply
     * drops the output to zero. The player is fully released 600 ms later
     * when DisposableEffect.onDispose() fires as the composable leaves the tree.
     */
    fun stopBgm() { runCatching { bgmPlayer?.setVolume(0f, 0f) } }

    /** Play a one-shot SFX. Silently skips if the sound file was absent. */
    fun playSfx(event: SfxEvent) {
        val id = soundIds[event] ?: return
        if (id != 0) pool.play(id, /*leftVol*/ 1f, /*rightVol*/ 1f, /*priority*/ 1, /*loop*/ 0, /*rate*/ 1f)
    }

    /** Release all native audio resources. Must be called exactly once, on disposal. */
    fun release() {
        bgmPlayer?.release()
        pool.release()
    }
}

/**
 * Direct R.raw resource IDs for every SFX event.
 * Using R.raw.* (compile-time constants) instead of getIdentifier() avoids the
 * discouraged-API warning and lets the build tool verify files exist at compile time.
 * All 6 .mp3 files must be present in app/src/main/res/raw/.
 */
private val sfxResIds = mapOf(
    SfxEvent.UI_CLICK   to R.raw.sfx_ui_click,
    SfxEvent.BOOK_TAP   to R.raw.sfx_book_tap,
    SfxEvent.BOOK_OPEN  to R.raw.sfx_book_open,
    SfxEvent.FLASH      to R.raw.sfx_flash,
    SfxEvent.PAGE_TURN  to R.raw.sfx_page_turn,
    SfxEvent.BOOK_CLOSE to R.raw.sfx_book_close
)

/**
 * rememberAscensionAudio — creates, remembers, and auto-releases an
 * AscensionAudioState tied to the current composition lifecycle.
 *
 * - MediaPlayer: uses prepareAsync() + setOnPreparedListener so the audio
 *   system thread handles buffering — never blocks the main/composition thread.
 *   The listener calls start() automatically when preparation completes.
 * - SoundPool: sounds are loaded in the background; any SFX played before its
 *   load completes is silently skipped (SoundPool's default behaviour).
 *
 * @param context  Android context, used only for resource loading.
 */
@Composable
private fun rememberAscensionAudio(context: Context): AscensionAudioState {

    val audioState = remember {

        // ── BGM: MediaPlayer — single looping book atmosphere track ───────────
        // bgm_book.mp3 is the ONLY BGM used inside the book screen.
        // R.raw.bgm_book is a compile-time constant — no getIdentifier() needed.
        // The 5 rank-specific tracks (bgm_observer, etc.) belong on the main screen.
        //
        // prepareAsync() is non-blocking: the MediaPlayer system thread handles
        // buffering. setOnPreparedListener fires on that thread when ready and
        // calls start() automatically — no external startBgm() call needed for
        // the initial play. This avoids the MEDIA_ERROR_SYSTEM (1,-2147483648)
        // that synchronous prepare() caused when called on the main thread.
        val bgmPlayer: MediaPlayer? = runCatching {
            MediaPlayer().apply {
                val afd = context.resources.openRawResourceFd(R.raw.bgm_book)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                setVolume(0.50f, 0.50f) // 50% — atmospheric, not overpowering
                setOnPreparedListener { mp -> mp.start() } // self-starts when ready
                prepareAsync()                              // never blocks main thread
            }
        }.getOrNull() // null if file missing — degrades gracefully with no crash

        // ── SFX: SoundPool — low-latency one-shot effects ────────────────────
        val sfxAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(4) // up to 4 sounds overlapping at once
            .setAudioAttributes(sfxAttributes)
            .build()

        // Load each SFX using compile-time R.raw.* IDs — no getIdentifier() needed.
        // runCatching per entry so one missing file never blocks the others.
        val soundIds: Map<SfxEvent, Int> = sfxResIds.mapValues { (_, resId) ->
            runCatching { pool.load(context, resId, 1) }.getOrElse { 0 }
        }

        AscensionAudioState(bgmPlayer, pool, soundIds)
    }

    // Release all native resources when this composable leaves the tree.
    DisposableEffect(Unit) {
        onDispose { audioState.release() }
    }

    return audioState
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
    val lore    = remember(rank.level) { getLoreForRank(rank.level) }
    val context = LocalContext.current

    var stage by remember { mutableStateOf(BookStage.CLOSED) }

    // ── AUDIO ─────────────────────────────────────────────────────────────────
    // BGM starts automatically via setOnPreparedListener inside rememberAscensionAudio
    // — no LaunchedEffect needed for the initial play. Released automatically
    // when this composable leaves the tree (DisposableEffect inside that function).
    val audio = rememberAscensionAudio(context)

    // Stop BGM on close; fade-out runs for 600 ms before calling onFinish().
    LaunchedEffect(stage) {
        if (stage == BookStage.CLOSING) {
            audio.stopBgm()
            delay(600)
            onFinish()
        }
    }

    val screenAlpha by animateFloatAsState(
        targetValue = if (stage == BookStage.CLOSING) 0f else 1f,
        animationSpec = tween(durationMillis = 550),
        label = "AscensionScreenFade"
    )

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
                    rank = rank, // Passed so the underlying TitlePage can render
                    isOpening = stage == BookStage.OPENING,
                    onOpenAnimationComplete = { stage = BookStage.READING },
                    onTap = {
                        if (stage == BookStage.CLOSED) {
                            audio.playSfx(SfxEvent.UI_CLICK)  // instant button feedback
                            audio.playSfx(SfxEvent.BOOK_TAP)  // tap thud
                            audio.playSfx(SfxEvent.BOOK_OPEN) // cover-swing creak
                            stage = BookStage.OPENING
                        }
                    },
                    // Flash shimmer plays at the animation midpoint (triggered inside ClosedBookScene).
                    onFlashSound = { audio.playSfx(SfxEvent.FLASH) }
                )
            }
            BookStage.READING -> {
                StoryReader(
                    lore = lore,
                    rank = rank,
                    onCloseBook = { stage = BookStage.CLOSING },
                    // Page-turn paper rustle — fired on every page change.
                    onPageTurn = { audio.playSfx(SfxEvent.PAGE_TURN) },
                    // Book-close thud — fired when the user taps "CLOSE THE BOOK".
                    onCloseSfx = { audio.playSfx(SfxEvent.BOOK_CLOSE) }
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
 * V6 REWRITE: To ensure a flawless transition, this Box is exactly the same 300x300
 * size and shadow as the StoryReader. It renders the actual TitlePage on Layer 1.
 * Layer 2 is the Cover, which swings open on a left hinge. When the cover finishes
 * swinging and fades out, the TitlePage is already perfectly in place, making the
 * state swap to StoryReader totally invisible.
 */
@Composable
private fun ClosedBookScene(
    lore: AscensionLore,
    rank: Rank,
    isOpening: Boolean,
    onOpenAnimationComplete: () -> Unit,
    onTap: () -> Unit,
    onFlashSound: () -> Unit  // fires the shimmer SFX at the animation midpoint
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

    // Play the flash shimmer SFX ~500ms into the 1000ms open swing (visual midpoint).
    LaunchedEffect(isOpening) {
        if (isOpening) {
            delay(500)
            onFlashSound()
        }
    }

    // rotationY 0 → -90: ONLY the cover swings open on its left hinge.
    val openRotation by animateFloatAsState(
        targetValue = if (isOpening) -90f else 0f,
        animationSpec = tween(durationMillis = 1000),
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

            // ── THE PHYSICAL BOOK BOUNDARY ────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .height(300.dp)
                    // The shadow matches StoryReader exactly and never rotates away.
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(10.dp))
                    .clickable(enabled = !isOpening, onClick = onTap)
            ) {
                // ── LAYER 1: BASE OPEN BOOK (Identical to StoryReader Page 0) ──
                Row(modifier = Modifier.fillMaxSize()) {
                    // Spine (Always visible)
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

                    // The Title Page (Waiting underneath the cover)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                            .background(ColorPageParchment),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TitlePage(lore = lore, rank = rank)
                        }
                    }
                }

                // ── LAYER 2: THE SWINGING COVER ────────────────────────────────
                // Placed exactly over the Title Page, offset by the 18dp spine.
                Row(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.width(18.dp)) // Leave the spine exposed
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                // Hinge exactly at the left edge of the cover (right of spine)
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                rotationY = openRotation
                                cameraDistance = 24 * density
                                // Fade the cover out in the final 15 degrees to prevent edge-on artifacts
                                alpha = if (openRotation < -75f) (90f + openRotation) / 15f else 1f
                            }
                            .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                    ) {
                        // Cover components: Face + Fore-edge
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Cover face
                            Box(
                                modifier = Modifier
                                    .weight(1f)
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

                            // Fore-edge — lines simulating page leaves
                            Canvas(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight()
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
                    }
                }
            } // End Book Box

            // Breathing drop shadow beneath the book (fades as it opens).
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(20.dp)
                    .graphicsLayer { alpha = 1f - openProgress }
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
 * StoryReader — the open book interior.
 *
 * Page-curl mechanics (V6):
 * To simulate turning a physical book page, we must:
 * 1. Cancel the pager's automatic X translation.
 * 2. Use `zIndex` so earlier pages (lower index) stack ON TOP of later pages.
 * 3. Always hinge the rotation on the LEFT EDGE (the spine).
 * When turning, the active page rotates 0° → -90° and smoothly fades out before
 * it becomes edge-on, revealing the next page already perfectly flat underneath.
 */
@Composable
private fun StoryReader(
    lore: AscensionLore,
    rank: Rank,
    onCloseBook: () -> Unit,
    onPageTurn: () -> Unit,  // fires sfx_page_turn on every page change
    onCloseSfx: () -> Unit   // fires sfx_book_close when user taps "CLOSE THE BOOK"
) {
    val totalPages = lore.pages.size + 2
    val pagerState = rememberPagerState(pageCount = { totalPages })

    // Track the previous page to avoid firing the SFX on the initial render
    // (currentPage starts at 0; only fire when the user actually swipes).
    var prevPage by remember { mutableIntStateOf(0) }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != prevPage) {
            onPageTurn()
            prevPage = pagerState.currentPage
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ── THE OPEN BOOK ─────────────────────────────────────────────────
            // Matches ClosedBookScene dimensions perfectly: 300dp x 300dp.
            Row(
                modifier = Modifier
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(10.dp))
                    .height(300.dp)
                    .width(300.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── OPEN SPINE ────────────────────────────────────────────────
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

                        val pageOffset = (pageIndex - pagerState.currentPage).toFloat() -
                                pagerState.currentPageOffsetFraction

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                // CRITICAL: Stacks pages so Page 0 is on top of Page 1, etc.
                                .zIndex((totalPages - pageIndex).toFloat())
                                .graphicsLayer {
                                    // Cancel horizontal sliding drag
                                    translationX = -pageOffset * size.width

                                    // ALL pages hinge on the left (the spine)
                                    transformOrigin = TransformOrigin(0f, 0.5f)

                                    if (pageOffset <= 0f) {
                                        // The active page or leaving page. Swings left (0 to -90).
                                        val rotY = pageOffset * 90f
                                        rotationY = rotY
                                        // Fade it out in the final 15 degrees so it doesn't artifact
                                        // when it becomes perfectly edge-on.
                                        alpha = if (rotY < -75f) (90f + rotY) / 15f else 1f
                                    } else {
                                        // Incoming page from the right sits perfectly flat waiting
                                        // to be revealed as the top page turns.
                                        rotationY = 0f
                                        alpha = 1f
                                    }

                                    cameraDistance = 24 * density
                                }
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
                                    totalPages-1 -> ClosingPage(
                                        lore         = lore,
                                        onCloseBook  = onCloseBook,
                                        onCloseSound = onCloseSfx
                                    )
                                    else         -> StoryPage(
                                        lore       = lore,
                                        paragraph  = lore.pages[pageIndex - 1],
                                        pageNumber = pageIndex,
                                        // Pass rank level so PageIllustration can pick the right drawing.
                                        rankLevel  = rank.level
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
 * StoryPage — a single body page with a Canvas illustration and body text.
 *
 * Illustration placement varies per page so the book feels alive, not templated:
 *   page 1 → illustration at the top, centred above the text
 *   page 2 → text at the top, illustration anchored to the bottom-end
 *   page 3 → illustration at the top-start (left), then text below
 *
 * @param lore        Rank theme (drop-cap colour, folio colour, illustration colours).
 * @param paragraph   Page text; kept short (2–4 sentences) so it fits the book.
 * @param pageNumber  1-based index shown as a small folio marker.
 * @param rankLevel   Rank level (1–5) used by PageIllustration to pick the drawing.
 */
@Composable
private fun StoryPage(
    lore: AscensionLore,
    paragraph: String,
    pageNumber: Int,
    rankLevel: Int
) {
    val firstChar = paragraph.firstOrNull()?.toString() ?: ""
    val rest = if (paragraph.isNotEmpty()) paragraph.substring(1) else ""

    // ── BODY TEXT BLOCK ─────────────────────────────────────────────────────
    // Extracted as a lambda so it can be reused in the three layout branches.
    val bodyText: @Composable () -> Unit = {
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
    }

    // ── FOLIO NUMBER ────────────────────────────────────────────────────────
    val folioNumber: @Composable () -> Unit = {
        Text(
            text = "— $pageNumber —",
            color = lore.theme.accentColor.copy(alpha = 0.6f),
            fontSize = 9.sp,
            letterSpacing = 2.sp
        )
    }

    // ── ILLUSTRATION ────────────────────────────────────────────────────────
    val illustration: @Composable (Modifier) -> Unit = { mod ->
        PageIllustration(
            rankLevel  = rankLevel,
            pageIndex  = pageNumber, // 1, 2, or 3
            theme      = lore.theme,
            modifier   = mod
        )
    }

    // ── LAYOUT — varies by page number ──────────────────────────────────────
    when (pageNumber) {

        // Page 1: illustration centred at the top, then text, then folio.
        1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            illustration(Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(10.dp))
            bodyText()
            Spacer(modifier = Modifier.height(10.dp))
            folioNumber()
        }

        // Page 2: text at the top, folio, then illustration anchored bottom-end.
        2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            bodyText()
            Spacer(modifier = Modifier.height(10.dp))
            folioNumber()
            Spacer(modifier = Modifier.height(10.dp))
            illustration(Modifier.align(Alignment.End))
        }

        // Page 3: illustration at the top-start (left), then text, then folio.
        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            illustration(Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(10.dp))
            bodyText()
            Spacer(modifier = Modifier.height(10.dp))
            folioNumber()
        }
    }
}

/**
 * ClosingPage — closing line, ornamental divider, and "CLOSE THE BOOK" button.
 *
 * @param lore          Rank theme.
 * @param onCloseBook   Called when the button is tapped (triggers screen close).
 * @param onCloseSound  Plays sfx_book_close before the screen fades out.
 */
@Composable
private fun ClosingPage(
    lore: AscensionLore,
    onCloseBook: () -> Unit,
    onCloseSound: () -> Unit
) {
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
        // Plays the book-close SFX then immediately triggers the screen fade.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(lore.theme.accentColor)
                .clickable {
                    onCloseSound()  // sfx_ui_click + sfx_book_close (fired inside AscensionBookScreen)
                    onCloseBook()
                }
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

// ─────────────────────────────────────────────────────────────────────────────
// PAGE ILLUSTRATIONS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * PageIllustration — a small Canvas drawing that decorates each story page.
 *
 * Each rank has three illustrations (one per body page). All drawings use only
 * the rank's accentColor and hotColor so they stay within the 60/30/10 palette.
 * Shapes are simple and geometric — they should read clearly at ~110×60dp.
 *
 * Rank → drawings:
 *   L1 Observer   p1: oval running track + figure    p2: stopwatch face    p3: finish-line tape
 *   L2 Initiate   p1: metal leg brace outline        p2: lone footprint    p3: three gold stars
 *   L3 Challenger p1: ship hull trapped in ice       p2: small boat+waves  p3: compass rose
 *   L4 Conqueror  p1: barbed wire                    p2: candle flame      p3: open book
 *   L5 Sovereign  p1: two Roman columns              p2: scroll with lines p3: laurel wreath
 *
 * @param rankLevel  1–5, selects which set of illustrations to draw.
 * @param pageIndex  1, 2, or 3 (body pages only — not title/closing).
 * @param theme      Rank theme, provides accentColor and hotColor.
 * @param modifier   Applied to the Canvas so the caller can control alignment.
 */
@Composable
private fun PageIllustration(
    rankLevel: Int,
    pageIndex: Int,
    theme: AscensionTheme,
    modifier: Modifier = Modifier
) {
    val ac = theme.accentColor   // 30% accent — main shapes
    val hc = theme.hotColor      // 10% hot   — highlights and details

    Canvas(
        modifier = modifier
            .width(110.dp)
            .height(60.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (rankLevel) {

            // ── L1: OBSERVER (Roger Bannister) ────────────────────────────────
            1 -> when (pageIndex) {

                // p1 — Oval running track with a tiny figure at the finish
                1 -> {
                    // Outer oval track lane
                    drawOval(
                        color = ac.copy(alpha = 0.65f),
                        topLeft = Offset(6f, 8f),
                        size = Size(w - 12f, h - 16f),
                        style = Stroke(width = 3f)
                    )
                    // Inner dashed lane (drawn as a slightly smaller oval, lower opacity)
                    drawOval(
                        color = ac.copy(alpha = 0.30f),
                        topLeft = Offset(18f, 16f),
                        size = Size(w - 36f, h - 32f),
                        style = Stroke(width = 1.5f)
                    )
                    // Finish line — two vertical strokes on the right side of the oval
                    drawLine(hc, Offset(w - 16f, cy - 10f), Offset(w - 16f, cy + 10f), strokeWidth = 2.5f)
                    drawLine(hc.copy(alpha = 0.45f), Offset(w - 21f, cy - 9f), Offset(w - 21f, cy + 9f), strokeWidth = 1f)
                    // Stick runner breaking the tape (simplified: head + body + arms + legs)
                    val rx = w - 14f; val ry = cy - 16f
                    drawCircle(hc, radius = 4f, center = Offset(rx, ry)) // head
                    drawLine(hc, Offset(rx, ry + 4f), Offset(rx, ry + 14f), strokeWidth = 1.8f) // torso
                    drawLine(hc, Offset(rx, ry + 7f), Offset(rx - 6f, ry + 4f), strokeWidth = 1.5f) // left arm
                    drawLine(hc, Offset(rx, ry + 7f), Offset(rx + 6f, ry + 5f), strokeWidth = 1.5f) // right arm
                    drawLine(hc, Offset(rx, ry + 14f), Offset(rx - 5f, ry + 22f), strokeWidth = 1.5f) // left leg
                    drawLine(hc, Offset(rx, ry + 14f), Offset(rx + 5f, ry + 21f), strokeWidth = 1.5f) // right leg
                    // Speed lines to the left of the runner
                    for (i in 0..2) {
                        drawLine(ac.copy(alpha = 0.45f),
                            Offset(rx - 10f, ry + 5f + i * 5f),
                            Offset(rx - 18f, ry + 5f + i * 5f), strokeWidth = 1f)
                    }
                }

                // p2 — Stopwatch face (3:59)
                2 -> {
                    val r = minOf(w, h) / 2f - 4f
                    // Outer ring
                    drawCircle(ac.copy(alpha = 0.65f), radius = r, center = Offset(cx, cy), style = Stroke(width = 2.5f))
                    // Inner ring
                    drawCircle(ac.copy(alpha = 0.25f), radius = r - 6f, center = Offset(cx, cy), style = Stroke(width = 1f))
                    // Crown button at the top
                    drawLine(ac.copy(alpha = 0.7f), Offset(cx, cy - r), Offset(cx, cy - r - 7f), strokeWidth = 3f, cap = StrokeCap.Round)
                    // Tick marks (12 positions)
                    for (i in 0 until 12) {
                        val angle = (i * 30.0 - 90.0) * (Math.PI / 180.0)
                        val outer = r - 1f; val inner = if (i % 3 == 0) r - 7f else r - 4f
                        drawLine(
                            if (i % 3 == 0) ac.copy(alpha = 0.7f) else ac.copy(alpha = 0.35f),
                            Offset(cx + inner * cos(angle).toFloat(), cy + inner * sin(angle).toFloat()),
                            Offset(cx + outer * cos(angle).toFloat(), cy + outer * sin(angle).toFloat()),
                            strokeWidth = if (i % 3 == 0) 1.5f else 0.8f
                        )
                    }
                    // Minute hand — pointing just before 12 (almost done)
                    drawLine(ac.copy(alpha = 0.85f), Offset(cx, cy), Offset(cx + 2f, cy - r + 10f), strokeWidth = 2f, cap = StrokeCap.Round)
                    // Second hand — pointing almost at 12
                    drawLine(hc, Offset(cx, cy), Offset(cx - 1f, cy - r + 6f), strokeWidth = 1.2f, cap = StrokeCap.Round)
                    // Centre dot
                    drawCircle(hc, radius = 2.5f, center = Offset(cx, cy))
                }

                // p3 — Finish-line tape breaking apart
                else -> {
                    val tapeY = cy - 4f; val tapeH = 12f
                    // Left post
                    drawRect(ac.copy(alpha = 0.70f), topLeft = Offset(4f, tapeY - 18f), size = Size(5f, tapeH + 26f))
                    // Right post
                    drawRect(ac.copy(alpha = 0.70f), topLeft = Offset(w - 9f, tapeY - 18f), size = Size(5f, tapeH + 26f))
                    // Left half of tape (intact)
                    drawRect(ac.copy(alpha = 0.55f), topLeft = Offset(9f, tapeY), size = Size(w / 2f - 18f, tapeH))
                    // Right half (curling away — drawn as a parallelogram-ish path)
                    val path = Path().apply {
                        moveTo(w / 2f + 6f, tapeY)
                        lineTo(w - 9f, tapeY - 6f)
                        lineTo(w - 9f, tapeY + tapeH - 6f)
                        lineTo(w / 2f + 6f, tapeY + tapeH)
                        close()
                    }
                    drawPath(path, ac.copy(alpha = 0.40f))
                    // Tear gap — two angled lines
                    drawLine(hc.copy(alpha = 0.7f), Offset(w / 2f - 6f, tapeY - 2f), Offset(w / 2f + 2f, tapeY + tapeH + 2f), strokeWidth = 1.5f)
                    drawLine(hc.copy(alpha = 0.7f), Offset(w / 2f + 4f, tapeY - 2f), Offset(w / 2f + 10f, tapeY + tapeH + 2f), strokeWidth = 1.5f)
                    // Flying confetti dots
                    drawCircle(hc.copy(alpha = 0.6f), 2.5f, Offset(w / 2f - 4f, tapeY - 10f))
                    drawCircle(ac.copy(alpha = 0.5f), 1.8f, Offset(w / 2f + 8f, tapeY - 14f))
                    drawCircle(hc.copy(alpha = 0.4f), 2f, Offset(w / 2f + 2f, tapeY + tapeH + 12f))
                }
            }

            // ── L2: INITIATE (Wilma Rudolph) ──────────────────────────────────
            2 -> when (pageIndex) {

                // p1 — Metal leg brace (geometric outline: two uprights + three crossbars)
                1 -> {
                    val lx = cx - 20f; val rx = cx + 20f
                    val top = 6f; val bot = h - 6f
                    // Two vertical struts
                    drawLine(ac.copy(alpha = 0.7f), Offset(lx, top), Offset(lx, bot), strokeWidth = 3f, cap = StrokeCap.Round)
                    drawLine(ac.copy(alpha = 0.7f), Offset(rx, top), Offset(rx, bot), strokeWidth = 3f, cap = StrokeCap.Round)
                    // Three horizontal crossbars
                    val bars = listOf(top + 6f, cy, bot - 6f)
                    bars.forEach { y ->
                        drawLine(hc.copy(alpha = 0.75f), Offset(lx - 2f, y), Offset(rx + 2f, y), strokeWidth = 2f, cap = StrokeCap.Round)
                    }
                    // Hinge circles at top and bottom (where the brace fastens)
                    drawCircle(ac.copy(alpha = 0.5f), 4f, Offset(lx, top + 6f), style = Stroke(1.5f))
                    drawCircle(ac.copy(alpha = 0.5f), 4f, Offset(rx, top + 6f), style = Stroke(1.5f))
                    drawCircle(ac.copy(alpha = 0.5f), 4f, Offset(lx, bot - 6f), style = Stroke(1.5f))
                    drawCircle(ac.copy(alpha = 0.5f), 4f, Offset(rx, bot - 6f), style = Stroke(1.5f))
                }

                // p2 — A single footprint (heel oval + five toe circles)
                2 -> {
                    // Foot main body (elongated oval tilted slightly)
                    drawOval(
                        hc.copy(alpha = 0.50f),
                        topLeft = Offset(cx - 12f, cy - 22f),
                        size = Size(24f, 40f)
                    )
                    // Heel (slightly wider oval at bottom)
                    drawOval(
                        ac.copy(alpha = 0.55f),
                        topLeft = Offset(cx - 10f, cy + 12f),
                        size = Size(20f, 12f)
                    )
                    // Five toes (small circles above the foot)
                    val toeX = listOf(cx - 14f, cx - 7f, cx, cx + 7f, cx + 13f)
                    val toeY = listOf(cy - 26f, cy - 29f, cy - 30f, cy - 28f, cy - 24f)
                    val toeR = listOf(4.5f, 4f, 4.5f, 4f, 3.5f)
                    toeX.forEachIndexed { i, tx ->
                        drawCircle(hc.copy(alpha = 0.55f), toeR[i], Offset(tx, toeY[i]))
                    }
                }

                // p3 — Three medal stars (horizontal row)
                else -> {
                    val starCentres = listOf(cx - 32f, cx, cx + 32f)
                    starCentres.forEach { sx ->
                        // Draw a 5-pointed star using a Path
                        val outerR = 14f; val innerR = 6f
                        val starPath = Path()
                        for (i in 0 until 10) {
                            val angle = (i * 36.0 - 90.0) * (Math.PI / 180.0)
                            val r = if (i % 2 == 0) outerR else innerR
                            val px = sx + r * cos(angle).toFloat()
                            val py = cy + r * sin(angle).toFloat()
                            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                        }
                        starPath.close()
                        // Fill the star with hot colour, outline with accent
                        drawPath(starPath, hc.copy(alpha = 0.65f))
                        drawPath(starPath, ac.copy(alpha = 0.80f), style = Stroke(width = 1.2f))
                    }
                }
            }

            // ── L3: CHALLENGER (Shackleton) ───────────────────────────────────
            3 -> when (pageIndex) {

                // p1 — Ship hull trapped in jagged ice
                1 -> {
                    // Ship hull (simple trapezoid)
                    val hull = Path().apply {
                        moveTo(cx - 28f, cy + 5f)   // bottom-left
                        lineTo(cx + 28f, cy + 5f)   // bottom-right
                        lineTo(cx + 20f, cy - 10f)  // top-right
                        lineTo(cx - 20f, cy - 10f)  // top-left
                        close()
                    }
                    drawPath(hull, ac.copy(alpha = 0.55f))
                    drawPath(hull, hc.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
                    // Mast
                    drawLine(hc.copy(alpha = 0.65f), Offset(cx, cy - 10f), Offset(cx, cy - 28f), strokeWidth = 2f)
                    // Horizontal spar on mast
                    drawLine(hc.copy(alpha = 0.45f), Offset(cx - 10f, cy - 20f), Offset(cx + 10f, cy - 20f), strokeWidth = 1.5f)
                    // Ice jagged lines (top and bottom of ship area)
                    val icePoints = listOf(
                        Offset(4f, cy + 14f), Offset(14f, cy + 6f), Offset(24f, cy + 16f),
                        Offset(36f, cy + 8f), Offset(48f, cy + 18f), Offset(58f, cy + 10f),
                        Offset(68f, cy + 20f), Offset(80f, cy + 12f), Offset(w - 4f, cy + 18f)
                    )
                    val icePath = Path()
                    icePoints.forEachIndexed { i, pt ->
                        if (i == 0) icePath.moveTo(pt.x, pt.y) else icePath.lineTo(pt.x, pt.y)
                    }
                    drawPath(icePath, hc.copy(alpha = 0.45f), style = Stroke(width = 1.5f))
                }

                // p2 — Small open boat on rough waves
                2 -> {
                    // Boat hull (simple curved bottom)
                    val boat = Path().apply {
                        moveTo(cx - 32f, cy)
                        cubicTo(cx - 32f, cy + 16f, cx + 32f, cy + 16f, cx + 32f, cy)
                        lineTo(cx + 28f, cy - 8f)
                        lineTo(cx - 28f, cy - 8f)
                        close()
                    }
                    drawPath(boat, ac.copy(alpha = 0.60f))
                    drawPath(boat, hc.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
                    // Mast + sail
                    drawLine(hc.copy(alpha = 0.7f), Offset(cx - 4f, cy - 8f), Offset(cx - 4f, cy - 30f), strokeWidth = 1.5f)
                    val sail = Path().apply {
                        moveTo(cx - 4f, cy - 28f)
                        lineTo(cx + 18f, cy - 12f)
                        lineTo(cx - 4f, cy - 10f)
                        close()
                    }
                    drawPath(sail, hc.copy(alpha = 0.40f))
                    // Wave lines beneath the boat
                    for (i in 0..2) {
                        val wy = cy + 20f + i * 8f
                        val wavePath = Path()
                        var wx = 4f
                        wavePath.moveTo(wx, wy)
                        while (wx < w - 4f) {
                            wavePath.cubicTo(wx + 8f, wy - 5f, wx + 16f, wy + 5f, wx + 24f, wy)
                            wx += 24f
                        }
                        drawPath(wavePath, ac.copy(alpha = 0.3f - i * 0.08f), style = Stroke(1f))
                    }
                }

                // p3 — Compass rose (8 directional lines from centre)
                else -> {
                    // Outer ring
                    drawCircle(ac.copy(alpha = 0.40f), radius = h / 2f - 4f, center = Offset(cx, cy), style = Stroke(1f))
                    // 8 compass arms
                    for (i in 0 until 8) {
                        val angle = (i * 45.0) * (Math.PI / 180.0)
                        val isCardinal = i % 2 == 0
                        val armLen = if (isCardinal) h / 2f - 5f else h / 2f - 12f
                        val x2 = cx + armLen * sin(angle).toFloat()
                        val y2 = cy - armLen * cos(angle).toFloat()
                        drawLine(
                            if (isCardinal) hc.copy(alpha = 0.75f) else ac.copy(alpha = 0.45f),
                            Offset(cx, cy), Offset(x2, y2),
                            strokeWidth = if (isCardinal) 1.8f else 1f
                        )
                    }
                    // North arrowhead (filled diamond tip)
                    val northPath = Path().apply {
                        moveTo(cx, cy - (h / 2f - 5f))
                        lineTo(cx - 5f, cy - (h / 2f - 15f))
                        lineTo(cx, cy - (h / 2f - 20f))
                        lineTo(cx + 5f, cy - (h / 2f - 15f))
                        close()
                    }
                    drawPath(northPath, hc.copy(alpha = 0.80f))
                    // Centre dot
                    drawCircle(hc, 3f, Offset(cx, cy))
                }
            }

            // ── L4: CONQUEROR (Viktor Frankl) ─────────────────────────────────
            4 -> when (pageIndex) {

                // p1 — Barbed wire (horizontal line + small X barb marks)
                1 -> {
                    // Three horizontal wire strands
                    val wireY = listOf(cy - 8f, cy, cy + 8f)
                    wireY.forEach { wy ->
                        drawLine(ac.copy(alpha = 0.55f), Offset(4f, wy), Offset(w - 4f, wy), strokeWidth = 1.5f)
                    }
                    // Barb crosses (X shapes) along the middle wire
                    val barbX = listOf(w * 0.15f, w * 0.35f, w * 0.55f, w * 0.75f, w * 0.90f)
                    barbX.forEach { bx ->
                        val blen = 5f
                        drawLine(hc.copy(alpha = 0.70f), Offset(bx - blen, cy - blen), Offset(bx + blen, cy + blen), strokeWidth = 1.5f)
                        drawLine(hc.copy(alpha = 0.70f), Offset(bx - blen, cy + blen), Offset(bx + blen, cy - blen), strokeWidth = 1.5f)
                    }
                    // Vertical post at each end
                    drawLine(ac.copy(alpha = 0.65f), Offset(4f, 4f), Offset(4f, h - 4f), strokeWidth = 2.5f)
                    drawLine(ac.copy(alpha = 0.65f), Offset(w - 4f, 4f), Offset(w - 4f, h - 4f), strokeWidth = 2.5f)
                }

                // p2 — Candle with flame
                2 -> {
                    // Candle body
                    drawRect(
                        ac.copy(alpha = 0.65f),
                        topLeft = Offset(cx - 10f, cy - 4f),
                        size = Size(20f, h / 2f)
                    )
                    // Wax drip on the side
                    val drip = Path().apply {
                        moveTo(cx + 10f, cy + 4f)
                        cubicTo(cx + 13f, cy + 12f, cx + 10f, cy + 18f, cx + 8f, cy + 20f)
                        lineTo(cx + 10f, cy + 20f)
                        lineTo(cx + 10f, cy + 4f)
                    }
                    drawPath(drip, ac.copy(alpha = 0.40f))
                    // Wick
                    drawLine(hc.copy(alpha = 0.8f), Offset(cx, cy - 4f), Offset(cx + 2f, cy - 12f), strokeWidth = 1.5f)
                    // Flame (teardrop path)
                    val flame = Path().apply {
                        moveTo(cx + 2f, cy - 12f)                             // base of flame (wick tip)
                        cubicTo(cx - 8f, cy - 20f, cx - 6f, cy - 34f, cx + 2f, cy - 36f) // left curve up
                        cubicTo(cx + 10f, cy - 34f, cx + 12f, cy - 20f, cx + 2f, cy - 12f) // right curve down
                        close()
                    }
                    drawPath(flame, hc.copy(alpha = 0.75f))
                    drawPath(flame, ac.copy(alpha = 0.60f), style = Stroke(1f))
                    // Warm glow halo under the flame
                    drawCircle(hc.copy(alpha = 0.12f), 18f, Offset(cx + 2f, cy - 24f))
                }

                // p3 — Open book (two rectangles with a spine, text lines inside)
                else -> {
                    val bw = w * 0.42f; val bh = h * 0.65f
                    val leftX = cx - bw - 3f; val rightX = cx + 3f
                    val bookTop = cy - bh / 2f
                    // Left page
                    drawRect(ac.copy(alpha = 0.50f), topLeft = Offset(leftX, bookTop), size = Size(bw, bh))
                    drawRect(ac.copy(alpha = 0.65f), topLeft = Offset(leftX, bookTop), size = Size(bw, bh), style = Stroke(1.2f))
                    // Right page
                    drawRect(ac.copy(alpha = 0.50f), topLeft = Offset(rightX, bookTop), size = Size(bw, bh))
                    drawRect(ac.copy(alpha = 0.65f), topLeft = Offset(rightX, bookTop), size = Size(bw, bh), style = Stroke(1.2f))
                    // Spine line
                    drawLine(hc.copy(alpha = 0.60f), Offset(cx, bookTop - 2f), Offset(cx, bookTop + bh + 2f), strokeWidth = 2f)
                    // Text lines on each page
                    for (i in 0 until 4) {
                        val lineY = bookTop + 10f + i * 9f
                        val lineW = bw - 14f
                        drawLine(hc.copy(alpha = 0.30f), Offset(leftX + 7f, lineY), Offset(leftX + 7f + lineW, lineY), strokeWidth = 1f)
                        drawLine(hc.copy(alpha = 0.30f), Offset(rightX + 7f, lineY), Offset(rightX + 7f + lineW, lineY), strokeWidth = 1f)
                    }
                }
            }

            // ── L5: SOVEREIGN (Marcus Aurelius) ───────────────────────────────
            5 -> when (pageIndex) {

                // p1 — Two Roman columns (rectangles + capitals + triangular pediment)
                1 -> {
                    val colW = 16f; val colH = h - 16f
                    val col1X = cx - 36f; val col2X = cx + 20f
                    val capH = 6f; val baseH = 5f; val colTop = 8f + capH + baseH

                    // Left column
                    drawRect(ac.copy(alpha = 0.60f), topLeft = Offset(col1X, colTop), size = Size(colW, colH - capH - baseH - 8f))
                    // Right column
                    drawRect(ac.copy(alpha = 0.60f), topLeft = Offset(col2X, colTop), size = Size(colW, colH - capH - baseH - 8f))

                    // Capitals (wider rectangles at the top)
                    drawRect(hc.copy(alpha = 0.65f), topLeft = Offset(col1X - 4f, colTop - capH), size = Size(colW + 8f, capH))
                    drawRect(hc.copy(alpha = 0.65f), topLeft = Offset(col2X - 4f, colTop - capH), size = Size(colW + 8f, capH))

                    // Bases (wider rectangles at the bottom)
                    val botY = colTop + colH - capH - baseH - 8f
                    drawRect(hc.copy(alpha = 0.50f), topLeft = Offset(col1X - 4f, botY), size = Size(colW + 8f, baseH))
                    drawRect(hc.copy(alpha = 0.50f), topLeft = Offset(col2X - 4f, botY), size = Size(colW + 8f, baseH))

                    // Shared horizontal entablature across both columns
                    drawRect(
                        ac.copy(alpha = 0.55f),
                        topLeft = Offset(col1X - 4f, colTop - capH - 5f),
                        size = Size(col2X + colW + 4f - (col1X - 4f), 5f)
                    )

                    // Triangular pediment above the entablature
                    val pedPath = Path().apply {
                        moveTo(col1X - 4f, colTop - capH - 5f)
                        lineTo(col2X + colW + 4f, colTop - capH - 5f)
                        lineTo(cx + colW / 2f, colTop - capH - 5f - 16f)
                        close()
                    }
                    drawPath(pedPath, ac.copy(alpha = 0.35f))
                    drawPath(pedPath, hc.copy(alpha = 0.55f), style = Stroke(1.2f))
                }

                // p2 — Scroll with horizontal text lines (curled ends)
                2 -> {
                    val sw = w - 16f; val sh = h * 0.55f
                    val sx = 8f; val sy = cy - sh / 2f
                    // Scroll body
                    drawRect(ac.copy(alpha = 0.50f), topLeft = Offset(sx, sy), size = Size(sw, sh))
                    drawRect(ac.copy(alpha = 0.65f), topLeft = Offset(sx, sy), size = Size(sw, sh), style = Stroke(1.2f))
                    // Left curl (oval)
                    drawOval(hc.copy(alpha = 0.55f), topLeft = Offset(sx - 6f, sy - 4f), size = Size(16f, sh + 8f), style = Stroke(2f))
                    // Right curl
                    drawOval(hc.copy(alpha = 0.55f), topLeft = Offset(sx + sw - 10f, sy - 4f), size = Size(16f, sh + 8f), style = Stroke(2f))
                    // Text lines on the scroll
                    val lineCount = 4
                    val lineSpacing = sh / (lineCount + 1)
                    for (i in 1..lineCount) {
                        val lineY = sy + i * lineSpacing
                        val lineWidth = sw - 24f - (if (i == lineCount) 20f else 0f)
                        drawLine(hc.copy(alpha = 0.35f), Offset(sx + 12f, lineY), Offset(sx + 12f + lineWidth, lineY), strokeWidth = 1.2f)
                    }
                }

                // p3 — Laurel wreath (two arcs of leaf shapes flanking a central dot)
                else -> {
                    val wrR = h / 2f - 5f
                    // Draw leaf ovals along left and right arcs
                    for (i in 0 until 9) {
                        val t = i / 8f
                        // Left arc: angles from -170° to -10° (left semicircle)
                        val leftAngle = (-170.0 + t * 160.0) * (Math.PI / 180.0)
                        val lx2 = cx + wrR * cos(leftAngle).toFloat()
                        val ly2 = cy + wrR * sin(leftAngle).toFloat()
                        // Leaf as a small oval at this arc position
                        drawOval(
                            ac.copy(alpha = 0.55f + t * 0.15f),
                            topLeft = Offset(lx2 - 5f, ly2 - 8f),
                            size = Size(10f, 16f)
                        )
                        // Right arc: mirror
                        val rightAngle = (-10.0 + t * 160.0) * (Math.PI / 180.0)
                        val rx2 = cx + wrR * cos(rightAngle).toFloat()
                        val ry2 = cy + wrR * sin(rightAngle).toFloat()
                        drawOval(
                            ac.copy(alpha = 0.55f + t * 0.15f),
                            topLeft = Offset(rx2 - 5f, ry2 - 8f),
                            size = Size(10f, 16f)
                        )
                    }
                    // Central radiant star at the bottom of the wreath
                    val starPath = Path()
                    val outerR = 9f; val innerR = 4.5f
                    for (i in 0 until 10) {
                        val angle = (i * 36.0 - 90.0) * (Math.PI / 180.0)
                        val r = if (i % 2 == 0) outerR else innerR
                        val px = cx + r * cos(angle).toFloat()
                        val py = (cy + wrR) + r * sin(angle).toFloat()
                        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                    }
                    starPath.close()
                    drawPath(starPath, hc.copy(alpha = 0.80f))
                    drawPath(starPath, ac.copy(alpha = 0.70f), style = Stroke(1f))
                }
            }

            // Fallback for any unrecognised rank level — draws a simple diamond.
            else -> {
                val dp = Path().apply {
                    moveTo(cx, 6f); lineTo(w - 6f, cy); lineTo(cx, h - 6f); lineTo(6f, cy); close()
                }
                drawPath(dp, ac.copy(alpha = 0.5f))
                drawPath(dp, hc.copy(alpha = 0.6f), style = Stroke(1.5f))
            }
        }
    }
}
