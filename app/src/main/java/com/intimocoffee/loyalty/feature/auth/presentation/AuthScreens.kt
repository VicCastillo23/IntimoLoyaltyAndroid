package com.intimocoffee.loyalty.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.loyalty.ui.components.IntimoOutlinedField
import com.intimocoffee.loyalty.ui.theme.IntimoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var setPasswordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess()
    }

    LaunchedEffect(uiState.needsSetPassword) {
        if (uiState.needsSetPassword) {
            showSetPasswordDialog = true
            newPassword = ""
            confirmNewPassword = ""
            setPasswordError = null
        }
    }

    if (showSetPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showSetPasswordDialog = false
                viewModel.dismissSetPassword()
            },
            title = { Text("Crear contraseña") },
            text = {
                Column {
                    Text(
                        "Tu cuenta está sin contraseña. Elige una para poder iniciar sesión.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    IntimoOutlinedField(
                        label = "Nueva contraseña",
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        isPassword = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    IntimoOutlinedField(
                        label = "Confirmar contraseña",
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        isPassword = true,
                    )
                    setPasswordError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = IntimoColors.Red)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            newPassword.length < 6 -> setPasswordError = "Mínimo 6 caracteres"
                            newPassword != confirmNewPassword -> setPasswordError = "Las contraseñas no coinciden"
                            else -> {
                                setPasswordError = null
                                val p = uiState.setPasswordPhone ?: phone
                                viewModel.setPassword(p, newPassword)
                                showSetPasswordDialog = false
                            }
                        }
                    },
                    enabled = !uiState.isLoading
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSetPasswordDialog = false
                    viewModel.dismissSetPassword()
                }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.Coffee, null, modifier = Modifier.size(80.dp), tint = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("Intimo Coffee", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Programa de Lealtad", style = MaterialTheme.typography.titleMedium, color = IntimoColors.SubtleText)
        Spacer(Modifier.height(48.dp))

        IntimoOutlinedField(
            label = "Número de teléfono",
            value = phone,
            onValueChange = { phone = it },
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
        )
        Spacer(Modifier.height(12.dp))
        IntimoOutlinedField(
            label = "Contraseña",
            value = password,
            onValueChange = { password = it },
            isPassword = true,
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.login(phone, password) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = phone.trim().isNotBlank() && password.isNotBlank() && !uiState.isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes cuenta? Regístrate", color = IntimoColors.SubtleText)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "¿Olvidaste tu contraseña? Contáctanos en cafeintimo@gmail.com o en el café.",
            style = MaterialTheme.typography.bodySmall,
            color = IntimoColors.SubtleText,
            textAlign = TextAlign.Center
        )
        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = IntimoColors.Red, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IntimoOutlinedField(label = "Nombre completo", value = name, onValueChange = { name = it })
            Spacer(Modifier.height(12.dp))
            IntimoOutlinedField(
                label = "Teléfono",
                value = phone,
                onValueChange = { phone = it },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
            )
            Spacer(Modifier.height(12.dp))
            IntimoOutlinedField(
                label = "Email (opcional)",
                value = email,
                onValueChange = { email = it },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            )
            Spacer(Modifier.height(12.dp))
            IntimoOutlinedField(
                label = "Contraseña",
                value = password,
                onValueChange = { password = it; validationError = null },
                isPassword = true,
            )
            Spacer(Modifier.height(12.dp))
            IntimoOutlinedField(
                label = "Confirmar contraseña",
                value = confirmPassword,
                onValueChange = { confirmPassword = it; validationError = null },
                isPassword = true,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    validationError = when {
                        password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
                        password != confirmPassword -> "Las contraseñas no coinciden"
                        else -> null
                    }
                    if (validationError == null) {
                        viewModel.register(name, phone, password, email.ifBlank { null })
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = name.isNotBlank() && phone.isNotBlank() && password.isNotBlank() &&
                    confirmPassword.isNotBlank() && !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Registrarse", fontWeight = FontWeight.Bold)
                }
            }
            (validationError ?: uiState.errorMessage)?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, color = IntimoColors.Red, textAlign = TextAlign.Center)
            }
        }
    }
}
