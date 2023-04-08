package pt.isel.pc.problemsets.set1

import kotlin.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class NAryExchanger<T>(private val groupSize: Int) {

    init {
        require (groupSize >= 2) { throw IllegalArgumentException("groupSize cannot be less than 2") }
    }

    private val lock = ReentrantLock()
    private val groupWaiters = lock.newCondition()

    private inner class Request(
        var values: MutableList<T> = mutableListOf(),
        var isDone: Boolean = false
    )

    private var requests = mutableListOf<Request>()
    private var items = mutableListOf<T>()


    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): List<T>? {
        lock.withLock {
            items.add(value)

            if (requests.size + 1 == groupSize) {
                val list = items
                unblockWaitingThreads(list)
                items = mutableListOf()
                requests = mutableListOf()
                return list
            }

            val request = Request()
            requests.add(request)

            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = groupWaiters.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (request.isDone) {   // in case thread is interrupted right when it's done
                        Thread.currentThread().interrupt()
                        return request.values
                    }
                    removeThreadFromGroupWaiters(value, request)
                    throw e
                }

                if (request.isDone) {
                    return request.values
                }

                if (remainingTime <= 0) {
                    removeThreadFromGroupWaiters(value, request)
                    return null
                }
            }
        }
    }

    private fun removeThreadFromGroupWaiters(value: T, request: Request) {
        items.remove(value)
        requests.remove(request)
    }

    private fun unblockWaitingThreads(list: MutableList<T>) {
        requests.forEach {
            it.values = list
            it.isDone = true
        }
        groupWaiters.signalAll()
    }
}