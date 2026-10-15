package com.mymetroui.launcher.data

import androidx.test.core.app.ApplicationProvider
import com.mymetroui.launcher.data.db.AppDatabase
import com.mymetroui.launcher.data.db.TileEntity
import com.mymetroui.launcher.domain.TileLayout
import com.mymetroui.launcher.domain.TileSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TileRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: TileRepository

    @Before fun setUp() {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), inMemory = true)
        repo = TileRepository(db.tileDao())
    }

    @After fun tearDown() = db.close()

    @Test fun pin_thenEmits() = runBlocking {
        repo.pin("a/A#0")
        val tiles = repo.tiles.first()
        assertEquals(listOf("a/A#0"), tiles.map { it.key })
        assertTrue(TileLayout.isValid(tiles))
    }

    @Test fun pin_sameKeyTwice_keepsOne() = runBlocking {
        repo.pin("a/A#0"); repo.pin("a/A#0")
        assertEquals(1, repo.tiles.first().size)
    }

    @Test fun unpin_removes() = runBlocking {
        repo.pin("a/A#0"); repo.pin("b/B#0")
        val id = repo.tiles.first().first { it.key == "a/A#0" }.id
        repo.unpin(id)
        assertEquals(listOf("b/B#0"), repo.tiles.first().map { it.key })
    }

    @Test fun move_persistsPositions() = runBlocking {
        repo.pin("a/A#0")
        val id = repo.tiles.first().single().id
        repo.move(id, 2, 4)
        val t = repo.tiles.first().single()
        assertEquals(2 to 4, t.col to t.row)
    }

    @Test fun resize_persists() = runBlocking {
        repo.pin("a/A#0")
        val id = repo.tiles.first().single().id
        repo.resize(id, TileSize.WIDE)
        assertEquals(TileSize.WIDE, repo.tiles.first().single().size)
    }

    @Test fun removeKeys_andIsEmptyAndSeed() = runBlocking {
        assertTrue(repo.isEmpty())
        repo.seed(listOf("a/A#0", "b/B#0"))
        assertEquals(2, repo.tiles.first().size)
        repo.removeKeys(setOf("a/A#0"))
        assertEquals(listOf("b/B#0"), repo.tiles.first().map { it.key })
    }

    @Test fun corruptRowOnDisk_emitsValidLayout() = runBlocking {
        db.tileDao().replaceAll(listOf(TileEntity(1, "a/A#0", "WIDE", 9, -3), TileEntity(2, "b/B#0", "BOGUS", 0, 0)))
        assertTrue(TileLayout.isValid(repo.tiles.first()))
    }
}
