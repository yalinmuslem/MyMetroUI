package com.mymetroui.launcher.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.mymetroui.launcher.data.hub.HubKind
import com.mymetroui.launcher.ui.hubs.HubScreen
import com.mymetroui.launcher.ui.hubs.PhotosHub
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotosHubTest {
    @get:Rule val rule = createComposeRule()

    private val opened = mutableListOf<String>()
    private var added = 0

    private fun show(uris: List<String>, loader: suspend (String) -> ImageBitmap? = { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap() }) = rule.setContent {
        MyMetroUITheme {
            HubScreen(HubKind.PHOTOS, {}, scrollable = false) { PhotosHub(uris, loader, { opened += it }, { added++ }) }
        }
    }

    private fun uris(n: Int) = (0 until n).map { "content://p/$it" }

    @Test fun emptyShowsMessageAndAdd() {
        show(emptyList())
        rule.onNodeWithTag("photos-empty").assertIsDisplayed()
        rule.onNodeWithTag("photos-add").assertIsDisplayed()
    }

    @Test fun addCallsCallback() {
        show(emptyList())
        rule.onNodeWithTag("photos-add").performClick()
        assertEquals(1, added)
    }

    @Test fun showsOneCellPerUri() {
        show(uris(3))
        (0..2).forEach { rule.onNodeWithTag("photo-$it").assertIsDisplayed() }
    }

    @Test fun capsAtSixty() {
        show(uris(70))
        rule.onNodeWithTag("photos-grid").performScrollToIndex(59)
        rule.onNodeWithTag("photo-59").assertExists()
        rule.onNodeWithTag("photo-60").assertDoesNotExist()
    }

    @Test fun tapCellOpensUri() {
        show(uris(2))
        rule.onNodeWithTag("photo-1").performClick()
        assertEquals(listOf("content://p/1"), opened)
    }

    @Test fun failedThumbnail_doesNotBreakOthers() {
        show(uris(3)) { if (it.endsWith("/1")) null else Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap() }
        (0..2).forEach { rule.onNodeWithTag("photo-$it").assertExists() }
    }
}
