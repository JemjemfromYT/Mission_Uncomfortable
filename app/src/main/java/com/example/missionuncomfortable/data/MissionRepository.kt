// ============================================================
// FILE: app/src/main/java/com/example/missionuncomfortable/data/MissionRepository.kt
//
// PURPOSE: Single source of truth for all available missions.
//
//   - Holds a curated list of 7 Uncomfortable Missions.
//     Each Mission uses the exact same data class as DashboardModels.kt —
//     all required fields are populated (id, title, objective, rules,
//     xpReward, status, difficulty, dateAssigned, isLocationDependent).
//
//   - getMissionForToday() uses the calendar day as a deterministic seed:
//       same mission is shown all day, a new one appears at midnight.
//       With 7 missions, the full rotation takes 7 days.
//
//   - getAlternateMission() is wired to the SWAP MISSION button.
//     It returns a random mission that is NOT the one currently on screen.
//
// NOTE: `dateAssigned` in the library missions is set to "" as a placeholder.
//       getMissionForToday() stamps today's real date on the returned mission
//       via .copy(dateAssigned = ...) so it is always accurate.
//
// v2 — FIXED: Replaced the API-26-only date class with
//      java.util.Calendar + java.text.SimpleDateFormat (available since API 1).
//      This makes the file compatible with minSdk 24.
// ============================================================

package com.example.missionuncomfortable.data

import com.example.missionuncomfortable.ui.dashboard.Mission
import com.example.missionuncomfortable.ui.dashboard.MissionStatus
import java.text.SimpleDateFormat   // API 1+ — used for "yyyy-MM-dd" date string formatting
import java.util.Calendar           // API 1+ — used to compute today's epoch day
import java.util.Locale             // Needed by SimpleDateFormat to prevent locale digit substitution

object MissionRepository {

    // --------------------------------------------------------
    // THE MISSION LIBRARY
    //
    // To add a new mission: append it to this list.
    // Thresholds and rank math live in DashboardModels.ALL_RANKS —
    // nothing here needs updating when ranks change.
    //
    // Field reference (matches DashboardModels.Mission exactly):
    //   id               — unique stable string identifier
    //   title            — short display name shown on the card
    //   objective        — single-sentence OBJECTIVE block
    //   rules            — ordered RULES list (numbered in UI)
    //   xpReward         — XP granted on completion
    //   status           — always ACTIVE here; ViewModel overrides if needed
    //   difficulty       — 1–5 scale shown as dots on the card footer
    //   dateAssigned     — "" here; getMissionForToday() stamps today's date
    //   isLocationDependent — true = user must travel; shows SWAP MISSION button
    //   discomfortRating — always null here; user fills it in after completion
    // --------------------------------------------------------
    private val ALL_MISSIONS = listOf(

        Mission(
            id = "silent_stranger",
            title = "The Silent Stranger",
            objective = "Go to a coffee shop or public space alone and sit in silence without your phone.",
            rules = listOf(
                "Remain seated for a minimum of 15 minutes.",
                "Your phone must stay in your pocket or bag.",
                "Observe your surroundings. Make eye contact if it happens naturally.",
                "Submit your discomfort rating before you leave."
            ),
            xpReward = 75,
            status = MissionStatus.ACTIVE,
            difficulty = 2,
            dateAssigned = "",         // Stamped with today's date by getMissionForToday()
            isLocationDependent = true // Requires travelling to a coffee shop or public space
        ),

        Mission(
            id = "cold_open",
            title = "The Cold Open",
            objective = "Strike up a genuine conversation with a complete stranger — not a service worker.",
            rules = listOf(
                "The conversation must last at least 60 seconds.",
                "You must introduce yourself by name.",
                "No phones, no excuses — initiate in person.",
                "Submit your discomfort rating immediately after."
            ),
            xpReward = 100,
            status = MissionStatus.ACTIVE,
            difficulty = 3,
            dateAssigned = "",
            isLocationDependent = true  // Requires being somewhere with strangers
        ),

        Mission(
            id = "the_volunteer",
            title = "The Volunteer",
            objective = "Raise your hand and volunteer for something you would normally avoid today.",
            rules = listOf(
                "Must be a real, public act of volunteering (meeting, class, or group).",
                "You cannot prepare a script in advance.",
                "Accept the outcome, whatever it is.",
                "Submit your discomfort rating within 10 minutes of completing."
            ),
            xpReward = 100,
            status = MissionStatus.ACTIVE,
            difficulty = 3,
            dateAssigned = "",
            isLocationDependent = false // Can happen anywhere — work, school, online
        ),

        Mission(
            id = "the_mirror",
            title = "The Mirror",
            objective = "Record a 60-second video of yourself speaking about your goals out loud. Watch it back.",
            rules = listOf(
                "No re-takes — the first recording is final.",
                "You must watch the full video back before submitting.",
                "Speak about at least one goal you have never told anyone.",
                "Delete or keep the video — your choice."
            ),
            xpReward = 75,
            status = MissionStatus.ACTIVE,
            difficulty = 2,
            dateAssigned = "",
            isLocationDependent = false // Done at home — no travel required
        ),

        Mission(
            id = "hold_the_door",
            title = "Hold the Door",
            objective = "Perform five deliberate, visible acts of kindness for strangers today.",
            rules = listOf(
                "Each act must involve direct eye contact and a verbal acknowledgment.",
                "Acts must be spread across at least 3 different locations.",
                "Do not announce that you are doing a challenge.",
                "Submit your discomfort rating after all five acts are complete."
            ),
            xpReward = 50,
            status = MissionStatus.ACTIVE,
            difficulty = 1,
            dateAssigned = "",
            isLocationDependent = true  // Requires going out to find strangers
        ),

        Mission(
            id = "the_refusal",
            title = "The Refusal",
            objective = "Say 'no' firmly and without apology to the next request you would normally reluctantly agree to.",
            rules = listOf(
                "The refusal must be spoken aloud — no text messages.",
                "You may not over-explain or apologize excessively.",
                "If no opportunity arises naturally, create one by making a small request of someone else.",
                "Submit your discomfort rating within 30 minutes."
            ),
            xpReward = 75,
            status = MissionStatus.ACTIVE,
            difficulty = 2,
            dateAssigned = "",
            isLocationDependent = false // Can happen at work, home, or anywhere
        ),

        Mission(
            id = "cold_exposure",
            title = "Cold Exposure",
            objective = "End your shower with two full minutes of cold water. No warming back up.",
            rules = listOf(
                "Cold phase must be the final phase — no warm water afterward.",
                "Minimum duration: 2 minutes of cold water.",
                "Keep your breathing controlled — no panicked gasping.",
                "Submit your discomfort rating immediately after drying off."
            ),
            xpReward = 100,
            status = MissionStatus.ACTIVE,
            difficulty = 3,
            dateAssigned = "",
            isLocationDependent = false // Done at home
        )
    )

