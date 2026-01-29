# Manual Test Plan for KeenClassik v1.5.0

**Date**: 2026-01-29
**Scope**: UI/UX improvements and MULTIPLICATION_ONLY mode integration
**Target**: Phase 1 & 2 validation

---

## Prerequisites

- Android device or emulator (API 24+)
- KeenClassik debug APK built with latest changes
- Screen sizes to test: 320dp (small), 480dp (medium), 720dp (large)

---

## Phase 1: UI/UX Validation

### 1.1 Grid Size and Space Utilization

**Test**: Verify grid uses 60-70% of screen height

Steps:
1. Launch app and start new 3x3 Easy game
2. Measure screen height and grid height visually
3. Verify grid is clearly visible and well-proportioned
4. Repeat for 5x5, 7x7, 9x9 grids

Expected:
- Grid occupies majority of screen
- No excessive empty space above or below grid
- Buttons are visible and accessible

Pass/Fail: ___

### 1.2 Button Sizing

**Test**: Buttons scale appropriately for different puzzle sizes

Steps:
1. Start 3x3 game - observe number pad buttons
2. Start 9x9 game - observe number pad buttons
3. Verify all buttons are tappable (WCAG 44dp minimum)
4. Verify buttons fit in 1-2 rows without wrapping

Expected:
- 3x3: Buttons ~52dp
- 9x9: Buttons ~48dp (slightly smaller)
- All buttons >= 44dp
- No horizontal scrolling required

Pass/Fail: ___

### 1.3 Clue Scaling

**Test**: Clues scale proportionally with cell size

Steps:
1. Start 3x3 Easy game - observe clue text sizes
2. Start 9x9 Extreme game - observe clue text sizes
3. Verify clues are readable in all cases
4. Check clue visibility in top-left corners

Expected:
- 3x3 clues: ~12sp (larger)
- 9x9 clues: ~9sp (smaller but readable)
- Clues never overlap cell values
- All clues visible and legible

Pass/Fail: ___

### 1.4 Notes Visibility

**Test**: Notes never overlap clues and have sufficient contrast

Steps:
1. Start 5x5 Normal game
2. Enter note mode (toggle notes button)
3. Add notes (1, 2, 3) to cells with clues in different positions:
   - Top-left clue
   - Top-right clue
   - Bottom-left clue
   - Center clue (if applicable)
4. Verify notes are positioned below clues
5. Check note contrast against background

Expected:
- Notes positioned dynamically below clues
- No overlap in any clue position
- Note background 95% opacity (clearly visible)
- 1dp border around note grid
- Notes readable without magnification

Pass/Fail: ___

### 1.5 Responsive Layout on Different Screens

**Test**: Layout adapts to various screen sizes

Steps:
1. Test on small screen (320dp width)
2. Test on medium screen (480dp width)
3. Test on large screen/tablet (720dp+ width)
4. For each: start 6x6 Normal game

Expected:
- All UI elements visible on all screens
- No truncation or overflow
- Proportional scaling
- Touch targets meet WCAG minimum (44dp)

Pass/Fail: ___

---

## Phase 2: Multiplication-Only Mode

### 2.1 Mode Selector Visibility

**Test**: GameModeSelector appears on menu screen

Steps:
1. Launch app to main menu
2. Observe mode selector UI
3. Verify 2 mode cards are visible

Expected:
- Mode selector visible above "Start Game" button
- Two cards: "Standard" and "Multiplication Only"
- Cards are tappable and show selection state
- Selected mode has visual indicator (highlight/border)

Pass/Fail: ___

### 2.2 Mode Selection

**Test**: Selecting MULTIPLICATION_ONLY mode works

Steps:
1. On menu screen, tap "Multiplication Only" card
2. Verify card shows selected state
3. Select grid size 5x5, difficulty Normal
4. Tap "Start Game"
5. Observe game screen

Expected:
- Mode card highlights when selected
- Game launches successfully
- No errors or crashes
- TopBar shows "Multiplication Only" badge

Pass/Fail: ___

### 2.3 Mode Badge Display

**Test**: Mode badge appears in TopBar

Steps:
1. Start game in STANDARD mode (5x5 Normal)
   - Verify no mode badge appears (STANDARD is default)
2. Return to menu and select MULTIPLICATION_ONLY mode
3. Start game (5x5 Normal)
   - Verify "Multiplication Only" badge appears in TopBar

Expected:
- STANDARD mode: No mode badge
- MULTIPLICATION_ONLY mode: Purple badge with "Multiplication Only" text
- Badge positioned after size/difficulty badge
- Badge readable and distinct

Pass/Fail: ___

### 2.4 Puzzle Generation

**Test**: MULTIPLICATION_ONLY generates valid puzzles

