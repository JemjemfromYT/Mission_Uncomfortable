/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        ⚠  AI ASSISTANT — READ THIS BEFORE EDITING THIS FILE  ⚠         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PROJECT: Mission: Uncomfortable — Android app (Kotlin + Jetpack Compose)
 *
 * KEY FILES for this feature:
 *   AscensionLore.kt        — this file. Story content + colour theme per rank.
 *   AscensionBookScreen.kt  — the full-screen book UI that reads this content.
 *   DashboardViewModel.kt   — fires ascensionEvent when a book should be shown.
 *   NavGraph.kt             — routes to AscensionBookScreen.
 *
 * ── COMMENTING RULES ─────────────────────────────────────────────────────────
 *   1. DO NOT remove existing comments. Update if outdated.
 *   2. ADD comments to every new block you write.
 *   3. CHANGELOG block is at the bottom of the file header — update on every change.
 *
 * ─── STORY PHILOSOPHY ────────────────────────────────────────────────────────
 *
 *   Each rank's book is a SHORT philosophical parable — not a biography, not an
 *   essay. Three pages. Each page is 2–4 sentences. The tone is majestic and
 *   timeless: "Once upon a time…" The lesson speaks to the user directly.
 *
 *   Each story is rooted in a real historical figure, but told as a parable so
 *   the reader connects with the IDEA, not just the person. All facts are drawn
 *   from the public-domain historical record — safe for Play Store release.
 *
 * ─── REAL FIGURES (inspiration, not biography) ───────────────────────────────
 *   L1 Observer  : Roger Bannister — the 4-minute mile (1954)
 *   L2 Initiate  : Wilma Rudolph   — polio, brace, 3 Olympic golds (1960)
 *   L3 Challenger: Ernest Shackleton — Endurance, 28 men, every one survived
 *   L4 Conqueror : Viktor Frankl   — Auschwitz, meaning, 12 million readers
 *   L5 Sovereign : Marcus Aurelius — private Meditations, daily practice
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *   app/src/main/java/com/example/missionuncomfortable/ui/ascension/AscensionLore.kt
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *   v1 — Initial fictional lore. 5 ranks, per-rank AscensionTheme.
 *   v2 — Stories replaced with short philosophical parables rooted in real
 *        historical figures. Each page is 2–4 sentences, majestic and clear.
 *        Themes and data-class structure unchanged.
 */

package com.example.missionuncomfortable.ui.ascension

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// THEME
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AscensionTheme — the deliberate colour palette for one rank's book.
 *
 * 60/30/10 colour rule inside AscensionBookScreen:
 *   60% — near-black background (ColorPageParchment, ColorVoid)
 *   30% — [accentColor]: rank signature hue, ambient glow, ornament, sigil
 *   10% — [hotColor]: brighter ember highlight for drop-cap, particles, flash
 *
 * @param accentColor  30%-weight rank hue.
 * @param hotColor     10%-weight brighter highlight.
 * @param mistColor    Very dark tint of accentColor for the radial background blend.
 * @param sigil        Single glyph engraved on the closed book cover.
 */
data class AscensionTheme(
    val accentColor: Color,
    val hotColor: Color,
    val mistColor: Color,
    val sigil: String
)

// ─────────────────────────────────────────────────────────────────────────────
// LORE DATA CLASS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AscensionLore — all story and theme data for one rank's Ascension Book.
 *
 * @param bookTitle    Shown on the closed cover and the first page.
 * @param openingLine  A single sentence that opens the book — sets the tone.
 * @param pages        3 body pages. Each is a short paragraph (2–4 sentences).
 *                     Keep each entry short enough to fit the 300×300dp book page.
 * @param closingLine  The final address to the reader, naming their new rank.
 * @param theme        The colour theme for this rank's book.
 */
data class AscensionLore(
    val bookTitle: String,
    val openingLine: String,
    val pages: List<String>,
    val closingLine: String,
    val theme: AscensionTheme
)

