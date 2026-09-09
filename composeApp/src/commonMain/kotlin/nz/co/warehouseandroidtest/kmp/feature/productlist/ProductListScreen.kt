package nz.co.warehouseandroidtest.kmp.feature.productlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import nz.co.warehouseandroidtest.kmp.AppContainer
import nz.co.warehouseandroidtest.kmp.ui.components.ErrorContent
import nz.co.warehouseandroidtest.kmp.ui.components.buildSpan
import nz.co.warehouseandroidtest.kmp.ui.components.computeRowIds
import nz.co.warehouseandroidtest.kmp.ui.components.equalHeightByRow
import nz.co.warehouseandroidtest.kmp.ui.components.equalizedGridItems
import nz.co.warehouseandroidtest.kmp.ui.components.rememberEqualHeightByRowState
import org.jetbrains.compose.resources.stringResource
import twg_android_test.composeapp.generated.resources.Res
import twg_android_test.composeapp.generated.resources.search_title

/**
 * Two layouts, one state: [ProductListLayout] chooses between a full-width [LazyColumn] and a
 * two-column [LazyVerticalGrid]. Everything else — top bar, filter chips, banner — is shared.
 *
 * Colours are inlined for now because the app has no theme file yet; they collect at the top of
 * this file so a design token pass can lift them out without walking the composables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    container: AppContainer,
    query: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProductListViewModel = viewModel {
        ProductListViewModel(
            query = query,
            searchRepository = container.searchRepository,
            preferencesStore = container.productListPreferencesStore,
        )
    }
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.query,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TwgGreen,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* search — wire when search route lands here */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TwgGreen,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                ),
            )
        },
        containerColor = Color.White,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                // Full-page error surfaces before anything has arrived; chips and the count
                // row would just be empty scaffolding underneath it.
                state.error != null && state.items.isEmpty() -> ErrorContent(
                    error = state.error!!,
                    screenName = stringResource(Res.string.search_title),
                    onAction = { viewModel.onIntent(ProductListIntent.Refresh) },
                )

                // Initial load: draw a skeleton in the shape of the real layout so the eye
                // sees the layout it is about to get, not a lonely spinner in the middle of
                // the screen. Filter chips and the "N products" row are hidden here — a
                // "0 products" caption on a screen that has not fetched yet is misleading.
                state.isLoading && state.items.isEmpty() -> when (state.layout) {
                    ProductListLayout.List -> ProductListSkeleton()
                    ProductListLayout.Grid -> ProductGridSkeleton()
                }

                else -> {
                    FilterChipsRow()
                    ProductCountRow(
                        totalCount = state.totalCount,
                        layout = state.layout,
                        onLayoutChange = {
                            viewModel.onIntent(ProductListIntent.LayoutChanged(it))
                        },
                    )
                    HorizontalDivider(color = DividerGrey)

                    // Once items are in state a background refresh keeps them visible —
                    // swapping them out for a spinner would be worse UX than a brief
                    // momentary staleness. items.isNotEmpty() is true in both remaining
                    // branches, so isLoading and error here always mean the paging kind
                    // — see the contract for why the split lives on `items.isEmpty()`
                    // rather than a second flag.
                    when (state.layout) {
                        ProductListLayout.List -> ProductList(
                            items = state.items,
                            isLoadingMore = state.isLoading,
                            loadMoreError = state.error != null,
                            canLoadMore = state.canLoadMore,
                            onLoadMore = { viewModel.onIntent(ProductListIntent.LoadMore) },
                        )

                        ProductListLayout.Grid -> ProductGrid(
                            items = state.items,
                            isLoadingMore = state.isLoading,
                            loadMoreError = state.error != null,
                            canLoadMore = state.canLoadMore,
                            onLoadMore = { viewModel.onIntent(ProductListIntent.LoadMore) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TwgGreen)
    }
}

/**
 * Initial-load placeholders — the grey outline of what the real rows or tiles will look like.
 *
 * Static (no shimmer) on purpose: the point is to show the shape of the incoming content
 * without adding an animation loop the first render has to compete with. Item count is a
 * screen-full guess rather than measured; extras below the fold get replaced by real data
 * before the user scrolls to them.
 */
@Composable
private fun ProductListSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(SKELETON_LIST_ROW_COUNT) {
            ProductListItemSkeleton()
            HorizontalDivider(color = DividerGrey)
        }
    }
}

