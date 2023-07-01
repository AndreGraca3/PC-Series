package pt.isel.pc.exams.en_2122

import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Exchanger<T> {

    private val lock = ReentrantLock()

    private var waiter: Request? = null

    private inner class Request(val cont: Continuation<T>, val value: T)

    suspend fun exchange(value: T): T {
        lock.lock()

        // fast path
        if (waiter != null) {
            waiter!!.cont.resume(value)
            val res = waiter!!.value
            waiter = null
            lock.unlock()
            return res
        }

        // suspend path
        return suspendCoroutine {
            waiter = Request(it, value)
            lock.unlock()
        }
    }
}