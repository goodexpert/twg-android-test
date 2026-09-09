package nz.co.warehouseandroidtest.kmp.feature.productlist

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductListPreferencesStoreTest {

    @Test
    fun defaultsToListWhenNothingIsPersisted() {
        val store = ProductListPreferencesStore(MapSettings())

        assertEquals(ProductListLayout.List, store.readLayout())
    }

    @Test
    fun readsBackWhatItWrote() {
        val store = ProductListPreferencesStore(MapSettings())

        store.writeLayout(ProductListLayout.Grid)

        assertEquals(ProductListLayout.Grid, store.readLayout())
    }

    @Test
    fun survivesRebuildingTheStoreOnTheSameSettings() {
        // A restart is what motivates persistence at all — a new store instance reading the
        // same underlying settings must see the write.
        val settings = MapSettings()
        ProductListPreferencesStore(settings).writeLayout(ProductListLayout.Grid)

        val reopened = ProductListPreferencesStore(settings)

        assertEquals(ProductListLayout.Grid, reopened.readLayout())
    }

    @Test
    fun overwritesTheLastChoice() {
        val store = ProductListPreferencesStore(MapSettings())

        store.writeLayout(ProductListLayout.Grid)
        store.writeLayout(ProductListLayout.List)

        assertEquals(ProductListLayout.List, store.readLayout())
    }

    @Test
    fun fallsBackToTheDefaultWhenTheStoredValueIsGarbage() {
        // Renamed enum values or hand-edited settings must not fail the screen; readLayout
        // catches the parse and returns the default.
        val settings = MapSettings()
        settings.putString("layout", "Carousel") // not a value of ProductListLayout

        assertEquals(ProductListLayout.List, ProductListPreferencesStore(settings).readLayout())
    }
}
