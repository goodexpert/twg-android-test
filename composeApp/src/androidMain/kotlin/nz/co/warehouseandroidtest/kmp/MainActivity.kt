package nz.co.warehouseandroidtest.kmp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Read from the Application rather than built here: a new container per Activity would
        // mean a new HttpClient on every configuration change.
        val container = (application as WarehouseApplication).container

        // Only forward the launching intent on a fresh start. On restore, Android replays the
        // same intent that opened us originally, which would double-dispatch the deep link.
        if (savedInstanceState == null) {
            forwardDeepLink(intent)
        }
        setContent { App(container) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTop launch mode delivers subsequent VIEW intents here instead of recreating.
        setIntent(intent)
        forwardDeepLink(intent)
    }

    private fun forwardDeepLink(intent: Intent) {
        val uri = intent.dataString ?: return
        val container = (application as WarehouseApplication).container
        container.deepLinkHandler.push(uri)
    }
}
