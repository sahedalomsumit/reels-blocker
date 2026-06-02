package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.example.util.isAccessibilityServiceEnabled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.auth.GoogleAuthManager
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.repository.BlockerRepository
import com.example.ui.BlockerViewModel
import com.example.ui.BlockerViewModelFactory
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlatformsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme

// Standard String Routes
const val ROUTE_LOGIN = "login_screen"
const val ROUTE_ONBOARDING = "onboarding_screen"
const val ROUTE_DASHBOARD = "dashboard_screen"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local persistence database & repository singletons
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = BlockerRepository(db.blockerDao())

        lifecycleScope.launch {
            repository.ensureSettingsExist()
        }

        val viewModel = ViewModelProvider(
            this,
            BlockerViewModelFactory(repository)
        )[BlockerViewModel::class.java]

        val googleAuthManager = GoogleAuthManager(this)

        setContent {
            val settings by viewModel.userSettings.collectAsState()
            val navController = rememberNavController()

            MyApplicationTheme(
                darkTheme = settings.theme == "dark"
            ) {
                fun navigateForSession(force: Boolean = false) {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (!settings.isLoggedIn) {
                        if (!force && currentRoute == ROUTE_LOGIN) return
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                        return
                    }
                    val hasPermission = isAccessibilityServiceEnabled(this@MainActivity)
                    val destination = if (hasPermission) ROUTE_DASHBOARD else ROUTE_ONBOARDING
                    if (!force && currentRoute == destination) return
                    navController.navigate(destination) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }

                // Route on login state changes
                LaunchedEffect(settings.isLoggedIn) {
                    try {
                        navigateForSession(force = true)
                    } catch (e: IllegalStateException) {
                        // NavHost may not be ready on first frame
                    }
                }

                // Re-route when returning from system settings (e.g. after enabling accessibility)
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, settings.isLoggedIn) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            try {
                                navigateForSession()
                            } catch (e: IllegalStateException) {
                                // Avoid crash if navigation runs before NavHost is ready
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (settings.theme == "dark") Color(0xFF050505) else Color(0xFFF5F3FF)),
                    contentWindowInsets = WindowInsets(0.dp) // Edge-to-edge transparent padding
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_LOGIN,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(ROUTE_LOGIN) {
                            var isSigningIn by remember { mutableStateOf(false) }
                            var signInError by remember { mutableStateOf<String?>(null) }
                            val signInScope = rememberCoroutineScope()

                            LoginScreen(
                                isSigningIn = isSigningIn,
                                signInError = signInError,
                                onGoogleSignIn = {
                                    signInScope.launch {
                                        isSigningIn = true
                                        signInError = null
                                        googleAuthManager.signIn(filterAuthorizedAccounts = false)
                                            .onSuccess { user ->
                                                viewModel.signIn(
                                                    user.displayName,
                                                    user.email,
                                                    user.photoUrl,
                                                )
                                            }
                                            .onFailure { error ->
                                                if (error !is GetCredentialCancellationException) {
                                                    signInError = error.message
                                                        ?: "Google sign-in failed. Try again."
                                                }
                                            }
                                        isSigningIn = false
                                    }
                                },
                            )
                        }

                        composable(ROUTE_ONBOARDING) {
                            OnboardingScreen(
                                onFinished = {
                                    viewModel.setBlockerEnabled(true)
                                    navController.navigate(ROUTE_DASHBOARD) {
                                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(ROUTE_DASHBOARD) {
                            val logoutScope = rememberCoroutineScope()
                            DashboardShell(viewModel = viewModel) {
                                logoutScope.launch {
                                    googleAuthManager.signOut()
                                }
                                viewModel.signOut()
                                navController.navigate(ROUTE_LOGIN) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Nested Navigation Bottom scaffold representing shell route with bottom bar
enum class DashboardTab {
    HOME, PLATFORMS, STATS, SETTINGS
}

@Composable
fun DashboardShell(
    viewModel: BlockerViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.HOME) }
    val settings by viewModel.userSettings.collectAsState()
    val isDark = settings.theme == "dark"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding() // Respect device gesture visual lines
                    .testTag("bottom_nav_bar"),
                containerColor = if (isDark) Color(0xFF0F0E17) else Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.HOME,
                    onClick = { selectedTab = DashboardTab.HOME },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DashboardTab.HOME) Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Home index"
                        )
                    },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        unselectedIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        selectedTextColor = Color(0xFF8B5CF6),
                        unselectedTextColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        indicatorColor = (if (isDark) Color(0x338B5CF6) else Color(0x198B5CF6))
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                NavigationBarItem(
                    selected = selectedTab == DashboardTab.PLATFORMS,
                    onClick = { selectedTab = DashboardTab.PLATFORMS },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DashboardTab.PLATFORMS) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Outlined.List,
                            contentDescription = "Platforms screen"
                        )
                    },
                    label = { Text("Platforms", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        unselectedIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        selectedTextColor = Color(0xFF8B5CF6),
                        unselectedTextColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        indicatorColor = (if (isDark) Color(0x338B5CF6) else Color(0x198B5CF6))
                    ),
                    modifier = Modifier.testTag("nav_tab_platforms")
                )

                NavigationBarItem(
                    selected = selectedTab == DashboardTab.STATS,
                    onClick = { selectedTab = DashboardTab.STATS },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DashboardTab.STATS) Icons.Default.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Addiction stats"
                        )
                    },
                    label = { Text("Stats", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        unselectedIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        selectedTextColor = Color(0xFF8B5CF6),
                        unselectedTextColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        indicatorColor = (if (isDark) Color(0x338B5CF6) else Color(0x198B5CF6))
                    ),
                    modifier = Modifier.testTag("nav_tab_stats")
                )

                NavigationBarItem(
                    selected = selectedTab == DashboardTab.SETTINGS,
                    onClick = { selectedTab = DashboardTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DashboardTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "Preferences settings"
                        )
                    },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        unselectedIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        selectedTextColor = Color(0xFF8B5CF6),
                        unselectedTextColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        indicatorColor = (if (isDark) Color(0x338B5CF6) else Color(0x198B5CF6))
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF050505) else Color(0xFFF5F3FF))
                .statusBarsPadding() // Avoid notch overlap on screen top
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                DashboardTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToPlatforms = { selectedTab = DashboardTab.PLATFORMS },
                    onNavigateToSettings = { selectedTab = DashboardTab.SETTINGS }
                )
                DashboardTab.PLATFORMS -> PlatformsScreen(viewModel = viewModel)
                DashboardTab.STATS -> StatsScreen(viewModel = viewModel)
                DashboardTab.SETTINGS -> SettingsScreen(viewModel = viewModel, onLogout = onLogout)
            }
        }
    }
}
