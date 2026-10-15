package com.mymetroui.launcher.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tiles", indices = [Index(value = ["componentKey"], unique = true)])
data class TileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val componentKey: String,
    val size: String,
    val col: Int,
    val row: Int,
)
