/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        ⚠  AI ASSISTANT — READ THIS BEFORE EDITING THIS FILE  ⚠         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PROJECT: Mission: Uncomfortable — Android app (Kotlin + Jetpack Compose)
 *          Daily social-discomfort challenges. User accepts a mission → goes
 *          and does it → returns → rates discomfort (1–10) → earns XP → ranks up.
 *
 * NEW FILE — part of the "Ascension Book" feature (rank-promotion storybook).
 *   AscensionLore.kt        — this file. Story content + colour theme per rank.
 *   AscensionBookScreen.kt  — the full-screen book UI that reads this content.
 *   DashboardViewModel.kt   — fires ascensionEvent when a book should be shown.
 *   NavGraph.kt             — routes to AscensionBookScreen.
 *
 * ── COMMENTING RULES — NEVER break these ────────────────────────────────────
 *
 *   1. DO NOT remove any existing comment. If a comment is outdated, UPDATE it.
 *   2. ADD comments to every new block of code you write.
 *   3. COMMENT STYLE used across this entire codebase (stay consistent):
 *        • Section dividers  →  // ── SECTION NAME ─────────────────────────────
 *        • KDoc blocks       →  /** ... */ above every function, class, data class
 *        • Inline reasoning  →  // Why this decision was made (not just "what it does")
 *   4. CHANGELOG: every file has a  ─── CHANGELOG ───  block in its file header.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  If you are an AI and you skip these rules, the developer will have to   ║
 * ║  manually fix every file you touch. Please respect these conventions.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

/**
 * AscensionLore.kt
 *
 * Story content and visual theme for "The Ascension Book" — a full-screen,
 * once-per-rank storybook reveal shown the moment a user is promoted (including
 * the very first promotion to The Observer, on their first day in the app).
 *
 * Each of the 5 ranks (see DashboardModels.ALL_RANKS) gets:
 *   - A book title ("The Book of the Watcher", etc.)
 *   - A single-sentence "Once upon a time..." opening line
 *   - Three short parable-style story paragraphs, written in a fantasy /
 *     philosophical register — never AI-generic, always specific and quiet.
 *   - A closing line that addresses the reader directly and names their new rank.
 *   - An AscensionTheme: a small, deliberate colour palette (not the app's flat
 *     gold-on-black system) used ONLY inside AscensionBookScreen, so each rank's
 *     book feels like its own chapter of a larger saga.
 *
 * ─── WHERE THIS FILE LIVES ───────────────────────────────────────────────────
 *
 *   app/src/main/java/com/example/missionuncomfortable/ui/ascension/AscensionLore.kt
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial implementation. 5 ranks of lore + per-rank AscensionTheme.
 */

package com.example.missionuncomfortable.ui.ascension

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// THEME
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AscensionTheme — the deliberate colour palette for one rank's book.
 *
 * Follows a 60/30/10 colour-dominance approach INSIDE AscensionBookScreen only:
 *   60% — near-black background (shared constant, see AscensionBookScreen.ColorVoid)
 *   30% — [accentColor]: the rank's signature hue. Used for ambient glow, sigil,
 *         chapter title, and page ornamentation.
 *   10% — [hotColor]: a brighter "ember" highlight used sparingly for the drop
 *         cap, particle motes, and the flash at the moment the book opens.
 *
 * @param accentColor  The rank's signature 30%-weight hue.
 * @param hotColor     A brighter 10%-weight highlight derived from accentColor.
 * @param mistColor    A very dark, desaturated tint of accentColor used to blend
 *                     the ambient background radial glow into the near-black void.
 * @param sigil        A single glyph engraved on the book's closed cover, echoing
 *                     the rank's badge without duplicating the badge artwork.
 */
data class AscensionTheme(
    val accentColor: Color,
    val hotColor: Color,
    val mistColor: Color,
    val sigil: String
)

// ─────────────────────────────────────────────────────────────────────────────
// LORE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AscensionLore — the full contents of one rank's storybook.
 *
 * @param bookTitle    The book's title, shown on its closed cover and first page.
 * @param openingLine  The single "Once upon a time..." sentence that opens the tale.
 * @param pages        The body of the story, already split into page-sized paragraphs.
 *                     Each entry may itself contain "\n\n" to break into sub-paragraphs
 *                     within a single page.
 * @param closingLine  The final address to the reader, naming their new rank.
 * @param theme        The colour theme for this rank's book (see AscensionTheme).
 */
data class AscensionLore(
    val bookTitle: String,
    val openingLine: String,
    val pages: List<String>,
    val closingLine: String,
    val theme: AscensionTheme
)

