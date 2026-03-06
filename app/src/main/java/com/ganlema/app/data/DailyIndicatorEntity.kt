package com.ganlema.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_indicators",
    indices = [Index(value = ["dateKey", "metricKey"], unique = true)]
)
data class DailyIndicatorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val metricKey: String,
    val optionValue: String,
    val displayLabel: String,
    val updatedAt: Long
)
