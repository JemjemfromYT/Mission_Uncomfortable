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
 *   L2 Initiate  : Theodore Roosevelt — "The Man in the Arena" speech (1910)
 *   L3 Challenger: Ernest Shackleton — Endurance, 28 men, every one survived
 *   L4 Conqueror : Alexander the Great — never lost a battle in 13 years (356–323 BC)
 *   L5 Sovereign : Marcus Aurelius — private Meditations, daily practice
 *
 * ─── STORY TONE ───────────────────────────────────────────────────────────────
 *   Stories must feel POWERFUL and COOL — majestic, not pitiful.
 *   No disability, no suffering-as-identity, no victim framing.
 *   Lead with what the figure CHOSE and DID, not what was done to them.
 *   Each page: maximum 2 sentences. Tight, sharp, memorable.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *   app/src/main/java/com/example/missionuncomfortable/ui/ascension/AscensionLore.kt
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *   v1 — Initial fictional lore. 5 ranks, per-rank AscensionTheme.
 *   v2 — Stories replaced with short philosophical parables rooted in real
 *        historical figures. Each page is 2–4 sentences, majestic and clear.
 *        Themes and data-class structure unchanged.
 *   v3 — STORY REWRITE + TRIM (tone and length pass).
 *        L2 Initiate: Replaced Wilma Rudolph (pity-framing: disability/brace) with
 *          Theodore Roosevelt "The Man in the Arena" (1910). Pure power and action —
 *          daring to enter the fight, not overcoming a condition. No pity angle.
 *        L4 Conqueror: Replaced Viktor Frankl (suffering-framing: Auschwitz) with
 *          Alexander the Great (356–323 BC). Never lost a battle in 13 years.
 *          World ran out of land before he ran out of will. No victim angle.
 *        All 5 stories trimmed to 2 sentences per page (was 3–5). Tight, sharp.
 *        All illustrations updated in AscensionBookScreen.kt to match new stories.
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
            // Page 1 — The wall (trimmed: 2 sentences)
            "The four-minute mile was a wall. Every doctor, coach, and champion had agreed: the human body could not pass it.",
            // Page 2 — One man looks differently (trimmed: 2 sentences)
            "One man studied the wall like a doctor studies a symptom — carefully, without assumption. He found it was made entirely of belief.",
            // Page 3 — The revelation (trimmed: 2 sentences)
            "He ran. Three minutes, fifty-nine — and within the year, a dozen others crossed the same line."
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
    // Parable of daring to step into the arena — not watching from the stands.
    // Historical root: Theodore Roosevelt — "The Man in the Arena" speech, 1910.
    // Theme: amber dawn. Bold first action. Entering the fight.
    // v3: Replaced Wilma Rudolph story (pity-framing) with Roosevelt (pure power/action).
    //     Illustrations updated: leg brace → crossed swords, footprint → cowboy hat.
    2 to AscensionLore(
        bookTitle = "The Book of the Arena",
        openingLine = "Once upon a time, a man described the only person in the world who truly counts.",
        pages = listOf(
            // Page 1 — The man in the arena (2 sentences)
            "He called him the man in the arena — not the critic in the stands, not the voice that says it cannot be done. The one whose face is marked with dust and effort, who dares to try.",
            // Page 2 — Roosevelt lived it (2 sentences)
            "Roosevelt had lived that speech entirely. He charged up enemy hills on horseback, ranched cattle alone in the Badlands, and commanded the most powerful nation on earth by forty-two.",
            // Page 3 — The lesson (2 sentences)
            "The arena is the only place where anything real is decided. The stands count for nothing."
        ),
        closingLine = "You are the Initiate now. You have stepped into the arena. The stands are empty.",
        theme = AscensionTheme(
            accentColor = Color(0xFFC8A84B),   // gold dawn (unchanged)
            hotColor    = Color(0xFFF0D98A),   // brighter first-light highlight
            mistColor   = Color(0xFF1E1A0E),   // near-black with a whisper of amber
            sigil       = "\u25B3"             // △ — ascending point, stepping into the arena
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
            // Page 1 — The catastrophe (trimmed: 2 sentences)
            "No radio, no rescue — the nearest harbour was hundreds of miles through the worst water on earth. Their captain looked at this and made a plan.",
            // Page 2 — What the Challenger does (trimmed: 2 sentences)
            "He kept the men fed, warm, and purposeful, moving when the ice shifted and sailing when the sea opened.",
            // Page 3 — What it earns (trimmed: 2 sentences)
            "Two years later, every man was rescued. The impossible had required only one thing: someone willing to begin again each morning."
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
    // Parable of the will that runs out of world before it runs out of want.
    // Historical root: Alexander the Great — never lost a battle in 13 years (356–323 BC).
    // Theme: royal violet + gold. Pure conquest energy. No pity, all power.
    // v3: Replaced Viktor Frankl story (suffering-framing) with Alexander the Great.
    //     Illustrations updated: barbed wire/candle/book → spear/Macedonian star/crown.
    4 to AscensionLore(
        bookTitle = "The Book of the Unshaken",
        openingLine = "Once upon a time, a twenty-year-old king was told his empire was the size it would always be — and he disagreed.",
        pages = listOf(
            // Page 1 — The march (2 sentences)
            "In thirteen years, he never lost a battle. He marched from Greece to Egypt, across Persia, through the Hindu Kush, to the edge of India — always forward, never retreating.",
            // Page 2 — How he led (2 sentences)
            "He camped with his soldiers, ate what they ate, and fought at the front of every charge. His men followed not because they had to — but because no other life felt worth living.",
            // Page 3 — The lesson (2 sentences)
            "The world ran out of land before he ran out of will. The only boundary that ever stopped him was drawn by death — not defeat."
        ),
        closingLine = "You are the Conqueror now. The only boundary that exists is the one you agree to.",
        theme = AscensionTheme(
            accentColor = Color(0xFF6C4B9E),   // royal violet (unchanged)
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
            // Page 1 — The man and his notebook (trimmed: 2 sentences)
            "He ruled sixty-five million people and commanded armies to the edge of the world. At night, he wrote about his impatience, his vanity, the times he spoke before he listened.",
            // Page 2 — The practice (trimmed: 2 sentences)
            "The notebook was a daily argument between who he was and who he had decided to become. He never declared himself finished.",
            // Page 3 — The revelation (trimmed: 2 sentences)
            "Sovereignty, he understood, is not a height you reach. It is the practice of returning — every day, however imperfectly — to who you decided to be."
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
