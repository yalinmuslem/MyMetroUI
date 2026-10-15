package com.mymetroui.launcher.ui

import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.width
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.ui.home.HomeScreen
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PinUnpinFlowTest {
    @get:Rule val rule = createComposeRule()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val isTile = SemanticsMatcher("is a tile") {
        SemanticsProperties.TestTag in it.config && it.config[SemanticsProperties.TestTag].startsWith("tile-")
    }

    @Test fun pinFromAppListAppearsOnStart_thenResizeAndUnpin() {
        val tiles = FakeTileSource()
        val apps = FakeAppSource(listOf(fakeApp("Alpha", "p.alpha"), fakeApp("Beta", "p.beta")))
        val start = StartViewModel(tiles, apps)
        val appList = AppListViewModel(tiles, apps)
        rule.setContent {
            MyMetroUITheme { HomeScreen(start, appList, loadIcon = { ColorDrawable(0) }) }
        }

        rule.onAllNodes(isTile).assertCountEquals(0)

        rule.onNodeWithTag("to-app-list").performClick()
        rule.onNodeWithText("Alpha").performTouchInput { longClick() }
        rule.onNodeWithText("Pin to Start").performClick()

        // pinning returns to Start, where the new tile is shown
        rule.onAllNodes(isTile).assertCountEquals(1)
        val tile = rule.onAllNodes(isTile).onFirst()
        tile.assertIsDisplayed()
        val before = tile.getUnclippedBoundsInRoot().width.value

        tile.performTouchInput { longClick() }
        val id = tiles.flow.value.single().id
        rule.onNodeWithTag("resize-$id").performClick()
        val after = rule.onAllNodes(isTile).onFirst().getUnclippedBoundsInRoot().width.value
        assertTrue("tile should be wider: $before -> $after", after > before)

        rule.onNodeWithTag("unpin-$id").performClick()
        rule.onAllNodes(isTile).assertCountEquals(0)
    }
}
