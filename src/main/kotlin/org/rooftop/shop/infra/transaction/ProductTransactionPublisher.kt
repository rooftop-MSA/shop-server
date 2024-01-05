package org.rooftop.shop.infra.transaction

import org.rooftop.api.transaction.Transaction
import org.rooftop.api.transaction.TransactionState
import org.rooftop.api.transaction.transaction
import org.rooftop.shop.domain.TransactionPublisher
import org.rooftop.shop.domain.product.UndoProduct
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
class ProductTransactionPublisher(
    @Qualifier("transactionServer") private val transactionServer: ReactiveRedisTemplate<String, ByteArray>,
    @Qualifier("undoServer") private val productUndoServer: ReactiveRedisTemplate<String, UndoProduct>,
) : TransactionPublisher<UndoProduct> {

    override fun join(transactionId: String, state: UndoProduct): Mono<String> {
        return joinOrStartTransaction()
            .undoBeforeState(state)
            .contextWrite { context ->
                context.put("transactionId", transactionId)
            }
    }

    private fun joinOrStartTransaction(): Mono<String> {
        return Mono.deferContextual<String> { Mono.just(it["transactionId"]) }
            .flatMap { transactionId ->
                publishTransaction(transactionId, transaction {
                    id = transactionId
                    state = TransactionState.JOIN
                })
            }
    }

    private fun Mono<String>.undoBeforeState(state: UndoProduct): Mono<String> {
        return this.flatMap { transactionId ->
            productUndoServer.opsForValue().set("PRODUCT:$transactionId", state)
                .flatMap {
                    when (it) {
                        true -> Mono.just(it)
                        false -> Mono.error {
                            IllegalStateException("error occurred cause set undo fail")
                        }
                    }
                }
                .transformTransactionId()
        }
    }

    private fun publishTransaction(transactionId: String, transaction: Transaction): Mono<String> {
        return transactionServer.opsForStream<String, ByteArray>()
            .add(
                Record.of<String?, String?, ByteArray?>(mapOf(DATA to transaction.toByteArray()))
                    .withStreamKey(transactionId)
            )
            .transformTransactionId()
    }

    private fun Mono<*>.transformTransactionId(): Mono<String> {
        return this.flatMap {
            Mono.deferContextual { Mono.just(it["transactionId"]) }
        }
    }

    override fun commit(transactionId: String): Mono<Unit> {
        TODO()
    }

    @Transactional
    override fun rollback(transactionId: String): Mono<Unit> {
        TODO()
    }

    companion object {
        private const val DATA = "data"
    }
}
