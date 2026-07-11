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
 * Call this in MissionNavGraph (NavGraph.kt) — NOT in DashboardScreen. Placing it
 * at the NavGraph level means the player lives for the entire tab session, so
 * switching to History or Stats does not destroy and recreate the MediaPlayer.
 *
 *   rememberDashboardAudio(
 *       context   = LocalContext.current,
 *       rankLevel = rankForBgm,          // state var kept at NavGraph level
 *       active    = showBottomNav        // false on welcome/rankup/ascension
 *   )
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
 * v2 — Added [active] parameter. The player now lives at NavGraph level so it
 *      survives History / Stats tab switches. When active=false (welcome, rankup,
 *      ascension screens) the volume is clamped to 0f so the BGM is silenced
 *      without destroying and recreating the MediaPlayer. setVolume() is safe to
 *      call in any MediaPlayer state, including PREPARING.
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
 * • Loops indefinitely at 45% volume when [active] is true.
 * • Instantly silenced (volume 0) when [active] is false — the player is NOT
 *   released, so it can resume immediately when [active] returns to true.
 * • Switches track instantly when [rankLevel] changes (e.g. after a rank-up).
 * • Releases resources when the composable leaves the tree via DisposableEffect.
 *
 * ── Why active instead of removing the call ──────────────────────────────────
 * This composable lives in MissionNavGraph (above the tab routes). It is NEVER
 * removed from the composition tree during tab switches. The [active] flag lets
 * us silence the player on screens that should be quiet (welcome, rankup,
 * ascension) without destroying and recreating the MediaPlayer — which would
 * cause a brief startup gap on return.
 *
 * ── Thread safety ────────────────────────────────────────────────────────────
 * prepareAsync() is called instead of prepare() so the MediaPlayer's internal
 * system thread handles buffering — the main/composition thread is never
 * blocked. setOnPreparedListener fires on that thread when ready and calls
 * start() automatically. setVolume() is safe in any MediaPlayer state,
 * including PREPARING, so active changes never throw.
 *
 * @param context    Android context for resource loading. Pass LocalContext.current.
 * @param rankLevel  Current rank level (1–5). Changes trigger a track switch.
 * @param active     True when BGM should be audible (Mission / History / Stats tabs).
 *                   False on welcome, rankup, and ascension screens.
 */
@Composable
fun rememberDashboardAudio(context: Context, rankLevel: Int, active: Boolean = true) {

    // remember(rankLevel) destroys and recreates the player whenever the rank
    // changes, which naturally switches the track. The initial volume respects
    // the current active state so the player is never audible when it shouldn't be.
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

                // Start at the correct volume for the current active state.
                // If the player is created while active=false (e.g. rank changes
                // during ascension), it starts silent and unmutes when active
                // returns to true via the LaunchedEffect below.
                val initialVol = if (active) 0.45f else 0f
                setVolume(initialVol, initialVol)

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

    // ── React to active changes ───────────────────────────────────────────────
    // When the user navigates to welcome/rankup/ascension, active becomes false
    // and the volume drops to 0 instantly (setVolume is safe in any state).
    // When they return to a tab route, active becomes true and sound resumes
    // without any gap — the player was running silently the whole time.
    LaunchedEffect(active) {
        if (active) player?.setVolume(0.45f, 0.45f)
        else        player?.setVolume(0f, 0f)
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
