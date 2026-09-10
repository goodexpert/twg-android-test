package nz.co.warehouseandroidtest.kmp.ui.components

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import kotlin.math.max

/**
 * Per-row max-height cache for [equalHeightByRow]: `rowId -> pixels`, where the value is the
 * tallest measurement any cell in that row has produced so far.
 *
 * A snapshot state map, so a measurement that pushes a row's max up recomposes the earlier,
 * shorter cells and lets them grow to match. Callers do not read this directly — hand it to
 * [equalHeightByRow] and let the modifier do the bookkeeping.
 */
@Stable
class LazyGridEqualHeightByRowState {
    val groupMaxHeightsPx = mutableStateMapOf<Int, Int>()
}

/**
 * Remembers a [LazyGridEqualHeightByRowState] and clears its cache when the previous
 * measurements would be wrong to reuse — the window size changed (rotation, split-screen), the
 * density or font scale changed (system settings), or [layoutKey] changed.
 *
 * [layoutKey] is the escape hatch for a page swap: pass the list the grid renders, so items
 * with different intrinsic heights do not inherit the previous page's row heights.
 */
@Composable
fun rememberEqualHeightByRowState(
    layoutKey: Any? = null,
): LazyGridEqualHeightByRowState {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val state = remember { LazyGridEqualHeightByRowState() }

    LaunchedEffect(
        density.density,
        density.fontScale,
        containerSize,
        layoutKey,
    ) {
        state.groupMaxHeightsPx.clear()
    }
    return state
}

/**
 * Forces this cell to match the tallest measurement seen for [rowId], via [state]. This is
 * what makes a two-column grid where one product name wraps to three lines and another to one
 * still draw with a straight bottom edge across the row — otherwise per-cell backgrounds and
 * dividers would zigzag.
 *
 * Measured in two passes: first with loose height constraints to learn the natural size, then
 * again at the row's running max if that number has grown. The tallest cell in a row settles
 * on the first frame; shorter cells can flicker once as they catch up in the recomposition
 * that follows the state map update.
 */
fun Modifier.equalHeightByRow(
    state: LazyGridEqualHeightByRowState,
    rowId: Int,
): Modifier = this.then(object : LayoutModifier {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val known = state.groupMaxHeightsPx.getOrPut(rowId) { 0 }
        val looseConstraints = Constraints(
            minWidth = constraints.minWidth,
            maxWidth = constraints.maxWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )
        val natural = measurable.measure(looseConstraints)

        val candidate = max(known, natural.height)
        if (candidate != known) state.groupMaxHeightsPx[rowId] = candidate

        val target = candidate.coerceIn(constraints.minHeight, constraints.maxHeight)
        val enforcedConstraints = constraints.copy(
            minHeight = target,
            maxHeight = target,
        )

        val finalPlaceable = if (natural.height == target) natural else measurable.measure(enforcedConstraints)

        return layout(finalPlaceable.width, finalPlaceable.height) {
            finalPlaceable.place(0, 0)
        }
    }
})

/**
 * Walks a [spans] array and returns a parallel array of which row each item lands on. A row
 * fills when its cell spans sum to [columns]; the next item starts the next row. Callers hand
 * the result to [equalizedGridItems], which passes each item's row id down to
 * [equalHeightByRow].
 */
fun computeRowIds(spans: IntArray, columns: Int): IntArray {
    val out = IntArray(spans.size)
    var rowId = 0
    var filled = 0
    for (i in spans.indices) {
        val s = spans[i].coerceIn(1, columns)
        out[i] = rowId
        filled += s
        if (filled >= columns) {
            rowId++
            filled = 0
        }
    }
    return out
}

/**
 * Builds a spans array by asking [spanFor] how many columns each item wants, clamped to
 * `1..columns` so a caller cannot ask for a cell wider than the grid or thinner than one
 * column. Pair with [computeRowIds] to get the row bookkeeping [equalizedGridItems] expects.
 */
fun <T> buildSpan(
    items: List<T>,
    columns: Int,
    spanFor: (index: Int, item: T) -> Int,
): IntArray = IntArray(items.size) { i ->
    spanFor(i, items[i]).coerceIn(1, columns)
}

/**
 * Emits [items] into a `LazyVerticalGrid` with the pre-computed [spans] and hands each item's
 * row id from [rowIds] down to [itemContent], so the caller can apply [equalHeightByRow] with
 * it.
 *
 * Splitting the spans/rowIds computation out of this call lets the caller `remember` it
 * against the item list and only rebuild when the list actually changes rather than on every
 * recomposition.
 */
fun <T> LazyGridScope.equalizedGridItems(
    items: List<T>,
    spans: IntArray,
    rowIds: IntArray,
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(index: Int, item: T, rowId: Int) -> Unit,
) {
    require(spans.size == items.size && rowIds.size == items.size) {
        "spans and rowIds must have the same size as items"
    }

    itemsIndexed(
        items = items,
        key = key,
        span = { index, _ -> GridItemSpan(spans[index]) },
    ) { index, item ->
        val rowId = rowIds[index]
        itemContent(index, item, rowId)
    }
}
