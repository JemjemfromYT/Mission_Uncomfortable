/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        ⚠  AI ASSISTANT — READ THIS BEFORE EDITING THIS FILE  ⚠         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PROJECT: Mission: Uncomfortable — Android app (Kotlin + Jetpack Compose)
 *          Daily social-discomfort challenges. User accepts a mission → goes
 *          and does it → returns → rates discomfort (1–10) → earns XP → ranks up.
 *
 * KEY FILES (read these for full context before making changes):
 *   DashboardModels.kt     — All enums + data classes (Mission, Rank, XpProgress, MissionCategory) ← YOU ARE HERE
 *   MissionRepository.kt   — Full mission library: 25 missions, 5 XP tiers, 6 categories
 *   DashboardViewModel.kt  — All game logic: XP, streaks, rank-ups, history saving, core loop
 *   DashboardScreen.kt     — Main UI. v5: SWAP MISSION button shows a popup, does NOT swap.
 *   HistoryModels.kt       — CompletedMissionEntry data class
 *   HistoryViewModel.kt    — Reads history from SharedPreferences JSON
 *   HistoryScreen.kt       — Scrollable mission history UI
 *   RankUpScreen.kt        — Full-screen rank-up celebration with spring animation
 *   StatsViewModel.kt      — Computes stats (streaks, category breakdown, avg discomfort)
 *   StatsScreen.kt         — Bar charts: category breakdown + discomfort by day of week
 *
 * ── COMMENTING RULES — NEVER break these ────────────────────────────────────
 *
 *   1. DO NOT remove any existing comment. If a comment is outdated, UPDATE it.
 *      The developer relies on comments to understand decisions made earlier.
 *      Deleting a comment is not allowed, even if the code seems obvious.
 *
 *   2. ADD comments to every new block of code you write:
 *        • Every function         → KDoc block /** ... */ with @param / @return
 *        • Every data class       → KDoc block on the class AND on every field
 *        • Every enum value       → inline comment explaining what state it represents
 *        • Every composable       → KDoc block + ── SECTION NAME ── dividers inside
 *        • Every non-obvious line → inline comment explaining WHY, not just what
 *
 *   3. COMMENT STYLE used across this entire codebase (stay consistent):
 *        • Section dividers  →  // ── SECTION NAME ─────────────────────────────
 *        • KDoc blocks       →  /** ... */ above every function, class, data class
 *        • Inline reasoning  →  // Why this decision was made (not just "what it does")
 *
 *   4. CHANGELOG: every file has a  ─── CHANGELOG ───  block in its file header.
 *      When you make a significant change, add a line:
 *        v3 — Short description of what changed and why.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these rules, the developer will have to   ║
 * ║  manually fix every file you touch. Please respect these conventions.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * DashboardModels.kt
 *
 * All data models used by the Dashboard, Mission Card, and History screens.
 *
 * ─── WHAT THIS FILE CONTAINS ────────────────────────────────────────────────
 *
 *   MissionStatus    — Enum: ACTIVE, IN_PROGRESS, COMPLETED, FAILED, LOCKED
 *   MissionCategory  — Enum: 6 categories of discomfort (NEW in v2)
 *   Rank             — Data class: level, title, description, XP threshold, badge resource ID
 *   Mission          — Data class: full mission data including category (NEW in v2)
 *   XpProgress       — Data class: current XP, rank, and progress bar fraction
 *   ALL_RANKS        — The 5 ranks the user can achieve, in order
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/dashboard/DashboardModels.kt
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial version. MissionStatus, Rank, Mission, XpProgress, ALL_RANKS.
 *
 *   v2 — Phase 3 + Phase 5 updates:
 *          1. MissionStatus: added IN_PROGRESS.
 *             IN_PROGRESS means the user tapped ACCEPT and is out doing the mission.
 *             The slider should NOT appear until the user returns and taps "I DID IT".
 *          2. MissionCategory enum added (Phase 5).
 *             6 categories: SOCIAL_INITIATION, EYE_CONTACT, REJECTION_PRACTICE,
 *             PRESENCE, PHYSICAL, PERFORMANCE.
 *             Used by the History and Stats screens to show category breakdowns.
 *          3. Mission data class: added `category` field (default SOCIAL_INITIATION).
 */

package com.example.missionuncomfortable.ui.dashboard

// R is needed to reference drawable resource IDs (R.drawable.badge_xxx).
// This is the only import in DashboardModels — kept minimal by design.
import com.example.missionuncomfortable.R

