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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.network.CustomerResponse
import com.intimocoffee.loyalty.core.network.LoyaltyApiService
import com.intimocoffee.loyalty.core.network.UpdateCustomerRequest
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
    val message: String? = null
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
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val customerId = sessionDataStore.customerId.first() ?: return@launch
                val response = apiService.getCustomer(customerId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isLoading = false, customer = response.body()?.data)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = "Error al cargar perfil")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, message = "Error: ${e.message}")
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
                val customerId = sessionDataStore.customerId.first() ?: return@launch
                val request = UpdateCustomerRequest(
                    name = name.ifBlank { null },
                    lastName = lastName.ifBlank { null },
                    email = email.ifBlank { null },
                    birthDate = birthDate.ifBlank { null },
                    favoriteDrink = favoriteDrink.ifBlank { null },
                    allergies = allergies.ifBlank { null },
                    gender = gender.ifBlank { null }
                )
                val response = apiService.updateCustomer(customerId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val updated = response.body()!!.data!!
                    sessionDataStore.saveSession(updated.id, updated.name, updated.phone)
                    _uiState.value = _uiState.value.copy(isSaving = false, customer = updated, message = "✓ Perfil actualizado")
                } else {
                    _uiState.value = _uiState.value.copy(isSaving = false, message = response.body()?.message ?: "Error")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, message = "Error: ${e.message}")
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
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() } }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            return@Scaffold
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text("Edita tu perfil de cliente", color = IntimoColors.SubtleText)
            
            Spacer(Modifier.height(20.dp))
            
            // Profile section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = IntimoColors.CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Información Personal", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    DarkTextField(value = name, onValueChange = { name = it }, label = "Nombre")
                    Spacer(Modifier.height(10.dp))
                    DarkTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellido")
                    Spacer(Modifier.height(10.dp))
                    DarkTextField(value = email, onValueChange = { email = it }, label = "Email")
                    Spacer(Modifier.height(10.dp))
                    DarkTextField(value = birthDate, onValueChange = { birthDate = it }, label = "Fecha de nacimiento (YYYY-MM-DD)")
                    Spacer(Modifier.height(10.dp))
                    
                    // Gender dropdown
                    var genderExpanded by remember { mutableStateOf(false) }
                    val genderOptions = listOf("" to "Sin especificar", "MALE" to "Masculino", "FEMALE" to "Femenino", "OTHER" to "Otro", "PREFER_NOT_SAY" to "Prefiero no decir")
                    val selectedGenderLabel = genderOptions.find { it.first == gender }?.second ?: "Sin especificar"
                    ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
                        OutlinedTextField(
                            value = selectedGenderLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Género", color = IntimoColors.SubtleText) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0xFF444444),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            genderOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { gender = value; genderExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Preferences section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = IntimoColors.CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Coffee, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Preferencias", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    DarkTextField(value = favoriteDrink, onValueChange = { favoriteDrink = it }, label = "Bebida favorita")
                    Spacer(Modifier.height(10.dp))
                    DarkTextField(value = allergies, onValueChange = { allergies = it }, label = "Alergias o restricciones")
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Save button
            Button(
                onClick = { viewModel.saveProfile(name, lastName, email, birthDate, favoriteDrink, allergies, gender) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Logout button
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
            
            Spacer(Modifier.height(24.dp))
            
            // App info
            Text(
                "Intimo Coffee Loyalty v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = IntimoColors.SubtleText,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun DarkTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = IntimoColors.SubtleText) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color(0xFF444444),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White
        )
    )
}
