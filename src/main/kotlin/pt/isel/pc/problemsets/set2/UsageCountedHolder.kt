package pt.isel.pc.problemsets.set2

import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference


class UsageCountedHolder<T : Closeable>(value: T) {

    private var value = AtomicReference(value)
    private var useCounter = AtomicInteger(1)

    fun tryStartUse(): T? {
        while (true) {
            val counter = useCounter.get()
            val res = value.get() ?: return null
            if (value.get() != null && useCounter.compareAndSet(counter, counter + 1)) {
                return res
            }
        }
    }

    fun endUse() {
        while (true) {
            val counter = useCounter.get()
            val res = value.get()
            if (counter == 0) throw IllegalStateException("Already closed")
            if (!useCounter.compareAndSet(counter, counter - 1)) continue
            if (counter - 1 == 0) {
                value.get()?.close()
                value.compareAndSet(res, null)
            }
            return
        }
    }
}