package com.mymetroui.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TileDao {
    @Query("SELECT * FROM tiles ORDER BY `row`, col, id")
    abstract fun observeAll(): Flow<List<TileEntity>>

    @Query("DELETE FROM tiles")
    protected abstract suspend fun deleteAll()

    @Insert
    protected abstract suspend fun insertAll(tiles: List<TileEntity>)

    @Transaction
    open suspend fun replaceAll(tiles: List<TileEntity>) {
        deleteAll()
        insertAll(tiles)
    }
}