// ─────────────────────────────────────────────────────────────────────────────
// MISSION STATUS ENUM
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MissionStatus — the lifecycle state of a daily mission.
 *
 * ACTIVE      → The mission has been assigned but the user has not yet accepted it.
 *               The "ACCEPT THE MISSION" gold button is shown.
 *
 * IN_PROGRESS → The user tapped ACCEPT and is currently out doing the mission.
 *               NEW in v2 (Phase 3). The card shows "MISSION IN PROGRESS" with
 *               "I DID IT" and "I COULDN'T DO IT" buttons.
 *               Persisted to SharedPreferences (key: "mission_status" = "IN_PROGRESS")
 *               so it survives the user pressing the home button.
 *
 * COMPLETED   → The user returned, tapped "I DID IT", rated their discomfort,
 *               and tapped SUBMIT RATING. XP has been awarded.
 *               The "VIEW COMPLETION" button is shown.
 *
 * FAILED      → The day ended without the user completing the mission.
 *               Reserved for future enforcement (e.g., a midnight reset job).
 *               Currently not set anywhere — included for future use.
 *
 * LOCKED      → The mission is not yet available at the user's current rank.
 *               Reserved for future use when mission tiers are enforced by rank.
 */
enum class MissionStatus {
    ACTIVE,        // Assigned but not yet accepted
    IN_PROGRESS,   // User tapped ACCEPT — they are out doing it right now (NEW v2)
    COMPLETED,     // User returned, rated it, XP awarded
    FAILED,        // Day ended without completion (future enforcement)
    LOCKED         // Not yet unlocked for this rank tier (future use)
}

// ─────────────────────────────────────────────────────────────────────────────
// MISSION CATEGORY ENUM (NEW in v2)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * MissionCategory — the type of discomfort this mission primarily targets.
 *
 * NEW in v2 (Phase 5). Used by:
 *   - MissionRepository: each mission is tagged with a category.
 *   - HistoryScreen: shows the category for each completed mission row.
 *   - StatsScreen: computes "most completed category" from history.
 *   - (Future) Mission selection: surface missions in weak categories first.
 *
 * The 6 categories cover the full range of social discomfort the app targets:
 */
enum class MissionCategory {
    SOCIAL_INITIATION,   // Starting conversations, approaching strangers
    EYE_CONTACT,         // Direct, sustained eye contact in public
    REJECTION_PRACTICE,  // Asking for something expecting to be refused
    PRESENCE,            // Being fully present without distraction or escape
    PHYSICAL,            // Physical discomfort the body resists (cold showers, etc.)
    PERFORMANCE          // Speaking in front of others, being visibly active
}

// ─────────────────────────────────────────────────────────────────────────────
// RANK DATA CLASS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Rank — represents one of the 5 achievable ranks.
 *
 * @param level        Numeric rank level, 1–5. Level 1 is the starting rank.
 * @param title        Display name. e.g. "The Observer".
 * @param description  Short flavour text shown under the badge. e.g. "You are watching."
 * @param xpRequired   The total XP needed to reach (and hold) this rank.
 *                     Level 1 starts at 0 XP.
 * @param badgeResId   Android drawable resource ID for the rank badge image.
 *                     Null shows a letter-circle placeholder (safe — no crash).
 *                     v2: checkNotNull() removed from DashboardScreen.RankBadgeSection —
 *                     a null badgeResId now shows a placeholder instead of crashing.
 *                     All 5 ranks now have their drawables connected (see ALL_RANKS below).
 */
data class Rank(
    val level: Int,
    val title: String,
    val description: String,
    val xpRequired: Int,
    val badgeResId: Int? = null   // Nullable so future ranks compile before a badge is added
)

// ─────────────────────────────────────────────────────────────────────────────
// ALL RANKS — the 5 ranks in order
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ALL_RANKS — the complete ordered list of achievable ranks.
 *
 * The user starts at Level 1 (The Observer) and advances by earning XP.
 * Rank level is determined by total XP, not by streak or missions completed.
 *
 * XP thresholds:
 *   Level 1 — The Observer    →    0 XP  (starting rank)
 *   Level 2 — The Initiate    →  200 XP  (~2 weeks of daily missions)
 *   Level 3 — The Challenger  →  500 XP  (~5 weeks of daily missions)
 *   Level 4 — The Conqueror   →  900 XP  (~9 weeks of daily missions)
 *   Level 5 — The Sovereign   → 1500 XP  (~15 weeks of daily missions)
 *
 * Badge drawable resource IDs must be set once the drawables are created.
 * Replace the null values below with the actual R.drawable.xxx resource IDs.
 */
