package nz.co.warehouseandroidtest.kmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PromotedGrey = Color(0xFFE1E1E1)

/**
 * The product name, optionally prefixed by a "Promoted" tag. Shared by the list card and the
 * details screen so both surfaces present the name the same way.
 *
 * [minLines] and [maxLines] let a card reserve space for a two- or three-line title without
 * pushing the price row down when the real name happens to be shorter — the list uses this to
 * keep grid tiles the same height across a row.
 */
@Composable
internal fun ProductNameLine(
    name: String,
    isPromoted: Boolean,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 2,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        if (isPromoted) {
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
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            minLines = minLines,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
