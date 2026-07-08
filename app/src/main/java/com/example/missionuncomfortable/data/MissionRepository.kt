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
 *   MissionRepository.kt   — Full mission library: 25 missions, 5 XP tiers, 6 categories ← YOU ARE HERE
 *   DashboardViewModel.kt  — All game logic: XP, streaks, rank-ups, history saving, core loop
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
 * ── MISSION LIBRARY RULES — follow these when adding new missions ────────────
 *
 *   • Mission IDs are permanent. NEVER change an id after the app ships.
 *     History entries store the missionId — changing it orphans old records.
 *   • New missions go at the END of their tier block (Tier 1–5).
 *   • Each mission must have: id, title, objective, rules (list), xpReward,
 *     difficulty, isLocationDependent, category, dateAssigned="", status=ACTIVE.
 *   • getMissionForToday() uses dayOfYear % ALL_MISSIONS.size — no code change
 *     needed when you add missions; the rotation automatically extends.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these rules, the developer will have to   ║
 * ║  manually fix every file you touch. Please respect these conventions.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * MissionRepository.kt
 *
 * The single source of truth for all mission data.
 *
 * ─── WHAT THIS FILE CONTAINS ────────────────────────────────────────────────
 *
 *   ALL_MISSIONS         — The complete library of 25 missions across 5 tiers.
 *   getMissionForToday() — Returns a deterministic mission based on the day of year.
 *   getAlternateMission()— Returns a different mission from today's (for future swap use).
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/data/MissionRepository.kt
 *
 * ─── HOW MISSION ROTATION WORKS ──────────────────────────────────────────────
 *
 *   getMissionForToday() uses: dayOfYear % ALL_MISSIONS.size
 *   This means the same mission appears on the same calendar day every year.
 *   With 25 missions, a daily user sees each mission once every 25 days.
 *   Add more missions to extend the rotation window.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — 7 missions. Initial version.
 *
 *   v2 — 25 missions across 5 tiers and 6 categories (Phase 5).
 *          Added MissionCategory field to every mission.
 *          Tier 1: 50 XP  (difficulty 1) — 5 missions
 *          Tier 2: 75 XP  (difficulty 2) — 7 missions
 *          Tier 3: 100 XP (difficulty 3) — 7 missions
 *          Tier 4: 125 XP (difficulty 4) — 4 missions
 *          Tier 5: 150 XP (difficulty 5) — 3 missions
 *          getMissionForToday() and getAlternateMission() unchanged — algorithm scales automatically.
 */

package com.example.missionuncomfortable.data

import com.example.missionuncomfortable.ui.dashboard.Mission
import com.example.missionuncomfortable.ui.dashboard.MissionCategory
import com.example.missionuncomfortable.ui.dashboard.MissionStatus
import java.util.Calendar

object MissionRepository {

    // ─────────────────────────────────────────────────────────────────────────
    // COMPLETE MISSION LIBRARY — 25 MISSIONS
    // ─────────────────────────────────────────────────────────────────────────
    //
    // Each mission has: id, title, objective, rules, xpReward, difficulty,
    // isLocationDependent, category.
    // status is always ACTIVE here — set at runtime by DashboardViewModel.
    // dateAssigned is always "" here — set at runtime by DashboardViewModel.