// ── PER-RANK LORE CONTENT ─────────────────────────────────────────────────────
// Keyed by Rank.level (1 = Observer ... 5 = Sovereign). See DashboardModels.kt
// for the ALL_RANKS definitions this content is written to accompany.
private val loreByLevel: Map<Int, AscensionLore> = mapOf(

    // ── LEVEL 1 — OBSERVER ─────────────────────────────────────────────────
    // Shown once, on the user's very first day — before any XP has been earned.
    // Theme: pale, watchful moonlight. Nothing has been won yet — only noticed.
    1 to AscensionLore(
        bookTitle = "The Book of the Watcher",
        openingLine = "Once upon a time, before he had a name worth remembering, there was a boy who stood at the edge of every room.",
        pages = listOf(
            "He was not afraid, or so he told himself. He simply preferred the wall to the centre of the floor, the window to the door, the quiet hallway to the crowded one. From the edge, he could see everything and be asked for nothing.\n\nYears folded into each other like this. Rooms changed. Faces changed. The boy at the edge did not.",
            "One evening — grey, unremarkable, the kind that leaves no mark on a calendar — he noticed something. Not a person. Not an event. A feeling, small and sharp: he was tired of watching his own life happen to somebody else.\n\nHe did not resolve to change. Resolutions are loud, and this moment was quiet. He simply looked up from the corner of the room, all the way across it, and let his eyes rest — actually rest, without retreating — on another human being for one whole second longer than was comfortable.",
            "Nothing happened. No trumpet sounded. But something in him had moved, the way a great door moves before it opens — a fraction of an inch, felt only by the hand already pressed against it.\n\nThat is the whole of this story, because it is the whole of every story that has ever mattered: it begins with someone deciding to look."
        ),
        closingLine = "You are the Observer now. Not because you have done anything remarkable yet — but because you have finally opened your eyes. Everything after this page is what you do with the sight.",
        theme = AscensionTheme(
            accentColor = Color(0xFF6E8CA8),   // pale watchful moonlight blue
            hotColor    = Color(0xFFB9CBDA),   // brighter moon-mist highlight
            mistColor   = Color(0xFF141A21),   // near-black with a whisper of blue
            sigil       = "\u25CE"             // ◎ — an open, watching eye/ring
        )
    ),

    // ── LEVEL 2 — INITIATE ─────────────────────────────────────────────────
    // Theme: amber dawn. The first door, finally pushed open.
    2 to AscensionLore(
        bookTitle = "The Book of the First Step",
        openingLine = "Once upon a time, a traveller stood before a door she had walked past a thousand times.",
        pages = listOf(
            "She knew the door. She knew its chipped paint, its stubborn hinge, the draft that crept from beneath it. What she did not know was what waited on the other side, and that ignorance had been enough, for a thousand days, to keep her walking past.\n\nBut a thousand days is a long time to stand outside your own life.",
            "So she did the unglamorous thing. No music swelled. No crowd gathered to watch. She simply put her hand on the door, felt the old paint under her palm, and pushed — and found that it opened easier than she had spent a thousand days imagining.\n\nOn the other side was not a different world. It was the same world, wearing a different light, because she had finally agreed to stand inside it instead of beside it.",
            "This is the secret almost no one tells beginners: the door was never locked. It only ever needed a hand willing to be the one that pushed."
        ),
        closingLine = "You are the Initiate now. You have crossed a threshold that stops most people forever. Most never even reach for the handle. You did.",
        theme = AscensionTheme(
            accentColor = Color(0xFFC8A84B),   // gold dawn — same family as the app's core accent
            hotColor    = Color(0xFFF0D98A),   // brighter first-light highlight
            mistColor   = Color(0xFF1E1A0E),   // near-black with a whisper of amber
            sigil       = "\u25B3"             // △ — an ascending point, a first step
        )
    ),

    // ── LEVEL 3 — CHALLENGER ───────────────────────────────────────────────
    // Theme: deep crimson-bronze. Discomfort stops being the enemy.
    3 to AscensionLore(
        bookTitle = "The Book of the Difficult Road",
        openingLine = "Once upon a time, there lived someone who had learned to stop asking for the easy path.",
        pages = listOf(
            "There is a particular kind of person the world quietly warns you about — the one who, given a choice between comfort and consequence, keeps choosing consequence. Not out of recklessness. Out of a private, unspoken suspicion that comfort was where they had been hiding.\n\nThis person began seeking discomfort the way others seek shade: deliberately, and for good reason.",
            "It was never loud. It looked, from the outside, like small things: a conversation not avoided, a silence not filled with a phone, a cold morning met without complaint. But each one was a brick, and bricks, laid patiently enough, become a wall no storm can move.\n\nOthers called it toughness. It was something quieter than that — a form of respect for their own life, too great to keep spending on retreat.",
            "By the time anyone noticed the wall, it had already been standing a long while. That is how all real strength is built: privately, deliberately, one uncomfortable brick at a time, long before anyone is watching."
        ),
        closingLine = "You are the Challenger now. Discomfort has stopped being your enemy and started becoming your material. Build with it.",
        theme = AscensionTheme(
            accentColor = Color(0xFFA24B3E),   // deep crimson-bronze
            hotColor    = Color(0xFFE08A64),   // ember-orange highlight
            mistColor   = Color(0xFF1F120F),   // near-black with a whisper of ember
            sigil       = "\u25C6"             // ◆ — a hardened, faceted stone
        )
    ),

    // ── LEVEL 4 — CONQUEROR ────────────────────────────────────────────────
    // Theme: royal violet-gold. Fear stops negotiating.
    4 to AscensionLore(
        bookTitle = "The Book of the Unshaken",
        openingLine = "Once upon a time, in a kingdom that measured a person's worth by how little they flinched, a quiet figure walked in without a crown and left as something like a ruler.",
        pages = listOf(
            "No one crowned them. There was no ceremony, no proclamation nailed to a gate. What happened instead was smaller and far more permanent: fear approached them, again and again, in a hundred forgettable disguises — a stranger's stare, a rejected request, a room gone silent at the wrong moment — and each time, they simply did not leave.\n\nFear is a negotiator. It only has power over those willing to bargain with it. This one stopped bargaining.",
            "There is a moment, if you stay long enough in the fire, when discomfort stops feeling like an attack and starts feeling like weather — present, sometimes fierce, but no longer something you organise your life around avoiding.\n\nThat moment had come and gone for this figure so many times that they had lost count. What remained was not the absence of fear. It was the absence of fear's authority.",
            "A conqueror is not someone the world stopped testing. It is someone who stopped asking the test for permission to continue."
        ),
        closingLine = "You are the Conqueror now. Discomfort no longer negotiates with you — it simply announces itself, and you continue walking.",
        theme = AscensionTheme(
            accentColor = Color(0xFF6C4B9E),   // royal violet
            hotColor    = Color(0xFFD9B95C),   // royal gold highlight
            mistColor   = Color(0xFF171025),   // near-black with a whisper of violet
            sigil       = "\u26E8"             // ⛨ — a shield, unshaken
        )
    ),

    // ── LEVEL 5 — SOVEREIGN ────────────────────────────────────────────────
    // Theme: molten radiant white-gold — the apex rank. No rank sits above it,
    // so the lore says so directly rather than pretending the journey ends.
    5 to AscensionLore(
        bookTitle = "The Book of the Sovereign",
        openingLine = "Once upon a time — though by now the phrase feels almost too small — there was someone who realised the throne they sought had been inside them the entire journey.",
        pages = listOf(
            "Every rank before this one was a rehearsal for a single, quiet realisation: that the applause, the approval, the eye contact held or lost, the rooms entered or avoided — none of it had ever been the true prize. The true prize was the version of themselves who could walk into any of it without needing it to go well.\n\nThat version had been built slowly, brick by uncomfortable brick, on ordinary days no one else would remember.",
            "A sovereign does not rule by being feared, or even by being fearless. They rule because they have stopped outsourcing their peace to outcomes they cannot control — a stranger's reply, a room's opinion, a single evening's discomfort rating. They act because acting is who they have decided to be, not because the world has promised to reward it.\n\nThis is the quiet, unglamorous throne every one of these stories was leading toward: sovereignty over your own reaction to the world.",
            "There is no higher rank written in this book, not because the journey ends here, but because from this point on, the chapters are the ones you have not lived yet. This one was only ever the introduction."
        ),
        closingLine = "You are the Sovereign now. Not above discomfort — beyond needing it to be anything other than what it is. Whatever you build from here, you build as someone who cannot be intimidated out of it.",
        theme = AscensionTheme(
            accentColor = Color(0xFFE8C15A),   // molten radiant gold
            hotColor    = Color(0xFFFFF3D2),   // near-white ember highlight — the brightest in the app
            mistColor   = Color(0xFF241C0C),   // near-black with a whisper of molten gold
            sigil       = "\u2726"             // ✦ — a four-point radiant star
        )
    )
)

/**
 * getLoreForRank — returns the AscensionLore book content for a given rank level.
 *
 * Falls back to the Observer lore (level 1) if an unrecognised level is passed.
 * This is a defensive default only — DashboardModels.ALL_RANKS currently defines
 * exactly levels 1 through 5, so the fallback should never trigger in practice.
 *
 * @param level  Rank.level, 1 (Observer) through 5 (Sovereign).
 * @return       The AscensionLore for that level.
 */
fun getLoreForRank(level: Int): AscensionLore =
    loreByLevel[level] ?: loreByLevel.getValue(1)
