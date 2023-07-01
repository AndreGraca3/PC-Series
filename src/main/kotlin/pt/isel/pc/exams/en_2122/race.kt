package pt.isel.pc.exams.en_2122

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

suspend fun race(f0: suspend () -> Int, f1: suspend () -> Int): Int = coroutineScope {
    val deferreds = listOf(
        async { f0() },
        async { f1() }
    )

    val completable = CompletableFuture<Int>()
    deferreds.map { it.asCompletableFuture() }.forEach { f ->
        f.whenComplete { res, error ->
            if (error != null) completable.completeExceptionally(error)
            else completable.complete(res)
            deferreds.forEach { it.cancel() }
        }
    }

    return@coroutineScope completable.await()
}

suspend fun f0(): Int {
    delay(3000)
    return 4
}

suspend fun f1(): Int {
    delay(1000)
    return 69
}

fun main() {
    runBlocking {
        println(race(::f0, ::f1))
    }
}
