package nz.co.warehouseandroidtest.kmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import nz.co.warehouseandroidtest.kmp.deeplink.parseDeepLink
import nz.co.warehouseandroidtest.kmp.feature.home.HomeScreen
import nz.co.warehouseandroidtest.kmp.feature.productdetails.ProductDetailsScreen
import nz.co.warehouseandroidtest.kmp.feature.productlist.ProductListScreen
import nz.co.warehouseandroidtest.kmp.feature.qrscanner.QrScannerScreen
import nz.co.warehouseandroidtest.kmp.feature.search.SearchScreen
import nz.co.warehouseandroidtest.kmp.ui.HomeRoute
import nz.co.warehouseandroidtest.kmp.ui.ProductDetailsRoute
import nz.co.warehouseandroidtest.kmp.ui.ProductListRoute
import nz.co.warehouseandroidtest.kmp.ui.QrScannerRoute
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
    // Coil's singleton ImageLoader has no HTTP fetcher on iOS without help. Registering
    // KtorNetworkFetcherFactory here wires the same Ktor stack the API calls use, so images
    // load on both platforms. setSingletonImageLoaderFactory only calls the factory the first
    // time an AsyncImage asks for the singleton, so nothing is built until it is needed.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    val navController = rememberNavController()

    // Drain platform-provided deep link URIs and push each parsed route onto the back stack.
    // Cold-start URIs pushed before this composable mounts are still delivered — the handler
    // buffers them in a channel and hands each out exactly once, so recompositions do not
    // re-dispatch. `parseDeepLink` is the single authority on which hosts are supported: the
    // Android intent filter only pins the scheme, so unknown hosts arrive here and are dropped.
    LaunchedEffect(Unit) {
        container.deepLinkHandler.uris.collect { uri ->
            parseDeepLink(uri)?.let { route -> navController.navigate(route) }
        }
    }

    // Coil's image loader, set up once for the whole app.
    //
    // Its own HttpClient on purpose. The one behind WarehouseApi puts the APIM base URL and the
    // subscription key on every request it makes; product images are served from
    // office-supplies.co.nz, so reusing it would send the key to a host that has no business
    // with it.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    MaterialTheme {
        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    container = container,
                    onOpenScanner = { navController.navigate(QrScannerRoute) },
                    onOpenSearch = { navController.navigate(SearchRoute) },
                )
            }

            composable<QrScannerRoute> {
                QrScannerScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = navController::navigateUp,
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
                ProductListScreen(
                    container = container,
                    query = route.query,
                    onOpenProductDetails = { navController.navigate(ProductDetailsRoute(it)) },
                    onBack = navController::navigateUp,
                )
            }

            composable<ProductDetailsRoute> { entry ->
                val route = entry.toRoute<ProductDetailsRoute>()
                ProductDetailsScreen(
                    container = container,
                    productId = route.productId,
                    onBack = navController::navigateUp,
                )
            }
        }
    }
}
