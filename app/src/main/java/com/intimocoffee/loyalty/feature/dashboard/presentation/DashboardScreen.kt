package com.intimocoffee.loyalty.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.network.*
import com.intimocoffee.loyalty.ui.components.IntimoDashedAction
import com.intimocoffee.loyalty.ui.components.IntimoElevatedCard
import com.intimocoffee.loyalty.ui.components.IntimoGreetingRow
import com.intimocoffee.loyalty.ui.components.IntimoPromoCard
import com.intimocoffee.loyalty.ui.theme.IntimoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val customerName: String = "",
    val totalPoints: Int = 0,
    val lifetimePoints: Int = 0,
    val tier: String = "BRONZE",
    val totalVisits: Int = 0,
    val currentMonthVisits: Int = 0,
    val availableRewards: Int = 0,
    val promos: List<LoyaltyPromoResponse> = emptyList(),
    val recentTransactions: List<TransactionResponse> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val apiService: LoyaltyApiService,
    private val contabilidadApi: ContabilidadApiService,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState
    
    init { loadDashboard(isPullRefresh = false) }
    
    fun loadDashboard(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            val prev = _uiState.value
            if (isPullRefresh) {
                _uiState.value = prev.copy(isRefreshing = true, error = null)
            } else if (!prev.hasLoadedOnce) {
                _uiState.value = prev.copy(isLoading = true, error = null)
            } else {
                _uiState.value = prev.copy(isRefreshing = true, error = null)
            }
            try {
                val customerId = sessionDataStore.customerId.first() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "Sesión no válida. Vuelve a iniciar sesión."
                    )
                    return@launch
                }
                val sessionName = sessionDataStore.customerName.first().orEmpty()
                
                val customerResponse = apiService.getCustomer(customerId)
                val pointsResponse = apiService.getPoints(customerId)
                val recentTxResponse = apiService.getTransactions(customerId, limit = 40)
                val allTxResponse = apiService.getTransactions(customerId, limit = 500)
                val rewardsResponse = apiService.getRewards()

                fun fail(msg: String) {
                    // Keep previous points/data; do not zero out on failure
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = msg
                    )
                }

                if (!customerResponse.isSuccessful || customerResponse.body()?.success != true) {
                    fail(customerResponse.body()?.message ?: "No se pudo cargar el perfil")
                    return@launch
                }
                if (!pointsResponse.isSuccessful || pointsResponse.body()?.success != true) {
                    fail(pointsResponse.body()?.message ?: "No se pudieron cargar los puntos")
                    return@launch
                }
                if (!recentTxResponse.isSuccessful || recentTxResponse.body()?.success != true) {
                    fail(recentTxResponse.body()?.message ?: "No se pudieron cargar transacciones")
                    return@launch
                }
                if (!allTxResponse.isSuccessful || allTxResponse.body()?.success != true) {
                    fail(allTxResponse.body()?.message ?: "No se pudieron cargar transacciones")
                    return@launch
                }
                if (!rewardsResponse.isSuccessful || rewardsResponse.body()?.success != true) {
                    fail(rewardsResponse.body()?.message ?: "No se pudieron cargar recompensas")
                    return@launch
                }
                
                val customer = customerResponse.body()?.data
                val points = pointsResponse.body()?.data
                    ?: run {
                        fail("Respuesta de puntos inválida")
                        return@launch
                    }
                val recentTxRaw = recentTxResponse.body()?.data ?: emptyList()
                val earnVisits = recentTxRaw.filter { it.type == "EARN" }.take(8)
                val allTransactions = allTxResponse.body()?.data ?: emptyList()
                val rewards = rewardsResponse.body()?.data ?: emptyList()
                val currentPoints = points.totalPoints

                val promos = try {
                    val promosResponse = contabilidadApi.getLoyaltyPromos()
                    if (promosResponse.isSuccessful && promosResponse.body()?.success == true) {
                        promosResponse.body()?.data.orEmpty()
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
                
                // Canjeables: rewards y promos con puntos suficientes
                val redeemableRewardIds = (
                    rewards.filter { it.pointsCost > 0 && it.pointsCost <= currentPoints }.map { it.id } +
                        promos.filter {
                            it.isRedeemable && it.rewardId != null &&
                                (it.pointsCost <= 0 || it.pointsCost <= currentPoints)
                        }.mapNotNull { it.rewardId }
                ).distinct()
                val redeemable = redeemableRewardIds.size
                
                // Visitas: usar max entre totalVisits del servidor y transacciones EARN
                val earnCount = allTransactions.count { it.type == "EARN" }
                val visits = maxOf(customer?.totalVisits ?: 0, earnCount)
                
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    isRefreshing = false,
                    hasLoadedOnce = true,
                    customerName = sessionName.ifBlank { customer?.name.orEmpty() },
                    totalPoints = currentPoints,
                    lifetimePoints = points.lifetimePoints,
                    tier = points.tier,
                    totalVisits = visits,
                    currentMonthVisits = maxOf(customer?.currentMonthVisits ?: 0, earnCount),
                    availableRewards = redeemable,
                    promos = promos,
                    recentTransactions = earnVisits
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message
                )
            }
        }
    }

    fun redeemPromo(rewardId: Long) {
        viewModelScope.launch {
            try {
                val customerId = sessionDataStore.customerId.first() ?: return@launch
                val response = apiService.redeemReward(RedeemRequest(customerId, rewardId))
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        message = response.body()?.data ?: "¡Cupón listo! Muéstralo en caja."
                    )
                    loadDashboard(isPullRefresh = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        message = response.body()?.message ?: "No se pudo canjear"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "Error al canjear")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

private val visitDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "MX"))

