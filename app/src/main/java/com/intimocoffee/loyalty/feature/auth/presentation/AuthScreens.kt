package com.intimocoffee.loyalty.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.loyalty.R
import com.intimocoffee.loyalty.ui.components.IntimoGenderField
import com.intimocoffee.loyalty.ui.components.IntimoOutlinedField
import com.intimocoffee.loyalty.ui.components.IntimoPrimaryButton
import com.intimocoffee.loyalty.ui.components.IntimoWarmBackground
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
            containerColor = IntimoColors.CardBackground,
            title = { Text("Crear contraseña") },
            text = {
                Column {
                    Text(
                        "Tu cuenta está sin contraseña. Elige una para poder iniciar sesión.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IntimoColors.SubtleText,
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
                    enabled = !uiState.isLoading,
                ) { Text("Guardar", color = IntimoColors.Caramel) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSetPasswordDialog = false
                    viewModel.dismissSetPassword()
                }) { Text("Cancelar", color = IntimoColors.SubtleText) }
            },
        )
    }

    IntimoWarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.8f))
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "Íntimo Coffee",
                modifier = Modifier.size(120.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Íntimo Coffee",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Donde el café se vuelve ritual",
                style = MaterialTheme.typography.bodyMedium,
                color = IntimoColors.SubtleText,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Programa de lealtad",
                style = MaterialTheme.typography.labelLarge,
                color = IntimoColors.Caramel,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(40.dp))

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
            Spacer(Modifier.height(20.dp))

            IntimoPrimaryButton(
                text = "Iniciar sesión",
                onClick = { viewModel.login(phone, password) },
                enabled = phone.trim().isNotBlank() && password.isNotBlank(),
                loading = uiState.isLoading,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onNavigateToRegister) {
                Text("¿No tienes cuenta? Regístrate", color = IntimoColors.Espresso)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "¿Olvidaste tu contraseña? Escríbenos a cafeintimo@gmail.com o en el café.",
                style = MaterialTheme.typography.bodySmall,
                color = IntimoColors.SubtleText,
                textAlign = TextAlign.Center,
            )
            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, color = IntimoColors.Red, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.weight(1f))
        }
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
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Únete a Íntimo", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = IntimoColors.Espresso)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = IntimoColors.Background,
    ) { padding ->
        IntimoWarmBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Crea tu cuenta y empieza a sumar puntos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IntimoColors.SubtleText,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
                IntimoOutlinedField(label = "Nombre", value = name, onValueChange = { name = it })
                Spacer(Modifier.height(12.dp))
                IntimoOutlinedField(label = "Apellido", value = lastName, onValueChange = { lastName = it })
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
                    label = "Fecha de nacimiento (opcional)",
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    placeholder = "YYYY-MM-DD",
                )
                Spacer(Modifier.height(12.dp))
                IntimoGenderField(value = gender, onValueChange = { gender = it })
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
                Spacer(Modifier.height(20.dp))
                IntimoPrimaryButton(
                    text = "Crear cuenta",
                    onClick = {
                        validationError = when {
                            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
                            password != confirmPassword -> "Las contraseñas no coinciden"
                            birthDate.isNotBlank() && !birthDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) ->
                                "La fecha de nacimiento debe ser YYYY-MM-DD"
                            else -> null
                        }
                        if (validationError == null) {
                            viewModel.register(
                                name = name,
                                lastName = lastName,
                                phone = phone,
                                password = password,
                                email = email.ifBlank { null },
                                birthDate = birthDate.ifBlank { null },
                                gender = gender,
                            )
                        }
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank() && password.isNotBlank() &&
                        confirmPassword.isNotBlank(),
                    loading = uiState.isLoading,
                )
                (validationError ?: uiState.errorMessage)?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = IntimoColors.Red, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
