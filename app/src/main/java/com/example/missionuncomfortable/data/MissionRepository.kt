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
 *   MissionRepository.kt   — Full mission library: 40 missions, 5 XP tiers, 6 categories ← YOU ARE HERE
 *   DashboardViewModel.kt  — All game logic: XP, streaks, rank-ups, history saving, core loop
 *   DashboardScreen.kt     — Main UI. v6: SWAP MISSION button now works (difficulty-gated).
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
 *   ALL_MISSIONS          — The complete library of 40 missions across 5 tiers.
 *   getMissionForToday()  — Returns a deterministic mission based on the day of year.
 *   getAlternateMission() — Returns a difficulty-compatible swap mission, or null.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/data/MissionRepository.kt
 *
 * ─── HOW MISSION ROTATION WORKS ──────────────────────────────────────────────
 *
 *   getMissionForToday() uses: dayOfYear % ALL_MISSIONS.size
 *   This means the same mission appears on the same calendar day every year.
 *   With 40 missions, a daily user sees each mission once every 40 days.
 *   Add more missions to extend the rotation window.
 *
 * ─── HOW SWAP WORKS ──────────────────────────────────────────────────────────
 *
 *   getAlternateMission(currentDifficulty) scans forward through ALL_MISSIONS
 *   (starting after today's index) and returns the first mission whose difficulty
 *   satisfies the swap rule: alternateD >= currentD - 2 AND alternateD <= currentD.
 *   This means swaps are only allowed to missions of the SAME difficulty or up to
 *   2 levels EASIER. You cannot swap UP to a harder mission or drop more than 2
 *   difficulty levels — this prevents the app from becoming an easy-mode exploit.
 *   Returns null if no compatible mission exists (rare with 40+ missions).
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
 *
 *   v3 — 40 missions (15 new added across all 5 tiers).
 *          Tier 1: 50 XP  (difficulty 1) — 8 missions  (+3 new)
 *          Tier 2: 75 XP  (difficulty 2) — 11 missions (+4 new)
 *          Tier 3: 100 XP (difficulty 3) — 11 missions (+4 new)
 *          Tier 4: 125 XP (difficulty 4) — 6 missions  (+2 new)
 *          Tier 5: 150 XP (difficulty 5) — 4 missions  (+1 new)
 *          getAlternateMission() updated: now accepts currentDifficulty: Int and
 *          returns Mission? (null if no compatible swap exists within the difficulty
 *          window of [currentDifficulty-2, currentDifficulty]).
 *
 *   v4 — RANK-BASED DIFFICULTY FILTERING (bug fix: Observer rank was too hard).
 *          getMissionForToday(rankLevel: Int = 1) now accepts the user's rank level
 *          and filters the mission pool to only difficulty ≤ rankLevel:
 *            Observer   (1) → difficulty 1 only   (prevents beginners seeing difficulty-5 missions)
 *            Initiate   (2) → difficulty 1–2
 *            Challenger (3) → difficulty 1–3
 *            Conqueror  (4) → difficulty 1–4
 *            Sovereign  (5) → all missions (unchanged behaviour at max rank)
 *          getAlternateMission(currentDifficulty, rankLevel) updated to match:
 *          swap candidates are now drawn from the same rank-filtered pool, so a
 *          swap cannot present a mission above the user's rank difficulty tier.
 */

package com.example.missionuncomfortable.data

import com.example.missionuncomfortable.ui.dashboard.Mission
import com.example.missionuncomfortable.ui.dashboard.MissionCategory
import com.example.missionuncomfortable.ui.dashboard.MissionStatus
import java.util.Calendar

object MissionRepository {

    // ─────────────────────────────────────────────────────────────────────────
    // COMPLETE MISSION LIBRARY — 40 MISSIONS
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

        // ── TIER 1 — NEW MISSIONS (v3) ───────────────────────────────────────

        Mission(
            id = "thank_you_note",
            title = "The Written Word",
            objective = "Write a genuine thank-you note — by hand — to someone who has helped you, and give it to them in person.",
            rules = listOf(
                "Must be handwritten. No printed or typed notes.",
                "The note must be specific: name one thing they did and how it affected you.",
                "Hand it to them directly. Do not leave it somewhere for them to find alone.",
                "Submit your discomfort rating after the handover."
            ),
            xpReward = 50, difficulty = 1, isLocationDependent = false,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "wave_at_neighbour",
            title = "The Acknowledgment",
            objective = "Acknowledge every neighbour, doorman, or building staff member you pass today with a greeting and eye contact.",
            rules = listOf(
                "Must apply to every person you pass in shared spaces — hallway, lift, entrance.",
                "Greeting must be verbal — not just a nod.",
                "If someone ignores you, greet the next person anyway.",
                "Submit your discomfort rating at the end of the day."
            ),
            xpReward = 50, difficulty = 1, isLocationDependent = true,
            category = MissionCategory.EYE_CONTACT,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "sit_without_distraction",
            title = "The Empty Five",
            objective = "Sit completely still for 5 minutes with nothing to do — no phone, no book, no fidgeting.",
            rules = listOf(
                "Choose a chair or bench. Sit upright. Hands on knees.",
                "No phone, no book, no food, no music.",
                "If you move your hands, restart the timer.",
                "Submit your discomfort rating when the 5 minutes are done."
            ),
            xpReward = 50, difficulty = 1, isLocationDependent = false,
            category = MissionCategory.PRESENCE,
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

        // ── TIER 2 — NEW MISSIONS (v3) ───────────────────────────────────────

        Mission(
            id = "disagree_out_loud",
            title = "The Disagreement",
            objective = "Respectfully but clearly disagree with someone's stated opinion today — out loud, in the moment.",
            rules = listOf(
                "Must be a genuine disagreement, not a manufactured one.",
                "State your position calmly and specifically — give one real reason.",
                "Do not retract or soften it if they push back. Hold your ground politely.",
                "Submit your discomfort rating after the exchange."
            ),
            xpReward = 75, difficulty = 2, isLocationDependent = false,
            category = MissionCategory.REJECTION_PRACTICE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "admit_a_mistake",
            title = "The Admission",
            objective = "Admit a mistake you made — today or recently — to the person it affected, directly and without minimising it.",
            rules = listOf(
                "Must be in person or on a voice call. Not a text.",
                "State clearly what you did wrong. No hedging ('I'm sorry you felt...').",
                "Do not wait for a 'right moment' — create the moment.",
                "Submit your discomfort rating within 10 minutes."
            ),
            xpReward = 75, difficulty = 2, isLocationDependent = false,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "walk_without_phone",
            title = "The Unguided Walk",
            objective = "Take a 20-minute walk in public with your phone completely off — not silent, OFF.",
            rules = listOf(
                "Phone must be powered off before you leave — not just silenced.",
                "Choose a route you do not normally walk.",
                "Notice three things you have never noticed before on that route.",
                "Submit your discomfort rating when you return home."
            ),
            xpReward = 75, difficulty = 2, isLocationDependent = true,
            category = MissionCategory.PRESENCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "ask_stranger_opinion",
            title = "The Canvass",
            objective = "Ask a stranger for their honest opinion on something that genuinely matters to you.",
            rules = listOf(
                "It must be a real question, not a throwaway one.",
                "Let them finish their full answer before you respond.",
                "Thank them specifically — mention one thing they said.",
                "Submit your discomfort rating after the conversation ends."
            ),
            xpReward = 75, difficulty = 2, isLocationDependent = true,
            category = MissionCategory.SOCIAL_INITIATION,
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

        // ── TIER 3 — NEW MISSIONS (v3) ───────────────────────────────────────

        Mission(
            id = "return_something",
            title = "The Return",
            objective = "Return something you bought — even if it is inconvenient — and deal with whatever resistance arises.",
            rules = listOf(
                "Must be an in-person return at a physical store.",
                "State your reason clearly and calmly once. Do not over-justify.",
                "If pushed back on, hold your position without aggression.",
                "Submit your discomfort rating after the transaction is resolved."
            ),
            xpReward = 100, difficulty = 3, isLocationDependent = true,
            category = MissionCategory.REJECTION_PRACTICE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "sing_hum_public",
            title = "The Audible Self",
            objective = "Hum or sing softly but audibly in a public place — loud enough that someone nearby could hear.",
            rules = listOf(
                "Must be in a public space — not alone at home.",
                "Must be loud enough that someone within 2 metres could hear it.",
                "Continue for at least 60 seconds without stopping due to embarrassment.",
                "Submit your discomfort rating when you stop."
            ),
            xpReward = 100, difficulty = 3, isLocationDependent = true,
            category = MissionCategory.PERFORMANCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "compliment_boss_senior",
            title = "Up the Ladder",
            objective = "Give a genuine, specific compliment to someone above you in status — your manager, a professor, or someone you find intimidating.",
            rules = listOf(
                "Must be delivered in person, not by email or text.",
                "It must be specific: not 'you're great' but 'the way you handled X was impressive because Y.'",
                "Do not apologise or undercut the compliment with self-deprecation.",
                "Submit your discomfort rating immediately after."
            ),
            xpReward = 100, difficulty = 3, isLocationDependent = false,
            category = MissionCategory.SOCIAL_INITIATION,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "skip_social_media",
            title = "The Blackout",
            objective = "Go the entire day without opening any social media app — and stay in every uncomfortable moment that produces.",
            rules = listOf(
                "Delete or log out from social media apps for the full day.",
                "When you feel the urge to check, sit with the urge for 60 seconds before doing anything else.",
                "Notice how many times you reach for your phone out of habit.",
                "Submit your discomfort rating at the end of the day."
            ),
            xpReward = 100, difficulty = 3, isLocationDependent = false,
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

        // ── TIER 4 — NEW MISSIONS (v3) ───────────────────────────────────────

        Mission(
            id = "confront_a_habit",
            title = "The Cut",
            objective = "Identify your biggest avoidance habit and deliberately face it once today — not for long, just once.",
            rules = listOf(
                "Write down the habit before you begin.",
                "Face it once, deliberately, for a minimum of 5 minutes.",
                "Do not allow yourself a 'reward' immediately afterward — sit with the feeling.",
                "Submit your discomfort rating after the 5 minutes end."
            ),
            xpReward = 125, difficulty = 4, isLocationDependent = false,
            category = MissionCategory.PRESENCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        ),

        Mission(
            id = "join_public_activity",
            title = "The Walk-In",
            objective = "Join a group activity you have never done before — without knowing anyone there and without telling anyone in advance.",
            rules = listOf(
                "Could be a fitness class, a community event, a local club, a workshop.",
                "Must be a real, in-person group. Online does not count.",
                "Stay for the full duration of the activity.",
                "Introduce yourself to at least one person before you leave."
            ),
            xpReward = 125, difficulty = 4, isLocationDependent = true,
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
        ),

        // ── TIER 5 — NEW MISSIONS (v3) ───────────────────────────────────────

        Mission(
            id = "perform_in_public",
            title = "The Stage",
            objective = "Perform something in public — sing, recite, play, or speak — where strangers can stop and watch.",
            rules = listOf(
                "Must be outdoors or in a public indoor space where people are present.",
                "Perform for a minimum of 2 minutes without stopping.",
                "Do not walk away if people gather to watch — face them.",
                "Submit your discomfort rating the moment you finish."
            ),
            xpReward = 150, difficulty = 5, isLocationDependent = true,
            category = MissionCategory.PERFORMANCE,
            dateAssigned = "", status = MissionStatus.ACTIVE
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC FUNCTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * getMissionForToday — returns a deterministic mission based on the current day of year,
     * filtered to only missions appropriate for the user's current rank level.
     *
     * v4 UPDATE — RANK-BASED DIFFICULTY FILTERING:
     *
     *   Each rank can only see missions up to its own max difficulty tier.
     *   This prevents Observer-rank beginners from receiving difficulty-5 missions
     *   that belong to veteran Sovereign players:
     *
     *     Observer   (level 1) → difficulty 1 only   (8 missions)
     *     Initiate   (level 2) → difficulty 1–2      (19 missions)
     *     Challenger (level 3) → difficulty 1–3      (30 missions)
     *     Conqueror  (level 4) → difficulty 1–4      (36 missions)
     *     Sovereign  (level 5) → difficulty 1–5      (all 40 missions)
     *
     *   The dayOfYear modulo rotates WITHIN the eligible pool only, so the rotation
     *   window shrinks for low ranks and grows as the user earns higher ranks.
     *   This is intentional — beginners get a manageable, encouraging experience
     *   before the harder missions are unlocked.
     *
     * The returned mission has status = ACTIVE and dateAssigned = "".
     * DashboardViewModel calls .copy(status = ..., dateAssigned = todayString)
     * to set the correct values at runtime.
     *
     * @param rankLevel  The user's current rank level (1–5). Defaults to 1 (Observer)
     *                   so existing call sites without rank data stay safe.
     * @return           The day's mission for this rank, deterministic for a given date.
     */
    fun getMissionForToday(rankLevel: Int = 1): Mission {
        // Clamp rankLevel to a valid range in case an unexpected value is passed.
        val maxDifficulty = rankLevel.coerceIn(1, 5)

        // Filter to only missions at or below the rank's max difficulty.
        // ifEmpty fallback: should never trigger with a well-populated ALL_MISSIONS,
        // but defensively returns the full list rather than crashing.
        val eligibleMissions = ALL_MISSIONS.filter { it.difficulty <= maxDifficulty }
            .ifEmpty { ALL_MISSIONS }

        // Rotate deterministically by day of year within the eligible pool.
        // Same day → same mission for a given rank level, across restarts and reinstalls.
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % eligibleMissions.size
        return eligibleMissions[index]
    }

    /**
     * getAlternateMission — returns the first difficulty-compatible swap candidate
     * within the user's eligible mission pool for their rank level.
     *
     * v3 UPDATE: accepts currentDifficulty and enforces the swap window rule.
     *
     * v4 UPDATE — RANK-BASED DIFFICULTY FILTERING:
     *   Now also accepts rankLevel so the swap candidate is drawn from the same
     *   eligible pool as getMissionForToday(rankLevel). A swap cannot present a
     *   mission that the user's rank level would not normally see.
     *
     *   A swap is allowed only when the candidate mission's difficulty satisfies:
     *     candidate.difficulty >= currentDifficulty - 2  AND
     *     candidate.difficulty <= currentDifficulty
     *
     *   In plain English: you can swap to a mission of the SAME difficulty, or up to
     *   2 levels EASIER. You cannot swap UP to something harder, and you cannot drop
     *   more than 2 difficulty levels — this prevents the app from being used to
     *   consistently dodge hard missions.
     *
     * The search starts one position after today's mission in the eligible list and wraps.
     * Today's mission is always skipped (offset starts at 1).
     *
     * Returns null if no mission in the eligible pool satisfies the difficulty window.
     * Callers must handle null.
     *
     * @param currentDifficulty  The difficulty of today's currently active mission (1–5).
     * @param rankLevel          The user's current rank level (1–5). Filters the pool.
     * @return                   The first compatible swap candidate, or null if none exists.
     */
    fun getAlternateMission(currentDifficulty: Int, rankLevel: Int = 1): Mission? {
        // Clamp rankLevel to a valid range.
        val maxDifficulty = rankLevel.coerceIn(1, 5)

        // Build the same eligible pool as getMissionForToday() for this rank.
        // Swap candidates must come from the same pool, not from higher-difficulty missions
        // that the user's rank doesn't yet permit.
        val eligibleMissions = ALL_MISSIONS.filter { it.difficulty <= maxDifficulty }
            .ifEmpty { ALL_MISSIONS }

        // Find today's mission's position within the ELIGIBLE pool (not ALL_MISSIONS),
        // so the forward scan starts from the right index.
        val todayMission = getMissionForToday(rankLevel)
        val todayIndex = eligibleMissions.indexOfFirst { it.id == todayMission.id }
            .takeIf { it >= 0 }
            ?: (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % eligibleMissions.size)

        // Scan forward through the eligible list, starting one position after today's mission.
        // offset starts at 1 so we always skip today's own mission.
        for (offset in 1..eligibleMissions.size) {
            val candidateIndex = (todayIndex + offset) % eligibleMissions.size
            val candidate = eligibleMissions[candidateIndex]

            // Swap rule: candidate difficulty must be equal to current, or up to 2 levels easier.
            // candidateD >= currentD - 2  →  not more than 2 levels easier
            // candidateD <= currentD      →  not harder than current
            val diffDiff = currentDifficulty - candidate.difficulty
            val isCompatible = diffDiff in 0..2   // 0 = same, 1 = one easier, 2 = two easier

            if (isCompatible) {
                return candidate
            }
        }

        // No compatible mission found in the eligible pool.
        // This can happen at low ranks with few missions. Callers must handle null.
        return null
    }
}
    