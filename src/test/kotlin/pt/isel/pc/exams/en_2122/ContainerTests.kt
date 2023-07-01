package pt.isel.pc.exams.en_2122

import pt.isel.pc.problemsets.auxs.pressureTest
import kotlin.test.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class ContainerTests {

    @Test
    fun `consume all container values by N threads`() {
        val N = 30
        val values = arrayOf(Value("isel", 3), Value("pc", 4))
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
        assertEquals(7, res.size)
        assertEquals(3, res.count { it == "isel" })
        assertEquals(4, res.count { it == "pc" })
    }

    @Test
    fun `pressure test consume all container values`() {
        pressureTest(::`consume all container values by N threads`, 1)
    }
}