package com.cprassist.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cprassist.data.models.EventCategory
import com.cprassist.data.models.EventType

/**
 * Room database for storing session history
 */
@Database(
    entities = [SessionEntity::class, EventEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SessionDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: SessionDatabase? = null

        fun getInstance(context: Context): SessionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    "cpr_assist_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Type converters for Room
 */
class Converters {
    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = EventType.valueOf(value)

    @TypeConverter
    fun fromEventCategory(value: EventCategory): String = value.name

    @TypeConverter
    fun toEventCategory(value: String): EventCategory = EventCategory.valueOf(value)
}
