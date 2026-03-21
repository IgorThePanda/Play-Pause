package com.igorthepadna.play_pause

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

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
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                if (shuffleModeEnabled) {
                    val count = player.mediaItemCount
                    val currentIndex = player.currentMediaItemIndex
                    if (count > 0 && currentIndex != C.INDEX_UNSET) {
                        val playNextIndices = mutableListOf<Int>()
                        val normalIndices = mutableListOf<Int>()
                        
                        for (i in 0 until count) {
                            if (i == currentIndex) continue
                            val item = player.getMediaItemAt(i)
                            val isPlayNext = item.mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false
                            if (isPlayNext) {
                                playNextIndices.add(i)
                            } else {
                                normalIndices.add(i)
                            }
                        }
                        
                        // Shuffle only the normal items
                        val shuffledNormal = normalIndices.shuffled()
                        
                        // Reconstruct the order:
                        // 1. Shuffled normal items that were BEFORE the current index
                        // 2. The current item
                        // 3. Play Next items (in their original order)
                        // 4. Remaining shuffled normal items
                        
                        val newOrder = mutableListOf<Int>()
                        val itemsBefore = currentIndex // original count of items before current
                        
                        // Fill up to currentIndex with shuffled items
                        for (i in 0 until itemsBefore) {
                            if (i < shuffledNormal.size) {
                                newOrder.add(shuffledNormal[i])
                            }
                        }
                        
                        // Add current item
                        newOrder.add(currentIndex)
                        
                        // Add all Play Next items right after current
                        newOrder.addAll(playNextIndices)
                        
                        // Add remaining shuffled items
                        val usedNormalCount = newOrder.size - 1 - playNextIndices.size
                        if (usedNormalCount < shuffledNormal.size) {
                            newOrder.addAll(shuffledNormal.subList(usedNormalCount, shuffledNormal.size))
                        }
                        
                        if (newOrder.size == count) {
                            player.setShuffleOrder(DefaultShuffleOrder(newOrder.toIntArray(), System.currentTimeMillis()))
                        }
                    }
                }
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.playbackState == ExoPlayer.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
