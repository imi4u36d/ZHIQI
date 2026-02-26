package com.ganlema.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val protections: String,
    val otherProtection: String?,
    val timeMillis: Long,
    val note: String?
)
