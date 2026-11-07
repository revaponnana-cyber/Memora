package com.memora.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.memora.app.data.dao.LocationDao
import com.memora.app.data.dao.LocationEventDao
import com.memora.app.data.dao.ObjectDao
import com.memora.app.data.entity.LocationEntity
import com.memora.app.data.entity.LocationEventEntity
import com.memora.app.data.entity.ObjectEntity

/**
 * Local SQLite database — the only place user data lives. No network, no
 * cloud. Schema JSONs are exported to app/schemas for migration testing.
 */
@Database(
    entities = [
        ObjectEntity::class,
        LocationEntity::class,
        LocationEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MemoraDatabase : RoomDatabase() {

    abstract fun objectDao(): ObjectDao
    abstract fun locationDao(): LocationDao
    abstract fun locationEventDao(): LocationEventDao

    companion object {
        @Volatile
        private var instance: MemoraDatabase? = null

        fun get(context: Context): MemoraDatabase {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                val again = instance
                if (again != null) {
                    again
                } else {
                    val created = Room.databaseBuilder(
                        context.applicationContext,
                        MemoraDatabase::class.java,
                        "memora.db",
                    ).build()
                    instance = created
                    created
                }
            }
        }
    }
}
