package com.alfresco.content.search

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.parentFragmentViewModel
import com.alfresco.content.HideSoftInputOnScrollListener
import com.alfresco.content.data.Entry
import com.alfresco.content.data.SearchFilters
import com.alfresco.content.listview.ListFragment
import com.alfresco.content.navigateTo

class SearchResultsFragment : ListFragment<SearchViewModel, SearchResultsState>() {

    override val viewModel: SearchViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.addOnScrollListener(HideSoftInputOnScrollListener())
    }

    fun setSearchQuery(query: String) {
        scrollToTop()
        viewModel.setSearchQuery(query)
    }

    fun setFilters(filters: SearchFilters) {
        scrollToTop()
        viewModel.setFilters(filters)
    }

    private fun scrollToTop() {
        if (isResumed) {
            recyclerView.layoutManager?.scrollToPosition(0)
        }
    }

    fun saveCurrentSearch() {
        viewModel.saveSearch()
    }

    override fun onItemClicked(entry: Entry) {
        viewModel.saveSearch()

        findNavController().navigateTo(entry)
    }
}
