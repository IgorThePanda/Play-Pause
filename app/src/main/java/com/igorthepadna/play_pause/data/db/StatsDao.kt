package com.igorthepadna.play_pause.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class TopTrack(
    val songId: Long,
    val playCount: Int
)

data class TopArtist(
    val artistName: String,
    val playCount: Int
)

data class DailyPlayTime(
    val date: Long, // Start of day timestamp
    val totalPlayCount: Int
)

@Dao
interface StatsDao {
    @Insert
    suspend fun insertPlayEvent(event: PlayEvent)

    @Query("""
        SELECT songId, COUNT(*) as playCount 
        FROM play_events 
        WHERE timestamp >= :since
        GROUP BY songId 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getTopTracks(limit: Int, since: Long = 0): Flow<List<TopTrack>>

    @Query("""
        SELECT s.artist as artistName, COUNT(*) as playCount 
        FROM play_events p
        JOIN cached_songs s ON p.songId = s.id
        WHERE p.timestamp >= :since
        GROUP BY s.artist 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getTopArtists(limit: Int, since: Long = 0): Flow<List<TopArtist>>

    @Query("SELECT COUNT(*) FROM play_events")
    fun getTotalPlayCount(): Flow<Long>

    @Query("""
        SELECT (timestamp / 86400000) * 86400000 as date, COUNT(*) as totalPlayCount
        FROM play_events
        WHERE timestamp >= :since
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getDailyPlayCounts(since: Long): Flow<List<DailyPlayTime>>

    @Query("DELETE FROM play_events")
    suspend fun clearAllStats()

    @Query("SELECT * FROM play_events")
    suspend fun getAllPlayEventsSync(): List<PlayEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayEvents(events: List<PlayEvent>)
}
