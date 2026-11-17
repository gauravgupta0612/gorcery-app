package com.golagrocery.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.golagrocery.app.R
import com.golagrocery.app.ui.components.appContainer
import com.golagrocery.app.ui.screens.*
import com.golagrocery.app.ui.screens.auth.*
import com.golagrocery.app.ui.screens.delivery.*
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)
private val tabs = listOf(
    Tab(Routes.HOME, R.string.home, Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.CATEGORIES, R.string.categories, Icons.Outlined.GridView, Icons.Filled.GridView),
    Tab(Routes.CART, R.string.cart, Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart),
    Tab(Routes.ACCOUNT, R.string.account, Icons.Outlined.Person, Icons.Filled.Person),
)

/** Navigates to a top-level destination clearing everything above the start. */
fun NavHostController.goRoot(route: String) = navigate(route) {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
}

@Composable
fun GolaNavHost(openOrderId: Long?, onOrderOpened: () -> Unit) {
    val nav = rememberNavController()
    val container = appContainer()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val quote by container.cart.quote.collectAsState()
    val sessionExpiredMsg = stringResource(R.string.session_expired)

    LaunchedEffect(Unit) {
        container.sessionExpiredEvents.collect {
            container.cart.clearLocal()
            nav.goRoot(Routes.LOGIN)
            snackbar.showSnackbar(sessionExpiredMsg)
        }
    }
    LaunchedEffect(openOrderId, route) {
        if (openOrderId != null && route != null && route != Routes.SPLASH && container.session.session.value != null) {
            nav.navigate(Routes.order(openOrderId)); onOrderOpened()
        }
    }
    val showMessage: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(route in Routes.bottomBarRoutes, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                    tabs.forEach { t ->
                        val selected = route == t.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = {
                                val count = quote?.itemCount ?: 0
                                if (t.route == Routes.CART && count > 0) {
                                    BadgedBox(badge = { Badge(containerColor = GolaColors.Yellow, contentColor = GolaColors.GreenDark) { Text("$count") } }) {
                                        Icon(if (selected) t.selectedIcon else t.icon, null)
                                    }
                                } else Icon(if (selected) t.selectedIcon else t.icon, null)
                            },
                            label = { Text(stringResource(t.label)) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = GolaColors.GreenLight, selectedIconColor = GolaColors.Green, selectedTextColor = GolaColors.Green),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav, startDestination = Routes.SPLASH, modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 8 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { it / 8 } },
        ) {
            composable(Routes.SPLASH) { SplashScreen(nav) }
            composable(Routes.ONBOARDING) { OnboardingScreen(nav) }
            composable(Routes.LOGIN) { LoginScreen(nav, showMessage) }
            composable(Routes.REGISTER) { RegisterScreen(nav, showMessage) }
            composable(Routes.OTP, arguments = listOf(
                navArgument("phone") { type = NavType.StringType }, navArgument("purpose") { type = NavType.StringType },
                navArgument("dev") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("to") { type = NavType.StringType; nullable = true; defaultValue = null },
            )) { e ->
                OtpScreen(nav, e.arguments!!.getString("phone")!!, e.arguments!!.getString("purpose")!!,
                    e.arguments?.getString("dev")?.ifBlank { null }, e.arguments?.getString("to")?.ifBlank { null }, showMessage)
            }
            composable(Routes.LOCATION) { LocationScreen(nav, showMessage) }
            composable(Routes.HOME) { HomeScreen(nav, showMessage) }
            composable(Routes.CATEGORIES) { CategoriesScreen(nav) }
            composable(Routes.PRODUCTS, arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("q") { type = NavType.StringType; defaultValue = "" },
            )) { e ->
                val cid = e.arguments!!.getLong("categoryId").takeIf { it > 0 }
                ProductListScreen(nav, cid, e.arguments!!.getString("title").orEmpty(), e.arguments!!.getString("q").orEmpty(), false, showMessage)
            }
            composable(Routes.SEARCH) { ProductListScreen(nav, null, "", "", true, showMessage) }
            composable(Routes.PRODUCT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                ProductDetailScreen(nav, e.arguments!!.getLong("id"), showMessage)
            }
            composable(Routes.CART) { CartScreen(nav, showMessage) }
            composable(Routes.COUPONS) { CouponsScreen(nav, showMessage) }
            composable(Routes.CHECKOUT) { CheckoutScreen(nav, showMessage) }
            composable(Routes.PAYMENT, arguments = listOf(navArgument("orderId") { type = NavType.LongType },
                navArgument("method") { type = NavType.StringType; defaultValue = "UPI" })) { e ->
                PaymentScreen(nav, e.arguments!!.getLong("orderId"), e.arguments!!.getString("method") ?: "UPI")
            }
            composable(Routes.ORDER_PLACED, arguments = listOf(navArgument("orderId") { type = NavType.LongType })) { e ->
                OrderPlacedScreen(nav, e.arguments!!.getLong("orderId"))
            }
            composable(Routes.ORDERS) { OrdersScreen(nav) }
            composable(Routes.ORDER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                OrderDetailScreen(nav, e.arguments!!.getLong("id"), showMessage)
            }
            composable(Routes.ADDRESSES) { AddressesScreen(nav, showMessage) }
            composable(Routes.ADDRESS_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) { e ->
                AddressEditScreen(nav, e.arguments!!.getLong("id").takeIf { it > 0 }, showMessage)
            }
            composable(Routes.WISHLIST) { WishlistScreen(nav, showMessage) }
            composable(Routes.ACCOUNT) { AccountScreen(nav, showMessage) }
            composable(Routes.PROFILE) { EditProfileScreen(nav, showMessage) }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen(nav) }
            composable(Routes.SUPPORT) { SupportScreen(nav, showMessage) }
            composable(Routes.REVIEW, arguments = listOf(navArgument("orderId") { type = NavType.LongType })) { e ->
                ReviewScreen(nav, e.arguments!!.getLong("orderId"), showMessage)
            }
            composable(Routes.DELIVERY_HOME) { DeliveryHomeScreen(nav, showMessage) }
            composable(Routes.DELIVERY_ASSIGNMENT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                AssignmentScreen(nav, e.arguments!!.getLong("id"), showMessage)
            }
            composable(Routes.DELIVERY_HISTORY) { DeliveryHistoryScreen(nav) }
        }
    }
}
