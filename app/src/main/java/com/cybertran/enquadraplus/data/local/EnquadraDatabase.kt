package com.cybertran.enquadraplus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        InfractionEntity::class,
        InfractionSearchEntity::class,
        InfractionNoteEntity::class,
        InfractionLegalBasisEntity::class,
        OfficialSourceEntity::class,
        InfractionSourceCrossRef::class,
        InfractionRelationEntity::class,
        DatasetMetadataEntity::class,
        CtbArticleEntity::class,
        CtbArticleSearchEntity::class,
        NormativeActEntity::class,
        NormativeActSearchEntity::class,
        NormativeFullTextEntity::class,
        NormativeFullTextSearchEntity::class,
        InfractionNormLinkEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class EnquadraDatabase : RoomDatabase() {

    abstract fun infractionDao(): InfractionDao

    abstract fun ctbDao(): CtbDao

    abstract fun normativeDao(): NormativeDao

    companion object {
        fun create(context: Context): EnquadraDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                EnquadraDatabase::class.java,
                "enquadra.db",
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
