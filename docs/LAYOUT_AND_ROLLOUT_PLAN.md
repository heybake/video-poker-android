# Video Poker Android: Landscape Layout & Rollout Plan

## 1. Executive Summary & Purpose

This document captures the finalized, approved landscape layout and design specifications for the standalone, offline **Video Poker Android** application for Dave and Amy, tailored specifically for their **Google Pixel 10 Pro** and **Pixel 10 Pro XL** phones.

Every dimension, spacing decision, color variable, and interaction in this document was derived through iterative visual design and direct testing against the desktop player (`VP_Player`). This document serves as the single source of truth when translating the HTML/CSS prototype into native **Kotlin** and **Jetpack Compose**.

---

## 2. Core Device & Operational Requirements

- **Target Hardware**: Google Pixel 10 Pro (~6.3" display) and Pixel 10 Pro XL (~6.8" display).
- **Aspect Ratio**: 20:9 native resolution (1024 x 460 dp logical canvas).
- **Orientation**: Fixed Landscape only (`android:screenOrientation="landscape"`).
- **Immersive Edge-to-Edge**: Fullscreen layout with hidden navigation/status bars (`fitsSystemWindows=false`).
- **Zero-Scroll Mandate**: Every single UI element—pay table, hold choices, plaque, cards, status indicators, action buttons, and footer—must fit within the 460px vertical height with **zero horizontal or vertical scrolling**.
- **100% Offline & Self-Contained**: Operates seamlessly in Airplane Mode. All 41 game definitions, pay tables, hold list strategy algorithms, card images, sound effects, and themes are bundled directly into the APK. No network calls or cloud dependencies.

---

## 3. Approved Three-Deck Layout & Dimensions

The viewport is divided vertically into three distinct decks:

```
+-------------------------------------------------------------------------+
| TOP DECK (156px): Pay Table (Left) + Hold Choices Panel (Right)         |
|   (Casino Mode: Expands to full-width 5-column pay table)               |
+-------------------------------------------------------------------------+
| MIDDLE DECK (200px):                                                    |
|   [ TWO PAIR Plaque (28px) ]                                            |
|   [ 5 Playing Cards (96x134px) with HELD badges & Choice Dots ]         |
+-------------------------------------------------------------------------+
| BOTTOM DECK (104px):                                                    |
|   Status Bar (24px): WIN  |  Sound Icon  |  BET  |  CASH / CREDIT       |
|   Button Strip (38px): HELP | MORE | OPTIONS | PAYS | SPEED | 1 | MAX | ¢ | DRAW
|   Footer Bar (16px): Variant & Return %  |  Hands & Denom Counters      |
+-------------------------------------------------------------------------+
```

### 3.1. Top Deck: 156px Total Height

The top deck hosts game knowledge and training feedback:

1. **Learn & Practice Modes (Split View)**:
   - **Pay Table Box (`.pay-box`)**: Width 380px, Height 150px.
     - Single-column display using a 2-column CSS Grid (`1fr auto`).
     - Hand name on the left with dotted leader lines (`.dots-fill`) running to the credit value on the right.
     - All 11 pay table rows fit cleanly with 5px top/bottom padding.
     - Only the winning hand row highlights in red with a gentle flash animation.
   - **Hold Choices Panel (`.hold-panel`)**: Flex 1 (remaining width ~620px), Height 150px.
     - Displays the top 3 optimal hold choices derived from strategy tables.
     - Each choice row contains:
       * Numbered badge (`#1`, `#2`, `#3`) with distinct colors (Gold `#ffd166`, Orange `#f77f00`, Red `#d62828`).
       * Mini card chip images (23x16.5px) showing held cards.
       * Keep description text (e.g., `Keep 2 Pair (K, 5)`).
       * Spot numbers list (e.g., `[1, 2, 4, 5]`).
       * Expected Value (EV) display.
       * Green `✓ Top choice` badge on Choice #1.
     - Clean layout without distracting header rows.

