# Forkyz Crosswords

Forkyz is an unofficial fork of [Shortyz](https://github.com/kebernet/shortyz/)
with wider-ranging support for a variety of puzzle types (e.g. grids that don't
follow a standard across/down set-up) and other customisations.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/app.crossword.yourealwaysbe.forkyz/)

[Release APKs][releases] can be installed directly.

[Forkyz Scanner][forkyz-scanner] is a companion app for scanning crosswords from the camera, images, or PDFs.

## Features

Play crosswords! Full board view, clue list view, make notes about clues, anagram helper.

Support for block, bar, and acrostic puzzles. Puzzles don't have to be across/down. E.g. rows gardens and marching bands are supported in JPZ and IPuz.

[Changelogs are available here][changelogs].

## Screenshots

<img
    alt="The puzzle browse menu"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/06-BrowseMenu.png"
    width=150
/>
<img
    alt="The standard grid view"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/01-StandardGrid.png"
    width=150
/>
<img
    alt="The standard grid view in dark mode"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/02-StandardGridDark.png"
    width=150
/>
<img
    alt="The clue list view"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/07-ClueList.png"
    width=150
/>
<img
    alt="The clue list view with the words shown with the clue"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/08-ClueListGrid.png"
    width=150
/>
<img
    alt="The clue/puzzle notes page with anagram helper"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/09-NotesPage.png"
    width=150
/>
<img
    alt="A rows garden puzzle in the app"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/03-RowsGarden.png"
    width=150
/>
<img
    alt="A bar puzzle in the app with coloured grid squares"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/04-BarPuzzle.png"
    width=150
/>
<img
    alt="A twists and turns puzzle in the app"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/05-TwistsAndTurns.png"
    width=150
/>
<img
    alt="The puzzle themed with dynamic Material You colours"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/10-DynamicTheme.png"
    width=150
/>
<img
    alt="The play screen for an acrostic puzzle"
    src="fastlane/metadata/android/en-US/images/phoneScreenshots/11-Acrostic.png"
    width=150
/>

## Obtaining Puzzles

Puzzles can be obtained in several ways:

* import files,
* scanning with the [Forkyz Scanner][forkyz-scanner] companion app,
* share a URL from a web browser, or
* use the built-in downloaders.

### Importing Files

Importing files can be done either by using the "+" button in the puzzle list page, or by dropping files into the "to-import" directory.

Files for import can be downloaded from a [number of websites][online-sources], or anywhere else you find them. I generally prefer this method over the downloaders since you get involved with the community.

Supported import file formats (can be zipped or unzipped):

* Across Lite (.puz),
* AM Universal JSON,
* AmuseLabs JSON,
* BrainsOnly TXT,
* Exolve,
* Guardian JSON and HTML,
* IPuz (crosswords and acrostics only),
* JPZ,
* Keesing XML,
* King Features TXT,
* PAPuzzles HTML,
* Przekroj Magazine JSON and HTML,
* RCI Jeux Mots-Fleches HJSON,
* Raetsel Zentrale Schwedenratsel JSON,
* Spoonbill,
* Uclick XML,
* Wall Street Journal JSON,
* .xd,
* XWord JSON.

Current downloaders are De Standaard, De Telegraaf, Guardian Daily Cryptic, Hamburger Abendblatt, Independent Daily Cryptic, Thomas Joseph Crossword, Le Parisien, Metro (UK), Newsday, Sheffer Crossword, Universal Crosssword, USA Today, Jonesin' Crosswords, Premier Crossword, the Wall Street Journal (Sat/Sun), Keglar's Cryptics, Cryptic Cru Archives, Private Eye, and Przekroj Magazine.

### Sharing From a Browser

From an online puzzle page, you can use your browser's "share" function to share the URL with Forkyz. Forkyz will attempt to retrieve the crossword from the website.

This only works for a limited number of sites, but may work for sites using similar crossword software. It has been tested with:

* Crossword Nexus,
* Exolve,
* Guardian,
* Hamburger Abendblatt (embedded from Raetsel Zentrale),
* De Standaard (embedded crosswords from Keesing),
* Metro (embedded from PA Puzzles),
* MyCrossword.co.uk,
* De Telegraaf (embedded from Pzzl.net),
* Przekroj,
* Wall Street Journal.

## Compilation

Gradle should compile fine as long as you have Java 17 or above.

    $ ./gradlew assembleDebug

Or to build and install, with device connected

    $ ./gradlew installDebug

## Project Structure

  * ./app The Android App.
  * ./puzlib A platform independent Java library for dealing with puzzle formats.
  * ./gfx Misc art assets.

## Thanks

Jonathan Blandford of [GNOME Crosswords][gnome-crosswords] for discussion on acrostics and the IPuz file on which the acrostic screenshot is based.

## License

Copyright (C) 2010-2016 Robert Cooper (and 2018- Forkyz Contributors)

Licensed under the GNU General Public License, Version 3

ipuz is a trademark of Puzzazz, used with permission.
The IPuz spec is reproduced [here][ipuz-spec].

[changelogs]: https://hague.gitlab.io/forkyz/changes.html
[forkyz-scanner]: https://gitlab.com/Hague/forkyzscanner
[gnome-crosswords]: https://gitlab.gnome.org/jrb/crosswords
[online-sources]: https://hague.gitlab.io/forkyz/
[ipuz-spec]: https://hague.gitlab.io/forkyz/ipuz.html
[releases]: https://gitlab.com/Hague/forkyz/-/releases
