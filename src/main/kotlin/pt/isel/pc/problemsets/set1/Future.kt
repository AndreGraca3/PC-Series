package pt.isel.pc.problemsets.set1

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

class Future<T> : Future<T> {
    private val lock = ReentrantLock()
    private val waiters = lock.newCondition()

    private var thread: Thread? = null
    private var result: T? = null
    private var exception: Exception? = null
    private var isCancelled = false

    fun start() {
        if (thread == null) thread = Thread.currentThread()
    }

    fun setResult(result: T) {
        lock.withLock {
            if (isDone) return
            this.result = result
            waiters.signalAll()
        }
    }

    fun setException(e: Exception) {
        lock.withLock {
            if (isDone) return
            this.exception = e
            waiters.signalAll()
        }
    }

    override fun get(): T {
        return getWithCtx()
    }

    @Throws(TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): T {
        return getWithCtx(timeout, unit)
    }

    private fun getWithCtx(timeout: Long? = null, unit: TimeUnit? = null): T {
        lock.withLock {
            // fast path
            var res = getResult()
            if(res != null) return res

            // wait path
            while (true) {
                try {
                    if (timeout == null) waiters.await()
                    else waiters.await(timeout, unit)
                } catch (e: InterruptedException) {
                    res = getResult()
                    if(res != null) {
                        Thread.currentThread().interrupt()
                        return res
                    }
                    throw e
                }
                return getResult() ?: throw TimeoutException("Timeout reached.")
            }
        }
    }

    // Gets current result, throws Exception if this future is cancelled or ended with exception.
    private fun getResult(): T? {
        if (isCancelled) throw CancellationException()
        if (exception != null) throw ExecutionException(exception)
        return result
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        lock.withLock {
            if (isCancelled) return false
            isCancelled = true
            waiters.signalAll()
            if (mayInterruptIfRunning) thread?.interrupt()
            return true
        }
    }

    override fun isCancelled(): Boolean {
        lock.withLock { return isCancelled }
    }

    override fun isDone(): Boolean {
        lock.withLock {
            return result != null || exception != null
        }
    }
}