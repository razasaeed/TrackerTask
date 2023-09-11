package com.example.contractsdemo.demo

import com.example.contractsdemo.mvi.BaseViewModel

class DemoViewModel : BaseViewModel<DemoState, DemoEffect, DemoEvent>() {

    override fun createInitialState(): DemoState = DemoState(
        isLoading = true,
        usersResult = emptyList()
    )

    override fun handleEvent(event: DemoEvent) {
        when (event) {
            DemoEvent.GetUsers -> getUsers()
            is DemoEvent.ValidateAmount -> validateAmount()
        }
    }

    private fun getUsers() {
        setState { copy(isLoading = true) }

        val users = arrayListOf<String>()
        users.add("raza")
        users.add("sibghat")
        users.add("manan")
        users.add("asif")

        if (users.size > 0) {
            setState {
                copy(isLoading = false, usersResult = users)
            }
        } else {
            setState { copy(isLoading = false) }
            sendEffect(DemoEffect.ErrorWithGetUsers("fghfghfg"))
        }
    }

    private fun validateAmount() {
        // implementation here
    }
}