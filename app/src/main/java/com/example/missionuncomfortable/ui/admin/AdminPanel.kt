/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        ⚠  AI ASSISTANT — READ THIS BEFORE EDITING THIS FILE  ⚠         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PROJECT: Mission: Uncomfortable — Android app (Kotlin + Jetpack Compose)
 *
 * KEY FILES (read these for full context before making changes):
 *   DashboardModels.kt     — All enums + data classes
 *   DashboardViewModel.kt  — All game logic; SharedPreferences keys live here ← READ THIS
 *   DashboardScreen.kt     — Secret tap activation lives in RankBadgeSection
 *   AdminPanel.kt          — This file ← YOU ARE HERE
 *
 * ── COMMENTING RULES — NEVER break these ────────────────────────────────────
 *   1. DO NOT remove any existing comment.
 *   2. ADD KDoc to every function. Add section dividers inside every composable.
 *   3. Comment WHY, not just what.
 *   4. Add a changelog entry for every significant change.
 *
 * ── SHAREDPREFERENCES KEYS ───────────────────────────────────────────────────
 *   Keys are duplicated from DashboardViewModel intentionally — AdminPanel must
 *   not import ViewModel classes. If you add a new key to DashboardViewModel,
 *   add it here too and keep them in sync.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these rules, the developer will have to  ║
 * ║  manually fix every file you touch. Please respect these conventions.   ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * AdminPanel.kt
 *
 * Secret testing overlay for Mission: Uncomfortable.
 *
 * ─── HOW TO ACTIVATE (keep this secret from users) ───────────────────────────
 *
 *   Tap the rank badge on the main Dashboard screen SEVEN TIMES quickly
 *   (all 7 taps must happen within 3 seconds).
 *   A near-black overlay will appear over the screen with all testing controls.
 *   Tap "CLOSE ADMIN" to dismiss and reload the ViewModel state.
 *
 * ─── WHAT THIS FILE CONTAINS ─────────────────────────────────────────────────
 *
 *   AdminPanel()           — Full-screen overlay composable with all controls.
 *   AdminSectionTitle()    — Small all-caps label above each card group.
 *   AdminCard()            — Rounded dark container for a group of admin controls.
 *   AdminRow()             — Label + value display row (for current state readout).
 *   AdminActionButton()    — Tappable action button (normal or destructive style).
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/admin/AdminPanel.kt
 *
 * ─── IMPORTANT — PRODUCTION SAFETY ──────────────────────────────────────────
 *
 *   The admin panel is only triggered by a secret gesture in DashboardScreen.
 *   For an extra layer of safety before any public release, wrap the trigger
 *   in DashboardScreen with a BuildConfig.DEBUG check:
 *
 *       if (BuildConfig.DEBUG) {
 *           // secret tap counting logic here
 *       }
 *
 *   This ensures the panel is unreachable even if someone discovers the gesture.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial version.
 *          Controls: skip day, force ACTIVE status, set XP for each rank threshold,
 *          full data reset, history-only clear.
 *          Activation: 7 rapid taps on the rank badge (handled in DashboardScreen).
 *
 *   v2 — MISSION CONTROLS section added.
 *          New actions in a dedicated "MISSION CONTROLS" card:
 *            • SWAP MISSION (force random swap — bypasses difficulty gate and daily limit)
 *            • SWAP TO DIFFICULTY 1 / 3 / 5 (quick access to specific difficulty tiers)
 *            • RESET SWAP FLAG (re-enables the in-app SWAP MISSION button for today)
 *          Added KEY_HAS_SWAPPED_TODAY and KEY_MISSION_DATE constants (synced from ViewModel).
 *          Added import of MissionRepository (data object — not a ViewModel, safe to import).
 *
 *   v3 — WARNING CLEANUP (Android Studio build output).
 *          All prefs.edit().putX().commit() and manual editor.commit() patterns converted
 *          to the KTX prefs.edit { putX() } extension (apply() semantics, background write).
 *          Added import androidx.core.content.edit for the KTX extension function.
 *          KEY_MISSION_DATE annotated @Suppress("unused") — constant is intentionally kept
 *          for cross-reference/sync documentation even though AdminPanel does not use it.
 */

