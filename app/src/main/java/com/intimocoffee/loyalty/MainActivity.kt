package com.intimocoffee.loyalty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                
                LaunchedEffect(Unit) {
                    delay(1500)
                    showSplash = false
                }
                
                if (showSplash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.splash_logo),
                            contentDescription = "Íntimo Coffee",
                            modifier = Modifier.size(300.dp)
                        )
                    }
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        LoyaltyApp(startLoggedIn = isLoggedIn)
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
                onNavigateToRegister = { navController.navigate(Destinations.REGISTER) }
            )
        }
        composable(Destinations.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Destinations.MAIN) { popUpTo(Destinations.LOGIN) { inclusive = true } }
                },
                onNavigateBack = { navController.popBackStack() }
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
    val bottomNavItems = listOf(
        BottomNavItem(BottomNavDestinations.DASHBOARD, "Inicio", Icons.Default.Home),
        BottomNavItem(BottomNavDestinations.REWARDS, "Premios", Icons.Default.CardGiftcard),
        BottomNavItem(BottomNavDestinations.QR_CODE, "QR", Icons.Default.QrCode2),
        BottomNavItem(BottomNavDestinations.HISTORY, "Historial", Icons.Default.History),
        BottomNavItem(BottomNavDestinations.SETTINGS, "Ajustes", Icons.Default.Settings),
    )
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = IntimoColors.TabBar,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            innerNavController.navigate(item.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.ui.graphics.Color.White,
                            selectedTextColor = androidx.compose.ui.graphics.Color.White,
                            unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF666666),
                            unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF666666),
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = innerNavController,
            startDestination = BottomNavDestinations.DASHBOARD,
            modifier = Modifier.padding(padding)
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
                    }
                )
            }
            composable(BottomNavDestinations.REWARDS) {
                RewardsScreen(
                    pendingCategoryFromParent = rewardsPendingCategory,
                    onPendingCategoryConsumed = { rewardsPendingCategory = null }
                )
            }
            composable(BottomNavDestinations.QR_CODE) { QRCodeScreen() }
            composable(BottomNavDestinations.HISTORY) { HistoryScreen() }
            composable(BottomNavDestinations.SETTINGS) { SettingsScreen(onLogout = onLogout) }
        }
    }
}
