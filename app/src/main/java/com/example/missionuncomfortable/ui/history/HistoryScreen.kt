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
 *   DashboardViewModel.kt  — All game logic: XP, streaks, rank-ups, history saving, core loop
 *   DashboardScreen.kt     — Main UI. v5: SWAP MISSION button shows a popup, does NOT swap.
 *   HistoryModels.kt       — CompletedMissionEntry data class
 *   HistoryViewModel.kt    — Reads history from SharedPreferences JSON
 *   HistoryScreen.kt       — Scrollable mission history UI (this file)
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
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these rules, the developer will have to   ║
 * ║  manually fix every file you touch. Please respect these conventions.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * HistoryScreen.kt
 *
 * The Mission History screen — a scrollable log of every mission the user has completed.
 *
 * ─── WHAT THIS FILE CONTAINS ────────────────────────────────────────────────
 *
 *   HistoryScreen()      — Root composable. Reads entries from HistoryViewModel
 *                          and decides whether to show the empty state or the list.
 *
 *   HistoryEntryRow()    — One card in the history list. Shows date, mission title,
 *                          a visual discomfort bar (1–10), and XP earned.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/history/HistoryScreen.kt
 *
 * ─── DESIGN AESTHETIC ────────────────────────────────────────────────────────
 *
 *   Same palette as DashboardScreen — dark, stoic, minimal.
 *   Background: near-black #0D0D0D
 *   Cards: dark grey #1A1A1A
 *   Gold accent #C8A84B for XP, discomfort bar fill, and count
 *   Secondary text: muted grey #8A8A8A
 *
 * ─── HOW TO NAVIGATE TO THIS SCREEN ─────────────────────────────────────────
 *
 *   Add a "HISTORY" button to the bottom of DashboardContent, or add a bottom
 *   navigation bar with two tabs: Dashboard and History. Use NavController to
 *   navigate between them. This screen does not yet have a NavController route —
 *   add one when you set up navigation in the app.
 *
 * ─── FUTURE WORK ─────────────────────────────────────────────────────────────
 *
 *   - Replace SharedPreferences JSON storage with Room database + DAO.
 *   - Add filter/sort options (by category, by discomfort rating, by date).
 *   - Add a "streak calendar" heatmap view above the list.
 *   - Tapping a row could expand it to show full mission rules.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial version. Introduced for Phase 6 (Mission History Screen).
 */

package com.example.missionuncomfortable.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// ─── COLOUR CONSTANTS ────────────────────────────────────────────────────────
// Matches DashboardScreen palette — defined here locally to avoid a shared
// dependency until a proper design system module is set up.

private val ColorBackground     = Color(0xFF0D0D0D)   // Near-black background
private val ColorSurface        = Color(0xFF1A1A1A)   // Dark grey card background
private val ColorSurfaceVariant = Color(0xFF242424)   // Slightly lighter — discomfort bar track
private val ColorTextPrimary    = Color(0xFFE0E0E0)   // Off-white — mission title
private val ColorTextSecondary  = Color(0xFF8A8A8A)   // Muted grey — date, labels
private val ColorAccentGold     = Color(0xFFC8A84B)   // Muted gold — XP, discomfort fill, count

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE — HistoryScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * HistoryScreen — root composable for the Mission History screen.
 *
 * This is the ONLY composable that knows about HistoryViewModel. All child
 * composables receive plain data (List<CompletedMissionEntry>) and are
 * therefore easy to test and preview in isolation.
 *
 * Shows either:
 *   - An empty state message if no missions have been completed yet.
 *   - A reverse-chronological scrollable list of completed mission cards.
 *
 * @param viewModel  Provided automatically by viewModel(). Do not pass manually.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel()
) {
    // Observe the entries LiveData from the ViewModel.
    // Default to an empty list while the first value arrives.
    val entries by viewModel.entries.observeAsState(emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        if (entries.isEmpty()) {
            // ── EMPTY STATE ───────────────────────────────────────────────────
            // Shown before the user has completed their first mission.
            // Centred on screen. Plain, no drama.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your history starts with\nyour first completed mission.",
                    color = ColorTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
        } else {
            // ── HISTORY LIST ──────────────────────────────────────────────────
            // Reverse chronological (most recent at the top — already sorted by ViewModel).
            // LazyColumn so the list is memory-efficient regardless of how many entries exist.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 48.dp,
                    bottom = 32.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── HEADER ────────────────────────────────────────────────────
                item {
                    // "MISSION HISTORY" small-caps label
                    Text(
                        text = "MISSION HISTORY",
                        color = ColorTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Large gold number — total completed missions
                    // This is the user's main proof of progress.
                    Text(
                        text = "${entries.size} missions completed",
                        color = ColorAccentGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── ENTRY LIST ────────────────────────────────────────────────
                // entries is already in reverse chronological order from HistoryViewModel.
                // Most recent mission appears at the top.
                items(entries) { entry ->
                    HistoryEntryRow(entry = entry)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ENTRY ROW — one card per completed mission
// ─────────────────────────────────────────────────────────────────────────────

/**
 * HistoryEntryRow — one card in the history list.
 *
 * Shows:
 *   - Top row: date on the left, XP earned on the right.
 *   - Mission title below.
 *   - Discomfort bar: a visual representation of the 1–10 rating.
 *     The bar fills left-to-right proportionally (rating / 10).
 *     Gold fill on a dark grey track — same visual language as the XP bar.
 *
 * @param entry  The completed mission data to display.
 */
@Composable
private fun HistoryEntryRow(entry: CompletedMissionEntry) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurface)
            .padding(16.dp)
    ) {
        Column {

            // ── TOP ROW: DATE + XP ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date — "2026-07-06" format. Small, muted.
                Text(
                    text = entry.date,
                    color = ColorTextSecondary,
                    fontSize = 11.sp
                )

                // XP earned — gold, right-aligned
                Text(
                    text = "+${entry.xpEarned} XP",
                    color = ColorAccentGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── MISSION TITLE ─────────────────────────────────────────────────
            // The most visually prominent element on the card.
            Text(
                text = entry.missionTitle,
                color = ColorTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── DISCOMFORT BAR ────────────────────────────────────────────────
            // Visual 1–10 rating. Uses the same gold-on-dark pattern as the XP bar.
            // "DISCOMFORT" label is fixed-width so the bar always starts at the same x.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed-width label so bars align across all entries in the list
                Text(
                    text = "DISCOMFORT",
                    color = ColorTextSecondary,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.width(80.dp)
                )

                // Bar track (dark grey background)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ColorSurfaceVariant)   // Dark grey track
                ) {
                    // Bar fill (gold, proportional to rating)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = entry.discomfortRating / 10f)  // 0.0–1.0
                            .fillMaxHeight()
                            .background(ColorAccentGold)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Numeric rating label — e.g. "8/10"
                Text(
                    text = "${entry.discomfortRating}/10",
                    color = ColorTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