package com.example.missionuncomfortable.ui.admin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// MissionRepository import — used by the "SWAP MISSION" admin action so the admin
// can force-swap to any mission in the full library, bypassing the normal difficulty gate.
// MissionRepository is a data object, not a ViewModel class, so this import is safe
// and does not violate the AdminPanel-has-no-ViewModel-dependency rule.
import com.example.missionuncomfortable.data.MissionRepository
// KTX SharedPreferences extension — provides prefs.edit { putX() } syntax with apply() semantics.
// This eliminates the boilerplate val editor = prefs.edit(); editor.putX(); editor.commit() pattern
// and resolves Android Studio warnings about "use KTX extension" and "prefer apply() over commit()".
import androidx.core.content.edit

// ─────────────────────────────────────────────────────────────────────────────
// SHAREDPREFERENCES KEY CONSTANTS
// ─────────────────────────────────────────────────────────────────────────────
// Duplicated from DashboardViewModel intentionally — AdminPanel must not import
// ViewModel classes (that would couple the admin tool to game logic).
// KEEP THESE IN SYNC with DashboardViewModel's companion object.

/** SharedPreferences file name — must match DashboardViewModel.PREFS_NAME. */
private const val PREFS_NAME           = "mission_uncomfortable_prefs"

/** Int — total accumulated XP. Never reset during normal play. */
private const val KEY_TOTAL_XP         = "total_xp"

/** String — "ACTIVE" | "IN_PROGRESS" | "COMPLETED". Today's mission status. */
private const val KEY_MISSION_STATUS   = "mission_status"

/** String — mission id assigned today. Cleared to force a new mission on next load. */
private const val KEY_MISSION_ID       = "mission_id_today"

/** String "yyyy-MM-dd" — last date a mission was completed. */
private const val KEY_LAST_COMPLETED   = "last_completed_date"

/** Int — current consecutive-day streak count. */
private const val KEY_STREAK_COUNT     = "streak_count"

/** String "yyyy-MM-dd" — last date the streak was updated. */
private const val KEY_LAST_STREAK_DATE = "last_streak_date"

// KEY_BEST_STREAK intentionally omitted — AdminPanel does not display or modify
// best_streak directly. It is managed automatically by DashboardViewModel.

/** String — JSON array of CompletedMissionEntry objects. */
private const val KEY_HISTORY_JSON     = "mission_history_json"

/** Boolean — true when the user has already used their one daily swap. */
private const val KEY_HAS_SWAPPED_TODAY = "has_swapped_today"

/** String "yyyy-MM-dd" — the date KEY_MISSION_ID was assigned. Not modified on swap.
 *  Kept for sync documentation — AdminPanel does not read or write the mission date directly.
 *  Suppressed: this constant is intentionally declared here even though AdminPanel never reads
 *  it, because all SharedPreferences keys are duplicated from DashboardViewModel for
 *  cross-reference. Remove the suppress only if you actively use this key below. */
@Suppress("unused")
private const val KEY_MISSION_DATE     = "mission_date_today"

// ─────────────────────────────────────────────────────────────────────────────
// COLOUR CONSTANTS
// ─────────────────────────────────────────────────────────────────────────────
// Local to this file — not shared with Dashboard colours so AdminPanel has
// no dependency on DashboardScreen's private colour definitions.

/** Muted gold — matches Dashboard accent colour. */
private val AdminGold    = Color(0xFFC8A84B)

/** Muted grey — secondary / label text. */
private val AdminMuted   = Color(0xFF8A8A8A)

/** Off-white — primary value text. */
private val AdminWhite   = Color(0xFFE0E0E0)

/** Dark card surface — slightly lighter than background. */
private val AdminSurface = Color(0xFF1A1A1A)

