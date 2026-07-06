/**
 * DashboardViewModel.kt
 *
 * This is the ViewModel for the Dashboard screen.
 *
 * What is a ViewModel?
 *   A ViewModel sits between your UI (the composable screen) and your data sources (Room DB, Supabase).
 *   It survives screen rotations and config changes — so if the user rotates their phone,
 *   the data doesn't have to reload from scratch. The screen just reconnects to the same ViewModel.
 *
 * What does THIS ViewModel do?
 *   v4: It now loads and saves data using SharedPreferences so XP and daily state
 *   survive app restarts. It also selects today's mission from MissionRepository
 *   instead of showing the same hardcoded mission every time.
 *
 * Architecture note:
 *   This follows the MVVM pattern (Model-View-ViewModel):
 *     Model     = DashboardModels.kt (data classes) + MissionRepository.kt (mission list)
 *     View      = DashboardScreen.kt (composables)
 *     ViewModel = this file (business logic + state management)
 *
 * ─── CHANGELOG ────────────────────────────────────────────────────────────────
 *   v2 — Updated DashboardUiState to support the reflection slider:
 *          `showReflectionSlider` — true when the user has accepted the mission
 *                                   and the post-completion slider should be visible.
 *          `currentDiscomfortRating` — holds the slider's live value (1–10) before submission.
 *        Updated `loadPlaceholderData()` to use new Mission fields (objective, rules,
 *          isLocationDependent) introduced in DashboardModels v2.
 *        Added `onAcceptMission()` — called when the user taps "ACCEPT THE MISSION".
 *        Added `onSwapMission()` — stub for swapping a location-dependent mission.
 *        Added `onDiscomfortRatingChanged(rating)` — called as the slider moves.
 *        Added `onSubmitReflection()` — called when the user submits their discomfort rating.
 *
 *   v3 — LIVE XP ENGINE. Three major additions:
 *
 *        1. `isDailyComplete: Boolean` added to DashboardUiState.
 *           When true, the screen transitions to the "Come back tomorrow" completion view.
 *           Set by onViewCompletion() after the user taps "VIEW COMPLETION".
 *
 *        2. `onSubmitReflection()` now does REAL XP MATH:
 *           - Reads the mission's xpReward from state.
 *           - Adds it to the user's current total XP.
 *           - Calls calculateXpProgress(newTotalXp) to compute the new rank + bar position.
 *           - Pushes the updated XpProgress into state so the animated progress bar reacts.
 *
 *        3. `calculateXpProgress(totalXp)` — new private helper.
 *           Walks ALL_RANKS (defined in DashboardModels) to find the correct rank for any
 *           XP total. Returns a fully-populated XpProgress object ready for the UI.
 *           This means rank-ups happen automatically — no if/else chains needed here.
 *
 *        4. `onViewCompletion()` — new function.
 *           Called when the user taps "VIEW COMPLETION" on the completed mission card.
 *           Sets isDailyComplete = true, which triggers DashboardScreen to show the
 *           "Come back tomorrow" screen.
 *
 *   v4 — PERSISTENCE + MISSION LIBRARY.
 *
 *        1. Class changed from ViewModel() to AndroidViewModel(application).
 *           This gives us access to Application context for SharedPreferences.
 *           The Compose viewModel() call in DashboardScreen.kt needs NO changes —
 *           Compose automatically detects AndroidViewModel and passes Application.
 *
 *        2. SharedPreferences persistence added. Two keys in "mission_uncomfortable_prefs":
 *             "total_xp"            → Int    — the user's cumulative XP across all sessions
 *             "last_completed_date" → String — "yyyy-MM-dd" of the last time they submitted
 *
 *        3. `loadPlaceholderData()` replaced by `loadInitialData()`.
 *           On every cold start it:
 *             a. Reads saved total_xp from prefs (default 0).
 *             b. Reads last_completed_date and compares to today.
 *             c. Gets today's mission from MissionRepository.getMissionForToday().
 *             d. If user already completed today → mission is COMPLETED, isDailyComplete = true.
 *             e. Pushes the restored state into LiveData.
 *
 *        4. `onSubmitReflection()` now persists:
 *             Writes new total_xp and today's date to SharedPreferences after XP math.
 *
 *        5. `onSwapMission()` now implemented using
 *             MissionRepository.getAlternateMission(currentMission.id).
 *
 *        6. `onRefresh()` now reloads from prefs + MissionRepository instead of placeholder data.
 */

