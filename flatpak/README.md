
# Building Flatpak Distributions

## Build Local

Following [the Flatpak documentation][flapakdocs]. First build:

    $ flatpak-builder --repo=repo-local build-local local-manifest.json --force-clean

Then make a bundle you can use to install with:

    $ flatpak build-bundle repo-local forkyz.flatpak com.gitlab.Hague.forkyz  --runtime-repo=https://flathub.org/repo/flathub.flatpakrepo

Then install:

    $ flatpak install --user forkyz.flatpak

## FlatHub: Preliminary Build

The app is too experimental for FlatHub release yet, but in preparation…

### Calculating the Sources Array

Calculating the "sources" for the "forkyz" module means finding out what Gradle needs to download. To do this:

1. get a run log from Gradle that downloads (and logs) everything needed,
2. extract the downloads from the log file to make the sources for the Flatpak manifest.

Add -i to the `local-manifest.json` Gradle build so you can see where downloads
come from. I.e.

    "./gradlew -i --no-daemon packageReleaseUberJarForCurrentOS",

Then do (where `build-test` is any junk directory to delete later):

    $ flatpak-builder build-local local-manifest.json --force-clean > build-log.txt

Wait till completion as each stage may download more files.

Then, for step 2, run

    $ python get-sources.py build-log.txt > sources.json

It will take some time to download and calculate sha512 sums for all files. Then copy the contents of sources.json into the "sources" array of the manifest.

Currently the following dependencies don't parse properly, so will need manual adjustment (search sources, fix-up):

* jdom2

Perhaps need to manually add

* skiko-awt-runtime-linux-arm64
* desktop-jvm-linux-arm64

If FlatHub tries to build an arm version.

## DBus Fix

The Flatpak manifests contain symlinking of the dbus library:

    {
      "build-commands": [
        "mkdir -p /app/lib",
        "ln -sf /usr/lib/<arch>/libdbus-1.so.3 /app/lib/libdbus-1.so"
      ],
      "buildsystem": "simple",
      "name": "dbus-compat"
    },

This is because isSystemInDarkTheme does not work on flatpak as the Linux
native code tries to load libdbus-1.so, which is not provided on the runtime.
Only libdbus-1.so.3 is. The code above creates a needed symlink so the native
theme detection will work.

[flatpakdocs]: https://docs.flatpak.org/en/latest/first-build.html]
