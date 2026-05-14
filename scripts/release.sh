#!/bin/fish

echo "Run from Forkyz root directory!"

if test (count $argv) -lt 1
    echo "Please pass the version name as the first argument. E.g. release.sh 40."
    exit
end

set forkyz_version $argv[1]

set changelog fastlane/metadata/android/en-US/changelogs/{$forkyz_version}00000.txt
set changelogfr fastlane/metadata/android/fr-FR/changelogs/{$forkyz_version}00000.txt

nvim $changelog
git add $changelog
uv run scripts/make_release_notes.py
git add $changelogfr
nvim gradle.properties

echo "Ready to commit!"
echo "Don't forget to git add the new changelog."
echo "Don't forget to start your runner for the release tag."

echo "WARNING: argostranslate does not work, translate change logs with translateLocally for now"

git status