@Composable
private fun ProductListItemSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        // Matches the real thumbnail's 96.dp box so the layout doesn't jump when data lands.
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SkeletonGrey),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Two title lines — narrower second line so the eye reads "wrapping headline".
            SkeletonBar(widthFraction = 0.7f, height = 14.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonBar(widthFraction = 0.5f, height = 14.dp)
            Spacer(Modifier.height(14.dp))
            // Price
            SkeletonBar(widthFraction = 0.25f, height = 12.dp)
            Spacer(Modifier.height(16.dp))
            // Button-row placeholder
            SkeletonBar(widthFraction = 0.8f, height = 20.dp)
        }
    }
}

@Composable
private fun ProductGridSkeleton() {
    // LazyVerticalGrid rather than a plain Column so the two-column arithmetic and the cell
    // sizing come from the same layout the real grid uses — one place to keep in sync.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(count = SKELETON_GRID_CELL_COUNT, key = { "grid-skel-$it" }) {
            ProductGridItemSkeleton(modifier = Modifier.gridCellDividers())
        }
    }
}

@Composable
private fun ProductGridItemSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Centred square, roughly 70% of the tile width — matches the reference. The real
        // ProductGridItem draws a full-width image, so the tile height is a bit tighter here
        // than after data lands; that trade is worth the calmer, centred loading look.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(SkeletonGrey),
        )
        Spacer(Modifier.height(14.dp))
        SkeletonBar(widthFraction = 0.9f, height = 12.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonBar(widthFraction = 0.65f, height = 12.dp)
        Spacer(Modifier.height(14.dp))
        // Price
        SkeletonBar(widthFraction = 0.35f, height = 14.dp)
        Spacer(Modifier.height(18.dp))
        // Button-row placeholder
        SkeletonBar(widthFraction = 0.95f, height = 20.dp)
    }
}

@Composable
private fun SkeletonBar(
    widthFraction: Float,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(SkeletonGrey),
    )
}

/** Enough list rows to fill a phone screen while the first response is in flight. */
private const val SKELETON_LIST_ROW_COUNT = 6

/** Six cells = three grid rows at two columns wide — enough for a phone screen. */
private const val SKELETON_GRID_CELL_COUNT = 6

@Composable
private fun FilterChipsRow() {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            AssistChip(
                onClick = { /* open filter */ },
                label = { Text("Filter") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FilterDot),
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = Color.Black,
                    leadingIconContentColor = Color.Black,
                ),
            )
        }
    }
}

@Composable
private fun ProductCountRow(
    totalCount: Int,
    layout: ProductListLayout,
    onLayoutChange: (ProductListLayout) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$totalCount products",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )

        val next = if (layout == ProductListLayout.List) ProductListLayout.Grid
        else ProductListLayout.List
        IconButton(onClick = { onLayoutChange(next) }) {
            Icon(
                imageVector = if (layout == ProductListLayout.List) Icons.Default.GridView
                else Icons.AutoMirrored.Filled.ViewList,
                contentDescription = "Toggle layout",
                tint = TwgGreen,
            )
        }
    }
}

@Composable
private fun ProductList(
    items: List<ProductCardData>,
    isLoadingMore: Boolean,
    loadMoreError: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(items = items, key = { it.id }) { item ->
            ProductListItem(item = item)
            HorizontalDivider(color = DividerGrey)
        }
        pagingFooter(
            isLoadingMore = isLoadingMore,
            loadMoreError = loadMoreError,
            onRetry = onLoadMore,
        )
    }

    NearEndTrigger(
        totalItemsCount = { listState.layoutInfo.totalItemsCount },
        lastVisibleIndex = { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 },
        enabled = canLoadMore && !loadMoreError,
        onReached = onLoadMore,
    )
}

