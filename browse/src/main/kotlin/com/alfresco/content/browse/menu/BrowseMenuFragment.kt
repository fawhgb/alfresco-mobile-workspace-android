package com.alfresco.content.browse.menu

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.alfresco.content.browse.R
import com.alfresco.content.browse.databinding.FragmentBrowseMenuBinding

class BrowseMenuFragment : BaseMvRxFragment() {

    private val viewModel: BrowseMenuViewModel by fragmentViewModel()
    private lateinit var binding: FragmentBrowseMenuBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBrowseMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun invalidate() = withState(viewModel) {
        binding.recyclerView.withModels {
            it.entries.forEach {
                if (it.path.isNotEmpty()) {
                    browseMenuRow {
                        id(it.title)
                        entry(it)
                        clickListener { _ -> navigateTo(it.path, it.title) }
                    }
                } else {
                    browseMenuSeparator {
                        id(it.title)
                    }
                }
            }
        }
    }

    private fun navigateTo(path: String, title: String) {
        if (path == getString(R.string.nav_path_my_files)) {
            val nodeId = viewModel.getMyFilesNodeId()
            navigateTo(Uri.parse("alfresco://content/folder/$nodeId?title=${Uri.encode(title)}"))
        } else {
            navigateTo(Uri.parse("alfresco://content/$path?title=${Uri.encode(title)}"))
        }
    }

    private fun navigateTo(uri: Uri) {
        findNavController().navigate(uri)
    }
}