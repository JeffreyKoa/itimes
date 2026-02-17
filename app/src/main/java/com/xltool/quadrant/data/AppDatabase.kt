package com.xltool.quadrant.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderIntervalValue INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderIntervalUnit INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isMIT INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN dueTimestamp INTEGER")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN audioPath TEXT")
            }
        }

        /**
         * 迁移 5 -> 6:
         * 1. 添加 repeatType 列
         * 2. 状态值迁移：
         *    - 旧 NOT_STARTED(0), DEFERRED(3) -> 新 IN_PROGRESS(0)
         *    - 旧 IN_PROGRESS(1) -> 新 IN_PROGRESS(0)
         *    - 旧 COMPLETED(2) -> 新 COMPLETED(1)
         *    - 旧 OVERDUE(4) -> 新 OVERDUE(2)
         */
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 添加 repeatType 列
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatType INTEGER NOT NULL DEFAULT 0")
                
                // 状态迁移：
                // NOT_STARTED(0) 和 DEFERRED(3) 都变成 IN_PROGRESS(0)
                // IN_PROGRESS(1) -> 0
                // COMPLETED(2) -> 1
                // OVERDUE(4) -> 2
                db.execSQL("""
                    UPDATE tasks SET status = CASE status
                        WHEN 0 THEN 0
                        WHEN 1 THEN 0
                        WHEN 2 THEN 1
                        WHEN 3 THEN 0
                        WHEN 4 THEN 2
                        ELSE 0
                    END
                """)
            }
        }
    }
}


