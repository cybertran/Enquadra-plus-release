package com.cybertran.enquadraplus.data.user

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteInfractionEntity::class,
        InfractionHistoryEntity::class,
        InfractionAnnotationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UserDataDatabase : RoomDatabase() {

    abstract fun userInfractionDao(): UserInfractionDao

    companion object {
        fun create(context: Context): UserDataDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                UserDataDatabase::class.java,
                "enquadra-user.db",
            ).build()
    }
}
