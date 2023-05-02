package pt.isel.pc.problemsets.set2

import pt.isel.pc.problemsets.auxs.pressureTest
import kotlin.test.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class ContainerTests {

    @Test
    fun `consume all container letters by N threads`() {
        val N = 10
        val values = arrayOf(Value("A", 2), Value("B", 1), Value("C", 3))
        val container = Container(values)
        val res: MutableList<String> = Collections.synchronizedList(ArrayList())

        val threads = (1..N).map {
            thread {
                while (true) {
                    val value = container.consume() ?: break
                    res.add(value)
                }
            }
        }

        threads.forEach(Thread::join)
        assertEquals(6, res.size)
        assertEquals(2, res.count { it == "A" })
        assertEquals(1, res.count { it == "B" })
        assertEquals(3, res.count { it == "C" })
    }

    @Test
    fun `pressure test consume all container letters`() {
        pressureTest(::`consume all container letters by N threads`, 5)
    }
}