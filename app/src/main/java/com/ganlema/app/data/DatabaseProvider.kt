package com.ganlema.app.data

import android.content.Context
import androidx.room.Room
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.ganlema.app.security.CryptoManager

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
        return Room.databaseBuilder(context, AppDatabase::class.java, "ganlema.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }
}
