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
 *   DashboardViewModel.kt  — All game logic: XP, streaks, rank-ups, history saving, core loop ← YOU ARE HERE
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
 * ── SHAREDPREFERENCES KEYS — all keys used in this ViewModel ─────────────────
 *
 *   "total_xp"             → Int.   Total accumulated XP. Never reset.
 *   "last_completed_date"  → String "yyyy-MM-dd". Last date a mission was completed.
 *   "mission_status"       → String "ACTIVE" | "IN_PROGRESS" | "COMPLETED". Today's status.
 *   "mission_id_today"     → String. Mission id assigned today. Used to detect day rollover.
 *   "streak_count"         → Int.   Current consecutive-day streak.
 *   "last_streak_date"     → String "yyyy-MM-dd". Last date streak was updated.
 *   "best_streak"          → Int.   All-time best streak.
 *   "mission_history_json" → String. JSON array of CompletedMissionEntry objects.
 *                            Newest entry is first (prepended on each save).
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these rules, the developer will have to   ║
 * ║  manually fix every file you touch. Please respect these conventions.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * DashboardViewModel.kt
 *
 * The ViewModel for all dashboard screens. Single source of truth for mission
 * state, XP, streaks, and rank. All UI state flows from this class.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/dashboard/DashboardViewModel.kt
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Original. ACTIVE, COMPLETED. onAcceptMission sets COMPLETED.
 *
 *   v2 — Phase 3, 4, 6, 7 combined update:
 *
 *          PHASE 3 — Fixed core loop:
 *            - MissionStatus.IN_PROGRESS added. onAcceptMission() → IN_PROGRESS.
 *            - onMissionDone() added — user tapped "I DID IT". Shows slider.
 *            - onMissionAbandoned() added — user tapped "I COULDN'T DO IT".
 *            - onSubmitReflection() now awards XP, saves history entry, checks rank-up.
 *            - UiState: added showReturnButtons, showDidItButton, showCouldntButton.
 *            - Mission status persisted to SharedPreferences so it survives app backgrounding.
 *
 *          PHASE 4 — Streak system:
 *            - streakCount: Int added to UiState.
 *            - isPersonalBest: Boolean added to UiState.
 *            - updateStreak() saves today's date and increments streak count.
 *            - SharedPreferences key KEY_STREAK_COUNT and KEY_LAST_STREAK_DATE added.
 *
 *          PHASE 6 — History saving:
 *            - saveHistoryEntry() added — prepends a CompletedMissionEntry to
 *              the "mission_history_json" SharedPreferences key as a JSON string.
 *
 *          PHASE 7 — Rank-up event:
 *            - rankUpEvent: Rank? added to UiState.
 *            - onRankUpCelebrationComplete() added — clears rankUpEvent, sets isDailyComplete.
 *            - onSubmitReflection() compares old rank level vs new rank level.
 *              If newRank.level > oldRank.level → sets rankUpEvent = newRank.
 */

package com.example.missionuncomfortable.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.missionuncomfortable.data.MissionRepository
import com.example.missionuncomfortable.ui.history.CompletedMissionEntry
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// UI STATE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DashboardUiState — the complete state of the Dashboard screen at any moment.
 *
 * Emitted as a single object so the composable has one source of truth.
 * Every field defaults to a safe value so the composable never crashes on
 * first emission before data loads.
 */
