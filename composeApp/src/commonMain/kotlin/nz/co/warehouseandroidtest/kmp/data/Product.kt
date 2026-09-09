package nz.co.warehouseandroidtest.kmp.data

import kotlin.math.abs
import kotlin.math.round
import kotlinx.serialization.Serializable

/**
 * One product, as `twgCSharpTest/Product.json` returns it inside [ProductResponse].
 *
 * Parsed straight from the wire — there is no second type and no mapping step. What the wire
 * leaves awkward is handled by the computed properties at the bottom rather than by a copy of
 * this class: a price is a `Double` here and a `$9.99` there, availability is nested inside
 * [inventory], and blank strings mean absent.
 *
 * Only the fields the app uses are declared; `ignoreUnknownKeys` drops the rest of what is a
 * large, mostly merchandising response. Every property has a default for the same reason as
 * [User]: a missing key without one fails the whole parse.
 */
@Serializable
data class Product(
    val productId: String = "",
    val productName: String = "",
    val productDescription: String? = null,
    /** The scannable code. Not the same value as [productId], which is what the route carries. */
    val productBarcode: String? = null,
    val brandCode: String? = null,
    val brandDescription: String? = null,
    val categoryId: String = "",
    val secondaryCategoryIds: List<String> = emptyList(),
    val priceInfo: PriceInfo? = null,
    val imageUrls: List<String> = emptyList(),
    /**
     * The single hero image the search endpoint sends alongside [imageGroups] — one URL rather
     * than a per-colour gallery, which is what the card thumbnail actually wants.
     */
    val productImageUrl: String? = null,
    /** The same pictures grouped by colour variant. See [ImageGroup] on how the two differ. */
    val imageGroups: List<ImageGroup> = emptyList(),
    val promotions: List<Promotion> = emptyList(),
    /** Replaces the legacy `Price.Type == "CLR"` test behind the clearance badge. */
    val isClearance: Boolean = false,
    val isMaster: Boolean = false,
    val onSpecial: Boolean = false,
    val inventory: Inventory? = null,
    val featureList: List<String> = emptyList(),

    /**
     * Only sent for a product with colour variants — absent from a single-colour product's
     * response rather than empty, which is why all three are nullable.
     */
    val colourAttribute: String? = null,
    val colourDescription: String? = null,
    /** The colour as the search filters spell it, which is not always [colourDescription]. */
    val refinementColour: String? = null,
    val sizeAttribute: String? = null,
    val sizeDescription: String? = null,

    /** Where the product sits on the website. What the share action hands out. */
    val productUrl: String? = null,

    val shippingSize: String? = null,
    val isOversized: Boolean = false,
    val deliveryTime: String? = null,
    /**
     * A flag the endpoint sends as text rather than as a boolean: "Y" or "N". Kept as the raw
     * string, because a wire type should not be the place a guess about the other values is
     * made.
     */
    val soldOnline: String? = null,
    /** Another text flag — "O" in the sample; the full set of values is unconfirmed. */
    val clickAndCollect: String? = null,
    /** The boolean the endpoint sends alongside [clickAndCollect]. Prefer this one. */
    val isClickAndCollect: Boolean = false,

    val isDigital: Boolean = false,
    val isGiftcard: Boolean = false,
    val isEssentialItem: Boolean = false,
    val isMarketPlace: Boolean = false,

    val manufacturer: String? = null,
    val manufacturerSku: String? = null,
    val subClassId: String? = null,
    /** The id this product has in the master data system, distinct from [productId]. */
    val mdmProductId: String? = null,
) {

    /** Formatted here because there is no locale-aware number formatter in common code. */
    val formattedPrice: String? get() = priceInfo?.price?.let(::formatNzd)

    val isAvailable: Boolean get() = inventory?.available ?: false

    /**
     * [imageUrls] with blanks removed. The gallery indexes this directly, so an empty string
     * would be a page showing nothing.
     */
    val images: List<String> get() = imageUrls.filter { it.isNotBlank() }

    /** Absent and blank mean the same thing to a screen, so both come back as null. */
    val brand: String? get() = brandDescription?.takeIf { it.isNotBlank() }

    val barcode: String? get() = productBarcode?.takeIf { it.isNotBlank() }

    val description: String? get() = productDescription?.takeIf { it.isNotBlank() }
}

/**
 * `9.99` to `$9.99`, `9.9` to `$9.90`. The legacy screen concatenated the raw value, which
 * printed a whole-dollar price as `$9.0`.
 *
 * Rounds to the nearest cent rather than truncating, so a price that arrives as `9.989999`
 * from a JSON double does not lose a cent.
 */
internal fun formatNzd(price: Double): String {
    val cents = round(abs(price) * 100).toLong()
    val sign = if (price < 0) "-" else ""
    return "$sign\$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}
