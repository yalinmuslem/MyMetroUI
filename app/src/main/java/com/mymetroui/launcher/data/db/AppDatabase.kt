package com.mymetroui.launcher.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tileDao(): TileDao

    companion object {
        fun create(context: Context, inMemory: Boolean = false): AppDatabase {
            val app = context.applicationContext
            return if (inMemory) Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
            else Room.databaseBuilder(app, AppDatabase::class.java, "launcher.db").build()
        }
    }
}
