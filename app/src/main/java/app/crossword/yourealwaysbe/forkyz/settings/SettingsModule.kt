
package app.crossword.yourealwaysbe.forkyz.settings

import javax.inject.Singleton

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

private const val SETTINGS_FILE_NAME = "settings.pb"

val Context.dataStore: DataStore<Settings> by dataStore(
    fileName = SETTINGS_FILE_NAME,
    serializer = SettingsSerializer,
)

@Module
@InstallIn(SingletonComponent::class)
public class SettingsModule {
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context : Context,
    ) : DataStore<Settings> = context.dataStore

    @Provides
    @Singleton
    fun provideSettingsStore(
        settingsStore : DataStore<Settings>,
    ) : ForkyzSettings = ForkyzSettings(settingsStore)
}
