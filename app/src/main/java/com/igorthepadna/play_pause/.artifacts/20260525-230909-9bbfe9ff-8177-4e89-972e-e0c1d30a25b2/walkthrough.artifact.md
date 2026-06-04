# High-Density Grid Refinement

I have refined the grid UI for Albums and Artists to provide better accessibility and a more balanced look in high-density modes.

## Changes Implemented

### User-Facing Changes
- **Quick-Play Buttons:**
    - Added a functional play button overlay to both **Albums** and **Artists** in "Auto" and "Medium" (3-column) views.
    - You can now start playback directly from the grid without having to open the detail page first.
- **Improved Typography:**
    - Reduced the text size for album titles and artist names in "Medium" (3-column) mode.
    - This ensures that long titles don't clutter the UI and maintains a clean, "Material You 3 Expressive" aesthetic at higher densities.
- **Consistent Scaling:**
    - Play buttons and text styles now adapt dynamically across all grid sizes (Small, Medium, Large, and Auto).

### Technical Changes (Under the Hood)
- **Component Updates:**
    - Updated `AlbumCard.kt` to enable the play button for up to 3 columns and scale its size (from 42dp down to 36dp for density).
    - Updated `ArtistCard.kt` to add the play button overlay and implement responsive text styling using `labelLarge` for medium grids.
- **Navigation & Control:**
    - Integrated `onPlayClick` callbacks in `LibraryScreen.kt` and `HomeHubScreen.kt` to trigger playback for artists and albums directly.
- **Visual Polish:**
    - Adjusted vertical spacing and padding to prevent metadata from feeling cramped in 3-column layouts.

## Verification Summary
- Verified that play buttons appear correctly on both Albums and Artists in 3-column mode.
- Confirmed that tapping the play button triggers immediate playback of the item's songs.
- Verified that text sizes are reduced in Medium mode as requested, resulting in a cleaner look.
- Ensured that "Small" (5-column) mode remains minimal by keeping buttons and extra text hidden.
