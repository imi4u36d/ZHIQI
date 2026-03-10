package com.zhiqi.app.data

import kotlinx.coroutines.flow.Flow

class DailyIndicatorRepository(private val dao: DailyIndicatorDao) {
    fun indicatorsByDate(dateKey: String): Flow<List<DailyIndicatorEntity>> = dao.getByDateFlow(dateKey)

    fun allIndicators(): Flow<List<DailyIndicatorEntity>> = dao.getAllFlow()

    suspend fun getAll(): List<DailyIndicatorEntity> = dao.getAll()

    suspend fun save(indicator: DailyIndicatorEntity): Long = dao.upsert(indicator)

    suspend fun deleteByDateAndMetric(dateKey: String, metricKey: String): Int =
        dao.deleteByDateAndMetric(dateKey, metricKey)

    suspend fun clearAll() = dao.clearAll()
}
