package com.intimocoffee.loyalty.feature.history.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.network.LoyaltyApiService
import com.intimocoffee.loyalty.core.network.TransactionResponse
import com.intimocoffee.loyalty.ui.components.IntimoEmptyState
import com.intimocoffee.loyalty.ui.components.IntimoScreenHeader
import com.intimocoffee.loyalty.ui.theme.IntimoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val apiService: LoyaltyApiService,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<TransactionResponse>>(emptyList())
    val transactions: StateFlow<List<TransactionResponse>> = _transactions

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init { loadAll(isPullRefresh = false) }

    fun loadAll(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true else _isLoading.value = true
            try {
                val customerId = sessionDataStore.customerId.first() ?: run {
                    _isLoading.value = false
                    _isRefreshing.value = false
                    return@launch
                }
                val response = apiService.getTransactions(customerId, limit = 100)
                _transactions.value = response.body()?.data ?: emptyList()
            } catch (_: Exception) { }
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }
}

private val txDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale("es", "MX"))

private fun formatTxDate(iso: String): String {
    return try {
        Instant.parse(iso).atZone(ZoneId.systemDefault()).format(txDateFormatter)
    } catch (_: Exception) {
        iso.take(16).replace("T", " ")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.loadAll(isPullRefresh = true) }
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        IntimoScreenHeader(
            title = "Historial",
            subtitle = "Compras, canjes y puntos",
        )
        Spacer(Modifier.height(16.dp))

        if (isLoading && !isRefreshing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IntimoColors.Caramel)
            }
        } else if (transactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
            ) {
                IntimoEmptyState(
                    emoji = "☕",
                    title = "Aún no hay movimientos",
                    subtitle = "Tu primera visita aparecerá aquí con los puntos que ganaste.",
                )
            }
        } else {
            Box(Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(transactions) { tx ->
                        val isEarn = tx.type == "EARN"
                        val title = if (isEarn) "Compra en Íntimo" else "Canje de premio"
                        val points = if (isEarn) tx.pointsEarned else tx.pointsRedeemed
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isEarn) {
                                            IntimoColors.Green.copy(alpha = 0.15f)
                                        } else {
                                            IntimoColors.Caramel.copy(alpha = 0.15f)
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (isEarn) Icons.Default.ShoppingBag else Icons.Default.CardGiftcard,
                                                contentDescription = null,
                                                tint = if (isEarn) IntimoColors.Green else IntimoColors.Caramel,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        formatTxDate(tx.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IntimoColors.SubtleText,
                                    )
                                }
                                Text(
                                    if (isEarn) "+$points" else "−$points",
                                    color = if (isEarn) IntimoColors.Green else IntimoColors.Caramel,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentColor = IntimoColors.Caramel,
                    backgroundColor = IntimoColors.CardBackground
                )
            }
        }
    }
}
