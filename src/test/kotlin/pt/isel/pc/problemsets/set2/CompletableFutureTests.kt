package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.assertThrows
import pt.isel.pc.problemsets.auxs.pressureTest
import kotlin.test.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals


class CompletableFutureTests {

    @Test
    fun `create N futures with different times to complete successfully`() {

        val N = 3
        val futures = mutableListOf<CompletableFuture<String>>()

        repeat(3) {
            futures.add(createDelayedFuture("Result $it", it.toLong()))
        }

        assertEquals("Result 0", any(futures).get())
    }

    @Test
    fun `pressure test create N futures with different times to complete successfully`() {
        pressureTest(::`create N futures with different times to complete successfully`, 1)
    }

    @Test
    fun `create N futures with different times to complete exceptionally`() {

        val N = 3
        val futures = mutableListOf<CompletableFuture<String>>()

        repeat(3) {
            futures.add(createDelayedFuture("Result $it", it.toLong(), true))
        }

        assertThrows<Exception> { any(futures).get() }
    }

    @Test
    fun `pressure test create N futures with different times to complete exceptionally`() {
        pressureTest(::`create N futures with different times to complete exceptionally`, 1)
    }


    // Aux Function
    /**
     * Creates a delayed CompletableFuture that completes with the specified value after a given delay.
     * @param value The value with which the CompletableFuture is completed.
     * @param delay The time delay (in milliseconds) before the CompletableFuture is completed.
     * @param completesExceptionally Specifies whether the CompletableFuture completes exceptionally.
     * @return The CompletableFuture that completes after the specified delay with the given value.
     */
    private fun <T> createDelayedFuture(value: T, delay: Long, completesExceptionally: Boolean = false): CompletableFuture<T> {
        return CompletableFuture<T>().apply {
            CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS).execute {
                if (completesExceptionally) completeExceptionally(Exception())
                else complete(value)
            }
        }
    }
}