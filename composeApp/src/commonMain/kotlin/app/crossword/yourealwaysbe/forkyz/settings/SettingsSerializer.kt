
package app.crossword.yourealwaysbe.forkyz.settings

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import androidx.datastore.core.okio.OkioSerializer
import okio.BufferedSink
import okio.BufferedSource

class SettingsSerializer(
    val defaultSettings : Settings,
) : OkioSerializer<Settings> {
    override val defaultValue : Settings = defaultSettings

    override suspend fun readFrom(source : BufferedSource) : Settings {
        try {
            return Settings.ADAPTER.decode(source)
        } catch (exception: IOException) {
            throw Exception("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t : Settings, sink : BufferedSink) {
        sink.write(t.encode())
    }
}

