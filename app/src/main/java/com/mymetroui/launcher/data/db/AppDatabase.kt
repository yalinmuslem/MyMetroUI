package com.mymetroui.launcher.data.db

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tileDao(): TileDao

    companion object {
        private const val NAME = "launcher.db"

        fun create(context: Context, inMemory: Boolean = false): AppDatabase {
            val app = context.applicationContext
            if (inMemory) return Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
            return try {
                open(app)
            } catch (_: SQLiteException) {
                recreate(app)
            } catch (_: IllegalStateException) { // Room's schema/identity check failed
                recreate(app)
            }
        }

        // Only tiles are stored, so losing them is cheap: StartViewModel reseeds the defaults.
        private fun recreate(app: Context): AppDatabase {
            app.deleteDatabase(NAME)
            return open(app)
        }

        // shortcut: opens on the calling thread (a few ms for this tiny DB), move off-main if the schema grows
        private fun open(app: Context): AppDatabase {
            val db = Room.databaseBuilder(app, AppDatabase::class.java, NAME).build()
            try {
                db.openHelper.writableDatabase
            } catch (e: Exception) {
                db.close()
                throw e
            }
            return db
        }
    }
}
