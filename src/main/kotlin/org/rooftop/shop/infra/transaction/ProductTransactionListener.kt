package org.rooftop.shop.infra.transaction

import org.rooftop.api.transaction.TransactionState
import org.rooftop.api.transaction.Transaction
import org.rooftop.shop.domain.TransactionJoinedEvent
import org.rooftop.shop.domain.product.ProductRollbackEvent
import org.rooftop.shop.domain.product.UndoProduct
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.data.redis.stream.StreamReceiver.StreamReceiverOptions
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration

@Component
class ProductTransactionListener(
    private val eventPublisher: ApplicationEventPublisher,
    @Qualifier("transactionServerConnectionFactory") private val connectionFactory: ReactiveRedisConnectionFactory,
    @Qualifier("undoServer") private val productUndoServer: ReactiveRedisTemplate<String, UndoProduct>,
) {

    @EventListener(TransactionJoinedEvent::class)
    fun subscribeStream(transactionJoinedEvent: TransactionJoinedEvent) {
        val options = StreamReceiverOptions.builder()
            .pollTimeout(Duration.ofMillis(100))
            .build()

        val receiver = StreamReceiver.create(connectionFactory, options)

        receiver.receive(StreamOffset.fromStart(transactionJoinedEvent.transactionId))
            .subscribeOn(Schedulers.boundedElastic())
            .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) }
            .dispatch()
            .subscribe()
    }

    private fun Flux<Transaction>.dispatch(): Flux<Transaction> {
        return this.filter { it.state == TransactionState.TRANSACTION_STATE_ROLLBACK }
            .flatMap { transaction ->
                productUndoServer.opsForValue()["PRODUCT:${transaction.id}"]
                    .doOnNext { eventPublisher.publishEvent(ProductRollbackEvent(it)) }
                    .map { transaction }
                    .flatMap {
                        productUndoServer.opsForValue().delete("PRODUCT:${transaction.id}")
                            .map { transaction }
                            .retry()
                    }
            }
    }
}
