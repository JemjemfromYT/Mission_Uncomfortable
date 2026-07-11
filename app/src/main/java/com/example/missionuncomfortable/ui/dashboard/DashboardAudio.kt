/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        ⚠  AI ASSISTANT — READ THIS BEFORE EDITING THIS FILE  ⚠         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PROJECT: Mission: Uncomfortable — Android app (Kotlin + Jetpack Compose)
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
 * DashboardAudio.kt
 *
 * Ambient background music for the MAIN / DASHBOARD screen.
 *
 * Each rank has its own looping BGM track. The correct track plays whenever
 * the user is on the dashboard, and stops automatically when the composable
 * leaves the tree (e.g. when the Ascension Book screen opens over it).
 *
 * ─── AUDIO FILES REQUIRED (app/src/main/res/raw/) ────────────────────────────
 *
 *   bgm_observer.mp3    — Rank 1: The Observer
 *   bgm_initiate.mp3    — Rank 2: The Initiate
 *   bgm_challenger.mp3  — Rank 3: The Challenger
 *   bgm_conqueror.mp3   — Rank 4: The Conqueror
 *   bgm_sovereign.mp3   — Rank 5: The Sovereign
 *
 * ─── HOW TO INTEGRATE ────────────────────────────────────────────────────────
 *
 * In your DashboardScreen composable, add ONE line at the top of the function
 * body (after val context = LocalContext.current):
 *
 *   rememberDashboardAudio(context = LocalContext.current, rankLevel = rank.level)
 *
 * That's it. The composable manages its own lifecycle — no manual start/stop.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/dashboard/DashboardAudio.kt
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 * v1 — Initial implementation. prepareAsync() + setOnPreparedListener so the
 *      MediaPlayer never blocks the main/composition thread. Auto-releases via
 *      DisposableEffect. Switches track instantly when rankLevel changes.
 */

package com.example.missionuncomfortable.ui.dashboard

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.example.missionuncomfortable.R

// ─────────────────────────────────────────────────────────────────────────────
// RANK → BGM RESOURCE MAP
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns the res/raw/ resource ID for the ambient BGM that matches the given
 * rank level (1–5). Defaults to bgm_observer for any unrecognised value.
 *
 * All five .mp3 files must be present in app/src/main/res/raw/ or the build
 * will fail — this is intentional so a missing file is caught at compile time
 * rather than silently producing no music at runtime.
 */
private fun bgmResIdForRank(rankLevel: Int): Int = when (rankLevel) {
    1    -> R.raw.bgm_observer
    2    -> R.raw.bgm_initiate
    3    -> R.raw.bgm_challenger
    4    -> R.raw.bgm_conqueror
    5    -> R.raw.bgm_sovereign
    else -> R.raw.bgm_observer   // safe fallback for any future rank extension
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * rememberDashboardAudio — plays the rank-appropriate ambient BGM loop for as
 * long as the calling composable stays in the tree.
 *
 * ── Behaviour ────────────────────────────────────────────────────────────────
 * • Starts automatically — no manual call needed.
 * • Loops indefinitely at 45% volume (ambient, not distracting).
 * • Switches track instantly when [rankLevel] changes (e.g. after a rank-up).
 * • Stops and releases resources as soon as the composable leaves the tree
 *   (e.g. when the Ascension Book screen appears) via DisposableEffect.
 *
 * ── Thread safety ────────────────────────────────────────────────────────────
 * prepareAsync() is called instead of prepare() so the MediaPlayer's internal
 * system thread handles buffering — the main/composition thread is never
 * blocked. setOnPreparedListener fires on that thread when ready and calls
 * start() automatically.
 *
 * @param context    Android context for resource loading. Pass LocalContext.current.
 * @param rankLevel  Current rank level (1–5). Changes trigger a track switch.
 */
@Composable
fun rememberDashboardAudio(context: Context, rankLevel: Int) {

    // remember(rankLevel) destroys and recreates the player whenever the rank
    // changes, which naturally switches the track.
    val player = remember(rankLevel) {
        runCatching {
            MediaPlayer().apply {

                // ── Load the rank-specific audio resource ─────────────────────
                val resId = bgmResIdForRank(rankLevel)
                val afd   = context.resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                // ── Audio focus / session attributes ──────────────────────────
                // USAGE_GAME keeps this track in the game audio stream, which is
                // the same stream used by AscensionBookScreen — both fade together
                // when the user presses the hardware volume keys.
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                isLooping = true
                setVolume(0.45f, 0.45f) // 45% — present but not overpowering

                // ── Non-blocking prepare ──────────────────────────────────────
                // setOnPreparedListener fires on the MediaPlayer system thread
                // when buffering is complete; start() is called there, so the
                // main thread is never touched.
                setOnPreparedListener { mp -> mp.start() }
                prepareAsync()
            }
        }.getOrNull()
        // getOrNull() means a missing or corrupt file produces null instead of
        // crashing. The screen still works; it's just silent.
    }

    // ── Lifecycle cleanup ─────────────────────────────────────────────────────
    // DisposableEffect re-runs its onDispose whenever rankLevel changes
    // (matching the remember key above) and also when the composable leaves
    // the tree. This guarantees the old player is always released before the
    // new one is created.
    DisposableEffect(rankLevel) {
        onDispose {
            runCatching { player?.release() }
        }
    }
}
