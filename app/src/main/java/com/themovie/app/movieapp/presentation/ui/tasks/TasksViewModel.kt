/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.themovie.app.movieapp.presentation.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.themovie.app.movieapp.ADD_EDIT_RESULT_OK
import com.themovie.app.movieapp.DELETE_RESULT_OK
import com.themovie.app.movieapp.EDIT_RESULT_OK
import com.themovie.app.movieapp.R
import com.themovie.app.movieapp.data.TaskRepository
import com.themovie.app.movieapp.data.source.network.DTOMovie
import com.themovie.app.movieapp.util.Async
import com.themovie.app.movieapp.util.WhileUiSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UiState for the task list screen.
 */
data class TasksUiState(
    val items: List<DTOMovie> = emptyList(),
    val isLoading: Boolean = false,
    val filteringUiInfo: FilteringUiInfo = FilteringUiInfo(),
    val userMessage: Int? = null
)

/**
 * ViewModel for the task list screen.
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _savedFilterType =
        savedStateHandle.getStateFlow(TASKS_FILTER_SAVED_STATE_KEY, TasksFilterType.ALL_TASKS)

    private val _filterUiInfo = _savedFilterType.map { getFilterUiInfo(it) }.distinctUntilChanged()
    private val _userMessage: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val _isLoading = MutableStateFlow(false)
    private val _filteredTasksAsync =
        combine(taskRepository.getTasksStreamPaging(), _savedFilterType) { tasks, type ->
            filterTasks(type)
        }
            .map { Async.Success(it) }
            .catch<Async<List<DTOMovie>>> { emit(Async.Error(R.string.loading_tasks_error)) }

    val uiState: StateFlow<TasksUiState> = combine(
        _filterUiInfo, _isLoading, _userMessage, _filteredTasksAsync
    ) { filterUiInfo, isLoading, userMessage, tasksAsync ->
        when (tasksAsync) {
            Async.Loading -> {
                TasksUiState(isLoading = true)
            }
            is Async.Error -> {
                TasksUiState(userMessage = tasksAsync.errorMessage)
            }
            is Async.Success -> {
                TasksUiState(
                    items = tasksAsync.data,
                    filteringUiInfo = filterUiInfo,
                    isLoading = isLoading,
                    userMessage = userMessage
                )
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = WhileUiSubscribed,
            initialValue = TasksUiState(isLoading = true)
        )

    fun setFiltering(requestType: TasksFilterType) {
        savedStateHandle[TASKS_FILTER_SAVED_STATE_KEY] = requestType
    }

    fun showEditResultMessage(result: Int) {
        when (result) {
            EDIT_RESULT_OK -> showSnackbarMessage(R.string.successfully_saved_task_message)
            ADD_EDIT_RESULT_OK -> showSnackbarMessage(R.string.successfully_added_task_message)
            DELETE_RESULT_OK -> showSnackbarMessage(R.string.successfully_deleted_task_message)
        }
    }

    fun snackbarMessageShown() {
        _userMessage.value = null
    }

    private fun showSnackbarMessage(message: Int) {
        _userMessage.value = message
    }

    fun refresh() {
        _isLoading.value = true
        viewModelScope.launch {
            taskRepository.refresh()
            _isLoading.value = false
        }
    }

    private fun filterTasks(filteringType: TasksFilterType): List<DTOMovie> {
        val tasksToShow = ArrayList<DTOMovie>()
        // We filter the tasks based on the requestType
//        for (task in tasks) {
//            when (filteringType) {
//                TasksFilterType.ALL_TASKS -> tasksToShow.add(task)
//                TasksFilterType.ACTIVE_TASKS -> tasksToShow.add(task)
//
//
//                TasksFilterType.COMPLETED_TASKS -> tasksToShow.add(task)
//            }
//        }
        return tasksToShow
    }

    private fun getFilterUiInfo(requestType: TasksFilterType): FilteringUiInfo =
        when (requestType) {
            TasksFilterType.ALL_TASKS -> {
                FilteringUiInfo(
                    R.string.label_all, R.string.no_tasks_all,
                    R.drawable.logo_no_fill
                )
            }

            TasksFilterType.ACTIVE_TASKS -> {
                FilteringUiInfo(
                    R.string.label_active, R.string.no_tasks_active,
                    R.drawable.ic_check_circle_96dp
                )
            }

            TasksFilterType.COMPLETED_TASKS -> {
                FilteringUiInfo(
                    R.string.label_completed, R.string.no_tasks_completed,
                    R.drawable.ic_verified_user_96dp
                )
            }
        }
}

// Used to save the current filtering in SavedStateHandle.
const val TASKS_FILTER_SAVED_STATE_KEY = "TASKS_FILTER_SAVED_STATE_KEY"

data class FilteringUiInfo(
    val currentFilteringLabel: Int = R.string.label_all,
    val noTasksLabel: Int = R.string.no_tasks_all,
    val noTaskIconRes: Int = R.drawable.logo_no_fill,
)
