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
 *     Model  = DashboardModels.kt (data classes)
 *     View   = DashboardScreen.kt (composables)
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
 */
data class DashboardUiState(
    val xpProgress: XpProgress? = null,          // Null = not yet loaded
    val todaysMission: Mission? = null,           // Null = not yet loaded or no mission today
    val isLoading: Boolean = true,                // Start in loading state by default
    val errorMessage: String? = null,            // Null = no error
    val showReflectionSlider: Boolean = false,   // False = slider hidden; true = slider visible
    val currentDiscomfortRating: Int = 5         // Default slider position = 5 (middle of 1–10)
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
    // MutableStateFlow holds a value that can change over time.
    // Compose will automatically re-draw any UI that reads from this flow
    // whenever the value changes.
    //
    // We expose it as a read-only StateFlow to the screen — only the ViewModel
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
     */
    private fun loadPlaceholderData() {

        // Grab the Level 2 rank from our rank registry defined in DashboardModels.kt
        // ALL_RANKS is a List<Rank>, indices start at 0, so index 1 = Level 2
        val currentRank = ALL_RANKS[1]  // "The Initiate" (Level 2)

        // Build the XP progress object for a Level 2 user
        // Level 2 starts at 200 XP, Level 3 starts at 500 XP.
        // The user currently has 350 XP, so they're 150/300 = 50% through Level 2.
        val xpProgress = XpProgress(
            currentXp = 350,            // The user has earned 350 total XP
            xpForCurrentRank = 200,     // Level 2 begins at 200 XP
            xpForNextRank = 500,        // Level 3 begins at 500 XP
            currentRank = currentRank   // The Rank object for Level 2
        )

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
            xpProgress = xpProgress,           // The XP/rank data we just built
            todaysMission = todaysMission,      // The mission we just built
            isLoading = false,                 // Done loading — hide any spinner/shimmer
            errorMessage = null,              // No error occurred
            showReflectionSlider = false,      // Slider hidden until user taps "ACCEPT THE MISSION"
            currentDiscomfortRating = 5        // Default slider position at the midpoint
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // USER ACTIONS
    // These functions are called by the UI when the user does something.
    // Right now they're stubs — add the real Room/Supabase logic later.
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
     * Records the discomfort rating on the mission and marks the mission as COMPLETED.
     * Hides the slider now that the rating has been submitted.
     *
     * In the future, this will:
     *   1. Write the rating + COMPLETED status to the Room database.
     *   2. Trigger a Supabase sync via WorkManager to persist the result to the cloud.
     *   3. Award the mission's XP to the user's total (update XpProgress in Room).
     */
    fun onSubmitReflection() {
        // Get the current state snapshot — bail out safely if state is null
        val currentState = _uiState.value ?: return
        val rating = currentState.currentDiscomfortRating

        // Build the updated mission with the submitted rating and COMPLETED status.
        // We use copy() so all other mission fields (title, rules, xpReward, etc.) stay intact.
        val updatedMission = currentState.todaysMission?.copy(
            status = MissionStatus.COMPLETED,   // Mission is now done
            discomfortRating = rating           // Store the user's 1–10 rating
        )

        // Push the new state: hide the slider, mark mission complete, store rating
        _uiState.value = currentState.copy(
            todaysMission = updatedMission,     // Updated mission with COMPLETED + rating
            showReflectionSlider = false        // Hide the slider — rating has been submitted
        )

        // TODO: Room — update MissionAttempt with status=COMPLETED and discomfortRating=rating
        // TODO: Room — increment user's total XP by mission.xpReward
        // TODO: WorkManager — trigger Supabase sync for the completed mission
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
