package nz.co.warehouseandroidtest.kmp.feature.productdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import nz.co.warehouseandroidtest.kmp.AppContainer
import nz.co.warehouseandroidtest.kmp.ui.components.ErrorContent
import nz.co.warehouseandroidtest.kmp.ui.components.ExpandableCard
import nz.co.warehouseandroidtest.kmp.ui.components.PriceInfoDisplay
import nz.co.warehouseandroidtest.kmp.ui.components.ProductNameLine
import org.jetbrains.compose.resources.stringResource
import twg_android_test.composeapp.generated.resources.Res
import twg_android_test.composeapp.generated.resources.product_descriptions_title
import twg_android_test.composeapp.generated.resources.product_details_title
import twg_android_test.composeapp.generated.resources.product_features_title

/**
 * Names this screen in the failure copy: "Unable to load Product Details due to a connection
 * error". Replaces the legacy Toast, which said "Get product detail failed!" over an empty
 * layout and offered no way to retry.
 */
private const val SCREEN_NAME = "Product Details"

/**
 * Ports `nz.co.warehouseandroidtest.ProductDetailActivity`. The single Glide image that screen
 * loaded is now [ProductGallery], because the endpoint returns a list: the legacy screen showed
 * the first URL and dropped the rest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    container: AppContainer,
    productId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProductDetailsViewModel = viewModel {
        ProductDetailsViewModel(productId, container.productRepository)
    }
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.product_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // No padding of its own: the gallery runs edge to edge, as it does in the design, and
        // the text below sets its own margin.
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            // Read once into locals: `state` is a delegated property, so the compiler cannot
            // smart-cast a null check on it.
            val product = state.product
            val error = state.error
            when {
                // The spinner only owns the screen while there is nothing to show; a retry over
                // loaded content leaves the content up.
                state.isLoading && product == null -> CircularProgressIndicator()

                product != null -> ProductDetailsUIContent(
                    product = product,
                    selectedImageIndex = state.selectedImageIndex,
                    onImageSelected = {
                        viewModel.onIntent(ProductDetailsIntent.ImageSelected(it))
                    },
                )

                error != null -> ErrorContent(
                    error = error,
                    screenName = SCREEN_NAME,
                    onAction = { viewModel.onIntent(ProductDetailsIntent.Retry) },
                )
            }
        }
    }
}

/**
 * The success branch of [ProductDetailsScreen]: gallery on top, the text block underneath.
 * Split out from the top-level composable so the loading and error branches can share the same
 * centring `Box` without dragging this content's helpers with them.
 */
@Composable
private fun ProductDetailsUIContent(
    product: ProductDetailsData,
    selectedImageIndex: Int,
    onImageSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProductGallery(
            imageUrls = product.imageUrls,
            selectedIndex = selectedImageIndex,
            onImageSelected = onImageSelected,
            badge = product.badge,
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            product.brand?.let {
                Text(text = it, style = MaterialTheme.typography.labelLarge)
            }
            ProductNameLine(
                name = product.name,
                isPromoted = false,
            )
            product.price?.let {
                PriceInfoDisplay(price = it)
            }

            Text(
                text = if (product.isAvailable) "In stock" else "Out of stock",
                style = MaterialTheme.typography.bodyMedium,
            )
            product.barcode?.let {
                Text(text = "Barcode $it", style = MaterialTheme.typography.bodySmall)
            }
            product.description?.let {
                DescriptionAccordion(
                    title = stringResource(Res.string.product_descriptions_title),
                    description = it,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            FeaturesAccordion(
                title = stringResource(Res.string.product_features_title),
                features = product.features,
            )
        }
    }
}

/**
 * The "Product Descriptions" accordion — one paragraph body wrapped in an [ExpandableCard]. A
 * sibling of [FeaturesAccordion]; they render different data shapes with the same accordion
 * chrome, which is why the two are not one function taking a `List<String>`.
 */
@Composable
private fun DescriptionAccordion(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    ExpandableCard(
        title = title,
        modifier = modifier,
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The "Product Features" accordion — one bullet per entry inside an [ExpandableCard]. See
 * [DescriptionAccordion] for the sister overload; this one exists so an empty list still
 * renders the header without an empty paragraph underneath.
 */
@Composable
private fun FeaturesAccordion(
    title: String,
    features: List<String>,
    modifier: Modifier = Modifier,
) {
    ExpandableCard(
        title = title,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            features.forEach { feature ->
                Text(
                    text = "• $feature",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}
