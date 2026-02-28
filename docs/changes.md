# Forkyz Changelog

## Version 79

- Add Dutch translation (thanks @ojppe).
- Fix La Croix downloads (was picking wrong puzzle for the date).
- Move 20 Minutes downloader to new Keesing source and Telegraaf to FortyTwoPuzzles.
- Support arrow words and cell borders in Keesing IO format.
- Add "advanced" puzzle source options pages.
- Replace "auto downloaders" option with "block auto download on day" (advanced puzzle source settings).
- Put page headings in settings search results.
- Add option to collapse headings on puzzle list page (browse settings).- Hide some unusable options on Desktop.
- Add decorations and undecorated setting on Desktop (desktop settings).

## Version 78

- Add option to split native input responses across cells.
- Fix file permission bug for external directories.
- Support non-alphabetic characters on hardware keyboard.
- Interpret + as space for inferred separators.
- Fix La Croix downloader and Everyman share import.
- Optimise state update (fixes lag on desktop).

## Version 77

- Add Dutch NRC and La Croix crossword sources.
- Support share import from Crossweird web pages.
- Support Claude as well as Chat GPT for LLM usage (needs re-configuring).
- Voice commands option to enhance interpretation with LLMs.
- Add Material green, blue, and orange themes.
- Show errors with a red box instead of varying background/font colour changes.
- Built-in keyboard: move special entry key to long-press on change direction.
- Hardware/built-in keyboard: act on key down rather than up.
- Move to new Material 3 action button menu.
- Support per-app language settings.
- Refinement of French translation (thanks @d-faure, @Clem0908, @Ash_Crow)
- More backend rewrites, bugs fixes, new bugs.
- Initial desktop version available: https://gitlab.com/Hague/forkyz/-/releases

## Version 76

- Add French translation (thanks Clem0908).
- Add special entry key to built-in keyboard.
- Add setting to turn of board snapping to screen edge (interaction settings).
- Fix Everyman scraper again.
- Fix minor UI bugs and reduce board snap amount.
- Backend refactoring, please report bugs.

## Version 75

- Fix unwanted announcements (must now be enabled explicitly in settings).

## Version 74

- New downloaders: TF1/Sud Ouest Mots Felches/Croises.
- Options on how clue lists/numbers are shown in clue display (display settings).
- Allow clue line to expand with clue size (display settings).
- Borrow Kotwords/xword-dl's clever deobfuscation for Everyman decoding.
- Rewrite board rendering code in Compose (please report bugs).
- Minor bug fixes and new bugs.
- Target Android 16.

## Version 73

- Use Compose Navigation for moving between pages (please report bugs).
- Fix crash on Android 10 and below.
- Bug fixes (anagrams, jump-to-clue, puzzle search, status bar color) and new bugs.

## Version 72

- Add El Pais Experto downloader.
- Add completion share and completion time in millis (thanks @develinthedetails).
- Add back arrow to submenus (thanks @develsinthedetails).
- Changes to backend settings management (please report bugs).

## Version 71

- Support shapes in TCA puz format (LA Times).
- Fix built-in keyboard keypress bug (thanks @MIC132MIC).
- Properly stop shake detection if enabled when app in background.

## Version 70

- Add AZERTY keyboard layout (thanks Patrik Herbeuval).
- Add TCA JSON parser and LA Times downloader (via Chicago Tribune).
- Add option for movement behaviour at end of clue in Clue List.
- Add option to suppress delete confirmation dialog.
- Fix Everyman import via share (again).
- Fix force caps for native keyboard.

## Version 69

- New downloader dialog.
- Toggle all puzzles in settings.
- Complete transition of final app widgets to Compose.
- Fix Everyman sharing.
- Minor bug fixes and new bugs.

## Version 68

- Add confirm delete dialog.
- Fix show errors status-bar indicator on older Android versions.
- Repair Metro downloads.
- Move clue list widget to Compose.

## Version 67

- Move Browse activity to Compose.
- Add confirmations and change defaults for swipes on Browse.
- Correct keyboard positioning/rendering with larger fonts.
- Repair Everyman scraper.
- Other minor tidies and new bugs.


## Version 66

- Fix delete key on built-in keyboard.
- Trial a more robust Everyman import from shared URL.

## Version 65

- Update Joseph, Metro, Premier, and Sheffer downloaders.
- Remove Everyman downloader since move from Guardian. Experimental support by sharing the Observer Everyman URL with Forkyz, likely to break soon.
- Mark Le Parisien downloader as it does not download the true current puzzles.
- Repair hourly background downloads and protect against simultaneous downloads.
- Move main play screen to Compose.

