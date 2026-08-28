package com.intimocoffee.loyalty.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.network.CustomerResponse
import com.intimocoffee.loyalty.core.network.LoyaltyApiService
import com.intimocoffee.loyalty.core.network.UpdateCustomerRequest
import com.intimocoffee.loyalty.ui.components.IntimoAvatar
import com.intimocoffee.loyalty.ui.components.IntimoGenderField
import com.intimocoffee.loyalty.ui.components.IntimoOutlinedField
import com.intimocoffee.loyalty.ui.components.IntimoPrimaryButton
import com.intimocoffee.loyalty.ui.components.IntimoScreenHeader
import com.intimocoffee.loyalty.ui.theme.IntimoAppInfo
import com.intimocoffee.loyalty.ui.theme.IntimoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val customer: CustomerResponse? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val apiService: LoyaltyApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val customerId = sessionDataStore.customerId.first() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Sesión no válida. Vuelve a iniciar sesión."
                    )
                    return@launch
                }
                val response = apiService.getCustomer(customerId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(isLoading = false, customer = response.body()?.data)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "No se pudo cargar el perfil"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error")
            }
        }
    }

    fun saveProfile(
        name: String, lastName: String, email: String,
        birthDate: String, favoriteDrink: String, allergies: String, gender: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val customerId = sessionDataStore.customerId.first() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = "Sesión no válida. Vuelve a iniciar sesión."
                    )
                    return@launch
                }
                val request = UpdateCustomerRequest(
                    name = name.trim().ifBlank { null },
                    lastName = lastName.trim().ifBlank { null },
                    email = email.trim().ifBlank { null },
                    birthDate = birthDate.trim().ifBlank { null },
                    favoriteDrink = favoriteDrink.trim().ifBlank { null },
                    allergies = allergies.trim().ifBlank { null },
                    gender = gender.ifBlank { null }
                )
                val response = apiService.updateCustomer(customerId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val updated = response.body()!!.data!!
                    val displayName = listOf(updated.name.orEmpty(), updated.lastName.orEmpty())
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { updated.name }
                    sessionDataStore.saveSession(updated.id, displayName, updated.phone)
                    _uiState.value = _uiState.value.copy(isSaving = false, customer = updated, message = "Perfil actualizado")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = response.body()?.message ?: "Error"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, message = e.message ?: "Error")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { sessionDataStore.clearSession() }
    }

    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    var name by remember(state.customer) { mutableStateOf(state.customer?.name ?: "") }
    var lastName by remember(state.customer) { mutableStateOf(state.customer?.lastName ?: "") }
    var email by remember(state.customer) { mutableStateOf(state.customer?.email ?: "") }
    var birthDate by remember(state.customer) { mutableStateOf(state.customer?.birthDate ?: "") }
    var favoriteDrink by remember(state.customer) { mutableStateOf(state.customer?.favoriteDrink ?: "") }
    var allergies by remember(state.customer) { mutableStateOf(state.customer?.allergies ?: "") }
    var gender by remember(state.customer) { mutableStateOf(state.customer?.gender ?: "") }

    if (state.message != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("Aviso") },
            text = { Text(state.message.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) { Text("OK") }
            }
        )
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = IntimoColors.Espresso)
        }
        return
    }

    if (state.error != null && state.customer == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(state.error.orEmpty(), color = IntimoColors.Red, textAlign = TextAlign.Center)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        val initials = listOf(name.trim(), lastName.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .ifBlank { "IC" }
        IntimoAvatar(initials = initials)
        Spacer(Modifier.height(12.dp))
        IntimoScreenHeader(
            title = "Tu perfil",
            subtitle = "Ajustes y preferencias",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = IntimoColors.Espresso)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Información personal",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(16.dp))
                IntimoOutlinedField(label = "Nombre", value = name, onValueChange = { name = it })
                Spacer(Modifier.height(10.dp))
                IntimoOutlinedField(label = "Apellido", value = lastName, onValueChange = { lastName = it })
                Spacer(Modifier.height(10.dp))
                IntimoOutlinedField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    keyboardType = KeyboardType.Email,
                )
                Spacer(Modifier.height(10.dp))
                IntimoOutlinedField(
                    label = "Fecha de nacimiento",
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    placeholder = "YYYY-MM-DD",
                )
                Spacer(Modifier.height(10.dp))
                IntimoGenderField(value = gender, onValueChange = { gender = it })
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Coffee, null, tint = IntimoColors.Espresso)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Preferencias",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(16.dp))
                IntimoOutlinedField(label = "Bebida favorita", value = favoriteDrink, onValueChange = { favoriteDrink = it })
                Spacer(Modifier.height(10.dp))
                IntimoOutlinedField(label = "Alergias o restricciones", value = allergies, onValueChange = { allergies = it })
            }
        }

        Spacer(Modifier.height(20.dp))

        IntimoPrimaryButton(
            text = "Guardar cambios",
            onClick = {
                viewModel.saveProfile(name, lastName, email, birthDate, favoriteDrink, allergies, gender)
            },
            enabled = !state.isSaving,
            loading = state.isSaving,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.logout(); onLogout() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = IntimoColors.Red),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(IntimoColors.Red.copy(alpha = 0.5f)))
        ) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar Sesión")
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "¿Olvidaste tu contraseña o tu cuenta no la tiene? Escríbenos a cafeintimo@gmail.com o acércate al café y te ayudamos a activarla.",
            style = MaterialTheme.typography.bodySmall,
            color = IntimoColors.SubtleText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Text(
            IntimoAppInfo.settingsFooter,
            style = MaterialTheme.typography.bodySmall,
            color = IntimoColors.SubtleText,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
