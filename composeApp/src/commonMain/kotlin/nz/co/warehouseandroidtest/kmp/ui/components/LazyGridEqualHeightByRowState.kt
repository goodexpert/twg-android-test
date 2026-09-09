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

@Stable
class LazyGridEqualHeightByRowState {
    val groupMaxHeightsPx = mutableStateMapOf<Int, Int>()
}

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

fun <T> buildSpan(
    items: List<T>,
    columns: Int,
    spanFor: (index: Int, item: T) -> Int,
): IntArray = IntArray(items.size) { i ->
    spanFor(i, items[i]).coerceIn(1, columns)
}

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
