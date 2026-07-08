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
 *   HistoryModels.kt       — CompletedMissionEntry data class
 *   HistoryViewModel.kt    — Reads history from SharedPreferences JSON
 *   HistoryScreen.kt       — Scrollable mission history UI
 *   RankUpScreen.kt        — Full-screen rank-up celebration with spring animation
 *   StatsViewModel.kt      — Computes stats (streaks, category breakdown, avg discomfort) ← YOU ARE HERE
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
 * StatsViewModel.kt
 *
 * ViewModel for the Stats screen.
 *
 * ─── WHAT THIS FILE DOES ─────────────────────────────────────────────────────
 *
 *   Reads the mission history from SharedPreferences and computes summary stats:
 *     - Total missions completed
 *     - Average discomfort rating across all completions
 *     - Breakdown by category (count per MissionCategory)
 *     - Breakdown by day of week (count per weekday) — for the bar chart
 *     - Best streak (longest unbroken run of daily completions)
 *     - Current streak
 *
 *   All stats are derived purely from the history JSON — no separate counters
 *   are maintained. This keeps the data simple and self-consistent.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/stats/StatsViewModel.kt
 *
 * ─── FUTURE WORK ─────────────────────────────────────────────────────────────
 *
 *   - Replace SharedPreferences JSON parsing with Room DAO.
 *   - Add time-range filtering (last 7 days, last 30 days, all time).
 *   - Expose per-category average discomfort.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial version. Introduced for Phase 9 (Stats Screen).
 */

package com.example.missionuncomfortable.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.missionuncomfortable.ui.dashboard.MissionCategory
import com.example.missionuncomfortable.ui.history.CompletedMissionEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// STATS DATA CLASS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * StatsData — all computed stats for the Stats screen.
 *
 * Exposed as a single object from StatsViewModel so that StatsScreen
 * only needs to observe one LiveData.
 *
 * @param totalCompleted          Total number of missions completed all time.
 * @param averageDiscomfort       Average discomfort rating, 1.0–10.0. Null if no missions.
 * @param categoryBreakdown       Map from MissionCategory to count of completions.
 *                                May not contain all categories (missing = 0).
 * @param discomfortByDay         List of 7 floats: average discomfort for Mon–Sun.
 *                                Index 0 = Monday, index 6 = Sunday.
 *                                0f for days with no completions.
 * @param currentStreak           Number of consecutive days ending today (or yesterday)
 *                                where the user completed a mission.
 * @param bestStreak              Longest consecutive daily streak the user has ever had.
 */
data class StatsData(
    val totalCompleted: Int,
    val averageDiscomfort: Float?,
    val categoryBreakdown: Map<MissionCategory, Int>,
    val discomfortByDay: List<Float>,    // 7 values: Mon–Sun average discomfort
    val currentStreak: Int,
    val bestStreak: Int
)