val ALL_RANKS = listOf(
    Rank(
        level       = 1,
        title       = "The Observer",
        description = "You are watching. The first step is noticing.",
        xpRequired  = 0,
        // v2: connected to badge drawable — was null until drawables were added to res/drawable/.
        // File: app/src/main/res/drawable/badge_observer.xml
        badgeResId  = R.drawable.badge_observer
    ),
    Rank(
        level       = 2,
        title       = "The Initiate",
        description = "You have begun. Most people never do.",
        xpRequired  = 200,
        // File: app/src/main/res/drawable/badge_initiate.xml
        badgeResId  = R.drawable.badge_initiate
    ),
    Rank(
        level       = 3,
        title       = "The Challenger",
        description = "You seek difficulty on purpose. That is rare.",
        xpRequired  = 500,
        // File: app/src/main/res/drawable/badge_challenger.xml
        badgeResId  = R.drawable.badge_challenger
    ),
    Rank(
        level       = 4,
        title       = "The Conqueror",
        description = "Discomfort does not stop you. It fuels you.",
        xpRequired  = 900,
        // File: app/src/main/res/drawable/badge_conqueror.xml
        badgeResId  = R.drawable.badge_conqueror
    ),
    Rank(
        level       = 5,
        title       = "The Sovereign",
        description = "You have become someone who cannot be intimidated.",
        xpRequired  = 1500,
        // File: app/src/main/res/drawable/badge_sovereign.xml
        badgeResId  = R.drawable.badge_sovereign
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// MISSION DATA CLASS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mission — a single daily mission assignment.
 *
 * @param id                  Stable unique string ID. e.g. "cold_open".
 *                            Never change this after shipping — it is used as the
 *                            missionId in CompletedMissionEntry history records.
 *
 * @param title               Display title. e.g. "The Cold Open".
 *
 * @param objective           Single sentence: what the user must do.
 *
 * @param rules               Ordered list of specific constraints.
 *                            Displayed as a numbered list in the mission card.
 *
 * @param xpReward            XP awarded on completion.
 *
 * @param status              Current lifecycle state of the mission.
 *                            Set to ACTIVE for all missions in the repository.
 *                            Updated by DashboardViewModel as the user progresses.
 *
 * @param difficulty          1–5 difficulty rating. Shown as filled/empty dots on the card.
 *
 * @param dateAssigned        The date this mission was assigned, "yyyy-MM-dd".
 *                            Set by DashboardViewModel when the mission is loaded for the day.
 *                            Empty string ("") in the repository — filled at runtime.
 *
 * @param isLocationDependent True if the mission requires the user to leave the house.
 *                            When true, the "SWAP MISSION" button is shown on the card.
 *                            (In v5, SWAP MISSION shows a motivational popup instead of swapping.)
 *
 * @param discomfortRating    The user's 1–10 self-reported discomfort rating after completion.
 *                            Null until the user submits a rating.
 *
 * @param category            The type of discomfort this mission targets. (NEW in v2)
 *                            Used by History and Stats screens.
 */
data class Mission(
    val id: String,
    val title: String,
    val objective: String,
    val rules: List<String>,
    val xpReward: Int,
    val status: MissionStatus,
    val difficulty: Int,
    val dateAssigned: String,
    val isLocationDependent: Boolean = false,
    val discomfortRating: Int? = null,
    val category: MissionCategory = MissionCategory.SOCIAL_INITIATION   // NEW in v2
)

// ─────────────────────────────────────────────────────────────────────────────
// XP PROGRESS DATA CLASS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * XpProgress — a snapshot of the user's current XP and rank progress.
 *
 * Computed by DashboardViewModel.calculateXpProgress() from the user's total XP.
 * Passed to XpProgressSection and DailyCompleteScreen for display.
 *
 * @param currentXp         The user's total accumulated XP.
 * @param currentRank       The rank the user currently holds (based on total XP).
 * @param xpForNextRank     The total XP needed to reach the next rank.
 *                          0 if the user is at max rank (Level 5).
 * @param xpUntilNextRank   How many more XP the user needs to rank up.
 *                          0 if the user is at max rank.
 * @param progressFraction  A 0.0–1.0 float representing fill level of the XP progress bar.
 *                          0.0 = just entered this rank. 1.0 = about to rank up.
 *                          Used by animateFloatAsState in XpProgressSection.
 */
data class XpProgress(
    val currentXp: Int,
    val currentRank: Rank,
    val xpForNextRank: Int,
    val xpUntilNextRank: Int,
    val progressFraction: Float
)
