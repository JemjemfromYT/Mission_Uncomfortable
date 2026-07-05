/**
 * DashboardModels.kt
 *
 * This file holds all the pure data models (data classes) used by the Dashboard screen.
 * These are simple Kotlin data classes — they just hold information, they don't DO anything.
 *
 * Why separate models from the screen?
 *   Keeping data models in their own file makes them reusable across multiple screens,
 *   and it keeps DashboardScreen.kt focused purely on how things LOOK (UI), not what things ARE (data).
 *
 * In the future, these models will be populated from:
 *   - The local Room database (offline-first)
 *   - Supabase (synced in the background via WorkManager)
 */

package com.example.missionuncomfortable.ui.dashboard

// ─────────────────────────────────────────────────────────────────────────────
// RANK MODEL
// Represents a user's current rank/level in the gamification system.
// Ranks range from Level 1 (The Observer) to Level 5 (The Sovereign).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Rank — describes one of the 5 rank tiers a user can achieve.
 *
 * @param level         The numeric rank level (1 through 5).
 * @param title         The display name of the rank (e.g., "The Observer", "The Sovereign").
 * @param description   A short flavour-text description of what this rank represents.
 * @param badgeResId    The drawable resource ID for the rank's badge image.
 *                      Use R.drawable.badge_observer, etc. (you'll add these images to your res/drawable folder).
 *                      Set to null for now — we'll show a placeholder until real badges are designed.
 */
data class Rank(
    val level: Int,                    // Numeric level: 1, 2, 3, 4, or 5
    val title: String,                 // Human-readable title shown in the UI
    val description: String,          // Flavour text describing the rank
    val badgeResId: Int? = null        // Optional drawable resource ID — null = show placeholder
)

// A hardcoded list of all 5 possible ranks. This acts as the "rank registry" —
// the single source of truth for what each level is called and what it means.
// To change rank names or descriptions, only change them here.
val ALL_RANKS = listOf(
    // Level 1 — Beginner. The user is just starting out, observing the world around them.
    Rank(
        level = 1,
        title = "The Observer",
        description = "You watch from the sidelines. The first step is awareness."
    ),
    // Level 2 — The user has started taking small, tentative social steps.
    Rank(
        level = 2,
        title = "The Initiate",
        description = "You've taken your first uncomfortable step. Keep moving."
    ),
    // Level 3 — The user regularly engages in mild-to-moderate discomfort.
    Rank(
        level = 3,
        title = "The Challenger",
        description = "Discomfort is now familiar. You push through anyway."
    ),
    // Level 4 — The user actively seeks out difficult social situations.
    Rank(
        level = 4,
        title = "The Conqueror",
        description = "You don't avoid hard things. You walk straight into them."
    ),
    // Level 5 — The pinnacle. The user has mastered social resilience.
    Rank(
        level = 5,
        title = "The Sovereign",
        description = "You command your environment. Nothing is impossible."
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// XP MODEL
// Tracks the user's current experience points and calculates progress.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * XpProgress — holds all XP-related info needed to render the progress bar.
 *
 * @param currentXp         How much XP the user has earned so far in total.
 * @param xpForCurrentRank  The XP threshold at which the user ENTERED their current rank.
 * @param xpForNextRank     The XP threshold at which the user will reach the NEXT rank.
 *                          If the user is at max rank (5), this equals currentXp — progress is "full".
 * @param currentRank       The user's current Rank object (see Rank data class above).
 */
data class XpProgress(
    val currentXp: Int,                // e.g., 350 XP total earned
    val xpForCurrentRank: Int,         // e.g., 200 XP (threshold to enter current rank)
    val xpForNextRank: Int,            // e.g., 500 XP (threshold to reach next rank)
    val currentRank: Rank              // The current Rank object for title, badge, etc.
) {
    /**
     * progressFraction — calculates what fraction of the current rank's XP bar is filled.
     *
     * Example:
     *   currentXp = 350, xpForCurrentRank = 200, xpForNextRank = 500
     *   → (350 - 200) / (500 - 200) = 150 / 300 = 0.5  (50% of the way to next rank)
     *
     * Clamped to [0f, 1f] so we never get a negative or >100% bar (just in case of data errors).
     */
    val progressFraction: Float
        get() {
            // Avoid division by zero if xpForNextRank somehow equals xpForCurrentRank
            val range = xpForNextRank - xpForCurrentRank
            if (range <= 0) return 1f  // If at max rank or bad data, show full bar

            // Calculate raw progress fraction
            val rawProgress = (currentXp - xpForCurrentRank).toFloat() / range.toFloat()

            // Clamp between 0.0 and 1.0 so the progress bar never breaks
            return rawProgress.coerceIn(0f, 1f)
        }

    /**
     * xpUntilNextRank — a convenience property for displaying "X XP until next rank".
     * Returns 0 if the user is already at or beyond the next rank threshold.
     */
    val xpUntilNextRank: Int
        get() = (xpForNextRank - currentXp).coerceAtLeast(0)
}

// ─────────────────────────────────────────────────────────────────────────────
// MISSION MODEL
// Represents a single daily mission assigned to the user.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MissionStatus — an enum describing what state a mission is currently in.
 *
 * Using an enum (rather than a plain string) lets us write safe `when` statements
 * that the compiler will warn us about if we forget a case.
 */
enum class MissionStatus {
    ACTIVE,     // The user has been assigned the mission and hasn't completed it yet
    COMPLETED,  // The user has submitted their reflection/photo and marked it done
    FAILED,     // The mission expired (day ended) without a completion submission
    LOCKED      // The user hasn't unlocked this difficulty tier yet (future use)
}

/**
 * Mission — describes a single daily challenge assigned to the user.
 *
 * @param id            A unique identifier for this mission (will be a UUID from Supabase in production).
 * @param title         Short title of the mission, e.g. "The Silent Stranger".
 * @param description   Full description of what the user must do.
 * @param xpReward      How much XP the user earns for completing this mission.
 * @param status        Current status of the mission (see MissionStatus enum above).
 * @param difficulty    A 1–5 difficulty rating. Higher rank users get harder missions.
 * @param dateAssigned  The date this mission was assigned (ISO-8601 string, e.g. "2026-07-05").
 */
data class Mission(
    val id: String,                          // Unique ID — will come from Supabase later
    val title: String,                       // Short display title
    val description: String,                 // Full instructions for what the user must do
    val xpReward: Int,                       // XP granted on completion (e.g., 50)
    val status: MissionStatus,               // Current state (ACTIVE, COMPLETED, etc.)
    val difficulty: Int,                     // Difficulty tier 1–5
    val dateAssigned: String                 // Date string, e.g. "2026-07-05"
)
