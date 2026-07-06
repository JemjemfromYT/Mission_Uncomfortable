/**
 * WelcomeScreen.kt
 *
 * The first screen a brand-new user sees when they open Mission: Uncomfortable.
 *
 * ─── WHEN IS THIS SHOWN? ──────────────────────────────────────────────────────
 *
 *   MainActivity checks SharedPreferences for the key "has_seen_welcome" on every launch.
 *   If the key is missing (first install) or false, this screen is shown.
 *   When the user taps "BEGIN YOUR JOURNEY", MainActivity writes true to that key
 *   and switches to DashboardScreen. On every subsequent launch, this screen is skipped.
 *
 * ─── DESIGN AESTHETIC ─────────────────────────────────────────────────────────
 *
 *   Matches the Dashboard exactly:
 *     Background: near-black (#0D0D0D)
 *     Primary text: off-white (#E0E0E0)
 *     Secondary text: muted grey (#8A8A8A)
 *     Accent: muted gold (#C8A84B)
 *   No emojis. No colour outside the established palette.
 *
 * ─── FUTURE WORK ──────────────────────────────────────────────────────────────
 *
 *   - Replace the static screen with a short 2–3 step onboarding flow (Pager composable).
 *   - Add a name / username input so the ViewModel can personalise the Dashboard greeting.
 *   - Persist the onboarding flag to Room / DataStore instead of SharedPreferences once
 *     the database layer is set up.
 *
 * ─── CHANGELOG ───────────────────────────────────────────────────────────────
 *
 *   v1 — Initial implementation.
 *        Single-screen welcome with badge image, title, tagline, and "BEGIN YOUR JOURNEY" button.
 *        Wired to MainActivity via the onStartJourney callback.
 */

package com.example.missionuncomfortable.ui.welcome

// ─── IMPORTS ──────────────────────────────────────────────────────────────────
import androidx.compose.foundation.Image                        // Displays the rank badge image
import androidx.compose.foundation.background                   // Sets background colour
import androidx.compose.foundation.clickable                    // Makes the button tappable
import androidx.compose.foundation.layout.*                    // Box, Column, Spacer, padding, fillMaxSize, etc.
import androidx.compose.foundation.shape.CircleShape            // Clips the badge image to a circle
import androidx.compose.foundation.shape.RoundedCornerShape     // Rounded corners on the CTA button
import androidx.compose.material3.Text                          // Text composable
import androidx.compose.runtime.Composable                      // Marks a function as a composable
import androidx.compose.ui.Alignment                            // Alignment constants
import androidx.compose.ui.Modifier                             // Modifier chain
import androidx.compose.ui.draw.clip                            // Clips a composable to a shape
import androidx.compose.ui.graphics.Color                       // Colour class
import androidx.compose.ui.layout.ContentScale                  // Controls image scaling inside its bounds
import androidx.compose.ui.res.painterResource                  // Loads a drawable resource
import androidx.compose.ui.text.font.FontWeight                 // Bold, SemiBold, etc.
import androidx.compose.ui.text.style.TextAlign                 // Centre / start / end alignment for text
import androidx.compose.ui.unit.dp                              // Density-independent pixels
import androidx.compose.ui.unit.sp                              // Scale-independent pixels for font sizes
import com.example.missionuncomfortable.R                       // App resource references

// ─── COLOUR CONSTANTS ─────────────────────────────────────────────────────────
// Duplicated from DashboardScreen so WelcomeScreen is self-contained and portable.
// When a shared design-system file (e.g., Theme.kt or AppColors.kt) exists, move these there
// and delete the local copies in both screens.

private val ColorBackground    = Color(0xFF0D0D0D)   // Near-black — main screen background
private val ColorTextPrimary   = Color(0xFFE0E0E0)   // Off-white — headings and primary text
private val ColorTextSecondary = Color(0xFF8A8A8A)   // Muted grey — taglines and supporting text
private val ColorAccentGold    = Color(0xFFC8A84B)   // Muted gold — CTA button + accent highlights

// ─────────────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE — WelcomeScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * WelcomeScreen — the intro screen shown on first launch.
 *
 * Stateless: all it needs is a callback to call when the user taps "BEGIN YOUR JOURNEY".
 * MainActivity owns the SharedPreferences write + the navigation decision.
 *
 * @param onStartJourney  Called when the user taps the CTA button.
 *                        MainActivity should: (1) write has_seen_welcome=true to SharedPreferences,
 *                        (2) switch showDashboard state to true so DashboardScreen is shown.
 */
@Composable
fun WelcomeScreen(onStartJourney: () -> Unit) {

    // Full-screen dark background — identical to the Dashboard
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center  // Centre the content column vertically and horizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)  // Generous side padding so text doesn't hug the edges
        ) {

            // ── RANK BADGE IMAGE ───────────────────────────────────────────
            // Reuses the same placeholder badge as the Dashboard.
            // When real badge art is ready, swap for a dedicated welcome/intro image.
            Image(
                painter = painterResource(id = R.drawable.rank_badge_placeholder),
                contentDescription = "Mission Uncomfortable emblem",  // Accessibility label
                contentScale = ContentScale.Fit,     // Scale to fit within the 96dp circle
                modifier = Modifier
                    .size(96.dp)                     // Slightly smaller than the Dashboard's 120dp
                    .clip(CircleShape)               // Mask to a circle — matches the badge design
            )

            Spacer(modifier = Modifier.height(48.dp))  // Generous breathing room below the badge

            // ── MAIN TITLE ────────────────────────────────────────────────
            // Two-line title for visual weight — kept in all-caps for the stoic aesthetic.
            // "WELCOME TO THE" is a lighter preface; "UNCOMFORTABLE" is the powerful word.
            Text(
                text = "WELCOME TO THE",
                color = ColorTextSecondary,          // Muted — the preface, not the hero word
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,               // Wide tracking for a refined, stamp-like feel
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "UNCOMFORTABLE",
                color = ColorTextPrimary,            // Full brightness — this is the brand word
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── TAGLINE ───────────────────────────────────────────────────
            // One sentence that frames the app's purpose without hype.
            // Stoic, direct, no fluff.
            Text(
                text = "Build the muscle of discomfort.\nOne mission at a time.",
                color = ColorTextSecondary,          // Muted — supporting text, not the hero
                fontSize = 14.sp,
                lineHeight = 22.sp,                 // Comfortable line height for the two-line tagline
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))  // Large gap between tagline and CTA button

            // ── CTA BUTTON ────────────────────────────────────────────────
            // "BEGIN YOUR JOURNEY" — primary action. Gold fill, dark text.
            // Styled identically to "ACCEPT THE MISSION" on the Dashboard for design consistency.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()                          // Button spans full column width
                    .clip(RoundedCornerShape(10.dp))        // Rounded corners — same radius as Dashboard buttons
                    .background(ColorAccentGold)            // Muted gold fill — the single accent colour
                    .clickable(onClick = onStartJourney)    // Calls the MainActivity callback
                    .padding(vertical = 16.dp)              // Tall tap target — comfortable for thumbs
            ) {
                Text(
                    text = "BEGIN YOUR JOURNEY",
                    color = Color(0xFF0D0D0D),              // Near-black text on gold — high contrast
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp                 // Wide tracking matches other button labels
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── DISCLAIMER ────────────────────────────────────────────────
            // Sets the tone: this is not a casual app.
            // One sentence, muted, small — the user should almost miss it.
            Text(
                text = "Discomfort is the curriculum.",
                color = ColorTextSecondary.copy(alpha = 0.5f),  // Very faint — almost a watermark
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
