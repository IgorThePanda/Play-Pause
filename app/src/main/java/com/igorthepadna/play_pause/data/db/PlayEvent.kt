package com.igorthepadna.play_pause.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "play_events")
data class PlayEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val timestamp: Long // System.currentTimeMillis()
)