## Version 64

- Rewrite Notes page in Compose.
- Add log viewer in settings.
- Add puzzles export in settings.
- Fix Guardian downloads.
- Fix keyboard in Clues List.
- Fix puzzle time in Info Dialog.
- Fix crash during save when phone waking up.

## Version 63

- Fix background downloads.
- Fix clue list freeze when show errors activated.
- Improve puzzle save reliability.
- Fix initial value in special entry dialog.

## Version 62

- Add force caps option to special entry dialog.
- Fix Irish News Cryptic Downloader.
- Fix stop-on-end behaviour on clue list and notes page.
- Move settings to datastore (requires exported settings to be re-exported).
- Reimplement Clue List in Compose (hopefully invisible except menu positioning).

## Version 61

- Target Android 15.
- Add Metro Quick downloader.
- Rewrite of settings screen.
- Add option to force native input to upper case (keyboard settings).
- Add "locked" fit to screen (display settings).
- Long-press to move anagram solution back to source on Notes page (focus source first).
- Tweaks to separators display on board.
- Minor bug fixes and new bugs.

## Version 60

- Add PML as supported puzzle/share format and fix Metro download.
- Add Irish Times and Guardian Quiptic (thanks winkelnp) downloaders.
- Show selected clue in list by highlight or radio button (display settings).
- Display word separators on grid (display settings).
- Add fifteensquared.net (puzzle) and DuckDuckGo (clue) searches to external tools.


## Version 59

- New puzzles: share from mycrossword.co.uk, scrape Guardian Everyman and Quick.
- Add puzzle shape as default grid size in play screen (display settings).
- Add same grid size options for landscape as portrait (display settings).
- Add options for displaying two clue lists side by side (display settings).
- Add option to cycle unfilled only when moving forwards (interaction settings).
- Add option to change direction before move with arrow keys (interaction settings).
- Add settings import/export.
- Remove focus indicator from board on play screen when using hardware keys.

## Version 58

- Fix crash on show errors.

## Version 57

- Render puzzle in (on-screen-ish) tiles (for large grids and high zooms).
- Option to indicate in puzzle list if puzzle has a solution (browser settings).
- Consistency improvements in status bar show errors indication.
- Add full range of options to clue list and notes pages.

## Version 56

- Add puzzle ratings feature via puzzle info dialog.
- Indicate in status bar when show errors is active.
- Support most recent Hamburger Abendblatt puzzles.
- Add keyboard pause at end of undo stack.

## Version 55

- Change long-press behaviour on notes page (long-pressing a widget copies to the currently focussed widget).
- Fix UI overlap that occurred on some Android versions.
- Fix to-import crash on duplicate filenames.
- Also predict anagram chars when the clue is not on the board.

## Version 54

- Configure built-in keyboard repeat (keyboard settings).
- Wait until internet available before auto-download.
- Min Android version bumped to 5 (SDK21, library updates).
- Fix crash when zooming into maximum.

## Version 53

- Update ChatGPT model (thanks Adam Cole).
- Add "play letter undo stack" (interaction settings, thanks @jimmylord).
- Make previous clue respect "skip complete letters".
- Change "show errors clue" to only show wrong letters as hinted.
- Refine "don't delete crossing" options (interaction settings).
- Make theme a bit more "Material 3".

## Version 52

- Fix erratic next clue button.
- Fix crash on load on Android 7.
- Fix crash in anagram helper on Android 9 and prediction setting.
- Fix crash on external puzzle import.
- Read date and full title in Brains Only format.

## Version 51

- Add compact option for Forkyz keyboard (keyboard settings).
- Add grid/clue ratio option for Play screen (display settings).
- Add optional clue line below grid (display settings).
- Add NYT Syndicated downloader and Pzzl.com share-import.
- Target Android 14, update libs, min version KitKat now.

## Version 50

- Import puzzles by sharing a URL with Forkyz (works with links to puzzle files and webpages for Crossword Nexus, Exolve, Guardian, Hamburger Abendblatt, Metro (UK), Przekroj, De Standaard, De Telegraaf, Wall Street Journal (sometimes), and similar).
- Support new input formats (AM Universal JSON, Exolve, PA Puzzles HTML, Spoonbill, Wall Street Journal JSON, .xd, XWord).
- Downloader for UK Metro daily cryptic.
- Ability to flag cells.
- Add show percentage correct option in puzzle browser.
- Editing of unselectable clues.

## Version 49

