package org.rooftop.order.app

import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.netx.api.*
import org.rooftop.netx.idl.TransactionState
import org.rooftop.netx.meta.TransactionHandler
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

@TransactionHandler
class TransactionEventCapture {

    val events = mutableMapOf<TransactionState, AtomicInteger>()

    fun clear() {
        events.clear()
    }

    fun commitShouldBeEqual(count: Int) {
        (events.get(TransactionState.TRANSACTION_STATE_COMMIT)?.get() ?: 0) shouldBeEqual count
    }

    fun rollbackShouldBeEqual(count: Int) {
        (events.get(TransactionState.TRANSACTION_STATE_ROLLBACK)?.get() ?: 0) shouldBeEqual count
    }

    @TransactionCommitListener
    fun handleTransactionJoinEvent(transactionCommitEvent: TransactionCommitEvent): Mono<Int> {
        return Mono.fromCallable {
            events.putIfAbsent(TransactionState.TRANSACTION_STATE_COMMIT, AtomicInteger(0))
            events.get(TransactionState.TRANSACTION_STATE_COMMIT)!!.incrementAndGet()
        }
    }

    @TransactionRollbackListener
    fun handleRollbackEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Int> {
        return Mono.fromCallable {
            events.putIfAbsent(TransactionState.TRANSACTION_STATE_ROLLBACK, AtomicInteger(0))
            events.get(TransactionState.TRANSACTION_STATE_ROLLBACK)!!.incrementAndGet()
        }
    }
}
