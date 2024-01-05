package org.rooftop.shop.infra.transaction

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import org.rooftop.api.transaction.Transaction
import org.rooftop.api.transaction.TransactionState
import org.rooftop.api.transaction.transaction
import org.rooftop.shop.domain.product.UndoProduct
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@DisplayName("Redis play ground 의")
@ContextConfiguration(
    classes = [
        RedisContainerConfigurer::class,
        ByteArrayRedisSerializer::class,
        ReactiveRedisConfigurer::class,
        TransactionIdGeneratorImpl::class,
    ]
)
@TestPropertySource("classpath:application.properties")
internal class RedisPlayGround(
    private val transactionIdGenerator: TransactionIdGeneratorImpl,
    @Qualifier("transactionServer") private val transactionServer: ReactiveRedisTemplate<String, ByteArray>,
    @Qualifier("undoServer") private val productUndoServer: ReactiveRedisTemplate<String, UndoProduct>,
) : DescribeSpec({

    describe("redis-stream 에") {
        context("stream-key 와 함께 Transaction 을 저장하면,") {

            val expectedTransactionId = transactionIdGenerator.generate()

            it("stream-key로 저장된 Transaction들을 읽을 수 있다.") {
                val result = Mono.just(expectedTransactionId)
                    .joinOrCreateTransaction(transactionServer)
                    .map { expectedTransactionId }
                    .readTransactionByStreamKey(transactionServer)
                    .log()

                StepVerifier.create(result)
                    .assertNext {
                        it shouldBeEqualUsingFields transaction {
                            id = expectedTransactionId
                            serverId = TRANSACTION_SERVER_ID
                            state = TransactionState.JOIN
                        }
                    }
                    .verifyComplete()
            }

        }
    }
}) {

    companion object {
        private const val STREAM_KEY = "ROOFTOP_TRANSACTION"
        private const val TRANSACTION_SERVER_ID = "product-1"
        private const val TRANSACTION_KEY = "TRANSACTION_KEY"

        private fun Mono<String>.joinOrCreateTransaction(transactionServer: ReactiveRedisTemplate<String, ByteArray>): Mono<RecordId> {
            return this.flatMap { transactionId ->
                transactionServer.opsForStream<String, ByteArray>()
                    .add(
                        Record.of<String?, String?, ByteArray?>(mapOf(TRANSACTION_KEY to transaction {
                            id = transactionId
                            serverId = TRANSACTION_SERVER_ID
                            this.state = TransactionState.JOIN
                        }.toByteArray())).withStreamKey(transactionId)
                    )
            }
        }

        private fun Mono<String>.readTransactionByStreamKey(transactionServer: ReactiveRedisTemplate<String, ByteArray>): Mono<Transaction> {
            return this.flatMap { transactionId ->
                transactionServer.opsForStream<String, ByteArray>()
                    .range(transactionId, Range.open("-", "+"))
                    .map {
                        Transaction.parseFrom(
                            it.value[TRANSACTION_KEY].toString().toByteArray()
                        )
                    }.next()
            }
        }
    }
}
