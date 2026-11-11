package com.kostku.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kostku.app.KostKuApp
import com.kostku.app.data.local.Session
import com.kostku.app.data.local.SessionStore
import com.kostku.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: Session) : SessionState
}

class AppViewModel(
    sessionStore: SessionStore,
    private val authRepository: AuthRepository
) : ViewModel() {

    val session: StateFlow<SessionState> = sessionStore.session
        .map<Session?, SessionState> { if (it == null) SessionState.LoggedOut else SessionState.LoggedIn(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KostKuApp
                AppViewModel(
                    sessionStore = app.container.sessionStore,
                    authRepository = app.container.authRepository
                )
            }
        }
    }
}
