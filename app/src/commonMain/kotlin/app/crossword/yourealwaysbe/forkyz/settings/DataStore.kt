
package app.crossword.yourealwaysbe.forkyz.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage

import okio.FileSystem
import okio.Path

const val SETTINGS_FILE_NAME = "settings.pb"

fun createDataStore(
    fileSystem : FileSystem,
    producePath : () -> Path
): DataStore<Settings> = DataStoreFactory.create(
    storage = OkioStorage(
        fileSystem = fileSystem,
        producePath = producePath,
        serializer = SettingsSerializer,
    ),
)
