package org.rooftop.shop.infra.transaction

import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.api.transaction.TransactionState
import org.rooftop.api.transaction.transaction
import org.rooftop.shop.domain.product.UndoProduct
import org.springframework.beans.factory.annotation.Qualifier
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
        ByteArrayRedisSerializer::class,
        ReactiveRedisConfigurer::class,
        TransactionIdGenerator::class,
        RedisContainerConfigurer::class,
    ]
)
@TestPropertySource("classpath:application.properties")
internal class RedisPlayGround(
    private val transactionIdGenerator: TransactionIdGenerator,
    @Qualifier("transactionServer") private val transactionServer: ReactiveRedisTemplate<String, ByteArray>,
    @Qualifier("undoServer") private val productUndoServer: ReactiveRedisTemplate<String, UndoProduct>,
) : DescribeSpec({

    extension(SpringExtension)

    describe("redis-stream 에") {
        context("message-id와 함께 Transaction 을 저장하면,") {

            val expectedTransactionId = transactionIdGenerator.generate().block()!!

            it("message-id를 반환한다.") {
                val result = Mono.just(expectedTransactionId)
                    .joinOrCreateTransaction(transactionServer)

                StepVerifier.create(result)
                    .assertNext {
                        it.value shouldBeEqual expectedTransactionId
                    }
                    .verifyComplete()
            }

        }
    }
}) {

    companion object {
        private const val STREAM_KEY = "ROOFTOP_TRANSACTION"
        private const val TRANSACTION_KEY = "TRANSACTION_KEY"

        private fun Mono<String>.joinOrCreateTransaction(transactionServer: ReactiveRedisTemplate<String, ByteArray>): Mono<RecordId> {
            return this.flatMap { transactionId ->
                transactionServer.opsForStream<String, ByteArray>()
                    .add(
                        Record.of<String?, String?, ByteArray?>(mapOf(TRANSACTION_KEY to transaction {
                            id = transactionId
                            this.state = TransactionState.JOIN
                        }.toByteArray())).withId(RecordId.of(transactionId))
                            .withStreamKey(STREAM_KEY)
                    )
            }
        }
    }
}
