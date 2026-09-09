package nz.co.warehouseandroidtest.kmp

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class PlatformTest {

    @Test
    fun platformNameIsPopulated() {
        assertTrue(currentPlatform().name.isNotBlank())
    }

    @Test
    fun deviceHeaderIsAValueTheApiAccepts() {
        assertContains(setOf("Android", "iOS"), currentPlatform().deviceHeader)
    }
}