package com.example.missionuncomfortable.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.core.content.edit                  // KTX: enables the `edit { }` lambda on SharedPreferences
import androidx.lifecycle.AndroidViewModel         // v4: gives us Application context for SharedPreferences
import com.example.missionuncomfortable.data.MissionRepository
import java.time.LocalDate

/**
 * DashboardUiState — a single data class that holds EVERYTHING the Dashboard screen needs to display.
 *
 * By bundling all UI state into one object, the composable only needs to observe ONE thing.
 * When any field changes, Compose will re-compose only the parts of the screen that use that field.
 *
 * @param xpProgress               The user's current XP and rank progress data. Null while loading.
 * @param todaysMission            The mission assigned to the user today. Null if no mission yet.
 * @param isLoading                True while data is being fetched, false once data is ready.
 * @param errorMessage             If something went wrong loading data, this holds the error text.
 *                                 Null means no error.
 * @param showReflectionSlider     True when the user has accepted the mission and we should show
 *                                 the post-mission discomfort slider below the action buttons.
 *                                 False by default (slider hidden until the user accepts).
 * @param currentDiscomfortRating  The live value of the discomfort slider (1–10).
 *                                 Defaults to 5 (the midpoint). Updated as the slider moves.
 *                                 This is persisted to Mission.discomfortRating on submission.
 * @param isDailyComplete          v3: True once the user has tapped "VIEW COMPLETION" after
 *                                 submitting their discomfort rating for the day's mission.
 *                                 When true, DashboardScreen swaps to the "Come back tomorrow" view.
 *                                 Resets to false the next calendar day when loadInitialData() runs.
 */
data class DashboardUiState(
    val xpProgress: XpProgress? = null,          // Null = not yet loaded
    val todaysMission: Mission? = null,           // Null = not yet loaded or no mission today
    val isLoading: Boolean = true,                // Start in loading state by default
    val errorMessage: String? = null,            // Null = no error
    val showReflectionSlider: Boolean = false,   // False = slider hidden; true = slider visible
    val currentDiscomfortRating: Int = 5,        // Default slider position = 5 (middle of 1–10)
    val isDailyComplete: Boolean = false         // v3: True = show "Come back tomorrow" screen
)