@Composable
private fun ProductGrid(
    items: List<ProductCardData>,
    isLoadingMore: Boolean,
    loadMoreError: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val equalHeightByRowState = rememberEqualHeightByRowState(layoutKey = items)
    val columnCount = 2
    val spans = remember(items, columnCount) {
        buildSpan(items, columnCount) { i, item -> 1 }
    }
    val rowIds = remember(spans, columnCount) { computeRowIds(spans, columnCount) }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columnCount),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        equalizedGridItems(
            items = items,
            spans = spans,
            rowIds = rowIds,
        ) { _, item, rowId ->
            ProductGridItem(
                item = item,
                modifier = Modifier
                    .gridCellDividers()
                    .equalHeightByRow(equalHeightByRowState, rowId),
            )
        }

       // Full-width so the spinner/retry sits on its own row rather than in one cell.
        if (isLoadingMore || loadMoreError) {
            item(key = "paging-footer", span = { GridItemSpan(maxLineSpan) }) {
                if (isLoadingMore) LoadingMoreFooter() else LoadMoreErrorFooter(onLoadMore)
            }
        }
    }

    NearEndTrigger(
        totalItemsCount = { gridState.layoutInfo.totalItemsCount },
        lastVisibleIndex = { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 },
        enabled = canLoadMore && !loadMoreError,
        onReached = onLoadMore,
    )
}

private fun LazyListScope.pagingFooter(
    isLoadingMore: Boolean,
    loadMoreError: Boolean,
    onRetry: () -> Unit,
) {
    if (!isLoadingMore && !loadMoreError) return
    item(key = "paging-footer") {
        if (isLoadingMore) LoadingMoreFooter() else LoadMoreErrorFooter(onRetry)
    }
}

/**
 * Fires [onReached] when the last-visible index reaches within [PAGE_PREFETCH_THRESHOLD] of
 * the end. `distinctUntilChanged` + `filter { it }` means one dispatch per crossing rather
 * than one per pixel; the ViewModel's own guard is the safety net for the rest.
 *
 * The two lambdas are read inside `snapshotFlow`, which is what makes state changes to the
 * underlying [androidx.compose.foundation.lazy.LazyListState] or
 * [androidx.compose.foundation.lazy.grid.LazyGridState] observable here without picking one
 * type over the other.
 */
@Composable
private fun NearEndTrigger(
    totalItemsCount: () -> Int,
    lastVisibleIndex: () -> Int,
    enabled: Boolean,
    onReached: () -> Unit,
) {
    // `enabled` is a key so the effect restarts when the composable is allowed to fire again
    // — e.g. after `loadMoreError` clears via a retry.
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow {
            val total = totalItemsCount()
            total > 0 && lastVisibleIndex() >= total - PAGE_PREFETCH_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onReached() }
    }
}

@Composable
private fun LoadingMoreFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TwgGreen)
    }
}