// ─────────────────────────────────────────────────────────────────────────────
// ADMIN PANEL — ROOT COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AdminPanel — full-screen secret testing overlay.
 *
 * Reads and writes SharedPreferences directly so it works without any ViewModel.
 * Most changes take effect after the panel is closed (onRefresh triggers a
 * ViewModel reload). Some changes (like XP) take effect on the next mission
 * completion; others (like skip day) require an app restart or onRefresh.
 *
 * @param onDismiss  Called when the user taps "CLOSE ADMIN". Sets showAdminPanel = false.
 * @param onRefresh  Called just before dismiss — triggers ViewModel.onRefresh() so the
 *                   Dashboard reloads state from the updated SharedPreferences.
 */
@Composable
fun AdminPanel(
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    // ── SHARED PREFERENCES ACCESS ─────────────────────────────────────────────
    // We use LocalContext here instead of injecting Application so AdminPanel
    // stays a pure composable with no ViewModel dependency.
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // ── LIVE STATE — read once on open, updated after each action ─────────────
    // These mirror what's currently stored in SharedPreferences.
    // They update immediately after each action button tap for instant feedback.
    var displayXp     by remember { mutableStateOf(prefs.getInt(KEY_TOTAL_XP, 0)) }
    var displayStatus by remember { mutableStateOf(prefs.getString(KEY_MISSION_STATUS, "ACTIVE") ?: "ACTIVE") }
    var displayDate   by remember { mutableStateOf(prefs.getString(KEY_LAST_COMPLETED, "—") ?: "—") }
    var displayStreak by remember { mutableStateOf(prefs.getInt(KEY_STREAK_COUNT, 0)) }

    // ── FEEDBACK MESSAGE ──────────────────────────────────────────────────────
    // Shown in a green-tinted box after each action so the developer knows
    // the change was committed. Empty string = hidden.
    var feedback by remember { mutableStateOf("") }

    // ── FULL-SCREEN OVERLAY ───────────────────────────────────────────────────
    // A near-opaque Box that covers the entire screen.
    // clickable(enabled = false) prevents taps on the overlay from falling
    // through to the Dashboard composables behind it.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20D0D0D))   // ~95% opaque near-black
            .clickable(enabled = false) {}   // Block tap-through to Dashboard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 48.dp)   // top clearance for status bar
        ) {

            // ── HEADER ────────────────────────────────────────────────────────
            Text(
                text          = "⚙  ADMIN MODE",
                color         = AdminGold,
                fontSize      = 22.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "Secret testing panel — activate: tap badge × 7",
                color    = AdminMuted,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(28.dp))

            // ── SECTION: CURRENT STATE ────────────────────────────────────────
            // Displays the current values in SharedPreferences so you can see
            // exactly what state the app is in before making changes.
            AdminSectionTitle("CURRENT STATE")
            AdminCard {
                AdminRow("Total XP",       "$displayXp XP")
                AdminRow("Mission Status", displayStatus)
                AdminRow("Last Completed", displayDate)
                AdminRow("Streak",         "$displayStreak day(s)")
            }

            Spacer(Modifier.height(24.dp))

            // ── SECTION: DAY CONTROLS ─────────────────────────────────────────
            AdminSectionTitle("DAY CONTROLS")
            AdminCard {

                // SKIP TO NEXT DAY
                // Sets last_completed_date far in the past so the ViewModel sees
                // a new day on next load and picks a fresh mission.
                // Also resets mission_status to ACTIVE and clears mission_id_today
                // so a different mission can be selected.
                // Effect: close the panel → ViewModel.onRefresh() → new mission appears.
                AdminActionButton("SKIP TO NEXT DAY") {
                    prefs.edit {
                        putString(KEY_LAST_COMPLETED,   "2000-01-01")  // Far past = always new day
                        putString(KEY_MISSION_STATUS,   "ACTIVE")      // Reset status
                        remove(KEY_MISSION_ID)                          // Force new mission selection
                        putString(KEY_LAST_STREAK_DATE, "2000-01-01")  // Allow streak to increment
                    }
                    displayDate   = "2000-01-01"
                    displayStatus = "ACTIVE"
                    feedback = "✓ Day skipped. Close panel — Dashboard will load a new mission."
                }

                Spacer(Modifier.height(8.dp))

                // FORCE STATUS → ACTIVE
                // Resets mission_status to ACTIVE without changing the date.
                // Useful if you got stuck in IN_PROGRESS or COMPLETED mid-test
                // and want to see the ACCEPT button again without a day skip.
                AdminActionButton("FORCE STATUS → ACTIVE") {
                    prefs.edit { putString(KEY_MISSION_STATUS, "ACTIVE") }
                    displayStatus = "ACTIVE"
                    feedback = "✓ Status reset to ACTIVE. Close panel to see the ACCEPT button."
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── SECTION: MISSION CONTROLS ─────────────────────────────────────
            // Admin-only mission swap. Bypasses the normal difficulty gate and the
            // one-swap-per-day limit. Useful for testing the mission card UI with
            // a specific mission or after using today's regular swap already.
            AdminSectionTitle("MISSION CONTROLS")
            AdminCard {

                // SWAP TO RANDOM MISSION
                // Picks a random mission from the full 40-mission library that is
                // different from the currently active one, then writes its ID to
                // SharedPreferences. Sets has_swapped_today = true to prevent the
                // user from also using the normal in-app swap after the admin swap.
                // Effect: close the panel → ViewModel.onRefresh() → new mission shown.
                AdminActionButton("SWAP MISSION  (force random swap — bypasses difficulty gate)") {
                    val currentId  = prefs.getString(KEY_MISSION_ID, "") ?: ""
                    // Build the candidate list — every mission EXCEPT the current one.
                    // The admin bypass intentionally includes all difficulty tiers so
                    // you can test any mission at any rank without restrictions.
                    val candidates = MissionRepository.ALL_MISSIONS.filter { it.id != currentId }

                    if (candidates.isNotEmpty()) {
                        // Pick a random candidate from the full eligible list.
                        val newMission = candidates.random()
                        prefs.edit {
                            putString(KEY_MISSION_ID, newMission.id)      // Replace active mission
                            putString(KEY_MISSION_STATUS, "ACTIVE")       // Reset status so ACCEPT is shown
                            putBoolean(KEY_HAS_SWAPPED_TODAY, true)       // Consume daily swap slot
                        }
                        displayStatus = "ACTIVE"
                        feedback = "✓ Mission swapped to '${newMission.title}' (${newMission.id}). Close panel to see the new mission."
                    } else {
                        // Should never happen — the library always has more than one mission.
                        feedback = "⚠ No alternate mission available. Mission library may be empty."
                    }
                }

                Spacer(Modifier.height(8.dp))

                // PICK SPECIFIC DIFFICULTY — swap to the first mission of a chosen difficulty.
                // Useful for quickly testing the difficulty-1 Observer experience or verifying
                // that difficulty-5 missions look correct in the card UI.
                AdminActionButton("SWAP TO DIFFICULTY 1  (first available mission)") {
                    val newMission = MissionRepository.ALL_MISSIONS.firstOrNull { it.difficulty == 1 }
                    if (newMission != null) {
                        prefs.edit {
                            putString(KEY_MISSION_ID, newMission.id)
                            putString(KEY_MISSION_STATUS, "ACTIVE")
                            putBoolean(KEY_HAS_SWAPPED_TODAY, true)
                        }
                        displayStatus = "ACTIVE"
                        feedback = "✓ Swapped to difficulty-1 mission '${newMission.title}'. Close panel."
                    } else {
                        feedback = "⚠ No difficulty-1 mission found in the library."
                    }
                }

                Spacer(Modifier.height(8.dp))

                AdminActionButton("SWAP TO DIFFICULTY 3  (first available mission)") {
                    val newMission = MissionRepository.ALL_MISSIONS.firstOrNull { it.difficulty == 3 }
                    if (newMission != null) {
                        prefs.edit {
                            putString(KEY_MISSION_ID, newMission.id)
                            putString(KEY_MISSION_STATUS, "ACTIVE")
                            putBoolean(KEY_HAS_SWAPPED_TODAY, true)
                        }
                        displayStatus = "ACTIVE"
                        feedback = "✓ Swapped to difficulty-3 mission '${newMission.title}'. Close panel."
                    } else {
                        feedback = "⚠ No difficulty-3 mission found in the library."
                    }
                }

                Spacer(Modifier.height(8.dp))

                AdminActionButton("SWAP TO DIFFICULTY 5  (first available mission)") {
                    val newMission = MissionRepository.ALL_MISSIONS.firstOrNull { it.difficulty == 5 }
                    if (newMission != null) {
                        prefs.edit {
                            putString(KEY_MISSION_ID, newMission.id)
                            putString(KEY_MISSION_STATUS, "ACTIVE")
                            putBoolean(KEY_HAS_SWAPPED_TODAY, true)
                        }
                        displayStatus = "ACTIVE"
                        feedback = "✓ Swapped to difficulty-5 mission '${newMission.title}'. Close panel."
                    } else {
                        feedback = "⚠ No difficulty-5 mission found in the library."
                    }
                }

                Spacer(Modifier.height(8.dp))

                // RESET SWAP FLAG — clears the has_swapped_today flag so the user can
                // use the normal in-app swap button again today. Useful for testing the
                // swap confirmation dialog flow without needing to skip to the next day.
                AdminActionButton("RESET SWAP FLAG  (re-enable today's in-app swap)") {
                    prefs.edit { putBoolean(KEY_HAS_SWAPPED_TODAY, false) }
                    feedback = "✓ Swap flag reset. The in-app SWAP MISSION button is available again today."
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── SECTION: XP / RANK TESTING ────────────────────────────────────
            // Set XP to just below each rank threshold so the next mission
            // completion triggers the rank-up celebration screen.
            // The cheapest mission awards +50 XP, so "threshold − 25" guarantees
            // any mission will push you over the line.
            AdminSectionTitle("XP / RANK TESTING")
            AdminCard {

                // Near Initiate (200 XP threshold)
                AdminActionButton("SET XP → 175  (rank up to The Initiate on next mission)") {
                    prefs.edit { putInt(KEY_TOTAL_XP, 175) }
                    displayXp = 175
                    feedback  = "✓ XP = 175. Complete any mission (+50–150 XP) to hit 200 → rank up to The Initiate."
                }

                Spacer(Modifier.height(8.dp))

                // Near Challenger (500 XP threshold)
                AdminActionButton("SET XP → 475  (rank up to The Challenger on next mission)") {
                    prefs.edit { putInt(KEY_TOTAL_XP, 475) }
                    displayXp = 475
                    feedback  = "✓ XP = 475. Complete any mission to hit 500 → rank up to The Challenger."
                }

                Spacer(Modifier.height(8.dp))

                // Near Conqueror (900 XP threshold)
                AdminActionButton("SET XP → 875  (rank up to The Conqueror on next mission)") {
                    prefs.edit { putInt(KEY_TOTAL_XP, 875) }
                    displayXp = 875
                    feedback  = "✓ XP = 875. Complete any mission to hit 900 → rank up to The Conqueror."
                }

                Spacer(Modifier.height(8.dp))

                // Near Sovereign (1500 XP threshold)
                AdminActionButton("SET XP → 1475  (rank up to The Sovereign on next mission)") {
                    prefs.edit { putInt(KEY_TOTAL_XP, 1475) }
                    displayXp = 1475
                    feedback  = "✓ XP = 1475. Complete any mission to hit 1500 → rank up to The Sovereign."
                }

                Spacer(Modifier.height(8.dp))

                // Reset XP to zero
                AdminActionButton("SET XP → 0  (reset to The Observer)") {
                    prefs.edit { putInt(KEY_TOTAL_XP, 0) }
                    displayXp = 0
                    feedback  = "✓ XP reset to 0. You are back to The Observer."
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── SECTION: DANGER ZONE ──────────────────────────────────────────
            // These actions are irreversible — shown in red to make it obvious.
            AdminSectionTitle("DANGER ZONE")
            AdminCard {

                // FULL WIPE — clears ALL SharedPreferences.
                // Equivalent to a fresh app install.
                // Effect: close panel → ViewModel.onRefresh() → back to Day 1, 0 XP, no history.
                AdminActionButton(
                    label         = "RESET ALL DATA  (full wipe — cannot be undone)",
                    isDestructive = true
                ) {
                    prefs.edit { clear() }
                    displayXp     = 0
                    displayStatus = "ACTIVE"
                    displayDate   = "—"
                    displayStreak = 0
                    feedback = "✓ All data wiped. Close panel — app will start fresh."
                }

                Spacer(Modifier.height(8.dp))

                // HISTORY ONLY — wipes only the mission history log.
                // XP, rank, and streak are preserved.
                // Useful for testing HistoryScreen empty state.
                AdminActionButton(
                    label         = "CLEAR HISTORY ONLY",
                    isDestructive = true
                ) {
                    prefs.edit { putString(KEY_HISTORY_JSON, "[]") }
                    feedback = "✓ History cleared. HistoryScreen will now show empty state."
                }
            }

            // ── FEEDBACK BOX ──────────────────────────────────────────────────
            // Shown after any action button tap. Hidden when feedback is empty.
            if (feedback.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A2A1A))   // Very dark green tint — "success" feel
                        .padding(14.dp)
                ) {
                    Text(
                        text       = feedback,
                        color      = AdminGold,
                        fontSize   = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── CLOSE BUTTON ──────────────────────────────────────────────────
            // Calls onRefresh() before closing so the ViewModel reloads the
            // updated SharedPreferences values into the Dashboard UI.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A200A))   // Dark gold tint — matches accent
                    .clickable {
                        onRefresh()    // Reload ViewModel state from updated prefs
                        onDismiss()    // Hide the panel
                    }
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text          = "CLOSE ADMIN",
                    color         = AdminGold,
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRIVATE HELPER COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AdminSectionTitle — small all-caps section label rendered above each AdminCard.
 *
 * @param title  Section label text (will be displayed as-is, already uppercase by convention).
 */
@Composable
private fun AdminSectionTitle(title: String) {
    Text(
        text          = title,
        color         = AdminMuted,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 2.sp
    )
    Spacer(Modifier.height(8.dp))
}

/**
 * AdminCard — rounded dark surface card that groups related admin controls.
 *
 * @param content  The controls to display inside the card, provided as a Column scope.
 */
@Composable
private fun AdminCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AdminSurface)
            .padding(16.dp)
    ) {
        Column(content = content)
    }
    // Gap after each card — keeps section spacing consistent without requiring
    // the caller to add manual spacers between AdminCard and the next element.
    Spacer(Modifier.height(4.dp))
}

/**
 * AdminRow — key/value display row for the "Current State" section.
 *
 * @param label  Left-aligned descriptive label.
 * @param value  Right-aligned current value (rendered bold for scannability).
 */
@Composable
private fun AdminRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = AdminMuted, fontSize = 12.sp)
        Text(value, color = AdminWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * AdminActionButton — tappable action button inside an AdminCard.
 *
 * Normal style:      dark grey background, gold text.
 * Destructive style: dark red background, red text — visually warns of irreversible action.
 *
 * @param label          Button label text.
 * @param isDestructive  If true, uses the red destructive colour scheme.
 * @param onClick        Action to perform when the button is tapped.
 */
@Composable
private fun AdminActionButton(
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                // Dark red for destructive, slightly lighter dark grey for normal.
                if (isDestructive) Color(0xFF3A0A0A) else Color(0xFF242424)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 8.dp)
    ) {
        Text(
            text          = label,
            color         = if (isDestructive) Color(0xFFFF5555) else AdminGold,
            fontSize      = 12.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}
