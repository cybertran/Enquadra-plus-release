package com.cybertran.enquadraplus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NormativeDao {

    @Query(
        """
        SELECT DISTINCT normative_acts.*
        FROM normative_acts
        WHERE normative_acts.id IN (
            SELECT actId
            FROM normative_act_search
            WHERE normative_act_search MATCH :ftsQuery
            UNION
            SELECT actId
            FROM normative_full_text_search
            WHERE normative_full_text_search MATCH :ftsQuery
        )
        ORDER BY normative_acts.year DESC, normative_acts.number DESC
        """
    )
    fun searchFts(ftsQuery: String): Flow<List<NormativeActEntity>>

    @Query(
        """
        SELECT * FROM normative_acts
        ORDER BY year DESC, number DESC
        """
    )
    fun observeAll(): Flow<List<NormativeActEntity>>

    @Query("SELECT * FROM normative_acts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): NormativeActEntity?

    @Query(
        """
        SELECT * FROM normative_acts
        WHERE actType = :actType
          AND number = :number
          AND year = :year
        LIMIT 1
        """
    )
    suspend fun findByReference(
        actType: String,
        number: Int,
        year: Int,
    ): NormativeActEntity?

    @Query("SELECT version FROM dataset_metadata WHERE datasetId = :datasetId LIMIT 1")
    suspend fun getDatasetVersion(datasetId: String): String?

    @Query("SELECT COUNT(*) FROM normative_acts WHERE actType = :actType")
    suspend fun countByActType(actType: String): Int

    @Query("SELECT * FROM normative_full_text WHERE actId = :actId LIMIT 1")
    suspend fun findFullTextByActId(actId: String): NormativeFullTextEntity?

    @Query(
        """
        SELECT * FROM infraction_norm_links
        WHERE infractionCode = :infractionCode
        ORDER BY
            CASE relationKind
                WHEN 'EXPLICIT_MBFT_REFERENCE' THEN 0
                WHEN 'DIRECT_CTB_ARTICLE_REFERENCE' THEN 1
                ELSE 2
            END,
            actId
        """
    )
    suspend fun findLinksForInfraction(
        infractionCode: String,
    ): List<InfractionNormLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFullTexts(entities: List<NormativeFullTextEntity>)

    @Insert
    suspend fun insertFullTextSearchEntries(
        entities: List<NormativeFullTextSearchEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInfractionNormLinks(
        entities: List<InfractionNormLinkEntity>,
    )

    @Query("DELETE FROM normative_full_text_search")
    suspend fun clearFullTextSearchIndex()

    @Query("DELETE FROM normative_full_text")
    suspend fun clearFullTexts()

    @Query("DELETE FROM infraction_norm_links")
    suspend fun clearInfractionNormLinks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAct(entity: NormativeActEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActs(entities: List<NormativeActEntity>)

    @Insert
    suspend fun insertSearch(entity: NormativeActSearchEntity)

    @Insert
    suspend fun insertSearchEntries(entities: List<NormativeActSearchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatasetMetadata(entity: DatasetMetadataEntity)

    @Query("DELETE FROM normative_act_search")
    suspend fun clearSearchIndex()

    @Query("DELETE FROM normative_acts")
    suspend fun clearActs()

    @Query("DELETE FROM dataset_metadata WHERE datasetId = :datasetId")
    suspend fun clearDatasetMetadata(datasetId: String)
}
