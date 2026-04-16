package com.igorthepadna.play_pause

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.util.Random

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
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (player.shuffleModeEnabled) {
                    fixShuffleOrder(player)
                }
            }

            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                if (enabled) {
                    fixShuffleOrder(player)
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    clearPlayNextExtra(player, player.currentMediaItemIndex)
                }
                if (player.shuffleModeEnabled) {
                    fixShuffleOrder(player)
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

    @OptIn(UnstableApi::class)
    private fun fixShuffleOrder(player: ExoPlayer) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return
        
        val count = timeline.windowCount
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex < 0 || currentIndex >= count) return

        // 1. Get current shuffled order
        val shuffled = mutableListOf<Int>()
        var curr = timeline.getFirstWindowIndex(true)
        while (curr != C.INDEX_UNSET) {
            shuffled.add(curr)
            curr = timeline.getNextWindowIndex(curr, Player.REPEAT_MODE_OFF, true)
            if (shuffled.size >= count) break
        }
        
        if (shuffled.size < count) return

        val currentPosInShuffled = shuffled.indexOf(currentIndex)
        if (currentPosInShuffled == -1) return

        // 2. Identify all Play Next items (excluding current)
        val playNextIndices = mutableListOf<Int>()
        for (i in 0 until count) {
            if (i == currentIndex) continue
            val item = player.getMediaItemAt(i)
            if (item.mediaMetadata.extras?.getBoolean("is_play_next", false) == true) {
                playNextIndices.add(i)
            }
        }
        
        if (playNextIndices.isEmpty()) return

        // 3. Rebuild shuffle order
        val playNextSet = playNextIndices.toSet()
        val beforeCurrent = mutableListOf<Int>()
        for (i in 0 until currentPosInShuffled) {
            val idx = shuffled[i]
            if (!playNextSet.contains(idx)) {
                beforeCurrent.add(idx)
            }
        }
        
        val afterCurrent = mutableListOf<Int>()
        for (i in (currentPosInShuffled + 1) until shuffled.size) {
            val idx = shuffled[i]
            if (!playNextSet.contains(idx)) {
                afterCurrent.add(idx)
            }
        }
        
        val newShuffledOrder = mutableListOf<Int>()
        newShuffledOrder.addAll(beforeCurrent)
        newShuffledOrder.add(currentIndex)
        // Sort play next indices to maintain relative addition order (most recent first)
        newShuffledOrder.addAll(playNextIndices.sorted())
        newShuffledOrder.addAll(afterCurrent)
        
        if (newShuffledOrder != shuffled && newShuffledOrder.size == count) {
            player.setShuffleOrder(DefaultShuffleOrder(newShuffledOrder.toIntArray(), Random().nextLong()))
        }
    }

    private fun clearPlayNextExtra(player: Player, index: Int) {
        if (index < 0 || index >= player.mediaItemCount) return
        val item = player.getMediaItemAt(index)
        val extras = item.mediaMetadata.extras
        if (extras?.getBoolean("is_play_next") == true) {
            val newExtras = Bundle(extras).apply {
                remove("is_play_next")
            }
            val newItem = item.buildUpon()
                .setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(newExtras).build())
                .setRequestMetadata(item.requestMetadata.buildUpon().setExtras(newExtras).build())
                .build()
            player.replaceMediaItem(index, newItem)
        }
    }
}
