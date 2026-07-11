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
 *   MissionRepository.kt   — Full mission library: 40 missions, 5 XP tiers, 6 categories
 *   DashboardViewModel.kt  — All game logic: XP, streaks, rank-ups, history saving, core loop ← YOU ARE HERE
 *   DashboardScreen.kt     — Main UI. v6: SWAP MISSION button is now real (difficulty-gated).
 *   HistoryModels.kt       — CompletedMissionEntry data class
 *   HistoryViewModel.kt    — Reads history from SharedPreferences JSON
 *   HistoryScreen.kt       — Scrollable mission history UI
 *   RankUpScreen.kt        — Full-screen rank-up celebration with spring animation
 *   StatsViewModel.kt      — Computes stats (streaks, category breakdown, avg discomfort)
 *   StatsScreen.kt         — Bar charts: category breakdown + discomfort by day of week
 *   AscensionLore.kt       — Story content + colour theme per rank (Ascension Book feature)
 *   AscensionBookScreen.kt — Full-screen storybook shown on rank promotion
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
 *   "mission_id_today"     → String. Mission id currently active today (original or swapped).
 *   "mission_date_today"   → String "yyyy-MM-dd". The calendar date when mission_id_today was set.
 *                            Used to detect a new day reliably, even when the user has swapped —
 *                            the old isSameMission check (savedMissionId == today's mission ID)
 *                            broke when the saved ID was a swapped mission's ID, not the repo's
 *                            getMissionForToday() ID. A date comparison is unambiguous.
 *   "has_swapped_today"    → Boolean. True if the user already used their one daily swap.
 *                            Reset to false each new day in loadInitialData().
 *   "streak_count"         → Int.   Current consecutive-day streak.
 *   "last_streak_date"     → String "yyyy-MM-dd". Last date streak was updated.
 *   "best_streak"          → Int.   All-time best streak.
 *   "mission_history_json" → String. JSON array of CompletedMissionEntry objects.
 *                            Newest entry is first (prepended on each save).
 *   "ascension_seen_levels" → String. Comma-separated list of Rank.level values
 *                            (e.g. "1,2,3") whose Ascension Book has already been
 *                            shown. Ensures each rank's book is shown exactly once.
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
 *
 *   v3 — Ascension Book feature:
 *            - ascensionEvent: Rank? added to UiState. Non-null when the full-screen
 *              Ascension Book (AscensionBookScreen.kt) should be shown.
 *            - New SharedPreferences key KEY_ASCENSION_SEEN tracks which rank levels
 *              have already had their book shown (comma-separated level list).
 *            - loadInitialData(): if the Observer (level 1) book has never been
 *              shown, sets ascensionEvent so it plays once on the user's first
 *              ever visit to the dashboard — before any XP has been earned.
 *            - onRankUpCelebrationComplete(): now reads the rank the user just
 *              achieved (from the rankUpEvent being cleared) and, if that rank's
 *              book hasn't been shown yet, sets ascensionEvent for it. This means
 *              the Ascension Book plays AFTER the existing RankUpScreen ceremony,
 *              not simultaneously with it — see NavGraph.kt's ROUTE: ASCENSION
 *              comment for the full sequencing explanation.
 *            - onAscensionBookDismissed() added — called when the user taps
 *              "CLOSE THE BOOK". Marks the rank's book as seen and clears
 *              ascensionEvent.
 *
 *   v4 — Real swap logic:
 *            - alternateMission: Mission? added to UiState. Non-null when a valid
 *              swap candidate exists within the allowed difficulty window for today's
 *              mission. Null if no compatible mission exists or user already swapped.
 *            - hasSwappedToday: Boolean added to UiState. True once the daily swap
 *              is used. Prevents multi-swap exploitation.
 *            - Two new SharedPreferences keys added:
 *                KEY_MISSION_DATE     — "yyyy-MM-dd" the date mission_id_today was set.
 *                KEY_HAS_SWAPPED_TODAY — Boolean flag for the daily swap limit.
 *            - loadInitialData() redesigned: day-detection now uses KEY_MISSION_DATE
 *              (date comparison) instead of mission-ID comparison. This is necessary
 *              because after a swap the saved mission ID is the swapped mission's ID,
 *              not getMissionForToday()'s ID — the old check incorrectly treated a
 *              swap as a new day, resetting the swapped mission back to the original.
 *            - onSwapMission() now performs a real mission swap:
 *                1. Reads alternateMission from UiState.
 *                2. Replaces todaysMission with it.
 *                3. Saves the new mission ID and sets has_swapped_today = true.
 *                4. Clears alternateMission (one swap per day).
 */

package com.example.missionuncomfortable.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    val rankUpEvent: Rank? = null,

    // ── Ascension Book Event (v3) ──────────────────────────────────────────
    // Non-null when the full-screen Ascension Book should be shown for the
    // given Rank. Set either:
    //   (a) on the very first ever load, for the Observer rank (see loadInitialData), or
    //   (b) after the RankUpScreen celebration completes for a new promotion
    //       (see onRankUpCelebrationComplete).
    // DashboardScreen shows AscensionBookScreen when this is non-null.
    // Cleared by onAscensionBookDismissed().
    val ascensionEvent: Rank? = null,

    // ── Swap State (v4) ────────────────────────────────────────────────────
    // alternateMission: the precomputed swap candidate within the difficulty
    // window [currentDifficulty-2, currentDifficulty]. Non-null means a swap
    // is available right now. Null means: no compatible mission exists, or
    // the user already swapped today, or the mission is no longer ACTIVE.
    // Cleared to null once the swap is executed (one swap per day).
    val alternateMission: Mission? = null,

    // hasSwappedToday: true once the user has used their one daily swap.
    // Displayed in DashboardScreen so the swap button knows to show a
    // "already swapped" blocked message instead of a swap dialog.
    val hasSwappedToday: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // ── SHARED PREFERENCES ────────────────────────────────────────────────────
    // Using getSharedPreferences directly — androidx.preference is not in this project's deps.
    // PREFS_NAME must match across DashboardViewModel, HistoryViewModel, and StatsViewModel.
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── DATE FORMATTER ────────────────────────────────────────────────────────
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
    private fun today(): String = dateFormat.format(Date())

    // ── SHARED PREFERENCES KEYS ───────────────────────────────────────────────
    private companion object {
        // SharedPreferences file name — must match in HistoryViewModel and StatsViewModel.
        // All three ViewModels read/write the same prefs file.
        const val PREFS_NAME = "mission_uncomfortable_prefs"

        // XP + Rank
        const val KEY_TOTAL_XP             = "total_xp"
        const val KEY_LAST_COMPLETED_DATE  = "last_completed_date"

        // Mission state persistence (Phase 3)
        const val KEY_MISSION_STATUS       = "mission_status"        // "ACTIVE" | "IN_PROGRESS" | "COMPLETED"
        const val KEY_MISSION_ID           = "mission_id_today"      // Active mission id (original or swapped)

        // v4: Date-based day detection — replaces the old isSameMission (mission-ID) check.
        // Reason: after a swap, KEY_MISSION_ID holds the swapped mission's ID, NOT the value
        // returned by getMissionForToday(). The old check treated a swap as a new day, silently
        // resetting the user's swapped mission back to the original on next launch. Using the
        // calendar date avoids this ambiguity — a new day is simply a new yyyy-MM-dd string.
        const val KEY_MISSION_DATE         = "mission_date_today"    // "yyyy-MM-dd" of current mission

        // v4: One-swap-per-day limit
        const val KEY_HAS_SWAPPED_TODAY    = "has_swapped_today"     // Boolean

        // Streak (Phase 4)
        const val KEY_STREAK_COUNT         = "streak_count"
        const val KEY_LAST_STREAK_DATE     = "last_streak_date"
        const val KEY_BEST_STREAK          = "best_streak"

        // History (Phase 6)
        const val KEY_HISTORY_JSON         = "mission_history_json"

        // Ascension Book (v3) — comma-separated Rank.level values already shown,
        // e.g. "1,2,3". Read via hasSeenAscension(), written via markAscensionSeen().
        const val KEY_ASCENSION_SEEN       = "ascension_seen_levels"
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
     * 1. Determines if we are on the same calendar day as the last saved mission.
     *    v4: Uses KEY_MISSION_DATE (date string comparison) instead of mission-ID
     *    comparison. The old isSameMission check (savedId == getMissionForToday().id)
     *    broke when the saved ID was a swapped mission's ID — a date comparison is
     *    unambiguous and survives swaps correctly.
     *
     * 2. If same day: restores the active mission (original or swapped) and its status.
     * 3. If new day: resets status, swap flag, and uses getMissionForToday() for a fresh start.
     * 4. Loads total XP, computes XpProgress.
     * 5. Loads streak count.
     * 6. v4: Computes the alternate swap mission from MissionRepository (null if already
     *    swapped, mission not ACTIVE, or no compatible difficulty exists).
     * 7. (v3) Checks if the Observer's Ascension Book has ever been shown. If not,
     *    sets ascensionEvent so it plays on this — the user's very first — visit.
     */
    private fun loadInitialData() {
        val todayString = today()

        // ── v4: DAY DETECTION via date string ──────────────────────────────────
        // savedDate is the date string written when the current mission was assigned.
        // If savedDate == todayString, we are on the same calendar day.
        // If they differ, it is a new day and we reset.
        val savedDate      = prefs.getString(KEY_MISSION_DATE, "") ?: ""
        val savedMissionId = prefs.getString(KEY_MISSION_ID, "") ?: ""
        val isSameDay      = savedDate == todayString

        // ── Determine active mission and swap state ────────────────────────────
        val activeMission: Mission
        val hasSwappedToday: Boolean

        if (isSameDay) {
            // ── SAME DAY: restore the mission that was active when the app was last open ─
            hasSwappedToday = prefs.getBoolean(KEY_HAS_SWAPPED_TODAY, false)

            // Look up the mission by its saved ID. This might be the original day's mission
            // OR a swapped mission — either way, find it by ID from the full library.
            val foundMission = MissionRepository.ALL_MISSIONS.find { it.id == savedMissionId }
                ?: MissionRepository.getMissionForToday()  // Fallback to today's if ID not found
            activeMission = foundMission

        } else {
            // ── NEW DAY: reset all per-day state ───────────────────────────────
            hasSwappedToday = false
            activeMission   = MissionRepository.getMissionForToday()

            // Persist the new day's baseline. Use commit() not apply() — Kotlin's
            // generic apply { } extension conflicts with SharedPreferences.Editor.apply()
            // when chained, causing a "Cannot infer type for T" compile error.
            val editor = prefs.edit()
            editor.putString(KEY_MISSION_STATUS, "ACTIVE")
            editor.putString(KEY_MISSION_ID, activeMission.id)
            editor.putString(KEY_MISSION_DATE, todayString)
            editor.putBoolean(KEY_HAS_SWAPPED_TODAY, false)
            editor.commit()
        }

        // ── Restore mission status from SharedPreferences ─────────────────────
        // If the user backgrounded the app while a mission was IN_PROGRESS,
        // restore that state rather than showing ACCEPT again.
        val savedStatus = prefs.getString(KEY_MISSION_STATUS, "ACTIVE") ?: "ACTIVE"
        val missionStatus = if (isSameDay) {
            when (savedStatus) {
                "IN_PROGRESS" -> MissionStatus.IN_PROGRESS
                "COMPLETED"   -> MissionStatus.COMPLETED
                else          -> MissionStatus.ACTIVE
            }
        } else {
            // New day — always start ACTIVE.
            MissionStatus.ACTIVE
        }

        val restoredMission = activeMission.copy(
            status       = missionStatus,
            dateAssigned = todayString
        )

        // ── Load XP ───────────────────────────────────────────────────────────
        val totalXp    = prefs.getInt(KEY_TOTAL_XP, 0)
        val xpProgress = calculateXpProgress(totalXp)

        // ── Load streak ───────────────────────────────────────────────────────
        val streakCount = prefs.getInt(KEY_STREAK_COUNT, 0)

        // ── Check if today is already complete ────────────────────────────────
        val isDailyComplete = missionStatus == MissionStatus.COMPLETED

        // ── v4: COMPUTE ALTERNATE MISSION FOR SWAP ────────────────────────────
        // Only compute if:
        //   (a) the user has NOT already swapped today, AND
        //   (b) the mission is still ACTIVE (can't swap after accepting or completing)
        // getAlternateMission() returns null if no mission within the difficulty
        // window [currentDifficulty-2, currentDifficulty] is found.
        val alternateMission: Mission? = if (!hasSwappedToday && missionStatus == MissionStatus.ACTIVE) {
            MissionRepository.getAlternateMission(activeMission.difficulty)?.copy(
                status       = MissionStatus.ACTIVE,
                dateAssigned = todayString
            )
        } else {
            null  // No swap available — either already swapped, or mission is in progress/complete
        }

        // ── v3: FIRST-EVER-LAUNCH ASCENSION BOOK (The Observer) ────────────────
        // The user starts at Level 1 (Observer) with no rank-up event ever firing
        // for it — onSubmitReflection() only fires rankUpEvent on a level INCREASE,
        // and there is no level below Observer to increase from. So the Observer's
        // book is triggered here instead: the very first time loadInitialData()
        // runs and the Observer book has never been marked as seen.
        val observerRank = ALL_RANKS.first()   // Level 1 — always Observer
        val shouldShowObserverBook = !hasSeenAscension(observerRank.level)

        _uiState.value = DashboardUiState(
            isLoading             = false,
            todaysMission         = restoredMission,
            showReturnButtons     = missionStatus == MissionStatus.IN_PROGRESS,
            showReflectionSlider  = false,
            currentDiscomfortRating = 5,
            isDailyComplete       = isDailyComplete,
            xpProgress            = xpProgress,
            streakCount           = streakCount,
            // v3: non-null only on the user's very first ever dashboard load.
            ascensionEvent        = if (shouldShowObserverBook) observerRank else null,
            // v4: precomputed swap candidate (null if swap unavailable)
            alternateMission      = alternateMission,
            hasSwappedToday       = hasSwappedToday
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
     *
     * v4: Also clears alternateMission — once the user has accepted the mission,
     * swapping is no longer an option (they already committed).
     */
    fun onAcceptMission() {
        val current = _uiState.value ?: return
        val mission = current.todaysMission ?: return

        // Persist IN_PROGRESS to SharedPreferences
        // Using explicit editor variable to avoid Kotlin apply { } ambiguity (see comment above).
        val editor = prefs.edit()
        editor.putString(KEY_MISSION_STATUS, "IN_PROGRESS")
        editor.putString(KEY_MISSION_ID, mission.id)
        editor.commit()

        _uiState.value = current.copy(
            todaysMission    = mission.copy(status = MissionStatus.IN_PROGRESS),
            showReturnButtons = true,
            // v4: Clear alternate — can't swap once the mission is IN_PROGRESS
            alternateMission = null
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
     *
     * v4: Restores the alternateMission if the user hadn't swapped yet — they
     * abandoned the mission and can now swap if they want to try a different one.
     */
    fun onMissionAbandoned() {
        val current = _uiState.value ?: return
        val mission = current.todaysMission ?: return

        val editor = prefs.edit()
        editor.putString(KEY_MISSION_STATUS, "ACTIVE")
        editor.commit()

        // v4: Recompute the alternate in case we need to re-offer the swap button.
        // This handles the case where the user accepted → abandoned → wants to swap.
        val todayString = today()
        val restoredAlternate: Mission? = if (!current.hasSwappedToday) {
            MissionRepository.getAlternateMission(mission.difficulty)?.copy(
                status       = MissionStatus.ACTIVE,
                dateAssigned = todayString
            )
        } else {
            null   // Already swapped today — no second swap offered
        }

        _uiState.value = current.copy(
            todaysMission        = mission.copy(status = MissionStatus.ACTIVE),
            showReturnButtons    = false,
            showReflectionSlider = false,
            alternateMission     = restoredAlternate   // v4: restore swap option if not yet used
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
     *
     * NOTE (v3): ascensionEvent is intentionally NOT set here even on a rank-up.
     * It is set later, in onRankUpCelebrationComplete(), so the Ascension Book
     * plays AFTER the RankUpScreen ceremony finishes rather than racing it.
     */
    fun onSubmitReflection() {
        val current = _uiState.value ?: return
        val mission = current.todaysMission ?: return
        val rating  = current.currentDiscomfortRating

        // ── Step 1: Award XP ──────────────────────────────────────────────────
        val oldTotalXp  = prefs.getInt(KEY_TOTAL_XP, 0)
        val newTotalXp  = oldTotalXp + mission.xpReward
        prefs.edit().putInt(KEY_TOTAL_XP, newTotalXp).commit()

        // Compute old rank and new rank
        val oldXpProgress = calculateXpProgress(oldTotalXp)
        val newXpProgress = calculateXpProgress(newTotalXp)

        // ── Step 2: Update streak ─────────────────────────────────────────────
        val newStreakCount = updateStreak()
        val bestStreak    = prefs.getInt(KEY_BEST_STREAK, 0)
        val isPersonalBest = newStreakCount > bestStreak
        if (isPersonalBest) {
            prefs.edit().putInt(KEY_BEST_STREAK, newStreakCount).commit()
        }

        // ── Step 3: Save history entry ────────────────────────────────────────
        saveHistoryEntry(
            mission  = mission,
            rating   = rating,
            xpEarned = mission.xpReward
        )

        // ── Step 4: Mark mission as completed in SharedPreferences ────────────
        val completionEditor = prefs.edit()
        completionEditor.putString(KEY_MISSION_STATUS, "COMPLETED")
        completionEditor.putString(KEY_LAST_COMPLETED_DATE, today())
        completionEditor.commit()

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
            isDailyComplete      = !didRankUp,   // Deferred to after celebration if ranked up
            // Swap is irrelevant once the mission is completed — clear it.
            alternateMission     = null
        )
    }

    /**
     * onRankUpCelebrationComplete — user tapped CONTINUE on the RankUpScreen.
     *
     * Phase 7: Clears the rankUpEvent and sets isDailyComplete = true.
     * This causes DashboardScreen to transition to DailyCompleteScreen.
     *
     * v3: Also decides whether to queue the Ascension Book. Reads the rank that
     * was just achieved (current.rankUpEvent, BEFORE it is cleared below) and,
     * if that rank's book has never been shown, sets ascensionEvent to it.
     * DashboardScreen's LaunchedEffect(uiState.ascensionEvent) then navigates to
     * AscensionBookScreen once this function returns to Dashboard. This ordering
     * — book AFTER celebration, not simultaneous with it — is intentional: the
     * badge ceremony and the storybook are two distinct beats, and showing them
     * both at once would undercut each other.
     */
    fun onRankUpCelebrationComplete() {
        val current = _uiState.value ?: return

        // Capture the rank being celebrated BEFORE we clear rankUpEvent below.
        val achievedRank = current.rankUpEvent
        val shouldShowBook = achievedRank != null && !hasSeenAscension(achievedRank.level)

        _uiState.value = current.copy(
            rankUpEvent     = null,
            isDailyComplete = true,
            // v3: queue the Ascension Book for this rank if it hasn't been seen yet.
            ascensionEvent  = if (shouldShowBook) achievedRank else null
        )
    }

    /**
     * onAscensionBookDismissed — user tapped "CLOSE THE BOOK" on AscensionBookScreen
     * and its closing fade animation finished.
     *
     * v3: New function. Marks the current ascensionEvent's rank as seen (so its
     * book never plays again) and clears ascensionEvent so DashboardScreen
     * returns to its normal state.
     */
    fun onAscensionBookDismissed() {
        val current = _uiState.value ?: return
        val rank = current.ascensionEvent ?: return

        markAscensionSeen(rank.level)

        _uiState.value = current.copy(ascensionEvent = null)
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
     * onSwapMission — user confirmed the swap in SwapConfirmDialog.
     *
     * v4 UPDATE: This function now performs a real mission swap.
     *
     * Pre-condition: uiState.alternateMission must be non-null (the Screen only calls
     * this after verifying alternateMission != null in the swap confirmation dialog).
     * If called when alternateMission is null, this is a no-op (defensive).
     *
     * What happens:
     *   1. Reads the precomputed alternateMission from UiState.
     *   2. Replaces todaysMission with the alternate.
     *   3. Saves the new mission ID to SharedPreferences (KEY_MISSION_ID), so the
     *      swapped mission is correctly restored on the next app launch.
     *   4. Sets KEY_HAS_SWAPPED_TODAY = true to enforce the one-swap-per-day limit.
     *   5. Clears alternateMission in UiState (prevents a second swap attempt).
     *   6. Sets hasSwappedToday = true in UiState.
     *
     * Note: KEY_MISSION_DATE is NOT updated — the date was set when the original
     * mission was assigned and does not change on swap. This correctly preserves the
     * "same day" detection so the swapped mission is restored instead of the original
     * on next launch.
     */
    fun onSwapMission() {
        val current   = _uiState.value ?: return
        val alternate = current.alternateMission ?: return  // Safety: no swap if no alternate

        // Persist the swapped mission ID and the swap flag
        val editor = prefs.edit()
        editor.putString(KEY_MISSION_ID, alternate.id)
        editor.putString(KEY_MISSION_STATUS, "ACTIVE")   // Swapped mission starts ACTIVE
        editor.putBoolean(KEY_HAS_SWAPPED_TODAY, true)
        editor.commit()

        _uiState.value = current.copy(
            todaysMission    = alternate,
            alternateMission = null,       // One swap per day — no further swaps
            hasSwappedToday  = true
        )
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

        val streakEditor = prefs.edit()
        streakEditor.putInt(KEY_STREAK_COUNT, newStreak)
        streakEditor.putString(KEY_LAST_STREAK_DATE, todayString)
        streakEditor.commit()

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

        prefs.edit().putString(KEY_HISTORY_JSON, updatedJson).commit()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: ASCENSION BOOK (v3)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * hasSeenAscension — checks whether the Ascension Book for [level] has
     * already been shown to this user.
     *
     * Reads KEY_ASCENSION_SEEN as a comma-separated string (e.g. "1,2") and
     * checks whether [level] is one of the entries. Returns false (never seen)
     * if the key doesn't exist yet, which is correct for a brand-new install.
     *
     * @param level  Rank.level to check (1–5).
     * @return       True if that rank's book has already been shown.
     */
    private fun hasSeenAscension(level: Int): Boolean {
        val seen = prefs.getString(KEY_ASCENSION_SEEN, "") ?: ""
        if (seen.isBlank()) return false
        return seen.split(",").contains(level.toString())
    }

    /**
     * markAscensionSeen — records that the Ascension Book for [level] has now
     * been shown, so it never plays again for this user.
     *
     * Reads the existing comma-separated list, appends [level] if it isn't
     * already present (defensive — should never already be present given the
     * call sites guard with hasSeenAscension() first), and writes it back.
     *
     * @param level  Rank.level to mark as seen (1–5).
     */
    private fun markAscensionSeen(level: Int) {
        val seen = prefs.getString(KEY_ASCENSION_SEEN, "") ?: ""
        val levels = if (seen.isBlank()) {
            mutableListOf()
        } else {
            seen.split(",").toMutableList()
        }

        val levelString = level.toString()
        if (!levels.contains(levelString)) {
            levels.add(levelString)
        }

        prefs.edit().putString(KEY_ASCENSION_SEEN, levels.joinToString(",")).commit()
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
