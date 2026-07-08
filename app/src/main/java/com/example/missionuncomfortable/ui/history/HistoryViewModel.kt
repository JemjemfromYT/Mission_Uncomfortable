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
 *   HistoryViewModel.kt    — Reads history from SharedPreferences JSON (this file)
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
 * HistoryViewModel.kt
 *
 * ViewModel for the Mission History screen.
 *
 * ─── WHAT THIS FILE DOES ─────────────────────────────────────────────────────
 *
 *   Reads the "mission_history_json" SharedPreferences key, parses the JSON
 *   array into a List<CompletedMissionEntry>, and exposes it to HistoryScreen
 *   via LiveData. The list is already sorted reverse-chronologically (most
 *   recent first) because DashboardViewModel prepends new entries.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/history/HistoryViewModel.kt
 *
 * ─── JSON FORMAT ─────────────────────────────────────────────────────────────
 *
 *   Each entry in the JSON array looks like:
 *   {
 *     "date":             "2026-07-06",
 *     "missionId":        "cold_open",
 *     "missionTitle":     "The Cold Open",
 *     "discomfortRating": 8,
 *     "xpEarned":         100,
 *     "category":         "SOCIAL_INITIATION"
 *   }
 *
 *   The array is stored as a plain JSON string. No Gson or Moshi dependency —
 *   we parse it manually with simple string operations. This keeps the
 *   dependency count low during early development. Replace with a Room DAO
 *   when the database is set up.
 *
 * ─── FUTURE WORK ─────────────────────────────────────────────────────────────
 *
 *   - Replace manual JSON parsing with Room database + DAO.
 *   - Add a filter(category) function for the Stats/History filter UI.
 *   - Add a sortBy(field) function.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial version. Introduced for Phase 6 (Mission History Screen).
 */

package com.example.missionuncomfortable.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    // ── SHARED PREFERENCES ────────────────────────────────────────────────────
    // Same preferences file used by DashboardViewModel — they share data through
    // SharedPreferences as the interim storage layer.
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    // ── KEY CONSTANTS ─────────────────────────────────────────────────────────
    private companion object {
        const val KEY_HISTORY_JSON = "mission_history_json"   // JSON array of completed entries
    }

    // ── LIVE DATA ─────────────────────────────────────────────────────────────

    /**
     * entries — the full mission history in reverse chronological order.
     *
     * Observed by HistoryScreen. Populated on init by loadHistory().
     * Most recent mission is at index 0 (prepended when saved in DashboardViewModel).
     */
    private val _entries = MutableLiveData<List<CompletedMissionEntry>>(emptyList())
    val entries: LiveData<List<CompletedMissionEntry>> = _entries

    // ── INITIALISATION ────────────────────────────────────────────────────────

    init {
        // Load history immediately when the ViewModel is created.
        // This is fast — it is a simple SharedPreferences read + string parse.
        loadHistory()
    }

    // ── PRIVATE FUNCTIONS ─────────────────────────────────────────────────────

    /**
     * loadHistory — reads and parses the mission history from SharedPreferences.
     *
     * Reads the raw JSON string from SharedPreferences, splits it into individual
     * entry JSON objects, parses each one into a CompletedMissionEntry, and posts
     * the resulting list to the _entries LiveData.
     *
     * If the JSON is missing or malformed, posts an empty list instead of crashing.
     *
     * The JSON was written by DashboardViewModel.onSubmitReflection(). Format:
     *   [{"date":"...","missionId":"...","missionTitle":"...","discomfortRating":8,"xpEarned":100,"category":"..."},...]
     */
    private fun loadHistory() {
        val json = prefs.getString(KEY_HISTORY_JSON, "[]") ?: "[]"
        val trimmed = json.trim()

        // If the array is empty, push an empty list immediately and return.
        if (trimmed == "[]") {
            _entries.value = emptyList()
            return
        }

        // Parse the JSON array manually.
        // We strip the outer [ ] brackets, then split on "},{" to isolate each entry.
        // This works because our JSON values never contain "},{" — they are dates,
        // IDs, titles, and numbers. No nested objects or arrays.
        val inner = trimmed.substring(1, trimmed.length - 1)  // Remove outer [ ]
        val rawEntries = inner.split("},{").map { raw ->
            // Re-add the braces that split() stripped
            when {
                !raw.startsWith("{") && !raw.endsWith("}") -> "{$raw}"
                !raw.startsWith("{")                       -> "{$raw"
                !raw.endsWith("}")                         -> "$raw}"
                else                                       -> raw
            }
        }

        // Parse each raw JSON object into a CompletedMissionEntry
        val parsed = rawEntries.mapNotNull { parseEntry(it) }
        _entries.value = parsed
    }

    /**
     * parseEntry — parses a single JSON object string into a CompletedMissionEntry.
     *
     * Uses simple key extraction rather than a full JSON parser.
     * Returns null if any required field is missing or unparseable —
     * the caller (loadHistory) uses mapNotNull to skip bad entries silently.
     *
     * @param json  A single JSON object string, e.g.:
     *              {"date":"2026-07-06","missionId":"cold_open","missionTitle":"The Cold Open","discomfortRating":8,"xpEarned":100,"category":"SOCIAL_INITIATION"}
     */
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
            // If any field fails to parse, skip this entry rather than crashing.
            // This handles any edge cases from manual JSON construction.
            null
        }
    }

    /**
     * extractString — extracts a String value from a flat JSON object string.
     *
     * Finds the pattern: "key":"value" and returns value.
     * Assumes the value does not contain escaped quotes.
     *
     * @param json  The JSON object string to search.
     * @param key   The key whose value we want to extract.
     */
    private fun extractString(json: String, key: String): String {
        val marker = "\"$key\":\""
        val start = json.indexOf(marker) + marker.length
        val end = json.indexOf("\"", start)
        return json.substring(start, end)
    }

    /**
     * extractInt — extracts an Int value from a flat JSON object string.
     *
     * Finds the pattern: "key":number and returns number as an Int.
     *
     * @param json  The JSON object string to search.
     * @param key   The key whose value we want to extract.
     */
    private fun extractInt(json: String, key: String): Int {
        val marker = "\"$key\":"
        val start = json.indexOf(marker) + marker.length
        // Read digits until we hit a non-digit character (comma, }, space)
        val end = json.indexOfFirst { it == ',' || it == '}' || it == ' ' }.let { idx ->
            var i = start
            while (i < json.length && json[i].isDigit()) i++
            i
        }
        return json.substring(start, end).trim().toInt()
    }

    /**
     * refresh — re-reads the history from SharedPreferences.
     *
     * Call this if the history screen is shown after returning from the Dashboard
     * and a new mission may have been completed since the ViewModel was created.
     */
    fun refresh() {
        loadHistory()
    }
}
