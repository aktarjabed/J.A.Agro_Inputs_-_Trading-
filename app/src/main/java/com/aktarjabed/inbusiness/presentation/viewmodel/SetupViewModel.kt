package com.aktarjabed.inbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val businessContext: BusinessContext
) : ViewModel() {

    private val _setupComplete = MutableStateFlow(false)
    val setupComplete: StateFlow<Boolean> = _setupComplete

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun setupBusiness(name: String, address: String, gstin: String?) {
        viewModelScope.launch {
            try {
                if (name.isBlank() || address.isBlank()) {
                    _error.value = "Name and Address are required"
                    return@launch
                }

                val userId = UUID.randomUUID().toString()
                val businessId = UUID.randomUUID().toString()

                val businessData = BusinessData(
                    id = businessId,
                    name = name,
                    address = address,
                    gstin = gstin ?: "",
                    email = "",
                    phoneNumber = ""
                )

                businessRepository.saveBusinessData(businessData)

                // Initialize context
                businessContext.setUserId(userId)
                businessContext.setActiveBusinessId(businessId)

                _setupComplete.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to set up business"
            }
        }
    }
}
