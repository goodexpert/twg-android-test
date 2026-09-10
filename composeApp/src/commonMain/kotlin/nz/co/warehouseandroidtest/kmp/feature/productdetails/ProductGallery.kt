package nz.co.warehouseandroidtest.kmp.feature.productdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import nz.co.warehouseandroidtest.kmp.ui.components.RingBadge

private val THUMBNAIL_SIZE = 64.dp

/**
 * The image gallery: one large image the user can swipe through, and a row of thumbnails
 * underneath that scrolls when there are more than fit.
 *
 * The two are driven by [selectedIndex] rather than by each other, so a swipe and a tap end up
 * in the same place. The pager reports its settled page back through [onImageSelected]; the
 * caller owns the selection.
 *
 * Renders nothing at all when the product has no images, rather than leaving a grey rectangle
 * where a picture would be.
 */
@Composable
fun ProductGallery(
    imageUrls: List<String>,
    selectedIndex: Int,
    onImageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    if (imageUrls.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceIn(imageUrls.indices),
        pageCount = { imageUrls.size },
    )
    val thumbnailState = rememberLazyListState()

    // Swipe -> selection. `settledPage` — not `currentPage` — because `currentPage` transitions
    // through every page the pager crosses, and an animateScrollToPage from a thumbnail tap
    // would echo each intermediate page back through `onImageSelected`, re-key the effect
    // below and cancel its own animation mid-flight — the "scroll starts, then stalls" bug
    // when the user taps thumbnails in quick succession. `settledPage` emits only once the
    // pager comes to rest, which is what a swipe report was meant to mean.
    LaunchedEffect(pagerState, imageUrls) {
        snapshotFlow { pagerState.settledPage }.collect(onImageSelected)
    }

    // Selection -> the two lists. Guarded on the current value, otherwise this and the effect
    // above would keep answering each other.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex !in imageUrls.indices) return@LaunchedEffect

        if (pagerState.currentPage != selectedIndex) {
            pagerState.animateScrollToPage(selectedIndex)
        }
        // Keeps the selected thumbnail on screen when the selection moved by a swipe.
        thumbnailState.animateScrollToItem(selectedIndex)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                // Only one image: nothing to swipe to, and a drag that goes nowhere reads as
                // the screen being broken.
                userScrollEnabled = imageUrls.size > 1,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            ) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }

            if (badge != null) {
                RingBadge(
                    text = badge,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }
        }

        // One image needs no picker.
        if (imageUrls.size > 1) {
            LazyRow(
                state = thumbnailState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(imageUrls) { index, url ->
                    Thumbnail(
                        url = url,
                        isSelected = index == selectedIndex,
                        onClick = { onImageSelected(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(url: String, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    // Selection is a heavier, darker outline — the same distinction the design draws.
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(shape)
            .background(Color.White)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(4.dp),
    )
}

