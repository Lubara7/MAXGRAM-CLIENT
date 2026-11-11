package io.github.lubara7.tgmax

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lubara7.tgmax.auth.AuthRoute
import io.github.lubara7.tgmax.auth.AuthViewModel
import io.github.lubara7.tgmax.domain.AuthRepository
import io.github.lubara7.tgmax.localization.LanguageController
import io.github.lubara7.tgmax.localization.LanguagePickerScreen

@Composable
fun AppNavigation(repository: AuthRepository) {
    val factory = remember(repository) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AuthViewModel::class.java))
                return AuthViewModel(repository) as T
            }
        }
    }
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    var languagePicker by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = languagePicker) { languagePicker = false }
    Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (languagePicker) {
            LanguagePickerScreen(LanguageController.currentTag(), onSelect = {
                languagePicker = false
                LanguageController.select(it)
            }, onBack = { languagePicker = false })
        } else {
            AuthRoute(authViewModel, demo = BuildConfig.IS_DEMO, onLanguage = { languagePicker = true })
        }
    }
}