2. **Casino Mode (Full-Width View)**:
   - When switched to Casino Mode, the Hold Choices panel disappears completely.
   - The Pay Table expands to 100% width and switches to a 6-column CSS Grid (`1fr repeat(5, 13.5%)`).
   - Columns: Hand Name + 1 Coin, 2 Coins, 3 Coins, 4 Coins, 5 Coins (Max Bet).
   - Vertical divider borders separate each coin column.
   - The active bet column highlights with a subtle gold glow and brighter text.
   - All 11 hands (Royal Flush down to Jacks or Better) remain completely visible inside the gold border with no text cutoffs.

### 3.2. Middle Deck: 200px Total Height

The middle deck hosts the playing surface:

1. **Beveled Hand Plaque (`.hname`)**:
   - Height ~28px.
   - Styled as a beveled metallic plaque with engraved brass borders and soft drop shadow.
   - Displays current hand evaluation (e.g., `TWO PAIR`).
   - Positioned with a **14px dedicated vertical clearance** above the cards so it never collides with the cards or `HELD` badges.
2. **5 Playing Cards**:
   - Card dimensions: **96px width x 134px height** (scaled down from original 154px to preserve vertical breathing room).
   - High-contrast card face images with crisp corners and subtle box shadow.
   - Card lift on hold: moves up slightly with a gold glow border.
3. **HELD Badges (`.held`)**:
   - Placed above held cards with `top: -9px`.
   - Styled with theme-matched gold gradient, bold text, and metallic rim.
4. **Learn Mode Dots (`.card-dots`)**:
   - Located at the bottom-right corner of each card.
   - 3 colored indicator dots corresponding to Choice #1 (Gold), Choice #2 (Orange), and Choice #3 (Red).
   - Instantly shows the player which strategy choices hold which cards.
   - Automatically hidden in Casino Mode.
5. **Vertical Breathing Room**:
   - Exactly **24px clear margin** between the bottom of the cards and the status bar, preventing any feeling of crowding.

### 3.3. Bottom Deck: 104px Total Height

The bottom deck provides machine telemetry, physical controls, and statistical metadata:

1. **Status Row (24px)**:
   - `WIN`: Current hand payout (tap to toggle between coins and dollars/cents).
   - **Vector SVG Sound Speaker**: Centered between Win and Bet. Styled with the theme value color (`var(--t-stval)`). Tapping reveals an auto-hiding volume popup.
   - `BET`: Current wager (1 to 5 coins, tap to toggle coins vs dollars).
   - `CASH / CREDIT`: Player bankroll (tap to toggle between cash dollars `$41.25` and credits `165`).
2. **Action Button Strip (38px)**:
   - Ergonomically sized and ordered from left to right:
     * `HELP` (72px) - Opens game instructions and help overlay.
     * `MORE GAMES` (118px) - Opens game selector modal (all 11 families and 41 games).
     * `OPTIONS` (94px) - Opens theme selector, sound volume, and card back preferences.
     * `SEE PAYS` (94px) - Pops up full 5-column pay schedule in any mode.
     * `SPEED` (108px) - Cycles card deal animation speed (`▶`, `▶▶`, `▶▶▶`).
     * `BET ONE` (74px) - Increments bet by 1 coin (1 to 5).
     * `BET MAX` (74px) - Bets 5 coins immediately.
     * `25¢` (42px circular coin) - Opens coin denomination selector ($0.05 to $100).
     * `DRAW / DEAL` (146px tactile button) - Large, thumb-accessible green button with tactile beveling.
3. **Footer Information Bar (16px)**:
   - Left side: Game family, pay table variant, and theoretical return percentage (e.g., `Double Bonus (10/7) • 100.17%`).
   - Right side: Session hand counter and active denomination hand counter (e.g., `Hands: 12  •  Hands at 25¢: 12`).
   - Rendered in high-contrast theme-specific colors (`#c9a24d` in Smoke and Brass, `#3a1018` in Ruby Heirloom).

---

## 4. The 8 Authentic Visual Themes

The Android app will support all 8 authentic themes from the desktop player:

