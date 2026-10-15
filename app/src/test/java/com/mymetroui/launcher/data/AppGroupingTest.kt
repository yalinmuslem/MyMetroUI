package com.mymetroui.launcher.data

import android.content.ComponentName
import android.os.Process
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

fun fakeApp(label: String, pkg: String = "p.${label.hashCode()}", userId: Int = 0): AppInfo {
    val c = ComponentName(pkg, "$pkg.Main")
    return AppInfo(makeKey(c, userId), label, userId, c, Process.myUserHandle())
}

@RunWith(RobolectricTestRunner::class)
class AppGroupingTest {
    @Test fun groupByLetter_handlesEmptyDigitsAndAccents() {
        val g = groupByLetter(listOf(fakeApp("Zed"), fakeApp(""), fakeApp("1Password"), fakeApp("émoji")))
        assertEquals(listOf('#', 'É', 'Z'), g.keys.toList())
        assertEquals(2, g['#']!!.size)
    }

    @Test fun makeKey_format() {
        assertEquals("a.b/a.b.Main#10", makeKey(ComponentName("a.b", "a.b.Main"), 10))
    }
}
