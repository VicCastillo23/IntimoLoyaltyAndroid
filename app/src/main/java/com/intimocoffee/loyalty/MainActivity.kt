package com.intimocoffee.loyalty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.navigation.BottomNavDestinations
import com.intimocoffee.loyalty.core.navigation.Destinations
import com.intimocoffee.loyalty.feature.auth.presentation.LoginScreen
import com.intimocoffee.loyalty.feature.auth.presentation.RegisterScreen
import com.intimocoffee.loyalty.feature.dashboard.presentation.DashboardScreen
import com.intimocoffee.loyalty.feature.history.presentation.HistoryScreen
import com.intimocoffee.loyalty.feature.qrcode.presentation.QRCodeScreen
import com.intimocoffee.loyalty.feature.rewards.presentation.RewardsScreen
import com.intimocoffee.loyalty.feature.settings.presentation.SettingsScreen
import com.intimocoffee.loyalty.ui.components.IntimoNavItem
import com.intimocoffee.loyalty.ui.components.IntimoWarmBackground
import com.intimocoffee.loyalty.ui.theme.IntimoCoffeeLoyaltyTheme
import com.intimocoffee.loyalty.ui.theme.IntimoColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionDataStore: SessionDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLoggedIn = runBlocking { sessionDataStore.isLoggedIn.first() }

        setContent {
            IntimoCoffeeLoyaltyTheme {
                var showSplash by remember { mutableStateOf(true) }
                val splashAlpha by animateFloatAsState(
                    targetValue = if (showSplash) 1f else 0f,
                    label = "splashAlpha",
                )

                LaunchedEffect(Unit) {
                    delay(1200)
                    showSplash = false
                }

                Box(Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        if (!showSplash) {
                            LoyaltyApp(startLoggedIn = isLoggedIn)
                        }
                    }

                    if (splashAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(splashAlpha)
                                .background(IntimoColors.Background),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.splash_logo),
                                contentDescription = "Íntimo Coffee",
                                modifier = Modifier.size(140.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoyaltyApp(startLoggedIn: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startLoggedIn) Destinations.MAIN else Destinations.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Destinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Destinations.MAIN) { popUpTo(Destinations.LOGIN) { inclusive = true } }
                },
                onNavigateToRegister = { navController.navigate(Destinations.REGISTER) },
            )
        }
        composable(Destinations.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Destinations.MAIN) { popUpTo(Destinations.LOGIN) { inclusive = true } }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Destinations.MAIN) {
            MainScreen(onLogout = {
                navController.navigate(Destinations.LOGIN) { popUpTo(Destinations.MAIN) { inclusive = true } }
            })
        }
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val innerNavController = rememberNavController()
    var rewardsPendingCategory by remember { mutableStateOf<String?>(null) }
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(BottomNavDestinations.DASHBOARD, "Inicio", Icons.Default.Home),
        BottomNavItem(BottomNavDestinations.REWARDS, "Premios", Icons.Default.CardGiftcard),
        BottomNavItem(BottomNavDestinations.QR_CODE, "QR", Icons.Default.QrCode2),
        BottomNavItem(BottomNavDestinations.HISTORY, "Historial", Icons.Default.History),
        BottomNavItem(BottomNavDestinations.SETTINGS, "Ajustes", Icons.Default.Settings),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (currentRoute == BottomNavDestinations.DASHBOARD) {
                ExtendedFloatingActionButton(
                    onClick = { navigateTab(innerNavController, BottomNavDestinations.QR_CODE) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    text = { Text("Escanear") },
                    containerColor = IntimoColors.ScanFab,
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        },
        bottomBar = {
            IntimoBottomBar(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onNavigate = { navigateTab(innerNavController, it) },
            )
        },
    ) { padding ->
        IntimoWarmBackground {
            NavHost(
                navController = innerNavController,
                startDestination = BottomNavDestinations.DASHBOARD,
                modifier = Modifier.padding(padding),
            ) {
                composable(BottomNavDestinations.DASHBOARD) {
                    DashboardScreen(
                        onOpenCanjeables = {
                            rewardsPendingCategory = "POINTS"
                            innerNavController.navigate(BottomNavDestinations.REWARDS) {
                                popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenSettings = {
                            innerNavController.navigate(BottomNavDestinations.SETTINGS) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(BottomNavDestinations.REWARDS) {
                    RewardsScreen(
                        pendingCategoryFromParent = rewardsPendingCategory,
                        onPendingCategoryConsumed = { rewardsPendingCategory = null },
                    )
                }
                composable(BottomNavDestinations.QR_CODE) { QRCodeScreen() }
                composable(BottomNavDestinations.HISTORY) { HistoryScreen() }
                composable(BottomNavDestinations.SETTINGS) { SettingsScreen(onLogout = onLogout) }
            }
        }
    }
}

@Composable
private fun IntimoBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Surface(
        color = IntimoColors.TabBar,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                IntimoNavItem(
                    label = item.label,
                    icon = item.icon,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                )
            }
        }
    }
}

private fun navigateTab(
    navController: androidx.navigation.NavHostController,
    route: String,
) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
