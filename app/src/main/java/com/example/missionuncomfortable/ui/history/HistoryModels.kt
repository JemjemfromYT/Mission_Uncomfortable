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
 *   DashboardModels.kt     — All enums + data classes (Mission, Rank, XpProgress, MissionCategory)
 *   MissionRepository.kt   — Full mission library: 25 missions, 5 XP tiers, 6 categories
 *   DashboardViewModel.kt  — All game logic: XP, streaks, rank-ups, history saving, core loop
 *   DashboardScreen.kt     — Main UI. v5: SWAP MISSION button shows a popup, does NOT swap.
 *   HistoryModels.kt       — CompletedMissionEntry data class (this file)
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
 * HistoryModels.kt
 *
 * Data models for the Mission History screen.
 *
 * ─── WHAT THIS FILE CONTAINS ────────────────────────────────────────────────
 *
 *   CompletedMissionEntry — a single record representing one completed mission.
 *                           Stored as a JSON array in SharedPreferences under
 *                           the key "mission_history_json" until a Room database
 *                           is set up. These records are write-once — they never
 *                           change after being saved.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/history/HistoryModels.kt
 *
 * ─── HOW DATA FLOWS ──────────────────────────────────────────────────────────
 *
 *   1. User submits discomfort rating → DashboardViewModel.onSubmitReflection()
 *   2. ViewModel creates a CompletedMissionEntry and serialises it to JSON
 *   3. JSON is prepended to the existing "mission_history_json" array in SharedPreferences
 *   4. HistoryViewModel reads the JSON array, deserialises it back to a List<CompletedMissionEntry>
 *   5. HistoryScreen renders the list in reverse chronological order
 *
 * ─── FUTURE WORK ─────────────────────────────────────────────────────────────
 *
 *   Replace SharedPreferences JSON storage with a Room database.
 *   CompletedMissionEntry maps cleanly to a Room @Entity — all fields are
 *   primitive types (String, Int) so migration will be straightforward.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial version. Introduced for Phase 6 (Mission History Screen).
 */

package com.example.missionuncomfortable.ui.history

/**
 * CompletedMissionEntry — a single record in the user's mission history.
 *
 * Represents one completed mission. Created in DashboardViewModel.onSubmitReflection()
 * after the user submits their discomfort rating.
 *
 * Stored as JSON in SharedPreferences until Room is set up.
 * These are write-once records — once a mission is completed and saved,
 * the entry never changes.
 *
 * @param date              The date the mission was completed, formatted as "yyyy-MM-dd".
 *                          e.g. "2026-07-06". Used as the display date on the history row.
 *
 * @param missionId         The mission's stable unique identifier. e.g. "cold_open".
 *                          Matches the `id` field on the Mission data class.
 *                          Stable across app updates — do not change mission IDs once released.
 *
 * @param missionTitle      The human-readable mission title. e.g. "The Cold Open".
 *                          Stored here (not looked up from MissionRepository) so history
 *                          entries remain correct even if a mission's title changes in a future
 *                          app update.
 *
 * @param discomfortRating  The user's self-reported discomfort level, 1–10.
 *                          1 = barely uncomfortable. 10 = extreme discomfort.
 *                          Displayed as a visual bar on the history row.
 *
 * @param xpEarned          The XP the user received for completing this mission.
 *                          Stored here rather than looked up so the history remains accurate
 *                          even if a mission's XP reward changes in a future app update.
 *
 * @param category          The mission's category as a String.
 *                          e.g. "SOCIAL_INITIATION", "EYE_CONTACT", "PHYSICAL".
 *                          Stored as a String (not the enum) so it can be serialised/deserialised
 *                          to/from JSON without a Gson dependency.
 *                          Used by the Stats screen to compute "most completed category".
 */
data class CompletedMissionEntry(
    val date: String,
    val missionId: String,
    val missionTitle: String,
    val discomfortRating: Int,
    val xpEarned: Int,
    val category: String
)
