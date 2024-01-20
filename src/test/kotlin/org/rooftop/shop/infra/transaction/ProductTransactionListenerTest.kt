package org.rooftop.shop.infra.transaction

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.DisplayName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.rooftop.shop.app.product.TransactionIdGenerator
import org.rooftop.shop.domain.app.undoProduct
import org.rooftop.shop.domain.product.ProductRollbackEvent
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.seconds

@DisplayName("ProductTransactionListenerTest 클래스 의")
@ContextConfiguration(
    classes = [
        EventCapture::class,
        RedisContainerConfigurer::class,
        ByteArrayRedisSerializer::class,
        ReactiveRedisConfigurer::class,
        TransactionIdGeneratorImpl::class,
        ProductTransactionManager::class,
        ProductTransactionListener::class,
    ]
)
@TestPropertySource("classpath:application.properties")
internal class ProductTransactionListenerTest(
    private val eventCapture: EventCapture,
    private val transactionIdGenerator: TransactionIdGenerator,
    private val transactionPublisher: ProductTransactionManager,
    private val transactionListener: ProductTransactionListener,
) : DescribeSpec({

    afterEach { eventCapture.clear() }

    describe("subscribeStream 메소드는") {
        context("rollback transaction 이 들어오면,") {

            val transactionId = transactionIdGenerator.generate()

            it("ProductRollbackEvent 를 발행한다.") {
                transactionPublisher.join(transactionId, undoProduct()).block()
                transactionPublisher.rollback(transactionId).block()

                eventually(10.seconds) {
                    eventCapture.capturedCount(ProductRollbackEvent::class) shouldBeEqual 1
                }
            }
        }

        context("여러개의 transactionId가 등록되어도 ") {

            val transactionId1 = transactionIdGenerator.generate()
            val transactionId2 = transactionIdGenerator.generate()

            it("동시에 요청을 읽을 수 있다.") {
                transactionPublisher.join(transactionId1, undoProduct()).block()
                transactionPublisher.join(transactionId2, undoProduct()).block()

                transactionPublisher.rollback(transactionId1).block()
                transactionPublisher.rollback(transactionId2).block()

                eventually(10.seconds) {
                    eventCapture.capturedCount(ProductRollbackEvent::class) shouldBeEqual 2
                }
            }
        }
    }

})
