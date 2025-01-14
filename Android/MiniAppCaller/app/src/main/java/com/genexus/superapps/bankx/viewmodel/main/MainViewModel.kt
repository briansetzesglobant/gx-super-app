package com.genexus.superapps.bankx.viewmodel.main

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genexus.android.core.base.services.Services
import com.genexus.android.core.base.utils.Strings
import com.genexus.android.core.superapps.MiniApp
import com.genexus.android.core.superapps.MiniAppCollection
import com.genexus.android.core.superapps.errors.LoadError
import com.genexus.android.core.superapps.errors.SearchError
import com.genexus.android.core.tasking.OnCompleteListener
import com.genexus.android.core.tasking.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private var _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()
    private var _isRefreshing = MutableStateFlow(false)
    var isRefreshing = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            retrieveMiniApps()
            _isRefreshing.value = false
        }
    }

    private suspend fun retrieveMiniApps() {
        delay(1500)
        Services.SuperApps.searchByTag(Strings.EMPTY, 0, 10).addOnCompleteListener(object : OnCompleteListener<MiniAppCollection, SearchError> {
            override fun onComplete(task: Task<MiniAppCollection, SearchError>) {
                val miniApps = task.result
                if (!task.isSuccessful || miniApps.isNullOrEmpty()) {
                    _state.value = State.Error("No applications found")
                    return
                }

                _state.value = State.Data(miniApps)
            }
        })
    }

    fun loadMiniApp(miniApp: MiniApp) {
        Services.SuperApps.load(miniApp).addOnCompleteListener(object : OnCompleteListener<Boolean, LoadError> {
            override fun onComplete(task: Task<Boolean, LoadError>) {
                if (!task.isSuccessful)
                    _state.value = State.Error("MiniApp loading failed")
            }
        })
    }

    @SuppressLint("MissingPermission")
    suspend fun loadSandbox(context: Context) {
        val task = Services.SuperApps.Prototyping.loadSandbox()
        if (task == null) {
            Services.Device.runOnUiThread {
                Toast.makeText(context, "Loading task is null", Toast.LENGTH_SHORT).show()
            }
            return
        }

        task.addOnCompleteListener(object : OnCompleteListener<Boolean, LoadError> {
            override fun onComplete(task: Task<Boolean, LoadError>) {
                if (!task.isSuccessful) {
                    Services.Device.runOnUiThread {
                        Toast.makeText(context, "Loading task failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    init { refresh() }

    sealed class State {
        object Loading: State()
        data class Data(val data: MiniAppCollection): State()
        data class Error(val message: String): State()
    }
}