/**
 * DashboardViewModel — manages the state for DashboardScreen.
 *
 * v4: Now extends AndroidViewModel so it can access Application context
 * for reading/writing SharedPreferences without needing a Context from the UI layer.
 *
 * The Compose `viewModel()` call in DashboardScreen.kt automatically provides
 * the Application — no ViewModelProvider.Factory changes needed.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // ─────────────────────────────────────────────────────────────────────────
    // SHARED PREFERENCES
    // v4: One SharedPreferences file shared across the whole app.
    // The same file ("mission_uncomfortable_prefs") is used by MainActivity
    // for the has_seen_welcome flag — keeping all prefs in one place.
    // ─────────────────────────────────────────────────────────────────────────

    private val prefs = application.getSharedPreferences(
        "mission_uncomfortable_prefs",
        Context.MODE_PRIVATE
    )

    // Key constants — defined here to avoid typo-prone magic strings
    // scattered across the ViewModel's functions.
    private companion object {
        const val KEY_TOTAL_XP            = "total_xp"             // Int  — cumulative XP earned
        const val KEY_LAST_COMPLETED_DATE = "last_completed_date"  // String — "yyyy-MM-dd"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI STATE
    // MutableLiveData holds a value that can change over time.
    // Compose bridges LiveData → State via observeAsState() in DashboardScreen.
    //
    // We expose it as a read-only LiveData to the screen — only the ViewModel
    // can change the value. The screen can only READ it.
    // ─────────────────────────────────────────────────────────────────────────

    // Internal mutable state — only this ViewModel can write to it
    private val _uiState = androidx.lifecycle.MutableLiveData<DashboardUiState>(
        DashboardUiState(isLoading = true)  // Start with loading=true, no data yet
    )

    // Public read-only state — the screen observes this
    val uiState: androidx.lifecycle.LiveData<DashboardUiState> = _uiState

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALIZER BLOCK
    // This runs automatically when the ViewModel is first created.
    // v4: Calls loadInitialData() which reads from SharedPreferences
    // instead of hardcoded placeholder values.
    // ─────────────────────────────────────────────────────────────────────────

    init {
        // Load real persisted data on every cold start.
        loadInitialData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPER — XP & RANK CALCULATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * calculateXpProgress — converts a raw total-XP number into a full XpProgress object.
     *
     * This is the core of the XP engine. It walks ALL_RANKS (defined in DashboardModels)
     * to find the correct current rank and the correct next-rank threshold for any XP value.
     * Because it reads thresholds directly from ALL_RANKS, rank-ups are automatic —
     * there is no if/else chain here that needs updating when rank thresholds change.
     *
     * How it works:
     *   1. Find the highest rank whose xpThreshold is still <= totalXp.
     *      That is the user's current rank.
     *      (e.g., totalXp=425 → thresholds 0, 200 both qualify → highest is Level 2)
     *   2. Find the rank at level + 1 — that is the next rank to reach.
     *   3. If the user is at the max rank (Level 5), use the user's own XP as the "next" threshold
     *      so the progress bar shows 100% full.
     *
     * @param totalXp  The user's total earned XP after the latest change.
     * @return         A fully-populated XpProgress object ready to hand to the UI.
     */
    private fun calculateXpProgress(totalXp: Int): XpProgress {
        // Walk ALL_RANKS from highest to lowest to find the current rank.
        // lastOrNull { condition } returns the last element that satisfies the condition —
        // since ALL_RANKS is ordered by ascending xpThreshold, this gives us the highest
        // qualifying rank without any manual sorting.
        val currentRank = ALL_RANKS.lastOrNull { rank ->
            rank.xpThreshold <= totalXp
        } ?: ALL_RANKS.first()  // Safety fallback: if somehow no rank qualifies, use Level 1

        // Find the next rank — the one with level = currentRank.level + 1.
        // null means the user is at max rank (Level 5) — there is no next rank.
        val nextRank = ALL_RANKS.find { rank -> rank.level == currentRank.level + 1 }

        return XpProgress(
            currentXp = totalXp,
            xpForCurrentRank = currentRank.xpThreshold,   // Bottom of the current rank's XP band
            // If max rank, use totalXp as the ceiling so progressFraction = 1.0 (full bar).
            // Otherwise use the next rank's threshold as the ceiling.
            xpForNextRank = nextRank?.xpThreshold ?: totalXp,
            currentRank = currentRank
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * loadInitialData — restores persisted state and loads today's mission.
     *
     * v4: Replaces the old loadPlaceholderData() function.
     *
     * What it does on every cold start:
     *   1. Reads `total_xp` from SharedPreferences (default 0 for new users).
     *   2. Reads `last_completed_date` and compares it to today's date.
     *      If they match, the user already finished today's mission this session
     *      or a previous session — we restore the completed/locked state.
     *   3. Asks MissionRepository.getMissionForToday() for today's mission.
     *      The repository uses the calendar date as a seed so the same mission
     *      is shown all day and a new one appears at midnight.
     *   4. If the user already completed today: the mission status is set to COMPLETED
     *      and isDailyComplete is set to true → the "Come back tomorrow" screen shows.
     *   5. Pushes the fully-restored DashboardUiState into LiveData.
     *
     * Future: replace the SharedPreferences reads with Room DAO calls for richer
     * history, streak tracking, and multi-device sync via Supabase.
     */
    private fun loadInitialData() {
        // ── STEP 1: Read persisted XP ─────────────────────────────────────
        // getInt returns the default (0) if the key has never been written —
        // i.e., this is a brand-new user with no XP yet.
        val savedXp = prefs.getInt(KEY_TOTAL_XP, 0)

        // ── STEP 2: Check if today's mission is already done ──────────────
        // getString returns "" if the key has never been written.
        val lastCompletedDate = prefs.getString(KEY_LAST_COMPLETED_DATE, "") ?: ""
        val todayString = LocalDate.now().toString()   // "yyyy-MM-dd"
        val alreadyCompletedToday = (lastCompletedDate == todayString)

        // ── STEP 3: Get today's mission from the repository ───────────────
        // getMissionForToday() uses the epoch day as a seed — deterministic per day.
        // We override the status if the user has already completed it.
        val todaysMission = MissionRepository.getMissionForToday().copy(
            status = if (alreadyCompletedToday) MissionStatus.COMPLETED else MissionStatus.ACTIVE
        )

        // ── STEP 4: Calculate rank + bar fraction from restored XP ────────
        val xpProgress = calculateXpProgress(totalXp = savedXp)

        // ── STEP 5: Push restored state into LiveData ─────────────────────
        _uiState.value = DashboardUiState(
            xpProgress = xpProgress,
            todaysMission = todaysMission,
            isLoading = false,                // Done loading — hide spinner
            errorMessage = null,              // No error
            showReflectionSlider = false,     // Slider hidden by default
            currentDiscomfortRating = 5,      // Default slider midpoint
            isDailyComplete = alreadyCompletedToday  // Restore "Come back tomorrow" if needed
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // USER ACTIONS
    // These functions are called by the UI when the user does something.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * onMissionCardClicked — called when the user taps the mission card area (not the buttons).
     *
     * In the future, this will navigate to the MissionDetailScreen where the user
     * can read the full mission description and see their history.
     *
     * For now, it's a no-op stub. Wire it to NavController navigation later.
     */
    fun onMissionCardClicked() {
        // TODO: Navigate to MissionDetailScreen
        // Example future code:
        //   navController.navigate("mission_detail/${uiState.value?.todaysMission?.id}")
    }

    /**
     * onAcceptMission — called when the user taps the "ACCEPT THE MISSION" button on an ACTIVE mission.
     *
     * This reveals the discomfort rating slider below the action buttons, prompting the user
     * to rate how uncomfortable the experience was before they submit.
     *
     * In the future, this will also:
     *   1. Write a "mission_accepted" event to the Room database (for offline tracking).
     *   2. Optionally trigger a WorkManager task to record the acceptance in Supabase.
     */
    fun onAcceptMission() {
        // Show the discomfort slider by setting showReflectionSlider = true.
        // We use copy() to create a new state object — only changing the fields we care about.
        _uiState.value = _uiState.value?.copy(
            showReflectionSlider = true   // Reveals the 1–10 slider below the action buttons
        )

        // TODO: Room — insert MissionAttempt record with status = ACCEPTED
        // TODO: Supabase sync via WorkManager
    }

    /**
     * onSwapMission — called when the user taps "SWAP MISSION".
     *
     * This is only shown for location-dependent missions (Mission.isLocationDependent == true).
     * It replaces today's mission with an alternative that does NOT require travel —
     * for example, if the user is housebound, stuck at work, or simply cannot reach the location.
     *
     * v4: Now implemented using MissionRepository.getAlternateMission().
     * The alternate mission is random but will never repeat the current one.
     *
     * Future work:
     *   - A user should only get one free swap per day — enforce a swap-count limit here.
     *   - Store the swap event in Room so the limit survives app restarts.
     */
    fun onSwapMission() {
        val currentState = _uiState.value ?: return
        val currentMission = currentState.todaysMission ?: return

        // Get a random alternate mission that is not the one currently showing.
        // The alternate has dateAssigned stamped with today's date.
        val alternateMission = MissionRepository.getAlternateMission(currentMission.id)

        // Replace today's mission in state.
        // Reset showReflectionSlider in case the user had already tapped ACCEPT on the old mission.
        _uiState.value = currentState.copy(
            todaysMission = alternateMission,
            showReflectionSlider = false  // Reset slider state for the new mission
        )

        // TODO: Room — record the swap event; enforce one-swap-per-day limit
        // TODO: Supabase sync via WorkManager
    }

    /**
     * onDiscomfortRatingChanged — called continuously as the user drags the slider.
     *
     * Updates the live slider value in the UI state so the label (e.g., "7 / 10") stays in sync
     * with the thumb position in real time.
     *
     * @param rating  The new slider position as an integer (1–10).
     *                The composable converts the Float slider value to Int before calling this.
     */
    fun onDiscomfortRatingChanged(rating: Int) {
        // Clamp the incoming value to [1, 10] just in case of off-by-one from the slider
        val clamped = rating.coerceIn(1, 10)

        // Update only the slider value — everything else in state stays the same
        _uiState.value = _uiState.value?.copy(
            currentDiscomfortRating = clamped
        )
    }

    /**
     * onSubmitReflection — called when the user taps "SUBMIT RATING" after positioning the slider.
     *
     * v3: This function does REAL XP MATH:
     *   1. Reads the mission's xpReward.
     *   2. Adds it to the user's current total XP.
     *   3. Calls calculateXpProgress(newTotalXp) to recalculate rank + bar fraction.
     *      If the new XP crosses a rank threshold, calculateXpProgress() returns the
     *      new rank automatically — no special rank-up code needed here.
     *   4. Pushes the updated XpProgress into state.
     *      The animateFloatAsState() in XpProgressSection will smoothly animate the bar
     *      to the new fraction — this is where the progress bar animation happens.
     *   5. Marks the mission COMPLETED and stores the discomfort rating.
     *   6. Hides the slider.
     *
     * v4: Also persists XP and today's date to SharedPreferences so progress
     * survives app restarts. The date lock prevents double-completing today's mission.
     */
    fun onSubmitReflection() {
        // Get the current state snapshot — bail out safely if state is null
        val currentState = _uiState.value ?: return
        val rating = currentState.currentDiscomfortRating
        val mission = currentState.todaysMission ?: return          // Need a mission to complete
        val currentXpProgress = currentState.xpProgress ?: return  // Need current XP to add to

        // ── STEP 1: Calculate new total XP ────────────────────────────────
        // Add the mission's XP reward to the user's existing total.
        // e.g., 350 (current) + 75 (reward) = 425 (new total)
        val newTotalXp = currentXpProgress.currentXp + mission.xpReward

        // ── STEP 2: Recalculate rank for the new XP total ─────────────────
        // calculateXpProgress() walks ALL_RANKS to find the correct rank and bar fraction.
        // If newTotalXp crosses a rank threshold (e.g., 350+200=550 crosses Level 3 at 500),
        // the returned XpProgress will contain the NEW rank — automatic rank-up, no extra code.
        val newXpProgress = calculateXpProgress(totalXp = newTotalXp)

        // ── STEP 3: Update the mission to COMPLETED with the user's rating ─
        // copy() keeps all other mission fields intact — only status and rating change.
        val updatedMission = mission.copy(
            status = MissionStatus.COMPLETED,   // Mission is now done
            discomfortRating = rating           // Store the user's 1–10 discomfort rating
        )

        // ── STEP 4: Persist new XP and today's date to SharedPreferences ──
        // v4: This is the critical addition — without this the XP resets on every app restart.
        //
        // putInt("total_xp", newTotalXp):
        //   Saves the user's cumulative XP so it survives app kills and restarts.
        //
        // putString("last_completed_date", today):
        //   Stamps today's date so loadInitialData() can detect on next cold start
        //   that the user already completed today's mission and should see the
        //   "Come back tomorrow" screen instead of a fresh mission.
        //
        // KTX `edit { }` lambda (from androidx.core.content.edit):
        //   More idiomatic than the old .edit().put___().apply() chain.
        //   Applies changes asynchronously in the background (same as .apply()).
        val todayString = LocalDate.now().toString()
        prefs.edit {
            putInt(KEY_TOTAL_XP, newTotalXp)
            putString(KEY_LAST_COMPLETED_DATE, todayString)
        }

        // ── STEP 5: Push updated state ────────────────────────────────────
        // - newXpProgress contains the incremented XP + any rank change.
        //   The animateFloatAsState() in XpProgressSection reads progressFraction from this
        //   and will smoothly animate the bar from the old fraction to the new one.
        // - showReflectionSlider = false hides the slider now that the rating is recorded.
        // - isDailyComplete stays false here — the user still sees the COMPLETED card
        //   and "VIEW COMPLETION" button. isDailyComplete flips when they tap that button.
        _uiState.value = currentState.copy(
            xpProgress = newXpProgress,          // Updated XP — triggers animated bar fill
            todaysMission = updatedMission,      // COMPLETED status + discomfort rating stored
            showReflectionSlider = false         // Hide the slider — rating has been submitted
        )

        // TODO: Room — update MissionAttempt record with status=COMPLETED and discomfortRating=rating
        // TODO: WorkManager — trigger Supabase sync for the completed mission + updated XP
    }

    /**
     * onViewCompletion — called when the user taps "VIEW COMPLETION" on the COMPLETED mission card.
     *
     * v3: NEW FUNCTION.
     *
     * At this point the mission is already marked COMPLETED and XP has already been added
     * (that happened in onSubmitReflection). This function is purely a UI transition:
     * it sets isDailyComplete = true, which causes DashboardScreen to swap from the
     * mission card view to the "Come back tomorrow" screen.
     *
     * The "Come back tomorrow" screen still shows the updated XP progress bar so the user
     * gets a final satisfying look at their new total before the app goes quiet for the day.
     *
     * In the future, this could also:
     *   - Navigate to a "Mission Complete" summary screen with the discomfort rating + streak count.
     *   - Trigger a local notification to remind the user about tomorrow's mission.
     */
    fun onViewCompletion() {
        // Flip isDailyComplete → DashboardScreen transitions to the completion view.
        _uiState.value = _uiState.value?.copy(
            isDailyComplete = true
        )

        // TODO: Show mission-complete summary screen via NavController (future)
        // TODO: Schedule next-day reminder notification via WorkManager (future)
    }

    /**
     * onRefresh — called if the user pull-to-refreshes the screen.
     *
     * v4: Now reloads from SharedPreferences + MissionRepository instead of placeholder data.
     * This correctly restores the user's real XP and today's mission state.
     *
     * In the future, this will also trigger a sync with Supabase (if online)
     * and reload data from Room (always available offline).
     */
    fun onRefresh() {
        // Show loading spinner briefly while we reload
        _uiState.value = _uiState.value?.copy(isLoading = true)

        // Reload from SharedPreferences + MissionRepository
        loadInitialData()

        // TODO: Trigger WorkManager sync + reload from Room
        // Example future code:
        //   viewModelScope.launch {
        //       syncWithSupabase()       // WorkManager one-time sync request
        //       loadFromRoom()           // Reload fresh data from local Room DB
        //   }
    }
}
