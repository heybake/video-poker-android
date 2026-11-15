# Card Deck Dimension & Typography Spec

Standardized geometry used by custom playing-card decks in `app/src/main/assets/<deck_folder>/` so future decks preserve balanced visibility across corner ranks, index suit pips, upper-right artwork, and lower primary suit pips on both phones and tablets.

---

## 1. Canvas, Frame, and Aspect Ratio
- **Output Size**: `450 × 630 px` RGBA (`1 : 1.40` playing-card aspect ratio)
- **Supersampling**: Render vector pips, typography, and borders at `2×`–`4×` resolution and downsample with `LANCZOS` resampling.
- **Corner Radius**: `32 px` (`7.11%` of width)
- **Interior Fill**: `#FFFFFF` (transparent outside the rounded card mask)
- **Outer Frame Border (Modern Crest style)**: `3 px` `#000000` rounded stroke (optional; omitted on borderless arcade styles like Vintage Crest)

---

## 2. Corner Rank & Index Suit Column (Left Edge)
All corner elements share a horizontal axis at **`center_x = 72 px` (`16.0% W`)**, leaving the right `64.9%` of the upper half unobstructed for character/crest illustrations.

### Rank Glyph (`2–9, 10, J, Q, K, A`)
- **Target Height**: **`110 px`** (`17.46%` of card height)
- **Top Offset (`y`)**: `24 px` (`3.81% H`)
- **Left Margin Guard (`min_x`)**: `24 px` (`5.33% W`) so wide glyphs (`10`, `A`, `K`, `Q`) never clip the rounded top-left border
- **Ten (`10`)**: Render `1` and `0` individually and composite with a `~5 px` gap (at `1×`), capping combined width at **`92 px`**
- **Colors**:
  - Black suits (`c`, `s`): `#000000`
  - Red suits (`d`, `h`): `#D40000`
- **Typography Preset (Modern Crest default)**:
  - Font: `Bahnschrift` (`C:\Windows\Fonts\bahnschrift.ttf`)
  - Variable axes: **`Weight = 500`**, **`Width = 78`**
  - Characteristics: slim, open-counter geometric sans that avoids crowding `6`/`8`/`9`/`4`/`7` while never overpowering court illustrations.

### Corner Index Suit Pip
- **Height**: **`112 px`** (`17.78% H`)
- **Width**: **`96 px`** for diamonds (`d`); **`104 px`** for spades/hearts/clubs (`s`, `h`, `c`)
- **Position**: Centered horizontally at `x = 72 px`, placed **`16 px`** below the rank bottom edge (`y = 150 px`, spanning `23.8%..41.6% H`)

---

## 3. Upper-Right Artwork Bay (`J`, `Q`, `K`, `A`)
- **Bay Bounds**: `x = 140..432` (**`292 px` wide**, `64.9% W`), `y = 18..324` (**`306 px` tall**, `48.6% H`)
- **Placement**: Transparent RGBA illustration centered horizontally and vertically inside the bay (`scale = min(max_w / w, max_h / h)` preserving aspect ratio).
- **Recommended Max Fit Limits**:
  - King / Queen court busts: `278 × 304 px`
  - Jack scene / bust: `278 × 268 px` (leaves clear horizontal margin next to wide `J`/`10` shapes)
  - Ace center crest: `272 × 240 px`

---

## 4. Lower Primary Suit Pip (All 52 Face Cards)
- **Height**: **`246 px`** (`39.05% H`)
- **Width**: **`224 px`** for diamonds (`d`); **`238 px`** for spades/hearts/clubs (`s`, `h`, `c`)
- **Center Point**: **`cx = 206 px` (`45.78% W`)**, **`cy = 462 px` (`73.33% H`)**

---

## 5. Asset Naming & Wiring Checklist for New Card Sets
1. Generate 53 RGBA PNG files inside `app/src/main/assets/<assetFolder>/`:
   - Faces: `<rank><suit>.png` using lowercase filenames:
     - Ranks: `2, 3, 4, 5, 6, 7, 8, 9, t, j, q, k, a`
     - Suits: `c, d, h, s`
   - Back: `back.png` (matching `450 × 630 px` aspect ratio with rounded corners)
2. Register the new enum entry in `CardDeck` inside `ThemeDefinitions.kt`.
3. Update/run `CardDeckTest.kt` to verify all 53 assets exist and `CardDeck.fromId(...)` resolves cleanly.

To regenerate **Modern Crest** cards (or scaffold a new set with custom font/border parameters), run:

```bash
python scripts/generate_card_deck.py
```
