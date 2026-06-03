package com.intimocoffee.loyalty.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.network.LoyaltyApiService
import com.intimocoffee.loyalty.core.network.LoginRequest
import com.intimocoffee.loyalty.core.network.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: LoyaltyApiService,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    
    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val response = apiService.login(LoginRequest(phone, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val customer = response.body()!!.data!!
                    sessionDataStore.saveSession(customer.id, customer.name, customer.phone)
                    _uiState.value = AuthUiState(isSuccess = true)
                } else {
                    _uiState.value = AuthUiState(
                        errorMessage = response.body()?.message ?: "Error al iniciar sesión"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    errorMessage = "No se pudo conectar al servidor: ${e.message}"
                )
            }
        }
    }
    
    fun register(name: String, phone: String, password: String, email: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val response = apiService.register(RegisterRequest(name, phone, password, email))
                if (response.isSuccessful && response.body()?.success == true) {
                    val customer = response.body()!!.data!!
                    sessionDataStore.saveSession(customer.id, customer.name, customer.phone)
                    _uiState.value = AuthUiState(isSuccess = true)
                } else {
                    _uiState.value = AuthUiState(
                        errorMessage = response.body()?.message ?: "Error al registrarse"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    errorMessage = "No se pudo conectar al servidor: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
