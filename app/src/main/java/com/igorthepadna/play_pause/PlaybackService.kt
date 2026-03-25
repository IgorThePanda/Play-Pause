package com.igorthepadna.play_pause

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.igorthepadna.play_pause.utils.CustomShuffleOrder
import kotlinx.coroutines.*

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateShuffleJob: Job? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handleAudioFocus
            )
            .setHandleAudioBecomingNoisy(true) // Pauses music when headphones are disconnected
            .build()

        // Handle reshuffle logic while preserving "Play Next" items and current item positions
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateShuffleOrder(player)
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    // Use a shorter debounce for manual additions, but still debounce to handle chunks
                    updateShuffleOrder(player, debounceMs = 100)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateShuffleOrder(player)
            }
        })
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_PLAYER", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    @OptIn(UnstableApi::class)
    private fun updateShuffleOrder(player: ExoPlayer, debounceMs: Long = 500) {
        updateShuffleJob?.cancel()
        updateShuffleJob = serviceScope.launch {
            delay(debounceMs)
            
            val count = player.mediaItemCount
            if (count > 0) {
                val playNextIndices = mutableListOf<Int>()
                val normalIndices = mutableListOf<Int>()
                val currentIndex = player.currentMediaItemIndex
                
                // For very large playlists, scanning all items for extras is expensive.
                // We'll only do it if the count is reasonable or we're sure we have playNext items.
                // Optimization: if count > 2000, we might want to skip this or use a more efficient way.
                for (i in 0 until count) {
                    if (i == currentIndex) continue
                    val item = player.getMediaItemAt(i)
                    // Optimization: avoid accessing extras if possible
                    val extras = item.mediaMetadata.extras
                    val isPlayNext = extras?.getBoolean("is_play_next", false) ?: false
                    if (isPlayNext) {
                        playNextIndices.add(i)
                    } else {
                        normalIndices.add(i)
                    }
                }
                
                player.setShuffleOrder(CustomShuffleOrder(count, currentIndex, playNextIndices, normalIndices))
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.playbackState == ExoPlayer.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
