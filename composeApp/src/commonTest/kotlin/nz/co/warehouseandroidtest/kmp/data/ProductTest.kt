package nz.co.warehouseandroidtest.kmp.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The computed properties are where the wire's awkwardness is absorbed — nested availability,
 * blank-means-absent strings, a price that is a number on one side and text on the other. They
 * are the only logic on this class, so they are what there is to test.
 */
class ProductTest {

    @Test
    fun formatsThePrice() {
        val product = Product(priceInfo = PriceInfo(price = 9.9))

        assertEquals("$9.90", product.formattedPrice)
    }

    @Test
    fun leavesTheFormattedPriceNullWhenThereIsNoPrice() {
        // priceInfo is optional on the wire, and "$null" on screen would be worse than nothing.
        assertNull(Product(priceInfo = null).formattedPrice)
        assertNull(Product(priceInfo = PriceInfo(price = null)).formattedPrice)
    }

    @Test
    fun readsAvailabilityOutOfTheInventory() {
        assertTrue(Product(inventory = Inventory(available = true)).isAvailable)
        assertFalse(Product(inventory = Inventory(available = false)).isAvailable)
    }

    @Test
    fun treatsAMissingInventoryAsUnavailable() {
        // Better to under-promise than to claim stock the response never mentioned.
        assertFalse(Product(inventory = null).isAvailable)
    }

    @Test
    fun dropsBlankImageUrls() {
        // The gallery indexes this list directly, so a blank would be a page showing nothing.
        val product = Product(
            imageUrls = listOf("https://example.test/1.jpg", "", "  ", "https://example.test/2.jpg")
        )

        assertEquals(
            listOf("https://example.test/1.jpg", "https://example.test/2.jpg"),
            product.images,
        )
    }

    @Test
    fun keepsImageOrder() {
        val urls = listOf("https://example.test/a.jpg", "https://example.test/b.jpg")

        assertEquals(urls, Product(imageUrls = urls).images)
    }

    @Test
    fun readsBlankTextFieldsAsAbsent() {
        val product = Product(
            brandDescription = "  ",
            productBarcode = "",
            productDescription = " ",
        )

        assertNull(product.brand)
        assertNull(product.barcode)
        assertNull(product.description)
    }

    @Test
    fun passesTextFieldsThroughWhenTheyHaveContent() {
        val product = Product(
            brandDescription = "Reflex",
            productBarcode = "9311995048689",
            productDescription = "A ream of paper.",
        )

        assertEquals("Reflex", product.brand)
        assertEquals("9311995048689", product.barcode)
        assertEquals("A ream of paper.", product.description)
    }
}
