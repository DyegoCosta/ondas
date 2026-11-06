package com.dyegocosta.ondas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface Screen {
    data object Home : Screen
    data object History : Screen
    data class Note(val id: Long, val isNew: Boolean, val animate: Boolean = false, val focus: Boolean = false) : Screen
}

class MainActivity : ComponentActivity() {
    private var screen by mutableStateOf<Screen>(Screen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        val repository = OndasRepository.get(this)
        setContent {
            OndasTheme {
                OndasApp(repository = repository, screen = screen, navigate = { screen = it })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action != ACTION_NOTE) return
        val id = intent.getLongExtra(EXTRA_ENTRY_ID, -1)
        intent.action = null
        if (id > 0 && OndasRepository.get(this).find(id) != null) {
            screen = Screen.Note(id = id, isNew = true, focus = true)
            Widgets.clearConfirmation(this)
        }
    }

    companion object {
        const val ACTION_NOTE = "com.dyegocosta.ondas.NOTE"
        const val EXTRA_ENTRY_ID = "entry_id"
    }
}