    // --------------------------------------------------------
    // getMissionForToday()
    //
    // Deterministic daily mission selection — same mission all day,
    // new mission at midnight, 7-day rotation cycle.
    //
    // HOW IT WORKS (API 24+ compatible):
    //   1. Get today's epoch day: normalise Calendar to midnight in the
    //      device's local timezone, then divide timeInMillis by ms-per-day.
    //      This gives a stable integer that only changes at local midnight.
    //   2. Use (epochDay % 7) as an index into ALL_MISSIONS.
    //   3. The safeIndex guard handles negative values (dates before 1970).
    //   4. Stamp today's real date string onto the returned mission via .copy()
    //      so dateAssigned is always accurate.
    //
    // WHY Calendar INSTEAD OF java.time?
    //   The java.time package (e.g. toEpochDay()) requires API 26.
    //   minSdk is 24, so we use Calendar + SimpleDateFormat instead
    //   (both available since API 1 with no extra dependencies).
    // --------------------------------------------------------
    fun getMissionForToday(): Mission {
        // ── Compute today's epoch day (API 24+ compatible) ────────────────
        // Normalise to midnight local time before dividing — ensures the
        // value is stable for the full 24-hour local day and only ticks
        // forward when the device's clock crosses midnight.
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val epochDay = cal.timeInMillis / (24L * 60L * 60L * 1000L)

        // ── Compute safe list index ────────────────────────────────────────
        val rawIndex = (epochDay % ALL_MISSIONS.size).toInt()
        // The double-modulo + add guards against negative rawIndex values
        // for dates before 1970-01-01 (purely defensive — unlikely in practice).
        val safeIndex = ((rawIndex % ALL_MISSIONS.size) + ALL_MISSIONS.size) % ALL_MISSIONS.size

        // ── Stamp today's date string onto the mission ─────────────────────
        // Locale.US prevents Arabic-Indic digit substitution on some devices
        // (e.g. a device set to Arabic locale would otherwise produce "٢٠٢٦-٠٧-٠٦").
        // The comparison in DashboardViewModel uses the same format, so they match.
        val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(Calendar.getInstance().time)

        return ALL_MISSIONS[safeIndex].copy(
            dateAssigned = todayDateString   // e.g. "2026-07-06"
        )
    }

    // --------------------------------------------------------
    // getAlternateMission()
    //
    // Returns a random mission that is NOT the one currently
    // displayed (identified by its id). Called from
    // DashboardViewModel.onSwapMission().
    //
    // The returned mission also has dateAssigned stamped with today.
    //
    // With 7 missions, candidates always has at least 6 items —
    // .random() will never throw on an empty list here.
    // --------------------------------------------------------
    fun getAlternateMission(currentMissionId: String): Mission {
        val candidates = ALL_MISSIONS.filter { it.id != currentMissionId }
        val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(Calendar.getInstance().time)
        return candidates.random().copy(
            dateAssigned = todayDateString   // Stamp today's real date
        )
    }
}
