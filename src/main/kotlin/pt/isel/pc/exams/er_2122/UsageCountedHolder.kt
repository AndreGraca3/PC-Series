package pt.isel.pc.exams.er_2122

import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class UsageCountedHolder<T : Closeable>(value: T) {

    @Volatile
    private var value: T? = value
    private var useCounter: AtomicInteger = AtomicInteger(1)

    fun startUse(): T {
        while (true) {
            val oldCounter = useCounter.get()

            if (oldCounter == 0) throw IllegalStateException("Already closed")
            if (!useCounter.compareAndSet(oldCounter, oldCounter + 1)) continue
            return value!!
        }
    }

    fun endUse() {
        while (true) {
            val oldCounter = useCounter.get()
            val oldValue = value

            if (oldCounter == 0) throw IllegalStateException("Already closed")

            if (!useCounter.compareAndSet(oldCounter, oldCounter - 1)) continue

            if (oldCounter - 1 == 0) {
                oldValue!!.close()
                value = null
            }
        }
    }
}


fun main() {
    val closeable = Closeable { println("ola") }
    val holder = UsageCountedHolder(closeable)
    repeat(100) {
        thread {
            holder.startUse()
            Thread.sleep(10*it.toLong())
            holder.endUse()
        }
    }
}