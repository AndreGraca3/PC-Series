package pt.isel.pc.problemsets.set2

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