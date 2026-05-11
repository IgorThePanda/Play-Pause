package com.igorthepadna.play_pause.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class SkipType {
    ENTIRE_SONG,
    SECTION
}

@Serializable
@Entity(tableName = "skip_rules")
data class SkipRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mediaId: String,
    val type: SkipType,
    val startTime: Long = 0L, // in milliseconds
    val endTime: Long = 0L    // in milliseconds
)
