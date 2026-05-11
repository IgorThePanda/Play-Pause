package com.igorthepadna.play_pause.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SkipRuleDao {
    @Query("SELECT * FROM skip_rules")
    fun getAllRules(): Flow<List<SkipRuleEntity>>

    @Query("SELECT * FROM skip_rules WHERE mediaId = :mediaId")
    fun getRulesForSong(mediaId: String): Flow<List<SkipRuleEntity>>

    @Query("SELECT * FROM skip_rules WHERE mediaId = :mediaId")
    fun getRulesForSongSync(mediaId: String): List<SkipRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SkipRuleEntity): Long

    @Delete
    suspend fun delete(rule: SkipRuleEntity)

    @Query("DELETE FROM skip_rules WHERE mediaId = :mediaId")
    suspend fun deleteRulesForSong(mediaId: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<SkipRuleEntity>)
}
