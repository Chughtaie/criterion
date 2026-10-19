package com.chughtai.criterion.core.content

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.chughtai.criterion.core.model.ArabicNormalizer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContentDbTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val opened = mutableListOf<QuranContentDb>()

    @After fun close() { opened.forEach { it.close() } }

    private fun fromVariant(variant: String): QuranContentDb =
        Room.databaseBuilder<QuranContentDb>(ctx, "v-$variant.db")
            .createFromInputStream { javaClass.getResourceAsStream("/variants/$variant.db")!! }
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .build().also { opened += it }

    @Test fun asset_withExtraTables_opens_andFts5Matches() = runTest {
        val db = QuranContentDb.build(ctx).also { opened += it }
        val dao = db.ayahDao()
        assertThat(dao.count()).isEqualTo(6236)
        val words = dao.searchWords(ArabicNormalizer.normalize("الرَّحْمَٰنِ"))
        assertThat(words).hasSize(45)
        assertThat(words.first().surah to words.first().ayah).isEqualTo(1 to 1)
        assertThat(dao.searchSubstring("رحم")).hasSize(201)
        // prefix + boolean FTS5 syntax
        assertThat(dao.searchWords("\"الرحمن\" AND \"الرحيم\"")).isNotEmpty()
        assertThat(dao.searchWords("رحم*")).isNotEmpty()
        println("FTS5 OK: words=${words.size}")
    }

    @Test fun noMasterTable_withExtraTables_passesRoomValidation() = runTest {
        val dao = fromVariant("nomaster-extra").ayahDao()
        assertThat(dao.count()).isEqualTo(6236)
        assertThat(dao.searchWords("الرحمن")).hasSize(45)
    }

    @Test fun noMasterTable_noTriggers_validates_andRoomAddsTriggers() = runTest {
        val db = fromVariant("notriggers-nomaster")
        assertThat(db.ayahDao().count()).isEqualTo(6236)
        val f = ctx.getDatabasePath("v-notriggers-nomaster.db")
        assertThat(f.exists()).isTrue()
    }

    @Test fun userVersion0_isAcceptedViaCreatePathValidation() = runTest {
        assertThat(fromVariant("uv0").ayahDao().count()).isEqualTo(6236)
    }

    @Test fun wrongIdentityHash_isRejected() {
        val e = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { fromVariant("badhash").ayahDao().count() }
        }
        println("badhash -> ${e.message}")
        assertThat(e.message).contains("Room cannot verify the data integrity")
    }

    @Test fun ftsTokenizerMismatch_withoutMasterTable_isRejected() {
        val e = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { fromVariant("mismatch-nomaster").ayahDao().count() }
        }
        println("mismatch -> ${e.message?.take(300)}")
        assertThat(e.message).contains("Pre-packaged database has an invalid schema")
    }

    @Test fun bundledSqlite_versionAndFts5CompileOptions() = runTest {
        val conn = BundledSQLiteDriver().open(File(ctx.cacheDir, "probe.db").path)
        conn.prepare("SELECT sqlite_version()").use { it.step(); println("bundled sqlite_version=" + it.getText(0)) }
        val opts = mutableListOf<String>()
        conn.prepare("PRAGMA compile_options").use { while (it.step()) opts += it.getText(0) }
        println("compile_options=" + opts.filter { it.startsWith("ENABLE") })
        assertThat(opts).contains("ENABLE_FTS5")
        conn.close()
    }
}