| Theme Key | Name | Felt / Background | Accents & Borders | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `brass` | **Smoke & Brass** (Default) | Deep charcoal & slate | Warm gold `#d4af37`, brass trim | Dave's preferred classic aesthetic |
| `classic` | **Classic Blue** | Traditional royal blue | Crisp white, silver borders | Standard casino video poker feel |
| `lounge` | **High Roller Lounge** | Midnight navy blue | Champagne gold, rich leather | Sophisticated lounge look |
| `noir` | **Noir Ace** | Pure obsidian black | Polished chrome, stark white | Ultra-high contrast, OLED-friendly |
| `penthouse` | **Penthouse Lounge** | Deep emerald green | Rosewood rail, brass accents | Authentic green felt table feel |
| `autumn` | **Autumn Filigree** | Warm mahogany & bronze | Copper accents, warm glow | Warm, earthy casino tone |
| `parchment`| **Parchment & Ember** | Muted antique parchment | Rich amber, dark charcoal | Vintage cardroom paper look |
| `ruby` | **Ruby Heirloom** | Deep royal burgundy | Antique gold, crimson borders | Luxury parlor aesthetic |

Every theme dynamically styles:
- Background felts and rail gradients.
- Panel background colors, borders, and inner inset shadows.
- Pay table text, header text, and win highlight colors.
- Beveled plaque gradients and borders.
- Button face colors, borders, and tactile shadows.
- Sound icon fill and stroke colors.
- Footer counter text colors.

---

## 5. Overlays and Modal Windows

All 5 overlay dialogs are modal, centered over the screen with a semi-transparent darkened backdrop:

1. **Help Modal**: Plain-language game instructions, hand hierarchy explanations, and button guides. Clean header padding with no overlapping icons.
2. **More Games Modal**:
   - Groups all **41 games** into their **11 authentic families**:
     * Jacks or Better (3 variants)
     * Bonus Poker (3 variants)
     * Double Bonus Poker (3 variants)
     * Double Double Bonus (5 variants)
     * Triple Double Bonus (2 variants)
     * Deuces Wild (8 variants)
     * Deuces Wild Bonus (3 variants)
     * Joker Poker (5 variants)
     * All American (3 variants)
     * White Hot Aces (3 variants)
     * Super Double Bonus (3 variants)
   - Displays return percentages (e.g., `100.17%`, `99.54%`) and variant specs.
3. **Options Modal**:
   - Theme picker with visual color swatches for all 8 themes.
   - Master volume slider and sound effects toggle.
   - Card deck back artwork selector.
4. **Denomination Modal**:
   - Authentic round coin tokens: **5¢, 10¢, 25¢, 50¢, $1, $5, $10, $25, $100**.
   - Active denomination highlighted with gold border and glow.
5. **See Pays Modal**: Full-screen 5-column pay schedule table accessible during any game mode.

---

## 6. Key Lessons Learned from Iterations

1. **Grid over Tables**: HTML `<table>` elements enforce inflexible cell padding and line-heights that break across mobile landscape viewports. Using CSS Grid with explicit `line-height: 12.2px` and `font-size: 11px` guarantees all 11 pay table rows fit without clipping.
2. **Plaque & Card Collision Prevention**: Cards shifting upward on hold (`translateY(-5px)`) combined with the `HELD` badge (`top: -9px`) requires a minimum 14px vertical gap below the hand plaque to ensure zero visual overlap.
3. **Theme-Aware Vector Assets**: Never use OS-level emojis (like `&#128266;`) for core UI controls because Android renders them as fixed-color yellow/blue emojis. Vector SVGs styled with theme CSS variables (`var(--t-stval)`) guarantee perfect color harmony across all 8 themes.
4. **Dynamic Text Contrast**: Fixed colors (e.g. blue `#8fa0d4`) fail against dark burgundy (Ruby Heirloom) or dark charcoal (Smoke & Brass). Footer metadata must be bound to theme-specific accent variables.
5. **Ergonomic Button Hierarchy**: The primary action button (`DRAW / DEAL`) requires maximum width (146px) and high-contrast green styling to be effortlessly reachable with the player's right thumb in landscape mode.

---

## 7. Kotlin & Jetpack Compose Architecture

The native Android app (`D:\Claude\VP_Android`) will follow modern Android architecture guidelines:

