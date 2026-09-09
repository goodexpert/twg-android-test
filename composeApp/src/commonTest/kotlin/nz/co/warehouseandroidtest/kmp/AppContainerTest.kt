package nz.co.warehouseandroidtest.kmp

import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class AppContainerTest {

    /**
     * Only [AppContainer.warehouseApi] is exercised. Reading `sessionRepository` would build the
     * secure store, which means the real Keychain here and a Context on Android — neither
     * belongs in a unit test. The laziness contract is the same for both.
     */
    @Test
    fun warehouseApiIsBuiltOnceAndReused() {
        val container = AppContainer()

        assertSame(container.warehouseApi, container.warehouseApi)
    }

    @Test
    fun separateContainersDoNotShareInstances() {
        // Being a class rather than an object is what makes fakes possible in tests.
        assertNotSame(AppContainer().warehouseApi, AppContainer().warehouseApi)
    }
}
