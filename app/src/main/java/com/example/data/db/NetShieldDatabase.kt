package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppFirewallRule
import com.example.data.model.CustomDomainRule
import com.example.data.model.NetworkLogEntity

@Database(
    entities = [
        NetworkLogEntity::class,
        AppFirewallRule::class,
        CustomDomainRule::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NetShieldDatabase : RoomDatabase() {

    abstract fun netShieldDao(): NetShieldDao

    companion object {
        @Volatile
        private var INSTANCE: NetShieldDatabase? = null

        fun getInstance(context: Context): NetShieldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NetShieldDatabase::class.java,
                    "netshield_secure.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
