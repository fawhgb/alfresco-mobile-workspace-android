package com.alfresco.content.actions

import android.content.Context
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.alfresco.content.data.BrowseRepository
import com.alfresco.content.data.Entry
import com.alfresco.content.data.FavoritesRepository
import com.alfresco.content.data.Settings
import com.alfresco.coroutines.asFlow
import com.alfresco.events.on
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ContextualActionsViewModel(
    state: ContextualActionsState,
    val context: Context,
    private val settings: Settings,
) : MavericksViewModel<ContextualActionsState>(state) {

    init {
        if (!state.isMultiSelection) {
            buildModelSingleSelection()
        } else {
            buildModelForMultiSelection()
        }

        // Update the model if necessary
        viewModelScope.on<ActionAddFavorite>(block = ::updateState)
        viewModelScope.on<ActionRemoveFavorite>(block = ::updateState)
        viewModelScope.on<ActionAddOffline>(block = ::updateState)
        viewModelScope.on<ActionRemoveOffline>(block = ::updateState)
        viewModelScope.on<ActionMoveFilesFolders>(block = ::updateState)
        viewModelScope.on<ActionUpdateFileFolder>(block = ::updateState)
        viewModelScope.on<ActionStartProcess>(block = ::updateState)
    }

    private fun buildModelSingleSelection() = withState { state ->
        // If entry is partial and not in the offline tab
        if (state.entries.isNotEmpty()) {
            state.entries.first().let { entry ->
                if (entry.isPartial && !entry.hasOfflineStatus) {
                    viewModelScope.launch {
                        fetchEntry(entry).execute {
                            when (it) {
                                is Success ->
                                    ContextualActionsState(entries = listOf(it()), actions = makeActions(it()), topActions = makeTopActions(it()), fetch = it)

                                is Fail ->
                                    ContextualActionsState(entries = listOf(entry), actions = makeActions(entry), topActions = makeTopActions(entry), fetch = it)

                                else ->
                                    copy(fetch = it)
                            }
                        }
                    }
                } else {
                    setState { copy(actions = makeActions(entry), topActions = makeTopActions(entry), fetch = Success(entry)) }
                }
            }
        }
    }

    private fun buildModelForMultiSelection() = withState { state ->
        // If entry is partial and not in the offline tab
        setState { copy(entries = state.entries, actions = makeMultiActions(state), topActions = emptyList()) }
    }

    private fun updateState(action: Action) {
        setState {
            val entry = action.entry as Entry

            ContextualActionsState(
                entries = if (isMultiSelection) action.entries else listOf(entry),
                actions = if (isMultiSelection) makeMultiActions(this) else makeActions(entry),
                topActions = makeTopActions(entry),
            )
        }
    }

    private fun fetchEntry(entry: Entry): Flow<Entry> =
        when (entry.type) {
            Entry.Type.SITE -> FavoritesRepository()::getFavoriteSite.asFlow(entry.id)
            else -> BrowseRepository()::fetchEntry.asFlow(entry.id)
        }

    fun execute(action: Action) =
        action.execute(context, GlobalScope)

    fun executeMulti(action: Action) =
        action.executeMulti(context, GlobalScope)

    private fun makeActions(entry: Entry): List<Action> =
        when {
            entry.isTrashed -> {
                actionsForTrashed(entry)
            }

            entry.hasOfflineStatus -> {
                actionsForOffline(entry)
            }

            else -> {
                defaultActionsFor(entry)
            }
        }

    fun makeMultiActions(state: ContextualActionsState): List<Action> {
        val actions = mutableListOf<Action>()
        val entry = Entry.withSelectedEntries(state.entries)

        when {
            state.entries.all { it.isTrashed } -> {
                actions.add(ActionRestore(entry, state.entries))
                actions.add(ActionDeleteForever(entry, state.entries))
            }

            state.entries.all { it.hasOfflineStatus } -> {
                // Added Favorite Action
                actions.addAll(sharedActions(entry, state.entries))
            }

            else -> {
                actions.addAll(sharedActions(entry, state.entries))
            }
        }

        return actions
    }

    private fun sharedActions(entry: Entry, entries: List<Entry>): List<Action> {
        val actions = mutableListOf<Action>()
        // Added Favorite Action
        if (entries.any { !it.isFavorite }) {
            actions.add(ActionAddFavorite(entry, entries))
        } else {
            actions.add(ActionRemoveFavorite(entry, entries))
        }

        // Added Start Process Action
        processMultiActionFor(entry, entries)?.let { action ->
            actions.add(action)
        }

        // Added Move Action
        if (isMoveDeleteAllowed(entries)) {
            actions.add(ActionMoveFilesFolders(entry, entries))
        }

        // Added Offline Action
        actions.add(offlineMultiActionFor(entry, entries))

        // Added Delete Action
        if (isMoveDeleteAllowed(entries)) {
            actions.add((ActionDelete(entry, entries)))
        }
        return actions
    }

    private fun defaultActionsFor(entry: Entry) =
        listOf(
            externalActionsFor(entry),
            favoriteActionFor(entry),
            if (settings.isProcessEnabled && entry.isFile) actionsProcesses(entry) else listOf(),
            renameMoveActionFor(entry),
            offlineActionFor(entry),
            deleteActionFor(entry),
        ).flatten()

    private fun actionsForTrashed(entry: Entry): List<Action> =
        listOf(ActionRestore(entry), ActionDeleteForever(entry))

    private fun actionsForOffline(entry: Entry): List<Action> =
        listOf(
            externalActionsFor(entry),
            if (settings.isProcessEnabled && entry.isFile) actionsProcesses(entry) else listOf(),
            offlineActionFor(entry),
        ).flatten()

    private fun actionsProcesses(entry: Entry): List<Action> =
        listOf(ActionStartProcess(entry))

    private fun processMultiActionFor(entry: Entry, entries: List<Entry>): Action? {
        if (settings.isProcessEnabled && (entries.isNotEmpty() && entries.all { it.isFile })) {
            return ActionStartProcess(entry, entries)
        }
        return null
    }

    private fun offlineActionFor(entry: Entry) =
        if (!entry.isFile && !entry.isFolder) {
            listOf()
        } else if (entry.hasOfflineStatus && !entry.isOffline) {
            listOf()
        } else {
            listOf(if (entry.isOffline) ActionRemoveOffline(entry) else ActionAddOffline(entry))
        }

    private fun offlineMultiActionFor(entry: Entry, entries: List<Entry>): Action {
        val filteredOffline = entries.filter { it.isFile || it.isFolder }.filter { !it.hasOfflineStatus || it.isOffline }

        return if (filteredOffline.any { !it.isOffline }) {
            ActionAddOffline(entry, entries)
        } else {
            ActionRemoveOffline(entry, entries)
        }
    }

    private fun favoriteActionFor(entry: Entry) =
        listOf(if (entry.isFavorite) ActionRemoveFavorite(entry) else ActionAddFavorite(entry))

    private fun externalActionsFor(entry: Entry) =
        if (entry.isFile) {
            listOf(ActionOpenWith(entry), ActionDownload(entry))
        } else {
            listOf()
        }

    private fun deleteActionFor(entry: Entry) = if (entry.canDelete) listOf(ActionDelete(entry)) else listOf()

    private fun renameMoveActionFor(entry: Entry): List<Action> {
        val actions = mutableListOf<Action>()
        if (entry.canDelete && (entry.isFile || entry.isFolder)) {
            actions.add(ActionUpdateFileFolder(entry))
            actions.add(ActionMoveFilesFolders(entry))
        }
        return actions
    }

    private fun makeTopActions(entry: Entry): List<Action> {
        val actions = mutableListOf<Action>()
        if (!entry.hasOfflineStatus) {
            if (entry.isFavorite) {
                actions.add(ActionRemoveFavorite(entry))
            } else {
                actions.add(ActionAddFavorite(entry))
            }
        }
        if (entry.isFile) {
            actions.add(ActionDownload(entry))
        }
        return actions
    }

    companion object : MavericksViewModelFactory<ContextualActionsViewModel, ContextualActionsState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: ContextualActionsState,
        ) =
            // Requires activity context in order to present other fragments
            ContextualActionsViewModel(state, viewModelContext.activity(), Settings(viewModelContext.activity))
    }
}
