package pt.isel.pc.problemsets.set2

import pt.isel.pc.problemsets.auxs.pressureTest
import java.io.Closeable
import kotlin.test.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class UsageCountedHolderTests {

    @Test
    fun `use holder one time for N threads`() {
        val N = 30
        val closeable = Closeable { "ISEL" }
        val holder = UsageCountedHolder(closeable)
        val res: MutableList<Closeable?> = Collections.synchronizedList(ArrayList())

        val threads = (1..N).map {
            thread {
                val value = holder.tryStartUse()
                holder.endUse()
                res.add(value)
            }
        }

        threads.forEach(Thread::join)
        assertEquals(N, res.size)
        assertEquals(1, res.filterNotNull().distinct().size)
    }

    @Test
    fun `pressure test use holder one time for N threads`() {
        pressureTest(::`use holder one time for N threads`, 1)
    }
}