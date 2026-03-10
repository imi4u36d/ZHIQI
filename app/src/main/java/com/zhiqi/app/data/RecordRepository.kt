package com.zhiqi.app.data

import kotlinx.coroutines.flow.Flow

class RecordRepository(private val dao: RecordDao) {
    fun records(): Flow<List<RecordEntity>> = dao.getAllFlow()

    suspend fun getAll(): List<RecordEntity> = dao.getAll()

    suspend fun add(record: RecordEntity): Long = dao.insert(record)

    suspend fun update(record: RecordEntity) = dao.update(record)

    suspend fun delete(record: RecordEntity) = dao.delete(record)

    suspend fun getById(id: Long): RecordEntity? = dao.getById(id)

    suspend fun clearAll() = dao.clearAll()
}
