# Card Deck Dimension & Typography Spec

Standardized geometry used by custom playing-card decks in `app/src/main/assets/<deck_folder>/` so future decks preserve balanced visibility across corner ranks, index suit pips, upper-right artwork, and lower primary suit pips on both phones and tablets.

---

## 1. Canvas, Frame, and Aspect Ratio
- **Output Size**: `450 × 630 px` RGBA (`1 : 1.40` playing-card aspect ratio)
- **Supersampling**: Render vector pips, typography, and borders at `4×` resolution and downsample with `LANCZOS` resampling.
- **Corner Radius**: `32 px` (`7.11%` of width)
- **Interior Fill**: `#FFFFFF` (transparent outside the rounded card mask)
- **Outer Frame Border (Modern Crest style)**: subtle `1.25 px` `#CBD5E1` rounded hairline stroke (omitted on borderless arcade styles like Vintage Crest)

---

## 2. Corner Rank & Index Suit Column (Left Edge)
All corner elements share a horizontal axis at **`center_x = 76 px` (`16.9% W`)**, leaving the right portion of the upper half unobstructed for character/crest illustrations.

### Rank Glyph (`2–9, 10, J, Q, K, A`)
- **Target Height**: **`118 px`** (`18.73%` of card height)
- **Top Offset (`y`)**: `24 px` (`3.81% H`)
- **Left Margin Guard (`min_x`)**: `22 px` (`4.89% W`) so wide glyphs (`10`, `A`, `K`, `Q`) never clip the rounded top-left border
- **Ten (`10`)**: Render `1` and `0` individually and composite with a `6 px` gap (at `1×`), capping combined width at **`108 px`**
- **Colors**:
  - Black suits (`c`, `s`): `#000000`
  - Red suits (`d`, `h`): `#D81919`
- **Typography Preset (Modern Crest default)**:
  - Font: `Tw Cen MT Condensed Extra Bold` (`C:\Windows\Fonts\TCCEB.TTF`)
  - Characteristics: high-contrast geometric condensed bold matching Vintage Crest's instant glance legibility while keeping open modern counters.

### Corner Index Suit Pip
- **Height**: **`118 px`** (`18.73% H`)
- **Width**: **`100 px`** for diamonds (`d`); **`108 px`** for spades/hearts/clubs (`s`, `h`, `c`)
- **Position**: Centered horizontally at `x = 76 px`, placed **`14 px`** below the rank bottom edge (`y = 156 px`)

---

## 3. Upper-Right Artwork Bay (`J`, `Q`, `K`, `A`)
- **Bay Bounds**: `x = 138..432` (**`294 px` wide**, `65.3% W`), `y = 18..328` (**`310 px` tall**, `49.2% H`)
- **Placement**: Crisp vector-ink cel-shaded RGBA illustrations centered horizontally inside the bay (`scale = min(max_w / w, max_h / h)` preserving aspect ratio), with Black suits (`♠`/`♣`) featuring dark hair, brown eyes, cool slate-navy outfits, all-blue Ace shield, and Obsidian/Hazel Pixel devices, and Red suits (`♥`/`♦`) featuring blonde hair, blue eyes, crimson outfits, crimson Ace shield, and Porcelain Pixel devices.
- **Fit Limits**:
  - King / Queen court busts: `286 × 308 px` (bottom-aligned to bay baseline)
  - Jack drone scene / bust: `290 × 282 px` (bottom-aligned to bay baseline)
  - Ace lion & Wi-Fi shield crest: `286 × 255 px` (centered vertically `-8 px`)

---

## 4. Lower Primary Suit Pip (All 52 Face Cards)
- **Height**: **`252 px`** (`40.00% H`)
- **Width**: **`226 px`** for diamonds (`d`); **`244 px`** for spades/hearts/clubs (`s`, `h`, `c`)
- **Center Point**: **`cx = 210 px` (`46.67% W`)**, **`cy = 466 px` (`73.97% H`)**

---

## 5. Asset Naming & Wiring Checklist for New Card Sets
1. Generate 53 RGBA PNG files inside `app/src/main/assets/<assetFolder>/`:
   - Faces: `<rank><suit>.png` (`2–9, T, J, Q, K, A` × `c, d, h, s`)
   - Back: `back.png` (`450 × 630 px` RGBA with `32 px` rounded corners)
2. Register the new enum entry in `CardDeck` inside `ThemeDefinitions.kt`.
3. Update/run `CardDeckTest.kt` to verify all 53 assets exist and `CardDeck.fromId(...)` resolves cleanly.

To regenerate **Modern Crest** cards from `scripts/modern_crest_parts/`, run:

```bash
python scripts/generate_card_deck.py
```