private fun formatVisitDate(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        instant.atZone(ZoneId.systemDefault()).format(visitDateFormatter)
    } catch (_: Exception) {
        iso.take(10)
    }
}

private fun promoDisplayCta(promo: LoyaltyPromoResponse, currentPoints: Int): String {
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onOpenCanjeables: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.loadDashboard(isPullRefresh = true) }
    )

    state.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("Aviso") },
            text = { Text(msg) },
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

    Box(
        Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
        ) {
            item {
                IntimoGreetingRow(
                    name = state.customerName.ifBlank { "amigo" },
                    onProfileClick = onOpenSettings,
                )
            }

            item {
                IntimoElevatedCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${state.totalPoints}",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Puntos",
                                style = MaterialTheme.typography.titleMedium,
                                color = IntimoColors.SubtleText,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = IntimoColors.ChipBg,
                            modifier = Modifier.clickable(onClick = onOpenCanjeables),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Premios", fontWeight = FontWeight.SemiBold, color = IntimoColors.Espresso)
                                if (state.availableRewards > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = IntimoColors.Gold,
                                    ) {
                                        Text(
                                            "${state.availableRewards}",
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    TierProgressBar(tier = state.tier, lifetimePoints = state.lifetimePoints)
                    Spacer(Modifier.height(16.dp))
                    IntimoDashedAction(
                        text = "Detalles de recompensa",
                        onClick = onOpenCanjeables,
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.DirectionsWalk,
                        value = "${state.totalVisits}",
                        label = "Visitas",
                    )
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        value = "${state.currentMonthVisits}",
                        label = "Este mes",
                    )
                }
            }

            items(state.promos, key = { it.id }) { promo ->
                IntimoPromoCard(
                    title = promo.title,
                    subtitle = promo.subtitle,
                    cta = promoDisplayCta(promo, state.totalPoints),
                    imageUrl = promo.imageUrl.takeIf { it.isNotBlank() },
                    onClick = {
                        when {
                            promo.isRedeemable && promo.rewardId != null -> viewModel.redeemPromo(promo.rewardId)
                            promo.linkUrl.trim().isNotBlank() -> {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(promo.linkUrl.trim()))
                                )
                            }
                            else -> onOpenCanjeables()
                        }
                    },
                )
            }

            item {
                Text(
                    "Últimas visitas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    IntimoElevatedCard {
                        Text(
                            "Aún no hay visitas — tu primera taza suma puntos",
                            color = IntimoColors.SubtleText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            items(state.recentTransactions) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            formatVisitDate(tx.createdAt),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "+${tx.pointsEarned} pts",
                            color = IntimoColors.Green,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            state.error?.let { err ->
                item {
                    Text(
                        err,
                        style = MaterialTheme.typography.bodySmall,
                        color = IntimoColors.Red,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = IntimoColors.Espresso,
            backgroundColor = Color.White,
        )
    }
}

@Composable
private fun TierBadge(tier: String) {
    val (tierName, tierColor) = when (tier) {
        "GOLD" -> "Oro" to IntimoColors.Gold
        "SILVER" -> "Plata" to IntimoColors.Silver
        else -> "Bronce" to IntimoColors.Bronze
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = tierColor.copy(alpha = 0.2f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tierName, fontWeight = FontWeight.Bold, color = tierColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TierProgressBar(tier: String, lifetimePoints: Int) {
    val (nextTier, threshold, progress) = when (tier) {
        "GOLD" -> Triple("Máximo", lifetimePoints, 1f)
        "SILVER" -> Triple("Oro", 1000, (lifetimePoints / 1000f).coerceIn(0f, 1f))
        else -> Triple("Plata", 500, (lifetimePoints / 500f).coerceIn(0f, 1f))
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (tier == "GOLD") "Nivel máximo" else "Próximo: $nextTier",
                style = MaterialTheme.typography.bodySmall,
                color = IntimoColors.SubtleText
            )
            if (tier != "GOLD") {
                Text("$lifetimePoints/$threshold", style = MaterialTheme.typography.bodySmall, color = IntimoColors.SubtleText)
            }
        }
        Spacer(Modifier.height(4.dp))
        @Suppress("DEPRECATION")
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = IntimoColors.ProgressGold,
            trackColor = IntimoColors.ProgressTrack
        )
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = IntimoColors.Espresso, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp)
            Text(label, style = MaterialTheme.typography.bodySmall, color = IntimoColors.SubtleText)
        }
    }
}
