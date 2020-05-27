package com.alfresco.content.browse

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.alfresco.content.models.NodeChildAssociationEntry

data class BrowseViewState(
    val nodes: Async<List<NodeChildAssociationEntry>> = Uninitialized
) : MvRxState