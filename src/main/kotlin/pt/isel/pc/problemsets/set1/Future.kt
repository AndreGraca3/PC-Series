package pt.isel.pc.problemsets.set1

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

class Future<T> {

    private val lock = ReentrantLock()
    private val waiters = lock.newCondition()

    private var thread: Thread? = null
    private var result: T? = null
    private var exception: Exception? = null
    private var isDone = false
    private var isCancelled = false

    fun start() {
        if (thread == null) thread = Thread.currentThread()
    }

    fun setResult(result: T) {
        lock.withLock {
            if (!isDone) {
                this.result = result
                isDone = true
                waiters.signalAll()
            }
        }
    }

    fun setException(e: Exception) {
        lock.withLock {
            if (!isDone) {
                this.exception = e
                isDone = true
                waiters.signalAll()
            }
        }
    }

    @Throws(ExecutionException::class)
    fun get(): T? {
        lock.withLock {
            while (true) {
                if (isCancelled) throw CancellationException()
                if (exception != null) throw ExecutionException(exception)
                if (isDone) return result
                waiters.await()
            }
        }
    }

    @Throws(TimeoutException::class)
    fun get(timeout: Long, unit: TimeUnit): T? {
        lock.withLock {
            if (isCancelled) throw CancellationException()
            if (exception != null) throw ExecutionException(exception)
            if (isDone) return result
            waiters.await(timeout, unit)
            if (isCancelled) throw CancellationException()
            if (exception != null) throw ExecutionException(exception)
            if (!isDone) throw TimeoutException()
            return result
        }
    }

    fun cancel(mayInterruptIfRunning: Boolean) {
        lock.withLock {
            isCancelled = true
            isDone = true
            waiters.signalAll()
            if (mayInterruptIfRunning) thread?.interrupt()
        }
    }

    fun isDone(): Boolean {
        lock.withLock {
            return isDone
        }
    }
}