    val ALL_MISSIONS = listOf(

        // ── TIER 1 — 50 XP — difficulty 1 ───────────────────────────────────

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
            xpReward = 50, difficulty = 1, isLocationDependent = true,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "phone_away_cafe",
            title = "The Phone-Free Sit",
            objective = "Sit in a public place for 20 minutes with your phone face-down.",
            rules = listOf(
                "Choose somewhere busy: a food court, a park bench, a station platform.",
                "Phone must stay face-down and untouched for the full 20 minutes.",
                "Look up. Observe people. If eye contact happens, hold it briefly before looking away.",
                "Submit your discomfort rating when the 20 minutes are up."
            ),
            xpReward = 50, difficulty = 1, isLocationDependent = true,
            category = MissionCategory.PRESENCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "eye_contact_cashier",
            title = "The Held Look",
            objective = "Make and hold genuine eye contact with every cashier or service worker you interact with today.",
            rules = listOf(
                "Must apply to at least 3 different interactions.",
                "You must greet them by name if their name badge is visible.",
                "Hold eye contact through the full exchange — do not look at the counter or your phone.",
                "Submit your discomfort rating at the end of the day."
            ),
            xpReward = 50, difficulty = 1, isLocationDependent = true,
            category = MissionCategory.EYE_CONTACT,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "smile_at_strangers",
            title = "The First Signal",
            objective = "Smile and make brief eye contact with 10 strangers today.",
            rules = listOf(
                "Must be genuine — not a grimace.",
                "At least 5 must be people you would normally avoid looking at.",
                "Do not look away first in any of the 10 interactions.",
                "Submit your discomfort rating at the end of the day."
            ),
            xpReward = 50, difficulty = 1, isLocationDependent = true,
            category = MissionCategory.EYE_CONTACT,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "compliment_stranger",
            title = "The Honest Compliment",
            objective = "Give a genuine, specific compliment to a stranger — not about their looks.",
            rules = listOf(
                "It must be specific. Not 'nice shoes.' Specific: 'The colour of your jacket is great.'",
                "Deliver it in person, out loud. No texts.",
                "Do not apologise before or after giving it.",
                "Submit your discomfort rating immediately after."
            ),
            xpReward = 50, difficulty = 1, isLocationDependent = true,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        // ── TIER 2 — 75 XP — difficulty 2 ───────────────────────────────────

        Mission(
            id = "silent_stranger",
            title = "The Silent Stranger",
            objective = "Go to a coffee shop or public space alone and sit in silence without your phone for 15 minutes.",
            rules = listOf(
                "Remain seated for a minimum of 15 minutes.",
                "Your phone must stay in your pocket or bag.",
                "Observe your surroundings. Make eye contact if it happens naturally.",
                "Submit your discomfort rating before you leave."
            ),
            xpReward = 75, difficulty = 2, isLocationDependent = true,
            category = MissionCategory.PRESENCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
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
            xpReward = 75, difficulty = 2, isLocationDependent = false,
            category = MissionCategory.PERFORMANCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
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
            xpReward = 75, difficulty = 2, isLocationDependent = false,
            category = MissionCategory.REJECTION_PRACTICE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "ask_for_directions",
            title = "The Ask",
            objective = "Ask a stranger for directions to a specific place — even if you already know how to get there.",
            rules = listOf(
                "Must be a stranger, not a friend or colleague.",
                "Initiate the interaction yourself — don't wait for a natural opportunity.",
                "Make and hold eye contact when asking and receiving the answer.",
                "Thank them by name if they told you their name."
            ),
            xpReward = 75, difficulty = 2, isLocationDependent = true,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "sit_in_silence_home",
            title = "The Still Point",
            objective = "Sit in total silence — no phone, no music, no podcasts — for 10 minutes.",
            rules = listOf(
                "Find a quiet spot. Sit upright. No lying down.",
                "No phone, no screen, no audio of any kind.",
                "When uncomfortable thoughts arrive, let them. Do not distract yourself.",
                "Submit your discomfort rating immediately after the 10 minutes."
            ),
            xpReward = 75, difficulty = 2, isLocationDependent = false,
            category = MissionCategory.PRESENCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "eat_alone_restaurant",
            title = "Table for One",
            objective = "Go to a sit-down restaurant and eat a full meal alone without looking at your phone.",
            rules = listOf(
                "Must be a restaurant with table service — not fast food.",
                "Phone stays in your bag for the entire meal.",
                "Order from the menu verbally. Ask the server a genuine question about the food.",
                "Submit your discomfort rating after you pay."
            ),
            xpReward = 75, difficulty = 2, isLocationDependent = true,
            category = MissionCategory.PRESENCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "cold_shower_two_min",
            title = "Cold Exposure",
            objective = "End your shower with two full minutes of cold water. No warming back up.",
            rules = listOf(
                "Cold phase must be the final phase — no warm water afterward.",
                "Minimum duration: 2 minutes of cold water.",
                "Keep your breathing controlled — no panicked gasping.",
                "Submit your discomfort rating immediately after drying off."
            ),
            xpReward = 75, difficulty = 2, isLocationDependent = false,
            category = MissionCategory.PHYSICAL,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        // ── TIER 3 — 100 XP — difficulty 3 ──────────────────────────────────

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
            xpReward = 100, difficulty = 3, isLocationDependent = true,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
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
            xpReward = 100, difficulty = 3, isLocationDependent = false,
            category = MissionCategory.PERFORMANCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "ask_for_discount",
            title = "The Negotiator",
            objective = "Ask for a discount on something you are buying today. Say the words out loud.",
            rules = listOf(
                "Must be in person. Not online.",
                "You must say the actual words: 'Is there any discount available?'",
                "Do not apologise before asking. Just ask.",
                "Whatever the answer, accept it without argument. Submit your rating."
            ),
            xpReward = 100, difficulty = 3, isLocationDependent = true,
            category = MissionCategory.REJECTION_PRACTICE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "stare_30_seconds",
            title = "The 30-Second Hold",
            objective = "Hold unbroken eye contact with someone for a full 30 seconds during a conversation today.",
            rules = listOf(
                "Must be during a real conversation — not a staring contest.",
                "The other person does not need to know this is a challenge.",
                "You may blink. You may not look away.",
                "If they look away, you win. Submit your discomfort rating."
            ),
            xpReward = 100, difficulty = 3, isLocationDependent = false,
            category = MissionCategory.EYE_CONTACT,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "speak_in_meeting",
            title = "The Floor",
            objective = "Speak first in the next group situation where you would normally stay silent.",
            rules = listOf(
                "Could be a meeting, a class, a group call, or a social gathering.",
                "You must speak within the first 5 minutes of the meeting starting.",
                "What you say must be substantive — not just 'good morning'.",
                "Submit your discomfort rating within 10 minutes of speaking."
            ),
            xpReward = 100, difficulty = 3, isLocationDependent = false,
            category = MissionCategory.PERFORMANCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "cold_shower_five_min",
            title = "Full Cold",
            objective = "Take an entirely cold shower — no warm water at all — for at least 5 minutes.",
            rules = listOf(
                "No warm water at any point during the shower.",
                "Minimum 5 minutes. Use a timer.",
                "Control your breathing from the first second. Do not flinch away from the stream.",
                "Submit your discomfort rating immediately."
            ),
            xpReward = 100, difficulty = 3, isLocationDependent = false,
            category = MissionCategory.PHYSICAL,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "sit_crowd_alone",
            title = "The Crowd",
            objective = "Go somewhere crowded and sit or stand alone for 20 minutes, fully present.",
            rules = listOf(
                "Somewhere genuinely crowded: a train station, a market, a food court.",
                "No phone. No book. No distraction.",
                "Observe the crowd. Let yourself be seen. Do not hide.",
                "Submit your discomfort rating after 20 minutes."
            ),
            xpReward = 100, difficulty = 3, isLocationDependent = true,
            category = MissionCategory.PRESENCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        // ── TIER 4 — 125 XP — difficulty 4 ──────────────────────────────────

        Mission(
            id = "introduce_to_group",
            title = "The Introduction",
            objective = "Walk up to a group of strangers and introduce yourself unprompted.",
            rules = listOf(
                "Must be a group of at least 3 people you have never met.",
                "You must say your name and ask at least one genuine question.",
                "Stay in the conversation for at least 2 minutes.",
                "Submit your discomfort rating immediately after leaving the group."
            ),
            xpReward = 125, difficulty = 4, isLocationDependent = true,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "ask_for_phone_number",
            title = "The Ask for More",
            objective = "Ask someone new you met today for their contact details to stay in touch.",
            rules = listOf(
                "Must be someone you genuinely want to know, not a random person.",
                "Ask in person. Do not text or DM first.",
                "Accept any answer without showing disappointment.",
                "Submit your discomfort rating whatever the outcome."
            ),
            xpReward = 125, difficulty = 4, isLocationDependent = true,
            category = MissionCategory.REJECTION_PRACTICE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "speak_in_front_of_camera",
            title = "On Record",
            objective = "Record a 3-minute video of yourself — fully lit, facing the camera — and watch it back completely.",
            rules = listOf(
                "No re-takes on the recording.",
                "Fully lit — no dark room, no hiding.",
                "You must watch the full video from start to finish.",
                "Write down one thing you noticed that you want to improve. Submit your rating."
            ),
            xpReward = 125, difficulty = 4, isLocationDependent = false,
            category = MissionCategory.PERFORMANCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "call_instead_of_text",
            title = "The Call",
            objective = "Call someone you would normally text. Have a real conversation, minimum 5 minutes.",
            rules = listOf(
                "No texting to arrange the call — just dial.",
                "Minimum 5 minutes of genuine conversation.",
                "Ask them a question you have never asked before.",
                "Submit your discomfort rating after hanging up."
            ),
            xpReward = 125, difficulty = 4, isLocationDependent = false,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        // ── TIER 5 — 150 XP — difficulty 5 ──────────────────────────────────

        Mission(
            id = "give_speech_group",
            title = "The Address",
            objective = "Give a short, prepared 2-minute speech to a group of real people today.",
            rules = listOf(
                "Must be in person, not on a video call.",
                "At least 3 people must be listening.",
                "Speak about something you actually believe in.",
                "No reading from a script — notes allowed, but eyes must be on the audience.",
                "Submit your discomfort rating immediately after."
            ),
            xpReward = 150, difficulty = 5, isLocationDependent = true,
            category = MissionCategory.PERFORMANCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "start_conversation_bus",
            title = "The Seat Neighbour",
            objective = "Start a genuine conversation with the person sitting next to you on public transport.",
            rules = listOf(
                "Must be a stranger. Must be on public transport (bus, train, tube).",
                "Conversation must last at least 90 seconds.",
                "You must introduce yourself by name.",
                "Submit your discomfort rating before you reach your stop."
            ),
            xpReward = 150, difficulty = 5, isLocationDependent = true,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "apologise_for_real",
            title = "The Apology",
            objective = "Give a genuine, unprompted apology to someone you have wronged — however small.",
            rules = listOf(
                "Must be in person or on a voice/video call. Not a text.",
                "No deflection or blame-sharing. Own it completely.",
                "Do not expect anything in return — not forgiveness, not acknowledgment.",
                "Submit your discomfort rating after."
            ),
            xpReward = 150, difficulty = 5, isLocationDependent = false,
            category = MissionCategory.REJECTION_PRACTICE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC FUNCTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * getMissionForToday — returns a deterministic mission based on the current day of year.
     *
     * Uses dayOfYear % ALL_MISSIONS.size so the same mission appears on the same
     * calendar day every year. The result is consistent across app restarts on the
     * same day — no randomness involved.
     *
     * The returned mission has status = ACTIVE and dateAssigned = "".
     * DashboardViewModel calls .copy(status = ..., dateAssigned = todayString)
     * to set the correct values at runtime.
     */
    fun getMissionForToday(): Mission {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % ALL_MISSIONS.size
        return ALL_MISSIONS[index]
    }

    /**
     * getAlternateMission — returns a different mission from today's.
     *
     * Used as the "swap" target. Returns the next mission in the list
     * (wrapping around at the end). This ensures the alternate is always
     * different from today's assignment.
     *
     * NOTE: In v5 (DashboardScreen), the SWAP MISSION button no longer calls
     * the ViewModel's onSwapMission() — it shows a motivational popup instead.
     * This function is retained for when swapping is re-enabled in a future version.
     */
    fun getAlternateMission(): Mission {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val todayIndex = dayOfYear % ALL_MISSIONS.size
        val alternateIndex = (todayIndex + 1) % ALL_MISSIONS.size
        return ALL_MISSIONS[alternateIndex]
    }
}
