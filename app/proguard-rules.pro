# Room generates AppDatabase_Impl / TileDao_Impl and loads them by name; the Room AAR ships consumer rules,
# this is a safety net in case a future Room version relies on it.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