@Composable
private fun LoadMoreErrorFooter(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Couldn't load more.", color = SubtleText)
        TextButton(onClick = onRetry) {
            Text(text = "Retry", color = TwgGreen, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * How many rows away from the end the paging trigger fires. Three keeps the next page
 * arriving before the user actually hits the last row while the grid layout does not
 * eagerly over-fetch on the very first render.
 */
private const val PAGE_PREFETCH_THRESHOLD = 3

@Composable
private fun ProductListItem(item: ProductCardData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        ProductThumbnail(
            item = item,
            modifier = Modifier.size(width = 96.dp, height = 96.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (item.savingLabel != null) {
                SavingTag(text = item.savingLabel)
                Spacer(Modifier.height(6.dp))
            }
            ProductNameLine(item = item)
            Spacer(Modifier.height(4.dp))
            PriceInfoDisplay(price = item.price, unitPrice = item.unitPrice)
            if (item.wasPrice != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.wasPrice,
                    color = WasRed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewButton(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductGridItem(
    item: ProductCardData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (item.savingLabel != null) {
            SavingTag(text = item.savingLabel)
            Spacer(Modifier.height(6.dp))
        }
        ProductThumbnail(
            item = item,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Spacer(Modifier.height(8.dp))
        ProductNameLine(item = item, minLines = 2, maxLines = 3)
        Spacer(Modifier.height(6.dp))
        PriceInfoDisplay(price = item.price)

        if (item.unitPrice != null) {
            Text(
                text = item.unitPrice,
                color = SubtleText,
                fontSize = 12.sp,
            )
        }
        if (item.wasPrice != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.wasPrice,
                color = WasRed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        ViewButton(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProductThumbnail(item: ProductCardData, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // The [ImagePlaceholder] background stays on both fronts: it fills the box while Coil
        // is fetching, and it fills the box when [ProductCardData.imageUrl] is null or the
        // request fails — AsyncImage draws over the top only once the bitmap is ready, so a
        // slow or missing image never leaves an empty gap.
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            // Fit rather than Crop: product photos in the feed are already framed for a square
            // tile, and Fit keeps the whole thing on screen if the aspect ratio drifts.
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(ImagePlaceholder),
        )

        val badgeLabel = when {
            item.discountLabel != null -> item.discountLabel
            item.isSpecial -> "Special"
            else -> null
        }
        if (badgeLabel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(width = 2.dp, color = TwgGreen, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badgeLabel,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ProductNameLine(
    item: ProductCardData,
    minLines: Int = 1,
    maxLines: Int = 2,
) {
    Row(verticalAlignment = Alignment.Top) {
        if (item.isPromoted) {
            Text(
                text = "Promoted",
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PromotedGrey)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            minLines = minLines,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SavingTag(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SavingsYellow)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun PriceInfoDisplay(
    price: String,
    unitPrice: String? = null,
    modifier: Modifier = Modifier,
) {
    val (dollars, cents) = splitPrice(price)
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Text(
            text = dollars,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
        )
        if (cents.isNotEmpty()) {
            Text(
                text = cents,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
            )
        }
        if (unitPrice != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = unitPrice,
                color = SubtleText,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Bottom),
            )
        }
    }
}

/** "$4.95" -> ("$4", "95"); "$29" -> ("$29", ""). Superscript cents rendered by the caller. */
private fun splitPrice(price: String): Pair<String, String> {
    val dot = price.indexOf('.')
    return if (dot < 0) price to "" else price.substring(0, dot) to price.substring(dot + 1)
}

@Composable
private fun ViewButton(modifier: Modifier = Modifier) {
    Button(
        onClick = { /* add to cart */ },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = TwgGreen,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text("View Details", fontWeight = FontWeight.Bold)
    }
}

/**
 * Draws thin lines on the right and bottom edges of a grid cell — together they make the
 * vertical column divider and the horizontal row divider that separate tiles.
 *
 * LazyVerticalGrid has no built-in cell divider, and inserting `HorizontalDivider` items
 * between rows only handles one direction. Per-cell right + bottom borders give both directions
 * with one modifier. The right border on cells in the last column falls at the pane's edge and
 * is invisible; the bottom border on the last row draws a footer line for the loaded set,
 * which reads as a natural end for the grid.
 *
 * Colour and thickness match the list's [HorizontalDivider] so the two layouts feel like the
 * same grid grammar.
 */
private fun Modifier.gridCellDividers(
    color: Color = DividerGrey,
    thickness: Dp = 1.dp,
): Modifier = this.drawBehind {
    val stroke = thickness.toPx()
    // Right edge — inset by half the stroke width so the line paints fully inside the cell
    // rather than being clipped by the next cell's bounds.
    drawLine(
        color = color,
        start = Offset(size.width - stroke / 2f, 0f),
        end = Offset(size.width - stroke / 2f, size.height),
        strokeWidth = stroke,
    )
    // Bottom edge.
    drawLine(
        color = color,
        start = Offset(0f, size.height - stroke / 2f),
        end = Offset(size.width, size.height - stroke / 2f),
        strokeWidth = stroke,
    )
}

private val TwgGreen = Color(0xFF008036)
private val FilterDot = Color(0xFFD91E7A)
private val SavingsYellow = Color(0xFFFFDA47)
private val WasRed = Color(0xFFD1231A)
private val PromotedGrey = Color(0xFFE1E1E1)
private val DividerGrey = Color(0xFFEAEAEA)
private val ImagePlaceholder = Color(0xFFF3F3F3)
private val SkeletonGrey = Color(0xFFEDEDED)
private val SubtleText = Color(0xFF757575)
