package com.cybertran.enquadraplus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CtbDao {

    @Query(
        """
        SELECT DISTINCT ctb_articles.*
        FROM ctb_articles
        JOIN ctb_article_search
          ON ctb_article_search.articleId = ctb_articles.id
        WHERE ctb_article_search MATCH :ftsQuery
        ORDER BY ctb_articles.number, ctb_articles.suffix
        """
    )
    fun searchFts(ftsQuery: String): Flow<List<CtbArticleEntity>>

    @Query("SELECT * FROM ctb_articles ORDER BY number, suffix")
    fun observeAll(): Flow<List<CtbArticleEntity>>

    @Query("SELECT * FROM ctb_articles WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CtbArticleEntity?

    @Query("SELECT version FROM dataset_metadata WHERE datasetId = :datasetId LIMIT 1")
    suspend fun getDatasetVersion(datasetId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(entity: CtbArticleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(entities: List<CtbArticleEntity>)

    @Insert
    suspend fun insertSearch(entity: CtbArticleSearchEntity)

    @Insert
    suspend fun insertSearchEntries(entities: List<CtbArticleSearchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatasetMetadata(entity: DatasetMetadataEntity)

    @Query("DELETE FROM ctb_article_search")
    suspend fun clearSearchIndex()

    @Query("DELETE FROM ctb_articles")
    suspend fun clearArticles()

    @Query("DELETE FROM dataset_metadata WHERE datasetId = :datasetId")
    suspend fun clearDatasetMetadata(datasetId: String)
}
