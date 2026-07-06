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
 *
 * ─── CHANGELOG ────────────────────────────────────────────────────────────────
 *   v2 — Added `objective` and `rules` fields to Mission for military briefing format.
 *        Added `isLocationDependent` to Mission to conditionally show "Swap Mission" button.
 *        Added `discomfortRating` to Mission to store the post-completion 1-10 slider rating.
 *
 *   v3 — Added `xpThreshold` to Rank.
 *        This is the MINIMUM total XP a user must have to hold this rank.
 *        The ViewModel uses this to recalculate rank automatically whenever XP changes,
 *        removing any hardcoded threshold logic from business code.
 *        ALL_RANKS now includes thresholds for all 5 tiers.
 *
 *   v4 — Wired real badge drawable resource IDs into each Rank.
 *        Each rank now points to its own unique badge drawable instead of the placeholder.
 *        RankBadgeSection in DashboardScreen uses rank.badgeResId to display the correct art.
 */

package com.example.missionuncomfortable.ui.dashboard

import com.example.missionuncomfortable.R   // v4: needed to reference R.drawable.badge_* resource IDs

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
 * @param xpThreshold   v3: The minimum total XP required to hold this rank.
 *                      The ViewModel calls calculateXpProgress(totalXp) which walks this list
 *                      to find the highest rank whose xpThreshold is still <= the user's total XP.
 *                      To tune rank-up requirements, only edit the values here — nothing else
 *                      in the codebase needs to change.
 *
 *                      Current thresholds:
 *                        Level 1 (Observer):   0 XP   — starting rank
 *                        Level 2 (Initiate):   200 XP
 *                        Level 3 (Challenger): 500 XP
 *                        Level 4 (Conqueror):  900 XP
 *                        Level 5 (Sovereign):  1400 XP
 *
 * @param badgeResId    v4: The drawable resource ID for the rank's unique badge image.
 *                      Each rank now has its own distinct artwork:
 *                        Level 1 → R.drawable.badge_observer   (open eye)
 *                        Level 2 → R.drawable.badge_initiate   (single chevron)
 *                        Level 3 → R.drawable.badge_challenger (shield + sword)
 *                        Level 4 → R.drawable.badge_conqueror  (eagle wings)
 *                        Level 5 → R.drawable.badge_sovereign  (military crown)
 *                      RankBadgeSection reads this field — no other code needs updating
 *                      when badge art changes. To swap an image, only edit the entry here.
 */
data class Rank(
    val level: Int,                    // Numeric level: 1, 2, 3, 4, or 5
    val title: String,                 // Human-readable title shown in the UI
    val description: String,          // Flavour text describing the rank
    val xpThreshold: Int,              // v3: Min total XP required to enter this rank
    val badgeResId: Int? = null        // v4: Drawable resource ID for this rank's badge
)