Steps:
1. Select MULTIPLICATION_ONLY mode
2. Generate puzzles for each size:
   - 3x3 Easy
   - 5x5 Normal
   - 7x7 Hard
   - 9x9 Extreme
3. For each puzzle, verify:
   - All clues use multiplication (×) symbol
   - No +, -, ÷ operations present
   - Puzzle is solvable
   - Solution satisfies Latin square property

Expected:
- All generations succeed (no errors)
- All cage clues end with "×" (e.g., "12×", "20×")
- Single-cell cages have number only (no operation)
- Puzzles are solvable by standard techniques

Pass/Fail: ___

### 2.5 Save and Load with Mode

**Test**: Mode persists across save/load

Steps:
1. Start MULTIPLICATION_ONLY game (6x6 Normal)
2. Make 3-5 moves
3. Open save dialog and save to Slot 1
4. Exit to main menu
5. Load game from Slot 1
6. Verify:
   - Puzzle state restored correctly
   - Mode badge still shows "Multiplication Only"
   - Can continue solving puzzle

Expected:
- Save succeeds
- Load restores exact puzzle state
- Mode persists correctly
- TopBar shows mode badge after load

Pass/Fail: ___

### 2.6 Auto-Save with Mode

**Test**: Mode persists with auto-save

Steps:
1. Start MULTIPLICATION_ONLY game (5x5 Hard)
2. Make several moves
3. Exit app (use home button or back)
4. Relaunch app
5. App should auto-restore game

Expected:
- Game auto-saves on exit
- On relaunch, game restores automatically
- Mode badge shows "Multiplication Only"
- All moves preserved

Pass/Fail: ___

### 2.7 Mode Switching Between Games

**Test**: Can switch modes between games

Steps:
1. Start STANDARD mode game (4x4 Easy)
2. Solve or exit to menu
3. Select MULTIPLICATION_ONLY mode
4. Start new game (4x4 Easy)
5. Return to menu
6. Select STANDARD mode again
7. Start new game (4x4 Easy)

Expected:
- Mode switches cleanly
- Each game uses correct mode
- No errors or cross-contamination
- Mode badge updates correctly

Pass/Fail: ___

### 2.8 Victory Animation with MULTIPLICATION_ONLY

**Test**: Solving MULT_ONLY puzzle triggers victory

Steps:
1. Start MULTIPLICATION_ONLY game (3x3 Easy for quick solve)
2. Solve puzzle completely
3. Observe victory animation

Expected:
- Victory animation plays
- Confetti/bounce effect displays
- Victory banner shows
- Mode is preserved if restarting

Pass/Fail: ___

---

## Phase 3: Accessibility

### 3.1 TalkBack Compatibility

**Test**: Screen reader announces UI correctly

Steps:
1. Enable TalkBack on device
2. Navigate menu screen
3. Select mode, size, difficulty
4. Start game
5. Navigate game grid with TalkBack

Expected:
- Mode selector cards announced
- Grid cells announce position and value
- Clues announced correctly
- Notes announced when present
- Buttons have clear labels

Pass/Fail: ___

### 3.2 D-Pad Navigation

**Test**: Can navigate with D-pad (TV mode)

Steps:
1. Connect hardware keyboard or enable D-pad simulation
2. Use arrow keys to navigate menu
3. Use Enter to select
4. In game, use D-pad to move between cells

Expected:
- D-pad moves focus visibly
- Can select all interactive elements
- Focus indicators clear and visible
- Can complete full game flow with D-pad only

Pass/Fail: ___

### 3.3 Contrast and Readability

**Test**: WCAG AAA compliance

Steps:
1. Start 9x9 Extreme game
2. Add notes to several cells
3. Measure contrast ratios (use accessibility scanner):
   - Note text vs background
   - Clue text vs background
   - Cell values vs background
4. Verify all ratios >= 6.5:1 (WCAG AAA)

Expected:
- Note background 95% opacity: ~6.5:1 contrast
- Clue text: >= 6.5:1 contrast
- All text readable without strain

Pass/Fail: ___

---

## Summary

**Total Tests**: 17
**Passed**: ___
**Failed**: ___
**Blocked**: ___

### Critical Issues

(List any critical bugs found)

### Minor Issues

(List any minor UX issues)

### Recommendations

(Suggestions for improvements)

---

## Sign-Off

**Tested By**: ___________________
**Date**: ___________________
**Build**: KeenClassik v1.5.0 (commit: ____________)
**Device**: ___________________
**Android Version**: ___________________

**Approved for Release**: Yes / No
**Comments**:

---

## Appendix: Measurement Tools

- **Grid Measurement**: Use developer options "Show layout bounds"
- **Contrast Measurement**: Use Accessibility Scanner app
- **Touch Target Measurement**: Use developer options "Show taps"
- **Screen Recording**: Recommended for documenting issues
