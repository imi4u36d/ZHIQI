package com.zhiqi.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.zhiqi.app.security.CryptoManager

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }
    }

    private fun build(context: Context): AppDatabase {
        SQLiteDatabase.loadLibs(context)
        val passphrase = CryptoManager(context).getOrCreateDbPassphrase()
        val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
        return Room.databaseBuilder(context, AppDatabase::class.java, "zhiqi.db")
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `daily_indicators` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dateKey` TEXT NOT NULL,
                    `metricKey` TEXT NOT NULL,
                    `optionValue` TEXT NOT NULL,
                    `displayLabel` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_indicators_dateKey_metricKey`
                ON `daily_indicators` (`dateKey`, `metricKey`)
                """.trimIndent()
            )
        }
    }
}
