package com.videopoker.android

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.videopoker.android.ui.MainScreen
import com.videopoker.android.ui.state.GameViewModel
import com.videopoker.android.ui.theme.VideoPokerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        viewModel.loadGames(this)

        setContent {
            VideoPokerTheme {
                MainScreen(viewModel = viewModel)
            }
        }

        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        try {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (viewModel.uiState.value.showSplash) {
                viewModel.dismissSplash()
                return true
            }
            if (viewModel.uiState.value.activeOverlay != null) {
                if (event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    viewModel.closeOverlay()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
            when (event.keyCode) {
                KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> {
                    viewModel.onDealDraw()
                    return true
                }
                KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> {
                    viewModel.onToggleHold(0)
                    return true
                }
                KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> {
                    viewModel.onToggleHold(1)
                    return true
                }
                KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> {
                    viewModel.onToggleHold(2)
                    return true
                }
                KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> {
                    viewModel.onToggleHold(3)
                    return true
                }
                KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> {
                    viewModel.onToggleHold(4)
                    return true
                }
                KeyEvent.KEYCODE_B -> {
                    viewModel.onBetOne()
                    return true
                }
                KeyEvent.KEYCODE_M -> {
                    viewModel.onBetMax()
                    return true
                }
                KeyEvent.KEYCODE_A -> {
                    viewModel.onOpenAddMoney()
                    return true
                }
                KeyEvent.KEYCODE_C -> {
                    viewModel.onCashOut()
                    return true
                }
                KeyEvent.KEYCODE_S -> {
                    viewModel.onToggleMute()
                    return true
                }
                KeyEvent.KEYCODE_ESCAPE -> {
                    viewModel.closeOverlay()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
