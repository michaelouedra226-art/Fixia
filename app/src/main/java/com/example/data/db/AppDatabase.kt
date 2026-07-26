package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.models.ChatMessageEntity
import com.example.data.models.ConnaissancePersonnelleEntity
import com.example.data.models.DiagnosticEntity
import com.example.data.models.ProblemeSuiviEntity
import com.example.data.models.RoomEntity
import com.example.data.models.ZoneEntity

@Database(
    entities = [
        DiagnosticEntity::class,
        ProblemeSuiviEntity::class,
        ChatMessageEntity::class,
        ConnaissancePersonnelleEntity::class,
        RoomEntity::class,
        ZoneEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diagnosticDao(): DiagnosticDao
    abstract fun roomDao(): RoomDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fixia_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
