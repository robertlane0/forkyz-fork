---
name: addsetting
description: add a new setting to Forkyz with a settings page entry
---

Add a new setting to Forkyz. The argument describes the setting to add.

## Step 1: Read all files in parallel first

Before making any edits, read all of these files at once to understand the
current state:

- `composeApp/src/commonMain/proto/settings.proto` — find the last field
  number before `// defunct settings` (use the next number for the new field)
- `composeApp/src/commonMain/kotlin/app/crossword/yourealwaysbe/forkyz/settings/DataStore.kt`
  — find the alphabetical position for the new default value
- `composeApp/src/commonMain/kotlin/app/crossword/yourealwaysbe/forkyz/settings/ForkyzSettings.kt`
  — read the section with similar live/set entries to find the right position,
  and the `doExportImport` function area for the alphabetical position of the
  new run entry. Use Grep to find the relevant line ranges efficiently.
- `composeApp/src/androidHostTest/kotlin/app/crossword/yourealwaysbe/forkyz/settings/ForkyzSettingsTest.kt`
  — find `settingsValue1`, `settingsValue2`, and `settingsValue3`
- `composeApp/src/commonMain/composeResources/values/strings.xml` — find the
  alphabetical position for the new strings (search the `download_r` area or
  whatever prefix fits)
- `composeApp/src/commonMain/kotlin/app/crossword/yourealwaysbe/forkyz/SettingsPageViewModel.kt`
  — find the right page section and the import block for the string resources

## Step 2: Make all edits

### proto file

Add `bool <fieldName> = <nextNumber>;` before `// defunct settings`.

### DataStore.kt

Add `<fieldName> = <defaultValue>,` in alphabetical order in `DEFAULT_SETTINGS`.

### ForkyzSettings.kt — live/set methods

Add after a similar neighbouring entry (alphabetical by the live property name):

```kotlin
val live<PrefixedName> : Flow<<Type>>
    = settingsMap { it.<fieldName> }

fun set<PrefixedName>(value : <Type>) {
    settingsUpdate { settings ->
        settings.copy(<fieldName> = value)
    }
}
```

Do NOT add a `get` method — these are being phased out.

### ForkyzSettings.kt — doExportImport

Add a `run(...)` block in alphabetical order by the key string:

```kotlin
run(
    "<fieldName>",
    { s, key -> json.put(key, s.<fieldName>) },
    { key -> json.ifHasBoolean(key) { set<PrefixedName>(it) } },
)
```

Adjust `ifHasBoolean` / `ifHasInt` / `ifHasString` etc. to match the field type.

### ForkyzSettingsTest.kt

`settingsValue1` and `settingsValue3` use the default value; `settingsValue2`
uses a different value. `settingsValue3` is a `.copy()` of `settingsValue2` so
only add fields there that need to differ from `settingsValue2`.

Add `<fieldName> = <defaultValue>` in alphabetical order to `settingsValue1`.
Add `<fieldName> = <toggledValue>` in alphabetical order to `settingsValue2`.
Add `<fieldName> = <defaultValue>` to the `settingsValue3` copy call (to reset
it back to default from the toggled value in settingsValue2).

### strings.xml

Add in alphabetical order:

```xml
<string name="<snake_name>"><Title></string>
<string name="<snake_name>_desc"><Description></string>
```

### SettingsPageViewModel.kt

Add **two** things:

1. Imports (alphabetically in the import block):

```kotlin
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.<snake_name>
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.<snake_name>_desc
```

2. A `SettingsBoolean` (or `SettingsList` etc.) in the appropriate page
   section. If unsure of the position, ask the user before editing.

```kotlin
SettingsBoolean(
    SettingsPage.<PAGE>,
    Res.string.<snake_name>,
    Res.string.<snake_name>_desc,
    settings.live<PrefixedName>.stateInHere(),
    settings::set<PrefixedName>,
),
```
