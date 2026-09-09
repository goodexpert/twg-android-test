package nz.co.warehouseandroidtest.kmp.feature.productlist

import com.russhwolf.settings.Settings
import nz.co.warehouseandroidtest.kmp.feature.productlist.ProductListPreferencesStore.Companion.DEFAULT_LAYOUT

/**
 * Remembers what the user picked for the product-list screen across launches.
 *
 * Right now that is just the layout (list vs grid). Sort and any filter memory land here as
 * well when they arrive — a screen has one preferences store rather than one per field so the
 * ViewModel takes one dependency and the settings file stays cohesive.
 *
 * The layout is stored as [ProductListLayout.name] rather than an ordinal, so reordering the
 * enum entries later cannot silently swap a saved value's meaning. A corrupt value (renamed
 * enum, hand-edited settings) falls back to [DEFAULT_LAYOUT] rather than failing the screen.
 *
 * Nothing here is a secret, so it uses ordinary settings rather than the secure store.
 */
class ProductListPreferencesStore(
    private val settings: Settings,
) {

    fun readLayout(): ProductListLayout {
        val raw = settings.getStringOrNull(KEY_LAYOUT) ?: return DEFAULT_LAYOUT
        return runCatching { ProductListLayout.valueOf(raw) }.getOrDefault(DEFAULT_LAYOUT)
    }

    fun writeLayout(layout: ProductListLayout) {
        settings.putString(KEY_LAYOUT, layout.name)
    }

    private companion object {
        const val KEY_LAYOUT = "layout"
        val DEFAULT_LAYOUT = ProductListLayout.List
    }
}
