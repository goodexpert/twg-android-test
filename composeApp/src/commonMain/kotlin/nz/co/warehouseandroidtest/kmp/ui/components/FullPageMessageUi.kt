package nz.co.warehouseandroidtest.kmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import twg_android_test.composeapp.generated.resources.Res
import twg_android_test.composeapp.generated.resources.twg_logo

/** Wide enough for two lines of body copy, narrow enough that it never spans a tablet. */
private val TEXT_MAX_WIDTH = 320.dp

/**
 * The asset is 250px wide, so this is about its natural size — big enough to read, small
 * enough that a raster logo is not scaled up into softness.
 */
private val LOGO_WIDTH = 250.dp

/**
 * A whole screen given over to one message: an illustration, a title, a line of explanation and
 * a single action. What the product details and product list screens show when a load fails,
 * and the shape any other empty or dead-end state should take.
 *
 * The action is a text button rather than a filled one: this is a way back from a dead end, not
 * the screen's primary call to action, and a filled button would compete with the content that
 * appears once the retry succeeds.
 *
 * The artwork is the TWG logo from `composeResources`, fixed here rather than passed in: every
 * use of this so far is a failed load, and one component drawing one thing is what keeps those
 * screens looking alike. Take it as a parameter when a caller genuinely needs different
 * artwork, not before.
 */
@Composable
fun FullPageMessageUi(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(Res.drawable.twg_logo),
            contentDescription = null,
            modifier = Modifier.requiredSize(LOGO_WIDTH)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = TEXT_MAX_WIDTH)
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
