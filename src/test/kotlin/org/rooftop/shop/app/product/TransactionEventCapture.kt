package org.rooftop.shop.app.product

import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.netx.api.SagaCommitEvent
import org.rooftop.netx.api.SagaCommitListener
import org.rooftop.netx.api.SagaRollbackEvent
import org.rooftop.netx.api.SagaRollbackListener
import org.rooftop.netx.meta.SagaHandler
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

@SagaHandler
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

    @SagaCommitListener
    fun handleCommitEvent(sagaCommitEvent: SagaCommitEvent): Mono<Int> {
        return Mono.fromCallable {
            events.putIfAbsent("COMMIT", AtomicInteger(0))
            events["COMMIT"]!!.incrementAndGet()
        }
    }

    @SagaRollbackListener
    fun handleRollbackEvent(sagaRollbackEvent: SagaRollbackEvent): Mono<Int> {
        return Mono.fromCallable {
            events.putIfAbsent("ROLLBACK", AtomicInteger(0))
            events["ROLLBACK"]!!.incrementAndGet()
        }
    }
}
