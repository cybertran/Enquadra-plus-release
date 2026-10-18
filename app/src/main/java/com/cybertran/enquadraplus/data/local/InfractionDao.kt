package com.cybertran.enquadraplus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InfractionDao {

    @Query(
        """
        SELECT DISTINCT infractions.*
        FROM infractions
        JOIN infraction_search
          ON infraction_search.code = infractions.code
        WHERE infraction_search MATCH :ftsQuery
        ORDER BY infractions.code
        """
    )
    fun searchFts(ftsQuery: String): Flow<List<InfractionEntity>>

    @Query("SELECT * FROM infractions ORDER BY code")
    fun observeAll(): Flow<List<InfractionEntity>>

    @Transaction
    @Query("SELECT * FROM infractions WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): InfractionWithDetails?

    @Query("SELECT version FROM dataset_metadata WHERE datasetId = :datasetId LIMIT 1")
    suspend fun getDatasetVersion(datasetId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInfraction(entity: InfractionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInfractions(entities: List<InfractionEntity>)

    @Insert
    suspend fun insertSearch(entity: InfractionSearchEntity)

    @Insert
    suspend fun insertSearchEntries(entities: List<InfractionSearchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(entities: List<InfractionNoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLegalBasis(entities: List<InfractionLegalBasisEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(entity: OfficialSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(entities: List<OfficialSourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSourceCrossRef(entity: InfractionSourceCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSourceCrossRefs(entities: List<InfractionSourceCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(entities: List<InfractionRelationEntity>)

    @Query("SELECT COUNT(*) FROM infraction_relations")
    suspend fun relationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatasetMetadata(entity: DatasetMetadataEntity)

    @Query("DELETE FROM infraction_source_cross_ref")
    suspend fun clearSourceCrossRefs()

    @Query("DELETE FROM infraction_relations")
    suspend fun clearRelations()

    @Query("DELETE FROM infraction_notes")
    suspend fun clearNotes()

    @Query("DELETE FROM infraction_legal_basis")
    suspend fun clearLegalBasis()

    @Query("DELETE FROM infraction_search")
    suspend fun clearSearchIndex()

    @Query("DELETE FROM infractions")
    suspend fun clearInfractions()

    @Query("DELETE FROM official_sources")
    suspend fun clearSources()

    @Query("DELETE FROM dataset_metadata WHERE datasetId = :datasetId")
    suspend fun clearDatasetMetadata(datasetId: String)
}
