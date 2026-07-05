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
 */

package com.example.missionuncomfortable.ui.dashboard

import androidx.lifecycle.ViewModel  // Base class that gives us lifecycle-awareness and state survival

/**
 * DashboardUiState — a single data class that holds EVERYTHING the Dashboard screen needs to display.
 *
 * By bundling all UI state into one object, the composable only needs to observe ONE thing.
 * When any field changes, Compose will re-compose only the parts of the screen that use that field.
 *
 * @param xpProgress    The user's current XP and rank progress data. Null while loading.
 * @param todaysMission The mission assigned to the user today. Null if no mission yet or still loading.
 * @param isLoading     True while data is being fetched, false once data is ready.
 * @param errorMessage  If something went wrong loading data, this holds the error text to show.
 *                      Null means no error.
 */
data class DashboardUiState(
    val xpProgress: XpProgress? = null,          // Null = not yet loaded
    val todaysMission: Mission? = null,           // Null = not yet loaded or no mission today
    val isLoading: Boolean = true,                // Start in loading state by default
    val errorMessage: String? = null             // Null = no error
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

        // Build a placeholder mission representing "today's challenge"
        val todaysMission = Mission(
            id = "placeholder-mission-001",               // Fake ID — real ones will come from Supabase
            title = "The Silent Stranger",               // Short catchy mission name
            description = "Go to a coffee shop or public space alone. " +
                    "Sit without your phone for at least 15 minutes. " +
                    "Observe your surroundings. Write a one-paragraph reflection when you're done.",
            xpReward = 75,                               // Completing this earns 75 XP
            status = MissionStatus.ACTIVE,               // The mission is currently active (not done yet)
            difficulty = 2,                              // Difficulty 2/5 — suitable for a Level 2 user
            dateAssigned = "2026-07-05"                  // Today's date (hardcoded for placeholder)
        )

        // Push the loaded data into the UI state.
        // Setting isLoading = false tells the screen to stop showing a spinner.
        _uiState.value = DashboardUiState(
            xpProgress = xpProgress,      // The XP/rank data we just built
            todaysMission = todaysMission, // The mission we just built
            isLoading = false,            // Done loading — hide any spinner/shimmer
            errorMessage = null           // No error occurred
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // USER ACTIONS
    // These functions are called by the UI when the user does something.
    // Right now they're stubs — add the real Room/Supabase logic later.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * onMissionCardClicked — called when the user taps on the Today's Mission card.
     *
     * In the future, this will navigate to the MissionDetailScreen where the user
     * can read the full mission description and submit their proof (reflection/photo).
     *
     * For now, it's a no-op stub. Wire it to NavController navigation later.
     */
    fun onMissionCardClicked() {
        // TODO: Navigate to MissionDetailScreen
        // Example future code:
        //   navController.navigate("mission_detail/${uiState.value?.todaysMission?.id}")
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
