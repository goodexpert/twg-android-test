package nz.co.warehouseandroidtest.kmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import nz.co.warehouseandroidtest.kmp.feature.home.HomeScreen
import nz.co.warehouseandroidtest.kmp.feature.productlist.ProductListScreen
import nz.co.warehouseandroidtest.kmp.feature.search.SearchScreen
import nz.co.warehouseandroidtest.kmp.ui.HomeRoute
import nz.co.warehouseandroidtest.kmp.ui.ProductListRoute
import nz.co.warehouseandroidtest.kmp.ui.SearchRoute

/**
 * Shared entry point, hosted by [nz.co.warehouseandroidtest.kmp.MainActivity] on Android and by
 * `MainViewController()` on iOS.
 *
 * Only the graph lives here. Each destination owns its own state, so this file does not grow
 * as screens are added.
 */
@Composable
fun App(container: AppContainer) {
    val navController = rememberNavController()

    MaterialTheme {
        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    container = container,
                    onOpenSearch = { navController.navigate(SearchRoute) },
                )
            }

            composable<SearchRoute> {
                SearchScreen(
                    container = container,
                    onOpenProductList = { navController.navigate(ProductListRoute(it)) },
                    onBack = navController::navigateUp,
                )
            }

            composable<ProductListRoute> { entry ->
                // Decoded from the route by the serializer, not parsed out of a path string.
                val route = entry.toRoute<ProductListRoute>()
                ProductListScreen(query = route.query, onBack = navController::navigateUp)
            }
        }
    }
}
