package com.aktarjabed.inbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

sealed class SplashState {
    object Loading : SplashState()
    object GoToDashboard : SplashState()
    object GoToSetup : SplashState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val businessContext: BusinessContext,
    private val businessRepository: BusinessRepository
) : ViewModel() {

    val splashState: StateFlow<SplashState> = businessContext.activeBusinessId
        .map { businessId ->
            if (businessId.isBlank()) {
                SplashState.GoToSetup
            } else {
                val businessExists = businessRepository.getBusinessDataById(businessId) != null
                if (businessExists) {
                    SplashState.GoToDashboard
                } else {
                    SplashState.GoToSetup
                }
            }
        }
        .catch {
            emit(SplashState.GoToSetup)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SplashState.Loading
        )
}
