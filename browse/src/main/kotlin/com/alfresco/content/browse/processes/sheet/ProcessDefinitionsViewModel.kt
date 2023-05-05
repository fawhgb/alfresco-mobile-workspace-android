package com.alfresco.content.browse.processes.sheet

import android.content.Context
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.alfresco.content.data.TaskRepository
import com.alfresco.coroutines.asFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch

internal class ProcessDefinitionsViewModel(
    state: ProcessDefinitionsState,
    val context: Context
) : MavericksViewModel<ProcessDefinitionsState>(state) {

    init {
        buildModel()
    }

    private fun buildModel() = withState { state ->
        viewModelScope.launch {
            processDefinitions().execute {
                when (it) {
                    is Success -> {
                        ProcessDefinitionsState(
                            entry = state.entry,
                            listProcessDefinitions = it().listRuntimeProcessDefinitions
                        )
                    }
                    else -> {
                        ProcessDefinitionsState(
                            entry = state.entry,
                            listProcessDefinitions = null
                        )
                    }
                }
            }
        }
    }

    private fun processDefinitions() = TaskRepository()::processDefinitions.asFlow()

    companion object : MavericksViewModelFactory<ProcessDefinitionsViewModel, ProcessDefinitionsState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: ProcessDefinitionsState
        ) =
            // Requires activity context in order to present other fragments
            ProcessDefinitionsViewModel(state, viewModelContext.activity())
    }
}