// A hardcoded list of all 5 possible ranks.
// This is the single source of truth for rank names, descriptions, XP thresholds, and badge art.
// The ViewModel reads this list to determine rank boundaries after XP changes.
// To change rank names, descriptions, XP costs, or badge art, only edit here.
val ALL_RANKS = listOf(
    // Level 1 — Beginner. The user is just starting out, watching the world around them.
    Rank(
        level = 1,
        title = "The Observer",
        description = "You watch from the sidelines. The first step is awareness.",
        xpThreshold = 0,                         // Starting rank — everyone begins at 0 XP
        badgeResId = R.drawable.badge_observer   // Open eye — watching, not yet acting
    ),
    // Level 2 — The user has started taking small, tentative social steps.
    Rank(
        level = 2,
        title = "The Initiate",
        description = "You've taken your first uncomfortable step. Keep moving.",
        xpThreshold = 200,                       // Reached at 200 total XP
        badgeResId = R.drawable.badge_initiate   // Single military chevron — one stripe earned
    ),
    // Level 3 — The user regularly engages in mild-to-moderate discomfort.
    Rank(
        level = 3,
        title = "The Challenger",
        description = "Discomfort is now familiar. You push through anyway.",
        xpThreshold = 500,                         // Reached at 500 total XP
        badgeResId = R.drawable.badge_challenger   // Shield + sword — actively fighting
    ),
    // Level 4 — The user actively seeks out difficult social situations.
    Rank(
        level = 4,
        title = "The Conqueror",
        description = "You don't avoid hard things. You walk straight into them.",
        xpThreshold = 900,                        // Reached at 900 total XP
        badgeResId = R.drawable.badge_conqueror   // Eagle wings spread — dominant, commanding
    ),
    // Level 5 — The pinnacle. The user has mastered social resilience.
    Rank(
        level = 5,
        title = "The Sovereign",
        description = "You command your environment. Nothing is impossible.",
        xpThreshold = 1400,                      // Reached at 1400 total XP — the final rank
        badgeResId = R.drawable.badge_sovereign  // Military crown — the pinnacle
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
    COMPLETED,  // The user has submitted their discomfort rating and marked it done
    FAILED,     // The mission expired (day ended) without a completion submission
    LOCKED      // The user hasn't unlocked this difficulty tier yet (future use)
}

/**
 * Mission — describes a single daily challenge assigned to the user.
 *
 * ─── v2 CHANGES ────────────────────────────────────────────────────────────
 *   - Added `objective`:           A single clear sentence stating WHAT the user must do.
 *                                  Shown as the "OBJECTIVE" section in the military briefing format.
 *
 *   - Added `rules`:               A list of specific constraints/steps for the mission.
 *                                  Shown as the "RULES" section in the military briefing format.
 *                                  Replaces the old monolithic `description` paragraph.
 *
 *   - Added `isLocationDependent`: True if the mission requires the user to travel to a
 *                                  specific type of place (e.g., a coffee shop, a park, a mall).
 *                                  When true, the "Swap Mission" button is shown on the card,
 *                                  allowing the user to swap to a non-location-dependent mission.
 *
 *   - Added `discomfortRating`:    The user's submitted 1-10 discomfort rating for this mission.
 *                                  Null means the user hasn't submitted a rating yet.
 *                                  Populated when the user submits the post-mission discomfort slider.
 * ───────────────────────────────────────────────────────────────────────────
 *
 * @param id                 A unique identifier for this mission (will be a UUID from Supabase in production).
 * @param title              Short title of the mission, e.g. "The Silent Stranger".
 * @param objective          A single clear sentence: WHAT must the user do? (military briefing: OBJECTIVE block).
 * @param rules              A list of specific constraints and steps (military briefing: RULES block).
 * @param xpReward           How much XP the user earns for completing this mission.
 * @param status             Current status of the mission (see MissionStatus enum above).
 * @param difficulty         A 1–5 difficulty rating. Higher rank users get harder missions.
 * @param dateAssigned       The date this mission was assigned (ISO-8601 string, e.g. "2026-07-05").
 * @param isLocationDependent True if the mission requires travelling to a specific type of location.
 *                            Shows the "Swap Mission" button on the card when true.
 * @param discomfortRating   The user's submitted 1–10 discomfort rating. Null = not yet submitted.
 */
data class Mission(
    val id: String,                                // Unique ID — will come from Supabase later
    val title: String,                             // Short display title
    val objective: String,                         // Single-sentence objective (what to do)
    val rules: List<String>,                       // Ordered list of rules/constraints for the mission
    val xpReward: Int,                             // XP granted on completion (e.g., 75)
    val status: MissionStatus,                     // Current state (ACTIVE, COMPLETED, etc.)
    val difficulty: Int,                           // Difficulty tier 1–5
    val dateAssigned: String,                      // Date string, e.g. "2026-07-05"
    val isLocationDependent: Boolean = false,      // True = user must travel somewhere specific
    val discomfortRating: Int? = null             // 1–10 discomfort rating, null = not yet submitted
)
