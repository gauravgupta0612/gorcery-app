package com.golagrocery.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golagrocery.app.GolaApp
import com.golagrocery.app.di.AppContainer

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as GolaApp).container

/** Creates a ViewModel with access to the [AppContainer] (manual DI). */
@Composable
inline fun <reified VM : ViewModel> golaViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = appContainer()
    return viewModel(key = key, factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create(container) as T
    })
}
