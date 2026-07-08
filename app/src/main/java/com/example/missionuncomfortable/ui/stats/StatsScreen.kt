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
 *   HistoryScreen.kt       — Scrollable mission history UI
 *   RankUpScreen.kt        — Full-screen rank-up celebration with spring animation
 *   StatsViewModel.kt      — Computes stats (streaks, category breakdown, avg discomfort)
 *   StatsScreen.kt         — Bar charts: category breakdown + discomfort by day of week ← YOU ARE HERE
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
 * StatsScreen.kt
 *
 * The Stats screen — a summary of the user's mission performance over time.
 *
 * ─── WHAT THIS FILE CONTAINS ────────────────────────────────────────────────
 *
 *   StatsScreen()           — Root composable. Reads from StatsViewModel.
 *   StatSummaryRow()        — A single stat label + value row (total, average, streak).
 *   CategoryBreakdownChart()— Horizontal bar chart: missions completed per category.
 *   DayOfWeekChart()        — Vertical bar chart: average discomfort per day of week.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/stats/StatsScreen.kt
 *
 * ─── DESIGN PRINCIPLE ────────────────────────────────────────────────────────
 *
 *   No chart library. Charts are plain Boxes sized by fraction.
 *   Same gold-on-dark palette as the rest of the app.
 *   Information hierarchy: biggest number first (total missions), details below.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial version. Introduced for Phase 9 (Stats Screen).
 */

package com.example.missionuncomfortable.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.missionuncomfortable.ui.dashboard.MissionCategory

// ─── COLOUR CONSTANTS ────────────────────────────────────────────────────────

private val ColorBackground     = Color(0xFF0D0D0D)
private val ColorSurface        = Color(0xFF1A1A1A)
private val ColorSurfaceVariant = Color(0xFF242424)
private val ColorTextPrimary    = Color(0xFFE0E0E0)
private val ColorTextSecondary  = Color(0xFF8A8A8A)
private val ColorAccentGold     = Color(0xFFC8A84B)

// Human-readable labels for each MissionCategory enum value
private val CATEGORY_LABELS = mapOf(
    MissionCategory.SOCIAL_INITIATION  to "Social Initiation",
    MissionCategory.EYE_CONTACT        to "Eye Contact",
    MissionCategory.REJECTION_PRACTICE to "Rejection Practice",
    MissionCategory.PRESENCE           to "Presence",
    MissionCategory.PHYSICAL           to "Physical",
    MissionCategory.PERFORMANCE        to "Performance"
)

// Short 3-letter labels for days of week (Mon=index 0 … Sun=index 6)
private val DOW_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE — StatsScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel()
) {
    val stats by viewModel.stats.observeAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        if (stats == null || stats!!.totalCompleted == 0) {
            // ── EMPTY STATE ───────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Complete your first mission\nto see your stats.",
                    color = ColorTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
        } else {
            // ── STATS CONTENT ─────────────────────────────────────────────────
            val data = stats!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 48.dp, bottom = 40.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── HEADER ────────────────────────────────────────────────────
                Text(
                    text = "STATS",
                    color = ColorTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                )

                // ── HERO STAT: TOTAL MISSIONS ──────────────────────────────
                // Largest single number on the screen. User's primary proof of progress.
                Text(
                    text = "${data.totalCompleted}",
                    color = ColorAccentGold,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 64.sp
                )
                Text(
                    text = "missions completed",
                    color = ColorTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.offset(y = (-12).dp)   // Tuck under the big number
                )

                // ── SUMMARY ROWS ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorSurface)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatSummaryRow(
                            label = "Average discomfort",
                            value = data.averageDiscomfort?.let {
                                // Locale.US ensures decimal point is always '.' not ','
                                String.format(Locale.US, "%.1f / 10", it)
                            } ?: "—"
                        )
                        StatSummaryRow(
                            label = "Current streak",
                            value = "${data.currentStreak} day${if (data.currentStreak == 1) "" else "s"}"
                        )
                        StatSummaryRow(
                            label = "Best streak",
                            value = "${data.bestStreak} day${if (data.bestStreak == 1) "" else "s"}"
                        )
                    }
                }

                // ── CATEGORY BREAKDOWN CHART ──────────────────────────────────
                if (data.categoryBreakdown.isNotEmpty()) {
                    SectionHeader("BY CATEGORY")
                    CategoryBreakdownChart(breakdown = data.categoryBreakdown)
                }

                // ── DAY OF WEEK CHART ─────────────────────────────────────────
                val hasAnyDowData = data.discomfortByDay.any { it > 0f }
                if (hasAnyDowData) {
                    SectionHeader("DISCOMFORT BY DAY")
                    DayOfWeekChart(discomfortByDay = data.discomfortByDay)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = ColorTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// STAT SUMMARY ROW
// ─────────────────────────────────────────────────────────────────────────────

/**
 * StatSummaryRow — one label + value pair in the summary card.
 *
 * Label is left-aligned and muted. Value is right-aligned and white.
 */
@Composable
private fun StatSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = ColorTextSecondary,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = ColorTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CATEGORY BREAKDOWN CHART — horizontal bars
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CategoryBreakdownChart — horizontal bar chart showing missions per category.
 *
 * Each category gets a row. The bar fills proportionally to its count
 * relative to the category with the most completions.
 *
 * Sorted descending (highest count at the top).
 */
@Composable
private fun CategoryBreakdownChart(breakdown: Map<MissionCategory, Int>) {
    val maxCount = breakdown.values.maxOrNull() ?: 1
    val sorted = breakdown.entries
        .sortedByDescending { it.value }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurface)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sorted.forEach { (category, count) ->
                val label = CATEGORY_LABELS[category] ?: category.name
                val fraction = count.toFloat() / maxCount.toFloat()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Label + count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = ColorTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$count",
                            color = ColorTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Bar: track + fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ColorSurfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(ColorAccentGold)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DAY OF WEEK CHART — vertical bars
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DayOfWeekChart — vertical bar chart: average discomfort per day of week.
 *
 * 7 bars (Mon–Sun), each proportional to that day's average discomfort rating.
 * Bar height is proportional to value relative to the max value in the list.
 * Days with no data show an empty (dark) bar.
 *
 * @param discomfortByDay  7-element list: index 0 = Monday, index 6 = Sunday.
 *                         Value is average discomfort (0f = no data).
 */
@Composable
private fun DayOfWeekChart(discomfortByDay: List<Float>) {
    val maxVal = discomfortByDay.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val chartHeight = 80.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            discomfortByDay.forEachIndexed { index, value ->
                val fraction = if (value > 0f) (value / maxVal) else 0f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Bar
                    Box(
                        modifier = Modifier
                            .height(chartHeight)
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Track (full height, dark)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(3.dp))
                                .background(ColorSurfaceVariant)
                        )
                        // Fill (proportional height, gold)
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ColorAccentGold)
                                    .align(Alignment.BottomCenter)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Day label (Mon, Tue, …)
                    Text(
                        text = DOW_LABELS[index],
                        color = ColorTextSecondary,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