data class DashboardUiState(
    // ── Loading + Error ────────────────────────────────────────────────────
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    // ── Mission ────────────────────────────────────────────────────────────
    val todaysMission: Mission? = null,

    // ── Core Loop State ────────────────────────────────────────────────────
    // Phase 3:
    //   showReturnButtons = true when mission is IN_PROGRESS
    //   (shows "I DID IT" and "I COULDN'T DO IT" side by side)
    val showReturnButtons: Boolean = false,
    //   showReflectionSlider = true after "I DID IT" is tapped
    val showReflectionSlider: Boolean = false,
    val currentDiscomfortRating: Int = 5,

    // ── Completion ─────────────────────────────────────────────────────────
    val isDailyComplete: Boolean = false,
    val xpProgress: XpProgress? = null,

    // ── Streak (Phase 4) ───────────────────────────────────────────────────
    val streakCount: Int = 0,
    val isPersonalBest: Boolean = false,

    // ── Rank-Up Event (Phase 7) ────────────────────────────────────────────
    // Non-null when a rank-up just occurred. DashboardScreen shows RankUpScreen.
    // Cleared by onRankUpCelebrationComplete().
    val rankUpEvent: Rank? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // ── SHARED PREFERENCES ────────────────────────────────────────────────────
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    // ── DATE FORMATTER ────────────────────────────────────────────────────────
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
    private fun today(): String = dateFormat.format(Date())

    // ── SHARED PREFERENCES KEYS ───────────────────────────────────────────────
    private companion object {
        // XP + Rank
        const val KEY_TOTAL_XP             = "total_xp"
        const val KEY_LAST_COMPLETED_DATE  = "last_completed_date"

        // Mission state persistence (Phase 3)
        const val KEY_MISSION_STATUS       = "mission_status"        // "ACTIVE" | "IN_PROGRESS" | "COMPLETED"
        const val KEY_MISSION_ID           = "mission_id_today"      // today's mission id

        // Streak (Phase 4)
        const val KEY_STREAK_COUNT         = "streak_count"
        const val KEY_LAST_STREAK_DATE     = "last_streak_date"
        const val KEY_BEST_STREAK          = "best_streak"

        // History (Phase 6)
        const val KEY_HISTORY_JSON         = "mission_history_json"
    }

    // ── LIVE DATA ─────────────────────────────────────────────────────────────
    private val _uiState = MutableLiveData(DashboardUiState())
    val uiState: LiveData<DashboardUiState> = _uiState

    // ── INITIALISATION ────────────────────────────────────────────────────────
    init {
        loadInitialData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: LOAD INITIAL DATA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * loadInitialData — called once on init.
     *
     * 1. Gets today's mission from MissionRepository.
     * 2. Restores mission status from SharedPreferences (so IN_PROGRESS survives app kill).
     * 3. Loads total XP and computes XpProgress.
     * 4. Loads streak count.
     * 5. Checks if today has already been completed.
     */
    private fun loadInitialData() {
        val todayString = today()
        val mission     = MissionRepository.getMissionForToday()

        // ── Restore mission status from SharedPreferences ─────────────────────
        // If the user backgrounded the app while a mission was IN_PROGRESS,
        // restore that state rather than showing ACCEPT again.
        val savedStatus = prefs.getString(KEY_MISSION_STATUS, "ACTIVE")
        val savedMissionId = prefs.getString(KEY_MISSION_ID, "")
        val isSameMission = savedMissionId == mission.id   // Different mission = new day

        val missionStatus = if (isSameMission) {
            when (savedStatus) {
                "IN_PROGRESS" -> MissionStatus.IN_PROGRESS
                "COMPLETED"   -> MissionStatus.COMPLETED
                else          -> MissionStatus.ACTIVE
            }
        } else {
            // New day — reset status
            prefs.edit()
                .putString(KEY_MISSION_STATUS, "ACTIVE")
                .putString(KEY_MISSION_ID, mission.id)
                .apply()
            MissionStatus.ACTIVE
        }

        val restoredMission = mission.copy(
            status       = missionStatus,
            dateAssigned = todayString
        )

        // ── Load XP ───────────────────────────────────────────────────────────
        val totalXp  = prefs.getInt(KEY_TOTAL_XP, 0)
        val xpProgress = calculateXpProgress(totalXp)

        // ── Load streak ───────────────────────────────────────────────────────
        val streakCount = prefs.getInt(KEY_STREAK_COUNT, 0)

        // ── Check if today is already complete ────────────────────────────────
        val lastCompleted = prefs.getString(KEY_LAST_COMPLETED_DATE, "")
        val isDailyComplete = missionStatus == MissionStatus.COMPLETED

        _uiState.value = DashboardUiState(
            isLoading             = false,
            todaysMission         = restoredMission,
            showReturnButtons     = missionStatus == MissionStatus.IN_PROGRESS,
            showReflectionSlider  = false,
            currentDiscomfortRating = 5,
            isDailyComplete       = isDailyComplete,
            xpProgress            = xpProgress,
            streakCount           = streakCount
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC: USER ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * onAcceptMission — user tapped "ACCEPT THE MISSION".
     *
     * Transitions mission from ACTIVE → IN_PROGRESS.
     * Persists the new status to SharedPreferences.
     * Shows the return buttons ("I DID IT" / "I COULDN'T DO IT").
     */
    fun onAcceptMission() {
        val current = _uiState.value ?: return
        val mission = current.todaysMission ?: return

        // Persist IN_PROGRESS to SharedPreferences
        prefs.edit()
            .putString(KEY_MISSION_STATUS, "IN_PROGRESS")
            .putString(KEY_MISSION_ID, mission.id)
            .apply()

        _uiState.value = current.copy(
            todaysMission     = mission.copy(status = MissionStatus.IN_PROGRESS),
            showReturnButtons = true
        )
    }

    /**
     * onMissionDone — user returned and tapped "I DID IT".
     *
     * Phase 3: Shows the discomfort rating slider.
     * The user will then rate their discomfort and tap SUBMIT RATING.
     */
    fun onMissionDone() {
        val current = _uiState.value ?: return
        _uiState.value = current.copy(
            showReturnButtons    = false,
            showReflectionSlider = true
        )
    }

    /**
     * onMissionAbandoned — user returned and tapped "I COULDN'T DO IT".
     *
     * Phase 3: No XP awarded. Resets mission status back to ACTIVE
     * so the user can try again today.
     *
     * A compassionate response — not punitive. The user can try again.
     */
    fun onMissionAbandoned() {
        val current = _uiState.value ?: return
        val mission = current.todaysMission ?: return

        prefs.edit()
            .putString(KEY_MISSION_STATUS, "ACTIVE")
            .apply()

        _uiState.value = current.copy(
            todaysMission     = mission.copy(status = MissionStatus.ACTIVE),
            showReturnButtons = false,
            showReflectionSlider = false
        )
    }

    /**
     * onDiscomfortRatingChanged — user moved the slider.
     *
     * Updates the currentDiscomfortRating in UiState. No side effects.
     *
     * @param rating  New slider value, 1–10.
     */
    fun onDiscomfortRatingChanged(rating: Int) {
        val current = _uiState.value ?: return
        _uiState.value = current.copy(currentDiscomfortRating = rating)
    }

    /**
     * onSubmitReflection — user tapped "SUBMIT RATING".
     *
     * This is the completion event. Everything happens here:
     *
     *   1. Award XP (Phase 3)
     *   2. Update streak (Phase 4)
     *   3. Save history entry (Phase 6)
     *   4. Check for rank-up (Phase 7)
     *   5. If ranked up → set rankUpEvent = newRank (Phase 7)
     *   6. If no rank-up → set isDailyComplete = true
     */
    fun onSubmitReflection() {
        val current = _uiState.value ?: return
        val mission = current.todaysMission ?: return
        val rating  = current.currentDiscomfortRating

        // ── Step 1: Award XP ──────────────────────────────────────────────────
        val oldTotalXp  = prefs.getInt(KEY_TOTAL_XP, 0)
        val newTotalXp  = oldTotalXp + mission.xpReward
        prefs.edit().putInt(KEY_TOTAL_XP, newTotalXp).apply()

        // Compute old rank and new rank
        val oldXpProgress = calculateXpProgress(oldTotalXp)
        val newXpProgress = calculateXpProgress(newTotalXp)

        // ── Step 2: Update streak ─────────────────────────────────────────────
        val newStreakCount = updateStreak()
        val bestStreak    = prefs.getInt(KEY_BEST_STREAK, 0)
        val isPersonalBest = newStreakCount > bestStreak
        if (isPersonalBest) {
            prefs.edit().putInt(KEY_BEST_STREAK, newStreakCount).apply()
        }

        // ── Step 3: Save history entry ────────────────────────────────────────
        saveHistoryEntry(
            mission  = mission,
            rating   = rating,
            xpEarned = mission.xpReward
        )

        // ── Step 4: Mark mission as completed in SharedPreferences ────────────
        prefs.edit()
            .putString(KEY_MISSION_STATUS, "COMPLETED")
            .putString(KEY_LAST_COMPLETED_DATE, today())
            .apply()

        val completedMission = mission.copy(
            status           = MissionStatus.COMPLETED,
            discomfortRating = rating
        )

        // ── Step 5: Check for rank-up ─────────────────────────────────────────
        val didRankUp = newXpProgress.currentRank.level > oldXpProgress.currentRank.level

        _uiState.value = current.copy(
            todaysMission        = completedMission,
            showReflectionSlider = false,
            showReturnButtons    = false,
            xpProgress           = newXpProgress,
            streakCount          = newStreakCount,
            isPersonalBest       = isPersonalBest,
            // Phase 7: set rankUpEvent if ranked up; otherwise mark complete immediately
            rankUpEvent          = if (didRankUp) newXpProgress.currentRank else null,
            isDailyComplete      = !didRankUp   // Deferred to after celebration if ranked up
        )
    }

    /**
     * onRankUpCelebrationComplete — user tapped CONTINUE on the RankUpScreen.
     *
     * Phase 7: Clears the rankUpEvent and sets isDailyComplete = true.
     * This causes DashboardScreen to transition to DailyCompleteScreen.
     */
    fun onRankUpCelebrationComplete() {
        val current = _uiState.value ?: return
        _uiState.value = current.copy(
            rankUpEvent     = null,
            isDailyComplete = true
        )
    }

    /**
     * onViewCompletion — user tapped "VIEW COMPLETION" from the daily complete screen.
     *
     * No-op currently. Reserved for future navigation to a full completion detail screen.
     */
    fun onViewCompletion() {
        // Future: navigate to a completion detail screen
    }

    /**
     * onSwapMission — user tapped the SWAP MISSION button.
     *
     * NOTE: In DashboardScreen v5, the SWAP MISSION button shows a motivational
     * popup (SwapBlockedDialog) instead of calling this function. This function
     * is retained for when swapping is re-enabled in a future version.
     */
    fun onSwapMission() {
        // Swapping is currently blocked at the UI level (DashboardScreen v5).
        // This function is a no-op until swapping is re-enabled.
        // Future: fetch MissionRepository.getAlternateMission() and update state.
    }

    /**
     * onMissionCardClicked — user tapped the mission card (not a button).
     *
     * Toggles an "expanded" state for the rules list.
     * Not yet implemented — reserved for a future expand/collapse animation.
     */
    fun onMissionCardClicked() {
        // Future: toggle mission card expanded state
    }

    /**
     * onRefresh — user triggered a manual refresh (pull-to-refresh or error retry).
     */
    fun onRefresh() {
        _uiState.value = DashboardUiState(isLoading = true)
        loadInitialData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: STREAK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * updateStreak — increments or resets the streak based on the last streak date.
     *
     * Rules:
     *   - If last streak date == yesterday → increment streak (consecutive day).
     *   - If last streak date == today     → no change (already counted today).
     *   - Any other date                   → reset streak to 1 (broken streak).
     *
     * Saves the new streak count and today's date to SharedPreferences.
     *
     * @return The new streak count.
     */
    private fun updateStreak(): Int {
        val todayString     = today()
        val lastStreakDate   = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        val currentStreak   = prefs.getInt(KEY_STREAK_COUNT, 0)

        // Compute yesterday's date string
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayString = dateFormat.format(cal.time)

        val newStreak = when (lastStreakDate) {
            todayString     -> currentStreak          // Already counted today — no change
            yesterdayString -> currentStreak + 1      // Consecutive — extend streak
            else            -> 1                      // Broken streak — reset to 1
        }

        prefs.edit()
            .putInt(KEY_STREAK_COUNT, newStreak)
            .putString(KEY_LAST_STREAK_DATE, todayString)
            .apply()

        return newStreak
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: HISTORY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * saveHistoryEntry — prepends a new CompletedMissionEntry to the history JSON.
     *
     * Reads the existing JSON array from SharedPreferences, creates a new entry
     * JSON object, and prepends it (most recent first). Writes the updated array back.
     *
     * The JSON is built manually (no Gson/Moshi dependency) using string concatenation.
     * This matches the parsing logic in HistoryViewModel and StatsViewModel.
     *
     * Format of the stored JSON array:
     *   [{"date":"2026-07-06","missionId":"cold_open","missionTitle":"The Cold Open","discomfortRating":8,"xpEarned":100,"category":"SOCIAL_INITIATION"},...]
     *
     * @param mission   The mission that was just completed.
     * @param rating    The user's discomfort rating (1–10).
     * @param xpEarned  The XP awarded for this completion.
     */
    private fun saveHistoryEntry(mission: Mission, rating: Int, xpEarned: Int) {
        val existing = prefs.getString(KEY_HISTORY_JSON, "[]") ?: "[]"

        // Build the new entry as a JSON object string
        // We escape mission title manually since it could contain apostrophes or quotes.
        // For simplicity, we strip any double-quotes from the title to avoid breaking JSON.
        val safeTitle    = mission.title.replace("\"", "'")
        val categoryName = mission.category.name

        val newEntry = """{"date":"${today()}","missionId":"${mission.id}","missionTitle":"$safeTitle","discomfortRating":$rating,"xpEarned":$xpEarned,"category":"$categoryName"}"""

        // Prepend the new entry to the existing array
        val updatedJson = if (existing.trim() == "[]") {
            "[$newEntry]"
        } else {
            // Strip the leading '[' from the existing array and prepend the new entry
            "[" + newEntry + "," + existing.substring(1)
        }

        prefs.edit()
            .putString(KEY_HISTORY_JSON, updatedJson)
            .apply()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: XP CALCULATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * calculateXpProgress — derives the user's current rank and progress bar
     * fraction from their total XP.
     *
     * Finds the highest rank whose xpRequired is ≤ totalXp (= current rank).
     * The progress fraction fills from that rank's xpRequired to the next rank's
     * xpRequired.
     *
     * At max rank (Level 5), progressFraction = 1.0 and xpUntilNextRank = 0.
     *
     * @param totalXp  The user's total accumulated XP.
     */
    private fun calculateXpProgress(totalXp: Int): XpProgress {
        // Find current rank: the highest rank the user has earned
        val currentRank = ALL_RANKS.lastOrNull { it.xpRequired <= totalXp }
            ?: ALL_RANKS.first()   // Default to Level 1 if no rank qualifies

        // Find next rank
        val nextRank = ALL_RANKS.firstOrNull { it.level > currentRank.level }

        return if (nextRank == null) {
            // Max rank — progress bar full
            XpProgress(
                currentXp       = totalXp,
                currentRank     = currentRank,
                xpForNextRank   = 0,
                xpUntilNextRank = 0,
                progressFraction = 1f
            )
        } else {
            val xpIntoCurrentTier = totalXp - currentRank.xpRequired
            val xpNeededForTier   = nextRank.xpRequired - currentRank.xpRequired
            val fraction          = (xpIntoCurrentTier.toFloat() / xpNeededForTier.toFloat())
                .coerceIn(0f, 1f)

            XpProgress(
                currentXp        = totalXp,
                currentRank      = currentRank,
                xpForNextRank    = nextRank.xpRequired,
                xpUntilNextRank  = nextRank.xpRequired - totalXp,
                progressFraction = fraction
            )
        }
    }
}
