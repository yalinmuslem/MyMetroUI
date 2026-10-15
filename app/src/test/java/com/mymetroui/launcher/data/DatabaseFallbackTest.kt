package com.mymetroui.launcher.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.mymetroui.launcher.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseFallbackTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file get() = context.getDatabasePath("launcher.db").also { it.parentFile!!.mkdirs() }

    private fun assertRecovers() = runBlocking {
        val db = AppDatabase.create(context)
        val repo = TileRepository(db.tileDao())
        assertEquals(emptyList<Any>(), repo.tiles.first())
        repo.pin("a/A#0") // and it is writable
        assertEquals(1, repo.tiles.first().size)
        db.close()
    }

    @Test fun corruptDbFile_recreatesEmptyDatabase() {
        file.writeBytes(ByteArray(4096) { 0x7F })
        assertRecovers()
    }

    @Test fun foreignSchemaAtSameVersion_recreatesEmptyDatabase() {
        SQLiteDatabase.openOrCreateDatabase(file, null).use {
            it.execSQL("CREATE TABLE tiles (x TEXT)")
            it.version = 1
        }
        assertRecovers()
    }
}
