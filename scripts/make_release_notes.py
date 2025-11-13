#!/bin/python

# Convencience script for generating release.html and changes.md from fastlane
# changelogs.
#
# To be run from project root.

import requests

from dataclasses import dataclass
from pathlib import Path
import argostranslate.package
import argostranslate.translate

@dataclass
class Lang:
    res_suffix : str
    fastlane_folder : str
    changes_header : str
    version_header_fmt : str
    translate_lang : str

DEFAULT_LANG = Lang(
    "",
    "en-US",
    """
        <h1>Forkyz</h1><br>
        <p>
            This is an unofficial fork of the Shortyz
            crossword app. It implements a number of new features and
            removes some non-open libraries / trackers.
        </p><br>
    """,
    "Version {version}",
    "en",
)

LANGS = [
    DEFAULT_LANG,
    Lang(
        "-fr",
        "fr-FR",
        """
            <h1>Forkyz</h1>
            <p>
                C'est une fourche non officielle du Shortyz application
                de mots croisés. Il met en œuvre un certain nombre de
                nouvelles caractéristiques et enlever certaines
                bibliothèques/traceurs non ouverts.
            </p>
        """,
        "Version {version}",
        "fr",
    ),
]

release_file_fmt = "app/src/main/res/raw{lang}/release.html"
changelogs_folder_fmt = "fastlane/metadata/android/{folder}/changelogs"
changes_md = "docs/changes.md"

TRANSLATE_ACKS = "\nChangelog translated with Argos Translate."

def version_number(filepath : Path) -> int:
    end = -len("00000.txt")
    return int(filepath.name[:end])

def translate_file(
    source_file : Path,
    source_lang : Lang,
    target_file : Path,
    target_lang : Lang,
):
    with open(source_file) as f:
        source_text = f.read() + TRANSLATE_ACKS

    from_code = source_lang.translate_lang
    to_code = target_lang.translate_lang

    argostranslate.package.update_package_index()
    available_packages = argostranslate.package.get_available_packages()
    package_to_install = next(
        filter(
            lambda x: (
                x.from_code == from_code
                and x.to_code == to_code
            ),
            available_packages,
        )
    )
    argostranslate.package.install_from_path(package_to_install.download())

    translation = argostranslate.translate.translate(
        source_text,
        from_code,
        to_code,
    )

    with open(target_file, "w") as f:
        f.write(translation)

def translate_changelogs():
    for lang in LANGS:
        if lang.fastlane_folder == DEFAULT_LANG.fastlane_folder:
            continue

        lang_changelogs_folder \
            = Path(changelogs_folder_fmt.format(folder=lang.fastlane_folder))
        lang_changelogs_folder.mkdir(parents=True, exist_ok=True)
        default_changelogs = Path(
            changelogs_folder_fmt.format(folder=DEFAULT_LANG.fastlane_folder)
        ).glob("*")
        for changelog in default_changelogs:
            lang_changelog = lang_changelogs_folder / Path(changelog.name)
            if not lang_changelog.is_file():
                translate_file(
                    changelog,
                    DEFAULT_LANG,
                    lang_changelog,
                    lang,
                )

def compile_app_release_notes():
    for lang in LANGS:
        release_file = release_file_fmt.format(lang=lang.res_suffix)
        with open(release_file.format(release_file), "w") as release_file:
            release_file.write(lang.changes_header)

            changelogs = list(
                Path(changelogs_folder_fmt.format(folder=lang.fastlane_folder))
                    .glob("*")
            )
            changelogs.sort(key=lambda file: -version_number(file))

            for num, file in enumerate(changelogs):
                if num > 0:
                    release_file.write("<br>\n")
                release_file.write(
                    "<h2>"
                    + lang.version_header_fmt.format(
                        version=version_number(file),
                    ) + "</h2><br>\n"
                )
                release_file.write("\n")
                with open(file) as changelog:
                    for line in changelog:
                        if line.startswith("-"):
                            line = line[1:]
                            line = line.strip()
                            release_file.write(f"• {line}<br>\n")
                        else:
                            release_file.write(f"{line}<br>\n")

def compile_docs_releasenotes():
    with open(changes_md, "w") as f:
        f.write("# Forkyz Changelog\n\n")

        changelogs = list(
            Path(
                changelogs_folder_fmt.format(
                    folder=DEFAULT_LANG.fastlane_folder,
                ),
            ).glob("*")
        )
        changelogs.sort(key=lambda file: -version_number(file))

        for file in changelogs:
            f.write(f"## Version {version_number(file)}\n\n")
            with open(file) as changelog:
                for line in changelog:
                    f.write(line)
            f.write("\n")

translate_changelogs()
compile_app_release_notes()
compile_docs_releasenotes()