```
com.videopoker.android/
├── data/
│   ├── models/           # Card, Suit, Rank, HandEvaluation, PayTable, StrategyRule
│   ├── engine/           # DeckGenerator, HandEvaluator, StrategyEngine
│   ├── repository/       # GameRepository (loads JSONs from assets/games/)
│   └── ledger/           # SessionHistory, BankrollManager, LocalDatabase
├── ui/
│   ├── theme/            # 8 ColorSchemes, Typography, Dimensions, Gradients
│   ├── components/       # CardView, HeldBadge, PlaqueView, PayTableGrid, HoldPanel
│   ├── decks/
│   │   ├── TopDeck.kt    # PayTable + HoldChoices switcher
│   │   ├── MidDeck.kt    # Plaque + 5 Cards + Dots
│   │   └── BottomDeck.kt # StatusBar + ButtonStrip + FooterBar
│   ├── overlays/         # HelpDialog, MoreGamesDialog, OptionsDialog, DenomDialog
│   ├── MainScreen.kt     # Root landscape scaffold
│   └── MainViewModel.kt  # Unidirectional state flow (StateFlow<GameState>)
└── MainActivity.kt       # Landscape lock, edge-to-edge immersive setup
```

### 7.1. Jetpack Compose UI Breakdown

- **`MainScreen`**: Root composable constrained to landscape orientation.
  - Column with 3 child composables corresponding to the 3 decks:
    * `TopDeck(modifier = Modifier.weight(156f))`
    * `MidDeck(modifier = Modifier.weight(200f))`
    * `BottomDeck(modifier = Modifier.weight(104f))`
- **`TopDeck`**:
  - In Learn/Practice Mode: `Row` with `PayTableComposable` (width ~380dp) and `HoldPanelComposable` (weight 1f).
  - In Casino Mode: `PayTableComposable` expands to full width with 6 columns.
- **`MidDeck`**:
  - `HandPlaqueComposable`: Custom drawn metallic gradient border and shadow.
  - Spacer (14dp).
  - `Row` with 5 `CardSlotComposable` instances (96dp x 134dp).
    * Includes `HeldBadge` overlay when held.
    * Includes `LearnDots` in bottom-right corner during Learn Mode.
- **`BottomDeck`**:
  - `StatusBarComposable`: `Text` for Win, `IconButton` for Sound with `DropdownMenu`/`Popup` for volume, `Text` for Bet, and `Text` for Cash/Credits.
  - `ActionBarComposable`: `Row` with the 9 buttons, using custom tactile button styles.
  - `FooterBarComposable`: Theme-tinted text with variant details and hand counters.

---

## 8. Rollout Plan & Milestones

### Phase 1: Android Project Scaffolding
- Initialize Gradle project in `D:\Claude\VP_Android` with Jetpack Compose BOM and Kotlin 2.0.
- Configure `AndroidManifest.xml` with `screenOrientation="landscape"`, `configChanges="orientation|screenSize"`, and edge-to-edge system bars.
- Bundle all 52 card PNG assets into `res/drawable/`.
- Bundle all 41 game strategy JSON files into `assets/games/`.

### Phase 2: Core Game Engine & Strategy
- Port card deck shuffling and 52-card model.
- Port hand evaluation engine (Royal Flush, Straight Flush, 4 of a Kind, Full House, Flush, Straight, 3 of a Kind, 2 Pair, Jacks or Better).
- Port strategy hold-list evaluator to compute the top 3 optimal hold choices and EV scores.
- Build local ledger for bankroll and hand statistics.

### Phase 3: Compose UI & Theming
- Implement the 8 authentic ColorSchemes matching `play_base.css`.
- Build the 3-Deck layout (`TopDeck`, `MidDeck`, `BottomDeck`) using exact approved dimensions.
- Implement card dealing animations and tactile button press feedback.

### Phase 4: Overlays & Training Modes
- Build the 5 modal dialogs: More Games (41 games/11 families), Options (themes & volume), Help, Denom, and See Pays.
- Implement Learn, Practice, and Casino mode toggles with live strategy verification.

### Phase 5: Verification & Device Testing
- Build and run debug APK on Android Emulator (`Medium_Phone_API_37.0`).
- Test on physical Pixel 10 Pro / Pixel 10 Pro XL hardware.
- Verify zero network traffic in Airplane Mode.
