package com.example.cybershield.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    val incidents: Flow<List<Incident>>
    val totalScanned: Flow<Int>
    val totalFlagged: Flow<Int>
    fun clearIncidents()
    fun incrementScanned()
    fun incrementFlagged()
    fun resetStats()
}

class DefaultDataRepository(private val context: Context) : DataRepository {
    
    private val dbHelper = IncidentDatabaseHelper.getInstance(context)
    private val appSettings = AppSettings.getInstance(context)

    override val incidents: Flow<List<Incident>> = dbHelper.incidentsFlow
    override val totalScanned: Flow<Int> = appSettings.totalScannedFlow
    override val totalFlagged: Flow<Int> = appSettings.totalFlaggedFlow

    override fun clearIncidents() {
        dbHelper.clearAllIncidents()
    }

    override fun incrementScanned() {
        appSettings.incrementScanned()
    }

    override fun incrementFlagged() {
        appSettings.incrementFlagged()
    }

    override fun resetStats() {
        appSettings.resetStats()
    }
}
