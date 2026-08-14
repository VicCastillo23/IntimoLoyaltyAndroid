package com.intimocoffee.loyalty.feature.rewards.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.network.*
import com.intimocoffee.loyalty.ui.theme.IntimoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject

private data class CategoryInfo(val key: String, val label: String, val icon: String)

private val CATEGORIES = listOf(
    CategoryInfo("ALL", "Todos", "🎯"),
    CategoryInfo("POINTS", "Puntos", "⭐"),
    CategoryInfo("WELCOME", "Bienvenida", "🎁"),
    CategoryInfo("VISIT_MILESTONE", "Visitas", "☕"),
    CategoryInfo("MONTHLY_VISITS", "Mensual", "📅"),
    CategoryInfo("BIRTHDAY", "Cumpleaños", "🎂"),
    CategoryInfo("TIER_PERK", "Nivel", "👑"),
    CategoryInfo("SPENDING", "Gasto", "💰"),
    CategoryInfo("EVENT", "Eventos", "🎪")
)

data class RewardsUiState(
    val isLoading: Boolean = true,
    val rewards: List<RewardResponse> = emptyList(),
    val coupons: List<CouponResponse> = emptyList(),
    val currentPoints: Int = 0,
    val selectedCategory: String = "ALL",
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val apiService: LoyaltyApiService,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState
    
    init { loadRewards() }
    
    fun loadRewards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val customerId = sessionDataStore.customerId.first() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Sesión no válida. Vuelve a iniciar sesión."
                    )
                    return@launch
                }
                val rewardsResp = apiService.getRewards()
                val pointsResp = apiService.getPoints(customerId)
                val couponsResp = apiService.getCoupons(customerId)

                if (!rewardsResp.isSuccessful || rewardsResp.body()?.success != true) {
                    throw IllegalStateException(rewardsResp.body()?.message ?: "No se pudieron cargar recompensas")
                }
                if (!pointsResp.isSuccessful || pointsResp.body()?.success != true) {
                    throw IllegalStateException(pointsResp.body()?.message ?: "No se pudieron cargar puntos")
                }
                if (!couponsResp.isSuccessful || couponsResp.body()?.success != true) {
                    throw IllegalStateException(couponsResp.body()?.message ?: "No se pudieron cargar cupones")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    rewards = rewardsResp.body()?.data ?: emptyList(),
                    coupons = couponsResp.body()?.data ?: emptyList(),
                    currentPoints = pointsResp.body()?.data?.totalPoints ?: 0
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
    
    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }
    
    fun redeem(rewardId: Long) {
        viewModelScope.launch {
            try {
                val customerId = sessionDataStore.customerId.first() ?: return@launch
                val response = apiService.redeemReward(RedeemRequest(customerId, rewardId))
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(message = response.body()?.data ?: "¡Canjeado!")
                    loadRewards()
                } else {
                    _uiState.value = _uiState.value.copy(message = response.body()?.message ?: "Error al canjear")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "Error al canjear")
            }
        }
    }
    
    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel = hiltViewModel(),
    pendingCategoryFromParent: String? = null,
    onPendingCategoryConsumed: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf<RewardResponse?>(null) }
    var selectedCoupon by remember { mutableStateOf<CouponResponse?>(null) }

    LaunchedEffect(pendingCategoryFromParent) {
        pendingCategoryFromParent?.let {
            viewModel.selectCategory(it)
            onPendingCategoryConsumed()
        }
    }
    
    val alertText = state.message ?: state.error
    if (alertText != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearMessage()
                viewModel.clearError()
            },
            title = { Text("Aviso") },
            text = { Text(alertText) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearMessage()
                    viewModel.clearError()
                }) { Text("OK") }
            }
        )
    }
    
    val filtered = if (state.selectedCategory == "ALL") state.rewards
                   else state.rewards.filter { it.category == state.selectedCategory }
    // Cupones: siempre visibles arriba; el filtro de chips solo aplica a la lista de recompensas.
    val activeCoupons = state.coupons
    
    Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    "Recompensas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${state.currentPoints} puntos disponibles",
                    style = MaterialTheme.typography.bodyLarge,
                    color = IntimoColors.SubtleText
                )
            }
            
            // Category filter chips
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.forEach { cat ->
                    val selected = state.selectedCategory == cat.key
                    Surface(
                        onClick = { viewModel.selectCategory(cat.key) },
                        shape = RoundedCornerShape(50),
                        color = if (selected) Color.White else IntimoColors.ChipBg,
                    ) {
                        Text(
                            "${cat.icon} ${cat.label}",
                            fontSize = 13.sp,
                            color = if (selected) Color.Black else Color(0xFFAAAAAA),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (filtered.isEmpty() && activeCoupons.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin recompensas en esta categoría", color = IntimoColors.SubtleText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Text(
                            "Mis cupones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    if (activeCoupons.isEmpty()) {
                        item {
                            Text(
                                "No tienes cupones activos. Los que ganes aparecerán aquí con un botón para ver el QR en caja.",
                                style = MaterialTheme.typography.bodySmall,
                                color = IntimoColors.SubtleText
                            )
                        }
                    } else {
                        items(activeCoupons) { coupon ->
                            CouponCard(coupon = coupon, onShowQr = { selectedCoupon = coupon })
                        }
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                    items(filtered) { reward ->
                        RewardCard(
                            reward = reward,
                            currentPoints = state.currentPoints,
                            onRedeem = { showDialog = reward }
                        )
                    }
                }
            }
        }

    showDialog?.let { reward ->
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("Confirmar Canje", color = Color.White) },
            text = {
                Text("¿Canjear ${reward.name} por ${reward.pointsCost} puntos?", color = Color.White)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.redeem(reward.id); showDialog = null }) {
                    Text("Canjear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) {
                    Text("Cancelar", color = IntimoColors.SubtleText)
                }
            },
            containerColor = Color(0xFF2A2A2A)
        )
    }

    selectedCoupon?.let { coupon ->
        CouponQrSheet(coupon = coupon, onDismiss = { selectedCoupon = null })
    }
}