// ─────────────────────────────────────────────────────────────────────────────
// STATS VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private companion object {
        const val KEY_HISTORY_JSON = "mission_history_json"
    }

    private val _stats = MutableLiveData<StatsData>()
    val stats: LiveData<StatsData> = _stats

    init {
        computeStats()
    }

    // ── PUBLIC API ────────────────────────────────────────────────────────────

    fun refresh() {
        computeStats()
    }

    // ── PRIVATE COMPUTATION ───────────────────────────────────────────────────

    private fun computeStats() {
        val entries = loadHistory()

        if (entries.isEmpty()) {
            _stats.value = StatsData(
                totalCompleted     = 0,
                averageDiscomfort  = null,
                categoryBreakdown  = emptyMap(),
                discomfortByDay    = List(7) { 0f },
                currentStreak      = 0,
                bestStreak         = 0
            )
            return
        }

        // ── TOTAL ─────────────────────────────────────────────────────────────
        val total = entries.size

        // ── AVERAGE DISCOMFORT ────────────────────────────────────────────────
        val avgDiscomfort = entries.map { it.discomfortRating }.average().toFloat()

        // ── CATEGORY BREAKDOWN ────────────────────────────────────────────────
        // Group entries by category string, map back to MissionCategory enum.
        val categoryBreakdown: Map<MissionCategory, Int> = entries
            .groupBy { entry ->
                // Parse the string back to enum; default to SOCIAL_INITIATION if unrecognised.
                try { MissionCategory.valueOf(entry.category) }
                catch (e: IllegalArgumentException) { MissionCategory.SOCIAL_INITIATION }
            }
            .mapValues { it.value.size }

        // ── DISCOMFORT BY DAY OF WEEK ─────────────────────────────────────────
        // Build a map of DayOfWeek (1=Mon ... 7=Sun) → list of ratings.
        // Then average per day. Index 0 in result = Monday.
        val ratingsByDow = mutableMapOf<Int, MutableList<Int>>()
        entries.forEach { entry ->
            try {
                val date = LocalDate.parse(entry.date, dateFormatter)
                val dow = date.dayOfWeek.value  // 1 = Monday, 7 = Sunday
                ratingsByDow.getOrPut(dow) { mutableListOf() }.add(entry.discomfortRating)
            } catch (e: Exception) {
                // Skip malformed dates
            }
        }
        // Build 7-item list: index 0 = Mon (dow=1), index 6 = Sun (dow=7)
        val discomfortByDay = (1..7).map { dow ->
            val ratings = ratingsByDow[dow]
            if (ratings.isNullOrEmpty()) 0f else ratings.average().toFloat()
        }

        // ── STREAKS ───────────────────────────────────────────────────────────
        val (currentStreak, bestStreak) = computeStreaks(entries)

        _stats.value = StatsData(
            totalCompleted     = total,
            averageDiscomfort  = avgDiscomfort,
            categoryBreakdown  = categoryBreakdown,
            discomfortByDay    = discomfortByDay,
            currentStreak      = currentStreak,
            bestStreak         = bestStreak
        )
    }

    /**
     * computeStreaks — derives currentStreak and bestStreak from the history entries.
     *
     * Extracts unique dates, sorts them, then traverses them to find runs of
     * consecutive days.
     *
     * currentStreak is the run that includes today or yesterday.
     * If the most recent entry is older than yesterday, currentStreak = 0.
     *
     * @return Pair(currentStreak, bestStreak)
     */
    private fun computeStreaks(entries: List<CompletedMissionEntry>): Pair<Int, Int> {
        val today     = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Unique completion dates, sorted ascending
        val dates = entries
            .mapNotNull { entry ->
                try { LocalDate.parse(entry.date, dateFormatter) }
                catch (e: Exception) { null }
            }
            .toSortedSet()
            .toList()

        if (dates.isEmpty()) return Pair(0, 0)

        // Walk the sorted dates and compute all streak lengths
        var maxStreak     = 1
        var currentLen    = 1
        var lastDate      = dates[0]

        for (i in 1 until dates.size) {
            val diff = dates[i].toEpochDay() - lastDate.toEpochDay()
            if (diff == 1L) {
                currentLen++
                if (currentLen > maxStreak) maxStreak = currentLen
            } else {
                currentLen = 1
            }
            lastDate = dates[i]
        }

        // currentStreak = length of the streak ending today or yesterday
        val mostRecent = dates.last()
        val currentStreak = when {
            mostRecent == today || mostRecent == yesterday -> {
                // Walk backwards from the most recent date to find the streak length
                var streak = 1
                var prev   = mostRecent
                for (i in dates.size - 2 downTo 0) {
                    val diff = prev.toEpochDay() - dates[i].toEpochDay()
                    if (diff == 1L) {
                        streak++
                        prev = dates[i]
                    } else {
                        break
                    }
                }
                streak
            }
            else -> 0   // Most recent completion was before yesterday — streak broken
        }

        return Pair(currentStreak, maxStreak)
    }

    // ── SHARED PREFERENCES JSON PARSING ──────────────────────────────────────
    // Identical to HistoryViewModel — duplicated here to avoid a shared dependency
    // until a Room DAO is set up.

    private fun loadHistory(): List<CompletedMissionEntry> {
        val json    = prefs.getString(KEY_HISTORY_JSON, "[]") ?: "[]"
        val trimmed = json.trim()
        if (trimmed == "[]") return emptyList()

        val inner      = trimmed.substring(1, trimmed.length - 1)
        val rawEntries = inner.split("},{").map { raw ->
            when {
                !raw.startsWith("{") && !raw.endsWith("}") -> "{$raw}"
                !raw.startsWith("{")                       -> "{$raw"
                !raw.endsWith("}")                         -> "$raw}"
                else                                       -> raw
            }
        }
        return rawEntries.mapNotNull { parseEntry(it) }
    }

    private fun parseEntry(json: String): CompletedMissionEntry? {
        return try {
            CompletedMissionEntry(
                date             = extractString(json, "date"),
                missionId        = extractString(json, "missionId"),
                missionTitle     = extractString(json, "missionTitle"),
                discomfortRating = extractInt(json, "discomfortRating"),
                xpEarned         = extractInt(json, "xpEarned"),
                category         = extractString(json, "category")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractString(json: String, key: String): String {
        val marker = "\"$key\":\""
        val start  = json.indexOf(marker) + marker.length
        val end    = json.indexOf("\"", start)
        return json.substring(start, end)
    }

    private fun extractInt(json: String, key: String): Int {
        val marker = "\"$key\":"
        val start  = json.indexOf(marker) + marker.length
        var i      = start
        while (i < json.length && json[i].isDigit()) i++
        return json.substring(start, i).trim().toInt()
    }
}
