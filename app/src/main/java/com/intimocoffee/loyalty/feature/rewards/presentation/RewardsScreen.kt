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
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.network.*
import com.intimocoffee.loyalty.ui.components.IntimoPromoCard
import com.intimocoffee.loyalty.ui.theme.IntimoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject

private data class CategoryInfo(val key: String, val label: String)

private val CATEGORIES = listOf(
    CategoryInfo("ALL", "Todos"),
    CategoryInfo("POINTS", "Puntos"),
    CategoryInfo("WELCOME", "Bienvenida"),
    CategoryInfo("VISIT_MILESTONE", "Visitas"),
    CategoryInfo("MONTHLY_VISITS", "Mensual"),
    CategoryInfo("BIRTHDAY", "Cumpleaños"),
    CategoryInfo("TIER_PERK", "Nivel"),
    CategoryInfo("SPENDING", "Gasto"),
    CategoryInfo("EVENT", "Eventos"),
)

data class RewardsUiState(
    val isLoading: Boolean = true,
    val rewards: List<RewardResponse> = emptyList(),
    val coupons: List<CouponResponse> = emptyList(),
    val promos: List<LoyaltyPromoResponse> = emptyList(),
    val currentPoints: Int = 0,
    val selectedCategory: String = "ALL",
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val apiService: LoyaltyApiService,
    private val contabilidadApi: ContabilidadApiService,
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

                val promos = try {
                    val promosResp = contabilidadApi.getLoyaltyPromos()
                    if (promosResp.isSuccessful && promosResp.body()?.success == true) {
                        promosResp.body()?.data.orEmpty()
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    rewards = rewardsResp.body()?.data ?: emptyList(),
                    coupons = couponsResp.body()?.data ?: emptyList(),
                    promos = promos,
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

    fun redeemPromo(rewardId: Long) = redeem(rewardId)

    fun redeemCouponCode(code: String) {
        viewModelScope.launch {
            try {
                val customerId = sessionDataStore.customerId.first() ?: return@launch
                val trimmed = code.trim()
                if (trimmed.isBlank()) {
                    _uiState.value = _uiState.value.copy(message = "Escribe un código")
                    return@launch
                }
                val response = apiService.redeemCouponCode(
                    PointCouponCodeRedeemRequest(customerId, trimmed)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        message = response.body()?.data ?: "¡Puntos agregados!"
                    )
                    loadRewards()
                } else {
                    _uiState.value = _uiState.value.copy(
                        message = response.body()?.message ?: "Código inválido"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "Error al canjear código")
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
    val context = LocalContext.current
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
    val promoRewardIds = state.promos
        .filter { it.isRedeemable && it.rewardId != null }
        .mapNotNull { it.rewardId }
        .toSet()
    val filteredRewards = filtered.filter { it.id !in promoRewardIds }
    // Cupones: siempre visibles arriba; el filtro de chips solo aplica a la lista de recompensas.
    val activeCoupons = state.coupons
    
    Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    "Premios",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${state.currentPoints} puntos disponibles",
                    style = MaterialTheme.typography.bodyLarge,
                    color = IntimoColors.SubtleText,
                )
            }

            if (!state.isLoading) {
                CouponCodeEntryCard(onRedeem = viewModel::redeemCouponCode)
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
                            cat.label,
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
                    CircularProgressIndicator(color = IntimoColors.Espresso)
                }
            } else if (filteredRewards.isEmpty() && activeCoupons.isEmpty() && state.promos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin recompensas en esta categoría", color = IntimoColors.SubtleText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (state.promos.isNotEmpty()) {
                        item {
                            Text(
                                "Promociones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        items(state.promos, key = { it.id }) { promo ->
                            IntimoPromoCard(
                                title = promo.title,
                                subtitle = promo.subtitle,
                                cta = rewardsPromoCta(promo, state.currentPoints),
                                imageUrl = promo.imageUrl.takeIf { it.isNotBlank() },
                                onClick = {
                                    when {
                                        promo.isRedeemable && promo.rewardId != null ->
                                            viewModel.redeemPromo(promo.rewardId)
                                        promo.linkUrl.trim().isNotBlank() ->
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(promo.linkUrl.trim()))
                                            )
                                    }
                                },
                            )
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }
                    item {
                        Text(
                            "Mis cupones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
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
                    if (filteredRewards.isNotEmpty()) {
                        item {
                            Text(
                                "Canjear con puntos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                    items(filteredRewards) { reward ->
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
            title = { Text("Confirmar canje") },
            text = {
                Text("¿Canjear ${reward.name} por ${reward.pointsCost} puntos?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.redeem(reward.id); showDialog = null }) {
                    Text("Canjear", color = IntimoColors.Espresso)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) {
                    Text("Cancelar", color = IntimoColors.SubtleText)
                }
            },
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
                Text(coupon.rewardName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
    val canRedeem = reward.pointsCost > 0 &&
        currentPoints >= reward.pointsCost &&
        reward.triggerType == "CLAIMABLE" &&
        reward.category != "WELCOME"
    val catLabel = CATEGORIES.find { it.key == reward.category }?.label ?: reward.category
    val initials = reward.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "R"
    val titleColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = IntimoColors.CardBackground),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(IntimoColors.ChipBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IntimoColors.Espresso)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reward.name,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                reward.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = IntimoColors.SubtleText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = IntimoColors.ChipBg,
                    ) {
                        Text(
                            catLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = IntimoColors.EspressoSoft,
                        )
                    }
                    reward.discountPercent?.let { pct ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IntimoColors.Green.copy(alpha = 0.12f),
                        ) {
                            Text(
                                "-$pct%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IntimoColors.Green,
                            )
                        }
                    }
                    reward.minTier?.let { tier ->
                        val tierColor = when (tier) {
                            "GOLD" -> IntimoColors.Gold
                            "SILVER" -> IntimoColors.Silver
                            else -> IntimoColors.Bronze
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tierColor.copy(alpha = 0.15f),
                        ) {
                            Text(
                                tier.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = tierColor,
                            )
                        }
                    }
                }
            }

            if (reward.pointsCost > 0) {
                Spacer(Modifier.width(12.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${reward.pointsCost}",
                            fontWeight = FontWeight.Bold,
                            color = IntimoColors.Espresso,
                            fontSize = 22.sp,
                            lineHeight = 24.sp,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Puntos",
                            color = IntimoColors.SubtleText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onRedeem,
                        enabled = canRedeem,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IntimoColors.Espresso,
                            contentColor = Color.White,
                            disabledContainerColor = IntimoColors.ChipBg,
                            disabledContentColor = IntimoColors.SubtleText,
                        ),
                    ) {
                        Text(
                            if (canRedeem) "Canjear" else "Faltan puntos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

private fun rewardsPromoCta(promo: LoyaltyPromoResponse, currentPoints: Int): String {
    if (promo.isRedeemable && promo.rewardId != null) {
        if (promo.pointsCost > 0) {
            return if (currentPoints >= promo.pointsCost) {
                "Canjear ${promo.pointsCost} pts"
            } else {
                "${promo.pointsCost} pts"
            }
        }
        return promo.ctaLabel.ifBlank { "Obtener cupón" }
    }
    return promo.ctaLabel.ifBlank { "Ver más" }
}

@Composable
private fun CouponCodeEntryCard(onRedeem: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Canjear código",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Si tienes un cupón de puntos, escríbelo aquí",
                style = MaterialTheme.typography.bodySmall,
                color = IntimoColors.SubtleText,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("CAFE100") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                )
                Button(
                    onClick = {
                        onRedeem(code)
                        code = ""
                    },
                    enabled = code.trim().isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IntimoColors.Espresso,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Aplicar")
                }
            }
        }
    }
}
