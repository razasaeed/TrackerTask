package com.example.contractsdemo.demo

import com.example.contractsdemo.mvi.UiEffect
import com.example.contractsdemo.mvi.UiEvent
import com.example.contractsdemo.mvi.UiState

data class DemoState(
    val isLoading: Boolean,
    val usersResult: List<String>?,
) : UiState()

sealed class DemoEffect : UiEffect() {
//    object ErrorWithGetUsers : DemoEffect()
    data class ErrorWithGetUsers(val message: String) : DemoEffect()
}

sealed class DemoEvent : UiEvent() {
    object GetUsers : DemoEvent()
    class ValidateAmount(val amount: String) : DemoEvent()
}
