package com.alfresco.content.browse

import android.content.Context
import android.net.Uri
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.alfresco.content.actions.Action
import com.alfresco.content.actions.ActionAddFavorite
import com.alfresco.content.actions.ActionCreateFolder
import com.alfresco.content.actions.ActionDeleteForever
import com.alfresco.content.actions.ActionExtension
import com.alfresco.content.actions.ActionRemoveFavorite
import com.alfresco.content.actions.ActionRestore
import com.alfresco.content.actions.ActionUploadExtensionFiles
import com.alfresco.content.data.AnalyticsManager
import com.alfresco.content.data.BrowseRepository
import com.alfresco.content.data.Entry
import com.alfresco.content.data.FavoritesRepository
import com.alfresco.content.data.OfflineRepository
import com.alfresco.content.data.PageView
import com.alfresco.content.data.ResponsePaging
import com.alfresco.content.data.SearchRepository
import com.alfresco.content.data.SharedLinksRepository
import com.alfresco.content.data.SitesRepository
import com.alfresco.content.data.TrashCanRepository
import com.alfresco.content.listview.ListViewModel
import com.alfresco.content.listview.ListViewState
import com.alfresco.coroutines.asFlow
import com.alfresco.events.on
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

class BrowseViewModel(
    state: BrowseViewState,
    val context: Context,
    private val browseRepository: BrowseRepository,
    private val offlineRepository: OfflineRepository,
) : ListViewModel<BrowseViewState>(state) {

    private var observeUploadsJob: Job? = null
    private var observeTransferUploadsJob: Job? = null

    init {
        fetchInitial()
        withState {
            if (it.sortOrder == BrowseViewState.SortOrder.ByModifiedDate) {
                BrowseViewState.ModifiedGroup.prepare(context)
            }
        }
        if (state.path == context.getString(R.string.nav_path_recents)) {
            val list = offlineRepository.buildTransferList()
            if (list.isEmpty()) {
                offlineRepository.updateTransferSize(0)
            }
            setState { copy(totalTransfersSize = offlineRepository.getTotalTransfersSize()) }
        }

        if (state.path == context.getString(R.string.nav_path_favorites)) {
            val types = setOf(Entry.Type.FILE, Entry.Type.FOLDER)
            viewModelScope.on<ActionAddFavorite> { it.entry.ifType(types, ::refresh) }
            viewModelScope.on<ActionRemoveFavorite> {
                if (it.entries.isNotEmpty()) {
                    it.entries.forEach { entryObj ->
                        entryObj.ifType(types, ::removeEntry)
                    }
                } else {
                    it.entry.ifType(types, ::removeEntry)
                }
            }
        }

        if (state.path == context.getString(R.string.nav_path_fav_libraries)) {
            val types = setOf(Entry.Type.SITE)
            viewModelScope.on<ActionAddFavorite> { it.entry.ifType(types, ::refresh) }
            viewModelScope.on<ActionRemoveFavorite> { it.entry.ifType(types, ::removeEntry) }
        }

        if (state.path == context.getString(R.string.nav_path_trash)) {
            viewModelScope.on<ActionRestore> { removeDataEntry(it.entry, it.entries) }
            viewModelScope.on<ActionDeleteForever> { removeDataEntry(it.entry, it.entries) }
        }
    }

    private fun removeDataEntry(entry: Entry, entries: List<Entry>) {
        if (entries.isNotEmpty()) {
            entries.forEach { entryObj ->
                removeEntry(entryObj)
            }
        } else {
            removeEntry(entry)
        }
    }

    /**
     * trigger the screen events
     */
    fun triggerAnalyticsEvent(state: BrowseViewState) {
        when (state.path) {
            context.getString(R.string.nav_path_recents) -> AnalyticsManager().screenViewEvent(PageView.Recent)
            context.getString(R.string.nav_path_favorites) -> AnalyticsManager().screenViewEvent(PageView.Favorites)
            context.getString(R.string.nav_path_extension) -> AnalyticsManager().screenViewEvent(PageView.ShareExtension, noOfFiles = browseRepository.getExtensionDataList().size)
        }
    }

    @Suppress("ControlFlowWithEmptyBody")
    private fun Entry.ifType(
        types: Set<Entry.Type>,
        block: (entry: Entry) -> Unit,
    ) = if (types.contains(type)) {
        block(this)
    } else {
        // TODO
    }

    /**
     * refresh totalTransferSize count
     */
    fun refreshTransfersSize() = setState { copy(totalTransfersSize = offlineRepository.getTotalTransfersSize()) }

    @Suppress("UNUSED_PARAMETER")
    private fun refresh(ignored: Entry) = refresh() // TODO: why?

    override fun refresh() = fetchInitial()

    override fun fetchNextPage() = withState { state ->
        val path = state.path
        val nodeId = state.nodeId
        val skipCount = state.baseEntries.count()

        viewModelScope.launch {
            loadResults(
                path,
                nodeId,
                skipCount,
                ITEMS_PER_PAGE,
            ).execute {
                when (it) {
                    is Loading -> copy(request = Loading())
                    is Fail -> copy(request = Fail(it.error))
                    is Success -> {
                        update(it()).copy(request = Success(it()))
                    }

                    else -> {
                        this
                    }
                }
            }
        }
    }

    private fun fetchInitial() = withState { state ->
        viewModelScope.launch {
            // Fetch children and folder information
            loadResults(
                state.path,
                state.nodeId,
                0,
                ITEMS_PER_PAGE,
            ).zip(
                fetchNode(state.path, state.nodeId),
            ) { paging, parent ->
                Pair(paging, parent)
            }.execute {
                when (it) {
                    is Loading -> copy(request = Loading())
                    is Fail -> copy(request = Fail(it.error))
                    is Success -> {
                        observeUploads(it().second?.id)
                        update(it().first).copy(parent = it().second, request = Success(it().first))
                    }

                    else -> {
                        this
                    }
                }
            }
        }
    }

    private suspend fun fetchNode(path: String, item: String?): Flow<Entry?> = if (item.isNullOrEmpty()) {
        flowOf(null)
    } else {
        when (path) {
            context.getString(R.string.nav_path_site) ->
                BrowseRepository()::fetchLibraryDocumentsFolder.asFlow(item)

            else ->
                BrowseRepository()::fetchEntry.asFlow(item)
        }
    }

    private suspend fun loadResults(path: String, item: String?, skipCount: Int, maxItems: Int): Flow<ResponsePaging> {
        return when (path) {
            context.getString(R.string.nav_path_recents) -> {
                SearchRepository()::getRecents.asFlow(skipCount, maxItems)
            }

            context.getString(R.string.nav_path_favorites) ->
                FavoritesRepository()::getFavorites.asFlow(skipCount, maxItems)

            context.getString(R.string.nav_path_my_libraries) ->
                SitesRepository()::getMySites.asFlow(skipCount, maxItems)

            context.getString(R.string.nav_path_fav_libraries) ->
                FavoritesRepository()::getFavoriteLibraries.asFlow(skipCount, maxItems)

            context.getString(R.string.nav_path_shared) ->
                SharedLinksRepository()::getSharedLinks.asFlow(skipCount, maxItems)

            context.getString(R.string.nav_path_trash) ->
                TrashCanRepository()::getDeletedNodes.asFlow(skipCount, maxItems)

            context.getString(R.string.nav_path_folder) ->
                BrowseRepository()::fetchFolderItems.asFlow(requireNotNull(item), skipCount, maxItems)

            context.getString(R.string.nav_path_extension), context.getString(R.string.nav_path_move) ->
                BrowseRepository()::fetchExtensionFolderItems.asFlow(requireNotNull(item), skipCount, maxItems)

            context.getString(R.string.nav_path_site) ->
                BrowseRepository()::fetchLibraryItems.asFlow(requireNotNull(item), skipCount, maxItems)

            else -> throw IllegalStateException()
        }
    }

    private fun observeUploads(nodeId: String?) {
        if (nodeId == null) return

        val repo = OfflineRepository()

        // On refresh clean completed uploads
        repo.removeCompletedUploads(nodeId)

        observeUploadsJob?.cancel()
        observeUploadsJob = repo.observeUploads(nodeId)
            .execute {
                if (it is Success) {
                    updateUploads(it())
                } else {
                    this
                }
            }
    }

    /**
     * observer for transfer uploads
     */
    fun observeTransferUploads() {
        observeTransferUploadsJob?.cancel()
        observeTransferUploadsJob = OfflineRepository().observeTransferUploads()
            .execute {
                if (it is Success) {
                    updateTransferUploads(it())
                } else {
                    this
                }
            }
    }

    /**
     * reset local files after uploading to server
     */
    fun resetTransferData() {
        offlineRepository.removeCompletedUploads()
        offlineRepository.updateTransferSize(0)
    }

    override fun emptyMessageArgs(state: ListViewState) =
        when ((state as BrowseViewState).path) {
            context.getString(R.string.nav_path_recents) ->
                Triple(R.drawable.ic_empty_recent, R.string.recent_empty_title, R.string.recent_empty_message)

            context.getString(R.string.nav_path_favorites) ->
                Triple(R.drawable.ic_empty_favorites, R.string.favorite_files_empty_title, R.string.favorites_empty_message)

            context.getString(R.string.nav_path_fav_libraries) ->
                Triple(R.drawable.ic_empty_favorites, R.string.favorite_sites_empty_title, R.string.favorites_empty_message)

            else ->
                Triple(R.drawable.ic_empty_folder, R.string.folder_empty_title, R.string.folder_empty_message)
        }

    fun canAddItems(state: BrowseViewState): Boolean =
        when (state.path) {
            context.getString(R.string.nav_path_folder) -> state.parent?.canCreate ?: false
            context.getString(R.string.nav_path_site) -> state.parent?.canCreate ?: false
            else -> false
        }

    /**
     * Upload files on the current node
     */
    fun uploadFiles(state: BrowseViewState) {
        val list = browseRepository.getExtensionDataList().map { Uri.parse(it) }
        if (!list.isNullOrEmpty() && state.parent != null) {
            execute(ActionUploadExtensionFiles(state.parent), list)
            browseRepository.clearExtensionData()
        }
    }

    /**
     * It will create the new folder on the current node
     */
    fun createFolder(state: BrowseViewState) {
        if (state.parent != null) {
            execute(ActionCreateFolder(state.parent))
        }
    }

    private fun execute(action: ActionExtension, list: List<Uri>) =
        action.execute(context, GlobalScope, list)

    private fun execute(action: Action) =
        action.execute(context, GlobalScope)

    fun toggleSelection(entry: Entry) = setState {
        val hasReachedLimit = selectedEntries.size == MULTI_SELECTION_LIMIT
        if (!entry.isSelectedForMultiSelection && hasReachedLimit) {
            this
        } else {
            val updatedEntries = entries.map {
                if (it.id == entry.id && it.type != Entry.Type.GROUP) {
                    it.copy(isSelectedForMultiSelection = !it.isSelectedForMultiSelection)
                } else {
                    it
                }
            }
            copy(baseEntries = updatedEntries.filter { it.type != Entry.Type.GROUP }, entries = updatedEntries, selectedEntries = updatedEntries.filter { it.isSelectedForMultiSelection })
        }
    }

    fun resetMultiSelection() = setState {
        val resetMultiEntries = entries.map {
            it.copy(isSelectedForMultiSelection = false)
        }
        copy(baseEntries = resetMultiEntries.filter { it.type != Entry.Type.GROUP }, entries = resetMultiEntries, selectedEntries = emptyList())
    }

    companion object : MavericksViewModelFactory<BrowseViewModel, BrowseViewState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: BrowseViewState,
        ) = BrowseViewModel(state, viewModelContext.activity, BrowseRepository(), OfflineRepository())
    }
}
