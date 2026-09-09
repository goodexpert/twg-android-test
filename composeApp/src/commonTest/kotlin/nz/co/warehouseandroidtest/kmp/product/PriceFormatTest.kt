package nz.co.warehouseandroidtest.kmp.product

import nz.co.warehouseandroidtest.kmp.data.formatNzd
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceFormatTest {

    @Test
    fun keepsTwoDecimals() {
        assertEquals("$9.99", formatNzd(9.99))
    }

    @Test
    fun padsAWholeDollarPrice() {
        // The legacy screen concatenated the raw value and printed this as "$9.0".
        assertEquals("$9.00", formatNzd(9.0))
    }

    @Test
    fun padsASingleDecimal() {
        assertEquals("$9.90", formatNzd(9.9))
    }

    @Test
    fun roundsToTheNearestCent() {
        // A price that arrives as a JSON double must not lose a cent to truncation.
        assertEquals("$9.99", formatNzd(9.989999))
        assertEquals("$10.00", formatNzd(9.999))
    }

    @Test
    fun formatsZero() {
        assertEquals("$0.00", formatNzd(0.0))
    }

    @Test
    fun keepsTheSignOnANegativePrice() {
        // Not expected from this endpoint, but the sign must not be dropped silently.
        assertEquals("-$1.50", formatNzd(-1.5))
    }
}
