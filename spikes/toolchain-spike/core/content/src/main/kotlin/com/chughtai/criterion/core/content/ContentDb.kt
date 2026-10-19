package com.chughtai.criterion.core.content

import android.content.Context
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.FtsOptions
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

@Entity(tableName = "ayah")
data class AyahEntity(
    @PrimaryKey val id: Int,             // global ayah id 1..6236
    val surah: Int,
    val ayah: Int,
    @ColumnInfo(name = "text_uthmani") val textUthmani: String,
    @ColumnInfo(name = "text_norm") val textNorm: String,  // normalised search column
)

/** Whole-word FTS5 index over the normalised column (external content = ayah). */
@Fts5(contentEntity = AyahEntity::class, contentRowId = "id", tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "ayah_fts")
data class AyahFts(
    @ColumnInfo(name = "text_norm") val textNorm: String,
)

/** Substring FTS5 index (trigram) over the same column. */
@Fts5(contentEntity = AyahEntity::class, contentRowId = "id", tokenizer = FtsOptions.TOKENIZER_TRIGRAM)
@Entity(tableName = "ayah_fts_tri")
data class AyahFtsTrigram(
    @ColumnInfo(name = "text_norm") val textNorm: String,
)

@Dao
interface AyahDao {
    @Query("SELECT COUNT(*) FROM ayah")
    suspend fun count(): Int

    @Query(
        "SELECT ayah.* FROM ayah JOIN ayah_fts ON ayah.id = ayah_fts.rowid " +
            "WHERE ayah_fts MATCH :query ORDER BY ayah.id"
    )
    suspend fun searchWords(query: String): List<AyahEntity>

    @Query(
        "SELECT ayah.* FROM ayah JOIN ayah_fts_tri ON ayah.id = ayah_fts_tri.rowid " +
            "WHERE ayah_fts_tri MATCH :query ORDER BY ayah.id"
    )
    suspend fun searchSubstring(query: String): List<AyahEntity>
}

@Database(
    entities = [AyahEntity::class, AyahFts::class, AyahFtsTrigram::class],
    version = 1,
    exportSchema = true,
)
abstract class QuranContentDb : RoomDatabase() {
    abstract fun ayahDao(): AyahDao

    companion object {
        const val ASSET = "content/quran-content.db"
        fun build(context: Context, name: String = "quran-content.db"): QuranContentDb =
            Room.databaseBuilder<QuranContentDb>(context, name)
                .createFromAsset(ASSET)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .setDriver(BundledSQLiteDriver())
                .build()
    }
}
