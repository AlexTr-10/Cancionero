package com.example.data.repository

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.model.ActaEntity
import com.example.data.model.MeetingMinute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ActaRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val actaDao = database.actaDao()

    val allActas: Flow<List<MeetingMinute>> = actaDao.getAllActas().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun getActaById(id: String): MeetingMinute? = withContext(Dispatchers.IO) {
        actaDao.getActaById(id)?.toDomainModel()
    }

    suspend fun insertOrUpdateActa(minute: MeetingMinute) = withContext(Dispatchers.IO) {
        val entity = ActaEntity.fromDomainModel(minute)
        actaDao.insertActa(entity)
    }

    suspend fun deleteActa(id: String) = withContext(Dispatchers.IO) {
        actaDao.deleteActaById(id)
    }
}
