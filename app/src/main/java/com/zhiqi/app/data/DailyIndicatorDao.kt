package com.zhiqi.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyIndicatorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(indicator: DailyIndicatorEntity): Long

    @Query("SELECT * FROM daily_indicators WHERE dateKey = :dateKey ORDER BY updatedAt DESC")
    fun getByDateFlow(dateKey: String): Flow<List<DailyIndicatorEntity>>

    @Query("SELECT * FROM daily_indicators ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<DailyIndicatorEntity>>

    @Query("SELECT * FROM daily_indicators ORDER BY updatedAt DESC")
    suspend fun getAll(): List<DailyIndicatorEntity>

    @Query("DELETE FROM daily_indicators WHERE dateKey = :dateKey AND metricKey = :metricKey")
    suspend fun deleteByDateAndMetric(dateKey: String, metricKey: String): Int

    @Query("DELETE FROM daily_indicators")
    suspend fun clearAll()
}
