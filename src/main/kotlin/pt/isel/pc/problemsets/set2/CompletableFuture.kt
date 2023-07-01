package pt.isel.pc.problemsets.set2

import java.lang.Thread.currentThread
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture


fun <T> any(futures: List<CompletableFuture<T>>): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    if (futures.isEmpty()) return future
    var i = 0
    val ex = Exception()

    for (f in futures) {
        f.whenComplete { res, throwable ->
            i++
            if (throwable != null) {
                ex.addSuppressed(throwable)
            } else {
                if (i == 1) future.complete(res)
            }
            if (i == futures.size) future.completeExceptionally(ex)
        }
    }
    return future
}

fun asyncFunction1(str: String) : CompletableFuture<String> =
    CompletableFuture.supplyAsync<String> {
        println("++ AUXILIAR [T${ currentThread().id }] ++")
        sleep(3000)
        println("++ FINISHED [T${ currentThread().id }] ++")
        str
    }

fun main() {
    /*val res = asyncFunction1("ISEL").thenCombine(asyncFunction1("PC")) { a, b ->
        sleep(1000)
        a + b
    }.get()
    println("Done: $res")*/
}