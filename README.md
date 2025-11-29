# Forkyz Crosswords

Forkyz is an unofficial fork of [Shortyz](https://github.com/kebernet/shortyz/)
with wider-ranging support for a variety of puzzle types (e.g. grids that don't
follow a standard across/down set-up) and other customisations.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/app.crossword.yourealwaysbe.forkyz/)

[Releases][releases] can be installed directly on

* Android
* Linux (experimental)
* Flatpak (experimental)
* Windows (experimental)

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
* PML JSON and HTML,
* Przekrój Magazine JSON and HTML,
* RCI Jeux Mots-Fléchés and Croisés HJSON,
* Raetsel Zentrale Schwedenratsel JSON,
* Slow Down Wise Up JSON,
* Smart Games JSON,
* Spoonbill,
* Uclick XML,
* .xd,
* XWord JSON.

Current downloaders are De Standaard, De Telegraaf, El Pais Experto, Guardian Daily Cryptic/Quick, Hamburger Abendblatt, Independent Daily Cryptic, Irish News Cryptic, Thomas Joseph Crossword, Le Parisien, Metro (UK), Newsday, New York Times (Syndicated), Sheffer Crossword, Sud Ouest Mot-Fléchés/Croisés, TF1 Mot-Fléchés/Croisés, Universal Crosssword, USA Today, Jonesin' Crosswords, Premier Crossword, the Wall Street Journal (Sat/Sun), Keglar's Cryptics, Cryptic Cru Archives, Private Eye, and Przekrój Magazine.

### Sharing From a Browser

From an online puzzle page, you can use your browser's "share" function to share the URL with Forkyz. Forkyz will attempt to retrieve the crossword from the website.

This only works for a limited number of sites, but may work for sites using similar crossword software. It has been tested with:

* Crossword Nexus,
* Exolve,
* Guardian,
* Hamburger Abendblatt (embedded from Raetsel Zentrale),
* Irish News,
* Metro (embedded from PA Puzzles),
* MyCrossword.co.uk,
* Observer (Everyman at least),
* De Telegraaf (embedded from Pzzl.net),
* Przekrój,
* Wall Street Journal.

## Compilation

Gradle should compile fine as long as you have Java 17 or above.

For Android:

    $ ./gradlew assembleDebug

Or to build and install, with device connected

    $ ./gradlew installDebug

For desktop:

    $ ./gradlew run

See `./gradlew tasks` for all build commands.

## Project Structure

  * ./app The Android App.
  * ./puzlib A platform independent Java library for dealing with puzzle formats.
  * ./gfx Misc art assets.

## Translations

Translations are welcome. Here is a quick guide to what is needed.

* Most strings are in
  `composeApp/src/commonMain/composeResources/values/strings.xml`
  and need a copy in
  `composeApp/src/commonMain/composeResources/values-<lang>/strings.xml`.
* Help pages &c. are in
  `composeApp/src/commonMain/composeResources/files/en/<screen>.html`
  and need a copy in
  `composeApp/src/commonMain/composeResources/files/<lang>/<screen>.html`.
* Android strings are in
  `androidApp/src/main/res/values/strings.xml`
  and need a copy in
  `androidApp/src/main/res/values-<lang>/strings.xml`.
* The following resources in the `files/en` directory should not be translated
  (do not create a new copy, the one in `files/en` will be used instead).
    * `license.html` -- unless you have an officially translated version.
    * `release.html` -- generated from changelogs (see Fastlane translations below).
* The following files in
  `fastlane/metadata/android/en-US/`
  need a copy in
  `fastlane/metadata/android/<lang>`:
  `full_description.txt`, `short_description.txt`, `title.txt`.
  The files in the `changelogs` directory are optional. By default i will
  create auto-translations with [Argos Translate][argostranslate]. This avoids
  a barrier to releases. I will accept corrections though.
* The desktop metainfo in `flatpak/com.gitlab.Hague.forkyz.metainfo.xml` needs
  additional elements for supported languages.
* Finally, create a translated copy of
  `puzlib/src/main/resources/app/crossword/yourealwaysbe/puz/strings.properties`
  with the name
  `puzlib/src/main/resources/app/crossword/yourealwaysbe/puz/strings_<lang>.properties`.

The last bullet is a file of clue list names. These have a strange status. Some input formats include the list name as part of the file (which will be used instead of any translations). Some don't encode the list name, but are kind of tied to a particular country. Hardcoded strings are used for these. The properties file will be used for formats that don't specify the list name, but aren't tied to a country. Note, the list names are decided on import, so the translation won't apply to previously downloaded/imported puzzles.

Strings without translations are given [on this page][missingtranslations].

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
[argostranslate]: https://github.com/argosopentech/argos-translate
[missingtranslations]: https://hague.gitlab.io/forkyz/missing_translations.html