// ── PER-RANK LORE ─────────────────────────────────────────────────────────────
// Keyed by Rank.level (1–5). See DashboardModels.ALL_RANKS.
// Stories are SHORT philosophical parables — 2–4 sentences per page.
private val loreByLevel: Map<Int, AscensionLore> = mapOf(

    // ── LEVEL 1 — OBSERVER ─────────────────────────────────────────────────
    // Parable of the wall that was only ever believed.
    // Historical root: Roger Bannister, 1954, first sub-4-minute mile.
    // Theme: pale watchful moonlight. Nothing won yet — only seen clearly.
    1 to AscensionLore(
        bookTitle = "The Book of the Watcher",
        openingLine = "Once upon a time, the world decided what was possible — and most people believed it.",
        pages = listOf(
            // Page 1 — The wall
            "The four-minute mile was a wall. Every doctor, every coach, every champion had agreed: the human body could not pass it. And so, for years, none did.",
            // Page 2 — One man looks differently
            "One man chose to look at the wall differently. He studied it the way a doctor studies a symptom — carefully, without assumption. He found that the wall was made entirely of belief.",
            // Page 3 — The revelation
            "He ran. Three minutes, fifty-nine. Within the year, a dozen others crossed the same line. The wall had never been real. It had only ever been agreed upon."
        ),
        closingLine = "You are the Observer now. See things as they are — not as you were told to see them.",
        theme = AscensionTheme(
            accentColor = Color(0xFF6E8CA8),   // pale watchful moonlight blue
            hotColor    = Color(0xFFB9CBDA),   // brighter moon-mist highlight
            mistColor   = Color(0xFF141A21),   // near-black with a whisper of blue
            sigil       = "\u25CE"             // ◎ — an open, watching eye/ring
        )
    ),

    // ── LEVEL 2 — INITIATE ─────────────────────────────────────────────────
    // Parable of the first step taken when no one was watching.
    // Historical root: Wilma Rudolph — polio, brace, 3 Olympic golds, 1960.
    // Theme: amber dawn. The first door, finally pushed open.
    2 to AscensionLore(
        bookTitle = "The Book of the First Step",
        openingLine = "Once upon a time, a girl was born into the world with a leg that would not work — and was told it never would.",
        pages = listOf(
            // Page 1 — The agreement
            "She wore a metal brace from the age of five. The doctors were not unkind; they simply told the truth as they understood it. She would not walk normally. This was agreed.",
            // Page 2 — The step
            "One Sunday morning, she walked into church without the brace. There was no ceremony. She had simply decided, alone and quietly, that the agreement no longer applied to her.",
            // Page 3 — What it meant
            "Eight years later, she won three gold medals at the Olympic Games in Rome — the fastest woman on earth. The gold was not the moment. The moment was the church, the brace left behind, the first unaided step."
        ),
        closingLine = "You are the Initiate now. The first step was never the easy one — it was always the only one that mattered.",
        theme = AscensionTheme(
            accentColor = Color(0xFFC8A84B),   // gold dawn
            hotColor    = Color(0xFFF0D98A),   // brighter first-light highlight
            mistColor   = Color(0xFF1E1A0E),   // near-black with a whisper of amber
            sigil       = "\u25B3"             // △ — an ascending point, a first step
        )
    ),

    // ── LEVEL 3 — CHALLENGER ───────────────────────────────────────────────
    // Parable of beginning again when all plans have failed.
    // Historical root: Ernest Shackleton — Endurance, 1914–1916. All 28 survived.
    // Theme: deep crimson-bronze. Choosing the hard road deliberately.
    3 to AscensionLore(
        bookTitle = "The Book of the Difficult Road",
        openingLine = "Once upon a time, the ice swallowed a ship called Endurance — and twenty-eight men were left standing on the frozen ocean.",
        pages = listOf(
            // Page 1 — The catastrophe
            "By every measure, they should have perished. No radio. No rescue. The nearest harbour was hundreds of miles through the worst waters on earth. Their captain looked at this — and made a plan.",
            // Page 2 — What the Challenger does
            "He kept the men fed, warm, and purposeful. He rationed the food to the gram. He moved when the ice shifted and sailed when the sea opened. He did not ask permission from the conditions.",
            // Page 3 — What it earns
            "Two years later, every man was rescued. The impossible had required only one thing: someone willing to begin — and to begin again each morning, without complaint and without permission."
        ),
        closingLine = "You are the Challenger now. Conditions do not grant permission. You do.",
        theme = AscensionTheme(
            accentColor = Color(0xFFA24B3E),   // deep crimson-bronze
            hotColor    = Color(0xFFE08A64),   // ember-orange highlight
            mistColor   = Color(0xFF1F120F),   // near-black with a whisper of ember
            sigil       = "\u25C6"             // ◆ — a hardened, faceted stone
        )
    ),

    // ── LEVEL 4 — CONQUEROR ────────────────────────────────────────────────
    // Parable of the last freedom that cannot be taken.
    // Historical root: Viktor Frankl — Auschwitz, Man's Search for Meaning, 1946.
    // Theme: royal violet + gold. Discomfort losing its authority.
    4 to AscensionLore(
        bookTitle = "The Book of the Unshaken",
        openingLine = "Once upon a time, a man arrived at the gates of Auschwitz with nothing — not even his name.",
        pages = listOf(
            // Page 1 — The loss
            "They had taken everything: his family, his work, his manuscript, his freedom. He watched which prisoners endured and which did not. The difference was rarely physical. The ones who survived had a reason.",
            // Page 2 — His reason
            "He made his reason the book he would write when he was free. He kept the pages in his mind, adding to them in stolen moments between forced labour. That place could not enter there.",
            // Page 3 — What it earns
            "He was freed. He wrote the book in nine days. Twelve million have read it since. The last freedom, he wrote, is the power to choose your own response — and that freedom cannot be taken."
        ),
        closingLine = "You are the Conqueror now. What cannot be taken from you is the only thing that ever truly mattered.",
        theme = AscensionTheme(
            accentColor = Color(0xFF6C4B9E),   // royal violet
            hotColor    = Color(0xFFD9B95C),   // royal gold highlight
            mistColor   = Color(0xFF171025),   // near-black with a whisper of violet
            sigil       = "\u26E8"             // ⛨ — a shield, unshaken
        )
    ),

    // ── LEVEL 5 — SOVEREIGN ────────────────────────────────────────────────
    // Parable of the practice that never ends — sovereignty as a daily return.
    // Historical root: Marcus Aurelius — private Meditations, 161–180 AD.
    // Theme: molten radiant white-gold — the apex rank.
    5 to AscensionLore(
        bookTitle = "The Book of the Sovereign",
        openingLine = "Once upon a time, the most powerful man in the world kept a private notebook — and filled it with his failures.",
        pages = listOf(
            // Page 1 — The man and his notebook
            "He ruled sixty-five million people and commanded armies that stretched to the edge of the world. At night, he wrote about his impatience. His vanity. The times he had spoken before he had listened. He did not record his victories.",
            // Page 2 — The practice
            "The notebook was never meant to be read. It was a daily argument between who he was and who he had decided to become. He never declared himself finished. He simply returned to the work, each morning, without ceremony.",
            // Page 3 — The revelation
            "He never finished the notebook. It could not be finished. Sovereignty, he understood, is not a height you reach. It is the practice of returning — every day, however imperfectly — to who you decided to be."
        ),
        closingLine = "You are the Sovereign now. The practice is the throne. Return to it daily.",
        theme = AscensionTheme(
            accentColor = Color(0xFFE8C15A),   // molten radiant gold
            hotColor    = Color(0xFFFFF3D2),   // near-white ember highlight
            mistColor   = Color(0xFF241C0C),   // near-black with a whisper of molten gold
            sigil       = "\u2726"             // ✦ — a four-point radiant star
        )
    )
)

/**
 * getLoreForRank — returns the AscensionLore for the given rank level.
 *
 * Falls back to Observer (level 1) if an unrecognised level is passed.
 * In practice this fallback never triggers — DashboardModels defines exactly
 * levels 1 through 5 — but it prevents a crash if that ever changes.
 *
 * @param level  Rank.level, 1 (Observer) through 5 (Sovereign).
 */
fun getLoreForRank(level: Int): AscensionLore =
    loreByLevel[level] ?: loreByLevel.getValue(1)
