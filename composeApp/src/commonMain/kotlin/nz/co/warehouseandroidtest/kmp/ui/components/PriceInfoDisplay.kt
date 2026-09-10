package nz.co.warehouseandroidtest.kmp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Big dollars, small cents — the signature price treatment shared by the product list card and
 * the product details screen. [unitPrice] renders alongside when the caller has a per-unit line
 * to show; the details screen leaves it null.
 */
@Composable
fun PriceInfoDisplay(
    price: String,
    modifier: Modifier = Modifier,
    unitPrice: String? = null,
) {
    val (dollars, cents) = splitPrice(price)
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Text(
            text = dollars,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        if (cents.isNotEmpty()) {
            Text(
                text = cents,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (unitPrice != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = unitPrice,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                color = SubtleText,
                modifier = Modifier.align(Alignment.Bottom),
            )
        }
    }
}

/** "$4.95" -> ("$4", "95"); "$29" -> ("$29", ""). The caller renders the cents smaller. */
internal fun splitPrice(price: String): Pair<String, String> {
    val dot = price.indexOf('.')
    return if (dot < 0) price to "" else price.substring(0, dot) to price.substring(dot + 1)
}

private val SubtleText = Color(0xFF757575)
