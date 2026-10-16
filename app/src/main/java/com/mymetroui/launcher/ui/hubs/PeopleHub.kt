package com.mymetroui.launcher.ui.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.R
import com.mymetroui.launcher.data.hub.Contact
import com.mymetroui.launcher.data.hub.ContactsQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface PeopleState {
    object Loading : PeopleState
    object NoAccess : PeopleState
    data class Loaded(val contacts: List<Contact>) : PeopleState
}

private fun loadPeople(query: ContactsQuery): PeopleState =
    if (!query.hasPermission()) PeopleState.NoAccess
    else try {
        PeopleState.Loaded(query.contacts())
    } catch (_: SecurityException) { // permission revoked while running
        PeopleState.NoAccess
    }

/** Contacts are read off the main thread; [refresh] changes whenever the permission result arrives. */
@Composable
fun ColumnScope.PeopleHub(query: ContactsQuery, refresh: Int, onRequestAccess: () -> Unit, onOpen: (Contact) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val state = produceState<PeopleState>(PeopleState.Loading, query, refresh) {
        value = withContext(Dispatchers.IO) { loadPeople(query) }
    }.value
    when (state) {
        PeopleState.Loading -> {}
        PeopleState.NoAccess -> Text(
            stringResource(R.string.people_allow),
            Modifier.padding(top = 12.dp).background(scheme.primary).clickable(onClick = onRequestAccess)
                .padding(horizontal = 20.dp, vertical = 12.dp).testTag("people-allow"),
            color = scheme.onPrimary,
        )
        is PeopleState.Loaded ->
            if (state.contacts.isEmpty()) Text(stringResource(R.string.people_empty), Modifier.padding(top = 12.dp).testTag("people-empty"))
            else LazyColumn(Modifier.weight(1f)) {
                items(state.contacts, key = { it.id }) { c ->
                    Text(
                        c.name,
                        Modifier.fillMaxWidth().clickable { onOpen(c) }.padding(vertical = 12.dp).testTag("contact-${c.id}"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
    }
}
