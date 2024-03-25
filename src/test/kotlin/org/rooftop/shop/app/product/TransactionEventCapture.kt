package org.rooftop.order.app

import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.netx.api.TransactionCommitEvent
import org.rooftop.netx.api.TransactionCommitListener
import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionRollbackListener
import org.rooftop.netx.engine.core.TransactionState
import org.rooftop.netx.meta.TransactionHandler
import org.rooftop.shop.app.product.event.PayCancelEvent
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

@TransactionHandler
class TransactionEventCapture {

    val events = mutableMapOf<TransactionState, AtomicInteger>()

    fun clear() {
        events.clear()
    }

    fun commitShouldBeEqual(count: Int) {
        (events[TransactionState.COMMIT]?.get() ?: 0) shouldBeEqual count
    }

    fun rollbackShouldBeEqual(count: Int) {
        (events[TransactionState.ROLLBACK]?.get() ?: 0) shouldBeEqual count
    }

    @TransactionCommitListener
    fun handleCommitEvent(transactionCommitEvent: TransactionCommitEvent): Mono<Int> {
        return Mono.fromCallable {
            events.putIfAbsent(TransactionState.COMMIT, AtomicInteger(0))
            events[TransactionState.COMMIT]!!.incrementAndGet()
        }
    }

    @TransactionRollbackListener
    fun handleRollbackEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Int> {
        println(">>> $transactionRollbackEvent")
        return Mono.fromCallable {
            events.putIfAbsent(TransactionState.ROLLBACK, AtomicInteger(0))
            events[TransactionState.ROLLBACK]!!.incrementAndGet()
        }
    }
}