- Add external tools menu and settings (billthefarmer's Crossword solver, dictionaries, ChatGPT).
- Predict anagram letters in notes (interaction settings).
- Fix enter changes direction, Przekroj download, and reading of legacy date metadata.
- Make parsing of some puzzle files (JSON) more lenient.
- Clear empty sections when moving/deleting puzzles.

## Version 48

- Skip letters that won't be deleted when backspacing
- When cycle unfilled words is enabled, cycle backwards as well as forwards
- Detect .puz files with "no solution" (i.e. either all answers X or all D)
- Brighten default flag colour in night mode
- Fix floating action button position on some devices

## Version 47

- Fix crash on start on some devices.

## Version 46

- Add filter to puzzle list
- Automatically import files from "to-import" folder
- Infer some metadata from filename if needed (supports default xword-dl and Forkyz conventions)
- Report ChatGPT error messages in response

## Version 45

- Fix broken autodownloads
- Add edit clue to notes page menu
- Fix scroll to clue behaviour

## Version 44

- Add cycle on unfilled words option in settings.
- Add multi-coloured flags (long press flag clue on notes or use menu).

## Version 43

- Large back-end rework to settings management and other parts (please report issues).
- Minor bug fixes, consistency improvements, and slight theme change.

## Version 42

- New Forkyz Scanner app submitted to FDroid (scans photos, images, PDF).
- Add show errors for completed clues option.
- Add edit clue hint feature from clue list view menu.
- Fix Hamburger Abendblatt download.
- Hide show errors and reveal options when puzzle has no stored solution.
- Brighten standard theme grey a little.
- Simplify help/release notes screens with better theme support.
- Use Material text inputs in places.
- Close after external import to avoid launcher weirdness.
- Open shared files and application/json.
- Native keyboard improvements should work on more devices.

## Version 41

- Support background shapes in IPuz spec (both IPuz and JPZ import).
- Jump to random clue via shake (interaction settings) or voice ("jump").
- Remove Notre Temps Geant Force 2 downloader (not a regular puzzle).

## Version 40

- Add "ask Chat GPT for help" (configure in interaction settings).
- Welcome users to new versions, with link to release notes.
- For cells not attached to clues, toggle between selecting the cell and all detached cells.
- Add Nathan Curtis and Luckystreak XWords to online sources page.

## Version 39

- Improved accessibility: clue/box announcements via TalkBack or text-to-speech.
- Support more puzzle styling (colours, dots/dashes, blocks).
- Support more IPuz style features (plus some relaxations like HTML colours).
- Support JPZ "clue" cells, move citations to completion message.
- Support dashed bars in RCI Jeux imports, plus more French puzzles (thanks slock83).
- Fix cleanup bug for completed puzzles.
- Display intro on first run, add completion data to puzzle info.
- Changelog in release notes in help.

## Version 38

- New De Telegraaf and De Standaard downloaders.
- New play screen for acrostic puzzles.
- Streamline board rendering (please report bugs).
- Wrap long board views in clue list and notes.
- New Keesing XML parser.
- Additional features BrainsOnly: circled boxes, rebus square, puzzle info.
- Additional features IPuz: clue labels, any crossword kind, acrostic, "smart" cell indexing (default 0-based now).
- Additional features JPZ: support Alex Boisvert's acrostic extensions.
- Group cells with no attached clue for easier entering.
- Remake app icon with Android 13 themed icon support.
- Fix scratch mode in main board view and other bugs.

## Version 37

- Add voice commands to puzzle activities.
- Downloaders setup now aware of expected availability time of puzzle sources.
- Target Android 13.
- Minor UI tweaks.

## Version 36

- Fix King Digital downloaders.

## Version 35

- Warn if no connection when downloading.
- Add legacy-like theme option.
- Allow auto/background downloaders to be a sub-selection of enabled downloaders.

## Version 34

- Support USA Today Sunday downloads (thx th0mcat).

## Version 33

- "Pinned" solution word in Hamburger Abendblatt puzzles.
- Move "show all words" clue list option to clue list menu.

## Version 32

- Switch to Material 3 Theme.
- Enable dynamic (Material You) colours in settings.
- Revamp settings screen into sub-screens.

## Version 31

Remove "failed" notification when downloading an existing puzzle.

## Version 30

Optional PuzzleMe mobile-style clues list showing all words

## Version 29

- Daily downloads from WSJ
- Washington Post Sunday download (from Martin Herbach)

## Version 28

- Fix crash in landscape mode.

## Version 27

- Slightly finer-grained control of notifications in settings.
- Notify about failed downloads.

## Version 26

- Add configurable timeout to puzzle download.
- Parallelise downloads.
- Support rebus cells in AcrossLite (.puz) files.
- Support puzzle intro messages (in "info") and completion messages (on complete dialog).

## Version 25

Bugfix release.

## Version 24

- New daily download: Hamburger Abendblatt (German).
- New daily download: Le Parisien (French).
- New scraper: Przekroj Magazine (Polish), with pictures!
- Attempt to handle native keyboard properly (can now input diacritics and emojis).
- Allow custom download source in any supported format, not just .puz.
- New supported import formats: Raetselzentrale Schwedenratsel JSON, RCI Jeux Mots Fleches JSON, Guardian HTML, Przekroj HTML and JSON.
- Refresh online puzzle sources list.
- Correct charset for Across Lite (.puz) version 2.0 and up.
- Add share URL. Visible when sharing clues, and can be opened from share menu.

## Version 23

- Add share clue/board feature.
- Support notes on all clues, cells with numbers in different positions, and entering multiple characters into a single cell.
- Enable automatic clue text size scaling for older devices, remove clue text size menu option.

## Version 22

- Experimental sources are now scrapers, visible in download dialog.
- Add Private Eye as downloadable source.
- Support split clues in Guardian puzzles.
- Add insert special characters feature.
- Refresh online sources list.
- Remove Washington Post source: AmuseLabs now obfuscate their data, i assume to prevent third-party downloads.

## Version 21

- Allow customisation of automatic downloads, and support older devices.
- Bring back the native keyboard (your mileage may vary).
- Restore wrap to next list behaviour for next clue movement strategy.

## Version 20

- Move to puzzle "zones" instead of only across/down clues, and allow non-standard clue numbers. Supports e.g. spirals and rows gardens in IPuz and JPZ.
- Haptic feedback option on keyboard.

## Version 19

- Support coloured cells in JPZ and IPuz
- Support bars in JPZ as well as IPuz
- Support HTML in clue/title/etc text
- Player notes for whole puzzle as well as just clue
- Limited support for additional clue lists (not just across/down)
- Allow cells that would typically be numbered to not be numbered


## Version 18

- Remove LA Times and Washington Post Classic downloads.
- Allow import of multiple files simultaneously.

## Version 17

- Fix crashes when first box is blank, and when crossword directories have been deleted.

## Version 16

- Initial support for bar puzzles in IPuz format (tested on Square Chase puzzles only).
- Add "flag clue" feature.
- Add hide button option for keyboard (good for gesture navigation).
- Display word count in clue list when option set.
- Recode parallel movement strategy.

## Version 15

- Repair download for Android 12
- Notes page: long press board while anagram source selected now copies board to anagram solution.
- Corrected snap-to-clue behaviour
- Fixed oddities in clue tabs


## Version 14

- Bug fixes and library updates.

## Version 13

- Support for QWERTZ, Colemak and Dvorak keyboard layouts.
- Custom daily .puz source added to downloads.

## Version 12

- Fix Newsday downloads.

## Version 11

- Add change clue and direction buttons to keyboard.
- Show puzzle author on browse screen.

## Version 10

- Updated list of online puzzle sources.
- Add show errors under cursor option.
- Amuse Labs and King Digital downloads supported: circled boxes now work for LA Times puzzles, bring back Washington Post, LAT Sundays, Joseph, Sheffer, and Premier crossword downloaders.
- Circled boxes re-enabled in JPZ puzzles.
- Improved error highlighting in night colors.

## Version 9

- Bug fix: set source and support URL properly for Guardian downloads.

## Version 8

- Import of IPuz files
- Use IPuz as backend file format instead of Across Lite
- Remove HTTP/download system actions in favour of "open with"
- Prevent multiple simultaneous browse activities
- Avoid reloading puzzle list in browse activity on delete/move, and losing scroll position
- Friendlier import file names

## Version 7

- Import files from storage.
- Updated help pages, including notes help.
- List of external sources where puzzles can be downloaded.
- New default keyboard mode only hides with back button.
- Adjustment of zoom/snap behaviour on play board.

## Version 6

- Cache meta data to speed up browsing (after first view).
- Clean up zoom/scroll behaviour.
- Add menu option link to support puzzle sources.
- Improve arrow key navigation (inc. bug fixes).
- Fix crash in browsing when last puzzle was deleted.

## Version 5

- Compatible with Android 11.
- Updated downloadable puzzles.
- Theme and UI adjustments.
- Notes/anagram solving screen.
- Remove Google integration.
- Remove Crashlytics.

