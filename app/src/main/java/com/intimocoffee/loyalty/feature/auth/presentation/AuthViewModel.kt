package com.intimocoffee.loyalty.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.network.LoyaltyApiService
import com.intimocoffee.loyalty.core.network.LoginRequest
import com.intimocoffee.loyalty.core.network.RegisterRequest
import com.intimocoffee.loyalty.core.network.SetPasswordRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    /** When login fails because account has no password yet */
    val needsSetPassword: Boolean = false,
    val setPasswordPhone: String? = null
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
                val normalized = phone.filter { it.isDigit() }.let { if (it.length >= 10) it.takeLast(10) else it }
                val response = apiService.login(LoginRequest(normalized, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val customer = response.body()!!.data!!
                    sessionDataStore.saveSession(customer.id, customer.name, customer.phone)
                    _uiState.value = AuthUiState(isSuccess = true)
                } else {
                    val raw = response.body()?.message ?: "Error al iniciar sesión"
                    val needsSet = raw.contains("sin contraseña", ignoreCase = true) ||
                        (raw.contains("contraseña", ignoreCase = true) &&
                            (raw.contains("sin", ignoreCase = true) || raw.contains("aún no", ignoreCase = true)))
                    if (needsSet) {
                        _uiState.value = AuthUiState(
                            errorMessage = raw,
                            needsSetPassword = true,
                            setPasswordPhone = normalized
                        )
                    } else if (raw.contains("incorrecta", ignoreCase = true)) {
                        _uiState.value = AuthUiState(
                            errorMessage = "$raw Si no recuerdas tu contraseña, escríbenos a cafeintimo@gmail.com."
                        )
                    } else {
                        _uiState.value = AuthUiState(errorMessage = raw)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(errorMessage = e.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun setPassword(phone: String, newPassword: String, currentPassword: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val normalized = phone.filter { it.isDigit() }.let { if (it.length >= 10) it.takeLast(10) else it }
                val response = apiService.setPassword(
                    SetPasswordRequest(
                        phone = normalized,
                        newPassword = newPassword,
                        currentPassword = currentPassword
                    )
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val customer = response.body()!!.data!!
                    sessionDataStore.saveSession(customer.id, customer.name, customer.phone)
                    _uiState.value = AuthUiState(isSuccess = true)
                } else {
                    _uiState.value = AuthUiState(
                        errorMessage = response.body()?.message ?: "Error al guardar contraseña",
                        needsSetPassword = true,
                        setPasswordPhone = normalized
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    errorMessage = e.message ?: "Error al guardar contraseña",
                    needsSetPassword = true,
                    setPasswordPhone = phone
                )
            }
        }
    }

    fun dismissSetPassword() {
        _uiState.value = _uiState.value.copy(needsSetPassword = false)
    }

    fun register(name: String, phone: String, password: String, email: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val normalized = phone.filter { it.isDigit() }.let { if (it.length >= 10) it.takeLast(10) else it }
                val response = apiService.register(
                    RegisterRequest(name.trim(), normalized, password, email?.trim()?.ifBlank { null })
                )
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
                _uiState.value = AuthUiState(errorMessage = e.message ?: "Error al registrarse")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
