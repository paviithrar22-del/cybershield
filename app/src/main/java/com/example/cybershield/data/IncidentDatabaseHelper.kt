package com.example.cybershield.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cybershield.nlp.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Incident(
    val id: Long = 0,
    val timestamp: Long,
    val sender: String,
    val messageContent: String,
    val sourceApp: String,
    val severity: Severity,
    val reason: String
)

class IncidentDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "cybershield.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_INCIDENTS = "incidents"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_SENDER = "sender"
        private const val COLUMN_MESSAGE = "message_content"
        private const val COLUMN_SOURCE_APP = "source_app"
        private const val COLUMN_SEVERITY = "severity"
        private const val COLUMN_REASON = "reason"

        @Volatile
        private var instance: IncidentDatabaseHelper? = null

        fun getInstance(context: Context): IncidentDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: IncidentDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _incidentsFlow = MutableStateFlow<List<Incident>>(emptyList())
    val incidentsFlow: StateFlow<List<Incident>> = _incidentsFlow.asStateFlow()

    init {
        refreshIncidents()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_INCIDENTS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_SENDER + " TEXT,"
                + COLUMN_MESSAGE + " TEXT,"
                + COLUMN_SOURCE_APP + " TEXT,"
                + COLUMN_SEVERITY + " TEXT,"
                + COLUMN_REASON + " TEXT" + ")")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INCIDENTS")
        onCreate(db)
    }

    fun insertIncident(incident: Incident): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TIMESTAMP, incident.timestamp)
            put(COLUMN_SENDER, incident.sender)
            put(COLUMN_MESSAGE, incident.messageContent)
            put(COLUMN_SOURCE_APP, incident.sourceApp)
            put(COLUMN_SEVERITY, incident.severity.name)
            put(COLUMN_REASON, incident.reason)
        }
        val id = db.insert(TABLE_INCIDENTS, null, values)
        refreshIncidents()
        return id
    }

    fun getAllIncidents(): List<Incident> {
        val list = mutableListOf<Incident>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_INCIDENTS ORDER BY $COLUMN_TIMESTAMP DESC"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                val sender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER))
                val messageContent = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))
                val sourceApp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE_APP))
                val severityStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SEVERITY))
                val reason = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REASON))

                val severity = try {
                    Severity.valueOf(severityStr)
                } catch (e: Exception) {
                    Severity.NONE
                }

                list.add(Incident(id, timestamp, sender, messageContent, sourceApp, severity, reason))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun clearAllIncidents() {
        val db = this.writableDatabase
        db.delete(TABLE_INCIDENTS, null, null)
        refreshIncidents()
    }

    fun refreshIncidents() {
        _incidentsFlow.value = getAllIncidents()
    }
}
