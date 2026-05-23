package com.igorthepadna.play_pause

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.igorthepadna.play_pause.ui.PlayPauseApp
import com.igorthepadna.play_pause.ui.theme.PlayPauseTheme

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        setContent {
            val viewModel: MainViewModel = viewModel()
            
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val colorSchemeType by viewModel.colorSchemeType.collectAsStateWithLifecycle()

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            val useDynamicColor = colorSchemeType == ColorSchemeType.DYNAMIC

            LaunchedEffect(controllerFuture) {
                controllerFuture?.addListener({
                    viewModel.setPlayer(controllerFuture?.get())
                }, MoreExecutors.directExecutor())
            }

            PlayPauseTheme(
                darkTheme = isDarkTheme,
                dynamicColor = useDynamicColor,
                colorSchemeType = colorSchemeType
            ) {
                val player by viewModel.player.collectAsStateWithLifecycle()
                PlayPauseApp(viewModel, player, intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
