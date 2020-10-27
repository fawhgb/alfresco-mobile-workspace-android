package com.alfresco.content.browse

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.alfresco.content.actions.ActionAddFavorite
import com.alfresco.content.actions.ActionRemoveFavorite
import com.alfresco.content.actions.on
import com.alfresco.content.data.BrowseRepository
import com.alfresco.content.data.Entry
import com.alfresco.content.data.FavoritesRepository
import com.alfresco.content.data.ResponsePaging
import com.alfresco.content.data.SearchRepository
import com.alfresco.content.data.SharedLinksRepository
import com.alfresco.content.data.SitesRepository
import com.alfresco.content.data.TrashCanRepository
import com.alfresco.content.listview.ListViewModel
import java.lang.IllegalStateException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BrowseViewModel(
    state: BrowseViewState,
    val context: Context
) : ListViewModel<BrowseViewState>(state) {

    init {
        refresh()

        withState {
            if (sortOrder(state.path) == Entry.SortOrder.ByModifiedDate) {
                BrowseViewState.ModifiedGroup.prepare(context)
            }
        }

        if (state.path == context.getString(R.string.nav_path_favorites)) {
            val types = setOf(Entry.Type.File, Entry.Type.Folder)
            viewModelScope.on<ActionAddFavorite> { it.entry.ifType(types, ::addEntry) }
            viewModelScope.on<ActionRemoveFavorite> { it.entry.ifType(types, ::removeEntry) }
        }

        if (state.path == context.getString(R.string.nav_path_fav_libraries)) {
            val types = setOf(Entry.Type.Site)
            viewModelScope.on<ActionAddFavorite> { it.entry.ifType(types, ::addEntry) }
            viewModelScope.on<ActionRemoveFavorite> { it.entry.ifType(types, ::removeEntry) }
        }
    }

    @Suppress("ControlFlowWithEmptyBody")
    private fun Entry.ifType(
        types: Set<Entry.Type>,
        block: (entry: Entry) -> Unit
    ) = if (types.contains(type)) { block(this) } else { }

    private fun addEntry(entry: Entry) =
        setState { copy(entries = listOf(entry) + entries) }

    private fun removeEntry(entry: Entry) =
        setState { copy(entries = entries.filter { it.id != entry.id }) }

    override fun refresh() = fetch()

    override fun fetchNextPage() = fetch(true)

    private fun fetch(nextPage: Boolean = false) = withState { state ->
        val path = state.path
        val nodeId = state.nodeId
        val skipCount = if (nextPage) state.baseEntries.count() else 0

        viewModelScope.launch {
            loadResults(
                path,
                nodeId,
                skipCount,
                ITEMS_PER_PAGE
            ).execute {
                if (it is Loading) {
                    copy(request = it)
                } else {
                    updateEntries(it(), sortOrder(path)).copy(request = it)
                }
            }
        }
    }

    private suspend fun loadResults(path: String, item: String?, skipCount: Int, maxItems: Int): Flow<ResponsePaging> {
        return when (path) {
            context.getString(R.string.nav_path_recents) ->
                SearchRepository().getRecents(skipCount, maxItems)

            context.getString(R.string.nav_path_favorites) ->
                FavoritesRepository().getFavorites(skipCount, maxItems)

            context.getString(R.string.nav_path_my_libraries) ->
                SitesRepository().getMySites(skipCount, maxItems)

            context.getString(R.string.nav_path_fav_libraries) ->
                FavoritesRepository().getFavoriteLibraries(skipCount, maxItems)

            context.getString(R.string.nav_path_shared) ->
                SharedLinksRepository().getSharedLinks(skipCount, maxItems)

            context.getString(R.string.nav_path_trash) ->
                TrashCanRepository().getDeletedNodes(skipCount, maxItems)

            context.getString(R.string.nav_path_folder) ->
                BrowseRepository().loadItemsInFolder(requireNotNull(item), skipCount, maxItems)

            context.getString(R.string.nav_path_site) ->
                BrowseRepository().loadItemsInSite(requireNotNull(item), skipCount, maxItems)

            else -> throw IllegalStateException()
        }
    }

    private fun sortOrder(path: String): Entry.SortOrder {
        return when (path) {
            context.getString(R.string.nav_path_recents) -> Entry.SortOrder.ByModifiedDate
            else -> Entry.SortOrder.Default
        }
    }

    companion object : MvRxViewModelFactory<BrowseViewModel, BrowseViewState> {

        override fun create(viewModelContext: ViewModelContext, state: BrowseViewState): BrowseViewModel? {
            return BrowseViewModel(state, viewModelContext.app())
        }
    }
}
