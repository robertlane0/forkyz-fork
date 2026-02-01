
package app.crossword.yourealwaysbe.forkyz.util.files

import java.time.LocalDate

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.util.toUri
import app.crossword.yourealwaysbe.puz.Puzzle

class MetaCache(private val utils : NativeBackendUtils) {
    object Converters {
        // need nullable not just non-null now
        // https://stackoverflow.com/a/77888612
        @TypeConverter
        fun fromEpochDay(value : Long?) : LocalDate? {
            return value?.let { LocalDate.ofEpochDay(it) }
        }

        @TypeConverter
        fun dateToEpochDay(date : LocalDate?) : Long? {
            return date?.let { it.toEpochDay() }
        }

        @TypeConverter
        fun fromUriString(value : String?) : Uri? {
            return value?.let { it.toUri() }
        }

        @TypeConverter
        fun uriToString(uri : Uri?) : String? {
            return uri?.let { it.toString() }
        }
    }

    @Entity(tableName = "cachedMeta")
    @TypeConverters(Converters::class)
    data class CachedMeta(
        @PrimaryKey
        var mainFileUri : Uri,

        // currently unused but may be used to speed up dir listing in
        // browse activity
        @ColumnInfo
        var metaFileUri : Uri?,

        @ColumnInfo
        var directoryUri : Uri,

        // this field is now defunct, remove on next schema change
        @ColumnInfo
        var isUpdatable : Boolean,

        @ColumnInfo
        var date : LocalDate?,

        @ColumnInfo
        var percentComplete : Int,

        @ColumnInfo
        var percentFilled : Int,

        @ColumnInfo
        var source : String?,

        @ColumnInfo
        var title : String?,

        // from db version 2
        @ColumnInfo
        var author : String?,

        // from db version 3
        @ColumnInfo(name = "rating")
        var ratingCode : Int,

        // from db version 4
        @ColumnInfo
        var hasSolution : Boolean
    ) {
        var rating : Char
            get() = ratingCode.toChar()
            set(value) { ratingCode = value.code }
    }

    @Dao
    @TypeConverters(Converters::class)
    interface CachedMetaDao {
        @Query("SELECT * FROM cachedMeta WHERE directoryUri = :directory")
        suspend fun getDirCache(directory : Uri) : List<CachedMeta>

        @Query("SELECT * FROM cachedMeta WHERE mainFileUri = :mainFileUri")
        suspend fun getCache(mainFileUri : Uri) : CachedMeta?

        @Query(
            "SELECT mainFileUri FROM cachedMeta "
            + "WHERE directoryUri = :directory"
        )
        suspend fun getDirFileUris(directory : Uri) : List<Uri>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertAll(vararg metas : CachedMeta)

        @Query("DELETE FROM cachedMeta WHERE mainFileUri = :mainFileUri")
        suspend fun delete(mainFileUri : Uri)

        @Delete
        suspend fun delete(cm : CachedMeta)

        /**
         * Delete files in dir not in puzMetaFiles
         */
        @Transaction
        suspend fun deleteOutside(
            directory : Uri,
            puzMetaFiles : Collection<PuzMetaFile>,
        ) {
            val keepUris = HashSet<Uri>(puzMetaFiles.size)
            for (pm in puzMetaFiles) {
                keepUris.add(pm.puzHandle.mainFileHandle.uri)
            }

            for (cm in getDirCache(directory)) {
                if (!keepUris.contains(cm.mainFileUri)) {
                    delete(cm)
                }
            }
        }
    }

    @Database(entities = [CachedMeta::class], version = 4)
    abstract class CachedMetaDB : RoomDatabase() {
        abstract fun cachedMetaDao() : CachedMetaDao

