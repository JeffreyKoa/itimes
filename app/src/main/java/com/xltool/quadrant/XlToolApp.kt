package com.xltool.quadrant

import android.app.Application
import androidx.room.Room
import com.xltool.quadrant.data.AppDatabase
import com.xltool.quadrant.data.TaskRepository

class XlToolApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var taskRepository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "xltool.db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .build()

        taskRepository = TaskRepository(database)
    }
}


