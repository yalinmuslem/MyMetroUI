package com.mymetroui.launcher.data.hub

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class Contact(val id: Long, val lookupKey: String, val name: String)

interface ContactsQuery {
    fun hasPermission(): Boolean
    fun contacts(): List<Contact>
}

/** Drops nameless entries and sorts case-insensitively by name. */
fun tidyContacts(raw: List<Contact>): List<Contact> =
    raw.filter { it.name.isNotBlank() }.sortedBy { it.name.lowercase() }

class ContentResolverContactsQuery(private val context: Context) : ContactsQuery {
    override fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    override fun contacts(): List<Contact> {
        val out = mutableListOf<Contact>()
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null, null, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        )?.use { c ->
            while (c.moveToNext()) {
                val key = c.getString(1) ?: continue
                out += Contact(c.getLong(0), key, c.getString(2).orEmpty())
            }
        }
        return tidyContacts(out)
    }
}
