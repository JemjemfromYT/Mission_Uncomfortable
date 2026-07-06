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
 *   Right now it uses hardcoded placeholder data so you can see the screen working immediately.
 *   Later, you'll replace the placeholder data with calls to:
 *     1. Room DAO (local database, for offline-first reads)
 *     2. Supabase repository (for syncing data from the cloud via WorkManager)
 *
 * Architecture note:
 *   This follows the MVVM pattern (Model-View-ViewModel):
 *     Model     = DashboardModels.kt (data classes)
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
 */

package com.example.missionuncomfortable.ui.dashboard

import androidx.lifecycle.ViewModel  // Base class that gives us lifecycle-awareness and state survival

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
 *                                 Resets to false the next time loadPlaceholderData() is called
 *                                 (i.e., when a new day's mission is loaded in the future).
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
 * Extends ViewModel from AndroidX Lifecycle, which gives it:
 *   - Survival across screen rotations (the screen reconnects, data stays)
 *   - A coroutineScope (viewModelScope) for async work (we'll use this when adding Room/Supabase)
 */
class DashboardViewModel : ViewModel() {

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
    // For now it loads placeholder data. Later, replace loadPlaceholderData()
    // with loadFromRoom() which reads from the local Room database.
    // ─────────────────────────────────────────────────────────────────────────

    init {
        // Load data immediately when ViewModel is created.
        // In the future: call loadFromRoom() here instead.
        loadPlaceholderData()
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
     * loadPlaceholderData — populates the UI state with hardcoded fake data.
     *
     * TEMPORARY — replace this entire function once Room DAO is set up.
     *
     * The placeholder simulates a Level 2 user who has 350 XP, needs 500 XP to reach Level 3,
     * and has been assigned a medium-difficulty mission for today.
     *
     * v2 update: The Mission now uses `objective` + `rules` instead of a flat `description`,
     * and `isLocationDependent = true` because the coffee shop mission requires travel.
     *
     * v3 update: XP is now calculated via calculateXpProgress() instead of hardcoded — so
     * even the initial state is consistent with the live XP engine.
     */
    private fun loadPlaceholderData() {

        // v3: Use the XP helper so placeholder data goes through the same rank-calculation
        // logic as live data. Starting XP = 350. The helper will resolve this to Level 2.
        val xpProgress = calculateXpProgress(totalXp = 350)

        // Build a placeholder mission representing "today's challenge".
        // v2: Uses the new `objective` + `rules` format instead of a flat `description`.
        // The coffee shop mission is location-dependent (requires travelling to a coffee shop or
        // public space), so `isLocationDependent = true` — this shows the "Swap Mission" button.
        val todaysMission = Mission(
            id = "placeholder-mission-001",                   // Fake ID — real ones will come from Supabase

            title = "The Silent Stranger",                    // Short catchy mission name

            // OBJECTIVE: A single clear sentence stating what the user must do.
            // This is displayed under the "OBJECTIVE" header in the military briefing format.
            objective = "Go to a coffee shop or public space alone and sit in silence without your phone.",

            // RULES: An ordered list of specific constraints the user must follow.
            // Each string becomes a numbered rule in the briefing card.
            // Keep each rule concise — one action or constraint per item.
            rules = listOf(
                "Remain seated for a minimum of 15 minutes.",
                "Your phone must stay in your pocket or bag.",
                "Observe your surroundings. Make eye contact if it happens naturally.",
                "Submit your discomfort rating before you leave."
            ),

            xpReward = 75,                                   // Completing this earns 75 XP
            status = MissionStatus.ACTIVE,                   // The mission is currently active
            difficulty = 2,                                  // Difficulty 2/5 — suitable for Level 2
            dateAssigned = "2026-07-05",                     // Today's date (hardcoded for placeholder)
            isLocationDependent = true,                      // TRUE — user must travel to a coffee shop
            discomfortRating = null                          // Not yet submitted — user hasn't done it yet
        )

        // Push the loaded data into the UI state.
        // Setting isLoading = false tells the screen to stop showing a spinner.
        _uiState.value = DashboardUiState(
            xpProgress = xpProgress,           // Calculated via calculateXpProgress(350)
            todaysMission = todaysMission,      // The mission we just built
            isLoading = false,                 // Done loading — hide any spinner/shimmer
            errorMessage = null,              // No error occurred
            showReflectionSlider = false,      // Slider hidden until user taps "ACCEPT THE MISSION"
            currentDiscomfortRating = 5,       // Default slider position at the midpoint
            isDailyComplete = false           // Not yet complete — user still needs to do the mission
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
     * In the future, this will:
     *   1. Request an alternative non-location-dependent mission from Supabase.
     *   2. Store the swap event in Room (so the user can't swap infinitely).
     *   3. A user should only get one free swap per day — enforce this in the ViewModel later.
     */
    fun onSwapMission() {
        // TODO: Request alternative mission from Supabase
        // TODO: Update Room with the new mission, mark old mission as swapped
        // TODO: Update _uiState with the new mission object

        // For now, this is a no-op stub.
        // When you implement this:
        //   1. Set isLoading = true so the user sees a brief spinner.
        //   2. Fetch an alternative mission from your repository layer.
        //   3. Update _uiState with the new mission.
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
     * v3: This function now does REAL XP MATH:
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
     * Future work:
     *   - Write the rating + COMPLETED status to Room.
     *   - Trigger a Supabase sync via WorkManager.
     *   - Persist the updated XP total to Room (UserProfile table or equivalent).
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

        // ── STEP 4: Push updated state ────────────────────────────────────
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
        // TODO: Room — write newTotalXp to the UserProfile table (or equivalent) to persist XP
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
     * In the future, this will trigger a sync with Supabase (if online)
     * and reload data from Room (always available offline).
     */
    fun onRefresh() {
        // TODO: Trigger WorkManager sync + reload from Room
        // Example future code:
        //   viewModelScope.launch {
        //       syncWithSupabase()       // WorkManager one-time sync request
        //       loadFromRoom()           // Reload fresh data from local Room DB
        //   }

        // For now, just reload the placeholder data again
        _uiState.value = _uiState.value?.copy(isLoading = true) // Show loading briefly
        loadPlaceholderData()
    }
}
