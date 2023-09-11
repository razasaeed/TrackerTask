package com.tracking.task.demo

import com.tracking.task.mvi.UiEffect
import com.tracking.task.mvi.UiEvent
import com.tracking.task.mvi.UiState

data class DemoState(
    val isLoading: Boolean,
    val locationsResult: String?
) : UiState()

sealed class DemoEffect : UiEffect() {
    data class ErrorWithGetLocations(val message: String) : DemoEffect()
}

sealed class DemoEvent : UiEvent() {
    object GetLocationsData : DemoEvent()
}
