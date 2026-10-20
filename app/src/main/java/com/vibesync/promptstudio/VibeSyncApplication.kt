package com.vibesync.promptstudio

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.vibesync.promptstudio.data.local.AppDatabase
import com.vibesync.promptstudio.data.remote.KtorProviderStreamingClient
import com.vibesync.promptstudio.data.repository.CredentialRepositoryImpl
import com.vibesync.promptstudio.data.repository.PromptRepositoryImpl
import com.vibesync.promptstudio.data.security.EncryptedVault
import com.vibesync.promptstudio.domain.usecase.ExecutePromptUseCase
import com.vibesync.promptstudio.domain.usecase.ManageCredentialsUseCase
import com.vibesync.promptstudio.domain.usecase.SubstituteVariablesUseCase
import com.vibesync.promptstudio.domain.usecase.TokenCounterUseCase
import com.vibesync.promptstudio.presentation.PromptStudioViewModel
import com.vibesync.promptstudio.presentation.SettingsViewModel
import com.vibesync.promptstudio.presentation.auth.OAuthManager

class VibeSyncApplication : Application() {
    private val containerDelegate = lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AppContainer(this) }
    val container: AppContainer by containerDelegate

    override fun onTerminate() {
        if (containerDelegate.isInitialized()) container.close()
        super.onTerminate()
    }
}

class AppContainer(application: Application) {
    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "vibesync_local.db"
    ).build()

    private val vault = EncryptedVault(application)
    private val streamingClient = KtorProviderStreamingClient()
    private val promptRepository = PromptRepositoryImpl(database.promptDao(), vault, streamingClient)
    private val credentialRepository = CredentialRepositoryImpl(vault)
    private val substituteVariables = SubstituteVariablesUseCase()
    private val tokenCounter = TokenCounterUseCase()
    private val executePrompt = ExecutePromptUseCase(promptRepository)
    private val manageCredentials = ManageCredentialsUseCase(credentialRepository)
    val oauthManager = OAuthManager(vault, streamingClient.httpClient)

    val promptStudioFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PromptStudioViewModel::class.java)) {
                return PromptStudioViewModel(
                    promptRepository,
                    executePrompt,
                    substituteVariables,
                    tokenCounter
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: " + modelClass.name)
        }
    }

    val settingsFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(manageCredentials, oauthManager.isConfigured) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: " + modelClass.name)
        }
    }

    fun close() {
        streamingClient.httpClient.close()
        database.close()
    }
}
