package com.mymetroui.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mymetroui.launcher.data.hub.Contact
import com.mymetroui.launcher.data.hub.ContactsQuery
import com.mymetroui.launcher.data.hub.HubKind
import com.mymetroui.launcher.data.hub.tidyContacts
import com.mymetroui.launcher.ui.hubs.HubScreen
import com.mymetroui.launcher.ui.hubs.PeopleHub
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class FakeContactsQuery(
    val permission: Boolean = true,
    val list: List<Contact> = emptyList(),
    val throwOnRead: Boolean = false,
) : ContactsQuery {
    override fun hasPermission() = permission
    override fun contacts(): List<Contact> = if (throwOnRead) throw SecurityException() else list
}

class ContactsTest {
    @Test fun tidyDropsBlankAndSorts() {
        val raw = listOf(Contact(1, "a", "zed"), Contact(2, "b", " "), Contact(3, "c", "Amy"), Contact(4, "d", "bob"))
        assertEquals(listOf("Amy", "bob", "zed"), tidyContacts(raw).map { it.name })
    }
}

@RunWith(RobolectricTestRunner::class)
class PeopleHubTest {
    @get:Rule val rule = createComposeRule()

    private var asked = 0
    private val opened = mutableListOf<Long>()

    private fun show(query: ContactsQuery) = rule.setContent {
        MyMetroUITheme {
            HubScreen(HubKind.PEOPLE, {}, scrollable = false) { PeopleHub(query, 0, { asked++ }, { opened += it.id }) }
        }
    }

    // contacts load on Dispatchers.IO, which the compose clock does not wait for
    private fun await(tag: String) = rule.waitUntil(5000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    @Test fun noPermission_showsAllowPrompt() {
        show(FakeContactsQuery(permission = false))
        await("people-allow")
        rule.onNodeWithTag("people-allow").performClick()
        assertEquals(1, asked)
    }

    @Test fun permissionButEmpty_showsEmptyMessage() {
        show(FakeContactsQuery())
        await("people-empty")
        rule.onNodeWithTag("people-empty").assertIsDisplayed()
    }

    @Test fun showsContacts() {
        show(FakeContactsQuery(list = listOf(Contact(1, "a", "Amy"), Contact(2, "b", "Bob"))))
        await("contact-1")
        rule.onNodeWithTag("contact-2").assertIsDisplayed()
    }

    @Test fun tapContactCallsOnOpen() {
        show(FakeContactsQuery(list = listOf(Contact(7, "k", "Cy"))))
        await("contact-7")
        rule.onNodeWithTag("contact-7").performClick()
        assertEquals(listOf(7L), opened)
    }

    @Test fun revokedPermission_treatedAsNoPermission() {
        show(FakeContactsQuery(throwOnRead = true))
        await("people-allow")
        rule.onNodeWithTag("people-allow").assertIsDisplayed()
    }
}