        companion object {
            private var instance : CachedMetaDB? = null

            /**
             * Version 2 adds an author column
             *
             * Thanks to
             * https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929
             */
            private val MIGRATION_l_2 = object : Migration(1, 2) {
                override fun migrate(connection : SQLiteConnection) {
                    connection.execSQL(
                        "ALTER TABLE cachedMeta ADD COLUMN author TEXT",
                    )
                }
            }

            private val MIGRATION_2_3 = object : Migration(2, 3) {
                override fun migrate(connection : SQLiteConnection) {
                    connection.execSQL(
                        "ALTER TABLE cachedMeta "
                        + "ADD COLUMN rating INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

            private val MIGRATION_3_4 = object : Migration(3, 4) {
                override fun migrate(connection : SQLiteConnection) {
                    connection.execSQL(
                        "ALTER TABLE cachedMeta "
                        + "ADD COLUMN hasSolution INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

            fun getInstance(utils : NativeBackendUtils) : CachedMetaDB {
                val inst = instance ?: utils.getCachedMetaDBBuilder()
                    .addMigrations(
                        MIGRATION_l_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                    ).build()
                instance = inst
                return inst
            }
        }
    }

    class MetaRecord(private val dbRow : CachedMeta) {
        val isUpdatable : Boolean
            // updatable field now defunct
            get() = dbRow.isUpdatable
        val caption : String?
            get() = dbRow.title
        val date : LocalDate?
            get() = dbRow.date
        val percentComplete : Int
            get() = dbRow.percentComplete
        val percentFilled : Int
            get() = dbRow.percentFilled
        val source : String?
            get() = dbRow.source
        val title : String?
            get() = dbRow.title
        val author : String?
            get() = dbRow.author
        val rating : Char
            get() = dbRow.rating

        val hasSolution : Boolean
            get() {
                // because old puzzles have false as the default infer
                // from percent complete
                return dbRow.hasSolution || dbRow.percentComplete > 0
            }
    }

    /**
     * Returns all cached meta data records for the given directory
     */
    suspend fun getDirCache(
        fileHandler : FileHandler,
        dirHandle : DirHandle,
    ) : Map<Uri, MetaRecord> {
        val dirUri = fileHandler.getUri(dirHandle)
        val cache = HashMap<Uri, MetaRecord>()
        for (cm in dao.getDirCache(dirUri)) {
            cache.put(cm.mainFileUri, MetaRecord(cm))
        }
        return cache
    }

    /**
     * Return cached meta for given handle
     *
     * @return null if no entry in cache
     */
    suspend fun getCache(puzFileUri : Uri) : MetaRecord? {
        return dao.getCache(puzFileUri)?.let { MetaRecord(it) }
    }

    /**
     * Cache meta for a file URI, returns new record
     *
     * Uses derived date if puz has no date.
     */
    suspend fun addRecord(
        fileHandler : FileHandler,
        puzHandle : PuzHandle,
        puz : Puzzle,
    ) : MetaRecord {
        val mainFileHandle = puzHandle.mainFileHandle
        val metaHandle = puzHandle.accept<FileHandle?>(
            object  : PuzHandle.Visitor<FileHandle?> {
                override fun visit(puzHandle : PuzHandle.Puz) : FileHandle? {
                    return puzHandle.metaFileHandle
                }

                override fun visit(ipuzHandle : PuzHandle.IPuz) : FileHandle? {
                    return null
                }
            }
        )
        val mainFileName = fileHandler.getName(mainFileHandle)
        var date = AppPuzzleUtils.deriveDate(puz.getDate(), mainFileName)
        if (date == null)
            date = fileHandler.getModifiedDate(mainFileHandle)

        val cm = CachedMeta(
            mainFileUri = fileHandler.getUri(mainFileHandle),
            metaFileUri = metaHandle?.let { fileHandler.getUri(it) },
            directoryUri = fileHandler.getUri(puzHandle.dirHandle),
            isUpdatable = true, // used to be a puzzle field
            date = date,
            percentComplete = puz.getPercentComplete(),
            percentFilled = puz.getPercentFilled(),
            source = puz.getSource(),
            title = puz.getTitle(),
            author = puz.getAuthor(),
            ratingCode = puz.getRating().code,
            hasSolution = puz.hasSolution()
        )

        dao.insertAll(cm)

        return MetaRecord(cm)
    }

    /**
     * Remove a record from the cache
     */
    suspend fun deleteRecord(fileHandler : FileHandler, puzHandle : PuzHandle) {
        dao.delete(fileHandler.getUri(puzHandle.mainFileHandle))
    }

    /**
     * Remove all records from directory that are not in the handles
     */
    suspend fun cleanupCache(
        fileHandler : FileHandler,
        dir : DirHandle,
        puzMetaFiles : List<PuzMetaFile>,
    ) {
        val dirUri = fileHandler.getUri(dir)
        dao.deleteOutside(dirUri, puzMetaFiles)
    }

    private val dao : CachedMetaDao
        get() = CachedMetaDB.Companion
            .getInstance(utils)
            .cachedMetaDao()
}

