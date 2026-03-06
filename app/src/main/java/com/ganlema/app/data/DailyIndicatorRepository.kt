package com.ganlema.app.data

import kotlinx.coroutines.flow.Flow

class DailyIndicatorRepository(private val dao: DailyIndicatorDao) {
    fun indicatorsByDate(dateKey: String): Flow<List<DailyIndicatorEntity>> = dao.getByDateFlow(dateKey)

    fun allIndicators(): Flow<List<DailyIndicatorEntity>> = dao.getAllFlow()

    suspend fun save(indicator: DailyIndicatorEntity): Long = dao.upsert(indicator)

    suspend fun clearAll() = dao.clearAll()
}
