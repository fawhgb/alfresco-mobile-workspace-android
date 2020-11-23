package com.alfresco.content.app.activity

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.alfresco.content.MvRxViewModel
import com.alfresco.content.data.AuthenticationRepository
import com.alfresco.content.session.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MainActivityState(
    val requiresReLogin: Boolean = false
) : MvRxState

class MainActivityViewModel(
    state: MainActivityState,
    context: Context
) : MvRxViewModel<MainActivityState>(state), LifecycleObserver {

    private val processLifecycleOwner = ProcessLifecycleOwner.get()
    private var refreshTicketJob: Job? = null

    init {
        // Start a new session
        val session = SessionManager.newSession(context)

        session?.onSignedOut {
            setState { copy(requiresReLogin = true) }
        }

        // Receives current state on observe
        processLifecycleOwner.lifecycle.addObserver(this)
    }

    val requiresLogin: Boolean
        get() = SessionManager.currentSession == null

    override fun onCleared() {
        super.onCleared()
        processLifecycleOwner.lifecycle.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() = refreshTicket()

    private fun refreshTicket() {
        refreshTicketJob?.cancel()
        refreshTicketJob = viewModelScope.launch {
            var success = false
            while (!success && isActive) {
                try {
                    val session = SessionManager.currentSession ?: return@launch
                    session.ticket = AuthenticationRepository().fetchTicket()
                    success = true
                } catch (_: Exception) {
                    delay(60 * 1000L)
                }
            }
        }
    }

    companion object : MvRxViewModelFactory<MainActivityViewModel, MainActivityState> {

        override fun create(viewModelContext: ViewModelContext, state: MainActivityState): MainActivityViewModel? {
            return MainActivityViewModel(state, viewModelContext.app())
        }
    }
}
