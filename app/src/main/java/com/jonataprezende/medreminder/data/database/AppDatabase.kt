package com.jonataprezende.medreminder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jonataprezende.medreminder.data.dao.DoseLogDao
import com.jonataprezende.medreminder.data.dao.MedicationDao
import com.jonataprezende.medreminder.data.dao.ScheduleDao
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.data.entity.Schedule
import com.jonataprezende.medreminder.data.entity.ScheduleType
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Database(
    entities = [Medication::class, Schedule::class, DoseLog::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medreminder.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @androidx.room.TypeConverter
    fun scheduleTypeToString(type: ScheduleType): String = type.name

    @androidx.room.TypeConverter
    fun stringToScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)

    @androidx.room.TypeConverter
    fun doseStatusToString(status: DoseStatus): String = status.name

    @androidx.room.TypeConverter
    fun stringToDoseStatus(value: String): DoseStatus = DoseStatus.valueOf(value)

    @androidx.room.TypeConverter
    fun listToJson(times: List<String>): String = json.encodeToString(times)

    @androidx.room.TypeConverter
    fun jsonToList(jsonStr: String): List<String> = json.decodeFromString(jsonStr)
}