@Composable
private fun CouponCard(coupon: CouponResponse, onShowQr: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = IntimoColors.CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🎟️ ${coupon.rewardName}", color = Color.White, fontWeight = FontWeight.Bold)
                coupon.discountPercent?.let {
                    Text("Descuento: $it%", color = IntimoColors.Green, style = MaterialTheme.typography.bodySmall)
                }
                coupon.couponCode?.let {
                    Text("Código: $it", color = IntimoColors.SubtleText, style = MaterialTheme.typography.bodySmall)
                }
                Text("QR listo para escanear en caja", color = IntimoColors.SubtleText, style = MaterialTheme.typography.bodySmall)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2E7D32).copy(alpha = 0.2f)
            ) {
                Text(
                    "ACTIVO",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = IntimoColors.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onShowQr,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text("Ver QR en caja", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CouponQrSheet(
    coupon: CouponResponse,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(coupon.qrData) { generateQrBitmap(coupon.qrData) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Cupón: ${coupon.rewardName}", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            qrBitmap?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR cupón",
                        modifier = Modifier
                            .size(220.dp)
                            .padding(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Muestra este QR en caja para aplicar tu cupón",
                style = MaterialTheme.typography.bodySmall,
                color = IntimoColors.SubtleText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss) { Text("Cerrar", color = Color.White) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun generateQrBitmap(data: String): Bitmap? {
    return try {
        val size = 512
        val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
        Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bitmap ->
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun RewardCard(reward: RewardResponse, currentPoints: Int, onRedeem: () -> Unit) {
    val canRedeem = currentPoints >= reward.pointsCost && reward.category == "POINTS"
    val emoji = reward.iconEmoji ?: CATEGORIES.find { it.key == reward.category }?.icon ?: "⭐"
    val catInfo = CATEGORIES.find { it.key == reward.category }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = IntimoColors.CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }
            
            Spacer(Modifier.width(14.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        reward.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (reward.discountPercent != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IntimoColors.Green.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "-${reward.discountPercent}%",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IntimoColors.Green
                            )
                        }
                    }
                }
                
                reward.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = IntimoColors.SubtleText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                
                Spacer(Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF333333)
                    ) {
                        Text(
                            catInfo?.label ?: reward.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = IntimoColors.SubtleText
                        )
                    }
                    
                    // Min tier badge
                    reward.minTier?.let { tier ->
                        val tierColor = when (tier) {
                            "GOLD" -> IntimoColors.Gold
                            "SILVER" -> IntimoColors.Silver
                            else -> IntimoColors.Bronze
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tierColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                tier.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = tierColor
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.width(10.dp))
            
            // Points + action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${reward.pointsCost}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text("pts", fontSize = 11.sp, color = IntimoColors.SubtleText)
                if (reward.category == "POINTS") {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onRedeem,
                        enabled = canRedeem,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF3B3B3B),
                            disabledContentColor = Color(0xFF666666)
                        )
                    ) {
                        Text(if (canRedeem) "Canjear" else "---", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
