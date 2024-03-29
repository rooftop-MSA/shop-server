package org.rooftop.order.app

import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.netx.api.TransactionCommitEvent
import org.rooftop.netx.api.TransactionCommitListener
import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionRollbackListener
import org.rooftop.netx.meta.TransactionHandler
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

@TransactionHandler
class TransactionEventCapture {

    val events = mutableMapOf<String, AtomicInteger>()

    fun clear() {
        events.clear()
    }

    fun commitShouldBeEqual(count: Int) {
        (events["COMMIT"]?.get() ?: 0) shouldBeEqual count
    }

    fun rollbackShouldBeEqual(count: Int) {
        (events["ROLLBACK"]?.get() ?: 0) shouldBeEqual count
    }

    @TransactionCommitListener
    fun handleCommitEvent(transactionCommitEvent: TransactionCommitEvent): Mono<Int> {
        return Mono.fromCallable {
            events.putIfAbsent("COMMIT", AtomicInteger(0))
            events["COMMIT"]!!.incrementAndGet()
        }
    }

    @TransactionRollbackListener
    fun handleRollbackEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Int> {
        return Mono.fromCallable {
            events.putIfAbsent("ROLLBACK", AtomicInteger(0))
            events["ROLLBACK"]!!.incrementAndGet()
        }
    }
}
